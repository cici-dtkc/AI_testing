from app.state.models import SharedState


def run_risk_prioritization(llm, state: SharedState) -> dict:
    """Rank defects by impact and recommend the next testing action."""
    risks = llm.ask(
        "Bạn là Risk & Prioritization Agent. Xếp hạng defect theo "
        "mức độ ảnh hưởng (Critical, High, Medium, Low), giải thích "
        "ngắn gọn và đề xuất ưu tiên xử lý.",
        state.get("defects", ""),
    )
    return {
        "risk_priorities": risks,
        "trace": ["risk_prioritization: đã ghi risk_priorities"],
    }
