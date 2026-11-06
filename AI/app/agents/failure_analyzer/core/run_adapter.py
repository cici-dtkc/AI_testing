from __future__ import annotations

import hashlib
import json
import re
import zipfile
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any


_ATTEMPT_PATTERN = re.compile(r"attempt_(\d+)$")
_TIMESTAMP_PATTERN = re.compile(r"(\d{8}T\d{12})")
_SECRET_PATTERN = re.compile(
    r"(?i)\b(password|passwd|secret|token|api[_ -]?key|authorization|cookie|"
    r"session(?:id)?|jsessionid|csrf|username|user name|account|email|phone|"
    r"mật khẩu|mat khau|tên đăng nhập|ten dang nhap|số điện thoại|so dien thoai)"
    r"\b(\s*[:=]\s*|\s+)(['\"]?)[^\s,;'\"]+"
)
_BEARER_PATTERN = re.compile(r"(?i)\bBearer\s+[A-Za-z0-9._~+/-]+=*")
_JWT_PATTERN = re.compile(r"\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b")
_EMAIL_PATTERN = re.compile(r"\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b", re.IGNORECASE)
_URL_PATTERN = re.compile(r"https?://[^\s)>'\"]+", re.IGNORECASE)
_LONG_NUMBER_PATTERN = re.compile(r"(?<!\w)\+?\d[\d .()-]{7,}\d(?!\w)")


def _safe_text(value: Any, limit: int = 1200) -> str | None:
    if not isinstance(value, str) or not value.strip():
        return None
    text = value.strip()
    text = _SECRET_PATTERN.sub(r"\1\2[REDACTED]", text)
    text = _BEARER_PATTERN.sub("Bearer [REDACTED]", text)
    text = _JWT_PATTERN.sub("[REDACTED_TOKEN]", text)
    text = _EMAIL_PATTERN.sub("[REDACTED_EMAIL]", text)
    text = _URL_PATTERN.sub("[REDACTED_URL]", text)
    text = _LONG_NUMBER_PATTERN.sub("[REDACTED_NUMBER]", text)
    return text[:limit]


def _safe_text_list(value: Any, limit: int = 30) -> list[str]:
    if isinstance(value, str):
        value = [value]
    if not isinstance(value, list):
        return []
    return [
        text
        for item in value[:limit]
        if (text := _safe_text(item)) is not None
    ]


def _read_json_source(path: Path, label: str, warnings: list[str]) -> dict[str, Any] | None:
    if not path.is_file():
        warnings.append(f"{label} source was not found: {path.name}.")
        return None
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError(f"Cannot read {label} source {path.name}") from exc
    if not isinstance(value, dict):
        raise ValueError(f"{label} source {path.name} must contain a JSON object")
    return value


def _safe_id_list(*values: Any) -> list[str]:
    result: list[str] = []
    for value in values:
        if isinstance(value, list):
            for item in value:
                if isinstance(item, str) and item.strip() and item not in result:
                    result.append(item.strip())
    return result


