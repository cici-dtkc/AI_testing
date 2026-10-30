 
from __future__ import annotations

import json
import os
import time
from pathlib import Path
from typing import Any


def _now() -> str:
    return time.strftime("%Y%m%d-%H%M%S")


class RunStore:
    """Creates and names files/directories for one supervisor pipeline run."""

    def __init__(self, root: str | os.PathLike[str] = "app/storage/runs") -> None:
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)

    def create_run(
        self,
        request: str = "",
        config: dict[str, Any] | None = None,
    ) -> tuple[str, Path]:
        """Create a new run directory and return ``(run_id, run_dir)``."""
        run_id = _now()
        run_dir = self.root / run_id
        i = 1
        while run_dir.exists():
            run_id = f"{_now()}-{i}"
            run_dir = self.root / run_id
            i += 1
        run_dir.mkdir(parents=True)
        self.save_meta(run_id, {"request": request, "config": config or {}, "started_at": run_id})
        return run_id, run_dir

    def run_dir(self, run_id: str) -> Path:
        return self.root / run_id

    def _subdir(self, run_dir: Path, name: str) -> Path:
        d = Path(run_dir) / name
        d.mkdir(parents=True, exist_ok=True)
        return d

    def analysis_dir(self, run_dir: Path) -> Path:
        return self._subdir(run_dir, "analysis")

    def test_cases_dir(self, run_dir: Path) -> Path:
        return self._subdir(run_dir, "test_cases")

    def prioritized_dir(self, run_dir: Path) -> Path:
        return self._subdir(run_dir, "prioritized")

    def executor_dir(self, run_dir: Path) -> Path:
        return self._subdir(run_dir, "executor")

    def checkpoints_dir(self, run_dir: Path) -> Path:
        return self._subdir(run_dir, "checkpoints")

    def analysis_path(self, run_dir: Path) -> Path:
        return Path(run_dir) / "analysis" / "analysis.json"

    def analysis_report_path(self, run_dir: Path) -> Path:
        return Path(run_dir) / "analysis" / "report.html"

    def test_cases_path(self, run_dir: Path) -> Path:
        return Path(run_dir) / "test_cases" / "test_cases.json"

    def prioritized_path(self, run_dir: Path) -> Path:
        return Path(run_dir) / "prioritized" / "prioritized_tests.json"

    def executor_log_path(self, run_dir: Path, run_label: str) -> Path:
        return self.executor_dir(run_dir) / f"{run_label}.log"

    def state_path(self, run_dir: Path) -> Path:
        return Path(run_dir) / "state.json"

    def meta_path(self, run_dir: Path) -> Path:
        return Path(run_dir) / "meta.json"

    def report_path(self, run_dir: Path) -> Path:
        return Path(run_dir) / "final_report.html"

    def save_meta(self, run_id: str, meta: dict[str, Any]) -> None:
        self._write_json(self.meta_path(self.root / run_id), meta)

    def finalize_meta(self, run_id: str, **fields: Any) -> None:
        p = self.meta_path(self.root / run_id)
        meta: dict[str, Any] = {}
        if p.exists():
            meta = json.loads(p.read_text(encoding="utf-8"))
        meta.update(fields)
        self._write_json(p, meta)

    def save_executor_log(self, run_dir: Path, run_label: str, log: str) -> None:
        self.executor_log_path(run_dir, run_label).write_text(log, encoding="utf-8")

    def list_runs(self) -> list[str]:
        if not self.root.exists():
            return []
        return sorted((p.name for p in self.root.iterdir() if p.is_dir()), reverse=True)

    @staticmethod
    def _write_json(path: Path, data: Any) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(
            json.dumps(data, ensure_ascii=False, indent=2, default=str),
            encoding="utf-8",
        )
