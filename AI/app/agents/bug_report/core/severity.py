
# Module tính toán Mức độ nghiêm trọng (Severity) và Độ tin cậy (Confidence).
# Tuân thủ tuyệt đối Guardrail G1:
# - Severity do bộ quy tắc xác định (deterministic code) tính toán, không để LLM chấm điểm.
# - Công thức: Score = I + M + W + S (Thang điểm 0 - 10).


from __future__ import annotations

import re
from typing import Optional, Tuple
from .schemas import ReportConfidence, SeverityFactors, SeverityLevel

# Bản đồ trọng yếu module (Module Criticality Map)
MODULE_CRITICALITY_MAP = {
    # Nhóm 4 điểm: Chặn luồng chính
    "ThanhToan": 4,
    "Payment": 4,
    "GioHang": 4,
    "Cart": 4,
    "DocTruyen": 4,
    "Reader": 4,
    "DangNhap": 4,
    "Auth": 4,

    # Nhóm 3 điểm: Sai business rule quan trọng
    "QuanLyTonKho": 3,
    "Inventory": 3,
    "GiamGia": 3,
    "Coupon": 3,
    "FlashSale": 3,
    "DonHang": 3,
    "Order": 3,

    # Nhóm 2 điểm: Chức năng phụ / tiện ích
    "TimKiem": 2,
    "Search": 2,
    "Wishlist": 2,
    "YeuThich": 2,
    "DanhGiaDoiTra": 2,
    "BaoCaoThongKe": 2,
    "GoiYSanPham": 2,
    "PhanTrang": 2,

    # Nhóm 1 điểm: Giao diện, thẩm mỹ
    "GiaoDien": 1,
    "UI": 1,
    "ChinhTa": 1,
}


def calculate_severity(
    module: str,
    actual_text: str = "",
    expected_text: str = "",
    error_details: str = "",
    is_security_concern: bool = False
) -> Tuple[SeverityLevel, int, SeverityFactors]:

    # Tính điểm Severity dựa trên 4 yếu tố I, M, W, S.
    # Trả về: (severity_level, total_score, factors)

    text_corpus = f"{module} {actual_text} {expected_text} {error_details}".lower()

    # 1. Tính I (Ảnh hưởng chức năng: 0 - 4)
    # Lấy điểm theo Module Criticality Map
    i_score = 2  # Mặc định mức 2 cho chức năng phụ
    for mod_key, score in MODULE_CRITICALITY_MAP.items():
        if mod_key.lower() in module.lower():
            i_score = score
            break

    # 2. Tính M (Tiền tệ / Dữ liệu giỏ hàng: 0 - 3)
    # 3: Mất tiền, thu tiền sai, lộ dữ liệu
    # 2: Giảm giá sai, sai tồn kho, sai đơn hàng
    # 1: Sai dữ liệu hiển thị nhỏ
    # 0: Không liên quan tiền/dữ liệu
    m_score = 0
    if any(k in text_corpus for k in ["lộ dữ liệu", "mất tiền", "thanh toán không", "payment fail"]):
        m_score = 3
    elif any(k in text_corpus for k in ["giảm giá", "voucher", "tồn kho", "stock", "giá", "tiền", "discount", "coupon", "tổng"]):
        m_score = 2
    elif any(k in text_corpus for k in ["hiển thị", "label", "tên", "tiêu đề", "title"]):
        m_score = 1

    # 3. Tính W (Khả năng né lỗi / Workaround: 0 - 2)
    # 2: Hoàn toàn không né được
    # 1: Né được nhưng khó / bất tiện
    # 0: Dễ dàng né được (click lại, reload)
    w_score = 1  # Mặc định mức 1
    if i_score >= 3 and m_score >= 2:
        w_score = 2
    elif any(k in text_corpus for k in ["chặn hoàn toàn", "cannot", "crash", "500", "not found"]):
        w_score = 2
    elif any(k in text_corpus for k in ["refresh", "thử lại", "click"]):
        w_score = 0

    # 4. Tính S (Phạm vi ảnh hưởng: 0 - 1)
    # 1: Toàn bộ người dùng / toàn bộ sản phẩm
    # 0: Chỉ trong điều kiện hẹp
    s_score = 0
    if any(k in text_corpus for k in ["all", "mọi", "toàn bộ", "homepage", "trang chủ", "search", "tìm kiếm"]):
        s_score = 1

    # QUY TẮC GHI ĐÈ BẢO MẬT (Security Override)
    if is_security_concern or any(k in text_corpus for k in ["security", "lộ dữ liệu", "xss", "sqli", "auth bypass"]):
        i_score = 4
        m_score = 3
        w_score = 2
        s_score = 1

    total_score = i_score + m_score + w_score + s_score
    total_score = min(max(total_score, 0), 10)

    # Ánh xạ Thang điểm ra Nhãn Severity
    if total_score >= 7:
        severity_level: SeverityLevel = "CRITICAL"
    elif total_score >= 5:
        severity_level: SeverityLevel = "HIGH"
    elif total_score >= 3:
        severity_level: SeverityLevel = "MEDIUM"
    else:
        severity_level: SeverityLevel = "LOW"

    factors = SeverityFactors(i=i_score, m=m_score, w=w_score, s=s_score)
    return severity_level, total_score, factors


def calculate_confidence(
    has_screenshot: bool,
    has_trace: bool,
    reproducibility: str,
    faa_confidence_score: Optional[float] = None,
    has_unknown_fields: bool = False
) -> Tuple[ReportConfidence, bool]:

    # Xác định Confidence nhãn High / Medium / Low và cờ reviewRequired.
    # Tuân thủ Mục 2.3 tài liệu nghiên cứu:
    # - Low: Thiếu evidence tối thiểu hoặc FAA confidence < 0.60 -> bắt buộc review.
    # - Medium: Đủ evidence nhưng lỗi chập chờn (Sometimes/Once) hoặc còn trường UNKNOWN.
    # - High: Đủ evidence, Always (1/1 hoặc 3/3), FAA >= 0.60 và không còn UNKNOWN.

    has_min_evidence = has_screenshot or has_trace
    faa_ok = (faa_confidence_score is None) or (faa_confidence_score >= 0.60)
    is_always = "always" in reproducibility.lower()

    if not has_min_evidence or not faa_ok:
        return "Low", True

    if not is_always or has_unknown_fields:
        return "Medium", True

    return "High", True  # Trạng thái ban đầu luôn là New và cần con người duyệt theo G10
