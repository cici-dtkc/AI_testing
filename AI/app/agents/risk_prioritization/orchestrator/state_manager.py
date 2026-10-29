from datetime import datetime, timezone
from pathlib import Path
import json
from .models import ExecutionState


class StateManager:
    def __init__(self, run_id: str):
        self.run_id = run_id
        self._states = {}

    def set_state(self, tc_id: str, state: ExecutionState, **kwargs):
        now = datetime.now(timezone.utc).isoformat()
        if tc_id not in self._states:
            self._states[tc_id] = {"state": state.value, "timestamp": now, "transitions": []}

        self._states[tc_id]["state"] = state.value
        self._states[tc_id]["timestamp"] = now
        self._states[tc_id]["transitions"].append({"state": state.value, "timestamp": now})

        for k, v in kwargs.items():
            self._states[tc_id][k] = v

    def get_state(self, tc_id: str) -> dict:
        return self._states.get(tc_id, {})

    def get_all_states(self) -> dict[str, dict]:
        return self._states

    def save(self, path: str):
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        Path(path).write_text(json.dumps({"run_id": self.run_id, "states": self._states}, indent=2))

    @classmethod
    def load(cls, path: str) -> "StateManager":
        data = json.loads(Path(path).read_text())
        sm = cls(data["run_id"])
        sm._states = data["states"]
        return sm