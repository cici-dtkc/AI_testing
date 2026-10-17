import json
from pathlib import Path
from .gemini import generate_test_cases
from .validation import validate_test_cases
from .models import TestSuite

def generate_full_test_cases(analysis_path: Path, key: str, model: str) -> tuple[TestSuite, list[str]]:
    if not analysis_path.is_file():
        raise ValueError(f'Không tìm thấy file analysis đầu vào tại: {analysis_path}')
    
    analysis_data = json.loads(analysis_path.read_text(encoding='utf-8'))
    
    # Gọi Gemini sinh test cases từ dữ liệu analysis
    test_suite = generate_test_cases(analysis_data, key, model)
    
    # Validate liên kết
    warnings = validate_test_cases(test_suite, analysis_data)
    
    return test_suite, warnings