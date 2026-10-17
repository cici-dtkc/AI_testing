from .models import TestSuite

def validate_test_cases(test_suite: TestSuite, analysis_data: dict) -> list[str]:
    warnings = []
    seen_tc_ids = set()
    for tc in test_suite.test_cases:
        if tc.test_case_id in seen_tc_ids:
            warnings.append(f"Trùng lặp Test Case ID: {tc.test_case_id}")
        seen_tc_ids.add(tc.test_case_id)
        
    return warnings