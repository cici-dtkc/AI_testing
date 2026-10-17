from typing import Literal
from pydantic import BaseModel, ConfigDict, Field

class StrictModel(BaseModel):
    model_config = ConfigDict(extra='forbid')

class TestCase(StrictModel):
    test_case_id: str = Field(min_length=1)
    title: str = Field(min_length=1)
    type: Literal['POSITIVE', 'NEGATIVE', 'BOUNDARY', 'AUTHORIZATION', 'STATE', 'NON_FUNCTIONAL']
    priority: Literal['Cao', 'Thông thường', 'Thấp'] = 'Cao'
    preconditions: list[str]
    given: str
    steps: list[str] = Field(min_length=1)
    expected_result: str = Field(min_length=1)
    trace_scenario_ids: list[str] = Field(min_length=1)
    trace_rule_ids: list[str] = Field(min_length=1)

class TestSuite(StrictModel):
    summary: str
    test_cases: list[TestCase]