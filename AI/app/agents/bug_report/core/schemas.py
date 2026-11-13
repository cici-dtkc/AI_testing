from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict, List, Literal, Optional, Union
from pydantic import BaseModel, Field

# 1. HỢP ĐỒNG ĐẦU VÀO TỪ FAILURE ANALYSIS AGENT (SCHEMA V1.0)

PrimaryCategory = Literal[
    "PRODUCT",
    "PRODUCT_BUG",
    "TEST",
    "TEST_BUG",
    "DATA",
    "ENVIRONMENT",
    "UNKNOWN"
]

RoutingAction = Literal[
    "BUG_REPORT_DRAFT",
    "TEST_MAINTENANCE",
    "INFRASTRUCTURE_TRIAGE",
    "HUMAN_TRIAGE"
]


class FAAObservation(BaseModel):
    """Sự thật quan sát khách quan từ Failure Analysis Agent"""
    observation_id: str
    fact: str
    evidence_refs: List[Union[str, Dict[str, Any]]] = Field(default_factory=list)


class FAAHypothesisItem(BaseModel):
    """Giả thuyết kỹ thuật căn nguyên từ Failure Analysis Agent & LLM Provider"""
    hypothesis_id: str
    explanation: str
    supporting_evidence_refs: List[str] = Field(default_factory=list)
    contradicting_evidence_refs: List[str] = Field(default_factory=list)
    assessment: Optional[str] = "PLAUSIBLE"


class FAANextAction(BaseModel):
    """Đề xuất hành động khắc phục trực tiếp từ Failure Analysis Agent"""
    action: str
    owner_role: Optional[str] = "DEVELOPER"
    requires_approval: bool = True
    priority: Optional[str] = "P2"


class FAAEvidenceItem(BaseModel):
    """Tệp bằng chứng (Screenshot, Playwright Trace) đính kèm mã băm SHA-256"""
    evidence_id: str
    kind: str
    artifact_ref: str
    sha256: str
    captured_at: Optional[str] = None
    run_id: Optional[str] = None
    attempt_id: Optional[str] = None
    build_id: Optional[str] = None
    redaction_status: Optional[str] = None


# Để tương thích ngược với unit tests và code cũ
class EvidenceRef(BaseModel):
    evidence_id: str
    locator: Optional[str] = None


class FAAConfidence(BaseModel):
    score: Optional[float] = None
    level: Optional[str] = "MODERATE"
    basis: Optional[str] = None


class FAAObservedFact(BaseModel):
    fact_id: Optional[str] = None
    statement: str
    evidence_refs: List[Any] = Field(default_factory=list)


class FAAHypothesis(BaseModel):
    hypothesis_id: str
    label: str = "PRODUCT"
    statement: str
    supporting_evidence_ids: List[str] = Field(default_factory=list)
    counter_evidence_ids: List[str] = Field(default_factory=list)
    status: str = "SUPPORTED"


class FAASeverity(BaseModel):
    score: Optional[str] = None
    rationale: Optional[str] = None


class FAARecommendedAction(BaseModel):
    action: str
    owner_role: Optional[str] = "Developer"
    priority: Optional[str] = "P2"
    requires_approval: bool = True


class FailureAnalysisOutput(BaseModel):
    """
    Hợp đồng dữ liệu đầu ra chính thức từ Failure Analysis Agent (Schema v1.0).
    Khớp từng trường với dictionary trả về bởi analyze_input() trong failure_analyzer/core/analyzer.py.
    """
    analysis_id: str
    schema_version: str = "1.0"
    project_id: str = "comic-ecommerce"
    run_id: str
    attempt_id: str = "attempt-1"
    test_case_id: str
    execution_status: str = "FAILED"
    failure_present: bool = True
    analysis_status: str = "COMPLETED"
    primary_category: Optional[str] = "PRODUCT"
    subtype: Optional[str] = None
    summary: str
    observations: List[FAAObservation] = Field(default_factory=list)
    hypotheses: List[FAAHypothesisItem] = Field(default_factory=list)
    confidence_score: Optional[float] = None
    confidence_basis: Optional[str] = None
    confidence: Optional[Union[FAAConfidence, float]] = None
    observedFacts: List[FAAObservedFact] = Field(default_factory=list)
    severity: Optional[FAASeverity] = None
    missing_evidence: List[str] = Field(default_factory=list)
    contributing_factors: List[Dict[str, Any]] = Field(default_factory=list)
    next_actions: List[FAANextAction] = Field(default_factory=list)
    recommendedActions: List[Any] = Field(default_factory=list)
    routing: str = "BUG_REPORT_DRAFT"
    severity_suggestion: Optional[str] = None
    review_status: str = "REVIEW_REQUIRED"
    provenance: Optional[Dict[str, Any]] = None
    evidence_manifest: List[FAAEvidenceItem] = Field(default_factory=list)
    gemini_assessment: Optional[Dict[str, Any]] = None


