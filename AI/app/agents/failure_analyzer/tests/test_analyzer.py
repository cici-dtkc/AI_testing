from __future__ import annotations

import json

import pytest

from AI.app.agents.failure_analyzer.core.analyzer import (
    FailureInputError,
    analyze_input,
    apply_gemini_assessments,
    load_input,
    write_outputs,
)
from AI.app.agents.failure_analyzer.core.run_adapter import load_run_evidence
from AI.app.agents.failure_analyzer.llm.gemini_provider import (
    GeminiAnalysisError,
    _downgrade_invalid_citations,
    _generate_with_retries,
    _validate_response,
)


def sample_input() -> dict:
    return {
        "schema_version": "1.0",
        "project_id": "project-test",
        "run_id": "run-test",
        "tests": [
            {
                "attempt_id": "attempt-1",
                "test_case_id": "TC-01",
                "test_case": {
                    "expected": {"value": "ok"},
                    "expected_review_status": "APPROVED",
                },
                "execution": {
                    "status": "FAILED",
                    "error_type": "assertion.mismatch",
                    "error_message": "Expected ok, got error",
                },
                "actual": {
                    "assertion_values": {
                        "passed": False,
                        "expected": "ok",
                        "actual": "error",
                    }
                },
                "evidence_manifest": [
                    {
                        "evidence_id": "ev-1",
                        "run_id": "run-test",
                        "attempt_id": "attempt-1",
                    }
                ],
            }
        ],
    }


def test_classifies_reviewed_assertion_mismatch_as_product():
    result = analyze_input(sample_input())
    analysis = result["analyses"][0]

    assert analysis["primary_category"] == "PRODUCT"
    assert analysis["analysis_status"] == "COMPLETED"
    assert analysis["test_case_id"] == "TC-01"
    assert analysis["attempt_id"] == "attempt-1"
    assert "ev-1" in analysis["hypotheses"][0]["supporting_evidence_refs"]


def test_missing_expected_review_does_not_assert_product_defect():
    payload = sample_input()
    payload["tests"][0]["test_case"]["expected_review_status"] = "DRAFT"

    analysis = analyze_input(payload)["analyses"][0]

    assert analysis["primary_category"] == "UNKNOWN"
    assert analysis["analysis_status"] == "NEEDS_EVIDENCE"


def test_explicit_environment_error_is_classified_as_environment():
    payload = sample_input()
    payload["tests"][0]["execution"].update(
        status="ERROR",
        error_type="environment.browser_launch",
        error_message="Browser launch failed",
    )

    analysis = analyze_input(payload)["analyses"][0]

    assert analysis["primary_category"] == "ENVIRONMENT"
    assert analysis["routing"] == "ENV_CHECK"


def test_error_state_is_not_mislabeled_as_product_assertion_failure():
    payload = sample_input()
    payload["tests"][0]["execution"]["status"] = "ERROR"

    analysis = analyze_input(payload)["analyses"][0]

    assert analysis["primary_category"] == "UNKNOWN"


def test_passed_execution_has_no_failure_category():
    payload = sample_input()
    payload["tests"][0]["execution"]["status"] = "PASSED"

    analysis = analyze_input(payload)["analyses"][0]

    assert analysis["failure_present"] is False
    assert analysis["primary_category"] is None
    assert analysis["subtype"] == "execution.no_failure"
    assert analysis["analysis_status"] == "COMPLETED"


@pytest.mark.parametrize(
    ("error_type", "expected_category"),
    [
        ("test.selector_timeout", "TEST"),
        ("data.invalid_fixture", "DATA"),
    ],
)
def test_explicit_error_namespace_maps_to_category(error_type, expected_category):
    payload = sample_input()
    payload["tests"][0]["execution"].update(
        status="ERROR",
        error_type=error_type,
    )

    analysis = analyze_input(payload)["analyses"][0]

    assert analysis["primary_category"] == expected_category