def _load_context_sources(app_root: Path, warnings: list[str]) -> dict[str, Any]:
    source_paths = {
        "risk": app_root / "agents" / "risk_prioritization" / "outputs" / "prioritized_tests.json",
        "test_suite": app_root / "agents" / "test_case_generator" / "outputs" / "comic" / "test_cases" / "test_cases.json",
        "requirements": app_root / "agents" / "requirement_analyzer" / "outputs" / "comic" / "analysis.json",
    }
    loaded = {
        name: _read_json_source(path, name, warnings)
        for name, path in source_paths.items()
    }
    risk_doc = loaded["risk"] or {}
    suite_doc = loaded["test_suite"] or {}
    requirement_doc = loaded["requirements"] or {}

    risk_rows = {
        row["test_case_id"]: row
        for row in risk_doc.get("items", [])
        if isinstance(row, dict) and isinstance(row.get("test_case_id"), str)
    }
    suite_rows = {
        row["test_case_id"]: row
        for row in suite_doc.get("test_suite", {}).get("test_cases", [])
        if isinstance(row, dict) and isinstance(row.get("test_case_id"), str)
    }
    analysis = requirement_doc.get("analysis", {})
    if not isinstance(analysis, dict):
        analysis = {}
    rules = {
        row["rule_id"]: row
        for row in analysis.get("rules", [])
        if isinstance(row, dict) and isinstance(row.get("rule_id"), str)
    }
    scenarios = {
        row["scenario_id"]: row
        for row in analysis.get("scenarios", [])
        if isinstance(row, dict) and isinstance(row.get("scenario_id"), str)
    }
    return {
        "risk_rows": risk_rows,
        "suite_rows": suite_rows,
        "rules": rules,
        "scenarios": scenarios,
        "issues": [
            issue for issue in analysis.get("issues", [])
            if isinstance(issue, dict)
        ],
        "versions": {
            "risk": {
                "generated_at": risk_doc.get("generated_at"),
                "model_version": risk_doc.get("formula_version"),
            },
            "test_suite": {
                "generated_at": suite_doc.get("created_at"),
                "version": suite_doc.get("prompt_version"),
            },
            "requirements": {
                "generated_at": requirement_doc.get("created_at"),
                "review_status": requirement_doc.get("review_status"),
                "version": requirement_doc.get("prompt_version"),
            },
        },
    }


def _selector_kind(selector: Any) -> str:
    if not isinstance(selector, str):
        return "unknown"
    normalized = selector.lower()
    for token, label in (
        ("data-testid", "test_id"),
        ("[role=", "role"),
        ("text=", "text"),
        ("#", "id"),
        ("[name=", "name"),
    ):
        if token and token in normalized:
            return label
    return "css_or_other"


def _classify_trace_error(message: str) -> str | None:
    normalized = message.lower()
    if any(token in normalized for token in ("err_connection_refused", "connection refused")):
        return "environment.target_connection_refused"
    if any(token in normalized for token in ("err_name_not_resolved", "dns", "name not resolved")):
        return "environment.dns_resolution"
    if any(token in normalized for token in ("invalid selector", "not a valid selector")):
        return "test.invalid_selector"
    if "browser has been closed" in normalized or "target page, context or browser has been closed" in normalized:
        return "environment.browser_closed"
    return None


def _read_trace_summary(path: Path) -> dict[str, Any]:
    event_counts: Counter[str] = Counter()
    error_type_counts: Counter[str] = Counter()
    error_signatures: Counter[tuple[str, str, int | None, str | None]] = Counter()
    category_hint_counts: Counter[str] = Counter()
    before_calls: dict[Any, dict[str, Any]] = {}
    try:
        with zipfile.ZipFile(path) as archive:
            try:
                trace_info = archive.getinfo("trace.trace")
            except KeyError as exc:
                raise ValueError(f"{path.name} does not contain trace.trace") from exc
            if trace_info.file_size > 64 * 1024 * 1024:
                raise ValueError(f"{path.name} trace.trace exceeds the 64 MiB analysis limit")
            with archive.open(trace_info) as stream:
                for raw_line in stream:
                    try:
                        event = json.loads(raw_line)
                    except (UnicodeDecodeError, json.JSONDecodeError):
                        continue
                    if not isinstance(event, dict):
                        continue
                    event_type = event.get("type")
                    if isinstance(event_type, str):
                        event_counts[event_type] += 1
                    if event_type == "before" and event.get("callId") is not None:
                        before_calls[event["callId"]] = event
                    error = event.get("error")
                    if event_type != "after" or not isinstance(error, dict):
                        continue
                    error_name = error.get("name")
                    if not isinstance(error_name, str) or not error_name:
                        continue
                    error_name = error_name[:80]
                    error_type_counts[error_name] += 1
                    call = before_calls.get(event.get("callId"), {})
                    params = call.get("params", {})
                    if not isinstance(params, dict):
                        params = {}
                    timeout = params.get("timeout")
                    timeout_ms = timeout if isinstance(timeout, int) and 0 < timeout <= 300000 else None
                    method = call.get("method")
                    method = method[:80] if isinstance(method, str) else "unknown"
                    call_class = call.get("class")
                    call_class = call_class[:80] if isinstance(call_class, str) else "unknown"
                    selector = params.get("selector")
                    selector_kind = _selector_kind(selector) if selector is not None else None
                    signature = (call_class, method, timeout_ms, selector_kind)
                    error_signatures[signature] += 1
                    message = error.get("message")
                    if isinstance(message, str):
                        category_hint = _classify_trace_error(message)
                        if category_hint:
                            category_hint_counts[category_hint] += 1
    except (OSError, zipfile.BadZipFile) as exc:
        raise ValueError(f"Cannot read Playwright trace archive {path.name}") from exc

    return {
        "event_counts": dict(sorted(event_counts.items())),
        "error_type_counts": dict(sorted(error_type_counts.items())),
        "error_event_count": sum(error_type_counts.values()),
        "error_signatures": [
            {
                "call_class": call_class,
                "method": method,
                "timeout_ms": timeout_ms,
                "selector_kind": selector_kind,
                "error_count": count,
            }
            for (call_class, method, timeout_ms, selector_kind), count
            in error_signatures.most_common(12)
        ],
        "category_hint_counts": dict(sorted(category_hint_counts.items())),
    }


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _captured_at(path: Path) -> str | None:
    match = _TIMESTAMP_PATTERN.search(path.name)
    if not match:
        return None
    try:
        return datetime.strptime(match.group(1), "%Y%m%dT%H%M%S%f").isoformat()
    except ValueError:
        return None


