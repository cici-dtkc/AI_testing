import json
import shutil
from datetime import datetime
from pathlib import Path

from .store import BASE_DIR

DEFAULT_CONFIG = {"keep_last_runs": 10, "keep_failures_days": 30, "keep_passes_days": 7}


def cleanup_old_runs(base_dir: str, keep_last_n: int) -> list[str]:
    runs = _run_dirs(Path(base_dir))
    runs.sort(key=lambda p: p.stat().st_mtime)
    victims = runs[:-keep_last_n] if keep_last_n > 0 else runs
    return [soft_delete(run) for run in victims]


def soft_delete(run_dir: Path) -> str:
    target = run_dir.with_name(run_dir.name + ".deleted")
    if target.exists():
        shutil.rmtree(target)
    run_dir.rename(target)
    return run_dir.name


def apply_retention_policy(config: dict) -> dict:
    cfg = {**DEFAULT_CONFIG, **config}
    base = Path(cfg.get("base_dir") or BASE_DIR)
    before = _size(base)

    deleted = cleanup_old_runs(str(base), cfg["keep_last_runs"])

    now = datetime.now().timestamp()
    for run in _run_dirs(base):
        days = cfg["keep_failures_days"] if _has_failures(run) else cfg["keep_passes_days"]
        if now - run.stat().st_mtime > days * 86400:
            deleted.append(soft_delete(run))

    return {"deleted_runs": len(deleted), "reclaimed_mb": round((before - _size(base)) / 1048576, 2)}


def _run_dirs(base: Path) -> list[Path]:
    if not base.is_dir():
        return []
    return [d for d in base.iterdir() if d.is_dir() and not d.name.endswith(".deleted")]


def _size(base: Path) -> int:
    return sum(f.stat().st_size for run in _run_dirs(base) for f in run.rglob("*") if f.is_file())


def _has_failures(run_dir: Path) -> bool:
    meta = run_dir / "metadata.json"
    if not meta.is_file():
        return False
    entries = json.loads(meta.read_text(encoding="utf-8"))
    return any(k in e.get("uri", "").lower() for e in entries for k in ("fail", "error"))
