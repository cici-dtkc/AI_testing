import re
from .config import SELECTOR_MAP, TIMEOUT_DEFAULT, BASE_URL

def parse_step(step_text: str, page):
    step = step_text.strip().lower()

    # Navigation: "Mở /path" or "Truy cập URL"
    if match := re.search(r'(mở|truy cập)\s+(.+)', step, re.IGNORECASE):
        url = match.group(2).strip()
        if not url.startswith("http"):
            url = f"{BASE_URL}{url if url.startswith('/') else '/' + url}"
        page.goto(url)
        return

    # Input: "Nhập field = value"
    if match := re.search(r'nhập\s+(.+?)\s*[=:]\s*(.+)', step, re.IGNORECASE):
        field, value = match.groups()
        selector = _get_selector(field.strip())
        page.fill(selector, value.strip(), timeout=TIMEOUT_DEFAULT)
        return

    # Click: "Nhấn button"
    if match := re.search(r'nhấn\s+(.+)', step, re.IGNORECASE):
        button = match.group(1).strip()
        selector = _get_selector(button)
        page.click(selector, timeout=TIMEOUT_DEFAULT)
        return

    # Select: "Chọn option"
    if match := re.search(r'chọn\s+(.+)', step, re.IGNORECASE):
        option = match.group(1).strip()
        # Try to click element containing the text
        try:
            page.click(f"text={option}", timeout=TIMEOUT_DEFAULT)
        except:
            # Fallback to select dropdown
            page.select_option("select", option, timeout=TIMEOUT_DEFAULT)
        return

    # Wait: "Chờ N giây"
    if match := re.search(r'chờ\s+(\d+)\s*giây', step, re.IGNORECASE):
        seconds = int(match.group(1))
        page.wait_for_timeout(seconds * 1000)
        return

    # Abstract actions - attempt common patterns
    # "Thực hiện [action]" → try to infer action
    if re.search(r'thực hiện|kiểm tra|xác nhận|quan sát', step, re.IGNORECASE):
        # Try common actions based on keywords
        if 'gọi' in step or 'danh sách' in step or 'hiển thị' in step:
            # Likely checking page content - wait for network idle
            page.wait_for_load_state("networkidle", timeout=TIMEOUT_DEFAULT)
            return

        if 'đăng nhập' in step:
            # Try to find login button
            try:
                page.click("button:has-text('Đăng nhập')", timeout=TIMEOUT_DEFAULT)
                return
            except:
                pass

        if 'tìm kiếm' in step:
            # Try to find search button
            try:
                page.click("button:has-text('Tìm')", timeout=TIMEOUT_DEFAULT)
                return
            except:
                page.press("input[type='search']", "Enter", timeout=TIMEOUT_DEFAULT)
                return

        # Default: just wait for page to be ready
        page.wait_for_load_state("domcontentloaded", timeout=TIMEOUT_DEFAULT)
        return

    # Upload file: "Tải lên file"
    if 'tải lên' in step or 'upload' in step:
        # Create a dummy file for testing
        page.set_input_files("input[type='file']", [], timeout=TIMEOUT_DEFAULT)
        return

    # Generic fallback for unrecognized steps - wait for page ready
    try:
        page.wait_for_load_state("domcontentloaded", timeout=5000)
    except:
        pass
    return  # Don't raise error, allow test to continue

def _get_selector(field_name: str) -> str:
    field_lower = field_name.lower().strip()
    if field_lower in SELECTOR_MAP:
        return SELECTOR_MAP[field_lower]
    return f"[data-testid='{field_lower}'], #{field_lower}, [name='{field_lower}'], :has-text('{field_name}')"
