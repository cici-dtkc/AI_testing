"""Data models for the Risk & Prioritization agent."""
from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field

Priority = Literal["P1", "P2", "P3", "P4"]


class DefectRecord(BaseModel):
    defect_id: str
    module: str
    severity: int = Field(ge=1, le=4)      # 1 (minor) .. 4 (blocker)
    age_releases: float = Field(ge=0, default=0)


class RiskSignals(BaseModel):
    """Optional external signals. None = the whole source is missing (defaults apply)."""
    rtm: dict[str, Any] | None = None             # "UC-9" | "M04" -> "Critical"/"High"/... or {"priority": ...}
    change_log: dict[str, str] | None = None      # "M07" -> "none|indirect|minor|major|core"
    defects: list[DefectRecord] | None = None
    dependencies: dict[str, Any] | None = None    # "TC-04" -> ["TC-03"] or module graph "M06" -> {...}


class ScoredTC(BaseModel):
    test_case_id: str
    execution_order: int
    score_rank: int
    priority: Priority
    priority_original: str | None = None
    S: float
    S100: float
    S_eff: float
    R: float
    C: float
    H: float
    D: float
    defaults_used: list[str] = Field(default_factory=list)
    depends_on: list[str] = Field(default_factory=list)
    dependents_count: int = 0
    promoted_by_dependency: bool = False
    in_cycle: bool = False
    module: str
    use_case: str | None = None
    source_ids: list[str] = Field(default_factory=list)
    evidence: dict[str, Any] = Field(default_factory=dict)
    suggested_runner: Literal["ui", "api"] = "ui"   # hint for the future Orchestrator
    rationale: str = ""
    rationale_source: Literal["llm", "template"] = "template"
    test_case: dict[str, Any]                         # original TC, untouched


class PrioritizationResult(BaseModel):
    formula_version: str
    input_hash: str
    generated_at: str
    total: int
    distribution: dict[str, int]
    warnings: list[str] = Field(default_factory=list)
    items: list[ScoredTC]
