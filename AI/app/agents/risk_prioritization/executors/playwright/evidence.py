from pathlib import Path
from typing import Dict, List

from playwright.sync_api import Page


def capture_screenshot(page, output_path: str) -> str:
    path = Path(output_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    page.screenshot(path=str(path), full_page=True)
    return str(path)


def start_tracing(context, trace_path: str) -> None:
    path = Path(trace_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    context.tracing.start(screenshots=True, snapshots=True, sources=True)


def stop_tracing(context, trace_path: str) -> str:
    path = Path(trace_path)
    context.tracing.stop(path=str(path))
    return str(path)


def capture_video(page) -> str | None:
    video = page.video
    if video:
        return video.path()
    return None


INPUT_SELECTORS = (
    "input:not([type='submit']):not([type='button']):not([type='hidden']):not([type='reset']),"
    "textarea, select"
)
BUTTON_SELECTORS = "button, input[type='submit'], input[type='button']"
DIALOG_SELECTORS = ("[role='dialog']", "dialog", ".modal", ".modal-content", ".offcanvas", ".modal-dialog")
ERROR_SELECTORS = (
    ".invalid-feedback", "[role='alert']", ".alert-danger", ".alert-warning",
    ".alert-success", ".text-danger", ".error",
)


def _describe(locator, max_text: int = 80) -> Dict | None:
    try:
        if not locator.is_visible():
            return None
        text = (locator.inner_text(timeout=500) or "").strip()[:max_text]
        return {
            "tag": locator.evaluate("e => e.tagName.toLowerCase()"),
            "text": text,
            "placeholder": locator.get_attribute("placeholder"),
            "aria_label": locator.get_attribute("aria-label"),
            "href": locator.get_attribute("href"),
        }
    except Exception:
        return None


def _within(containers, targets) -> str:
    return ", ".join(f"{c} {t}:visible" for c in containers for t in targets)


def collect_visible_elements(page: Page, limit: int = 20) -> List[Dict]:
    groups = [
        ("input", INPUT_SELECTORS + ":visible"),
        ("dialog_button", _within(DIALOG_SELECTORS, BUTTON_SELECTORS.split(", "))),
        ("link", "a:visible"),
        ("button", ", ".join(f"{s}:visible" for s in BUTTON_SELECTORS.split(", "))),
        ("error", ", ".join(f"{s}:visible" for s in ERROR_SELECTORS)),
    ]
    elements: List[Dict] = []
    seen = set()
    for kind, selector in groups:
        if len(elements) >= limit:
            break
        try:
            locators = page.locator(selector).all()
        except Exception:
            continue
        for locator in locators:
            if len(elements) >= limit:
                break
            item = _describe(locator, 300 if kind == "error" else 80)
            if not item:
                continue
            key = (item["tag"], item["text"], item["placeholder"], item["aria_label"], item["href"])
            if key in seen:
                continue
            seen.add(key)
            item["kind"] = kind
            elements.append(item)
    return elements


def capture_failure_diagnostics(page: Page, test_case_id: str, evidence_dir: Path) -> Dict:
    evidence_dir.mkdir(parents=True, exist_ok=True)
    screenshot_path = evidence_dir / f"{test_case_id}_failure.png"
    try:
        page.screenshot(path=str(screenshot_path), full_page=True)
    except Exception:
        pass

    try:
        title = page.title()
    except Exception:
        title = ""

    return {
        "screenshot": str(screenshot_path),
        "url": page.url,
        "visible_elements": collect_visible_elements(page, limit=20),
        "page_title": title,
    }
