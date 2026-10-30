"""Read inputs. test_cases.json is preferred; the HTML report is a fallback."""
from __future__ import annotations

import html
import json
import re
from pathlib import Path


def load_json(path: str | Path) -> dict | list:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def _extract(data) -> list[dict]:
    if isinstance(data, list):
        return data
    if "test_suite" in data:
        return data["test_suite"]["test_cases"]
    return data["test_cases"]


def _from_html(text: str) -> list[dict]:
    out = []
    for block in re.findall(r"<pre>(.*?)</pre>", text, flags=re.S):
        try:
            obj = json.loads(html.unescape(block))
        except json.JSONDecodeError:
            continue
        if isinstance(obj, dict) and "test_case_id" in obj:
            out.append(obj)
    return out


def load_test_cases(path: str | Path) -> list[dict]:
    p = Path(path)
    if p.suffix.lower() in (".html", ".htm"):
        cases = _from_html(p.read_text(encoding="utf-8"))
    else:
        cases = _extract(load_json(p))
    if not cases:
        raise ValueError(f"No test cases found in {p}")
    return cases


def load_analysis(path: str | Path | None) -> dict | None:
    return load_json(path) if path else None
