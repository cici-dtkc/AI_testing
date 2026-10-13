from app.runners.pytest_runner import run_pytest
from app.state.models import SharedState


def create_executor(tests_dir: str):
    def run_executor(state: SharedState) -> dict:
        run_id = f"run{state.get('retry_count', 0)}"
        status, log = run_pytest(tests_dir)
        return {
            "test_results": {run_id: {"status": status, "log_tail": log}},
            "trace": [f"executor: {run_id} -> {status}"],
        }

    return run_executor