def _record_context(
    test_case_id: str,
    sources: dict[str, Any],
) -> tuple[dict[str, Any], dict[str, Any]]:
    risk = sources["risk_rows"].get(test_case_id)
    suite = sources["suite_rows"].get(test_case_id)
    risk_case = risk.get("test_case", {}) if isinstance(risk, dict) else {}
    if not isinstance(risk_case, dict):
        risk_case = {}
    suite = suite if isinstance(suite, dict) else {}

    fields = (
        "title", "module", "type", "priority", "technique", "technique_variant",
        "flow", "preconditions", "given", "steps", "expected_result",
        "trace_scenario_ids", "trace_rule_ids", "scenario_description",
    )
    merged = {key: suite.get(key, risk_case.get(key)) for key in fields}
    rule_ids = _safe_id_list(
        suite.get("trace_rule_ids"), risk_case.get("trace_rule_ids")
    )
    scenario_ids = _safe_id_list(
        suite.get("trace_scenario_ids"), risk_case.get("trace_scenario_ids")
    )
    exact_rules = [sources["rules"][key] for key in rule_ids if key in sources["rules"]]
    exact_scenarios = [
        sources["scenarios"][key] for key in scenario_ids if key in sources["scenarios"]
    ]
    linked_scenarios = {row["scenario_id"] for row in exact_scenarios}
    issues = []
    for issue in sources["issues"]:
        blocks = issue.get("blocks_scenarios", [])
        if linked_scenarios.intersection(blocks if isinstance(blocks, list) else []):
            issues.append({
                "issue_id": issue.get("issue_id"),
                "type": issue.get("type"),
                "description": _safe_text(issue.get("description")),
                "status": "UNVERIFIED",
            })

    test_case = {
        "title": _safe_text(merged.get("title")),
        "module": _safe_text(merged.get("module")),
        "type": _safe_text(merged.get("type")),
        "priority": _safe_text(merged.get("priority")),
        "technique": _safe_text(merged.get("technique")),
        "technique_variant": _safe_text(merged.get("technique_variant")),
        "flow": _safe_text(merged.get("flow")),
        "preconditions": _safe_text_list(merged.get("preconditions")),
        "given": _safe_text_list(merged.get("given")),
        "steps": _safe_text_list(merged.get("steps")),
        "expected": _safe_text(merged.get("expected_result")),
        "expected_review_status": (
            _safe_text(suite.get("expected_review_status"))
            or _safe_text(risk_case.get("expected_review_status"))
            or "UNVERIFIED"
        ),
        "rule_ids": rule_ids,
        "scenario_ids": scenario_ids,
        "scenario_description": _safe_text(merged.get("scenario_description")),
    }
    agent_context = {
        "risk_prioritization": {
            "priority": _safe_text(risk.get("priority")) if risk else None,
            "risk_score": risk.get("S_eff", risk.get("S")) if risk else None,
            "risk_reason": _safe_text(risk.get("rationale")) if risk else None,
            "risk_model_version": sources["versions"]["risk"]["model_version"],
        },
        "requirements": {
            "linked_rules": [
                {
                    "rule_id": row["rule_id"],
                    "statement": _safe_text(row.get("statement")),
                    "category": _safe_text(row.get("category")),
                }
                for row in exact_rules
            ],
            "linked_scenarios": [
                {
                    "scenario_id": row["scenario_id"],
                    "title": _safe_text(row.get("title")),
                    "given": _safe_text_list(row.get("given")),
                    "when": _safe_text(row.get("when")),
                    "then": _safe_text_list(row.get("then")),
                }
                for row in exact_scenarios
            ],
            "scope_decision": "UNAVAILABLE",
            "open_issues": issues,
            "review_status": sources["versions"]["requirements"]["review_status"],
        },
        "context_provenance": {
            "risk_source": sources["versions"]["risk"],
            "test_suite_source": sources["versions"]["test_suite"],
            "requirements_source": sources["versions"]["requirements"],
            "exact_test_id_match": bool(risk or suite),
            "source_version_alignment": "UNVERIFIED",
        },
    }
    return test_case, agent_context


