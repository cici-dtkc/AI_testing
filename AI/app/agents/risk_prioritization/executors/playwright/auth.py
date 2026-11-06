"""Generic login layer for the Playwright executor."""
from __future__ import annotations

import json
import logging
import os
import re
import time
from pathlib import Path
from typing import Optional

from playwright.sync_api import BrowserContext, Page

from .config import AUTH

logger = logging.getLogger(__name__)

_LOGIN_URL_RE = re.compile(r"/login|dang-nhap|signin", re.I)
_LOGIN_LINK_RE = re.compile(r"Đăng nhập|Login", re.I)
_LOGIN_BUTTON_RE = re.compile(r"Đăng nhập|Login", re.I)
_CAPTCHA_TEXT_RE = re.compile(r"robot|captcha|không phải", re.I)
_CAPTCHA_FRAME = "iframe[src*='recaptcha'], iframe[title*='reCAPTCHA']"
_USER_FIELD_RE = re.compile(r"tên đăng nhập|username|email|tài khoản|user", re.I)
_LOGGED_IN_RE = re.compile(r"Đăng xuất|Logout|Tài khoản|Account", re.I)
_PASSWORD = "input[type=password]"

_PROTECTED_PATHS = {"admin": "/admin/dashboard", "user": "/updateUser"}


def _load_env() -> None:
    env_file = Path(__file__).resolve().parents[2] / ".env"
    if not env_file.is_file():
        return
    for line in env_file.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def _base_url() -> str:
    return AUTH["base_url"].rstrip("/")


def _url(path: str) -> str:
    return _base_url() + path


def _state_path(role: str) -> Path:
    root = Path(__file__).resolve().parents[2]
    return root / AUTH["storage_state_dir"] / f"{role}.json"


def _credentials(role: str) -> tuple[str, str]:
    _load_env()
    spec = AUTH["credentials"].get(role)
    if not spec:
        raise ValueError(f"unknown role: {role}")
    user = os.environ.get(spec["user_env"])
    password = os.environ.get(spec["pass_env"])
    if not user or not password:
        raise RuntimeError(
            f"missing credentials for role '{role}': set {spec['user_env']} and {spec['pass_env']}"
        )
    return user, password


def _headed() -> bool:
    _load_env()
    return os.environ.get("PLAYWRIGHT_HEADLESS", "true").strip().lower() == "false"


def _first_visible(locator):
    try:
        count = locator.count()
    except Exception:
        return None
    for i in range(count):
        candidate = locator.nth(i)
        try:
            if candidate.is_visible():
                return candidate
        except Exception:
            continue
    return None


def _has_password_input(page: Page) -> bool:
    return _first_visible(page.locator(_PASSWORD)) is not None


def detect_login_wall(page: Page) -> Optional[str]:
    url = page.url or ""
    if not _LOGIN_URL_RE.search(url) and not _has_password_input(page):
        return None
    return "admin" if "/admin/" in url else "user"


def _apply_storage_state(context: BrowserContext, data: dict) -> None:
    cookies = data.get("cookies") or []
    if cookies:
        try:
            context.add_cookies(cookies)
        except Exception as exc:
            logger.warning("ignoring stale cookies in saved session: %s", exc)
    for origin in data.get("origins") or []:
        origin_url = origin.get("origin")
        if not origin_url:
            continue
        items = {
            item["name"]: item["value"]
            for item in origin.get("localStorage") or []
            if item.get("name")
        }
        if not items:
            continue
        context.add_init_script(
            script=(
                f"if (location.origin === {json.dumps(origin_url)}) {{"
                f"const items = {json.dumps(items)};"
                "for (const [k, v] of Object.entries(items)) localStorage.setItem(k, v);"
                "}"
            )
        )


def _open_login(page: Page) -> None:
    if _has_password_input(page):
        return
    link = _first_visible(page.get_by_role("link", name=_LOGIN_LINK_RE))
    if link is None:
        link = _first_visible(
            page.locator(
                "a[href*='login' i], a[href*='dang-nhap' i], "
                "[aria-label*='tài khoản' i], .account-icon"
            )
        )
    if link is not None:
        try:
            link.click()
            page.wait_for_load_state("domcontentloaded")
        except Exception:
            pass
    if _has_password_input(page):
        return
    for path in ("/login", "/dang-nhap"):
        try:
            page.goto(_url(path), wait_until="domcontentloaded")
        except Exception:
            continue
        if _has_password_input(page):
            return
    raise RuntimeError("login form not found")


def _find_user_field(page: Page):
    for locator in (
        page.get_by_label(_USER_FIELD_RE),
        page.get_by_placeholder(_USER_FIELD_RE),
    ):
        field = _first_visible(locator)
        if field is not None:
            return field
    form = page.locator("form:has(input[type=password])")
    try:
        if form.count():
            return _first_visible(
                form.first.locator("input[type=text], input[type=email], input:not([type])")
            )
    except Exception:
        pass
    return None


def _captcha_scopes(page: Page) -> list:
    scopes = [page]
    for frame in page.frames:
        if frame == page.main_frame:
            continue
        if re.search(r"recaptcha|captcha", frame.url or "", re.I):
            scopes.append(frame)
    try:
        scopes.append(page.frame_locator(_CAPTCHA_FRAME))
    except Exception:
        pass
    return scopes


