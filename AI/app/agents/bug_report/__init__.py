"""
Bug Report Agent Package.
Standardized IEEE 829 defect reporting engine with deterministic severity scoring.
"""

from .core.schemas import *
from .core.severity import *
from .core.dedup import *
from .core.extractor import *
from .agent import (
    BugReportAgent,
    create_bug_report_from_failure,
    evaluate_gate,
    generate_professional_title,
)

__all__ = [
    "BugReportAgent",
    "create_bug_report_from_failure",
    "evaluate_gate",
    "generate_professional_title",
    "DataExtractor",
    "calculate_severity",
    "calculate_confidence",
    "check_duplicate",
    "generate_fingerprint",
    "BugReport",
]