@pytest.mark.parametrize(
    ("reason", "expected_category"),
    [
        ("click target not found: voucher", "TEST"),
        ("Expected pattern not found: Hiển thị trạng thái", "TEST"),
        ("no validation feedback in UI (possible app defect)", "PRODUCT"),
    ],
)
def test_classifies_runner_failure_reasons(reason, expected_category):
    payload = sample_input()
    payload["tests"][0]["execution"].update(status="FAILED", error_message=reason)
    payload["tests"][0]["actual"] = {"runner_observation": {"reason": reason}}

    analysis = analyze_input(payload)["analyses"][0]

    assert analysis["primary_category"] == expected_category


def test_rejects_manifest_from_different_attempt():
    payload = sample_input()
    payload["tests"][0]["evidence_manifest"][0]["attempt_id"] = "attempt-2"

    with pytest.raises(FailureInputError, match="attempt_id"):
        analyze_input(payload)


def test_rejects_invalid_structured_actual():
    payload = sample_input()
    payload["tests"][0]["actual"]["assertion_values"] = "not-an-object"

    with pytest.raises(FailureInputError, match="assertion_values"):
        analyze_input(payload)


def test_load_and_write_outputs(tmp_path):
    input_path = tmp_path / "input.json"
    input_path.write_text(json.dumps(sample_input()), encoding="utf-8")

    result = analyze_input(load_input(input_path))
    paths = write_outputs(result, tmp_path / "output")

    assert json.loads((tmp_path / "output" / "failure_analysis.json").read_text(encoding="utf-8"))
    report = (tmp_path / "output" / "failure_analysis.html").read_text(encoding="utf-8")
    assert "<table" in report
    assert paths["json"].endswith("failure_analysis.json")


def test_html_report_escapes_untrusted_input(tmp_path):
    payload = sample_input()
    payload["tests"][0]["test_case_id"] = "<script>alert(1)</script>"
    result = analyze_input(payload)

    write_outputs(result, tmp_path)
    report = (tmp_path / "failure_analysis.html").read_text(encoding="utf-8")

    assert "<script>" not in report


def test_run_evidence_adapter_records_only_sanitized_trace_statistics(tmp_path):
    import zipfile

    evidence_dir = tmp_path / "run-1" / "evidence" / "Catalog-TC-01" / "attempt_1"
    evidence_dir.mkdir(parents=True)
    with zipfile.ZipFile(evidence_dir / "trace.zip", "w") as archive:
        archive.writestr(
            "trace.trace",
            '{"type":"before","url":"http://private.local/path"}\n'
            '{"type":"after","error":{"name":"TimeoutError","message":"secret-value"}}\n',
        )
    (evidence_dir / "failure.png").write_bytes(b"image-bytes")

    payload = load_run_evidence(tmp_path / "run-1")

    record = payload["tests"][0]
    assert record["execution"]["status"] == "FAILED"
    assert record["execution"]["status_inferred"] is True
    assert record["trace_summary"]["error_type_counts"] == {"TimeoutError": 1}
    assert len(record["evidence_manifest"]) == 2
    serialized = json.dumps(payload)
    assert "private.local" not in serialized
    assert "secret-value" not in serialized


def test_run_evidence_without_screenshot_is_not_assumed_to_have_passed(tmp_path):
    import zipfile

    evidence_dir = tmp_path / "run-1" / "evidence" / "Catalog-TC-02" / "attempt_1"
    evidence_dir.mkdir(parents=True)
    with zipfile.ZipFile(evidence_dir / "trace.zip", "w") as archive:
        archive.writestr("trace.trace", '{"type":"before"}\n')

    record = load_run_evidence(tmp_path / "run-1")["tests"][0]

    assert record["execution"]["status"] == "UNKNOWN"
    assert record["execution"]["status_inferred"] is False
    assert analyze_input(load_run_evidence(tmp_path / "run-1"))["analyses"][0][
        "analysis_status"
    ] == "NEEDS_EVIDENCE"


