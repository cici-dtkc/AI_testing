from datetime import datetime
from pathlib import Path
from tempfile import TemporaryDirectory

from AI.app.agents.risk_prioritization.evidence_store.api import store_evidence
from AI.app.agents.risk_prioritization.orchestrator.models import ExecutionResult
from .newman_runner import run_collection

class ApiExecutor:
    def execute_test_case(self, tc: dict, run_id: str, attempt: int) -> ExecutionResult:
        tc_id = tc.get("id", "unknown")
        start_time = datetime.now()

        collection_path = tc.get("collection_path")
        if not collection_path:
            test_steps = tc.get("test_steps", "")
            if isinstance(test_steps, str) and ".json" in test_steps:
                collection_path = test_steps.strip()

        if not collection_path or not Path(collection_path).exists():
            end_time = datetime.now()
            duration_ms = int((end_time - start_time).total_seconds() * 1000)
            return ExecutionResult(
                test_case_id=tc_id,
                state="ERROR",
                attempt=attempt,
                start_time=start_time,
                end_time=end_time,
                duration_ms=duration_ms,
                evidence_uris=[],
                error_message=f"Collection file not found: {collection_path}"
            )

        environment_vars = tc.get("environment", {})
        if not environment_vars:
            environment_vars = tc.get("test_data", {})

        with TemporaryDirectory() as tmp:
            result = run_collection(collection_path, environment_vars, tmp)
            evidence_uris = [
                store_evidence(run_id, tc_id, attempt, Path(p).stem, p)
                for p in result.get("report_paths", []) if Path(p).is_file()
            ]

        end_time = datetime.now()
        duration_ms = result.get("duration_ms", 0)

        if result["error"]:
            state = "ERROR"
            error_message = result["error"]
        elif result["passed"]:
            state = "PASSED"
            error_message = None
        else:
            state = "FAILED"
            error_message = f"{result['failed_requests']}/{result['total_requests']} requests failed"

        return ExecutionResult(
            test_case_id=tc_id,
            state=state,
            attempt=attempt,
            start_time=start_time,
            end_time=end_time,
            duration_ms=duration_ms,
            evidence_uris=evidence_uris,
            error_message=error_message
        )
