from __future__ import annotations

import hashlib
import html
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from uuid import uuid4


INPUT_SCHEMA_VERSION = "1.0"
OUTPUT_SCHEMA_VERSION = "1.0"
POLICY_VERSION = "failure-policy-v1"
ALLOWED_STATES = {
    "PASSED", "PASS", "FAILED", "FAIL", "ERROR", "BLOCKED", "FLAKY", "MANUAL", "UNKNOWN"
}
APPROVED_EXPECTED_STATES = {"APPROVED", "REVIEWED", "ACCEPTED"}


class FailureInputError(ValueError):
    """Raised when an input file does not match the failure analysis contract."""


def load_input(path: str | Path) -> dict[str, Any]:
    input_path = Path(path)
    try:
        payload = json.loads(input_path.read_text(encoding="utf-8"))
    except OSError as exc:
        raise FailureInputError(f"Cannot read input JSON: {input_path}") from exc
    except json.JSONDecodeError as exc:
        raise FailureInputError(f"Invalid JSON at line {exc.lineno}, column {exc.colno}") from exc
    _validate_input(payload)
    return payload


def _require_text(value: Any, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise FailureInputError(f"{field} must be a non-empty string")
    return value.strip()


def _validate_input(payload: Any) -> None:
    if not isinstance(payload, dict):
        raise FailureInputError("Input root must be a JSON object")
    if payload.get("schema_version") != INPUT_SCHEMA_VERSION:
        raise FailureInputError(
            f"schema_version must be {INPUT_SCHEMA_VERSION!r}"
        )
    _require_text(payload.get("project_id"), "project_id")
    _require_text(payload.get("run_id"), "run_id")
    tests = payload.get("tests")
    if not isinstance(tests, list) or not tests:
        raise FailureInputError("tests must be a non-empty array")

    seen: set[tuple[str, str]] = set()
    for index, record in enumerate(tests):
        prefix = f"tests[{index}]"
        if not isinstance(record, dict):
            raise FailureInputError(f"{prefix} must be an object")
        test_case_id = _require_text(record.get("test_case_id"), f"{prefix}.test_case_id")
        attempt_id = _require_text(record.get("attempt_id"), f"{prefix}.attempt_id")
        identity = (test_case_id, attempt_id)
        if identity in seen:
            raise FailureInputError(f"Duplicate test_case_id/attempt_id: {identity}")
        seen.add(identity)

        execution = record.get("execution")
        if not isinstance(execution, dict):
            raise FailureInputError(f"{prefix}.execution must be an object")
        state = _require_text(execution.get("status"), f"{prefix}.execution.status").upper()
        if state not in ALLOWED_STATES:
            raise FailureInputError(f"Unsupported execution status {state!r} in {prefix}")

        for object_field in ("test_case", "actual", "environment"):
            field_value = record.get(object_field, {})
            if not isinstance(field_value, dict):
                raise FailureInputError(f"{prefix}.{object_field} must be an object")
        assertions = record.get("actual", {}).get("assertion_values")
        if assertions is not None and not isinstance(assertions, dict):
            raise FailureInputError(f"{prefix}.actual.assertion_values must be an object")

        manifest = record.get("evidence_manifest", [])
        if not isinstance(manifest, list):
            raise FailureInputError(f"{prefix}.evidence_manifest must be an array")
        evidence_ids: set[str] = set()
        for item_index, item in enumerate(manifest):
            evidence_prefix = f"{prefix}.evidence_manifest[{item_index}]"
            if not isinstance(item, dict):
                raise FailureInputError(f"{evidence_prefix} must be an object")
            evidence_id = _require_text(item.get("evidence_id"), f"{evidence_prefix}.evidence_id")
            if evidence_id in evidence_ids:
                raise FailureInputError(f"Duplicate evidence_id {evidence_id!r} in {prefix}")
            evidence_ids.add(evidence_id)
            for identity_field, expected in (
                ("run_id", payload["run_id"]),
                ("attempt_id", attempt_id),
            ):
                actual = item.get(identity_field)
                if actual is not None and actual != expected:
                    raise FailureInputError(
                        f"{evidence_prefix}.{identity_field} does not match its test record"
                    )


def _fingerprint(payload: dict[str, Any]) -> str:
    canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def _category_for(record: dict[str, Any]) -> tuple[str, str, str]:
    execution = record["execution"]
    state = execution["status"].upper()
    error_type = str(execution.get("error_type") or "").strip().lower()
    error_message = str(execution.get("error_message") or "").strip().lower()

    if state in {"PASSED", "PASS"}:
        return "UNKNOWN", "execution.no_failure", "No failing execution was reported."
    if state == "UNKNOWN":
        return "UNKNOWN", "execution.result_unavailable", "No authoritative per-test execution status was stored."
    if state == "BLOCKED":
        return "UNKNOWN", "execution.blocked", "The test was blocked before its own result could be evaluated."
    if state == "MANUAL":
        return "UNKNOWN", "execution.manual", "The test requires manual execution and has no automated failure result."

    if error_type.startswith("environment."):
        return "ENVIRONMENT", error_type, "Execution metadata identifies an environment error."
    if error_type.startswith("data."):
        return "DATA", error_type, "Execution metadata identifies a test-data error."
    if error_type.startswith("test."):
        return "TEST", error_type, "Execution metadata identifies a test definition or runner-script error."

    combined_error = f"{error_type} {error_message}"
    if any(term in combined_error for term in (
        "click target not found",
        "fill target not found",
        "quantity control not found",
        "invalid selector",
        "unknown step",
    )):
        return "TEST", "test.action_target_not_found", (
            "The runner could not resolve the action or selector described by the test step."
        )
    if "expected pattern not found" in combined_error:
        return "TEST", "test.assertion_pattern_not_found", (
            "The runner's text-pattern assertion did not match; verify the test oracle and the rendered page."
        )
    if "no validation feedback in ui" in combined_error:
        return "PRODUCT", "product.validation_feedback_missing", (
            "The recorded UI observation reports missing validation feedback for the expected validation case."
        )
    if any(term in combined_error for term in (
        "browser launch failed",
        "connection refused",
        "connection reset",
        "host unreachable",
        "dns",
        "environment unavailable",
        "dependency unavailable",
    )):
        return "ENVIRONMENT", "environment.execution_unavailable", (
            "The execution error indicates that the target or its dependency was unavailable."
        )
    if any(term in combined_error for term in (
        "collection file not found",
        "invalid selector",
        "unknown step",
        "test setup",
        "test script",
    )):
        return "TEST", "test.definition_or_setup", (
            "The execution error indicates a test definition or setup problem."
        )
    if any(term in combined_error for term in (
        "fixture",
        "seed data",
        "invalid test data",
        "constraint violation",
    )):
        return "DATA", "data.fixture_or_test_data", (
            "The execution error indicates a fixture or test-data problem."
        )

    expected_status = str(
        record.get("test_case", {}).get("expected_review_status") or ""
    ).strip().upper()
    assertions = record.get("actual", {}).get("assertion_values", {})
    if (
        state in {"FAILED", "FAIL"}
        and isinstance(assertions, dict)
        and assertions.get("passed") is False
        and expected_status in APPROVED_EXPECTED_STATES
        and "expected" in assertions
        and "actual" in assertions
    ):
        return "PRODUCT", "product.assertion_mismatch", (
            "A failed assertion records both expected and actual values, and the expected result is reviewed."
        )

    return "UNKNOWN", "unknown.insufficient_evidence", (
        "Available execution data does not safely distinguish a product, test, data, or environment failure."
    )


def _routing_for(category: str) -> str:
    return {
        "PRODUCT": "BUG_REPORT_DRAFT",
        "TEST": "TEST_MAINTENANCE",
        "DATA": "DATA_FIX",
        "ENVIRONMENT": "ENV_CHECK",
        "UNKNOWN": "MANUAL_REVIEW",
    }[category]


def _analyze_record(payload: dict[str, Any], record: dict[str, Any], fingerprint: str) -> dict[str, Any]:
    category, subtype, explanation = _category_for(record)
    execution = record["execution"]
    test_case = record.get("test_case", {})
    actual = record.get("actual", {})
    evidence_manifest = record.get("evidence_manifest", [])
    evidence_ids = [item["evidence_id"] for item in evidence_manifest]
    source_refs = ["input:execution"]
    if actual.get("assertion_values") is not None:
        source_refs.append("input:actual.assertion_values")
    if actual.get("filtered_response") is not None:
        source_refs.append("input:actual.filtered_response")
    if actual.get("ui_state") is not None:
        source_refs.append("input:actual.ui_state")
    if actual.get("db_state") is not None:
        source_refs.append("input:actual.db_state")
    if actual.get("runner_observation") is not None:
        source_refs.append("input:actual.runner_observation")
    if actual.get("runner_reason") is not None:
        source_refs.append("input:actual.runner_reason")

    state = execution["status"].upper()
    failure_present = (
        False if state in {"PASSED", "PASS"}
        else None if state in {"MANUAL", "BLOCKED", "UNKNOWN"}
        else True
    )
    assertion_values = actual.get("assertion_values")
    passed_assertion = assertion_values.get("passed") if isinstance(assertion_values, dict) else None
    summary = (
        f"{record['test_case_id']} reported {state}; category {category}. {explanation}"
    )
    missing_evidence: list[str] = []
    if not test_case.get("expected"):
        missing_evidence.append("Test case expected result is missing.")
    if not test_case.get("expected_review_status"):
        missing_evidence.append("Expected result review status is missing.")
    if not actual:
        missing_evidence.append("Structured actual observations are missing.")
    if not evidence_manifest:
        missing_evidence.append("No evidence manifest was provided.")
    if category == "UNKNOWN":
        missing_evidence.append("Evidence needed to distinguish the failure category.")
    if failure_present is False:
        missing_evidence = []
        analysis_status = "COMPLETED"
    elif category == "UNKNOWN" or missing_evidence:
        analysis_status = "NEEDS_EVIDENCE"
    else:
        analysis_status = "COMPLETED"

    observations: list[dict[str, Any]] = [{
        "observation_id": "obs-1",
        "fact": f"Trạng thái thực thi được ghi nhận là {state}.",
        "evidence_refs": source_refs,
    }]
    if execution.get("error_message"):
        observations.append({
            "observation_id": "obs-2",
            "fact": f"Thông báo runner: {execution['error_message']}",
            "evidence_refs": ["input:execution"],
        })
    if isinstance(actual.get("assertion_values"), dict):
        assertion = actual["assertion_values"]
        if "expected" in assertion or "actual" in assertion:
            observations.append({
                "observation_id": f"obs-{len(observations) + 1}",
                "fact": (
                    "Giá trị assertion được ghi nhận: "
                    f"expected={assertion.get('expected')!r}, actual={assertion.get('actual')!r}."
                ),
                "evidence_refs": ["input:actual.assertion_values"],
            })
    runner_observation = actual.get("runner_observation")
    if isinstance(runner_observation, dict) and runner_observation.get("reason"):
        observations.append({
            "observation_id": f"obs-{len(observations) + 1}",
            "fact": f"Quan sát của runner: {runner_observation['reason']}",
            "evidence_refs": ["input:actual.runner_observation"],
        })

    hypotheses = []
    if category != "UNKNOWN":
        hypotheses.append({
            "hypothesis_id": "hyp-1",
            "explanation": explanation,
            "supporting_evidence_refs": source_refs + evidence_ids,
            "contradicting_evidence_refs": [],
            "assessment": "SUPPORTED" if category == "PRODUCT" and passed_assertion is False else "PLAUSIBLE",
        })
    elif state not in {"PASSED", "PASS"}:
        hypotheses.append({
            "hypothesis_id": "hyp-1",
            "explanation": explanation,
            "supporting_evidence_refs": source_refs + evidence_ids,
            "contradicting_evidence_refs": [],
            "assessment": "UNVERIFIED",
        })

    confidence = None
    confidence_basis = "No calibrated confidence model is configured."
    if category == "PRODUCT":
        confidence = 0.75
        confidence_basis = (
            "A failed assertion includes expected and actual values, and the expected result is reviewed; "
            "the score is an uncalibrated rule-based estimate."
        )
    elif category != "UNKNOWN":
        confidence = 0.65
        confidence_basis = (
            "An explicit error type or recognizable execution error supports the category; "
            "the score is an uncalibrated rule-based estimate."
        )

    actions = {
        "PRODUCT": ("Compare the recorded actual value with the approved requirement and draft a bug report.", "DEVELOPER"),
        "TEST": ("Review the test definition, selectors, and runner setup.", "TEST_ENGINEER"),
        "DATA": ("Verify the referenced fixture and test data for this attempt.", "TEST_ENGINEER"),
        "ENVIRONMENT": ("Check target health and dependencies for this run and attempt.", "DEVOPS"),
        "UNKNOWN": ("Collect the missing execution or actual evidence, then review the classification.", "TEST_ENGINEER"),
    }
    action, owner = actions[category]
    return {
        "analysis_id": f"fa-{uuid4()}",
        "schema_version": OUTPUT_SCHEMA_VERSION,
        "project_id": payload["project_id"],
        "run_id": payload["run_id"],
        "attempt_id": record["attempt_id"],
        "test_case_id": record["test_case_id"],
        "execution_status": state,
        "failure_present": failure_present,
        "analysis_status": analysis_status,
        "primary_category": None if failure_present is False else category,
        "subtype": subtype,
        "summary": summary,
        "observations": observations,
        "hypotheses": hypotheses,
        "confidence_score": confidence,
        "confidence_basis": confidence_basis,
        "missing_evidence": missing_evidence,
        "contributing_factors": [],
        "next_actions": [{
            "action": action,
            "owner_role": owner,
            "requires_approval": True,
        }],
        "routing": _routing_for(category),
        "severity_suggestion": None,
        "review_status": "REVIEW_REQUIRED",
        "provenance": {
            "model": "deterministic-rules-v1",
            "prompt_version": None,
            "policy_version": POLICY_VERSION,
            "input_fingerprint": f"sha256:{fingerprint}",
            "created_at": datetime.now(timezone.utc).isoformat(),
        },
        "evidence_manifest": evidence_manifest,
    }


def apply_gemini_assessments(
    result: dict[str, Any],
    gemini_analyses: list[dict[str, Any]],
    model: str,
) -> dict[str, Any]:
    if len(result["analyses"]) != len(gemini_analyses):
        raise ValueError("Gemini analysis count does not match local analysis")

    updated = {**result, "analysis_provider": "gemini"}
    analyses = []
    for local, remote in zip(result["analyses"], gemini_analyses):
        if remote.get("record_index") != len(analyses):
            raise ValueError("Gemini analysis order does not match input record order")
        suggested_category = remote["primary_category"]
        supported_refs = {
            ref
            for hypothesis in remote["hypotheses_vi"]
            if hypothesis["assessment"] in {"SUPPORTED", "PLAUSIBLE"}
            for ref in hypothesis["supporting_evidence_refs"]
        }
        manifest_ids = {
            item.get("evidence_id")
            for item in local.get("evidence_manifest", [])
            if isinstance(item, dict)
        }
        runtime_refs = {
            "input:actual.runner_observation",
            "input:actual.runner_reason",
            "input:actual.assertion_values",
            "input:trace_summary",
        } | manifest_ids
        runtime_evidence_support = bool(supported_refs & runtime_refs)
        promotable_status = local.get("subtype") not in {
            "execution.no_failure",
            "execution.manual",
            "execution.blocked",
        }
        category_is_supported = (
            local["primary_category"] not in (None, "UNKNOWN")
            or (
                suggested_category != "UNKNOWN"
                and runtime_evidence_support
                and promotable_status
                and local.get("failure_present") is not False
            )
        )
        suggested_category = remote["primary_category"]
        missing = list(local["missing_evidence"])
        if not category_is_supported and local.get("failure_present") is not False:
            missing.append(
                "Gemini không thể xác nhận nhãn nếu giả thuyết không dẫn bằng chứng runtime cụ thể."
            )
        gemini_assessment = {
            "model": model,
            "prompt_version": "failure-analysis-gemini-vi-v1",
            "suggested_category": suggested_category,
            "summary_vi": remote["summary_vi"],
            "observations_vi": remote["observations_vi"],
            "hypotheses_vi": remote["hypotheses_vi"],
            "confidence_score": remote["confidence_score"],
            "confidence_basis_vi": remote["confidence_basis_vi"],
            "missing_evidence_vi": remote["missing_evidence_vi"],
            "next_actions_vi": remote["next_actions_vi"],
        }
        merged = {
            **local,
            "analysis_status": (
                "COMPLETED"
                if local.get("failure_present") is False
                else "NEEDS_EVIDENCE"
                if not category_is_supported
                else local["analysis_status"]
            ),
            "summary": (
                local["summary"]
                if local.get("failure_present") is False
                else remote["summary_vi"]
            ),
            "observations": local["observations"] + [
                {
                    "observation_id": f"gemini-obs-{item_index + 1}",
                    "fact": item["fact"],
                    "evidence_refs": item["evidence_refs"],
                }
                for item_index, item in enumerate(remote["observations_vi"])
            ],
            "hypotheses": local["hypotheses"] + [
                {
                    "hypothesis_id": f"gemini-hyp-{item_index + 1}",
                    "explanation": item["explanation"],
                    "supporting_evidence_refs": item["supporting_evidence_refs"],
                    "contradicting_evidence_refs": item["contradicting_evidence_refs"],
                    "assessment": item["assessment"],
                }
                for item_index, item in enumerate(remote["hypotheses_vi"])
            ],
            "missing_evidence": list(dict.fromkeys(missing + remote["missing_evidence_vi"])),
            "next_actions": [
                {
                    "action": action,
                    "owner_role": "TEST_ENGINEER",
                    "requires_approval": True,
                }
                for action in remote["next_actions_vi"]
            ] or local["next_actions"],
            "gemini_assessment": gemini_assessment,
            "provenance": {
                **local["provenance"],
                "model": model,
                "prompt_version": "failure-analysis-gemini-vi-v1",
            },
        }
        if not category_is_supported:
            merged["primary_category"] = (
                None if local.get("failure_present") is False else "UNKNOWN"
            )
            merged["subtype"] = (
                local["subtype"]
                if local.get("failure_present") is False
                else "unknown.insufficient_evidence"
            )
            merged["confidence_score"] = None
            merged["confidence_basis"] = (
                "Không thể xác nhận category chỉ từ metadata; xem phần đánh giá Gemini và bằng chứng cần bổ sung."
            )
        elif local["primary_category"] in (None, "UNKNOWN"):
            merged["primary_category"] = suggested_category
            merged["subtype"] = remote["subtype"]
            merged["routing"] = remote["routing"]
            merged["confidence_score"] = remote["confidence_score"]
            merged["confidence_basis"] = remote["confidence_basis_vi"]
        elif suggested_category != local["primary_category"]:
            merged["warnings"] = [
                f"Gemini đề xuất {suggested_category}, nhưng category chính được giữ theo bằng chứng deterministic."
            ]
        analyses.append(merged)
    updated["analyses"] = analyses
    return updated


def analyze_input(payload: dict[str, Any]) -> dict[str, Any]:
    _validate_input(payload)
    fingerprint = _fingerprint(payload)
    analyses = [_analyze_record(payload, record, fingerprint) for record in payload["tests"]]
    return {
        "schema_version": OUTPUT_SCHEMA_VERSION,
        "project_id": payload["project_id"],
        "run_id": payload["run_id"],
        "input_warnings": payload.get("input_warnings", []),
        "analysis_provider": "rules",
        "analyses": analyses,
    }


def write_outputs(result: dict[str, Any], output_dir: str | Path) -> dict[str, str]:
    out = Path(output_dir)
    out.mkdir(parents=True, exist_ok=True)
    json_path = out / "failure_analysis.json"
    html_path = out / "failure_analysis.html"
    json_path.write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    rows = []
    for analysis in result["analyses"]:
        rows.append(
            "<tr>"
            f"<td>{html.escape(analysis['test_case_id'])}</td>"
            f"<td>{html.escape(analysis['attempt_id'])}</td>"
            f"<td>{html.escape(analysis['analysis_status'])}</td>"
            f"<td>{html.escape(analysis['primary_category'] or 'N/A — không có failure')}</td>"
            f"<td>{html.escape(analysis['subtype'])}</td>"
            f"<td>{html.escape(analysis['summary'])}</td>"
            "</tr>"
        )
    html_path.write_text(
        "<!doctype html><html lang='vi'><meta charset='utf-8'>"
        "<title>Failure Analysis</title><h1>Failure Analysis</h1>"
        f"<p>Project: {html.escape(result['project_id'])} | Run: {html.escape(result['run_id'])}</p>"
        "<table border='1' cellpadding='6' cellspacing='0'>"
        "<tr><th>Test</th><th>Attempt</th><th>Status</th><th>Category</th><th>Subtype</th><th>Summary</th></tr>"
        f"{''.join(rows)}</table></html>",
        encoding="utf-8",
    )
    return {"json": str(json_path), "html": str(html_path)}