# 2. HỢP ĐỒNG ĐẦU RA CỦA BUG REPORT AGENT (CHUẨN HÓA CHO DASHBOARD & SHARED STATE)

SeverityLevel = Literal["CRITICAL", "HIGH", "MEDIUM", "LOW"]
ReportConfidence = Literal["High", "Medium", "Low"]
DefectType = Literal["PRODUCT_BUG", "TEST_DEFECT", "TEST_MAINTENANCE"]


class SeverityFactors(BaseModel):
    """
    Phân rã 4 yếu tố xác định mức độ nghiêm trọng: Score = I + M + W + S
    Tuân thủ Guardrail G1: Tính toán bằng code xác định, không dùng LLM chấm điểm.
    """
    i: int = Field(..., ge=0, le=4, description="Mức độ ảnh hưởng chức năng (0-4)")
    m: int = Field(..., ge=0, le=3, description="Tổn thất tiền tệ / dữ liệu giỏ hàng (0-3)")
    w: int = Field(..., ge=0, le=2, description="Khả năng né lỗi / Workaround (0-2)")
    s: int = Field(..., ge=0, le=1, description="Phạm vi người dùng / sản phẩm bị ảnh hưởng (0-1)")


class StepItem(BaseModel):
    step: int
    action: str
    traceRef: Optional[str] = None
    status: Optional[str] = "PASSED"
    latency: Optional[str] = None


class ObservedFact(BaseModel):
    fact: str
    evidenceRef: Optional[str] = None


class SuspectedCause(BaseModel):
    cause: str
    hypothesisId: Optional[str] = None


class EvidenceItem(BaseModel):
    id: str
    type: Literal["SCREENSHOT", "API_RESPONSE", "LOG", "TRACE"]
    path: str
    sha256: Optional[str] = None


class Traceability(BaseModel):
    testId: str
    requirementScenarioId: Optional[str] = None
    businessRuleId: Optional[str] = None
    failureId: Optional[str] = None


class Deduplication(BaseModel):
    fingerprint: str
    isDuplicate: bool = False
    duplicateOf: Optional[str] = None


class EnvironmentInfo(BaseModel):
    browser: Optional[str] = "Chromium 128 (Playwright)"
    os: Optional[str] = "Windows 11"
    build: Optional[str] = "v1.4.2-staging"
    baseUrl: Optional[str] = "http://localhost:8080/TTLTW_Nhom6"


class BugReport(BaseModel):
    """Mô hình dữ liệu đầu ra chuẩn hoá của Bug Report Agent"""
    bugId: str = Field(..., description="Mã định danh bug, ví dụ: BUG-20261111-TimKiem-TC-04")
    createdAt: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    createdBy: str = "BugReportAgent-v1.0.0"
    status: Literal["New", "Approved", "Rejected"] = "New"
    reviewRequired: bool = True
    gatePassed: bool = False
    defectType: DefectType = "PRODUCT_BUG"
    failureCategory: Optional[str] = None
    failureSubtype: Optional[str] = None
    routing: Optional[str] = None
    title: str = Field(..., description="Dạng: [Module] Hành vi sai khi điều kiện")
    module: str
    feature: str
    targetUrl: Optional[str] = None
    environment: EnvironmentInfo = Field(default_factory=EnvironmentInfo)
    preconditions: str
    testData: Optional[str] = None
    steps: List[StepItem]
    expected: str
    actual: str
    reproducibility: str = "Always (1/1)"
    severity: SeverityLevel
    severityScore: int
    factors: SeverityFactors
    faaSeveritySuggestion: Optional[str] = None
    confidence: ReportConfidence = "High"
    confidenceScore: Optional[float] = None
    observedFacts: List[ObservedFact] = Field(default_factory=list)
    suspectedCauses: List[SuspectedCause] = Field(default_factory=list)
    evidence: List[EvidenceItem] = Field(default_factory=list)
    recommendedActions: List[str] = Field(default_factory=list)
    missingEvidence: List[str] = Field(default_factory=list)
    traceability: Traceability
    deduplication: Deduplication
