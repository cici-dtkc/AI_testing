"""Deterministic 6-step pipeline: signals -> mapping -> scoring -> ranking -> dependency -> rationale."""
from __future__ import annotations

from datetime import datetime, timezone

from ..data import dependency as dep
from . import scoring, rationale
from AI.app.agents.risk_prioritization.config.config import Config
from AI.app.agents.risk_prioritization.data.mapping import AnalysisIndex, build_context
from AI.app.agents.risk_prioritization.data.models import PrioritizationResult, RiskSignals, ScoredTC
from .signals import input_hash
from .validation import GuardrailError, validate


def run_pipeline(test_cases: list[dict], analysis: dict | None = None, signals: RiskSignals | None = None,
                 config: Config | None = None, llm=None) -> PrioritizationResult:
    cfg, signals = config or Config(), signals or RiskSignals()
    ids = [t["test_case_id"] for t in test_cases]
    if len(set(ids)) != len(ids):
        raise GuardrailError(["duplicate test_case_id in input"])
    warnings: list[str] = []
    if analysis is None:
        warnings.append("no analysis.json: module/use-case mapping unavailable, R falls back to TC priority")

    # 2. map
    index = AnalysisIndex(analysis)
    tc_deps = {k: v for k, v in (signals.dependencies or {}).items() if isinstance(v, list)}
    ctx = {t["test_case_id"]: build_context(t, index, cfg, tc_deps) for t in test_cases}
    prereqs, w = dep.clean_dependencies(ids, {i: c.depends_on for i, c in ctx.items()})
    warnings += w
    dependents = dep.transitive_dependents(ids, prereqs)
    kmax = max((len(v) for v in dependents.values()), default=0)

    # 3. score
    wm = scoring.module_defect_weights(signals, cfg)
    rows: dict[str, dict] = {}
    for t in test_cases:
        i, c = t["test_case_id"], ctx[t["test_case_id"]]
        R, r_src, r_def = scoring.score_R(c, signals, cfg)
        C, c_src, c_def = scoring.score_C(c.module, signals, cfg)
        H, h_src, h_def = scoring.score_H(c.module, wm, signals, cfg)
        D, d_def = scoring.score_D(len(dependents[i]), kmax, bool(prereqs), cfg)
        S, s100 = scoring.combine(R, C, H, D, cfg)
        rows[i] = dict(R=R, C=C, H=H, D=D, S=S, S100=s100, r_src=r_src, c_src=c_src, h_src=h_src,
                       defaults=[n for n, f in zip("RCHD", (r_def, c_def, h_def, d_def)) if f])

    # 4. rank (score only)
    rank = sorted(ids, key=lambda i: (-rows[i]["S"], -rows[i]["R"], -rows[i]["H"], dep.natural_key(i)))
    score_rank = {i: n for n, i in enumerate(rank, 1)}

    # 5. dependency-aware order
    S = {i: rows[i]["S"] for i in ids}
    final, left, s_eff = dep.order(rank, prereqs, S, {i: rows[i]["R"] for i in ids},
                                   {i: rows[i]["H"] for i in ids}, dependents)
    if left:
        warnings.append(f"dependency cycle detected; kept by score: {', '.join(left)}")

    tc_by_id = {t["test_case_id"]: t for t in test_cases}
    bad_defects = {d.module: [] for d in signals.defects or []}
    for d in signals.defects or []:
        bad_defects[d.module].append(d.defect_id)
    items = []
    for order_no, i in enumerate(final, 1):
        r, c = rows[i], ctx[i]
        items.append(ScoredTC(
            test_case_id=i, execution_order=order_no, score_rank=score_rank[i],
            priority=scoring.to_priority(r["S100"], cfg), priority_original=c.original_priority,
            S=r["S"], S100=r["S100"], S_eff=round(s_eff[i], 6), R=r["R"], C=r["C"], H=r["H"], D=r["D"],
            defaults_used=r["defaults"], depends_on=prereqs.get(i, []), dependents_count=len(dependents[i]),
            promoted_by_dependency=s_eff[i] > r["S"] + 1e-9, in_cycle=i in left,
            module=c.module, use_case=c.use_cases[0] if c.use_cases else None, source_ids=c.source_ids,
            evidence={"r_source": r["r_src"], "c_source": r["c_src"], "h_source": r["h_src"],
                      "use_cases": c.use_cases, "block_ids": c.block_ids,
                      "defect_ids": bad_defects.get(c.module, [])},
            suggested_runner=c.runner, test_case=tc_by_id[i]))

    # 6. rationale (LLM never touches numbers)
    rationale.attach(items, llm)
    dist = {p: sum(1 for x in items if x.priority == p) for p in ("P1", "P2", "P3", "P4")}
    result = PrioritizationResult(
        formula_version=cfg.formula_version, input_hash=input_hash(test_cases, analysis, signals, cfg.to_dict()),
        generated_at=datetime.now(timezone.utc).isoformat(), total=len(items), distribution=dist,
        warnings=warnings, items=items)
    validate(result, test_cases, cfg)
    return result
