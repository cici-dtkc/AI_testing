import json
from pathlib import Path

from .store import BASE_DIR


class MetadataIndex:
    def __init__(self, run_id: str, entries: list[dict] | None = None, base_dir: str | Path | None = None):
        self.run_id = run_id
        self.entries = entries or []
        self.base_dir = Path(base_dir) if base_dir else BASE_DIR

    def add_entry(self, run_id: str, tc_id: str, attempt: int, artifact_type: str,
                  uri: str, sha256: str, timestamp: str) -> None:
        self.entries.append({
            "run_id": run_id,
            "tc_id": tc_id,
            "attempt": attempt,
            "timestamp": timestamp,
            "type": artifact_type,
            "uri": uri,
            "sha256": sha256,
        })

    def get_by_tc(self, run_id: str, tc_id: str) -> list[dict]:
        return [e for e in self.entries if e["run_id"] == run_id and e["tc_id"] == tc_id]

    def get_by_run(self, run_id: str) -> list[dict]:
        return [e for e in self.entries if e["run_id"] == run_id]

    def get_by_type(self, run_id: str, artifact_type: str) -> list[dict]:
        return [e for e in self.entries if e["run_id"] == run_id and e["type"] == artifact_type]

    def save(self) -> None:
        path = self.base_dir / self.run_id / "metadata.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(self.entries, indent=2, ensure_ascii=False), encoding="utf-8")

    @classmethod
    def load(cls, run_id: str, base_dir: str | Path | None = None) -> "MetadataIndex":
        index = cls(run_id, base_dir=base_dir)
        path = index.base_dir / run_id / "metadata.json"
        if path.is_file():
            index.entries = json.loads(path.read_text(encoding="utf-8"))
        return index
