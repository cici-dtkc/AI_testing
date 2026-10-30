import hashlib
import json
import os
import re
import time
from collections.abc import Callable
from pathlib import Path

from google import genai
from google.genai import errors, types

from .models import TestCase, TestSuite, TechniquePlan
from .prompt import PROMPT_VERSION, SYSTEM_PROMPT

# Nghỉ giữa các request module để không vượt rate limit của gói Free.
MODULE_DELAY_SECONDS = float(os.getenv('GEMINI_MODULE_DELAY', '2'))
MAX_ATTEMPTS = 4
# Lượt bù thiếu chỉ nhận tối đa chừng này scenario để request vẫn nhỏ.
MAX_FILL_SCENARIOS = 12


def gemini_schema(model: type) -> dict:
    """Hàm chuẩn hóa JSON schema cho Gemini API."""
    schema = model.model_json_schema()

    def remove_additional_properties(value):
        if isinstance(value, dict):
            value.pop('additionalProperties', None)
            for child in value.values():
                remove_additional_properties(child)
        elif isinstance(value, list):
            for child in value:
                remove_additional_properties(child)

    remove_additional_properties(schema)
    return schema


def _client(key: str) -> genai.Client:
    if not key or key == 'your_gemini_api_key_here':
        raise ValueError('Điền GEMINI_API_KEY trong .env trước khi chạy.')
    # Tăng timeout lên 300 giây để tránh bị timeout khi generate file lớn.
    return genai.Client(api_key=key, http_options=types.HttpOptions(timeout=300000))


def _payload(
    analysis: dict,
    plan: TechniquePlan | None,
    mode: str | None = None,
    white_box_sources: dict[str, str] | None = None,
) -> dict:
    payload = {
        'analysis': analysis,
        'technique_plan': plan.model_dump() if plan is not None else None,
    }
    if mode:
        payload['mode'] = mode
    if white_box_sources:
        payload['white_box_sources'] = white_box_sources
    return payload


def _generate_once(
    client: genai.Client,
    payload: dict,
    schema: dict,
    model: str,
) -> TestSuite:
    response = client.models.generate_content(
        model=model,
        contents=json.dumps(payload, ensure_ascii=False),
        config=types.GenerateContentConfig(
            system_instruction=SYSTEM_PROMPT,
            temperature=0,
            response_mime_type='application/json',
            response_schema=schema,
            max_output_tokens=65536,
        ),
    )
    if not response.text:
        raise ValueError('Gemini không trả text. Kiểm tra model hoặc input.')
    return TestSuite.model_validate_json(response.text)


def _friendly_error(exc: errors.APIError, model: str) -> str:
    detail = (getattr(exc, 'message', '') or '').strip()
    if exc.code == 429 and 'limit: 0' in detail:
        return (
            f'Gemini 429: model "{model}" không có quota Free Tier (limit: 0 cho mọi '
            'chỉ số). Đổi GEMINI_MODEL sang model còn quota, ví dụ gemini-flash-latest '
            'hoặc gemini-flash-lite-latest (hoặc chạy với --model gemini-flash-latest).'
        )
    if exc.code == 429:
        return (
            f'Gemini 429: tạm hết quota/rate limit cho model "{model}". Chờ khoảng '
            '1 phút rồi chạy lại hoặc đổi model. Chi tiết: ' + detail[:300]
        )
    return f'Gemini API lỗi {exc.code} (model "{model}"): ' + detail[:300]


def _retry_delay(exc: errors.APIError, attempt: int) -> float | None:
    detail = getattr(exc, 'message', '') or ''
    if 'limit: 0' in detail:
        return None  # quota model = 0: retry không bao giờ thành công
    if exc.code == 429:
        if 'per day' in detail:
            return None  # hết quota ngày: chờ vài phút cũng vô ích
        return 15 * (attempt + 1)
    if exc.code in (500, 502, 503, 504):
        return 5 * (attempt + 1)
    return None


def _generate_with_retry(
    client: genai.Client,
    payload: dict,
    schema: dict,
    model: str,
) -> TestSuite:
    for attempt in range(MAX_ATTEMPTS):
        try:
            return _generate_once(client, payload, schema, model)
        except errors.APIError as exc:
            delay = _retry_delay(exc, attempt)
            if delay is None or attempt == MAX_ATTEMPTS - 1:
                raise RuntimeError(_friendly_error(exc, model)) from None
            time.sleep(delay)
    raise RuntimeError('Gemini sinh test case thất bại.')


