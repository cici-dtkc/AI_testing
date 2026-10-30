"""Kiểm tra chéo bộ test case với analysis và technique plan.

Các kiểm tra bám theo nguyên tắc trong 2 tài liệu:
- Single-fault assumption: một test case chỉ chứa tối đa 1 lớp/biên không hợp lệ.
- Mỗi hạng mục bao phủ (equivalence class, boundary, rule, transition, flow,
  basis path...) chỉ cần một test case đại diện.
- Mỗi module phải dùng đúng kỹ thuật chính đã chọn; số case không thấp hơn mức
  tối thiểu mà công thức của kỹ thuật yêu cầu.
"""

from __future__ import annotations

import re
from collections import Counter, defaultdict

from .models import TestSuite, TechniquePlan
from .techniques import (
    COVERAGE_TAG_RE,
    TECHNIQUE_LIBRARY,
    is_invalid_coverage_tag,
    module_map,
)

# Quy ước bài tập 8: Chính = luồng chính, E1/E2… = luồng ngoại lệ, Ẩn = luồng bổ sung.
FLOW_RE = re.compile(r'^(Chính|Ẩn|E[1-9]\d?)$')
MAX_STEPS = 6
MAX_STEP_WORDS = 60
MAX_EXPECTED_WORDS = 30

WHITE_BOX_TECHNIQUES = {
    'BASIS_PATH',
    'STATEMENT_COVERAGE',
    'BRANCH_COVERAGE',
    'CONDITION_COVERAGE',
    'DATA_FLOW',
}


