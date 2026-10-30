"""Test cho engine chọn kỹ thuật, phân tích hộp trắng và validation."""

from typing import get_args

from generator.coverage import analyze_source_text
from generator.models import TechniqueName
from generator.models import TestCase as Case
from generator.models import TestSuite as Suite
from generator.techniques import (
    TECHNIQUE_LIBRARY,
    boundary_points,
    build_technique_plan,
    extract_ranges,
    is_invalid_coverage_tag,
)
from generator.validation import validate_test_cases


def analysis_of(rules: list[dict], scenarios: list[dict] | None = None) -> dict:
    return {'rules': rules, 'scenarios': scenarios or []}


def rule(rule_id: str, statement: str, *, category: str = 'BUSINESS_RULE', conditions=None) -> dict:
    return {
        'rule_id': rule_id,
        'statement': statement,
        'category': category,
        'conditions': conditions or ['Điều kiện nghiệp vụ'],
        'preconditions': [],
        'outcome': f'Kết quả {rule_id}',
    }


def scenario(scenario_id: str, rule_id: str, kind: str = 'POSITIVE', title: str = 'Kịch bản') -> dict:
    return {
        'scenario_id': scenario_id,
        'title': title,
        'type': kind,
        'rule_ids': [rule_id],
        'given': 'Trạng thái ban đầu',
        'when': 'Người dùng thực hiện thao tác',
        'then': 'Hệ thống phản hồi',
    }


def primary(plan):
    return plan.modules[0].techniques[0]


def module_of(plan, rule_id: str):
    for module in plan.modules:
        if rule_id in module.rule_ids:
            return module
    raise AssertionError(f'Không tìm thấy rule {rule_id} trong plan')


# --- Thư viện ---------------------------------------------------------------


def test_library_covers_every_technique_name():
    assert set(TECHNIQUE_LIBRARY) == set(get_args(TechniqueName))


def test_invalid_tag_only_matches_class_and_boundary():
    assert is_invalid_coverage_tag('X1')
    assert is_invalid_coverage_tag('D12')
    assert not is_invalid_coverage_tag('V1')
    assert not is_invalid_coverage_tag('B2')
    assert not is_invalid_coverage_tag('DF3')  # data-flow của hộp trắng


# --- Nhận diện biên ---------------------------------------------------------


def test_extract_ranges_handles_vietnamese_and_length_units():
    ranges = extract_ranges('Username dài từ 6 đến 12 ký tự.', 'R1')
    assert len(ranges) == 1
    assert (ranges[0].min, ranges[0].max) == (6, 12)
    assert ranges[0].unit == 'ký tự'


def test_two_value_boundary_points_from_document():
    numeric_range = extract_ranges('Mật khẩu dài từ 8 đến 16 ký tự.', 'R1')[0]
    assert boundary_points(numeric_range, 'TWO_VALUE') == [7, 8, 16, 17]
    assert boundary_points(numeric_range, 'THREE_VALUE') == [7, 8, 9, 15, 16, 17]
    assert boundary_points(numeric_range, 'ROBUST_WORST_CASE') == [7, 8, 9, 12, 15, 16, 17]


# --- Thang chọn kỹ thuật ----------------------------------------------------


def test_range_only_module_uses_bva_primary_and_formula():
    plan = build_technique_plan(
        analysis_of([rule('R1', 'Tìm kiếm chỉ hiển thị tối đa 12 truyện mỗi trang.')])
    )
    choice = primary(plan)
    assert choice.technique == 'BOUNDARY_VALUE_ANALYSIS'
    assert choice.variant == 'TWO_VALUE'
    assert choice.estimated_cases == 5  # 4n + 1 với n = 1
    assert plan.modules[0].numeric_ranges[0].boundary_points == [12, 13]


def test_multi_variable_numeric_rule_escalates_to_worst_case():
    rules = [
        rule('R1', 'Tìm kiếm theo khoảng giá từ 10 đến 100 và tối đa 20 kết quả mỗi trang.')
    ]
    choice = primary(build_technique_plan(analysis_of(rules)))
    assert choice.technique == 'BOUNDARY_VALUE_ANALYSIS'
    assert choice.variant == 'WORST_CASE'
    assert choice.estimated_cases == 25  # 5^2