def generate_test_cases(
    analysis_data: dict,
    key: str,
    model: str,
    plan: TechniquePlan | None = None,
    white_box_sources: dict[str, str] | None = None,
) -> TestSuite:
    """Sinh toàn bộ test case trong một request (giữ để tương thích)."""
    schema = gemini_schema(TestSuite)
    payload = _payload(
        analysis_data.get('analysis', analysis_data),
        plan,
        white_box_sources=white_box_sources,
    )
    with _client(key) as client:
        return _generate_with_retry(client, payload, schema, model)


_COVERAGE_TAG_RE = re.compile(r'[A-Z]{1,3}\d+')
# Model đôi khi chép nguyên ký hiệu "Bn/Vn/Xn" trong tài liệu; đánh số lại theo thứ tự.
_PLACEHOLDER_TAG_RE = re.compile(r'([A-Z]{1,3})N')


def clean_coverage_items(
    items: list[str],
    counters: dict[str, int] | None = None,
) -> list[str]:
    """Bỏ các ID/scenario lỡ bị đưa vào coverage_items.

    `coverage_items` chỉ nhận tag ngắn (V1/X1/B1/D1/R1/T1/F1/P1/S1/DF1); scenario_id
    và rule_id đã có trường trace riêng. Ký hiệu `Bn` (n là biến trong tài liệu)
    được đánh số tăng dần trong module. Nếu lọc xong rỗng thì giữ nguyên để
    validation báo lỗi thay vì âm thầm làm mất dữ liệu.
    """
    counters = {} if counters is None else counters
    cleaned: list[str] = []
    for item in items:
        normalized = str(item).strip().upper()
        placeholder = _PLACEHOLDER_TAG_RE.fullmatch(normalized)
        if placeholder:
            prefix = placeholder.group(1)
            counters[prefix] = counters.get(prefix, 0) + 1
            normalized = f'{prefix}{counters[prefix]}'
        if _COVERAGE_TAG_RE.fullmatch(normalized) and normalized not in cleaned:
            cleaned.append(normalized)
    return cleaned or list(items)


WHITE_BOX_TECHNIQUES = {
    'BASIS_PATH',
    'STATEMENT_COVERAGE',
    'BRANCH_COVERAGE',
    'CONDITION_COVERAGE',
    'DATA_FLOW',
}

# Quy ước bài tập 8: Chính / E1, E2… / Ẩn cho MỌI test case.
_FLOW_SYNONYMS = {
    'chính': 'Chính',
    'đầy đủ': 'Chính',
    'chinh': 'Chính',
    'main': 'Chính',
    'happy': 'Chính',
    'thành công': 'Chính',
    'ẩn': 'Ẩn',
    'an': 'Ẩn',
    'hidden': 'Ẩn',
    'bổ sung': 'Ẩn',
    'khác': 'Ẩn',
}

_E_FLOW_RE = re.compile(r'e\s*([1-9]\d?)$', re.IGNORECASE)


def normalize_flow(value: str | None) -> str:
    """Chuẩn hoá về `Chính`, `Ẩn` hoặc `E<k>`; giá trị lạ bị bỏ để validation báo."""
    text = (value or '').strip()
    match = _E_FLOW_RE.fullmatch(text)
    if match:
        return f'E{match.group(1)}'
    return _FLOW_SYNONYMS.get(text.lower(), '')


def _with_module(
    cases: list[TestCase],
    module: str,
    plan_module=None,
) -> list[TestCase]:
    """Ép module đúng theo kế hoạch để gộp an toàn, kể cả khi LLM bỏ trống.

    Đồng thời chuẩn hoá cột Luồng và bù truy vết cho case hộp đen bị thiếu, nhưng
    chỉ khi module chỉ có đúng 1 scenario/1 rule — lúc đó việc gán là chắc chắn.
    """
    scenario_ids = list(plan_module.scenario_ids) if plan_module else []
    rule_ids = list(plan_module.rule_ids) if plan_module else []
    counters: dict[str, int] = {}
    updated: list[TestCase] = []
    for case in cases:
        changes = {
            'module': module,
            'coverage_items': clean_coverage_items(case.coverage_items, counters),
            'flow': normalize_flow(case.flow),
        }
        if (
            case.technique not in WHITE_BOX_TECHNIQUES
            and not case.trace_scenario_ids
            and not case.trace_rule_ids
        ):
            if len(scenario_ids) == 1:
                changes['trace_scenario_ids'] = scenario_ids
            elif len(rule_ids) == 1:
                changes['trace_rule_ids'] = rule_ids
        updated.append(case.model_copy(update=changes))
    return updated


