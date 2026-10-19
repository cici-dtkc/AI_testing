"""Guardrails: any violation -> GuardrailError (no partial result)."""
from __future__ import annotations

from AI.app.agents.risk_prioritization.config.config import Config
from AI.app.agents.risk_prioritization.data.models import PrioritizationResult
from .scoring import to_priority


class GuardrailError(Exception):
    def __init__(self, violations: list[str]):
        super().__init__("; ".join(violations))
        self.violations = violations


def validate(result: PrioritizationResult, original: list[dict], cfg: Config) -> None:
    v: list[str] = []
    items = result.items
    ids = [i.test_case_id for i in items]
    if len(items) != len(original):
        v.append(f"count mismatch: in={len(original)} out={len(items)}")
    if len(set(ids)) != len(ids):
        v.append("duplicate test_case_id in output")
    by_id = {t["test_case_id"]: t for t in original}
    if set(ids) != set(by_id):
        v.append("output IDs differ from input IDs")
    if [i.execution_order for i in items] != list(range(1, len(items) + 1)):
        v.append("execution_order is not 1..N")
    pos = {i.test_case_id: i.execution_order for i in items}
    for it in items:
        if by_id.get(it.test_case_id) != it.test_case:
            v.append(f"{it.test_case_id}: test case content was modified")
        if it.priority != to_priority(it.S100, cfg):
            v.append(f"{it.test_case_id}: priority does not match S100")
        if not it.in_cycle:
            for p in it.depends_on:
                if p in pos and pos[p] > it.execution_order:
                    v.append(f"{it.test_case_id}: runs before prerequisite {p}")
    if v:
        raise GuardrailError(v)
