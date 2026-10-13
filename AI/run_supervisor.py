from __future__ import annotations

import os
from pathlib import Path

from app.agents.llm import RealLLM
from app.workflow.supervisor import build_graph


def load_env_file(path: Path) -> None:
    if not path.exists():
        raise FileNotFoundError(f"Không tìm thấy file cấu hình: {path}")

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        name, value = line.split("=", 1)
        os.environ.setdefault(name.strip(), value.strip().strip("\"'"))


def main() -> None:
    load_env_file(Path(__file__).with_name(".env"))

    api_key = os.getenv("GOOGLE_API_KEY", "")
    if not api_key or api_key == "PASTE_YOUR_GEMINI_API_KEY_HERE":
        raise RuntimeError(
            "Hãy điền GOOGLE_API_KEY trong AI/.env trước khi chạy."
        )

    model_name = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
    if ":" not in model_name:
        model_name = f"google_genai:{model_name}"

    graph = build_graph(RealLLM(model_name), "tests")
    result = graph.invoke(
        {
            "request": "Kiểm thử toàn bộ test suite hiện tại.",
            "test_plan": "Chạy pytest và phân tích kết quả kiểm thử.",
            "test_cases": "Chạy toàn bộ test trong thư mục tests.",
        },
        {"configurable": {"thread_id": "real-gemini-run"}},
    )

    print("Workflow hoàn tất.")
    print("Kết quả:", result.get("test_results"))
    print("Trace:")
    for item in result.get("trace", []):
        print(f"- {item}")


if __name__ == "__main__":
    main()
