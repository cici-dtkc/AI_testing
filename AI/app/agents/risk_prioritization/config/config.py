"""Versioned configuration: weights, thresholds, defaults, label maps."""
from __future__ import annotations

import json
from dataclasses import asdict, dataclass, field
from pathlib import Path

@dataclass
class OrchestratorConfig:
    base_url: str = "http://localhost:8080/TTLTW_Nhom6"
    max_workers: int = 2
    execution_timeout: int = 300
    retry_timeout: int = 300

@dataclass
class Config:
    formula_version: str = "1.0"
    weights: dict = field(default_factory=lambda: {"R": 0.35, "C": 0.25, "H": 0.25, "D": 0.15})
    complexity_weights: dict = field(default_factory=lambda: {"preconditions": 0.05, "steps": 0.03})
    thresholds: dict = field(default_factory=lambda: {"P1": 75.0, "P2": 55.0, "P3": 35.0})
    decay_lambda: float = 0.3                  # defect age decay (per release)
    defaults: dict = field(default_factory=lambda: {"R": 3, "C": 3, "H": 3, "D": 1})
    type_boost: dict = field(default_factory=lambda: {"NEGATIVE": 0.2, "BOUNDARY": 0.15})
    # label -> score (lower-case keys). Covers RTM labels, SRS "Priority" (Cao/Thong thuong/Thap) and TC priority
    r_levels: dict = field(default_factory=lambda: {
        "critical": 5, "high": 4, "medium": 3, "low": 2, "optional": 1,
        "cao": 4, "thông thường": 3, "thấp": 2})
    c_levels: dict = field(default_factory=lambda: {
        "none": 1, "indirect": 3, "minor": 4, "major": 5, "core": 5})
    # ASSUMPTION (expert setting): SRS flags payment/authorization as sensitive -> R + boost
    critical_modules: list = field(default_factory=lambda: ["M07", "M18"])
    critical_boost: float = 1.0
    api_keywords: list = field(default_factory=lambda: ["callback", "api", "endpoint", "timeout", "webhook"])

    def __post_init__(self):
        if abs(sum(self.weights.values()) - 1.0) > 1e-9:
            raise ValueError("weights must sum to 1")

    @classmethod
    def load(cls, path: str | Path | None) -> "Config":
        if not path:
            return cls()
        return cls(**json.loads(Path(path).read_text(encoding="utf-8")))

    def to_dict(self) -> dict:
        return asdict(self)
