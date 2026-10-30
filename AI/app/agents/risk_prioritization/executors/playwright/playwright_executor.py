from datetime import datetime
from pathlib import Path
from tempfile import TemporaryDirectory
import re

from playwright.sync_api import sync_playwright

from ...evidence_store.api import store_evidence
from ...orchestrator.models import ExecutionResult
from .config import BROWSER_TYPE, HEADLESS, VIEWPORT, TIMEOUT_NAVIGATION
from .step_parser import parse_step
from .assertions import verify_expected
from .evidence import capture_screenshot, start_tracing, stop_tracing, capture_video


def _store(run_id: str, tc_id: str, attempt: int, artifact_type: str, path: str | None) -> str | None:
    if not path or not Path(path).is_file():
        return None
    return store_evidence(run_id, tc_id, attempt, artifact_type, path)


class PlaywrightExecutor:
    def execute_test_case(self, tc: dict, run_id: str, attempt: int) -> ExecutionResult:
        tc_id = tc.get("test_case_id", tc.get("id", "unknown"))
        start_time = datetime.now()
        evidence_uris = []
        state = "PASSED"
        error_message = None

        try:
            with TemporaryDirectory() as tmp:
                evidence_base = Path(tmp)
                with sync_playwright() as p:
                    browser = p[BROWSER_TYPE].launch(headless=HEADLESS)
                    context = browser.new_context(viewport=VIEWPORT)
                    page = context.new_page()
                    page.set_default_timeout(TIMEOUT_NAVIGATION)

                    trace_path = evidence_base / "trace.zip"
                    start_tracing(context, str(trace_path))

                    try:
                        # Get steps from either 'steps' or 'test_steps' field
                        test_steps = tc.get("steps", tc.get("test_steps", []))
                        if isinstance(test_steps, str):
                            test_steps = [s.strip() for s in test_steps.split("\n") if s.strip()]

                        # Prepend navigation step if not present
                        if test_steps and not any(re.search(r'(mở|truy cập|goto)', test_steps[0], re.IGNORECASE) for _ in [None]):
                            # Infer page from module or use home page
                            module = tc.get("module", "")
                            if "TimKiem" in module or "tìm kiếm" in str(tc.get("title", "")).lower():
                                test_steps.insert(0, "Mở /")
                            elif "DangNhap" in module or "đăng nhập" in str(tc.get("title", "")).lower():
                                test_steps.insert(0, "Mở /login")
                            elif "DangKy" in module or "đăng ký" in str(tc.get("title", "")).lower():
                                test_steps.insert(0, "Mở /register")
                            elif "GioHang" in module or "giỏ hàng" in str(tc.get("title", "")).lower():
                                test_steps.insert(0, "Mở /cart")
                            else:
                                # Default to home page
                                test_steps.insert(0, "Mở /")

                        for step in test_steps:
                            parse_step(step, page)

                        expected = tc.get("expected_result", "")
                        passed, reason = verify_expected(expected, page)

                        if not passed:
                            state = "FAILED"
                            error_message = reason
                            screenshot_path = capture_screenshot(page, str(evidence_base / "failure.png"))
                            evidence_uris.append(_store(run_id, tc_id, attempt, "screenshot", screenshot_path))

                        stop_tracing(context, str(trace_path))
                        evidence_uris.append(_store(run_id, tc_id, attempt, "trace", str(trace_path)))

                        video_path = capture_video(page)
                        evidence_uris.append(_store(run_id, tc_id, attempt, "video", video_path))

                    except Exception as e:
                        state = "FAILED"
                        error_message = str(e)
                        try:
                            screenshot_path = capture_screenshot(page, str(evidence_base / "error.png"))
                            evidence_uris.append(_store(run_id, tc_id, attempt, "screenshot", screenshot_path))
                        except Exception:
                            pass
                        stop_tracing(context, str(trace_path))
                        evidence_uris.append(_store(run_id, tc_id, attempt, "trace", str(trace_path)))

                    finally:
                        browser.close()

        except Exception as e:
            state = "ERROR"
            error_message = f"Browser launch failed: {str(e)}"

        end_time = datetime.now()
        duration_ms = int((end_time - start_time).total_seconds() * 1000)

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