def _parse_runner_observation(row: dict[str, Any]) -> dict[str, Any]:
    candidates = [row.get("actual_result")]
    facts = row.get("observed_facts")
    if isinstance(facts, list):
        candidates.extend(
            fact.get("statement")
            for fact in facts
            if isinstance(fact, dict)
        )
    for candidate in candidates:
        if not isinstance(candidate, str):
            continue
        try:
            parsed = json.loads(candidate)
        except json.JSONDecodeError:
            continue
        if not isinstance(parsed, dict):
            continue
        result: dict[str, Any] = {}
        reason = _safe_text(parsed.get("reason"))
        if reason:
            result["reason"] = reason
        page_title = _safe_text(parsed.get("page_title"), 160)
        if page_title:
            result["page_title"] = page_title
        visible_elements = parsed.get("visible_elements")
        if isinstance(visible_elements, list):
            result["visible_elements"] = [
                {
                    key: safe_value
                    for key in ("tag", "kind", "text", "placeholder", "aria_label")
                    if (safe_value := _safe_text(element.get(key), 160))
                }
                for element in visible_elements[:20]
                if isinstance(element, dict)
            ]
        if result:
            return result
    raw_actual = row.get("actual_result")
    if isinstance(raw_actual, str):
        safe_actual = _safe_text(raw_actual)
        return {"summary": safe_actual} if safe_actual else {}
    return {}


def _load_execution_results(app_root: Path, run_id: str) -> dict[str, Any] | None:
    path = (
        app_root / "agents" / "risk_prioritization" / "outputs"
        / f"execution_results_{run_id}.json"
    )
    if not path.is_file():
        return None
    try:
        document = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError(f"Cannot read execution result source {path.name}") from exc
    if not isinstance(document, dict) or document.get("run_id") != run_id:
        raise ValueError(f"Execution result source {path.name} does not match run {run_id}")
    rows = document.get("execution_results")
    if not isinstance(rows, list):
        raise ValueError(f"Execution result source {path.name} has no execution_results array")
    indexed: dict[tuple[str, str], dict[str, Any]] = {}
    for row in rows:
        if not isinstance(row, dict):
            continue
        test_case_id = row.get("test_case_id")
        attempt = row.get("attempt_id")
        if not isinstance(test_case_id, str) or not test_case_id.strip():
            continue
        attempt_id = f"attempt-{attempt}" if isinstance(attempt, (int, str)) else "attempt-1"
        identity = (test_case_id, attempt_id)
        if identity in indexed:
            raise ValueError(f"Duplicate execution result for {test_case_id}/{attempt_id}")
        indexed[identity] = row
    return {
        "path": path,
        "document": document,
        "rows": indexed,
    }