def merge_module_cases(results: list[tuple[str, list[TestCase]]]) -> list[TestCase]:
    """Gộp kết quả từng module và đánh số lại ID cho duy nhất toàn bộ suite."""
    merged: list[TestCase] = []
    for _, cases in results:
        merged.extend(cases)
    return [
        case.model_copy(update={'test_case_id': f'TC-{index:02d}'})
        for index, case in enumerate(merged, start=1)
    ]


# --------------------------------------------------------------------------
# Cache theo module để chạy lại không mất phần đã sinh
# --------------------------------------------------------------------------


def _cache_path(cache_dir: Path, unit_name: str) -> Path:
    safe = re.sub(r'[^0-9A-Za-z_.-]+', '_', unit_name)[:120]
    return Path(cache_dir) / f'{safe}.json'


def _fingerprint(payload: dict) -> str:
    """Bỏ model khỏi fingerprint để đổi model vẫn dùng lại được case đã sinh.

    Prompt version nằm trong fingerprint để đổi format prompt (ví dụ thêm cột
    Luồng) sẽ tự sinh lại thay vì dùng cache cũ thiếu trường mới.
    """
    raw = PROMPT_VERSION + json.dumps(payload, ensure_ascii=False, sort_keys=True)
    return hashlib.sha256(raw.encode('utf-8')).hexdigest()


def _read_cache(cache_dir: Path | None, unit_name: str, payload: dict) -> list[TestCase] | None:
    if cache_dir is None:
        return None
    path = _cache_path(cache_dir, unit_name)
    if not path.is_file():
        return None
    try:
        data = json.loads(path.read_text(encoding='utf-8'))
        if data.get('fingerprint') != _fingerprint(payload):
            return None
        return [TestCase.model_validate(item) for item in data.get('cases') or []]
    except (OSError, ValueError, TypeError):
        return None


def _write_cache(
    cache_dir: Path | None,
    unit_name: str,
    payload: dict,
    cases: list[TestCase],
) -> None:
    if cache_dir is None:
        return
    path = _cache_path(cache_dir, unit_name)
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(
            json.dumps(
                {'fingerprint': _fingerprint(payload), 'cases': [c.model_dump() for c in cases]},
                ensure_ascii=False,
                indent=1,
            ),
            encoding='utf-8',
        )
    except OSError:
        pass  # cache là tối ưu, không được làm hỏng lần sinh chính


_COVERAGE_PART_RE = re.compile(r'([A-Z]{1,3})(\d+)')


def dedupe_coverage_tags(cases: list[TestCase]) -> list[TestCase]:
    """Cấp số mới cho coverage tag bị trùng trong cùng module.

    Lượt bù thiếu sinh tag từ đầu nên dễ trùng `V1`/`X1` với case đã có. Hai case
    khác nhau thì hạng mục cũng khác, nên tag trùng được đánh số tiếp theo của cùng
    tiền tố thay vì báo lỗi hoặc gộp case.
    """
    used: dict[str, set[int]] = {}
    for case in cases:
        for tag in case.coverage_items:
            match = _COVERAGE_PART_RE.fullmatch(tag.strip().upper())
            if match:
                used.setdefault(match.group(1), set()).add(int(match.group(2)))

    seen: set[str] = set()
    updated: list[TestCase] = []
    for case in cases:
        tags: list[str] = []
        for tag in case.coverage_items:
            normalized = tag.strip().upper()
            match = _COVERAGE_PART_RE.fullmatch(normalized)
            if match and normalized in seen:
                prefix = match.group(1)
                numbers = used.setdefault(prefix, set())
                new_number = max(numbers) + 1
                numbers.add(new_number)
                normalized = f'{prefix}{new_number}'
            tags.append(normalized)
            seen.add(normalized)
        updated.append(case.model_copy(update={'coverage_items': tags}))
    return updated