def test_invalid_handling_with_two_variables_uses_robust_worst_case():
    rules = [
        rule('R1', 'Tìm kiếm theo khoảng giá từ 10 đến 100 và tối đa 20 kết quả mỗi trang.')
    ]
    scenarios = [scenario('S1', 'R1', 'NEGATIVE'), scenario('S2', 'R1', 'NEGATIVE')]
    choice = primary(build_technique_plan(analysis_of(rules, scenarios)))
    assert choice.variant == 'ROBUST_WORST_CASE'
    assert choice.estimated_cases == 49  # 7^2


def test_conditional_business_rules_use_decision_table():
    rules = [
        rule('R1', 'Tìm kiếm theo tên hiển thị kết quả nhóm A.', conditions=['Có từ khóa', 'Có bộ lọc']),
        rule('R2', 'Tìm kiếm theo tác giả hiển thị kết quả nhóm B.', conditions=['Có từ khóa', 'Không lọc']),
    ]
    plan = build_technique_plan(analysis_of(rules))
    assert primary(plan).technique == 'DECISION_TABLE'


def test_state_lifecycle_uses_state_transition():
    rules = [
        rule('R1', 'Chỉ được chuyển sang trạng thái đơn hàng Đang giao khi đơn đang ở trạng thái đơn hàng Chờ xác nhận.'),
        rule('R2', 'Không được chuyển sang trạng thái đơn hàng Đã hủy khi đơn đã giao.'),
        rule('R3', 'Admin cập nhật trạng thái đơn hàng sang Hoàn thành.'),
    ]
    plan = build_technique_plan(analysis_of(rules, [scenario('S1', 'R1')]))
    assert module_of(plan, 'R2').techniques[0].technique == 'STATE_TRANSITION'


def test_authorization_scenario_uses_decision_table_matrix():
    rules = [
        rule('R1', 'Phân quyền Admin được thêm truyện vào hệ thống.'),
        rule('R2', 'Phân quyền Admin được xóa truyện khỏi hệ thống.'),
    ]
    plan = build_technique_plan(
        analysis_of(rules, [scenario('S1', 'R2', 'AUTHORIZATION')])
    )
    assert module_of(plan, 'R1').techniques[0].technique == 'DECISION_TABLE'


def test_multi_step_flow_uses_use_case():
    rules = [
        rule('R1', 'Khách hàng đặt hàng và thanh toán trong một bước.'),
        rule('R2', 'Khách hàng đặt hàng lại sau khi thanh toán thất bại.'),
    ]
    scenarios = [scenario('S1', 'R1'), scenario('S2', 'R2')]
    plan = build_technique_plan(analysis_of(rules, scenarios))
    module = module_of(plan, 'R1')
    assert module.techniques[0].technique == 'USE_CASE'
    assert any(choice.role == 'SUPPORTING' for choice in module.techniques)


def test_non_functional_module_uses_metric_technique():
    rules = [
        rule('R1', 'Thời gian phản hồi tìm kiếm không vượt quá 3 giây.', category='NON_FUNCTIONAL'),
        rule('R2', 'Mật khẩu phải được mã hóa hash.', category='NON_FUNCTIONAL'),
    ]
    plan = build_technique_plan(analysis_of(rules))
    assert primary(plan).technique == 'NON_FUNCTIONAL_METRIC'


def test_negative_scenario_makes_ep_robust():
    rules = [rule('R1', 'Hệ thống cập nhật thông tin cá nhân của khách hàng.')]
    plan = build_technique_plan(
        analysis_of(rules, [scenario('S1', 'R1', 'NEGATIVE')])
    )
    choice = primary(plan)
    assert choice.technique == 'EQUIVALENCE_PARTITIONING'
    assert choice.variant == 'WEAK_ROBUST'


def test_plain_module_falls_back_to_weak_normal_ep():
    rules = [rule('R1', 'Điểm thưởng được cộng vào tài khoản khách hàng.')]
    plan = build_technique_plan(analysis_of(rules))
    choice = primary(plan)
    assert choice.technique == 'EQUIVALENCE_PARTITIONING'
    assert choice.variant == 'WEAK_NORMAL'