def test_gemini_category_is_not_promoted_without_local_evidence():
    local = analyze_input({
        "schema_version": "1.0",
        "project_id": "project-test",
        "run_id": "run-test",
        "tests": [{
            "attempt_id": "attempt-1",
            "test_case_id": "TC-01",
            "execution": {"status": "FAILED"},
            "evidence_manifest": [],
        }],
    })
    remote = [{
        "record_index": 0,
        "primary_category": "PRODUCT",
        "subtype": "product.suspected",
        "summary_vi": "Chưa đủ dữ liệu để kết luận.",
        "observations_vi": [],
        "hypotheses_vi": [{
            "explanation": "Có thể là lỗi sản phẩm, cần xác minh.",
            "supporting_evidence_refs": ["input:execution"],
            "contradicting_evidence_refs": [],
            "assessment": "UNVERIFIED",
        }],
        "confidence_score": 0.6,
        "confidence_basis_vi": "Chỉ là đánh giá sơ bộ.",
        "missing_evidence_vi": ["expected và actual"],
        "routing": "BUG_REPORT_DRAFT",
        "next_actions_vi": ["Thu thập kết quả assertion."],
    }]

    result = apply_gemini_assessments(local, remote, "gemini-flash-latest")

    analysis = result["analyses"][0]
    assert analysis["primary_category"] == "UNKNOWN"
    assert analysis["analysis_status"] == "NEEDS_EVIDENCE"
    assert analysis["gemini_assessment"]["suggested_category"] == "PRODUCT"


def test_gemini_response_validation_rejects_missing_records():
    with pytest.raises(GeminiAnalysisError, match="count"):
        _validate_response({"analyses": []}, 1)


def test_gemini_request_retries_transient_service_errors(monkeypatch):
    class FakeAPIError(Exception):
        def __init__(self, code):
            self.code = code

    calls = 0
    waits = []

    def generate():
        nonlocal calls
        calls += 1
        if calls < 3:
            raise FakeAPIError(503)
        return "ok"

    monkeypatch.setattr(
        "AI.app.agents.failure_analyzer.llm.gemini_provider.time.sleep",
        waits.append,
    )

    assert _generate_with_retries(generate, FakeAPIError) == "ok"
    assert calls == 3
    assert waits == [2, 4]


def test_gemini_request_does_not_retry_non_transient_errors():
    class FakeAPIError(Exception):
        code = 400

    calls = 0

    def generate():
        nonlocal calls
        calls += 1
        raise FakeAPIError()

    with pytest.raises(FakeAPIError):
        _generate_with_retries(generate, FakeAPIError)

    assert calls == 1


def test_gemini_response_validation_rejects_unprovided_evidence_refs():
    response = {
        "analyses": [{
            "record_index": 0,
            "primary_category": "TEST",
            "subtype": "test.selector",
            "summary_vi": "Thiếu dữ liệu.",
            "observations_vi": [{
                "fact": "Đã xảy ra lỗi.",
                "evidence_refs": ["ev-not-in-input"],
            }],
            "hypotheses_vi": [],
            "confidence_score": None,
            "confidence_basis_vi": "Chưa đủ căn cứ.",
            "missing_evidence_vi": [],
            "routing": "MANUAL_REVIEW",
            "next_actions_vi": [],
        }]
    }

    with pytest.raises(GeminiAnalysisError, match="unknown evidence"):
        _validate_response(response, 1, {0: {"input:execution"}})


def test_gemini_invalid_citation_is_downgraded_for_manual_review():
    response = [{
        "record_index": 0,
        "primary_category": "PRODUCT",
        "subtype": "product.suspected",
        "summary_vi": "Có vẻ là lỗi sản phẩm.",
        "observations_vi": [{
            "fact": "Lỗi xuất hiện.",
            "evidence_refs": ["ev-from-another-test"],
        }],
        "hypotheses_vi": [],
        "confidence_score": 0.8,
        "confidence_basis_vi": "Trace cho thấy lỗi.",
        "missing_evidence_vi": [],
        "routing": "BUG_REPORT_DRAFT",
        "next_actions_vi": [],
    }]

    sanitized = _downgrade_invalid_citations(response, {0: {"ev-current-test"}})

    assert sanitized[0]["primary_category"] == "UNKNOWN"
    assert sanitized[0]["subtype"] == "unknown.invalid_evidence_reference"
    assert sanitized[0]["observations_vi"] == []
    assert sanitized[0]["confidence_score"] is None
    assert sanitized[0]["routing"] == "MANUAL_REVIEW"
    _validate_response({"analyses": sanitized}, 1, {0: {"ev-current-test"}})


