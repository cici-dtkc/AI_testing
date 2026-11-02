 
from __future__ import annotations

import os
import time
from pathlib import Path
from typing import Any, Callable

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph

from app.agents.executor import create_executor
from app.agents.requirement_analyzer import run_requirement_analyzer, run_srs_analysis
from app.agents.risk_prioritization import run_risk_prioritization
from app.agents.test_case_generator import run_test_case_generator
from app.state.models import SharedState, merge_state
from app.storage import RunStore, save_checkpoint, save_state, write_report
from app.storage.log import get_logger

MAX_RETRY = 2
RECURSION_LIMIT = 40


def _dispatch(agent: str, reason: str) -> dict:
    return {"next_agent": agent, "trace": [f"supervisor -> {agent}: {reason}"]}


def _route(state: SharedState, llm) -> dict:
    """Rule cascade deciding which agent runs next (or END)."""
    if state.get("report"):
        return _dispatch(END, "đã có report; kết thúc workflow")

    # --- generation chain: SRS -> Analysis -> TestSuite -> ranking ---
    if state.get("srs_path") and not state.get("analysis_path"):
        return _dispatch("srs_analyzer", "có SRS, chưa phân tích yêu cầu")
    if not state.get("test_cases"):
        if not state.get("analysis_path"):
            return _dispatch(END, "không có analysis/SRS để sinh test_cases")
        return _dispatch("test_case_generator", "đã có analysis, chưa có test_cases")
    if not state.get("risk_priorities"):
        return _dispatch("risk_prioritization", "đã có test_cases, chưa xếp hạng rủi ro")
    if not state.get("test_results"):
        return _dispatch("executor", "đã xếp hạng, chưa chạy test")

    latest = state["test_results"][f"run{state.get('retry_count', 0)}"]
    if latest["status"] == "PASS":
        return _dispatch(END, "kết quả kiểm thử PASS")
    if state.get("retry_count", 0) >= MAX_RETRY:
        return _dispatch(END, "đã hết lượt thử lại")
    if not state.get("defects"):
        return _dispatch("requirement_analyzer", "FAIL và chưa phân tích defects")

    decision = llm.route(
        f"Phân tích lỗi:\n{state['defects']}\n"
        f"Ưu tiên rủi ro:\n{state['risk_priorities']}\n"
        f"Đã chạy lại {state.get('retry_count', 0)} lần. "
        "Nên chạy lại (executor) hay kết thúc workflow?"
    )
    update = {
        "next_agent": decision.next,
        "trace": [f"supervisor: {decision.next} ({decision.reason})"],
    }
    if decision.next == "executor":
        update["retry_count"] = 1            # reducer is operator.add -> increments
        update["defects"] = ""                # clear so the analyzer runs again next FAIL
    return update


def _finish(state: SharedState, reason: str, store: RunStore) -> dict:
    """Finalize storage on END; returns extra state fields (``report``)."""
    run_dir = state.get("run_dir")
    if not run_dir:
        return {}
    rd = Path(run_dir)
    report_path = write_report(rd, state)
    save_state({**state, "report": str(report_path)}, store.state_path(rd))
    store.finalize_meta(
        state.get("run_id", ""),
        status="done",
        finished_reason=reason,
        finished_at=time.strftime("%Y-%m-%dT%H:%M:%S"),
    )
    log = get_logger(rd)
    if log:
        finish_reason = reason.split(": ", 1)[-1]
        log.finish(finish_reason)
        log.artifact("state", store.state_path(rd))
        log.artifact("báo cáo", report_path)
        log.artifact("pipeline log", rd / "pipeline.log")
    return {"report": str(report_path)}


