"""Risk-signal loading (RTM, change log, defects, dependencies) and input hash."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

from AI.app.agents.risk_prioritization.data.models import DefectRecord, RiskSignals


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
        change_log=_change_labels(read("change_log.json")),
        defects=[DefectRecord(**x) for x in defects] if defects is not None else None,
        dependencies=read("dependencies.json"),
    )


CHURN_LEVELS = [(6, "core"), (4, "major"), (2, "minor"), (1, "indirect")]


def _change_labels(raw) -> dict | None:
    """Accepts "M07" -> "major" or the per-file churn list; aggregates files per module."""
    if raw is None or isinstance(raw, dict):
        return raw
    totals: dict[str, int] = {}
    for rec in raw:
        mod = rec.get("module")
        if mod:
            totals[mod] = totals.get(mod, 0) + rec["change_count"]
    return {m: next(label for floor, label in CHURN_LEVELS if n >= floor)
            for m, n in totals.items() if n}


# Kept for the standalone extractor script which writes per-file churn with a module field.

def input_hash(test_cases: list[dict], analysis: dict | None, signals: RiskSignals, config: dict) -> str:
    payload = {
        "tc": test_cases,
        "analysis": (analysis or {}).get("source_document", {}).get("sha256") if analysis else None,
        "signals": signals.model_dump(),
        "config": config,
    }
    raw = json.dumps(payload, sort_keys=True, ensure_ascii=False, default=str)
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()
