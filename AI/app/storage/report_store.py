 
from __future__ import annotations

import html
import json
from pathlib import Path
from typing import Any, Mapping


def print_report(report: str) -> None:
    """Legacy helper: print a report string to stdout."""
    print("\n  BÁO CÁO \n" + report)


def _esc(text: Any) -> str:
    return html.escape(str(text))


def _read_json(path: Path) -> dict | None:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None


def _count(d: Any, *keys: str) -> int:
    cur: Any = d
    for k in keys:
        cur = cur.get(k) if isinstance(cur, dict) else None
    return len(cur) if isinstance(cur, (list, dict)) else 0


def _section(title: str, body: str) -> str:
    return f"<section><h2>{_esc(title)}</h2>{body}</section>"


def assemble_report(run_dir: str | Path, state: Mapping[str, Any] | None = None) -> str:
    """Assemble an HTML pipeline report from a run directory's artifacts."""
    run_dir = Path(run_dir)
    state = dict(state or {})

    analysis = _read_json(run_dir / "analysis" / "analysis.json")
    tc = _read_json(run_dir / "test_cases" / "test_cases.json")
    prio = _read_json(run_dir / "prioritized" / "prioritized_tests.json")

    parts: list[str] = [
        "<h1>Báo cáo pipeline kiểm thử</h1>",
        f"<p>Run: <code>{_esc(run_dir.name)}</code></p>",
        f"<p>Request: {_esc(state.get('request', ''))}</p>",
    ]

    if analysis:
        a = analysis.get("analysis", analysis)
        body = (
            f"<p>{_esc(a.get('summary', ''))}</p><ul>"
            f"<li>Business rules: {_count(a, 'rules')}</li>"
            f"<li>Test scenarios: {_count(a, 'scenarios')}</li>"
            f"<li>Issues: {_count(a, 'issues')}</li></ul>"
        )
        parts.append(_section("1. Phân tích yêu cầu (requirement_analyzer)", body))

    if tc:
        suite = tc.get("test_suite", tc)
        parts.append(
            _section(
                "2. Sinh test case (test_case_generator)",
                f"<p>Tổng số test case: {_count(suite, 'test_cases')}</p>",
            )
        )

    if prio:
        dist = prio.get("distribution", {})
        body = (
            f"<p>Trạng thái: {_esc(prio.get('status', ''))}</p>"
            f"<p>Phân bố ưu tiên: {_esc(json.dumps(dist, ensure_ascii=False))}</p>"
        )
        parts.append(_section("3. Xếp hạng rủi ro (risk_prioritization)", body))

    test_results = state.get("test_results") or {}
    if test_results:
        items = "".join(
            f"<tr><td>{_esc(k)}</td><td>{_esc(v.get('status', ''))}</td></tr>"
            for k, v in test_results.items()
        )
        parts.append(
            _section(
                "4. Kết quả thực thi (executor)",
                f"<table border=1><tr><th>Run</th><th>Trạng thái</th></tr>{items}</table>",
            )
        )

    trace = state.get("trace") or []
    if trace:
        items = "".join(f"<li>{_esc(t)}</li>" for t in trace)
        parts.append(_section("Trace điều phối", f"<ol>{items}</ol>"))

    style = (
        "<style>body{font:16px system-ui;max-width:1000px;margin:40px auto;"
        "padding:0 20px;color:#17243a}section{margin-top:24px}"
        "table{border-collapse:collapse}td,th{padding:4px 10px}"
        "code{background:#f2f6fa;padding:2px 6px;border-radius:4px}</style>"
    )
    return f"<!doctype html><html lang='vi'><meta charset='utf-8'>{style}<body>" + "".join(parts) + "</body></html>"


def write_report(run_dir: str | Path, state: Mapping[str, Any] | None = None) -> Path:
    """Write ``<run_dir>/final_report.html`` and return its path."""
    run_dir = Path(run_dir)
    path = run_dir / "final_report.html"
    path.write_text(assemble_report(run_dir, state), encoding="utf-8")
    return path
