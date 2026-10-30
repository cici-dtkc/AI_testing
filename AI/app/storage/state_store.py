 
from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Mapping


def _default(o: Any) -> Any:
    """JSON fallback: pydantic models -> dict, dataclasses -> dict, else str."""
    if hasattr(o, "model_dump"):
        return o.model_dump()
    if hasattr(o, "__dict__") and not isinstance(o, type):
        return {k: v for k, v in o.__dict__.items() if not k.startswith("_")}
    return str(o)


def serialize_state(state: Mapping[str, Any]) -> dict[str, Any]:
    """Return a JSON-safe copy of ``state`` (drops None values)."""
    return {k: v for k, v in state.items() if v is not None}


def save_state(state: Mapping[str, Any], path: str | Path) -> None:
    """Write a full state snapshot to ``path`` as pretty JSON."""
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    data = serialize_state(state)
    p.write_text(
        json.dumps(data, ensure_ascii=False, indent=2, default=_default),
        encoding="utf-8",
    )


def save_checkpoint(state: Mapping[str, Any], run_dir: str | Path, step: str) -> Path:
    """Write ``<run_dir>/checkpoints/<step>.json`` and return its path."""
    cp_dir = Path(run_dir) / "checkpoints"
    cp_dir.mkdir(parents=True, exist_ok=True)
    cp = cp_dir / f"{step}.json"
    save_state(state, cp)
    return cp


def load_state(path: str | Path) -> dict[str, Any]:
    """Read a state snapshot; returns ``{}`` if the file is absent or invalid."""
    p = Path(path)
    if not p.exists():
        return {}
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}
