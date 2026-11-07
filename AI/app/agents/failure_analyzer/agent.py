from __future__ import annotations

from typing import Any

from .core.analyzer import analyze_input


def run_failure_analyzer(_llm: Any, state: dict[str, Any]) -> dict[str, Any]:
    """Analyze the normalized failure input supplied in workflow state."""
    del _llm
    payload = state.get("failure_analysis_input")
    if not isinstance(payload, dict):
        raise ValueError("state.failure_analysis_input must be a normalized input object")
    result = analyze_input(payload)
    return {
        "failure_analysis": result,
        "defects": "\n".join(item["summary"] for item in result["analyses"]),
        "trace": [
            f"failure_analyzer: analyzed {len(result['analyses'])} test attempt(s)"
        ],
    }