def _attach_execution_result(
    record: dict[str, Any],
    row: dict[str, Any],
    environment: dict[str, Any],
) -> None:
    status_map = {
        "PASS": "PASSED",
        "PASSED": "PASSED",
        "FAIL": "FAILED",
        "FAILED": "FAILED",
        "ERROR": "ERROR",
        "BLOCKED": "BLOCKED",
        "FLAKY": "FLAKY",
        "MANUAL": "MANUAL",
    }
    source_status = str(row.get("status", "UNKNOWN")).upper()
    status = status_map.get(source_status, "UNKNOWN")
    observation = _parse_runner_observation(row)
    reason = observation.get("reason") or _safe_text(row.get("error_details"))
    expected = _safe_text(row.get("expected_result"))
    if expected:
        record["test_case"]["expected"] = expected
        record["test_case"]["expected_review_status"] = (
            record["test_case"].get("expected_review_status") or "UNVERIFIED"
        )
    actual: dict[str, Any] = {}
    if observation:
        actual["runner_observation"] = observation
    if reason:
        actual["runner_reason"] = reason
    record["actual"].update(actual)

    evidence_manifest = record.get("evidence_manifest", [])
    by_ref = {
        item.get("artifact_ref"): item.get("evidence_id")
        for item in evidence_manifest
        if isinstance(item, dict)
    }
    for fact_index, fact in enumerate(row.get("observed_facts", [])):
        if not isinstance(fact, dict):
            continue
        statement = _safe_text(fact.get("statement"))
        if not statement:
            continue
        try:
            parsed = json.loads(statement)
        except json.JSONDecodeError:
            parsed = None
        fact_reason = parsed.get("reason") if isinstance(parsed, dict) else None
        refs = []
        for reference in fact.get("evidence_refs", []):
            if isinstance(reference, dict):
                evidence_id = by_ref.get(reference.get("path"))
                if evidence_id:
                    refs.append(evidence_id)
        if fact_reason:
            record.setdefault("runner_observations", []).append({
                "fact": _safe_text(fact_reason),
                "evidence_refs": refs,
                "source": "observed_facts",
            })

    failure_point = row.get("failure_point")
    if isinstance(failure_point, dict):
        record["execution"]["failed_step"] = _safe_text(
            failure_point.get("step") or failure_point.get("description")
        )
    elif isinstance(failure_point, str):
        record["execution"]["failed_step"] = _safe_text(failure_point)
    error_details = row.get("error_details")
    error_type = error_details.get("type") if isinstance(error_details, dict) else None
    record["execution"].update({
        "runner": _safe_text(row.get("executor_type")) or "unknown",
        "runner_version": environment.get("playwright_version"),
        "status": status,
        "source_status": source_status,
        "status_source": "execution_results_file",
        "status_inferred": False,
        "error_type": _safe_text(error_type),
        "error_message": reason,
        "failed_step": record["execution"].get("failed_step")
        or _safe_text(row.get("failure_point")),
        "started_at": _safe_text(row.get("start_time")),
        "ended_at": _safe_text(row.get("end_time")),
        "duration_ms": row.get("duration_ms"),
    })
    record["environment"].update({
        key: _safe_text(value)
        for key, value in environment.items()
        if key in {"base_url", "browser", "playwright_version"} and isinstance(value, str)
    })


