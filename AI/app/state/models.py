from __future__ import annotations

import operator
from typing import Annotated, Any, Literal, TypedDict

from langgraph.graph.message import add_messages
from pydantic import BaseModel


def merge_by_key(cur: dict | None, new: dict | None) -> dict:
    return {**(cur or {}), **(new or {})}


class TestResult(TypedDict):
    status: Literal["PASS", "FAIL"]
    log_tail: str


class SharedState(TypedDict, total=False):
    request: str
    messages: Annotated[list, add_messages]
    test_plan: str
    test_cases: dict[str, Any] | list[dict[str, Any]] | str
    technique_plan: dict[str, Any]
    test_results: Annotated[dict[str, TestResult], merge_by_key]
    defects: str
    risk_priorities: str
    report: str
    retry_count: Annotated[int, operator.add]
    next_agent: str
    trace: Annotated[list, operator.add]
    # --- pipeline inputs / supervisor coordination ---
    srs_path: str
    document_id: str
    version: str
    analysis_path: str
    analysis: dict[str, Any]
    code_path: str
    output_path: str
    run_id: str
    run_dir: str


def merge_state(state: SharedState, update: dict[str, Any]) -> SharedState:
    """Apply agent updates using the same reducers as the workflow graph."""
    merged = {**state, **update}
    for key, reducer in (
        ("messages", add_messages),
        ("test_results", merge_by_key),
        ("retry_count", operator.add),
        ("trace", operator.add),
    ):
        if key in update and key in state:
            merged[key] = reducer(state[key], update[key])
    return merged


class Route(BaseModel):
    next: Literal["executor", "__end__"]
    reason: str
