from __future__ import annotations

import json
import logging
import time
from datetime import datetime, timezone
from pathlib import Path
from tempfile import TemporaryDirectory

from playwright.sync_api import Page, sync_playwright

from .....storage.evidence_store import EvidenceStore
from .....storage.store import RunStore
from ...orchestrator.models import ExecutionResult, ExecutionState
from .assertions import begin_observation, remember_value, verify_expected_result
from .auth import detect_login_wall, ensure_logged_in
from .config import AUTH, BROWSER, BROWSER_TYPE, HEADLESS
from .evidence import (
    capture_failure_diagnostics,
    capture_video,
    start_tracing,
    stop_tracing,
)
from .locator_resolver import (
    adjust_quantity,
    fill_form_with_data,
    resolve_click_target,
    resolve_fill_target,
    resolve_search_confirm,
)
from .module_url_map import KEYWORDS, _MAP as MODULE_MAP, _join as join_path
from .step_parser import ParsedAction, fold, parse_step

logger = logging.getLogger(__name__)

LOGIN_FEATURES = (
    "dang nhap", "login", "logout", "dang xuat", "dang ky", "register",
    "signin", "sign in", "sign up",
)
GUEST_HINTS = ("chua dang nhap", "khach vang lai", "khong dang nhap", "het han")
FIELD_VALUES = {
    "ma voucher": "SALE10",
    "ma giam gia": "SALE10",
    "voucher": "SALE10",
    "email": "test@example.com",
    "mat khau": "Test@1234",
    "ten dang nhap": "testuser",
    "so dien thoai": "0912345678",
    "dia chi": "Hà Nội",
    "tu khoa": "truyện",
    "ten tac gia": "Tác giả",
}
PRODUCT_SELECTORS = (
    "a[href*='comic-detail']",
    ".product-card a",
    ".comic-card a",
    ".product-item a",
)


def is_manual_test_case(test_case_id: str) -> bool:
    return test_case_id.startswith("TaiKhoanPhanQuyen-TC-")


def _store(run_dir, tc_id: str, attempt: int, artifact_type: str, path: str | None) -> str | None:
    if not path or not Path(path).is_file():
        return None
    return EvidenceStore(run_dir).save_artifact(tc_id, attempt, artifact_type, path)


def _as_list(value) -> list[str]:
    if isinstance(value, str):
        return [value]
    return list(value or [])


def _module_config(module: str) -> dict:
    return MODULE_MAP.get("modules", {}).get(module) or {}


def _start_url(module: str) -> str:
    return join_path(_module_config(module).get("start_url", "/home"))


def _infer_role(tc: dict) -> str:
    title = fold(tc.get("title") or "")[0]
    pre = fold(" ".join(_as_list(tc.get("preconditions"))))[0]
    steps = " ".join(_as_list(tc.get("steps")))
    if any(k in title for k in LOGIN_FEATURES):
        return "none"
    if any(k in pre for k in GUEST_HINTS):
        return "none"
    admin_words = [fold(k)[0] for k in AUTH["admin_keywords"]]
    required = _module_config(tc.get("module", "")).get("requires_login")
    if required == "admin" or any(k in pre for k in admin_words):
        return "admin"
    if any(k in title for k in admin_words) or "/admin/" in steps:
        return "admin"
    if required == "user" or any(k in pre for k in ("dang nhap", "checkout", "vi voucher")):
        return "user"
    return "none"


def _click(locator, timeout: int) -> str:
    try:
        locator.click(timeout=timeout)
        return "normal"
    except Exception as first:
        try:
            locator.click(force=True, timeout=timeout)
            return "force"
        except Exception:
            try:
                locator.dispatch_event("click")
                return "dispatch"
            except Exception:
                raise first


def _cart_reload(page: Page, target: str) -> bool:
    low = fold(target or "")[0]
    if low == "cap nhat gio hang" or "cap nhat gio" in low:
        try:
            page.reload(wait_until="domcontentloaded", timeout=BROWSER["timeout_navigation"])
            _settle(page)
            logger.info("step target=%r strategy=reload", target)
            return True
        except Exception:
            pass
    return False


def _settle(page: Page) -> None:
    wait = BROWSER["wait_after_nav"]
    try:
        if wait["domcontentloaded"]:
            page.wait_for_load_state("domcontentloaded")
    except Exception:
        pass
    try:
        page.wait_for_selector(wait["element"], state="attached", timeout=wait["element_timeout"])
    except Exception:
        pass


def _run_setup(page: Page, setup: str | None) -> None:
    if setup != "open_first_product":
        return
    for selector in PRODUCT_SELECTORS:
        card = page.locator(selector).first
        try:
            card.wait_for(state="visible", timeout=3000)
            card.click()
            _settle(page)
            return
        except Exception:
            continue


def _path_for(target: str, module: str) -> str | None:
    pages = _module_config(module).get("pages", {})
    low = fold(target)[0]
    for fragment, page_key in KEYWORDS:
        if fold(fragment)[0] in low and page_key in pages:
            return pages[page_key]
    return None


