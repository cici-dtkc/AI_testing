from pathlib import Path
import os
from .generator.pipeline import generate_full_test_cases
from .generator.report import write_test_report
from datetime import datetime, timezone

def run_test_case_generator(llm, state: dict) -> dict:
    """Test Case Generator Agent nhận input analysis.json từ state hoặc mặc định và sinh test cases."""
    # Lấy đường dẫn analysis.json từ state hoặc dùng mặc định outputs/comic/analysis.json
    analysis_file = Path(state.get("analysis_path", "outputs/comic/analysis.json"))
    output_folder = Path(state.get("output_path", "outputs/comic/test_cases"))
    
    # Lấy API key và model từ môi trường hoặc state
    key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY", "")
    model = os.getenv("GEMINI_MODEL", "gemini-flash-latest")
    
    # Chạy pipeline sinh test case
    # Lưu ý: nếu dùng object llm truyền vào, bạn có thể gọi trực tiếp, ở đây dùng hàm gemini chuẩn qua pipeline
    test_suite, warnings = generate_full_test_cases(analysis_file, key, model)
    
    payload = {
        "provider": "GEMINI",
        "model": model,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "test_suite": test_suite.model_dump(),
        "validation_warnings": warnings
    }
    
    write_test_report(payload, output_folder)
    
    return {
        "test_cases": test_suite.model_dump(),
        "trace": [f"test_case_generator: đã sinh {len(test_suite.test_cases)} test cases và ghi báo cáo."],
    }