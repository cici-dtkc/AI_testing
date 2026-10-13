from app.state.models import SharedState


def run_test_case_generator(llm, state: SharedState) -> dict:
    cases = llm.ask(
        "Bạn là Test Case Generator Agent. Sinh danh sách test case "
        "có id, bước và kết quả mong đợi.",
        state["test_plan"],
    )
    return {
        "test_cases": cases,
        "trace": ["test_case_generator: đã ghi test_cases"],
    }
