import logging
import re
import time
import unicodedata
from typing import Optional

from playwright.sync_api import Locator, Page

from .assertions import observed_values
from .config import BROWSER

logger = logging.getLogger(__name__)

BUTTON_SYNONYMS = {
    "lưu": ["Lưu", "Cập nhật", "Tạo mã", "Thêm mã", "Lưu lại", "Update", "Thêm", "Tạo", "Xác nhận", "Submit"],
    "cập nhật": ["Cập nhật", "Lưu", "Tạo mã", "Thêm mã", "Lưu lại", "Update", "Xác nhận"],
    "tạo mã": ["Thêm", "Tạo mã", "Thêm mã", "Thêm mới", "Tạo", "Add"],
    "thêm mã": ["Thêm", "Tạo mã", "Thêm mã", "Thêm mới", "Tạo", "Add"],
    "xác nhận": ["OK", "Confirm", "Submit", "Đồng ý", "Xác nhận"],
    "thêm vào giỏ hàng": ["Thêm vào giỏ", "Add to cart", "Thêm"],
    "áp dụng": ["Áp dụng", "Apply", "Sử dụng"],
    "tìm kiếm": ["Tìm", "Search", "Tìm kiếm"],
    "xóa": ["Xóa", "Delete", "Remove", "Có", "Yes"],
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


_CLICKABLE = "a[href], button, [role=button], [role=link], input[type=submit], input[type=button]"
_TAG_JS = """([selector, target, exact]) => {
  const f = s => (s || '').normalize('NFD').replace(/[̀-ͯ]/g, '')
      .replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().replace(/\\s+/g, ' ').trim();
  const want = f(target);
  document.querySelectorAll('[data-pw-fold]').forEach(e => e.removeAttribute('data-pw-fold'));
  for (const el of document.querySelectorAll(selector)) {
    if (!el.getClientRects().length) continue;
    if (el.closest('[aria-hidden="true"]')) continue;
    if (el.classList.contains('badge')) continue;
    if (el.disabled) continue;
    const st = getComputedStyle(el);
    if (st.visibility === 'hidden' || st.display === 'none') continue;
    const vals = [el.getAttribute('aria-label') || '', el.getAttribute('title') || '',
                  el.value || '', el.innerText || el.textContent || ''].map(f);
    if (vals.some(v => exact ? v === want : v.includes(want))) {
      el.setAttribute('data-pw-fold', 'hit');
      return true;
    }
  }
  return false;
}"""

_ACTION_PATTERNS = {
    "delete": r"trash|delete|xoa|remove",
    "edit": r"edit|pen|pencil|sua",
    "view": r"eye|view|xem",
}


def fold(text: str) -> str:
    """Accent- and case-insensitive normalization.
    NFD decompose → strip combining marks → đ→d → lowercase → collapse spaces"""
    base = "".join(
        c for c in unicodedata.normalize("NFD", text or "")
        if not unicodedata.combining(c)
    )
    base = base.replace("đ", "d").replace("Đ", "D").lower()
    return " ".join(base.split())


_fold = fold


def _names(target: str) -> list[str]:
    names = [target]
    key = fold(target.strip())
    for synonym_key, synonyms in BUTTON_SYNONYMS.items():
        if key in fold(synonym_key) or fold(synonym_key) in key:
            names.extend(s for s in synonyms if s not in names)
    return names


def _tag(scope, selector: str, target: str, exact: bool) -> bool:
    try:
        return bool(scope.evaluate(_TAG_JS, [selector, target, exact]))
    except Exception:
        return False


def _folded(scope, target: str, exact: bool) -> Optional[Locator]:
    if not _tag(scope, _CLICKABLE, target, exact):
        return None
    hit = scope.locator("[data-pw-fold='hit']").first
    try:
        return hit if hit.count() else None
    except Exception:
        return None


def _bare(build) -> Optional[Locator]:
    try:
        locator = build().first
    except Exception:
        return None
    if locator.count() == 0:
        return None
    try:
        if locator.evaluate(
            "el => el.classList.contains('badge') || !!el.closest('[aria-hidden=\"true\"]')",
            timeout=BROWSER["timeout_probe"],
        ):
            return None
    except Exception:
        return None
    return locator


def _row_action(target: str) -> Optional[str]:
    folded = fold(target)
    if "xoa" in folded or "delete" in folded:
        return "delete"
    if "sua" in folded or "edit" in folded:
        return "edit"
    return None


def resolve_row_button(scope, row_text: str = "", action: str = "delete") -> Optional[Locator]:
    pattern = re.compile(_ACTION_PATTERNS.get(action, _ACTION_PATTERNS["delete"]), re.IGNORECASE)
    rows = scope.locator("tr")
    try:
        total = min(rows.count(), 50)
    except Exception:
        return None
    chosen = None
    for i in range(total):
        row = rows.nth(i)
        try:
            cls = row.get_attribute("class") or ""
            if "pagination-row" in cls or row.locator("td").count() == 0:
                continue
            if row_text and fold(row_text) not in fold(row.inner_text(timeout=300)):
                continue
            chosen = row
            break
        except Exception:
            continue
    if chosen is None:
        return None
    buttons = chosen.locator("a, button")
    try:
        count = buttons.count()
    except Exception:
        return None
    for i in range(count):
        button = buttons.nth(i)
        try:
            blob = " ".join(filter(None, [
                button.get_attribute("class") or "",
                button.get_attribute("aria-label") or "",
                button.get_attribute("title") or "",
                button.inner_html(timeout=300) or "",
            ]))
        except Exception:
            continue
        if pattern.search(fold(blob)):
            return button
    if count == 0:
        return None
    return buttons.nth(count - 1 if action == "delete" else 0)


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


def _probe(build, strategy: str, target: str) -> Optional[Locator]:
    started = time.perf_counter()
    locator = None
    try:
        locator = build()
        if locator is None:
            raise RuntimeError("no candidate")
        locator = locator.first
        locator.wait_for(state="visible", timeout=BROWSER["timeout_probe"])
    except Exception:
        locator = None
    logger.info(
        "click target=%r strategy=%s probe_ms=%s",
        target, strategy, int((time.perf_counter() - started) * 1000),
    )
    return locator


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


def _strategies(scope, page, name: str):
    yield "role-button", lambda: scope.get_by_role("button", name=re.compile(re.escape(name), re.IGNORECASE))
    yield "role-link", lambda: scope.get_by_role("link", name=re.compile(re.escape(name), re.IGNORECASE))
    yield "folded-exact", lambda: _folded(scope, name, True)
    action = _row_action(name)
    if action and scope is page:
        values = observed_values()
        yield "row-button", lambda: (
            resolve_row_button(page, values[0], action) or resolve_row_button(page, "", action)
            if values else resolve_row_button(page, "", action)
        )
    yield "folded-contains", lambda: _folded(scope, name, False)
    yield "input-value", lambda: (
        scope.locator("[data-pw-fold='hit']").first
        if _tag(scope, "input[type=submit], input[type=button]", name, False)
        else None
    )
    for build in _click_candidates(scope, name):
        yield "bare-text", lambda b=build: _bare(b)


def _visible_modal(page: Page) -> Optional[Locator]:
    try:
        modal = page.locator(".popup-overlay, .modal.show, .delete-modal.show, [role=dialog]")
        for i in range(min(modal.count(), 10)):
            candidate = modal.nth(i)
            if candidate.is_visible():
                return candidate
    except Exception:
        pass
    return None


def resolve_click_target(page: Page, target: str, timeout: int = 5000) -> Optional[Locator]:
    if not (target or "").strip():
        return None
    modal = _visible_modal(page)
    scopes = [modal, page] if modal is not None else [page]
    first = None
    for scope in scopes:
        for name in _names(target):
            for strategy, build in _strategies(scope, page, name):
                if first is None:
                    first = build
                locator = _probe(build, strategy, target)
                if locator is not None:
                    return locator
    if first is not None:
        started = time.perf_counter()
        try:
            locator = first().first
            locator.wait_for(state="visible", timeout=BROWSER["timeout_candidate"])
            logger.info(
                "click target=%r strategy=first-retry probe_ms=%s",
                target, int((time.perf_counter() - started) * 1000),
            )
            return locator
        except Exception:
            logger.info(
                "click target=%r strategy=first-retry probe_ms=%s",
                target, int((time.perf_counter() - started) * 1000),
            )
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
