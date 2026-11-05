import re
import unicodedata
from dataclasses import dataclass
from typing import Dict, Optional, Tuple


@dataclass
class ParsedAction:
    action: str
    target: str
    value: Optional[str] = None
    metadata: Optional[Dict] = None


FIELDS = [
    "số lượng", "từ khóa", "mã voucher", "mã giảm giá", "tên tác giả",
    "khoảng thời gian", "email", "mật khẩu", "tên đăng nhập",
    "số điện thoại", "địa chỉ", "voucher",
]

INVALID_HINTS = ["khong hop le", "thieu", "sai dinh dang", "sai", "het han",
                 "khong ton tai", "het so luot", "da dung", "khong dung"]
VALID_HINTS = ["hop le"]

VERB_ALT = r"(?:nhan|bam|click|chon|xac nhan)"
COMPOUND = re.compile(r"\s+(?:va|sau do|roi)\s+(?=" + VERB_ALT + r"\s)")


def fold(text: str) -> Tuple[str, list]:
    out = []
    idx = []
    for i, ch in enumerate(text):
        base = "".join(
            c for c in unicodedata.normalize("NFD", ch)
            if not unicodedata.combining(c)
        )
        base = base.replace("đ", "d").replace("Đ", "D").lower()
        out.append(base)
        idx.extend([i] * len(base))
    return "".join(out), idx


def _slice(text: str, idx: list, start: int, end: int) -> str:
    if start >= end or start >= len(idx):
        return ""
    return text[idx[start]:idx[end - 1] + 1]


def _group(text: str, idx: list, match: re.Match, n: int) -> str:
    return _slice(text, idx, match.start(n), match.end(n)).strip(" .,:;'\"")


def _hints(low: str, text: str, idx: list) -> Optional[Dict]:
    md: Dict = {}
    if any(h in low for h in INVALID_HINTS):
        md["valid"] = False
    elif any(h in low for h in VALID_HINTS):
        md["valid"] = True
    if re.search(r"\bam\b", low):
        md["valid"] = False
        md["quantity"] = -1
    elif "so luong" in low or " sl " in low or re.search(r"\bthanh\s+-?\d+", low):
        m = re.search(r"-?\d+", low)
        if m:
            md["quantity"] = int(_slice(text, idx, m.start(), m.end()))
    return md or None


def _field_target(text: str, idx: list, low: str) -> Optional[str]:
    for field in FIELDS:
        f, _ = fold(field)
        pos = low.find(f)
        if pos >= 0:
            return _slice(text, idx, pos, pos + len(f))
    return None


def _quoted(text: str) -> Optional[str]:
    m = re.search(r"['\"“”‘’]([^'\"“”‘’]+)['\"“”‘’]", text)
    return m.group(1).strip() if m else None


def _strip_compound(text: str) -> str:
    low, idx = fold(text)
    m = COMPOUND.search(low)
    if m:
        return _slice(text, idx, 0, m.start()).strip()
    return text


def parse_step(step: str) -> ParsedAction:
    text = (step or "").strip().strip(".").strip()
    if not text:
        return ParsedAction("observe", "")

    text = _strip_compound(text)
    low, idx = fold(text)

    if m := re.match(r"de trong\s+(.+)", low):
        return ParsedAction("blank", _group(text, idx, m, 1))

    if re.match(r"(?:lam moi|tai lai)(?:\s+(?:lai\s+)?(?:trang|khu vuc)?\s*(.*))?$", low):
        return ParsedAction("navigate", text, metadata={"reload": True})

    if m := re.match(r"(?:mo|truy cap|tai)\s+(?:(?:lai|vao)\s+)?(.+)", low):
        return ParsedAction("navigate", _group(text, idx, m, 1))

    if m := re.match(r"cho\s+(\d+)\s+giay", low):
        return ParsedAction("observe", "wait", metadata={"seconds": int(m.group(1))})

    if re.match(r"(?:xac nhan|nhan)\s+tim kiem\b", low):
        return ParsedAction("search", "search")

    if m := re.match(r"tim kiem\s*(.*)", low):
        query = _group(text, idx, m, 1) if m.group(1) else None
        return ParsedAction("search", "search", metadata={"query": query} if query else None)

    if re.match(r"(?:nhap|thay doi|dien|dat)\s+thong tin\b", low):
        hints = _hints(low, text, idx) or {}
        hints["form_open"] = True
        return ParsedAction("fill", "form", metadata=hints)

    if m := re.match(r"(?:nhap|thay doi|dien|dat)\s+(?:so luong|sl)\b\s*(.*)", low):
        hints = _hints(low, text, idx) or {}
        value = None
        if "quantity" in hints and "so luong" in low:
            value = str(hints["quantity"])
        elif m.group(1):
            num = re.match(r"-?\d+", m.group(1))
            value = num.group(0) if num else None
        return ParsedAction("fill", "số lượng", value=value, metadata=hints or None)

    if m := re.match(r"(?:nhap|thay doi|dien|dat)\s+(.+?)\s+(?:vao|va|o|trong)\s+(.+)", low):
        field = _group(text, idx, m, 1)
        target = _group(text, idx, m, 2)
        hints = _hints(low, text, idx) or {}
        field_folded, _ = fold(field)
        is_literal = not any(
            k in field_folded for k in ("tu khoa", "ma ", "ten ", "thong tin", "khoang", "so luong")
        )
        if is_literal:
            return ParsedAction("fill", target, value=field, metadata=hints or None)
        hints["field"] = field
        if "tu khoa" in field_folded:
            hints["search"] = True
        return ParsedAction("fill", target, value=None, metadata=hints)

    if m := re.match(r"(?:nhap|thay doi|dien|dat)\s+(.+?)\s*(?:thanh\s+|la\s+|=|:)\s*(.+)", low):
        return ParsedAction("fill", _group(text, idx, m, 1), value=_group(text, idx, m, 2))

    if m := re.match(r"(?:nhap|thay doi|dien|dat)\s+(.+)", low):
        body = _group(text, idx, m, 1)
        target = _field_target(text, idx, low) or body
        hints = _hints(low, text, idx) or {}
        if "tu khoa" in fold(target)[0]:
            hints["search"] = True
        return ParsedAction("fill", target, metadata=hints or None)

    if m := re.match(r"(?:nhan|bam|click|chon)\s+(?:nut|vao muc|vao|o|link)?\s*(.+)", low):
        target = _quoted(text) or _group(text, idx, m, 1)
        target_folded = fold(target)[0]
        if "tim kiem" in target_folded:
            return ParsedAction("search", "search")
        hints = {"add_to_cart": True} if "them vao gio" in target_folded else None
        return ParsedAction("click", target, metadata=hints)

    if m := re.match(r"xac nhan\s*(.*)", low):
        target = _group(text, idx, m, 1) if m.group(1) else "Xác nhận"
        if "tim kiem" in fold(target)[0]:
            return ParsedAction("search", "search")
        return ParsedAction("click", target)

    if re.match(r"thuc hien\s+(?:mua hang|thanh toan)", low):
        return ParsedAction("click", "Thanh toán")

    if m := re.match(
        r"(?:quan sat|kiem tra|xac thuc|xac minh|thuc hien|gui|bat|he thong|trich xuat|"
        r"truy van|chinh sua|cap nhat|doi|cho|duyet)\s*(.*)",
        low,
    ):
        target = _group(text, idx, m, 1) if m.group(1) else text
        return ParsedAction("observe", target)

    return ParsedAction("observe", text, metadata={"fallback": True})