def build_supervisor(llm, store: RunStore, tests_dir: str) -> Callable[[SharedState], dict]:
    def supervisor(state: SharedState) -> dict:
        has_work = bool(
            state.get("srs_path") or state.get("analysis_path")
            or state.get("test_cases") or state.get("test_results")
        )
        # bootstrap run directory on first entry (side effect owned by the supervisor)
        bootstrap: dict[str, Any] = {}
        if has_work and not state.get("run_dir"):
            run_id, run_dir = store.create_run(
                request=state.get("request", ""),
                config={
                    "tests_dir": tests_dir,
                    "model": os.getenv("GEMINI_MODEL", "gemini-flash-latest"),
                },
            )
            bootstrap = {"run_id": run_id, "run_dir": str(run_dir)}
            state = {**state, **bootstrap}
            log = get_logger(run_dir)
            if log:
                log.info(f"bắt đầu run {run_id} — model {os.getenv('GEMINI_MODEL','?')}")
                if state.get("srs_path"):
                    log.info(f"SRS đầu vào: {state['srs_path']}")

        log = get_logger(state.get("run_dir"))
        decision = _route(state, llm)
        if decision["next_agent"] == END:
            return {
                **bootstrap, **decision,
                **_finish(merge_state(state, decision), decision["trace"][0], store),
            }
        if log:
            log.dispatch(decision["next_agent"], decision["trace"][0].split(": ", 1)[-1])
        return {**bootstrap, **decision}

    return supervisor


def _persist(name: str, fn: Callable[[SharedState], dict]) -> Callable[[SharedState], dict]:
    """Wrap an agent node: log start/done and write a state checkpoint after it runs."""

    def run(state: SharedState) -> dict:
        log = get_logger(state.get("run_dir"))
        if log:
            log.start(name)
        update = fn(state)
        run_dir = state.get("run_dir") or update.get("run_dir")
        if run_dir:
            save_checkpoint(merge_state(state, update), Path(run_dir), name)
        if log and update.get("trace"):
            log.done(name, update["trace"][-1])
        return update

    return run


def _make_tc_generator(llm) -> Callable[[SharedState], dict]:
    def run(state: SharedState) -> dict:
        enriched = dict(state)
        if state.get("run_dir"):
            enriched["output_path"] = str(Path(state["run_dir"]) / "test_cases")
        return run_test_case_generator(llm, enriched)

    return run


def _make_risk(llm) -> Callable[[SharedState], dict]:
    def run(state: SharedState) -> dict:
        kwargs: dict[str, Any] = {}
        if state.get("analysis_path"):
            kwargs["analysis_path"] = state["analysis_path"]
        if state.get("run_dir"):
            kwargs["out_dir"] = str(Path(state["run_dir"]) / "prioritized")
        return run_risk_prioritization(llm, state, **kwargs)

    return run


def _make_executor(tests_dir: str, store: RunStore) -> Callable[[SharedState], dict]:
    executor = create_executor(tests_dir)

    def run(state: SharedState) -> dict:
        run_label = f"run{state.get('retry_count', 0)}"
        update = executor(state)
        if state.get("run_dir"):
            store.save_executor_log(
                Path(state["run_dir"]), run_label,
                update["test_results"][run_label]["log_tail"],
            )
        return update

    return run


def build_graph(llm, tests_dir: str, storage_root: str = "app/storage/runs"):
    store = RunStore(storage_root)
    graph = StateGraph(SharedState)
    graph.add_node("supervisor", build_supervisor(llm, store, tests_dir))
    graph.add_node("srs_analyzer", _persist("srs_analyzer", lambda s: run_srs_analysis(llm, s)))
    graph.add_node(
        "test_case_generator",
        _persist("test_case_generator", _make_tc_generator(llm)),
    )
    graph.add_node(
        "risk_prioritization",
        _persist("risk_prioritization", _make_risk(llm)),
    )
    graph.add_node("executor", _persist("executor", _make_executor(tests_dir, store)))
    graph.add_node(
        "requirement_analyzer",
        _persist("requirement_analyzer", lambda s: run_requirement_analyzer(llm, s)),
    )

    for name in (
        "srs_analyzer",
        "test_case_generator",
        "risk_prioritization",
        "executor",
        "requirement_analyzer",
    ):
        graph.add_edge(name, "supervisor")
    graph.add_edge(START, "supervisor")
    graph.add_conditional_edges("supervisor", lambda state: state["next_agent"])
    return graph.compile(checkpointer=InMemorySaver())