def test_plan_links_rule_and_scenario_ids():
    rules = [rule('R1', 'Tìm kiếm truyện theo tên.')]
    plan = build_technique_plan(analysis_of(rules, [scenario('S1', 'R1')]))
    module = plan.modules[0]
    assert module.rule_ids == ['R1']
    assert module.scenario_ids == ['S1']


# --- Hộp trắng --------------------------------------------------------------

SOURCE = '''
def classify(a, b):
    total = 0
    if a > 0 and b > 0:
        total = a + b
    elif a == 0:
        total = b
    for i in range(3):
        total += i
    return total
'''


def test_white_box_computes_mccabe_and_coverage_targets():
    unit = analyze_source_text(SOURCE, 'demo.py')[0]
    assert unit.decisions == 3          # if, elif, for
    assert unit.conditions == 4         # (a>0 and b>0) + a==0 + vòng for
    assert unit.cyclomatic == 4         # V(G) = P + 1
    assert unit.basis_paths == 4
    assert unit.data_flow_pairs > 0
    assert unit.techniques[0].technique == 'BASIS_PATH'
    assert unit.techniques[0].estimated_cases == 4
    levels = {target.level: target.items for target in unit.coverage_targets}
    assert levels['Mức 2 — Bao phủ nhánh/quyết định'] == 6


def test_white_box_without_branches_needs_one_case():
    unit = analyze_source_text('def ping():\n    return 1\n', 'demo.py')[0]
    assert unit.cyclomatic == 1
    assert any('không có nhánh' in warning for warning in unit.warnings)


# --- Validation -------------------------------------------------------------


def make_case(case_id: str, **overrides) -> Case:
    data = {
        'test_case_id': case_id,
        'module': 'TimKiem',
        'title': 'Tìm kiếm truyện',
        'type': 'POSITIVE',
        'priority': 'Cao',
        'technique': 'EQUIVALENCE_PARTITIONING',
        'technique_variant': 'WEAK_NORMAL',
        'flow': 'Chính',
        'coverage_items': ['V1'],
        'preconditions': ['Hệ thống hoạt động'],
        'given': 'Người dùng ở trang tìm kiếm',
        'steps': ['Nhập từ khóa'],
        'expected_result': 'Hiển thị kết quả',
        'trace_scenario_ids': ['S1'],
        'trace_rule_ids': ['R1'],
    }
    data.update(overrides)
    return Case(**data)


def sample_plan():
    return build_technique_plan(
        analysis_of(
            [rule('R1', 'Tìm kiếm truyện theo tên.')],
            [scenario('S1', 'R1')],
        )
    )


def test_validation_accepts_planned_case():
    suite = Suite(summary='ok', test_cases=[make_case('TC-01')])
    assert validate_test_cases(suite, analysis_of([rule('R1', 'Tìm kiếm truyện theo tên.')], [scenario('S1', 'R1')]), sample_plan()) == []


def test_validation_flags_single_fault_violation():
    suite = Suite(summary='bad', test_cases=[make_case('TC-01', coverage_items=['X1', 'D1'])])
    warnings = validate_test_cases(suite, {'rules': [rule('R1', 'Tìm kiếm truyện theo tên.')], 'scenarios': [scenario('S1', 'R1')]}, sample_plan())
    assert any('single-fault' in warning for warning in warnings)


def test_validation_flags_unknown_trace_ids():
    suite = Suite(summary='bad', test_cases=[make_case('TC-01', trace_scenario_ids=['S404'], trace_rule_ids=['R404'])])
    warnings = validate_test_cases(suite, {'rules': [rule('R1', 'Tìm kiếm truyện theo tên.')], 'scenarios': [scenario('S1', 'R1')]}, sample_plan())
    assert any('không tồn tại' in warning for warning in warnings)


def test_validation_flags_technique_outside_plan():
    suite = Suite(summary='bad', test_cases=[make_case('TC-01', technique='STATE_TRANSITION')])
    warnings = validate_test_cases(suite, {'rules': [rule('R1', 'Tìm kiếm truyện theo tên.')], 'scenarios': [scenario('S1', 'R1')]}, sample_plan())
    assert any('không nằm trong kế hoạch' in warning for warning in warnings)


def test_validation_flags_duplicate_coverage_item():
    cases = [make_case('TC-01'), make_case('TC-02')]
    warnings = validate_test_cases(Suite(summary='dup', test_cases=cases), {'rules': [rule('R1', 'Tìm kiếm truyện theo tên.')], 'scenarios': [scenario('S1', 'R1')]}, sample_plan())
    assert any('trùng coverage item' in warning for warning in warnings)


