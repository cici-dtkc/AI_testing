# Standalone Runner cho Bug Report Agent.
# Chạy trực tiếp từ terminal để kiểm thử độc lập và xuất toàn bộ Bug Reports thực tế:
#     cd AI
#     python -m app.agents.bug_report.main

import json
import sys
from pathlib import Path

# Đảm bảo in UTF-8 mượt trên Windows console
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

# Đảm bảo thư mục AI luôn nằm trong sys.path
ai_root = Path(__file__).resolve().parents[3]
if str(ai_root) not in sys.path:
    sys.path.insert(0, str(ai_root))

from app.agents.bug_report.agent import create_bug_report_from_failure
from app.agents.bug_report.core.extractor import DataExtractor


def main():
    print("=" * 90)
    print(" [BUG REPORT AGENT - STANDALONE EXECUTION ENGINE]")
    print("=" * 90)

    extractor = DataExtractor()
    actionable_failures = extractor.get_actionable_executions()

    print(f" Đã nạp thành công dữ liệu đa nguồn từ pipeline:")
    print(f"   • Tệp thực thi Test Runner: {extractor.execution_path.name}")
    print(f"   • Số ca thất bại cần xử lý: {len(actionable_failures)} ca")
    print(f"   • Failure Analysis Agent analyses: {len(extractor.failure_analysis_map)} ca")
    print(f"   • Cơ sở dữ liệu Test Cases: {len(extractor.test_cases_map)} ca")
    print(f"   • Cơ sở dữ liệu Business Rules: {len(extractor.rules_map)} rules\n")

    output_dir = Path(__file__).resolve().parent / "outputs" / "generated_bugs"
    output_dir.mkdir(parents=True, exist_ok=True)

    generated_reports = []
    official_product_bugs = []
    fingerprint_registry = {}
    severity_counter = {"CRITICAL": 0, "HIGH": 0, "MEDIUM": 0, "LOW": 0}

    print("-" * 90)
    print(f"{'MÃ BUG':<24} | {'MODULE':<12} | {'PHÂN LOẠI FAA':<15} | {'SEVERITY':<10} | {'ĐIỂM (I+M+W+S)':<14}")
    print("-" * 90)

    for item in actionable_failures:
        report = create_bug_report_from_failure(
            exec_item=item,
            extractor=extractor,
            existing_fingerprints=fingerprint_registry,
            strict_gate=False,
        )
        if report:
            rep_dict = report.model_dump()
            generated_reports.append(rep_dict)
            fingerprint_registry[report.deduplication.fingerprint] = report.bugId
            severity_counter[report.severity] += 1

            if report.gatePassed:
                official_product_bugs.append(rep_dict)

            # Lưu từng file JSON cho từng Bug Report
            bug_file = output_dir / f"{report.bugId}.json"
            with open(bug_file, "w", encoding="utf-8") as f:
                json.dump(rep_dict, f, ensure_ascii=False, indent=2)

            factors_str = f"{report.severityScore}đ ({report.factors.i}+{report.factors.m}+{report.factors.w}+{report.factors.s})"
            faa_cat = f"{report.failureCategory} ({'PASS' if report.gatePassed else 'MAINT'})"
            print(f"{report.bugId:<24} | {report.module:<12} | {faa_cat:<15} | {report.severity:<10} | {factors_str:<14}")

    # 1. Xuất file tổng hợp danh sách tất cả 30 báo cáo
    all_bugs_file = output_dir / "all_bug_reports_summary.json"
    with open(all_bugs_file, "w", encoding="utf-8") as f:
        json.dump(generated_reports, f, ensure_ascii=False, indent=2)

    # 2. Xuất file danh sách lỗi sản phẩm chính thức (Passed Gate Check)
    product_bugs_file = output_dir / "official_product_bugs.json"
    with open(product_bugs_file, "w", encoding="utf-8") as f:
        json.dump(official_product_bugs, f, ensure_ascii=False, indent=2)

    print("-" * 90)
    print(f"🎉 TỔNG KẾT BÁO CÁO LỖI TỪ BUG REPORT AGENT:")
    print(f"   • Tổng số báo cáo khiếm khuyết: {len(generated_reports)} báo cáo")
    print(f"   • Vượt qua Gate Check lỗi sản phẩm (PRODUCT_BUG): {len(official_product_bugs)} ca")
    print(f"   • Phân loại cần bảo trì kịch bản / giao diện (TEST_MAINTENANCE): {len(generated_reports) - len(official_product_bugs)} ca")
    print(f"   • Phân bố Severity:")
    print(f"       🔴 CRITICAL: {severity_counter['CRITICAL']}")
    print(f"       🟠 HIGH:     {severity_counter['HIGH']}")
    print(f"       🟡 MEDIUM:   {severity_counter['MEDIUM']}")
    print(f"       🔵 LOW:      {severity_counter['LOW']}")
    print(f"   • Thư mục lưu kết quả: {output_dir}")
    print("=" * 90)


if __name__ == "__main__":
    main()
