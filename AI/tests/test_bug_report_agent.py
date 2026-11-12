import unittest
import sys
from pathlib import Path

# Ensure AI directory is on sys.path
ai_root = Path(__file__).resolve().parent.parent
if str(ai_root) not in sys.path:
    sys.path.insert(0, str(ai_root))

from app.agents.bug_report.severity import (
    calculate_severity,
    calculate_confidence,
    MODULE_CRITICALITY_MAP,
)
from app.agents.bug_report.dedup import (
    generate_fingerprint,
    normalize_error_message,
)
from app.agents.bug_report.agent import evaluate_gate
from app.agents.bug_report.schemas import (
    FailureAnalysisOutput,
    FAAConfidence,
    BugReport,
    SeverityLevel,
)


class TestBugReportSeverity(unittest.TestCase):
    def test_severity_critical(self):
        # Module GiamGia (Tier 3 -> M=2) + Financial Impact (I=3) + No Workaround (W=2) + Always (S=1)
        # Score = 3 + 2 + 2 + 1 = 8 -> CRITICAL
        sev, score, factors = calculate_severity(
            module="GiamGia",
            actual_text="Internal Server Error: 500 payment discount calculation failed",
            expected_text="Áp dụng mã giảm giá 10% thành công",
            error_details="Tài chính bị ảnh hưởng, không có phương án thay thế",
        )
        self.assertEqual(sev, "CRITICAL")
        self.assertGreaterEqual(score, 7)
        self.assertEqual(factors.m, 2)

    def test_severity_high(self):
        # Module Wishlist (Tier 2 -> M=1) + Severe impact (I=3) + Workaround hard (W=1)
        # Score = 3 + 1 + 1 + 0 = 5 -> HIGH
        sev, score, factors = calculate_severity(
            module="Wishlist",
            actual_text="Mất toàn bộ dữ liệu danh sách truyện yêu thích, crash trang cá nhân",
            expected_text="Hiển thị wishlist",
            error_details="Target element not found",
        )
        self.assertEqual(sev, "HIGH")
        self.assertIn(score, [5, 6])

    def test_severity_medium(self):
        # Module TimKiem (Tier 2 -> M=1) + no validation feedback (I=2, W=1, S=0) -> 4 (MEDIUM)
        sev, score, factors = calculate_severity(
            module="TimKiem",
            actual_text="no validation feedback in UI (possible app defect)",
            expected_text="Hiển thị thông báo yêu cầu nhập từ khóa",
            error_details="UI feedback missing",
        )
        self.assertEqual(sev, "MEDIUM")
        self.assertEqual(score, 4)

    def test_security_override_guardrail_g1(self):
        # Guardrail G1: Any security vulnerability must be escalated to CRITICAL with score 10
        sev, score, _ = calculate_severity(
            module="TimKiem",
            actual_text="Display search page",
            expected_text="Display search page",
            is_security_concern=True,
        )
        self.assertEqual(sev, "CRITICAL")
        self.assertEqual(score, 10)

    def test_module_criticality_map(self):
        self.assertEqual(MODULE_CRITICALITY_MAP.get("ThanhToan"), 4)
        self.assertEqual(MODULE_CRITICALITY_MAP.get("GiamGia"), 3)
        self.assertEqual(MODULE_CRITICALITY_MAP.get("FlashSale"), 3)
        self.assertEqual(MODULE_CRITICALITY_MAP.get("TimKiem"), 2)
        self.assertEqual(MODULE_CRITICALITY_MAP.get("Wishlist"), 2)

    def test_calculate_confidence(self):
        # High confidence: has evidence, Always (1/1), FAA >= 0.60
        c_high, req_rev1 = calculate_confidence(
            has_screenshot=True,
            has_trace=True,
            reproducibility="Always (1/1)",
            faa_confidence_score=0.90,
        )
        self.assertEqual(c_high, "High")
        self.assertTrue(req_rev1)

        # Medium confidence: flaky reproducibility
        c_med, _ = calculate_confidence(
            has_screenshot=True,
            has_trace=False,
            reproducibility="Sometimes (1/3)",
            faa_confidence_score=0.75,
        )
        self.assertEqual(c_med, "Medium")

        # Low confidence: missing all evidence
        c_low, _ = calculate_confidence(
            has_screenshot=False,
            has_trace=False,
            reproducibility="Always (1/1)",
            faa_confidence_score=0.80,
        )
        self.assertEqual(c_low, "Low")


class TestBugReportDedup(unittest.TestCase):
    def test_normalize_error_message(self):
        msg = "Error 500 at 2026-11-06T10:12:36 with ID a1b2c3d4-e5f6-7890-abcd-ef1234567890 in 185ms"
        norm = normalize_error_message(msg)
        self.assertNotIn("a1b2c3d4-e5f6-7890-abcd-ef1234567890", norm)
        self.assertNotIn("2026-11-06T10:12:36", norm)
        self.assertNotIn("500", norm)

    def test_fingerprint_consistency(self):
        fp1 = generate_fingerprint("GiamGia", "/api/coupon/apply", "ui_assertion", "200", "Code expired in 2026")
        fp2 = generate_fingerprint("GiamGia", "/api/coupon/apply", "ui_assertion", "200", "Code expired in 2027")
        self.assertTrue(fp1.startswith("fp_"))
        self.assertEqual(len(fp1), 11)  # 'fp_' + 8 hex chars
        # Due to stripping dynamic numbers, both fingerprints match!
        self.assertEqual(fp1, fp2)

    def test_fingerprint_uniqueness(self):
        fp1 = generate_fingerprint("GiamGia", "/api/coupon/apply", "ui_assertion", "200", "Code expired")
        fp2 = generate_fingerprint("TimKiem", "/api/search", "ui_assertion", "200", "No results found")
        self.assertNotEqual(fp1, fp2)


class TestBugReportGate(unittest.TestCase):
    def test_gate_allows_product_bug(self):
        fa = FailureAnalysisOutput(
            analysis_id="FA-TEST-01",
            run_id="run-1",
            test_case_id="TC-01",
            primary_category="PRODUCT_BUG",
            summary="Defect in checkout",
            confidence=FAAConfidence(score=0.85, level="HIGH"),
        )
        self.assertTrue(evaluate_gate(fa))

    def test_gate_blocks_non_product_bugs(self):
        # TEST_BUG must be filtered out (G0)
        fa_test = FailureAnalysisOutput(
            analysis_id="FA-TEST-02",
            run_id="run-1",
            test_case_id="TC-02",
            primary_category="TEST_BUG",
            summary="Assertion selector wrong",
            confidence=FAAConfidence(score=0.90, level="HIGH"),
        )
        self.assertFalse(evaluate_gate(fa_test))

        # ENVIRONMENT_FAILURE must be filtered out (G0)
        fa_env = FailureAnalysisOutput(
            analysis_id="FA-TEST-03",
            run_id="run-1",
            test_case_id="TC-03",
            primary_category="ENVIRONMENT_FAILURE",
            summary="Docker container unavailable",
            confidence=FAAConfidence(score=0.95, level="HIGH"),
        )
        self.assertFalse(evaluate_gate(fa_env))

    def test_gate_blocks_low_confidence(self):
        # Confidence < 0.60 must be rejected
        fa_low = FailureAnalysisOutput(
            analysis_id="FA-TEST-04",
            run_id="run-1",
            test_case_id="TC-04",
            primary_category="PRODUCT_BUG",
            summary="Suspicious behavior",
            confidence=FAAConfidence(score=0.55, level="LOW"),
        )
        self.assertFalse(evaluate_gate(fa_low))


if __name__ == "__main__":
    unittest.main()
