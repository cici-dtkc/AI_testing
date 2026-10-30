"""Write prioritized_tests.json and a small HTML report."""
from __future__ import annotations

import html
import json
from pathlib import Path

from ..data.models import PrioritizationResult


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
