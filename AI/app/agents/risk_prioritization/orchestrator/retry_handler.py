from .models import ExecutionState


class RetryHandler:
    def __init__(self, max_retries: int = 1):
        self.max_retries = max_retries

    def should_retry(self, tc_id: str, attempt: int, state: ExecutionState) -> bool:
        return attempt < self.max_retries and state == ExecutionState.FAILED

    def determine_final_state(self, attempt: int, states: list[ExecutionState]) -> ExecutionState:
        if attempt == 1 and states[0] == ExecutionState.PASSED:
            return ExecutionState.PASSED
        if attempt == 2:
            if states[0] == ExecutionState.FAILED and states[1] == ExecutionState.PASSED:
                return ExecutionState.FLAKY
            if states[0] == ExecutionState.FAILED and states[1] == ExecutionState.FAILED:
                return ExecutionState.FAILED
        return ExecutionState.ERROR