def load_run_evidence(run_dir: str | Path, project_id: str = "unknown-project") -> dict[str, Any]:
    """Load run evidence and exact-ID context from local agent outputs."""
    root = Path(run_dir)
    evidence_root = root / "evidence"
    if not root.is_dir():
        raise ValueError(f"Run directory does not exist: {root}")
    if not evidence_root.is_dir():
        raise ValueError(f"Run has no evidence directory: {evidence_root}")

    input_warnings: list[str] = []
    app_root = root.parents[2]
    sources = _load_context_sources(app_root, input_warnings)
    execution_source = _load_execution_results(app_root, root.name)
    execution_rows = execution_source["rows"] if execution_source else {}
    execution_environment = (
        execution_source["document"].get("environment", {})
        if execution_source else {}
    )
    if not execution_source:
        input_warnings.append("No execution_results file matched this run ID.")
    run_date = root.name[:8] if re.match(r"^\d{8}", root.name) else None
    for name, metadata in sources["versions"].items():
        generated = metadata.get("generated_at")
        source_date = generated[:10].replace("-", "") if isinstance(generated, str) else None
        if run_date and source_date and source_date < run_date:
            input_warnings.append(
                f"{name} context source predates run {root.name}; its expected/version alignment is unverified."
            )

    tests: list[dict[str, Any]] = []
    evidence_number = 0
    for test_dir in sorted(path for path in evidence_root.iterdir() if path.is_dir()):
        test_case, agent_context = _record_context(test_dir.name, sources)
        attempt_dirs = sorted(path for path in test_dir.iterdir() if path.is_dir())
        pending_records = []
        for attempt_dir in attempt_dirs:
            match = _ATTEMPT_PATTERN.fullmatch(attempt_dir.name)
            if not match:
                continue
            attempt_id = f"attempt-{match.group(1)}"
            files = sorted(path for path in attempt_dir.iterdir() if path.is_file())
            traces = [path for path in files if path.suffix.lower() == ".zip"]
            screenshots = [
                path for path in files
                if path.suffix.lower() in {".png", ".jpg", ".jpeg"}
            ]

            trace_summaries = [_read_trace_summary(path) for path in traces]
            event_counts: Counter[str] = Counter()
            error_type_counts: Counter[str] = Counter()
            error_signatures: Counter[tuple[str, str, int | None, str | None]] = Counter()
            category_hint_counts: Counter[str] = Counter()
            for summary in trace_summaries:
                event_counts.update(summary["event_counts"])
                error_type_counts.update(summary["error_type_counts"])
                category_hint_counts.update(summary["category_hint_counts"])
                for signature in summary["error_signatures"]:
                    key = (
                        signature["call_class"], signature["method"],
                        signature["timeout_ms"], signature["selector_kind"],
                    )
                    error_signatures[key] += signature["error_count"]

            execution_row = execution_rows.get((test_dir.name, attempt_id))
            status = (
                "FAILED"
                if screenshots
                else "UNKNOWN"
            )
            manifest = []
            trace_evidence_ids = []
            for evidence_file in files:
                evidence_number += 1
                kind = (
                    "playwright-trace"
                    if evidence_file in traces
                    else "screenshot"
                    if evidence_file in screenshots
                    else "other"
                )
                evidence_id = f"ev-{evidence_number:04d}"
                manifest.append({
                    "evidence_id": evidence_id,
                    "kind": kind,
                    "artifact_ref": evidence_file.relative_to(root).as_posix(),
                    "sha256": _sha256(evidence_file),
                    "captured_at": _captured_at(evidence_file),
                    "run_id": root.name,
                    "attempt_id": attempt_id,
                    "build_id": None,
                    "redaction_status": "RAW_LOCAL_ONLY_NOT_SENT_TO_MODEL",
                })
                if kind == "playwright-trace":
                    trace_evidence_ids.append(evidence_id)

            trace_summary = {
                "event_counts": dict(sorted(event_counts.items())),
                "error_type_counts": dict(sorted(error_type_counts.items())),
                "error_event_count": sum(error_type_counts.values()),
                "error_signatures": [
                    {
                        "call_class": call_class,
                        "method": method,
                        "timeout_ms": timeout_ms,
                        "selector_kind": selector_kind,
                        "error_count": count,
                    }
                    for (call_class, method, timeout_ms, selector_kind), count
                    in error_signatures.most_common(12)
                ],
                "category_hint_counts": dict(sorted(category_hint_counts.items())),
                "evidence_refs": trace_evidence_ids,
            }
            error_type = None
            error_message = None
            if category_hint_counts:
                error_type = category_hint_counts.most_common(1)[0][0]
                error_message = f"Playwright trace contains {error_type} evidence."
            elif trace_summary["error_event_count"]:
                error_message = (
                    f"Playwright trace contains {trace_summary['error_event_count']} error event(s); "
                    "the trace alone does not establish a root cause."
                )

            record = {
                "attempt_id": attempt_id,
                "test_case_id": test_dir.name,
                "test_case": test_case,
                "agent_context": agent_context,
                "execution": {
                    "runner": "playwright",
                    "runner_version": None,
                    "status": status,
                    "status_source": (
                        "inferred_from_failure_screenshot"
                        if screenshots
                        else "not_available_in_stored_run"
                    ),
                    "status_inferred": bool(screenshots),
                    "error_type": error_type,
                    "error_message": error_message,
                    "started_at": None,
                    "ended_at": None,
                },
                "actual": {},
                "environment": {
                    "browser": None,
                    "base_url": None,
                    "health_snapshot": None,
                    "note": "The stored run does not include authoritative environment metadata.",
                },
                "trace_summary": trace_summary,
                "retry_history": [],
                "evidence_manifest": manifest,
            }
            if execution_row:
                _attach_execution_result(record, execution_row, execution_environment)
            pending_records.append(record)

        history = [
            {
                "attempt_id": row["attempt_id"],
                "status": row["execution"]["status"],
                "status_source": row["execution"]["status_source"],
            }
            for row in pending_records
        ]
        for row in pending_records:
            row["retry_history"] = history
            tests.append(row)

    records_by_identity = {
        (record["test_case_id"], record["attempt_id"]): record
        for record in tests
    }
    for identity, execution_row in execution_rows.items():
        if identity in records_by_identity:
            continue
        test_case, agent_context = _record_context(identity[0], sources)
        record = {
            "attempt_id": identity[1],
            "test_case_id": identity[0],
            "test_case": test_case,
            "agent_context": agent_context,
            "execution": {
                "runner": "playwright",
                "runner_version": None,
                "status": "UNKNOWN",
                "status_source": "not_available_in_stored_run",
                "status_inferred": False,
                "error_type": None,
                "error_message": None,
                "started_at": None,
                "ended_at": None,
            },
            "actual": {},
            "environment": {},
            "trace_summary": {},
            "retry_history": [],
            "evidence_manifest": [],
        }
        _attach_execution_result(record, execution_row, execution_environment)
        record["retry_history"] = [{
            "attempt_id": identity[1],
            "status": record["execution"]["status"],
            "status_source": "execution_results_file",
        }]
        tests.append(record)

    if not tests:
        raise ValueError(f"No evidence with attempt_<n> layout found in {evidence_root}")

    matched_count = sum(bool(record["agent_context"]["context_provenance"]["exact_test_id_match"]) for record in tests)
    if matched_count < len(tests):
        input_warnings.append(
            f"Only {matched_count}/{len(tests)} test records matched test/risk sources by exact ID; "
            "unmatched cases were not joined by name or similarity."
        )
    input_warnings.extend([
        "The run directory has no authoritative per-test result, expected/actual assertion, build ID, or runner version.",
        "A screenshot indicates a failure only by the executor's evidence-storage convention; absence of a screenshot is not treated as pass.",
        "Test, risk, and requirement context is joined only by exact IDs; source-version alignment with this run is unverified.",
        "Trace signatures are metadata-only. Raw traces, screenshots, cookies, headers, URLs, DOM, and network payloads are not sent to Gemini.",
    ])
    return {
        "schema_version": "1.0",
        "project_id": project_id,
        "run_id": root.name,
        "input_source": (
            "stored-run-evidence-plus-execution-results-and-exact-id-agent-context"
            if execution_source else "stored-run-evidence-plus-exact-id-agent-context"
        ),
        "source_versions": sources["versions"],
        "input_warnings": list(dict.fromkeys(input_warnings)),
        "tests": tests,
    }
