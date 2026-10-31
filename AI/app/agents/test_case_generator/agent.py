from pathlib import Path
import os
from .generator.pipeline import generate_full_test_cases
from .generator.report import write_test_report
from datetime import datetime, timezone
from app.state.models import SharedState

def run_test_case_generator(llm, state: SharedState) -> dict:
    """Test Case Generator Agent nhận input analysis.json từ state hoặc mặc định và sinh test cases."""
    # Lấy đường dẫn analysis.json từ state hoặc dùng mặc định outputs/comic/analysis.json
    analysis_file = Path(state.get("analysis_path", "outputs/comic/analysis.json"))
    output_folder = Path(state.get("output_path", "outputs/comic/test_cases"))
    
    # Lấy API key và model từ môi trường hoặc state
    key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY", "")
    model = os.getenv("GEMINI_MODEL", "gemini-flash-latest")
    # Tuỳ chọn: mã nguồn để chạy kiểm thử hộp trắng (basis path/coverage/data-flow)
    code_path = state.get("code_path")
    
    # Chạy pipeline: engine chọn kỹ thuật trước, LLM sinh case theo kế hoạch
    test_suite, plan, warnings = generate_full_test_cases(
        analysis_file,
        key,
        model,
        Path(code_path) if code_path else None,
        cache_dir=output_folder / '.module_cache',
    )
    
    payload = {
        "provider": "GEMINI",
        "model": model,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "test_suite": test_suite.model_dump(),
        "technique_plan": plan.model_dump(),
        "validation_warnings": warnings
    }
    
    write_test_report(payload, output_folder)
    
    return {
        "test_cases": test_suite.model_dump(),
        "technique_plan": plan.model_dump(),
        "trace": [
            f"test_case_generator: {plan.summary}.",
            f"test_case_generator: đã sinh {len(test_suite.test_cases)} test cases và ghi báo cáo.",
        ],
    }
