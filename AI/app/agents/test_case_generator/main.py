import argparse, os, sys
from pathlib import Path
from dotenv import load_dotenv
from datetime import datetime, timezone

# Sửa lại thành import tuyệt đối để chạy trực tiếp không bị lỗi relative import
from generator.pipeline import generate_full_test_cases
from generator.report import write_test_report
from generator.prompt import PROMPT_VERSION

ROOT = Path(__file__).resolve().parent

def main():
    load_dotenv(ROOT / '.env')
    p = argparse.ArgumentParser(description='Test Case Generator Agent dùng Gemini dựa trên Analysis JSON')
    p.add_argument('--input', type=Path, default=ROOT / 'outputs/comic/analysis.json', help='Đường dẫn file analysis.json đầu vào')
    p.add_argument('--output', type=Path, default=ROOT / 'outputs/comic/test_cases', help='Thư mục chứa kết quả đầu ra')
    p.add_argument('--model', default=os.getenv('GEMINI_MODEL', 'gemini-flash-latest'))
    args = p.parse_args()
    
    try:
        key = os.getenv('GEMINI_API_KEY', '')
        if not key or key == 'your_gemini_api_key_here':
            raise ValueError('Điền GEMINI_API_KEY trong .env trước khi chạy.')
            
        test_suite, warnings = generate_full_test_cases(args.input, key, args.model)
        
        payload = {
            'provider': 'GEMINI',
            'model': args.model,
            'created_at': datetime.now(timezone.utc).isoformat(),
            'prompt_version': PROMPT_VERSION,
            'test_suite': test_suite.model_dump(),
            'validation_warnings': warnings
        }
        
        write_test_report(payload, args.output)
        print(f'Sinh thành công {len(test_suite.test_cases)} Test Cases!')
        print(f'JSON: {(args.output / "test_cases.json").resolve()}')
        print(f'HTML: {(args.output / "test_cases_report.html").resolve()}')
        return 0
    except (ValueError, RuntimeError, OSError) as exc:
        print(f'Lỗi: {exc}', file=sys.stderr)
        return 1

if __name__ == '__main__':
    raise SystemExit(main())