def _navigate(page: Page, action: ParsedAction, module: str, start_url: str) -> None:
    if (action.metadata or {}).get("reload"):
        page.reload(wait_until="domcontentloaded", timeout=BROWSER["timeout_navigation"])
        _settle(page)
        return
    path = _path_for(action.target, module)
    if path:
        page.goto(join_path(path), wait_until="domcontentloaded", timeout=BROWSER["timeout_navigation"])
        _settle(page)
        return
    link = resolve_click_target(page, action.target, timeout=BROWSER["timeout_candidate"])
    if link is not None:
        mode = _click(link, BROWSER["timeout_candidate"])
        logger.info("navigate target=%r click=%s", action.target, mode)
        _settle(page)
        return
    page.goto(start_url, wait_until="domcontentloaded", timeout=BROWSER["timeout_navigation"])
    _settle(page)


def _value_for(target: str, metadata: dict) -> str:
    low = fold(target or "")[0]
    for key, value in FIELD_VALUES.items():
        if key in low:
            return value
    if metadata.get("valid") is False:
        return "!"
    value = f"Test {datetime.now().strftime('%H%M%S%f')}"
    remember_value(value)
    return value


def _open_form(page: Page, timeout: int) -> bool:
    for name in ("Thêm", "Tạo", "Thêm mới", "Tạo mới", "Thêm mã", "Tạo mã"):
        button = resolve_click_target(page, name, timeout)
        if button is None:
            continue
        mode = _click(button, BROWSER["timeout_candidate"])
        logger.info("form target=%r click=%s", name, mode)
        _settle(page)
        return True
    return False


def _fill(page: Page, action: ParsedAction, timeout: int) -> None:
    metadata = action.metadata or {}
    target = fold(action.target or "")[0]
    if "so luong" in target or " sl" in target:
        quantity = action.value
        if quantity is None:
            quantity = metadata.get("quantity")
        if quantity is not None and adjust_quantity(page, int(quantity), timeout):
            return
        raise RuntimeError(f"quantity control not found for '{action.target}'")
    if target in ("", "form", "thong tin"):
        valid = metadata.get("valid", True)
        if fill_form_with_data(page, valid):
            return
        if _open_form(page, timeout) and fill_form_with_data(page, valid):
            return
        raise RuntimeError("no fillable form fields")
    field = resolve_fill_target(page, action.target, timeout)
    hint = metadata.get("field")
    if field is None and hint:
        field = resolve_fill_target(page, hint, timeout)
    if field is None:
        raise RuntimeError(f"fill target not found: {action.target}")
    field.fill(action.value or _value_for(hint or action.target, metadata), timeout=timeout)


def _search(page: Page, action: ParsedAction, timeout: int) -> None:
    query = (action.metadata or {}).get("query")
    folded = fold(query or "")[0]
    if query and len(query) <= 40 and "hoac" not in folded and "duyet" not in folded:
        field = resolve_fill_target(page, "tìm kiếm", timeout)
        if field is not None:
            field.fill(str(query), timeout=timeout)
    if not resolve_search_confirm(page, timeout):
        raise RuntimeError("search confirm control not found")


def _blank(page: Page, action: ParsedAction, timeout: int) -> None:
    field = resolve_fill_target(page, action.target or "tìm kiếm", timeout)
    if field is None:
        raise RuntimeError(f"blank target not found: {action.target}")
    field.fill("", timeout=timeout)


def _dispatch(page: Page, action: ParsedAction, module: str, start_url: str, timeout: int) -> None:
    kind = action.action
    if kind == "navigate":
        _navigate(page, action, module, start_url)
    elif kind == "click":
        started = time.perf_counter()
        target = resolve_click_target(page, action.target, timeout)
        if target is None:
            if _cart_reload(page, action.target):
                return
            raise RuntimeError(f"click target not found: {action.target}")
        mode = _click(target, BROWSER["timeout_candidate"])
        logger.info(
            "click target=%r mode=%s elapsed_ms=%s",
            action.target, mode, int((time.perf_counter() - started) * 1000),
        )
    elif kind == "fill":
        _fill(page, action, timeout)
    elif kind == "search":
        _search(page, action, timeout)
    elif kind == "blank":
        _blank(page, action, timeout)
    elif kind == "observe":
        seconds = (action.metadata or {}).get("seconds")
        page.wait_for_timeout(int(seconds) * 1000 if seconds else 200)
    else:
        page.wait_for_timeout(200)


def _run_step(page: Page, context, action: ParsedAction, module: str, start_url: str, index: int = 0) -> None:
    timeout = BROWSER["timeout_step"]
    before_url = page.url
    started = time.perf_counter()
    try:
        _dispatch(page, action, module, start_url, timeout)
        logger.info(
            "step %s action=%s target=%s elapsed_ms=%s",
            index, action.action, action.target, int((time.perf_counter() - started) * 1000),
        )
        return
    except Exception:
        role = detect_login_wall(page)
        if role is None:
            raise
        ensure_logged_in(context, role)
        if page.url != before_url:
            page.goto(before_url, wait_until="domcontentloaded", timeout=BROWSER["timeout_navigation"])
            _settle(page)
    _dispatch(page, action, module, start_url, timeout)
    logger.info(
        "step %s action=%s target=%s retried elapsed_ms=%s",
        index, action.action, action.target, int((time.perf_counter() - started) * 1000),
    )


