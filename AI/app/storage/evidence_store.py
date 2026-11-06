from __future__ import annotations

import shutil
from datetime import datetime
from pathlib import Path


class EvidenceStore:
    def __init__(self, run_dir: Path):
        self.run_dir = Path(run_dir)
        self.evidence_dir = self.run_dir / "evidence"
        self.evidence_dir.mkdir(parents=True, exist_ok=True)

    def save_artifact(self, tc_id: str, attempt: int, artifact_type: str, source_path: str) -> str:
        src = Path(source_path)
        if not src.is_file():
            raise FileNotFoundError(str(src))
        dest_dir = self.evidence_dir / tc_id / f"attempt_{attempt}"
        dest_dir.mkdir(parents=True, exist_ok=True)
        stamp = datetime.now().strftime("%Y%m%dT%H%M%S%f")
        dest = dest_dir / f"{artifact_type}_{stamp}{src.suffix}"
        shutil.copy2(src, dest)
        return str(dest)

    def get_evidence(self, tc_id: str) -> list[dict]:
        tc_dir = self.evidence_dir / tc_id
        if not tc_dir.is_dir():
            return []
        entries = []
        for attempt_dir in sorted(tc_dir.glob("attempt_*")):
            attempt = int(attempt_dir.name.split("_", 1)[1])
            for f in sorted(attempt_dir.iterdir()):
                if f.is_file():
                    entries.append({
                        "tc_id": tc_id,
                        "attempt": attempt,
                        "type": f.name.split("_", 1)[0],
                        "path": str(f),
                    })
        return entries

    def cleanup(self) -> None:
        target = self.run_dir / "evidence.deleted"
        if target.exists():
            shutil.rmtree(target)
        self.evidence_dir.rename(target)
