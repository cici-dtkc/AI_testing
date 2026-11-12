# Module Lọc trùng và Gom nhóm lỗi (Deduplication & Fingerprinting).
# Tuân thủ Bước 5 trong tài liệu nghiên cứu:
# fingerprint = hash(module + assertion_type + endpoint_chuan_hoa + http_status + normalized_error)


from __future__ import annotations

import hashlib
import re
from typing import Dict, List, Optional, Tuple
from .schemas import Deduplication


def normalize_error_message(msg: str) -> str:
    # Chuẩn hóa thông báo lỗi: loại bỏ số, ID, GUID, timestamp, địa chỉ bộ nhớ
    # để các lỗi cùng bản chất tạo ra cùng một dấu vân tay (fingerprint).
    if not msg:
        return ""
    # Chuyển chữ thường
    text = msg.lower()
    # Loại bỏ hex / memory addresses / GUID
    text = re.sub(r"0x[0-9a-f]+", "", text)
    text = re.sub(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "", text)
    # Loại bỏ timestamp dạng ISO hoặc số ngày giờ
    text = re.sub(r"\d{4}-\d{2}-\d{2}t\d{2}:\d{2}:\d{2}", "", text)
    # Loại bỏ số ID cụ thể
    text = re.sub(r"\b\d+\b", "", text)
    # Rút gọn khoảng trắng
    text = re.sub(r"\s+", " ", text).strip()
    return text


def generate_fingerprint(
    module: str,
    endpoint: str = "",
    assertion_type: str = "ui_assertion",
    http_status: str = "200",
    error_message: str = ""
) -> str:
    # Tạo mã băm fingerprint đại diện cho lỗi (ví dụ: fp_a8f9c2)
    norm_error = normalize_error_message(error_message)
    norm_endpoint = endpoint.split("?")[0].strip().lower()  # bỏ query params
    
    raw_key = f"{module.strip().lower()}|{assertion_type.strip().lower()}|{norm_endpoint}|{http_status}|{norm_error}"
    digest = hashlib.md5(raw_key.encode("utf-8")).hexdigest()[:8]
    return f"fp_{digest}"


def check_duplicate(
    new_fingerprint: str,
    existing_fingerprints: Dict[str, str]
) -> Deduplication:
    # Kiểm tra xem fingerprint đã tồn tại trong danh sách bug đang mở chưa.
    # existing_fingerprints: Dict[fingerprint, bugId]
    if new_fingerprint in existing_fingerprints:
        return Deduplication(
            fingerprint=new_fingerprint,
            isDuplicate=True,
            duplicateOf=existing_fingerprints[new_fingerprint]
        )
    return Deduplication(
        fingerprint=new_fingerprint,
        isDuplicate=False,
        duplicateOf=None
    )
