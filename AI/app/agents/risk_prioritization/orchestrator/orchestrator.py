from datetime import datetime, timezone
from uuid import uuid4
from concurrent.futures import ThreadPoolExecutor, as_completed
from .models import ExecutionState, ExecutionResult, OrchestratorReport
from .scheduler import build_execution_plan
from .state_manager import StateManager
from .retry_handler import RetryHandler
from ..config.config import OrchestratorConfig
from ..evidence_store.metadata import MetadataIndex
from ..executors.playwright.playwright_executor import PlaywrightExecutor
from ..reporting.report import generate_html_report


def _get_executor(suggested_runner: str):
    if suggested_runner == "api":
        try:
            from AI.app.agents.risk_prioritization.executors.api.api_executor import ApiExecutor
            return ApiExecutor()
        except ImportError:
            pass
    return PlaywrightExecutor()


def run_orchestrator(prioritization_result: dict, executor_factory=None, config: OrchestratorConfig | None = None,
                     report_path: str | None = None) -> OrchestratorReport:
    config = config or OrchestratorConfig()
    if executor_factory is None:
        executor_factory = lambda tc_item: _get_executor(tc_item.get("suggested_runner", "ui"))
    run_id = str(uuid4())
    started_at = datetime.now(timezone.utc)

    plan = build_execution_plan(prioritization_result, config.max_workers)
    state_mgr = StateManager(run_id)
    retry_handler = RetryHandler(max_retries=1)

    tc_map = {item["test_case_id"]: item for item in prioritization_result["items"]}
    results = {}
    blocked_tcs = set()

    for tc_id in tc_map:
        state_mgr.set_state(tc_id, ExecutionState.PENDING)

    for group in plan.groups:
        with ThreadPoolExecutor(max_workers=plan.max_workers) as executor:
            futures = {}
            for tc_id in group:
                if tc_id in blocked_tcs:
                    continue
                tc_item = tc_map[tc_id]
                futures[executor.submit(_execute_tc, tc_id, tc_item, run_id, state_mgr, retry_handler, executor_factory)] = tc_id

            for future in as_completed(futures):
                tc_id = futures[future]
                result = future.result()
                results[tc_id] = result

                if result.state in (ExecutionState.FAILED, ExecutionState.ERROR):
                    deps = _find_all_dependents(tc_id, tc_map)
                    blocked_tcs.update(deps)

    for tc_id in blocked_tcs:
        now = datetime.now(timezone.utc)
        blocker = _find_blocking_tc(tc_id, tc_map, results)
        results[tc_id] = ExecutionResult(
            test_case_id=tc_id, state=ExecutionState.BLOCKED, attempt=1,
            start_time=now, end_time=now, duration_ms=0,
            priority=tc_map[tc_id].get("priority"), blocked_by=blocker
        )
        state_mgr.set_state(tc_id, ExecutionState.BLOCKED, blocked_by=blocker)

    completed_at = datetime.now(timezone.utc)
    total_duration_ms = int((completed_at - started_at).total_seconds() * 1000)

    result_list = [results[tc_id] for tc_id in tc_map]
    state_counts = {}
    for r in result_list:
        state_counts[r.state.value] = state_counts.get(r.state.value, 0) + 1

    report = OrchestratorReport(
        run_id=run_id, started_at=started_at, completed_at=completed_at,
        total_duration_ms=total_duration_ms, total_tcs=len(result_list),
        state_counts=state_counts, results=result_list
    )

    generate_html_report(
        report, MetadataIndex.load(run_id),
        report_path or f"outputs/execution_report_{run_id}.html"
    )

    return report


def _execute_tc(tc_id, tc_item, run_id, state_mgr, retry_handler, executor_factory):
    state_mgr.set_state(tc_id, ExecutionState.RUNNING)
    exec_instance = executor_factory(tc_item)
    tc = {**tc_item.get("test_case", {}), "id": tc_id, "priority": tc_item.get("priority")}

    states = []
    result = None

    for attempt in range(1, 3):
        result = exec_instance.execute_test_case(tc, run_id, attempt)
        states.append(result.state)

        if not retry_handler.should_retry(tc_id, attempt, result.state):
            break

    final_state = retry_handler.determine_final_state(len(states), states)
    result.state = final_state
    result.priority = tc_item.get("priority")
    state_mgr.set_state(tc_id, final_state)

    return result


def _find_all_dependents(tc_id, tc_map):
    dependents = set()
    for tid, item in tc_map.items():
        if tc_id in item.get("depends_on", []):
            dependents.add(tid)
            dependents.update(_find_all_dependents(tid, tc_map))
    return dependents


def _find_blocking_tc(tc_id, tc_map, results):
    deps = tc_map[tc_id].get("depends_on", [])
    for dep in deps:
        if dep in results and results[dep].state in (ExecutionState.FAILED, ExecutionState.ERROR):
            return [dep]
    return []