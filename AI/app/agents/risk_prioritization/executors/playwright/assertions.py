import re
import time
from typing import Tuple

from playwright.sync_api import Page

FEEDBACK_SELECTORS = (
    ".invalid-feedback, [role='alert'], [role='status'], .alert, .toast, .toast-body, "
    ".text-danger, .swal2-popup"
)
MODAL_SELECTORS = "dialog[open], [role='dialog'], .modal.show, .modal[style*='display: block']"
LIST_SELECTORS = "table, tbody, ul, ol, .list, .list-group, .table, .row"

ERROR_KIND = re.compile(
    r"báo\s*lỗi|thông báo lỗi|hiển thị lỗi|không hợp lệ|yêu cầu nhập|vui lòng nhập|"
    r"chặn|vượt quá|tối thiểu|không tồn tại|hết hạn|hết lượt|invalid|error|required",
    re.IGNORECASE,
)
ABSENCE_KIND = re.compile(
    r"không hiển thị|không xuất hiện|ẩn khỏi|loại bỏ|không áp dụng|không thay đổi|không cho phép",
    re.IGNORECASE,
)

_observation = {"url": "", "count": 0, "modal": False, "values": []}


def begin_observation(page: Page) -> None:
    _observation["url"] = page.url
    _observation["count"] = _count(page)
    _observation["modal"] = _modal_open(page)
    _observation["values"] = []


def remember_value(value: str) -> None:
    if value and value not in _observation["values"]:
        _observation["values"].append(value)


def observed_values() -> list[str]:
    return list(_observation["values"])


def _selector(selector: str) -> str:
    return ", ".join(f"{part.strip()}:visible" for part in selector.split(","))


def _count(page: Page) -> int:
    try:
        return page.locator("body *").count()
    except Exception:
        return 0


def _modal_open(page: Page) -> bool:
    try:
        locator = page.locator(_selector(MODAL_SELECTORS))
        for i in range(min(locator.count(), 10)):
            if locator.nth(i).is_visible():
                return True
    except Exception:
        pass
    return False


def _validation(page: Page) -> list[str]:
    try:
        messages = page.evaluate(
            "() => Array.from(document.querySelectorAll('input, select, textarea'))"
            ".filter(el => el.willValidate && !el.checkValidity() && el.getClientRects().length)"
            ".map(el => (el.validationMessage || '').trim())"
        )
    except Exception:
        return []
    return [m for m in messages or [] if m][:5]


def _texts(page: Page, selector: str, limit: int = 5) -> list[str]:
    texts: list[str] = []
    try:
        locator = page.locator(_selector(selector))
        total = min(locator.count(), limit * 4)
    except Exception:
        return texts
    for i in range(total):
        if len(texts) >= limit:
            break
        try:
            text = (locator.nth(i).inner_text(timeout=300) or "").strip()
        except Exception:
            continue
        if text and text not in texts:
            texts.append(text[:160])
    return texts


def _record_visible(page: Page) -> list[str]:
    if not _observation["values"]:
        return []
    try:
        locator = page.locator(_selector(LIST_SELECTORS))
        total = min(locator.count(), 50)
        if total == 0:
            locator = page.locator("body")
            total = 1
        text = " ".join(locator.nth(i).inner_text(timeout=500) or "" for i in range(total))
    except Exception:
        return []
    low = text.lower()
    return [value for value in _observation["values"] if value.lower() in low]


def _signals(page: Page) -> dict:
    signals: dict = {}
    invalid = _validation(page)
    if invalid:
        signals["html5"] = invalid
    feedback = _texts(page, FEEDBACK_SELECTORS)
    if feedback:
        signals["feedback"] = feedback
    records = _record_visible(page)
    if records:
        signals["record"] = records
    if _observation["modal"] and not _modal_open(page):
        signals["modal_closed"] = True
    if _observation["url"] and page.url != _observation["url"]:
        signals["url"] = page.url
    count = _count(page)
    if _observation["count"] and count != _observation["count"]:
        signals["count"] = count - _observation["count"]
    return signals


def _summary(signals: dict) -> str:
    parts = []
    for key in ("html5", "feedback", "record"):
        if signals.get(key):
            parts.append(f"{key}: {signals[key][0]}")
    if signals.get("modal_closed"):
        parts.append("modal closed")
    if signals.get("url"):
        parts.append(f"url: {signals['url']}")
    if "count" in signals:
        parts.append(f"elements: {signals['count']:+d}")
    return "; ".join(parts)


def _poll(page: Page, timeout: int, accept) -> dict:
    deadline = time.monotonic() + max(0, min(timeout, 2000)) / 1000
    signals = _signals(page)
    while not accept(signals) and time.monotonic() < deadline:
        page.wait_for_timeout(150)
        signals = _signals(page)
    return signals


def verify_expected_result(page: Page, expected: str, timeout: int = 5000) -> Tuple[bool, str]:
    expected = (expected or "").strip()
    if not expected:
        return True, "No expected result specified; page loaded"

    if ERROR_KIND.search(expected):
        signals = _poll(page, timeout, lambda s: bool(s.get("html5") or s.get("feedback")))
        if signals.get("html5") or signals.get("feedback"):
            return True, f"Validation feedback visible: {_summary(signals)}"
        return False, "no validation feedback in UI (possible app defect)"

    if ABSENCE_KIND.search(expected):
        signals = _signals(page)
        if signals.get("record") or signals.get("feedback"):
            return False, f"Element still present: {_summary(signals)}"
        return True, f"Absence confirmed ({_summary(signals) or 'no new visible elements'})"

    signals = _poll(page, timeout, bool)
    if signals:
        return True, f"State changed: {_summary(signals)}"
    return False, f"Expected pattern not found: {expected}"


def verify_expected(expected_text: str, page: Page) -> Tuple[bool, str]:
    return verify_expected_result(page, expected_text)
