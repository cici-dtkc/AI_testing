from app.state.models import SharedState


def run_requirement_analyzer(llm, state: SharedState) -> dict:
    """Analyze the latest test run and extract requirement-related defects."""
    latest = state["test_results"][f"run{state.get('retry_count', 0)}"]
    defects = llm.ask(
        "Bạn là Requirement Analyzer Agent. Phân tích log kiểm thử "
        "theo yêu cầu và xác định các sai lệch hoặc defect.",
        latest["log_tail"],
    )
    return {
        "defects": defects,
        "trace": ["requirement_analyzer: đã ghi defects"],
    }
