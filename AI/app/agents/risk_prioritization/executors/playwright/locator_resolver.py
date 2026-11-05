import re
import unicodedata
from typing import Optional

from playwright.sync_api import Locator, Page

from .config import BROWSER

BUTTON_SYNONYMS = {
    "lưu": ["Lưu", "Cập nhật", "Tạo mã", "Thêm mã", "Lưu lại", "Update", "Thêm", "Tạo", "Xác nhận", "Submit"],
    "cập nhật": ["Cập nhật", "Lưu", "Tạo mã", "Thêm mã", "Lưu lại", "Update", "Xác nhận"],
    "tạo mã": ["Thêm", "Tạo mã", "Thêm mã", "Thêm mới", "Tạo", "Add"],
    "thêm mã": ["Thêm", "Tạo mã", "Thêm mã", "Thêm mới", "Tạo", "Add"],
    "xác nhận": ["OK", "Confirm", "Submit", "Đồng ý", "Xác nhận"],
    "thêm vào giỏ hàng": ["Thêm vào giỏ", "Add to cart", "Thêm"],
    "áp dụng": ["Áp dụng", "Apply", "Sử dụng"],
    "tìm kiếm": ["Tìm", "Search", "Tìm kiếm"],
    "xóa": ["Xóa", "Delete", "Remove"],
    "đăng nhập": ["Đăng nhập", "Login", "Sign in"],
    "đăng ký": ["Đăng ký", "Register", "Sign up"],
    "thanh toán": ["Thanh toán", "Checkout", "Đặt hàng"],
}

SEARCH_WORDS = ("tìm kiếm", "tìm", "search", "keyword", "từ khóa")

FIELD_TYPES = [
    ("email", "email"),
    ("mail", "email"),
    ("phone", "tel"),
    ("điện thoại", "tel"),
    ("sdt", "tel"),
    ("số lượng", "number"),
    ("quantity", "number"),
    ("ngày", "date"),
    ("date", "date"),
    ("mật khẩu", "password"),
    ("password", "password"),
]


def _fold(text: str) -> str:
    base = "".join(
        c for c in unicodedata.normalize("NFD", text)
        if not unicodedata.combining(c)
    )
    return base.replace("đ", "d").replace("Đ", "D").lower()


def _names(target: str) -> list[str]:
    names = [target]
    key = target.strip().lower()
    for synonym_key, synonyms in BUTTON_SYNONYMS.items():
        if key in synonym_key or synonym_key in key:
            names.extend(s for s in synonyms if s not in names)
    return names


def _first_visible(candidates, timeout: int) -> Optional[Locator]:
    builders = list(candidates)
    probe = BROWSER["timeout_probe"]
    for build in builders:
        try:
            locator = build().first
            locator.wait_for(state="visible", timeout=probe)
            return locator
        except Exception:
            continue
    if builders:
        try:
            locator = builders[0]().first
            locator.wait_for(state="visible", timeout=BROWSER["timeout_candidate"])
            return locator
        except Exception:
            pass
    return None


def _click_candidates(scope, name: str):
    yield lambda: scope.get_by_role("button", name=re.compile(re.escape(name), re.IGNORECASE))
    yield lambda: scope.get_by_role("button", name=name)
    yield lambda: scope.get_by_role("link", name=re.compile(re.escape(name), re.IGNORECASE))
    yield lambda: scope.get_by_role("link", name=name)
    yield lambda: scope.get_by_text(name, exact=True)
    yield lambda: scope.get_by_text(re.compile(re.escape(name), re.IGNORECASE))
    yield lambda: scope.get_by_text(name)
    yield lambda: scope.locator(f"input[type='submit'][value*='{name}'], input[type='button'][value*='{name}']")
    yield lambda: scope.locator(f"button:has-text('{name}')")


def _visible_modal(page: Page) -> Optional[Locator]:
    try:
        modal = page.locator("[role=dialog], .modal.show").first
        if modal.is_visible():
            return modal
    except Exception:
        pass
    return None


def resolve_click_target(page: Page, target: str, timeout: int = 5000) -> Optional[Locator]:
    if not (target or "").strip():
        return None
    modal = _visible_modal(page)
    for name in _names(target):
        if modal is not None:
            locator = _first_visible(list(_click_candidates(modal, name)), timeout)
            if locator:
                return locator
        locator = _first_visible(list(_click_candidates(page, name)), timeout)
        if locator:
            return locator
    return None