def drop_unknown_traces(
    cases: list[TestCase],
    analysis: dict,
) -> tuple[list[TestCase], list[str]]:
    """Bỏ ID truy vết không có trong analysis (model đôi khi tự bịa mã).

    Case vẫn được giữ vì nghiệp vụ và rule còn lại vẫn hợp lệ; mọi lần bỏ đều được
    ghi cảnh báo để QA biết model đã lệch khỏi danh mục scenario/rule.
    """
    scenario_ids = {str(s.get('scenario_id')) for s in analysis.get('scenarios') or []}
    rule_ids = {str(r.get('rule_id')) for r in analysis.get('rules') or []}
    cleaned: list[TestCase] = []
    warnings: list[str] = []
    for case in cases:
        bad_scenarios = [x for x in case.trace_scenario_ids if x not in scenario_ids]
        bad_rules = [x for x in case.trace_rule_ids if x not in rule_ids]
        if bad_scenarios or bad_rules:
            parts = []
            if bad_scenarios:
                parts.append('scenario ' + ', '.join(bad_scenarios))
            if bad_rules:
                parts.append('rule ' + ', '.join(bad_rules))
            warnings.append(
                f'{case.test_case_id}: bỏ truy vết không có trong analysis ('
                + '; '.join(parts)
                + ').'
            )
            case = case.model_copy(
                update={
                    'trace_scenario_ids': [
                        x for x in case.trace_scenario_ids if x in scenario_ids
                    ],
                    'trace_rule_ids': [x for x in case.trace_rule_ids if x in rule_ids],
                }
            )
        cleaned.append(case)
    return cleaned, warnings


def _fill_missing_coverage(
    client: genai.Client,
    plan_module,
    cases: list[TestCase],
    rules_by_id: dict,
    scenarios_by_id: dict,
    plan: TechniquePlan,
    schema: dict,
    model: str,
) -> tuple[list[TestCase], str | None]:
    """Gửi thêm 1 request nhỏ cho scenario/rule mà model đã bỏ sót ở module này.

    Module lớn (10+ scenario) thường bị model gộp bớt case để trả lời gọn, nên sau
    lượt sinh chính cần đối chiếu truy vết và bù đúng phần còn thiếu.
    """
    covered_scenarios = {sid for case in cases for sid in case.trace_scenario_ids}
    covered_rules = {rid for case in cases for rid in case.trace_rule_ids}
    missing_scenarios = [
        sid for sid in plan_module.scenario_ids if sid not in covered_scenarios
    ]
    missing_rules = [rid for rid in plan_module.rule_ids if rid not in covered_rules]
    if not missing_scenarios and not missing_rules:
        return cases, None
    if len(missing_scenarios) + len(missing_rules) > MAX_FILL_SCENARIOS:
        return cases, None

    needed_rules = set(missing_rules)
    for scenario_id in missing_scenarios:
        needed_rules.update(scenarios_by_id.get(scenario_id, {}).get('rule_ids') or [])
    fill_rules = [rid for rid in plan_module.rule_ids if rid in needed_rules]
    fill_module = plan_module.model_copy(
        update={'scenario_ids': missing_scenarios, 'rule_ids': fill_rules}
    )
    fill_payload = _payload(
        {
            'rules': [rules_by_id[i] for i in fill_rules if i in rules_by_id],
            'scenarios': [
                scenarios_by_id[i] for i in missing_scenarios if i in scenarios_by_id
            ],
        },
        plan.model_copy(update={'modules': [fill_module], 'white_box': []}),
        mode='FILL',
    )
    try:
        suite = _generate_with_retry(client, fill_payload, schema, model)
    except (RuntimeError, ValueError) as exc:
        return cases, (
            f'{plan_module.module}: chưa bù được {len(missing_scenarios)} scenario / '
            f'{len(missing_rules)} rule thiếu — {exc}'
        )
    added = _with_module(suite.test_cases, plan_module.module, fill_module)
    return cases + added, None


