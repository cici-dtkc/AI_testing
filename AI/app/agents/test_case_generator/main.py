import argparse, json, os, shutil, sys
from pathlib import Path
from dotenv import load_dotenv
from datetime import datetime, timezone

# Sửa lại thành import tuyệt đối để chạy trực tiếp không bị lỗi relative import
from generator.pipeline import build_plan, generate_full_test_cases
from generator.report import write_test_report
from generator.prompt import PROMPT_VERSION

ROOT = Path(__file__).resolve().parent

def main():
    load_dotenv(ROOT / '.env')
    p = argparse.ArgumentParser(description='Test Case Generator Agent dùng Gemini dựa trên Analysis JSON')
    p.add_argument('--input', type=Path, default=ROOT / 'outputs/comic/analysis.json', help='Đường dẫn file analysis.json đầu vào')
    p.add_argument('--output', type=Path, default=ROOT / 'outputs/comic/test_cases', help='Thư mục chứa kết quả đầu ra')
    p.add_argument('--model', default=os.getenv('GEMINI_MODEL', 'gemini-flash-latest'))
    p.add_argument('--code', type=Path, default=None, help='File hoặc thư mục mã nguồn để lập kế hoạch kiểm thử hộp trắng')
    p.add_argument('--plan-only', action='store_true', help='Chỉ in kế hoạch kỹ thuật, không gọi Gemini')
    p.add_argument('--delay', type=float, default=float(os.getenv('GEMINI_MODULE_DELAY', '2')), help='Giây nghỉ giữa 2 request module (tránh rate limit)')
    p.add_argument('--fresh', action='store_true', help='Xoá cache từng module và sinh lại từ đầu')
    args = p.parse_args()
    
    try:
        if args.plan_only:
            plan, warnings, _ = build_plan(args.input, args.code)
            args.output.mkdir(parents=True, exist_ok=True)
            plan_path = args.output / 'technique_plan.json'
            plan_path.write_text(
                json.dumps(plan.model_dump(), ensure_ascii=False, indent=2),
                encoding='utf-8',
            )
            print(f'Kế hoạch kỹ thuật: {plan.summary}')
            for module in plan.modules:
                primary = module.techniques[0]
                variant = f' ({primary.variant})' if primary.variant else ''
                print(f'  - {module.module}: {primary.technique}{variant} ~ {primary.estimated_cases} case')
                print(f'      {module.story}')
            for warning in warnings:
                print(f'  ! {warning}')
            print(f'Đã lưu kế hoạch: {plan_path.resolve()}')
            return 0

        key = os.getenv('GEMINI_API_KEY', '')
        if not key or key == 'your_gemini_api_key_here':
            raise ValueError('Điền GEMINI_API_KEY trong .env trước khi chạy.')
            
        cache_dir = args.output / '.module_cache'
        if args.fresh:
            shutil.rmtree(cache_dir, ignore_errors=True)

        test_suite, plan, warnings = generate_full_test_cases(
            args.input,
            key,
            args.model,
            code_path=args.code,
            module_delay=args.delay,
            progress=lambda message: print(f'  + {message}', flush=True),
            cache_dir=cache_dir,
        )
        
        payload = {
            'provider': 'GEMINI',
            'model': args.model,
            'created_at': datetime.now(timezone.utc).isoformat(),
            'prompt_version': PROMPT_VERSION,
            'test_suite': test_suite.model_dump(),
            'technique_plan': plan.model_dump(),
            'validation_warnings': warnings
        }
        
        write_test_report(payload, args.output)
        print(f'Kế hoạch kỹ thuật: {plan.summary}')
        for module in plan.modules:
            primary = module.techniques[0]
            variant = f' ({primary.variant})' if primary.variant else ''
            print(f'  - {module.module}: {primary.technique}{variant} ~ {primary.estimated_cases} case')
        print(f'Đã sinh {len(test_suite.test_cases)} Test Cases.')
        print(f'JSON: {(args.output / "test_cases.json").resolve()}')
        print(f'HTML: {(args.output / "test_cases_report.html").resolve()}')
        if warnings:
            print(f'Cảnh báo ({len(warnings)}):')
            for warning in warnings[:10]:
                print(f'  ! {warning}')
            if len(warnings) > 10:
                print(f'  ... xem toàn bộ trong {args.output / "test_cases.json"}')
        if not test_suite.test_cases:
            print('Không sinh được test case nào — xem cảnh báo phía trên.', file=sys.stderr)
            return 1
        return 0
    except (ValueError, RuntimeError, OSError) as exc:
        print(f'Lỗi: {exc}', file=sys.stderr)
        return 1

if __name__ == '__main__':
    raise SystemExit(main())