def test_clean_coverage_items_drops_scenario_ids_but_never_empties():
    from generator.gemini import clean_coverage_items

    assert clean_coverage_items(['T1', 'C003-SCN-M02-003']) == ['T1']
    assert clean_coverage_items(['v1', 'b1']) == ['V1', 'B1']
    # Nếu toàn bộ đều sai quy ước thì giữ nguyên để validation báo lỗi.
    assert clean_coverage_items(['C003-SCN-M02-003']) == ['C003-SCN-M02-003']


def test_module_cache_roundtrip_and_fingerprint(tmp_path):
    from generator.gemini import _read_cache, _write_cache

    payload = {
        'analysis': {'rules': [{'rule_id': 'R1'}]},
        'technique_plan': {'modules': []},
        'mode': 'MODULE',
    }
    cache_dir = tmp_path / 'cache'
    assert _read_cache(cache_dir, 'TimKiem', payload) is None
    _write_cache(cache_dir, 'TimKiem', payload, [make_case('TC-01')])
    loaded = _read_cache(cache_dir, 'TimKiem', payload)
    assert loaded and loaded[0].module == 'TimKiem'
    # Plan/input đổi thì cache cũ không được dùng lại.
    assert _read_cache(cache_dir, 'TimKiem', {**payload, 'mode': 'OTHER'}) is None


def test_merge_module_cases_renumbers_ids_and_keeps_module():
    from generator.gemini import merge_module_cases

    first = make_case('TC-01', module='TimKiem')
    second = make_case('TC-01', module='GiamGia')
    merged = merge_module_cases([('TimKiem', [first]), ('GiamGia', [second])])
    assert [case.test_case_id for case in merged] == ['TC-01', 'TC-02']
    assert [case.module for case in merged] == ['TimKiem', 'GiamGia']


def test_validation_requires_flow_for_every_case():
    missing = make_case('TC-01', technique='USE_CASE', coverage_items=['F1'], flow='')
    analysis = {'rules': [rule('R1', 'Tìm kiếm truyện theo tên.')], 'scenarios': [scenario('S1', 'R1')]}
    warnings = validate_test_cases(
        Suite(summary='flow', test_cases=[missing]),
        analysis,
        sample_plan(),
    )
    assert any('thiếu cột Luồng' in warning for warning in warnings)

    valid = missing.model_copy(update={'flow': 'Chính'})
    warnings = validate_test_cases(
        Suite(summary='flow', test_cases=[valid]),
        analysis,
        sample_plan(),
    )
    assert not any('Luồng' in warning for warning in warnings)


def test_validation_accepts_exception_and_hidden_flows():
    cases = [
        make_case('TC-01', flow='Chính'),
        make_case('TC-02', flow='E2', type='NEGATIVE', coverage_items=['X1']),
        make_case('TC-03', flow='Ẩn', type='NON_FUNCTIONAL',
                  technique='NON_FUNCTIONAL_METRIC', coverage_items=['V2']),
    ]
    warnings = validate_test_cases(
        Suite(summary='flows', test_cases=cases),
        {'rules': [rule('R1', 'Tìm kiếm truyện theo tên.')], 'scenarios': [scenario('S1', 'R1')]},
        sample_plan(),
    )
    assert not any('Luồng' in warning for warning in warnings)

    old_style = cases[1].model_copy(update={'flow': 'Ngoại lệ'})
    warnings = validate_test_cases(
        Suite(summary='flows', test_cases=[cases[0], old_style]),
        {'rules': [rule('R1', 'Tìm kiếm truyện theo tên.')], 'scenarios': [scenario('S1', 'R1')]},
        sample_plan(),
    )
    assert any('không hợp lệ' in warning for warning in warnings)


def test_normalize_flow_accepts_e_number_and_hidden():
    from generator.gemini import normalize_flow

    assert normalize_flow('e2') == 'E2'
    assert normalize_flow('E10') == 'E10'
    assert normalize_flow('Đầy đủ') == 'Chính'
    assert normalize_flow('ẩn') == 'Ẩn'
    assert normalize_flow('Ngoại lệ') == ''


