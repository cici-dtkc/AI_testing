"""Supervisor-facing entry point (replaces the placeholder agent)."""
from __future__ import annotations

import json
from pathlib import Path

from app.state.models import SharedState
from .config.config import Config
from .data.loader import load_analysis, load_test_cases
from .core.pipeline import run_pipeline
from .reporting.report import write_outputs
from .core.signals import load_signals
from .core.validation import GuardrailError


def _read_test_cases(raw) -> list[dict]:
    if isinstance(raw, (list, dict)):
        return raw if isinstance(raw, list) else raw.get("test_suite", raw)["test_cases"]
    if isinstance(raw, str) and Path(raw).exists():
        return load_test_cases(raw)
    data = json.loads(raw)
    return data if isinstance(data, list) else data.get("test_suite", data)["test_cases"]


def run_risk_prioritization(llm, state: SharedState, *, analysis_path: str | None = None,
                            signals_dir: str | None = None, out_dir: str | None = None,
                            config_path: str | None = None) -> dict:
    """Ranks test cases. `llm` may be None (template rationale only)."""
    analysis_path = analysis_path or state.get("analysis_path")
    analysis = state.get("analysis") or load_analysis(analysis_path)
    try:
        result = run_pipeline(_read_test_cases(state.get("test_cases", "")), analysis,
                              load_signals(signals_dir), Config.load(config_path), llm)
    except GuardrailError as exc:
        # spec: return an error to the Orchestrator instead of a partial result
        return {"risk_priorities": json.dumps({"status": "ERROR", "violations": exc.violations}, ensure_ascii=False),
                "trace": [f"risk_prioritization: LỖI guardrail ({len(exc.violations)})"]}
    paths = write_outputs(result, out_dir) if out_dir else {}
    summary = {"status": "OK", "formula_version": result.formula_version, "input_hash": result.input_hash,
               "distribution": result.distribution, "warnings": result.warnings, "files": paths,
               "order": [{"id": i.test_case_id, "order": i.execution_order, "priority": i.priority,
                          "S100": i.S100, "runner": i.suggested_runner, "depends_on": i.depends_on}
                         for i in result.items]}
    return {"risk_priorities": json.dumps(summary, ensure_ascii=False),
            "trace": [f"risk_prioritization: đã xếp hạng {result.total} TC {result.distribution}"]}