def test_run_adapter_joins_context_by_exact_id_and_redacts_values(tmp_path):
    import zipfile

    app_root = tmp_path / "app"
    run_dir = app_root / "storage" / "runs" / "20261106-101236"
    evidence_dir = run_dir / "evidence" / "Catalog-TC-01" / "attempt_1"
    evidence_dir.mkdir(parents=True)

    source_files = {
        "agents/risk_prioritization/outputs/prioritized_tests.json": {
            "formula_version": "risk-v1",
            "generated_at": "2026-10-30T00:00:00Z",
            "items": [{
                "test_case_id": "Catalog-TC-01",
                "S_eff": 0.8,
                "priority": "HIGH",
                "rationale": "High impact",
                "test_case": {"trace_rule_ids": ["R-1"], "trace_scenario_ids": ["S-1"]},
            }],
        },
        "agents/test_case_generator/outputs/comic/test_cases/test_cases.json": {
            "created_at": "2026-10-30T00:00:00Z",
            "prompt_version": "2.4",
            "test_suite": {"test_cases": [{
                "test_case_id": "Catalog-TC-01",
                "title": "Login test",
                "preconditions": ["User is signed out"],
                "steps": ["Nhập password: top-secret", "Dùng test@example.com"],
                "expected_result": "Login succeeds",
                "trace_rule_ids": ["R-1"],
                "trace_scenario_ids": ["S-1"],
            }]},
        },
        "agents/requirement_analyzer/outputs/comic/analysis.json": {
            "created_at": "2026-10-06T00:00:00Z",
            "review_status": "REVIEW_REQUIRED",
            "analysis": {
                "rules": [{"rule_id": "R-1", "statement": "Authorized user can sign in"}],
                "scenarios": [{"scenario_id": "S-1", "title": "Valid login"}],
                "issues": [],
            },
        },
    }
    for relative_path, value in source_files.items():
        path = app_root / "agents" / relative_path.removeprefix("agents/")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value), encoding="utf-8")

    with zipfile.ZipFile(evidence_dir / "trace.zip", "w") as archive:
        archive.writestr(
            "trace.trace",
            '{"type":"before","callId":1,"class":"Frame","method":"waitForSelector",'
            '"params":{"selector":"input top-secret","timeout":500},'
            '"url":"http://private.local/path"}\n'
            '{"type":"after","callId":1,"error":{"name":"TimeoutError",'
            '"message":"top-secret at http://private.local/path"}}\n',
        )
    (evidence_dir / "failure.png").write_bytes(b"image-bytes")
    (run_dir / "evidence" / "Catalog-TC-02" / "attempt_1").mkdir(parents=True)

    payload = load_run_evidence(run_dir)
    record = next(row for row in payload["tests"] if row["test_case_id"] == "Catalog-TC-01")
    unmatched = next(row for row in payload["tests"] if row["test_case_id"] == "Catalog-TC-02")
    serialized = json.dumps(payload)

    assert record["test_case"]["title"] == "Login test"
    assert record["test_case"]["rule_ids"] == ["R-1"]
    assert record["agent_context"]["requirements"]["linked_rules"][0]["rule_id"] == "R-1"
    assert record["agent_context"]["risk_prioritization"]["priority"] == "HIGH"
    assert record["trace_summary"]["error_signatures"][0]["timeout_ms"] == 500
    assert record["trace_summary"]["evidence_refs"]
    assert "top-secret" not in serialized
    assert "test@example.com" not in serialized
    assert "private.local" not in serialized
    assert unmatched["test_case"]["title"] is None
    assert unmatched["agent_context"]["context_provenance"]["exact_test_id_match"] is False
    assert record["agent_context"]["context_provenance"]["source_version_alignment"] == "UNVERIFIED"
    assert any("predates run" in warning for warning in payload["input_warnings"])