def _fill_candidates(page: Page, target: str):
    yield lambda: page.get_by_placeholder(target)
    yield lambda: page.get_by_label(target)
    yield lambda: page.get_by_role("textbox", name=target)
    yield lambda: page.locator(f"[name*='{target}'], [id*='{_fold(target).replace(' ', '')}']")
    if any(w in target.lower() for w in SEARCH_WORDS):
        for word in SEARCH_WORDS:
            yield lambda w=word: page.get_by_placeholder(re.compile(w, re.IGNORECASE))
            yield lambda w=word: page.get_by_role("searchbox", name=re.compile(w, re.IGNORECASE))
        yield lambda: page.get_by_role("searchbox")


def resolve_fill_target(page: Page, target: str, timeout: int = 5000) -> Optional[Locator]:
    if not (target or "").strip():
        return None
    folded = _fold(target)
    search = any(w in folded for w in ("tim kiem", "tu khoa"))
    if search:
        target = "tìm kiếm"
    return _first_visible(list(_fill_candidates(page, target)), timeout)


def _search_inputs(page: Page):
    yield page.get_by_role("searchbox")
    for word in SEARCH_WORDS:
        yield page.get_by_placeholder(re.compile(word, re.IGNORECASE))
    yield page.locator("input[type='search']")
    yield page.locator("input[name*='search'], input[id*='search'], input[name*='keyword']")


def _visible(locator: Locator) -> bool:
    try:
        return locator.count() > 0 and locator.first.is_visible()
    except Exception:
        return False


def resolve_search_confirm(page: Page, timeout: int = 5000) -> bool:
    for locator in _search_inputs(page):
        if not _visible(locator):
            continue
        try:
            locator.first.press("Enter", timeout=timeout)
            return True
        except Exception:
            continue
    for name in ("Tìm kiếm", "Tìm", "Search"):
        button = resolve_click_target(page, name, timeout)
        if button:
            try:
                button.click(timeout=timeout)
                return True
            except Exception:
                continue
    return False


def _field_kind(locator: Locator) -> str:
    try:
        descriptor = " ".join(
            filter(None, [
                locator.get_attribute("name") or "",
                locator.get_attribute("id") or "",
                locator.get_attribute("placeholder") or "",
                locator.get_attribute("type") or "",
                locator.get_attribute("aria-label") or "",
            ])
        ).lower()
    except Exception:
        return "text"
    for hint, kind in FIELD_TYPES:
        if hint in descriptor or hint in _fold(descriptor):
            return kind
    return "text"


def _value_for(kind: str, valid: bool) -> str:
    table = {
        "email": ("test@example.com", "invalid-email"),
        "tel": ("0912345678", "abc"),
        "number": ("10", "-1"),
        "date": ("01/01/2026", "99/99/9999"),
        "password": ("Test@1234", "123"),
        "text": ("Test Value", "!"),
    }
    return table.get(kind, table["text"])[0 if valid else 1]


def fill_form_with_data(page: Page, valid: bool = True) -> int:
    form = page.locator("form").first
    scope = form if _visible(form) else page.locator("body")
    selector = "input:not([type='hidden']):not([type='submit']):not([type='button'])"
    selector += ":not([type='file']):not([type='checkbox']):not([type='radio']), select, textarea"

    filled = 0
    count = 0
    try:
        count = scope.locator(selector).count()
    except Exception:
        return 0

    for i in range(count):
        control = scope.locator(selector).nth(i)
        try:
            if not control.is_visible() or control.is_disabled():
                continue
            if control.evaluate("el => el.readOnly"):
                continue
        except Exception:
            continue

        tag = (control.evaluate("el => el.tagName") or "").lower()
        try:
            if tag == "select":
                options = control.locator("option").all()
                values = [o.get_attribute("value") for o in options]
                values = [v for v in values if v]
                if values:
                    control.select_option(values[-1 if not valid else 1] if len(values) > 1 else values[0])
                    filled += 1
                continue
            kind = _field_kind(control)
            control.fill(_value_for(kind, valid))
            filled += 1
        except Exception:
            continue
    return filled


def adjust_quantity(page: Page, quantity: int, timeout: int = 5000) -> bool:
    for candidate in (
        page.locator("input[name*='quantity'], input[name*='soLuong'], input[type='number']"),
        page.get_by_role("spinbutton"),
    ):
        if _visible(candidate):
            try:
                candidate.first.fill(str(quantity), timeout=timeout)
                return True
            except Exception:
                continue

    for name in ("+", "Tăng", "Increase"):
        button = resolve_click_target(page, name, timeout)
        if not button:
            continue
        try:
            for _ in range(max(0, quantity - 1)):
                button.click(timeout=timeout)
            return True
        except Exception:
            continue
    return False
