import copy
import hashlib
import json
from datetime import datetime, timezone
from importlib.metadata import PackageNotFoundError, version
from pathlib import Path

from .models import ExecutionResult, ExecutionState
from ..executors.playwright.config import BASE_URL, BROWSER_TYPE
from ....storage.store import RunStore


OUTPUT_DIR = Path(__file__).resolve().parents[1] / "outputs"

_STATUS_MAP = {
    ExecutionState.PASSED: "PASS",
    ExecutionState.FAILED: "FAIL",
    ExecutionState.BLOCKED: "BLOCKED",
    ExecutionState.ERROR: "ERROR",
    ExecutionState.FLAKY: "FLAKY",
    ExecutionState.MANUAL: "MANUAL",
}


def _iso(dt: datetime) -> str:
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def _status(state: ExecutionState) -> str:
    return _STATUS_MAP.get(state, state.value)


def _evidence(result: ExecutionResult, run_dir: Path) -> list[dict]:
    out = []
    for i, uri in enumerate(result.evidence_uris, 1):
        path = Path(uri)
        try:
            rel = path.relative_to(run_dir).as_posix()
        except ValueError:
            rel = path.name
        out.append({
            "evidence_id": f"EV-{i:03d}",
            "type": path.name.split("_")[0],
            "path": rel,
            "timestamp": _iso(result.end_time),
            "size_bytes": path.stat().st_size if path.is_file() else 0,
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else "",
        })
    return out


def _observed_facts(result: ExecutionResult, evidence: list[dict]) -> list[dict]:
    if result.observed_facts:
        return result.observed_facts
    if result.state in (ExecutionState.FAILED, ExecutionState.ERROR) and result.error_message:
        return [{
            "fact_id": "F1",
            "statement": result.error_message,
            "evidence_refs": evidence[:1],
        }]
    return []


def _to_output_format(result: ExecutionResult, run_dir: Path) -> dict:
    evidence = _evidence(result, run_dir)
    return {
        "test_case_id": result.test_case_id,
        "module": result.module,
        "attempt_id": result.attempt,
        "status": _status(result.state),
        "start_time": _iso(result.start_time),
        "end_time": _iso(result.end_time),
        "duration_ms": result.duration_ms,
        "executor_type": result.executor_type,
        "failure_point": result.failure_point,
        "failure_category": result.failure_category,
        "failure_stage": result.failure_stage,
        "assertion_values": result.assertion_values,
        "observed_facts": _observed_facts(result, evidence),
        "expected_result": result.expected_result,
        "actual_result": result.actual_result,
        "error_details": result.error_details,
        "evidence": evidence,
        "blocked_by": result.blocked_by or [],
        "priority": result.priority,
    }


_ACTIONABLE_STATUSES = ("FAIL", "ERROR", "MANUAL", "FLAKY")

_ACTIONABLE_KEYS = (
    "test_case_id", "module", "attempt_id", "status", "start_time", "end_time",
    "duration_ms", "executor_type", "failure_point", "failure_category",
    "failure_stage", "assertion_values",
    "observed_facts", "expected_result", "actual_result", "error_details",
    "evidence", "blocked_by", "priority",
)


def _trim_statement(statement):
    if not isinstance(statement, str):
        return statement
    try:
        parsed = json.loads(statement)
    except ValueError:
        return statement
    if isinstance(parsed, dict) and isinstance(parsed.get("visible_elements"), list):
        parsed["visible_elements"] = f"{len(parsed['visible_elements'])} elements (trimmed)"
        return json.dumps(parsed, ensure_ascii=False, default=str)
    return statement


def _trim_error_details(error_details):
    if isinstance(error_details, dict) and isinstance(error_details.get("visible_elements"), list):
        error_details["visible_elements"] = f"{len(error_details['visible_elements'])} elements (trimmed)"
    return error_details


def _actionable_result(result: dict) -> dict:
    item = copy.deepcopy(result)
    for fact in item.get("observed_facts") or []:
        if isinstance(fact, dict) and "statement" in fact:
            fact["statement"] = _trim_statement(fact["statement"])
    item["error_details"] = _trim_error_details(item.get("error_details"))
    return {key: item.get(key) for key in _ACTIONABLE_KEYS}


def write_execution_output(run_id: str, results: list[ExecutionResult],
                           test_cases_source: str, analysis_source: str) -> Path:
    run_dir = RunStore().run_dir(run_id)
    statuses = [_status(r.state) for r in results]

    counts = {k: statuses.count(k) for k in ("PASS", "FAIL", "BLOCKED", "ERROR", "FLAKY", "MANUAL")}
    started_at = min((r.start_time.replace(tzinfo=None) for r in results), default=datetime.now())
    completed_at = max((r.end_time.replace(tzinfo=None) for r in results), default=started_at)

    try:
        playwright_version = version("playwright")
    except PackageNotFoundError:
        playwright_version = None

    output = {
        "schema_version": "1.0",
        "run_id": run_id,
        "project_id": "comic-ecommerce",
        "started_at": _iso(started_at),
        "completed_at": _iso(completed_at),
        "total_duration_ms": sum(r.duration_ms for r in results),
        "summary": {
            "total_test_cases": len(results),
            "passed": counts["PASS"],
            "failed": counts["FAIL"],
            "blocked": counts["BLOCKED"],
            "error": counts["ERROR"],
            "flaky": counts["FLAKY"],
            "manual": counts["MANUAL"]
        },
        "execution_results": [_to_output_format(r, run_dir) for r in results],
        "environment": {
            "base_url": BASE_URL,
            "browser": BROWSER_TYPE,
            "playwright_version": playwright_version,
        },
        "provenance": {
            "agent": "risk_prioritization",
            "version": "1.0.0",
            "test_cases_source": test_cases_source,
            "analysis_source": analysis_source,
        },
    }

    output_path = OUTPUT_DIR / f"execution_results_{run_id}.json"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding="utf-8")

    actionable = [_actionable_result(r) for r in output["execution_results"]
                  if r["status"] in _ACTIONABLE_STATUSES]
    actionable_counts = {k: statuses.count(k) for k in _ACTIONABLE_STATUSES}
    actionable_output = {}
    for key, value in output.items():
        if key == "summary":
            actionable_output["summary"] = {
                "total_test_cases": len(actionable),
                "passed": 0,
                "failed": actionable_counts["FAIL"],
                "blocked": 0,
                "error": actionable_counts["ERROR"],
                "flaky": actionable_counts["FLAKY"],
                "manual": actionable_counts["MANUAL"],
            }
            actionable_output["excluded"] = {"passed": counts["PASS"], "blocked": counts["BLOCKED"]}
        elif key == "execution_results":
            actionable_output["execution_results"] = actionable
        else:
            actionable_output[key] = value

    actionable_path = OUTPUT_DIR / f"execution_results_{run_id}_actionable.json"
    actionable_path.parent.mkdir(parents=True, exist_ok=True)
    actionable_path.write_text(json.dumps(actionable_output, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Execution output written to {output_path}")
    print(f"Actionable execution output written to {actionable_path}")
    return output_path
