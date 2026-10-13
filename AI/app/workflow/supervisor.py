from __future__ import annotations

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph

from app.agents.requirement_analyzer import run_requirement_analyzer
from app.agents.executor import create_executor
from app.agents.risk_prioritization import run_risk_prioritization
from app.agents.test_case_generator import run_test_case_generator
from app.agents.planner import run_planner
from app.agents.reporter import run_reporter
from app.state.models import SharedState

MAX_RETRY = 2
RECURSION_LIMIT = 40


def build_supervisor(llm):
    def supervisor(state: SharedState) -> dict:
        def dispatch(agent: str, reason: str) -> dict:
            return {
                "next_agent": agent,
                "trace": [f"supervisor -> {agent}: {reason}"],
            }

        if not state.get("test_plan"):
            return dispatch("planner", "chưa có test_plan")
        if not state.get("test_cases"):
            return dispatch("test_case_generator", "đã có test_plan, chưa có test_cases")
        if not state.get("test_results"):
            return dispatch("executor", "đã có test_cases, chưa có test_results")
        if state.get("report"):
            return dispatch(END, "đã có report")

        latest = state["test_results"][f"run{state.get('retry_count', 0)}"]
        if latest["status"] == "PASS":
            return dispatch("reporter", "kết quả kiểm thử PASS")
        if state.get("retry_count", 0) >= MAX_RETRY:
            return dispatch("reporter", "đã hết lượt thử lại")
        if not state.get("defects"):
            return dispatch(
                "requirement_analyzer",
                "kết quả FAIL và chưa phân tích defects",
            )
        if not state.get("risk_priorities"):
            return dispatch(
                "risk_prioritization",
                "đã phân tích defects và chưa xếp hạng rủi ro",
            )

        decision = llm.route(
            f"Phân tích lỗi:\n{state['defects']}\n"
            f"Ưu tiên rủi ro:\n{state['risk_priorities']}\n"
            f"Đã chạy lại {state.get('retry_count', 0)} lần. "
            "Nên chạy lại (executor) hay lập báo cáo (reporter)?"
        )
        update = {
            "next_agent": decision.next,
            "trace": [f"supervisor: {decision.next} ({decision.reason})"],
        }
        if decision.next == "executor":
            update["retry_count"] = 1
            update["defects"] = ""
        return update

    return supervisor


def build_graph(llm, tests_dir: str):
    graph = StateGraph(SharedState)
    graph.add_node("supervisor", build_supervisor(llm))
    agents = {
        "planner": lambda state: run_planner(llm, state),
        "test_case_generator": lambda state: run_test_case_generator(llm, state),
        "executor": create_executor(tests_dir),
        "requirement_analyzer": lambda state: run_requirement_analyzer(llm, state),
        "risk_prioritization": lambda state: run_risk_prioritization(llm, state),
        "reporter": lambda state: run_reporter(llm, state),
    }
    for name, agent in agents.items():
        graph.add_node(name, agent)
        graph.add_edge(name, "supervisor")
    graph.add_edge(START, "supervisor")
    graph.add_conditional_edges("supervisor", lambda state: state["next_agent"])
    return graph.compile(checkpointer=InMemorySaver())