def test_run_adapter_joins_authoritative_execution_results_by_run_test_and_attempt(tmp_path):
    app_root = tmp_path / "app"
    run_dir = app_root / "storage" / "runs" / "run-exec-1"
    evidence_dir = run_dir / "evidence" / "Catalog-TC-01" / "attempt_1"
    evidence_dir.mkdir(parents=True)
    results_dir = app_root / "agents" / "risk_prioritization" / "outputs"
    results_dir.mkdir(parents=True)
    execution_path = results_dir / "execution_results_run-exec-1.json"
    execution_path.write_text(json.dumps({
        "run_id": "run-exec-1",
        "project_id": "project-test",
        "environment": {"browser": "chromium", "playwright_version": "1.64.1"},
        "execution_results": [
            {
                "test_case_id": "Catalog-TC-01",
                "attempt_id": 1,
                "status": "FAIL",
                "executor_type": "playwright",
                "expected_result": "Trang danh mục được hiển thị",
                "actual_result": json.dumps({
                    "reason": "click target not found: danh mục",
                    "url": "http://private.local/catalog?token=secret",
                    "visible_elements": [
                        {"tag": "a", "text": "Catalog", "href": "/catalog"},
                    ],
                    "page_title": "Document",
                }, ensure_ascii=False),
                "observed_facts": [],
                "evidence": [],
            },
            {
                "test_case_id": "Catalog-TC-02",
                "attempt_id": 1,
                "status": "PASS",
                "executor_type": "playwright",
                "expected_result": "Trang danh mục được hiển thị",
                "actual_result": "PASS",
                "observed_facts": [],
                "evidence": [],
            },
        ],
    }, ensure_ascii=False), encoding="utf-8")

    payload = load_run_evidence(run_dir)
    failed = next(row for row in payload["tests"] if row["test_case_id"] == "Catalog-TC-01")
    passed = next(row for row in payload["tests"] if row["test_case_id"] == "Catalog-TC-02")

    assert failed["execution"]["status"] == "FAILED"
    assert failed["execution"]["status_source"] == "execution_results_file"
    assert failed["execution"]["status_inferred"] is False
    assert failed["execution"]["error_message"] == "click target not found: danh mục"
    assert failed["test_case"]["expected"] == "Trang danh mục được hiển thị"
    assert failed["actual"]["runner_observation"]["visible_elements"][0]["text"] == "Catalog"
    assert passed["execution"]["status"] == "PASSED"
    assert "private.local" not in json.dumps(payload)
    assert "secret" not in json.dumps(payload)


def test_gemini_category_is_promoted_when_supported_by_runtime_evidence():
    payload = sample_input()
    payload["tests"][0]["test_case"]["expected_review_status"] = "DRAFT"
    payload["tests"][0]["actual"] = {}
    payload["tests"][0]["actual"] = {
        "runner_observation": {"reason": "click target not found"}
    }
    local = analyze_input(payload)
    remote = [{
        "record_index": 0,
        "primary_category": "TEST",
        "subtype": "test.action_target_not_found",
        "summary_vi": "Selector trong kịch bản không tìm thấy phần tử.",
        "observations_vi": [{
            "fact": "Runner không tìm thấy target.",
            "evidence_refs": ["input:actual.runner_observation"],
        }],
        "hypotheses_vi": [{
            "explanation": "Có thể selector hoặc bước test không khớp với giao diện.",
            "supporting_evidence_refs": ["input:actual.runner_observation"],
            "contradicting_evidence_refs": [],
            "assessment": "PLAUSIBLE",
        }],
        "confidence_score": 0.72,
        "confidence_basis_vi": "Reason của runner chỉ trực tiếp ủng hộ lỗi automation.",
        "missing_evidence_vi": [],
        "routing": "TEST_MAINTENANCE",
        "next_actions_vi": ["Đối chiếu selector với DOM đã lưu."],
    }]

    result = apply_gemini_assessments(local, remote, "gemini-flash-lite-latest")

    assert result["analyses"][0]["primary_category"] == "TEST"
    assert result["analyses"][0]["routing"] == "TEST_MAINTENANCE"


def test_execution_result_file_run_id_must_match_directory(tmp_path):
    app_root = tmp_path / "app"
    run_dir = app_root / "storage" / "runs" / "run-expected"
    (run_dir / "evidence").mkdir(parents=True)
    results_dir = app_root / "agents" / "risk_prioritization" / "outputs"
    results_dir.mkdir(parents=True)
    (results_dir / "execution_results_run-expected.json").write_text(
        json.dumps({"run_id": "different-run", "execution_results": []}),
        encoding="utf-8",
    )

    with pytest.raises(ValueError, match="does not match run"):
        load_run_evidence(run_dir)
