import json
from pathlib import Path

_MAP = json.loads((Path(__file__).parent / "module_url_map.json").read_text(encoding="utf-8"))

KEYWORDS = [
    ("đăng nhập", "login"),
    ("đăng ký", "register"),
    ("đổi mật khẩu", "change_password"),
    ("cập nhật mật khẩu", "change_password"),
    ("thông tin tài khoản", "profile"),
    ("thanh toán", "checkout"),
    ("cập nhật giỏ hàng", "view"),
    ("giỏ hàng", "view"),
    ("flash sale", "list"),
    ("quản lý voucher", "admin_manage"),
    ("ví voucher", "wallet"),
    ("voucher", "apply"),
    ("bảng xếp hạng", "ranking"),
    ("tổng quan", "dashboard"),
    ("tồn kho", "inventory"),
    ("ô tìm kiếm", "search"),
    ("tìm kiếm", "search"),
    ("wishlist", "view"),
    ("trang chủ", "home"),
]


def resolve_start_url(tc: dict) -> str:
    module = _MAP["modules"].get(tc.get("module", ""), {})
    pages = module.get("pages", {})
    steps = tc.get("steps") or []

    for step in steps:
        low = step.lower()
        for fragment, page_key in KEYWORDS:
            if fragment in low and page_key in pages:
                return _join(pages[page_key])
    return _join(module.get("base_path", "/home"))


def _join(path: str) -> str:
    return _MAP["base_url"].rstrip("/") + _MAP["context_path"] + path
