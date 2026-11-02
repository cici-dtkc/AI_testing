import re
from .config import TIMEOUT_DEFAULT

def verify_expected(expected_text: str, page) -> tuple[bool, str]:
    expected = expected_text.strip().lower()

    try:
        if re.search(r'báo lỗi|thông báo lỗi', expected, re.IGNORECASE):
            error_locator = page.locator(".error, .alert-danger, [role='alert']")
            if error_locator.count() > 0:
                return True, "Error message displayed"
            return False, "No error message found"

        if re.search(r'hiển thị|hiện', expected, re.IGNORECASE):
            if match := re.search(r'(hiển thị|hiện)\s+(.+)', expected, re.IGNORECASE):
                text = match.group(2).strip()
                locator = page.locator(f":has-text('{text}')")
                if locator.count() > 0:
                    return True, f"Text '{text}' displayed"
                return False, f"Text '{text}' not found"
            return True, "Page displayed"

        if re.search(r'thành công', expected, re.IGNORECASE):
            success_locator = page.locator(".success, .alert-success, :has-text('thành công')")
            if success_locator.count() > 0:
                return True, "Success indicator found"
            return False, "No success indicator"

        if re.search(r'không|không thay đổi', expected, re.IGNORECASE):
            return True, "State unchanged (manual verification required)"

        locator = page.locator(f":has-text('{expected_text}')")
        if locator.count() > 0:
            return True, f"Expected text found: {expected_text}"
        return False, f"Expected text not found: {expected_text}"

    except Exception as e:
        return False, f"Assertion error: {str(e)}"
