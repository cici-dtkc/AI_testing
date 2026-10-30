import html
import json
import re
from pathlib import Path


def module_for(title: str) -> str:
    """Xác định module từ tiêu đề Test Case."""
    t = (title or "").lower()
    rules = [
        (["dashboard"], "Dashboard"),
        (["thống kê", "doanh thu", "lợi nhuận", "xuất báo cáo"], "BaoCaoThongKe"),
        (["flash sale"], "FlashSale"),
        (["voucher", "mã voucher", "giảm giá", "khuyến mãi"], "GiamGia"),
        (["vnpay", "thanh toán trực tuyến", "thanh toán tại cổng"], "ThanhToan"),
        (["ghn", "phí vận chuyển", "vận đơn", "địa chỉ giao hàng thiếu"], "VanChuyen"),
        (["wishlist"], "Wishlist"),
        (["đánh giá", "đổi/trả", "trả hàng thất bại"], "DanhGiaDoiTra"),
        (["điểm thưởng", "thông tin cá nhân", "số điện thoại sai"], "HoSoKhachHang"),
        (["thông báo", "notification", "fcm"], "ThongBao"),
        (["gợi ý"], "GoiYSanPham"),
        (["đăng ký", "đăng nhập", "tài khoản người dùng", "khóa tài khoản",
          "chưa đăng nhập", "trang quản trị", "người dùng thông thường"], "TaiKhoanPhanQuyen"),
        (["danh mục thể loại", "danh mục trùng", "xóa danh mục"], "QuanLyTheLoai"),
        (["tồn kho", "số lượng tồn kho", "hết hàng", "tồn kho bị âm"], "QuanLyTonKho"),
        (["thêm truyện", "xóa truyện", "truyện tranh mới", "tệp ảnh"], "QuanLyTruyen"),
        (["trạng thái đơn hàng", "đơn hàng ở trạng thái", "admin duyệt",
          "admin hủy đơn", "cập nhật trạng thái đơn hàng"], "QuanLyDonHang"),
        (["giỏ hàng", "thêm sản phẩm vào giỏ", "thêm vào giỏ", "mua ngay"], "GioHang"),
        (["tìm kiếm", "tra cứu"], "TimKiem"),
        (["đơn hàng", "đặt hàng", "checkout", "địa chỉ nhận hàng"], "DatHang"),
    ]
    for keywords, module in rules:
        if any(keyword in t for keyword in keywords):
            return module
    return "QuanLyWebsite"


