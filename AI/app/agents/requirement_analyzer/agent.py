
from __future__ import annotations

import os
from datetime import datetime, timezone

from app.state.models import SharedState

from .analyzer.parser import parse
from .analyzer.pipeline import analyze_full
from .analyzer.report import write_report
from .analyzer.prompt import PROMPT_VERSION


def run_srs_analysis(llm, state: SharedState) -> dict:
    """SRS Analyzer Agent nhận input SRS từ state và sinh analysis.json + report.html."""
    srs_path = state["srs_path"]
    document_id = state.get("document_id", "SRS")
    version = state.get("version", "1.0")
    run_dir = state["run_dir"]

    key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY", "")
    model = os.getenv("GEMINI_MODEL", "gemini-flash-latest")

    from pathlib import Path

    analysis_dir = Path(run_dir) / "analysis"
    analysis_dir.mkdir(parents=True, exist_ok=True)

    doc = parse(Path(srs_path), document_id, version)
    analysis, warnings, stats = analyze_full(
        doc,
        key,
        model,
        analysis_dir,
        chunk_chars=12000,
        resume=True,            
        skip_cross=False,
        cross_chars=26000,
    )

    payload = {
        "provider": "GEMINI",
        "model": model,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "prompt_version": PROMPT_VERSION,
        "review_status": "REVIEW_REQUIRED",
        "source_document": doc.model_dump(),
        "analysis": analysis.model_dump(),
        "validation_warnings": warnings,
        "pipeline_stats": stats,
    }
    write_report(payload, analysis_dir)

    return {
        "analysis_path": str(analysis_dir / "analysis.json"),
        "analysis": payload,
        "trace": [
            "srs_analyzer: đã phân tích SRS -> "
            f"{len(analysis.rules)} rules, {len(analysis.scenarios)} scenarios, "
            f"{len(analysis.issues)} issues"
        ],
    }


def run_requirement_analyzer(llm, state: SharedState) -> dict:
    """Analyze the latest test run and extract requirement-related defects."""
    latest = state["test_results"][f"run{state.get('retry_count', 0)}"]
    defects = llm.ask(
        "Bạn là Requirement Analyzer Agent. Phân tích log kiểm thử "
        "theo yêu cầu và xác định các sai lệch hoặc defect.",
        latest["log_tail"],
    )
    return {
        "defects": defects,
        "trace": ["requirement_analyzer: đã ghi defects"],
    }
