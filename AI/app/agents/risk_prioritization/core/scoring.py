"""Step 3: R, C, H, D, S, S100 -- pure deterministic functions (formulas 1-3)."""
from __future__ import annotations

import math
from typing import Any

from ..config.config import Config
from ..data.mapping import TCContext
from ..data.models import RiskSignals


def _level(label: Any, table: dict) -> float | None:
    if isinstance(label, dict):
        label = label.get("priority")
    return table.get(label.strip().lower()) if label else None


def score_R(ctx: TCContext, signals: RiskSignals, cfg: Config) -> tuple[float, str, bool]:
    """Returns (R, source, defaulted)."""
    rtm = signals.rtm or {}
    for key in (*ctx.use_cases, ctx.module):
        v = _level(rtm.get(key), cfg.r_levels)
        if v is not None:
            return _boost(v, ctx, cfg), f"rtm:{key}", False
    uc = [v for v in (_level(p, cfg.r_levels) for p in ctx.uc_priorities) if v is not None]
    if uc:
        return _boost(max(uc), ctx, cfg), "srs_use_case_priority", False
    v = _level(ctx.original_priority, cfg.r_levels)
    if v is not None:
        return _boost(v, ctx, cfg), "tc_priority_original", True
    return _boost(cfg.defaults["R"], ctx, cfg), "default", True


def _boost(v: float, ctx: TCContext, cfg: Config) -> float:
    v = min(5.0, v + cfg.critical_boost) if ctx.module in cfg.critical_modules else float(v)
    if ctx.test_type in cfg.type_boost:
        v = min(5.0, v + cfg.type_boost[ctx.test_type])
    return v


def score_C(ctx: TCContext, module: str, signals: RiskSignals, cfg: Config) -> tuple[float, str, bool]:
    if signals.change_log is None:
        base_score = float(cfg.defaults["C"])
        defaults_flag = True
        source = "default(no change_log)"
    else:
        label = signals.change_log.get(module)
        if label is None:
            base_score = 1.0
            defaults_flag = False
            source = "no_change_record"
        else:
            base_score = float(cfg.c_levels.get(label.lower(), cfg.defaults["C"]))
            defaults_flag = False
            source = f"change_log:{label}"

    # add structural complexity
    complexity_boost = (
        ctx.precondition_count * cfg.complexity_weights.get("preconditions", 0) +
        ctx.step_count * cfg.complexity_weights.get("steps", 0)
    )
    final_score = min(5.0, base_score + complexity_boost)

    return final_score, source, defaults_flag


def module_defect_weights(signals: RiskSignals, cfg: Config) -> dict[str, float]:
    w: dict[str, float] = {}
    for d in signals.defects or []:
        w[d.module] = w.get(d.module, 0.0) + d.severity * math.exp(-cfg.decay_lambda * d.age_releases)
    return w


def score_H(module: str, wm: dict[str, float], signals: RiskSignals, cfg: Config) -> tuple[float, str, bool]:
    if signals.defects is None:
        return float(cfg.defaults["H"]), "default(no defect history)", True
    wmax = max(wm.values(), default=0.0)
    if wmax == 0:
        return 1.0, "no_defects", False
    return 1 + 4 * wm.get(module, 0.0) / wmax, "defect_history", False


def score_D(k: int, kmax: int, any_deps: bool, cfg: Config) -> tuple[float, bool]:
    if not any_deps:
        return float(cfg.defaults["D"]), True
    return (1.0 if kmax == 0 else 1 + 4 * k / kmax), False


def combine(R: float, C: float, H: float, D: float, cfg: Config) -> tuple[float, float]:
    w = cfg.weights
    S = w["R"] * R + w["C"] * C + w["H"] * H + w["D"] * D
    return round(S, 6), round(25 * (S - 1), 4)


def to_priority(s100: float, cfg: Config) -> str:
    t = cfg.thresholds
    return "P1" if s100 >= t["P1"] else "P2" if s100 >= t["P2"] else "P3" if s100 >= t["P3"] else "P4"