def generate_test_cases_per_module(
    analysis_data: dict,
    plan: TechniquePlan,
    key: str,
    model: str,
    white_box_sources: dict[str, str] | None = None,
    delay: float | None = None,
    progress: Callable[[str], None] | None = None,
    cache_dir: Path | None = None,
) -> tuple[TestSuite, list[str]]:
    """Sinh test case lần lượt cho từng module/hàm theo technique plan.

    Mỗi request chỉ chứa rule/scenario của đúng một module, nên model không bị
    "ngợp" input lớn và buộc phải phủ hết scenario của module đó. Lỗi ở một module
    được ghi thành cảnh báo và bỏ qua; lỗi quota/429 thì dừng ngay vì ảnh hưởng toàn bộ.
    """
    if plan is None:
        raise ValueError('Cần technique_plan để sinh test case theo module.')

    analysis = analysis_data.get('analysis', analysis_data) or {}
    rules_by_id = {str(r.get('rule_id')): r for r in analysis.get('rules') or []}
    scenarios_by_id = {
        str(s.get('scenario_id')): s for s in analysis.get('scenarios') or []
    }
    schema = gemini_schema(TestSuite)
    pause = MODULE_DELAY_SECONDS if delay is None else max(0.0, delay)
    results: list[tuple[str, list[TestCase]]] = []
    warnings: list[str] = []
    cached_units = 0
    stopped_early = False

    with _client(key) as client:
        units: list[tuple[str, dict, object | None]] = []
        for module in plan.modules:
            units.append((
                module.module,
                _payload(
                    {
                        'rules': [rules_by_id[i] for i in module.rule_ids if i in rules_by_id],
                        'scenarios': [
                            scenarios_by_id[i]
                            for i in module.scenario_ids
                            if i in scenarios_by_id
                        ],
                    },
                    plan.model_copy(update={'modules': [module], 'white_box': []}),
                    mode='MODULE',
                ),
                module,
            ))

        for unit in plan.white_box:
            source = (white_box_sources or {}).get(unit.module)
            if not source:
                continue
            units.append((
                unit.module,
                _payload(
                    {'rules': [], 'scenarios': []},
                    plan.model_copy(update={'modules': [], 'white_box': [unit]}),
                    mode='MODULE',
                    white_box_sources={unit.module: source},
                ),
                None,
            ))

        for index, (unit_name, unit_payload, plan_module) in enumerate(units, start=1):
            cached = _read_cache(cache_dir, unit_name, unit_payload)
            if cached is not None:
                cases = dedupe_coverage_tags(_with_module(cached, unit_name, plan_module))
                results.append((unit_name, cases))
                cached_units += 1
                if progress:
                    progress(f'{unit_name}: {len(cases)} case (cache, {index}/{len(units)})')
                continue

            try:
                suite = _generate_with_retry(client, unit_payload, schema, model)
            except (RuntimeError, ValueError) as exc:
                message = str(exc)
                if 'Gemini 429' in message or 'quota Free Tier' in message:
                    warnings.append(
                        f'Dừng sớm tại {unit_name}: {message} Các module đã xong được lưu '
                        'cache — chạy lại sau khi quota hồi sẽ tiếp tục từ chỗ dừng.'
                    )
                    stopped_early = True
                    break
                warnings.append(f'{unit_name}: không sinh được test case — {message}')
                continue

            cases = _with_module(suite.test_cases, unit_name, plan_module)
            if plan_module is not None:
                cases, fill_warning = _fill_missing_coverage(
                    client,
                    plan_module,
                    cases,
                    rules_by_id,
                    scenarios_by_id,
                    plan,
                    schema,
                    model,
                )
                if fill_warning:
                    warnings.append(fill_warning)
            cases = dedupe_coverage_tags(cases)
            _write_cache(cache_dir, unit_name, unit_payload, cases)
            results.append((unit_name, cases))
            if progress:
                progress(f'{unit_name}: {len(cases)} case ({index}/{len(units)})')
            if pause and index < len(units):
                time.sleep(pause)

    merged, trace_warnings = drop_unknown_traces(merge_module_cases(results), analysis)
    warnings.extend(trace_warnings)
    summary = (
        f'Sinh theo {len(results)}/{len(units)} module/hàm được chọn: '
        f'{len(merged)} test case.'
    )
    if cached_units:
        summary += f' (dùng lại cache: {cached_units}).'
    if stopped_early:
        summary += ' Dừng sớm do hết quota — chạy lại để tiếp tục.'
    return TestSuite(summary=summary, test_cases=merged), warnings
