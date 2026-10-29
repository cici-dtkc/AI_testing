import re
from .config import SELECTOR_MAP, TIMEOUT_DEFAULT, BASE_URL

def parse_step(step_text: str, page):
    step = step_text.strip().lower()

    if match := re.search(r'nhập\s+(.+?)\s*[=:]\s*(.+)', step, re.IGNORECASE):
        field, value = match.groups()
        selector = _get_selector(field.strip())
        page.fill(selector, value.strip(), timeout=TIMEOUT_DEFAULT)
        return

    if match := re.search(r'nhấn\s+(.+)', step, re.IGNORECASE):
        button = match.group(1).strip()
        selector = _get_selector(button)
        page.click(selector, timeout=TIMEOUT_DEFAULT)
        return

    if match := re.search(r'chọn\s+(.+)', step, re.IGNORECASE):
        option = match.group(1).strip()
        page.select_option("select", option, timeout=TIMEOUT_DEFAULT)
        return

    if match := re.search(r'(mở|truy cập)\s+(.+)', step, re.IGNORECASE):
        url = match.group(2).strip()
        if not url.startswith("http"):
            url = f"{BASE_URL}{url if url.startswith('/') else '/' + url}"
        page.goto(url)
        return

    if match := re.search(r'chờ\s+(\d+)\s*giây', step, re.IGNORECASE):
        seconds = int(match.group(1))
        page.wait_for_timeout(seconds * 1000)
        return

    raise ValueError(f"Unknown step pattern: {step_text}")

def _get_selector(field_name: str) -> str:
    field_lower = field_name.lower().strip()
    if field_lower in SELECTOR_MAP:
        return SELECTOR_MAP[field_lower]
    return f"[data-testid='{field_lower}'], #{field_lower}, [name='{field_lower}'], :has-text('{field_name}')"
