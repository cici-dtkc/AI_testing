from __future__ import annotations

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class ExecutionState(str, Enum):
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    PASSED = "PASSED"
    FAILED = "FAILED"
    BLOCKED = "BLOCKED"
    FLAKY = "FLAKY"
    ERROR = "ERROR"


class ExecutionResult(BaseModel):
    test_case_id: str
    state: ExecutionState
    attempt: int = Field(ge=1, le=2)
    start_time: datetime
    end_time: datetime
    duration_ms: int = Field(ge=0)
    priority: str | None = None
    evidence_uris: list[str] = Field(default_factory=list)
    error_message: str | None = None
    blocked_by: list[str] | None = None


class ExecutionPlan(BaseModel):
    groups: list[list[str]]
    max_workers: int = Field(default=2, ge=1)


class OrchestratorReport(BaseModel):
    run_id: str
    started_at: datetime
    completed_at: datetime
    total_duration_ms: int = Field(ge=0)
    total_tcs: int = Field(ge=0)
    state_counts: dict[str, int] = Field(default_factory=dict)
    results: list[ExecutionResult] = Field(default_factory=list)
