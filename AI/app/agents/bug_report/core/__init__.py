"""
Core modules for Bug Report Agent:
- schemas: Pydantic models for inputs and standardized IEEE 829 bug reports
- severity: Deterministic I+M+W+S severity scoring engine
- dedup: Hashing-based deduplication fingerprint engine
- extractor: Multi-source data extractor (FAA, execution_results, test_cases_report)
"""

from .schemas import (
    BugReport,
    Deduplication,
    EnvironmentInfo,
    EvidenceItem,
    FAAEvidenceItem,
    FAAHypothesisItem,
    FAANextAction,
    FAAObservation,
    FailureAnalysisOutput,
    ObservedFact,
    ReportConfidence,
    SeverityFactors,
    SeverityLevel,
    StepItem,
    SuspectedCause,
    Traceability,
)
from .severity import (
    calculate_confidence,
    calculate_severity,
    MODULE_CRITICALITY_MAP,
)
from .dedup import (
    check_duplicate,
    generate_fingerprint,
    normalize_error_message,
)
from .extractor import DataExtractor

__all__ = [
    "BugReport",
    "Deduplication",
    "EnvironmentInfo",
    "EvidenceItem",
    "FAAEvidenceItem",
    "FAAHypothesisItem",
    "FAANextAction",
    "FAAObservation",
    "FailureAnalysisOutput",
    "ObservedFact",
    "ReportConfidence",
    "SeverityFactors",
    "SeverityLevel",
    "StepItem",
    "SuspectedCause",
    "Traceability",
    "calculate_confidence",
    "calculate_severity",
    "MODULE_CRITICALITY_MAP",
    "check_duplicate",
    "generate_fingerprint",
    "normalize_error_message",
    "DataExtractor",
]
