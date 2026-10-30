 
from __future__ import annotations

import argparse
import logging
import os
import sys
from pathlib import Path

from app.agents.llm import RealLLM
from app.storage.log import safe_print
from app.workflow.supervisor import RECURSION_LIMIT, build_graph

# silence google-genai SDK chit-chat (AFC + "both keys set" warnings)
logging.getLogger("google_genai").setLevel(logging.ERROR)

ROOT = Path(__file__).resolve().parent
DEFAULT_SRS = ROOT / "app" / "agents" / "requirement_analyzer" / "examples" / "coupon_srs.txt"


def load_env_file(path: Path) -> None:
    if not path.exists():
        raise FileNotFoundError(f"Không tìm thấy file cấu hình: {path}")
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        name, value = line.split("=", 1)
        os.environ.setdefault(name.strip(), value.strip().strip("\"'"))


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Supervisor pipeline: SRS -> analysis -> test cases -> ranking -> execution")
    p.add_argument("--srs", type=Path, default=DEFAULT_SRS, help="SRS document (.txt/.md/.docx/.pdf)")
    p.add_argument("--document-id", default="SRS-ECOMMERCE")
    p.add_argument("--version", default="1.0")
    p.add_argument("--tests-dir", default="tests", help="thư mục pytest mà executor chạy")
    p.add_argument("--storage-root", default="app/storage/runs", help="thư mục gốc cho các run")
    p.add_argument("--model", default=None, help="ghi đè GEMINI_MODEL (dùng cho RealLLM)")
    p.add_argument("--thread-id", default="supervisor-run")
    return p.parse_args()


def main() -> None:
    if sys.platform == "win32":
        os.system("chcp 65001 > nul")
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            try:
                stream.reconfigure(encoding="utf-8")
            except Exception:
                pass
    safe_print("Khởi động supervisor pipeline ...")
    args = parse_args()
    load_env_file(ROOT / ".env")

    api_key = os.getenv("GOOGLE_API_KEY", "")
    if not api_key or api_key == "PASTE_YOUR_GEMINI_API_KEY_HERE":
        raise RuntimeError("Hãy điền GOOGLE_API_KEY trong AI/.env trước khi chạy.")
    
    model_name = args.model or os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
    if ":" not in model_name:
        model_name = f"google_genai:{model_name}"   # langchain prefix for RealLLM

    srs_path = args.srs.resolve()
    if not srs_path.is_file():
        raise FileNotFoundError(f"Không tìm thấy SRS: {srs_path}")

    graph = build_graph(RealLLM(model_name), args.tests_dir, args.storage_root)
    safe_print("Bắt đầu chạy pipeline — theo dõi log từng bước:")
    result = graph.invoke(
        {
            "request": f"Phân tích SRS và sinh + xếp hạng + thực thi test cases cho {srs_path.name}.",
            "srs_path": str(srs_path),
            "document_id": args.document_id,
            "version": args.version,
        },
        {"configurable": {"thread_id": args.thread_id}, "recursion_limit": RECURSION_LIMIT},
    )

    run_dir = result.get("run_dir")
    print("Workflow hoàn tất.")
    print("Run dir:", run_dir)
    print("Kết quả:", result.get("test_results"))
    if run_dir:
        print("State:", Path(run_dir) / "state.json")
        print("Báo cáo:", Path(run_dir) / "final_report.html")
    print("Trace:")
    for item in result.get("trace", []):
        print(f"- {item}")


if __name__ == "__main__":
    main()
