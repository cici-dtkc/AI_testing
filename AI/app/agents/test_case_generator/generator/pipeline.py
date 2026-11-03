import json
from pathlib import Path
from collections.abc import Callable

from .coverage import analyze_path
from .gemini import generate_test_cases_per_module
from .models import TestSuite, TechniquePlan
from .techniques import build_technique_plan
from .validation import validate_test_cases

# Giới hạn mã nguồn đưa vào prompt để không phình input của Gemini.
MAX_WHITE_BOX_SOURCE_CHARS = 12000


def _bounded_sources(
    sources: dict[str, str], limit: int = MAX_WHITE_BOX_SOURCE_CHARS
) -> tuple[dict[str, str], list[str]]:
    selected: dict[str, str] = {}
    warnings: list[str] = []
    used = 0
    for module, code in sources.items():
        if used + len(code) > limit:
            warnings.append(
                f'Bỏ qua mã nguồn của {module}: vượt giới hạn {limit} ký tự cho prompt.'
            )
            continue
        selected[module] = code
        used += len(code)
    return selected, warnings


def apply_scope(analysis_data: dict) -> tuple[dict, list[str]]:
    """Thu hẹp analysis theo kết quả đối chiếu scope của Requirement Analyzer.

    Chỉ giữ rule/scenario có quyết định IN_SCOPE, cộng rule phụ thuộc của
    scenario được giữ (giống analyzer/scope.py::reconcile); issue giữ theo
    issue_audit. Input không có scope_comparison thì giữ nguyên.
    """
    scope = analysis_data.get('scope_comparison')
    analysis = analysis_data.get('analysis') or {}
    if not scope:
        return analysis_data, []

    decisions = scope.get('decisions') or []
    status_of = {
        (d.get('item_type'), str(d.get('item_id'))): d.get('status')
        for d in decisions
    }
    kept_scenario_ids = {
        iid for (typ, iid), st in status_of.items()
        if typ == 'scenario' and st == 'IN_SCOPE'
    }
    kept_rule_ids = {
        iid for (typ, iid), st in status_of.items()
        if typ == 'rule' and st == 'IN_SCOPE'
    }
    scenarios = [
        s for s in analysis.get('scenarios') or []
        if str(s.get('scenario_id')) in kept_scenario_ids
    ]
    dependencies = {
        str(rid) for s in scenarios for rid in (s.get('rule_ids') or [])
    } - kept_rule_ids
    kept_rule_ids |= dependencies
    rules = [
        r for r in analysis.get('rules') or []
        if str(r.get('rule_id')) in kept_rule_ids
    ]
    issues = list(analysis.get('issues') or [])
    issue_audit = scope.get('issue_audit')
    if issue_audit:
        retained = {
            str(a.get('issue_id')) for a in issue_audit if a.get('retained')
        }
        issues = [i for i in issues if str(i.get('issue_id')) in retained]

    filtered = dict(analysis)
    filtered.update({'rules': rules, 'scenarios': scenarios, 'issues': issues})
    data = dict(analysis_data)
    data['analysis'] = filtered

    warnings: list[str] = []
    if dependencies:
        warnings.append(
            'Rule giữ làm phụ thuộc của scenario IN_SCOPE: ' + ', '.join(sorted(dependencies))
        )
    return data, warnings


def load_analysis(analysis_path: Path) -> tuple[dict, list[str]]:
    if not analysis_path.is_file():
        raise ValueError(f'Không tìm thấy file analysis đầu vào tại: {analysis_path}')
    data = json.loads(analysis_path.read_text(encoding='utf-8'))
    return apply_scope(data)


def build_plan(
    analysis_path: Path,
    code_path: Path | None = None,
) -> tuple[TechniquePlan, list[str], dict[str, str]]:
    """Chỉ chạy bước chọn kỹ thuật (không gọi LLM) để kiểm tra/quyết định trước."""
    analysis_data, scope_warnings = load_analysis(analysis_path)
    white_box, warnings, sources = ([], [], {})
    if code_path is not None:
        white_box, warnings, sources = analyze_path(Path(code_path))
    plan = build_technique_plan(analysis_data, white_box)
    return plan, scope_warnings + warnings + plan.warnings, sources


def generate_full_test_cases(
    analysis_path: Path,
    key: str,
    model: str,
    code_path: Path | None = None,
    module_delay: float | None = None,
    progress: Callable[[str], None] | None = None,
    cache_dir: Path | None = None,
) -> tuple[TestSuite, TechniquePlan, list[str]]:
    analysis_data, scope_warnings = load_analysis(analysis_path)

    # Bước 1: engine quyết định kỹ thuật cho từng module (hộp đen + hộp trắng).
    white_box, white_box_warnings, sources = ([], [], {})
    if code_path is not None:
        white_box, white_box_warnings, sources = analyze_path(Path(code_path))
    plan = build_technique_plan(analysis_data, white_box)

    # Bước 2: LLM hiện thực hoá nghĩa vụ bao phủ — mỗi module/hàm một request nhỏ
    # để không bỏ sót scenario vì input quá lớn.
    white_box_sources, source_warnings = _bounded_sources(sources)
    test_suite, generation_warnings = generate_test_cases_per_module(
        analysis_data,
        plan,
        key,
        model,
        white_box_sources=white_box_sources,
        delay=module_delay,
        progress=progress,
        cache_dir=cache_dir,
    )

    # Bước 3: kiểm tra truy vết, coverage tag và mức phủ tối thiểu.
    warnings = (
        scope_warnings
        + white_box_warnings
        + source_warnings
        + generation_warnings
        + plan.warnings
        + validate_test_cases(test_suite, analysis_data, plan)
    )

    return test_suite, plan, warnings