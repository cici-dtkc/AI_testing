"""Risk-signal loading (RTM, change log, defects, dependencies) and input hash."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

from ..data.models import DefectRecord, RiskSignals


def load_signals(directory: str | Path | None) -> RiskSignals:
    """Each file is optional; a missing file means 'source missing' (defaults apply)."""
    if not directory:
        return RiskSignals()
    d = Path(directory)

    def read(name):
        p = d / name
        return json.loads(p.read_text(encoding="utf-8")) if p.exists() else None

    defects = read("defects.json")
    return RiskSignals(
        rtm=read("rtm.json"),
        change_log=read("change_log.json"),
        defects=[DefectRecord(**x) for x in defects] if defects is not None else None,
        dependencies=read("dependencies.json"),
    )


def input_hash(test_cases: list[dict], analysis: dict | None, signals: RiskSignals, config: dict) -> str:
    payload = {
        "tc": test_cases,
        "analysis": (analysis or {}).get("source_document", {}).get("sha256") if analysis else None,
        "signals": signals.model_dump(),
        "config": config,
    }
    raw = json.dumps(payload, sort_keys=True, ensure_ascii=False, default=str)
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()
