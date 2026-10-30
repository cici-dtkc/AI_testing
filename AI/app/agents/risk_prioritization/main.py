"""CLI demo: python -m app.agents.risk_prioritization.main --test-cases ... """
from __future__ import annotations

import argparse

from .config.config import Config
from .data.loader import load_analysis, load_test_cases
from .core.pipeline import run_pipeline
from .reporting.report import write_outputs
from .core.signals import load_signals


def main() -> None:
    ap = argparse.ArgumentParser(description="Risk & Prioritization agent (demo)")
    ap.add_argument("--test-cases", required=True, help="test_cases.json (or the HTML report)")
    ap.add_argument("--analysis", help="analysis.json from requirement_analyzer")
    ap.add_argument("--signals-dir", help="dir with optional rtm/change_log/defects/dependencies .json")
    ap.add_argument("--config", help="config JSON (weights, thresholds, ...)")
    ap.add_argument("--out", default="outputs")
    ap.add_argument("--model", help="optional LLM for rationale, e.g. google_genai:gemini-3.5-flash-lite")
    a = ap.parse_args()

    llm = None
    if a.model:
        from AI.app.agents.llm import RealLLM
        llm = RealLLM(a.model)
    result = run_pipeline(load_test_cases(a.test_cases), load_analysis(a.analysis),
                          load_signals(a.signals_dir), Config.load(a.config), llm)
    paths = write_outputs(result, a.out)
    print(f"{result.total} TC | {result.distribution} | hash {result.input_hash[:12]}")
    for w in result.warnings:
        print("WARN:", w)
    print(paths)


if __name__ == "__main__":
    main()