def _steps(tc: dict) -> list[str]:
    steps = tc.get("steps", tc.get("test_steps", []))
    if isinstance(steps, str):
        return [s.strip() for s in steps.split("\n") if s.strip()]
    return list(steps or [])


def _failure(page: Page, tc_id: str, attempt: int, run_dir, evidence_base: Path,
             evidence_uris: list, reason: Exception) -> tuple[str, str]:
    diagnostics = capture_failure_diagnostics(page, tc_id, evidence_base / "diagnostics")
    saved = _store(run_dir, tc_id, attempt, "screenshot", diagnostics.get("screenshot"))
    if saved:
        evidence_uris.append(saved)
        diagnostics["screenshot"] = saved
    payload = {"reason": str(reason), **diagnostics}
    return "FAILED", json.dumps(payload, ensure_ascii=False, default=str)


class PlaywrightExecutor:
    run_dir: Path | None = None

    def execute_test_case(self, tc: dict, run_id: str, attempt: int) -> ExecutionResult:
        tc_id = tc.get("test_case_id", tc.get("id", "unknown"))

        if is_manual_test_case(tc_id):
            return ExecutionResult(
                test_case_id=tc_id,
                state=ExecutionState.MANUAL,
                attempt=attempt,
                start_time=datetime.now(timezone.utc),
                end_time=datetime.now(timezone.utc),
                duration_ms=0,
                evidence_uris=[],
                error_message="Test requires manual DB/network inspection"
            )

        run_dir = self.run_dir or RunStore().run_dir(run_id)
        start_time = datetime.now()
        started = time.perf_counter()
        evidence_uris: list[str] = []
        state = "PASSED"
        error_message = None

        try:
            with TemporaryDirectory() as tmp:
                evidence_base = Path(tmp)
                with sync_playwright() as p:
                    browser = p[BROWSER_TYPE].launch(headless=HEADLESS)
                    try:
                        context = browser.new_context(viewport=BROWSER["viewport"])
                        page = context.new_page()
                        page.set_default_timeout(BROWSER["timeout_step"])

                        trace_path = evidence_base / "trace.zip"
                        start_tracing(context, str(trace_path))

                        try:
                            module = tc.get("module", "")
                            role = _infer_role(tc)
                            start_url = _start_url(module)

                            phase = time.perf_counter()
                            if role != "none":
                                ensure_logged_in(context, role)
                            logger.info("tc %s login_ms=%s", tc_id, int((time.perf_counter() - phase) * 1000))

                            phase = time.perf_counter()
                            page.goto(
                                start_url,
                                wait_until="domcontentloaded",
                                timeout=BROWSER["timeout_navigation"],
                            )
                            _settle(page)
                            logger.info("tc %s navigation_ms=%s", tc_id, int((time.perf_counter() - phase) * 1000))

                            phase = time.perf_counter()
                            _run_setup(page, _module_config(module).get("setup"))
                            begin_observation(page)
                            logger.info("tc %s setup_ms=%s", tc_id, int((time.perf_counter() - phase) * 1000))

                            for index, step in enumerate(_steps(tc)):
                                _run_step(page, context, parse_step(step), module, start_url, index)

                            phase = time.perf_counter()
                            passed, reason = verify_expected_result(
                                page, tc.get("expected_result", ""), timeout=BROWSER["timeout_step"]
                            )
                            logger.info("tc %s verify_ms=%s", tc_id, int((time.perf_counter() - phase) * 1000))
                            if not passed:
                                state, error_message = _failure(
                                    page, tc_id, attempt, run_dir, evidence_base, evidence_uris,
                                    AssertionError(reason),
                                )
                        except Exception as exc:
                            state, error_message = _failure(
                                page, tc_id, attempt, run_dir, evidence_base, evidence_uris, exc
                            )
                        finally:
                            stop_tracing(context, str(trace_path))
                            evidence_uris.append(_store(run_dir, tc_id, attempt, "trace", str(trace_path)))
                            try:
                                evidence_uris.append(_store(run_dir, tc_id, attempt, "video", capture_video(page)))
                            except Exception:
                                pass
                    finally:
                        browser.close()

        except Exception as exc:
            state = "ERROR"
            error_message = f"Browser launch failed: {exc}"

        end_time = datetime.now()
        duration_ms = int((end_time - start_time).total_seconds() * 1000)
        logger.info("tc %s total_ms=%s", tc_id, int((time.perf_counter() - started) * 1000))

        return ExecutionResult(
            test_case_id=tc_id,
            state=state,
            attempt=attempt,
            start_time=start_time,
            end_time=end_time,
            duration_ms=duration_ms,
            evidence_uris=[uri for uri in evidence_uris if uri],
            error_message=error_message
        )