def write_test_report(payload: dict, folder: Path):
    folder.mkdir(parents=True, exist_ok=True)
    suite = payload.get("test_suite", {})
    test_cases = suite.get("test_cases", [])

    def e(value):
        if value is None:
            return ""
        if isinstance(value, (dict, list)):
            value = json.dumps(value, ensure_ascii=False)
        return html.escape(str(value))

    def render_list(value):
        if value is None:
            return []
        return value if isinstance(value, list) else [value]

    # Chuẩn hóa module, mã Test Case và mô tả kịch bản.
    module_counters = {}
    for tc in test_cases:
        module = tc.get("module") or module_for(tc.get("title", ""))
        tc["module"] = module
        module_counters[module] = module_counters.get(module, 0) + 1
        tc["test_case_id"] = f"{module}-TC-{module_counters[module]:02d}"
        # Mô tả dễ đọc dựa trên tiêu đề Test Case; ID gốc vẫn giữ ở trace_scenario_ids.
        tc["scenario_description"] = tc.get("scenario_description") or tc.get("title", "")

    # Lưu JSON sau khi chuẩn hóa
    json_path = folder / "test_cases.json"
    json_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

    rows = []
    for tc in test_cases:
        tc_type = str(tc.get("type") or "N/A")
        type_class = tc_type.upper() if tc_type.upper() in {"POSITIVE", "NEGATIVE", "BOUNDARY"} else "OTHER"
        priority = str(tc.get("priority") or "N/A")
        priority_lower = priority.lower()
        if "cao" in priority_lower or priority_lower in {"high", "critical", "p0", "p1"}:
            priority_class = "high"
        elif "thấp" in priority_lower or priority_lower in {"low", "p3"}:
            priority_class = "low"
        else:
            priority_class = "normal"

        preconditions = "".join(f"<li>{e(x)}</li>" for x in render_list(tc.get("preconditions", [])))
        given = e(tc.get("given", ""))
        steps = "".join(f"<li>{e(x)}</li>" for x in render_list(tc.get("steps", [])))
        expected = e(tc.get("expected_result", ""))
        scenario_description = e(tc.get("scenario_description") or tc.get("title", ""))
        scenario_ids = render_list(tc.get("trace_scenario_ids", []))
        scenario_trace = ""
        if scenario_ids:
            scenario_trace = "<div class='trace-ids'><b>Mã truy vết:</b><ul>" + "".join(
                f"<li>{e(x)}</li>" for x in scenario_ids
            ) + "</ul></div>"
        rule_ids = render_list(tc.get("trace_rule_ids", []))
        rule_html = "<ul>" + "".join(f"<li>{e(x)}</li>" for x in rule_ids) + "</ul>" if rule_ids else "—"

        rows.append(f"""
        <tr>
            <td class="id">{e(tc.get('test_case_id'))}</td>
            <td class="module">{e(tc.get('module'))}</td>
            <td class="title">{e(tc.get('title'))}</td>
            <td><span class="type type-{type_class}">{e(tc_type)}</span></td>
            <td><span class="priority {priority_class}">{e(priority)}</span></td>
            <td><ul>{preconditions}</ul></td>
            <td>{given or '—'}</td>
            <td><ol>{steps}</ol></td>
            <td>{expected or '—'}</td>
            <td class="scenario"><div class="scenario-desc">{scenario_description}</div>{scenario_trace}</td>
            <td>{rule_html}</td>
        </tr>
        """)

    if rows:
        table_html = f"""
        <div class="table-wrap">
            <table id="testTable">
                <thead><tr>
                    <th>Mã Test Case</th><th>Module</th><th>Tên Test Case</th>
                    <th>Loại</th><th>Ưu tiên</th><th>Điều kiện tiên quyết</th>
                    <th>Dữ liệu đầu vào</th><th>Các bước thực hiện</th>
                    <th>Kết quả mong đợi</th><th>Kịch bản</th><th>Rule IDs</th>
                </tr></thead>
                <tbody>{''.join(rows)}</tbody>
            </table>
        </div>
        """
    else:
        table_html = "<p class='empty'>Không có Test Case nào.</p>"

    warnings = payload.get("validation_warnings", [])
    warnings_section = ""
    if warnings:
        warnings_section = (
            "<section class='warnings'><h2>Cảnh báo Validation</h2><ul>"
            + "".join(f"<li>{e(w)}</li>" for w in warnings)
            + "</ul></section>"
        )

    provider = e(payload.get("provider") or "N/A")
    model = e(payload.get("model") or "N/A")
    summary = e(suite.get("summary") or "Chưa có tóm tắt")
    total = len(test_cases)

    html_content = f"""<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Bộ Test Case - Website bán truyện tranh online</title>
<style>
:root{{--navy:#17324d;--blue:#245b85;--border:#d7e0e8;--text:#243447;--muted:#637487}}
*{{box-sizing:border-box}}
body{{margin:0;background:#f4f7fb;color:var(--text);font:14px/1.5 "Segoe UI",Arial,sans-serif}}
header{{background:linear-gradient(120deg,#17324d,#245b85);color:white;padding:28px clamp(16px,3vw,40px)}}
h1{{margin:0 0 8px;font-size:clamp(22px,3vw,32px)}} header p{{margin:0;color:#e0eaf3}}
main{{padding:22px clamp(12px,2vw,28px) 40px}}
.meta{{display:flex;flex-wrap:wrap;gap:10px;margin-bottom:18px;color:var(--muted)}}
.meta span,.summary{{background:white;border:1px solid var(--border);border-radius:8px;padding:10px 13px}}
.summary{{margin-bottom:18px}}
.toolbar{{display:flex;gap:12px;flex-wrap:wrap;align-items:center;margin-bottom:14px}}
#search{{flex:1;min-width:240px;padding:11px 13px;border:1px solid #c8d4df;border-radius:8px;font:inherit;background:white}}
.count{{color:var(--muted);font-weight:600}}
.table-wrap{{overflow:auto;background:white;border:1px solid var(--border);border-radius:10px;box-shadow:0 4px 16px #17324d0d;max-height:78vh}}
table{{border-collapse:separate;border-spacing:0;width:100%;min-width:2200px}}
thead th{{position:sticky;top:0;z-index:2;background:var(--navy);color:white;text-align:left;padding:13px 12px;white-space:nowrap}}
tbody td{{padding:12px;border-bottom:1px solid #e5ebf0;border-right:1px solid #edf1f5;vertical-align:top;max-width:340px;overflow-wrap:anywhere}}
tbody tr:nth-child(even){{background:#f8fbfd}} tbody tr:hover{{background:#edf6fc}}
td.id{{font-weight:700;color:var(--blue);white-space:nowrap;min-width:160px}} td.module{{font-weight:600;white-space:nowrap}}
td.title{{min-width:250px;font-weight:600}} td ul,td ol{{margin:0;padding-left:18px;min-width:160px}} td li{{margin:0 0 5px}}
.type,.priority{{display:inline-block;border-radius:5px;padding:4px 8px;font-size:12px;white-space:nowrap}}
.type-POSITIVE{{background:#dcfce7;color:#166534}} .type-NEGATIVE{{background:#fee2e2;color:#991b1b}} .type-BOUNDARY{{background:#fef9c3;color:#854d0e}} .type-OTHER{{background:#edf2f7;color:#334155}}
.priority{{border-radius:20px;font-weight:650}} .priority.high{{background:#fde8e7;color:#a62d2a}} .priority.normal{{background:#fff2d7;color:#815a08}} .priority.low{{background:#e4f5e9;color:#246c3d}}
td.scenario{{min-width:300px}} .scenario-desc{{font-weight:600;margin-bottom:8px}} .trace-ids{{font-size:12px;color:var(--muted);border-top:1px solid #e5ebf0;padding-top:6px}}
.warnings{{margin-top:24px;background:#fffbeb;border:1px solid #fde68a;border-radius:8px;padding:12px 18px}} footer{{color:var(--muted);padding:12px 2px;font-size:12px}}
@media print{{body{{background:white}}header{{background:white;color:black;padding:12px 0}}header p{{color:#333}}main{{padding:8px}}.toolbar{{display:none}}.table-wrap{{max-height:none;overflow:visible;box-shadow:none;border:0}}table{{min-width:0;font-size:8px}}thead th{{position:static;background:#e8edf2;color:#111}}tbody td{{padding:4px;font-size:8px}}}}
</style>
</head>
<body>
<header><h1>Bộ Test Case – Website bán truyện tranh online</h1>
<p>Danh sách {total} Test Case với dữ liệu đầu vào, bước thực hiện, kết quả mong đợi và kịch bản truy vết.</p></header>
<main>
<div class="meta"><span><b>Provider:</b> {provider}</span><span><b>Model:</b> {model}</span><span><b>Tổng số Test Case:</b> {total}</span></div>
<div class="summary"><b>Tóm tắt:</b> {summary}</div>
<div class="toolbar"><input id="search" type="search" placeholder="Tìm theo mã, module, tiêu đề hoặc nội dung test case..." aria-label="Tìm kiếm test case"><span class="count" id="count">{total} / {total} Test Case</span></div>
{table_html}
{warnings_section}
<footer>Test Case Generator Report · Nhấn Ctrl + P để in hoặc lưu thành PDF.</footer>
</main>
<script>
const search = document.getElementById('search');
const table = document.getElementById('testTable');
const count = document.getElementById('count');
if (search && table && count) {{
 const rows = Array.from(table.querySelectorAll('tbody tr'));
 search.addEventListener('input', () => {{
  const q = search.value.trim().toLocaleLowerCase('vi');
  let visible = 0;
  rows.forEach(row => {{
   const match = row.innerText.toLocaleLowerCase('vi').includes(q);
   row.hidden = !match;
   if (match) visible++;
  }});
  count.textContent = visible + ' / ' + rows.length + ' Test Case';
 }});
}}
</script>
</body>
</html>
"""
    html_path = folder / "test_cases_report.html"
    html_path.write_text(html_content, encoding="utf-8")
    print(f"Đã lưu JSON: {json_path.resolve()}")
    print(f"Đã lưu HTML: {html_path.resolve()}")
    print(f"Tổng số Test Case: {total}")