def _check_captcha(scope, allow_bare: bool) -> bool:
    locators = [
        scope.get_by_role("checkbox", name=_CAPTCHA_TEXT_RE),
        scope.locator("#recaptcha-anchor"),
    ]
    if allow_bare:
        locators.append(scope.get_by_role("checkbox"))
    for locator in locators:
        candidate = _first_visible(locator)
        if candidate is None:
            continue
        try:
            candidate.check(timeout=2000)
        except Exception:
            try:
                candidate.click(force=True, timeout=2000)
            except Exception:
                continue
        return True
    return False


def _captcha_checked(scopes) -> bool:
    for scope in scopes:
        try:
            if scope.locator(".recaptcha-checkbox-checked").count():
                return True
            anchor = scope.locator("#recaptcha-anchor")
            if anchor.count() and anchor.first.get_attribute("aria-checked") == "true":
                return True
        except Exception:
            continue
    return False


def _captcha_challenge_visible(page: Page) -> bool:
    try:
        challenge = page.frame_locator("iframe[title*='challenge' i], iframe[src*='bframe']")
        return (
            challenge.get_by_text(re.compile(r"Select all|Chọn tất cả|Xác minh", re.I)).count()
            > 0
        )
    except Exception:
        return False


def _solve_captcha(page: Page) -> None:
    scopes = _captcha_scopes(page)
    clicked = any(_check_captcha(scope, allow_bare=scope is not page) for scope in scopes)
    if not clicked:
        try:
            page.wait_for_selector(_CAPTCHA_FRAME, timeout=3000)
        except Exception:
            return
        scopes = _captcha_scopes(page)
        clicked = any(_check_captcha(scope, allow_bare=scope is not page) for scope in scopes)
        if not clicked:
            return

    deadline = time.monotonic() + 5
    while time.monotonic() < deadline:
        if _captcha_checked(scopes):
            return
        page.wait_for_timeout(250)

    if not _captcha_challenge_visible(page):
        return
    if _headed():
        logger.warning("captcha challenge requires manual solving; waiting 60s (headed mode)")
        page.wait_for_timeout(60_000)
    else:
        logger.warning(
            "captcha challenge requires manual solving; run with PLAYWRIGHT_HEADLESS=false"
        )


def _submit(page: Page) -> None:
    button = _first_visible(page.get_by_role("button", name=_LOGIN_BUTTON_RE))
    if button is None:
        button = _first_visible(page.locator("button[type=submit], input[type=submit]"))
    if button is not None:
        button.click()
        return
    field = _first_visible(page.locator(_PASSWORD))
    if field is not None:
        field.press("Enter")
        return
    raise RuntimeError("login submit control not found")


def _logged_in(page: Page, start_url: str) -> bool:
    if _has_password_input(page):
        return False
    if page.url != start_url:
        return True
    if _first_visible(page.get_by_role("link", name=_LOGGED_IN_RE)) is not None:
        return True
    return (
        _first_visible(page.locator("a[href*='logout' i], [aria-label*='tài khoản' i]"))
        is not None
    )


def _wait_login_success(page: Page, start_url: str, timeout_s: int = 15) -> bool:
    deadline = time.monotonic() + timeout_s
    while time.monotonic() < deadline:
        if _logged_in(page, start_url):
            return True
        page.wait_for_timeout(250)
    return _logged_in(page, start_url)


def _login(page: Page, role: str) -> None:
    user, password = _credentials(role)
    _open_login(page)

    password_field = _first_visible(page.locator(_PASSWORD))
    if password_field is None:
        raise RuntimeError("password field not found on login page")
    user_field = _find_user_field(page)
    if user_field is not None:
        user_field.fill(user)
    password_field.fill(password)

    _solve_captcha(page)

    start_url = page.url
    _submit(page)
    if not _wait_login_success(page, start_url):
        raise RuntimeError(f"login failed for role '{role}'")


def _verify_admin(page: Page) -> None:
    page.goto(_url("/admin/dashboard"), wait_until="domcontentloaded")
    if detect_login_wall(page) is not None:
        raise RuntimeError("admin login did not grant /admin/ access")


def ensure_logged_in(context: BrowserContext, role: str) -> None:
    if role not in AUTH["credentials"]:
        raise ValueError(f"unknown role: {role}")

    page = context.pages[0] if context.pages else context.new_page()
    state_path = _state_path(role)

    if state_path.is_file():
        try:
            data = json.loads(state_path.read_text(encoding="utf-8"))
        except Exception as exc:
            logger.warning("ignoring unreadable saved session %s: %s", state_path, exc)
        else:
            _apply_storage_state(context, data)
            page.goto(_url(_PROTECTED_PATHS[role]), wait_until="domcontentloaded")
            if detect_login_wall(page) is None:
                return
            logger.info("saved %s session expired; logging in again", role)

    _login(page, role)
    if role == "admin":
        _verify_admin(page)

    state_path.parent.mkdir(parents=True, exist_ok=True)
    context.storage_state(path=str(state_path))