def test_validation_flags_verbose_steps_and_expected_result():
    verbose = make_case(
        'TC-01',
        steps=['Bước một rất dài ' * 15, 'Bước hai rất dài ' * 15],
        expected_result='Kết quả mong đợi ' * 20,
    )
    warnings = validate_test_cases(
        Suite(summary='verbose', test_cases=[verbose]),
        {'rules': [rule('R1', 'Tìm kiếm truyện theo tên.')], 'scenarios': [scenario('S1', 'R1')]},
        sample_plan(),
    )
    assert any('các bước quá dài' in warning for warning in warnings)
    assert any('kết quả mong đợi quá dài' in warning for warning in warnings)


def test_report_drops_module_column_and_adds_flow(tmp_path):
    from generator.report import write_test_report

    case = make_case(
        'TC-01',
        technique='USE_CASE',
        technique_variant='',
        flow='Chính',
        coverage_items=['F1'],
    )
    payload = {
        'provider': 'TEST',
        'model': 'none',
        'test_suite': Suite(summary='demo', test_cases=[case]).model_dump(),
        'technique_plan': {'summary': 'demo', 'modules': [], 'white_box': []},
        'validation_warnings': [],
    }
    write_test_report(payload, tmp_path)
    content = (tmp_path / 'test_cases_report.html').read_text(encoding='utf-8')
    assert '<th>Module</th>' not in content
    assert '<th>Luồng</th>' in content
    assert '<td class="flow">Chính</td>' in content


def test_with_module_normalizes_flow_and_fills_known_trace():
    from generator.gemini import _with_module

    plan = sample_plan()
    module_plan = plan.modules[0]
    cases = [
        make_case('TC-01', technique='USE_CASE', flow='Đầy đủ', coverage_items=['F1'],
                  trace_scenario_ids=[], trace_rule_ids=[]),
        make_case('TC-02', technique='USE_CASE', flow='Chính', coverage_items=['F2']),
    ]
    fixed = _with_module(cases, module_plan.module, module_plan)
    assert fixed[0].flow == 'Chính'  # đồng nghĩa "Đầy đủ" -> "Chính"
    assert fixed[0].trace_scenario_ids == module_plan.scenario_ids
    assert fixed[1].trace_scenario_ids == ['S1']  # case đã có truy vết: giữ nguyên


def test_dedupe_coverage_tags_numbers_repeated_items():
    from generator.gemini import dedupe_coverage_tags

    cases = [
        make_case('TC-01', coverage_items=['V1', 'B1']),
        make_case('TC-02', coverage_items=['V1', 'B1']),
    ]
    fixed = dedupe_coverage_tags(cases)
    assert fixed[0].coverage_items == ['V1', 'B1']
    assert fixed[1].coverage_items == ['V2', 'B2']


def test_clean_coverage_items_numbers_placeholder_tags():
    from generator.gemini import clean_coverage_items

    counters: dict[str, int] = {}
    assert clean_coverage_items(['Bn'], counters) == ['B1']
    assert clean_coverage_items(['Bn'], counters) == ['B2']
    assert clean_coverage_items(['C012-SC-UC-02', 'V1'], {}) == ['V1']


def test_drop_unknown_traces_removes_invented_ids():
    from generator.gemini import drop_unknown_traces

    cases = [make_case('TC-01', trace_scenario_ids=['C012-SC-UC-02'], trace_rule_ids=['R1'])]
    analysis = {'rules': [rule('R1', 'Tìm kiếm truyện theo tên.')], 'scenarios': [scenario('S1', 'R1')]}
    cleaned, warnings = drop_unknown_traces(cases, analysis)
    assert cleaned[0].trace_scenario_ids == []
    assert cleaned[0].trace_rule_ids == ['R1']
    assert any('C012-SC-UC-02' in warning for warning in warnings)


def test_validation_flags_uncovered_scenario():
    suite = Suite(summary='missing', test_cases=[make_case('TC-01')])
    warnings = validate_test_cases(suite, {'rules': [rule('R1', 'Tìm kiếm truyện theo tên.')], 'scenarios': [scenario('S1', 'R1'), scenario('S2', 'R1')]}, sample_plan())
    assert any('scenario chưa được test case nào truy vết' in warning for warning in warnings)
