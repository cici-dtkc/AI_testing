from typing import Literal
from pydantic import BaseModel, ConfigDict, Field, field_validator

TechniqueName = Literal[
    'EQUIVALENCE_PARTITIONING',
    'BOUNDARY_VALUE_ANALYSIS',
    'DECISION_TABLE',
    'STATE_TRANSITION',
    'USE_CASE',
    'BASIS_PATH',
    'STATEMENT_COVERAGE',
    'BRANCH_COVERAGE',
    'CONDITION_COVERAGE',
    'DATA_FLOW',
    'NON_FUNCTIONAL_METRIC',
]
TechniqueFamily = Literal['BLACK_BOX', 'WHITE_BOX']
TechniqueRole = Literal['PRIMARY', 'SUPPORTING']

class StrictModel(BaseModel):
    model_config = ConfigDict(extra='forbid')

class TestCase(StrictModel):
    test_case_id: str = Field(min_length=1)

    @field_validator('module', 'technique_variant', 'flow', mode='before')
    @classmethod
    def _none_as_empty(cls, value):
        """Plan có thể trả null cho variant; schema Gemini chỉ nhận string."""
        return '' if value is None else value

    # Chuỗi rỗng thay vì null để response_schema của Gemini không phải dùng anyOf.
    module: str = ''
    title: str = Field(min_length=1)
    type: Literal['POSITIVE', 'NEGATIVE', 'BOUNDARY', 'AUTHORIZATION', 'STATE', 'NON_FUNCTIONAL']
    priority: Literal['Cao', 'Thông thường', 'Thấp'] = 'Cao'
    technique: TechniqueName
    technique_variant: str = ''
    # Luồng theo mẫu bài 8, bắt buộc cho mọi case: Chính / E1, E2… / Ẩn.
    flow: str = ''
    coverage_items: list[str] = Field(min_length=1)
    preconditions: list[str]
    given: str
    steps: list[str] = Field(min_length=1)
    expected_result: str = Field(min_length=1)
    # Test case hộp trắng không truy vết được về scenario/rule của SRS.
    trace_scenario_ids: list[str] = Field(default_factory=list)
    trace_rule_ids: list[str] = Field(default_factory=list)

class TestSuite(StrictModel):
    summary: str
    test_cases: list[TestCase]


class NumericRange(StrictModel):
    """Một ràng buộc số/độ dài tìm thấy trong rule, dùng cho BVA."""

    label: str
    min: int | None = None
    max: int | None = None
    unit: str = ''
    rule_ids: list[str] = Field(default_factory=list)
    boundary_points: list[int] = Field(default_factory=list)


class TechniqueChoice(StrictModel):
    technique: TechniqueName
    variant: str | None = None
    family: TechniqueFamily
    role: TechniqueRole
    rationale: str
    obligations: list[str] = Field(default_factory=list)
    estimated_cases: int = Field(ge=0, default=0)


class ModulePlan(StrictModel):
    module: str
    rule_ids: list[str] = Field(default_factory=list)
    scenario_ids: list[str] = Field(default_factory=list)
    techniques: list[TechniqueChoice] = Field(min_length=1)
    numeric_ranges: list[NumericRange] = Field(default_factory=list)
    story: str
    warnings: list[str] = Field(default_factory=list)


class CoverageTarget(StrictModel):
    level: str
    items: int = Field(ge=0)
    note: str = ''


class WhiteBoxPlan(StrictModel):
    module: str
    source: str
    statements: int
    decisions: int
    conditions: int
    cyclomatic: int
    basis_paths: int
    coverage_targets: list[CoverageTarget] = Field(default_factory=list)
    data_flow_pairs: int
    techniques: list[TechniqueChoice] = Field(min_length=1)
    warnings: list[str] = Field(default_factory=list)


class TechniquePlan(StrictModel):
    summary: str
    modules: list[ModulePlan]
    white_box: list[WhiteBoxPlan] = Field(default_factory=list)