def validate_test_cases(
    test_suite: TestSuite,
    analysis_data: dict,
    plan: TechniquePlan | None = None,
) -> list[str]:
    warnings: list[str] = []
    analysis = (analysis_data or {}).get('analysis', analysis_data) or {}
    scenarios = {str(s.get('scenario_id', '')) for s in analysis.get('scenarios') or []}
    rules = {str(r.get('rule_id', '')) for r in analysis.get('rules') or []}

    plans = module_map(plan)
    white_box_modules = {item.module: item for item in (plan.white_box if plan else [])}
    rule_to_module = {
        rule_id: module.module
        for module in (plan.modules if plan else [])
        for rule_id in module.rule_ids
    }
    scenario_to_module = {
        scenario_id: module.module
        for module in (plan.modules if plan else [])
        for scenario_id in module.scenario_ids
    }

    seen_ids: set[str] = set()
    coverage_seen: dict[tuple[str, str], str] = {}
    module_cases: Counter[str] = Counter()
    module_techniques: defaultdict[str, set[str]] = defaultdict(set)
    module_flows: defaultdict[str, set[str]] = defaultdict(set)
    module_has_positive: set[str] = set()
    white_box_cases: Counter[str] = Counter()
    covered_scenarios: set[str] = set()
    covered_rules: set[str] = set()

    for tc in test_suite.test_cases:
        prefix = tc.test_case_id
        if tc.test_case_id in seen_ids:
            warnings.append(f'Trùng lặp Test Case ID: {tc.test_case_id}')
        seen_ids.add(tc.test_case_id)

        # --- Truy vết -------------------------------------------------------
        unknown_scenarios = [x for x in tc.trace_scenario_ids if x not in scenarios]
        unknown_rules = [x for x in tc.trace_rule_ids if x not in rules]
        if unknown_scenarios:
            warnings.append(
                f'{prefix}: truy vết scenario không tồn tại trong analysis: '
                + ', '.join(unknown_scenarios)
            )
        if unknown_rules:
            warnings.append(
                f'{prefix}: truy vết rule không tồn tại trong analysis: '
                + ', '.join(unknown_rules)
            )
        covered_scenarios.update(x for x in tc.trace_scenario_ids if x in scenarios)
        covered_rules.update(x for x in tc.trace_rule_ids if x in rules)

        # --- Module ---------------------------------------------------------
        module = tc.module
        if not module and tc.trace_rule_ids:
            module = rule_to_module.get(tc.trace_rule_ids[0])
        if not module and tc.trace_scenario_ids:
            module = scenario_to_module.get(tc.trace_scenario_ids[0])
        if module:
            module_cases[module] += 1
            module_techniques[module].add(tc.technique)
            if tc.type == 'POSITIVE':
                module_has_positive.add(module)

        # --- Kỹ thuật: hộp trắng hay hộp đen --------------------------------
        if tc.technique in WHITE_BOX_TECHNIQUES:
            if not white_box_modules:
                warnings.append(
                    f'{prefix}: dùng kỹ thuật hộp trắng {tc.technique} nhưng kế hoạch không có '
                    'white_box (chưa truyền mã nguồn).'
                )
            elif module not in white_box_modules:
                warnings.append(
                    f'{prefix}: module "{module}" không khớp white_box module nào trong kế hoạch.'
                )
            else:
                white_box_cases[module] += 1
        else:
            if not tc.trace_rule_ids and not tc.trace_scenario_ids:
                warnings.append(
                    f'{prefix}: test case hộp đen thiếu truy vết scenario/rule.'
                )
            if module in plans:
                allowed = {choice.technique for choice in plans[module].techniques}
                if tc.technique not in allowed:
                    allowed_text = ', '.join(sorted(allowed))
                    warnings.append(
                        f'{prefix}: kỹ thuật {tc.technique} không nằm trong kế hoạch module '
                        f'{module} (được chọn: {allowed_text}).'
                    )

        # --- Luồng theo quy ước bài 8 + độ ngắn gọn ----------------------------
        flow = (tc.flow or '').strip()
        if not flow:
            warnings.append(f'{prefix}: thiếu cột Luồng (Chính / E1, E2… / Ẩn).')
        elif not FLOW_RE.match(flow):
            warnings.append(
                f'{prefix}: Luồng "{flow}" không hợp lệ — chỉ dùng Chính, E1, E2… '
                'hoặc Ẩn.'
            )
        elif module:
            module_flows[module].add(flow)
        step_words = sum(len(step.split()) for step in tc.steps)
        if len(tc.steps) > MAX_STEPS or step_words > MAX_STEP_WORDS:
            warnings.append(
                f'{prefix}: các bước quá dài ({len(tc.steps)} bước / {step_words} từ) — '
                'rút gọn còn 3–5 bước, mỗi bước ≤ 10 từ như bài 8.'
            )
        expected_words = len(tc.expected_result.split())
        if expected_words > MAX_EXPECTED_WORDS:
            warnings.append(
                f'{prefix}: kết quả mong đợi quá dài ({expected_words} từ) — nêu 1 kết quả '
                'quan sát được, ≤ 25 từ.'
            )

        # --- Coverage tag ---------------------------------------------------
        invalid_tags = [
            tag for tag in tc.coverage_items if is_invalid_coverage_tag(str(tag))
        ]
        if len(invalid_tags) > 1:
            warnings.append(
                f'{prefix}: chứa {len(invalid_tags)} hạng mục không hợp lệ '
                f'({", ".join(invalid_tags)}) — vi phạm single-fault assumption, '
                'hãy tách thành nhiều test case.'
            )
        for tag in tc.coverage_items:
            normalized = str(tag).strip().upper()
            if not COVERAGE_TAG_RE.match(normalized):
                warnings.append(
                    f'{prefix}: coverage item "{tag}" sai quy ước tag '
                    '(V1/X1/B1/D1/R1/T1/F1/P1/S1/DF1).'
                )
                continue
            key = (module or '', normalized)
            if key in coverage_seen:
                warnings.append(
                    f'{prefix}: trùng coverage item {normalized} với '
                    f'{coverage_seen[key]} trong module {module}.'
                )
            else:
                coverage_seen[key] = tc.test_case_id

    # --- Kiểm tra theo module ----------------------------------------------
    if plan:
        for module in plan.modules:
            count = module_cases.get(module.module, 0)
            primary = module.techniques[0]
            if count == 0:
                warnings.append(f'Module {module.module}: chưa có test case nào.')
                continue
            if primary.technique not in module_techniques[module.module]:
                warnings.append(
                    f'Module {module.module}: kỹ thuật chính '
                    f'{TECHNIQUE_LIBRARY[primary.technique].title} chưa được dùng trong '
                    f'{count} test case hiện có.'
                )
            if module.module in module_has_positive and 'Chính' not in module_flows[module.module]:
                warnings.append(
                    f'Module {module.module}: có case POSITIVE nhưng không case nào ở '
                    'luồng Chính.'
                )
            if primary.technique == 'BOUNDARY_VALUE_ANALYSIS' and module.numeric_ranges:
                minimum = 2 * len(module.numeric_ranges)
                if count < minimum:
                    warnings.append(
                        f'Module {module.module}: BVA cần ít nhất {minimum} test case cho '
                        f'{len(module.numeric_ranges)} ràng buộc biên, hiện có {count}.'
                    )

        for white_box in plan.white_box:
            count = white_box_cases.get(white_box.module, 0)
            if count < white_box.basis_paths:
                warnings.append(
                    f'Hàm {white_box.module}: cần tối thiểu V(G) = {white_box.basis_paths} '
                    f'test case đường cơ sở, hiện có {count}.'
                )

    # --- Độ phủ scenario/rule ----------------------------------------------
    uncovered_scenarios = sorted(scenarios - covered_scenarios)
    if uncovered_scenarios:
        shown = ', '.join(uncovered_scenarios[:10])
        more = '...' if len(uncovered_scenarios) > 10 else ''
        warnings.append(
            f'{len(uncovered_scenarios)} scenario chưa được test case nào truy vết: {shown}{more}'
        )

    uncovered_rules = sorted(rules - covered_rules)
    if uncovered_rules and len(uncovered_rules) > 5:
        warnings.append(
            f'{len(uncovered_rules)} rule chưa được test case nào truy vết (có thể đã gộp '
            'vào scenario tương ứng).'
        )

    return warnings
