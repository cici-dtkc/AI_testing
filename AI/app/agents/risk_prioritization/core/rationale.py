"""Step 6: rationale. Template is always built; LLM text is used only if it passes the checker."""
from __future__ import annotations

import json
import re

from AI.app.agents.risk_prioritization.data.models import ScoredTC

ID_RE = re.compile(r"\b(?:TC-\d+|C\d{3}-[A-Z0-9-]+|UC-\d+|M\d{2}|DEF-[\w-]+|B\d{4})\b")

SYSTEM = (
    "Bạn là Risk & Prioritization Agent. Chỉ GIẢI THÍCH lý do xếp hạng dựa trên bản ghi điểm được cung cấp. "
    "Không tự tính hay đổi điểm/priority. Viết 2-3 câu tiếng Việt, nhắc đúng priority và S100 như trong bản ghi, "
    "và trích dẫn ít nhất một ID nguồn có trong 'allowed_ids'. Không dùng ID nào ngoài danh sách."
)


def allowed_ids(item: ScoredTC) -> set[str]:
    ids = {item.test_case_id, item.module, *item.source_ids, *item.depends_on, *item.evidence.get("block_ids", []),
           *item.evidence.get("defect_ids", [])}
    if item.use_case:
        ids.add(item.use_case)
    ids.update(item.evidence.get("use_cases", []))
    return {i for i in ids if i}


def template(item: ScoredTC) -> str:
    ev = item.evidence
    parts = [
        f"{item.test_case_id} xếp {item.priority} (S100={item.S100:g}) vì R={item.R:g} ({ev.get('r_source')}), "
        f"C={item.C:g}, H={item.H:g}, D={item.D:g}; module {item.module}"
        + (f", use case {item.use_case}" if item.use_case else "") + "."
    ]
    if item.source_ids:
        parts.append("Nguồn: " + ", ".join(item.source_ids[:3]) + ".")
    if item.promoted_by_dependency:
        parts.append(f"Được đẩy lên sớm vì là tiền đề của TC rủi ro cao hơn (S_eff={item.S_eff:g}).")
    if item.depends_on:
        parts.append("Chạy sau: " + ", ".join(item.depends_on) + ".")
    if item.defaults_used:
        parts.append("Dùng giá trị mặc định cho: " + ", ".join(item.defaults_used) + ".")
    if item.in_cycle:
        parts.append("Cảnh báo: nằm trong chu trình phụ thuộc.")
    return " ".join(parts)


def valid_llm_text(text: str, item: ScoredTC) -> bool:
    cited = set(ID_RE.findall(text))
    return bool(cited) and cited <= allowed_ids(item) and item.priority in text and f"{item.S100:g}" in text


def llm_rationale(llm, item: ScoredTC) -> str | None:
    record = item.model_dump(exclude={"test_case", "rationale"})
    record["allowed_ids"] = sorted(allowed_ids(item))
    record["tc_title"] = item.test_case.get("title")
    try:
        text = llm.ask(SYSTEM, json.dumps(record, ensure_ascii=False)).strip()
    except Exception:
        return None
    return text if valid_llm_text(text, item) else None


def attach(items: list[ScoredTC], llm=None) -> None:
    for it in items:
        it.rationale, it.rationale_source = template(it), "template"
        if llm is not None:
            text = llm_rationale(llm, it)
            if text:
                it.rationale, it.rationale_source = text, "llm"
