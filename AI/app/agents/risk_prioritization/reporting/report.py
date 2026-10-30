"""Write prioritized_tests.json and a small HTML report."""
from __future__ import annotations

import html
import json
import os
from pathlib import Path

# from ..data.models import PrioritizationResult
from AI.app.agents.risk_prioritization.data.models import PrioritizationResult
from AI.app.agents.risk_prioritization.evidence_store.api import get_evidence
from AI.app.agents.risk_prioritization.orchestrator.models import ExecutionState, OrchestratorReport


def write_outputs(result: PrioritizationResult, out_dir: str | Path) -> dict[str, str]:
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    jp, hp = out / "prioritized_tests.json", out / "prioritized_report.html"
    jp.write_text(result.model_dump_json(indent=2), encoding="utf-8")
    rows = "".join(
        f"<tr><td>{i.execution_order}</td><td>{html.escape(i.test_case_id)}</td><td><b>{i.priority}</b></td>"
        f"<td>{i.S100:g}</td><td>{i.R:g}/{i.C:g}/{i.H:g}/{i.D:g}</td><td>{html.escape(i.module)}</td>"
        f"<td>{html.escape(i.test_case.get('title', ''))}</td>"
        f"<td>{'promoted ' if i.promoted_by_dependency else ''}{html.escape(', '.join(i.defaults_used))}</td>"
        f"<td>{html.escape(i.rationale)}</td></tr>" for i in result.items)
    warns = "".join(f"<li>{html.escape(w)}</li>" for w in result.warnings) or "<li>None</li>"
    hp.write_text(
        "<!doctype html><html lang='vi'><meta charset='utf-8'><title>Risk Prioritization</title>"
        "<style>body{font:14px system-ui;margin:24px}table{border-collapse:collapse}"
        "td,th{border:1px solid #ccc;padding:4px 8px;vertical-align:top}</style>"
        f"<h1>Risk &amp; Prioritization</h1><p>formula {result.formula_version} | hash {result.input_hash[:12]} | "
        f"total {result.total} | {html.escape(json.dumps(result.distribution))}</p><h3>Warnings</h3><ul>{warns}</ul>"
        "<table><tr><th>#</th><th>TC</th><th>Pri</th><th>S100</th><th>R/C/H/D</th><th>Module</th><th>Title</th>"
        f"<th>Flags</th><th>Rationale</th></tr>{rows}</table></html>", encoding="utf-8")
    return {"json": str(jp), "html": str(hp)}


def generate_html_report(report: OrchestratorReport, metadata_index, output_path: str) -> str:
    out = Path(output_path)
    out.parent.mkdir(parents=True, exist_ok=True)

    def rel(uri: str) -> str:
        return Path(os.path.relpath(uri, out.parent)).as_posix()

    rows = []
    failures = []
    for r in report.results:
        entries = metadata_index.get_by_tc(report.run_id, r.test_case_id) if metadata_index else get_evidence(report.run_id, r.test_case_id)
        links = " ".join(
            f"<a href='{html.escape(rel(e['uri']))}'>{html.escape(e['type'])}</a>" for e in entries)
        rows.append(
            f"<tr><td>{html.escape(r.test_case_id)}</td><td>{html.escape(r.priority or '-')}</td>"
            f"<td>{r.state.value}</td><td>{r.duration_ms}</td><td>{links or '-'}</td></tr>")

        if r.state in (ExecutionState.FAILED, ExecutionState.ERROR):
            failures.append(f"<h3>{html.escape(r.test_case_id)}: {html.escape(r.error_message or '')}</h3>")
            for e in entries:
                if e["type"] == "screenshot":
                    failures.append(f"<img src='{html.escape(rel(e['uri']))}' width='640'/>")

    summary = " ".join(f"{html.escape(k)}={v}" for k, v in sorted(report.state_counts.items()))

    out.write_text(
        "<!doctype html><html lang='vi'><meta charset='utf-8'>"
        f"<title>Execution Report {html.escape(report.run_id)}</title>"
        "<style>body{font:14px system-ui;margin:24px}table{border-collapse:collapse}"
        "td,th{border:1px solid #ccc;padding:4px 8px;vertical-align:top}"
        "img{display:block;margin:8px 0;border:1px solid #ccc}</style>"
        f"<h1>Execution Report</h1><p>run {html.escape(report.run_id)} | total TCs {report.total_tcs} | "
        f"duration {report.total_duration_ms} ms | {summary}</p>"
        "<table><tr><th>TC ID</th><th>Priority</th><th>State</th><th>Duration (ms)</th>"
        f"<th>Evidence</th></tr>{''.join(rows)}</table>"
        f"<h2>Failures</h2>{''.join(failures) or '<p>None</p>'}</html>", encoding="utf-8")
    return str(out)
