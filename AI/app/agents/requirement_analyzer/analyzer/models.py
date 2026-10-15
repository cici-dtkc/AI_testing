from typing import Literal
from pydantic import BaseModel, ConfigDict, Field

def gemini_schema(model: type[BaseModel]) -> dict:
    schema=model.model_json_schema()
    def remove_additional_properties(value):
        if isinstance(value,dict):
            value.pop('additionalProperties',None)
            for child in value.values():remove_additional_properties(child)
        elif isinstance(value,list):
            for child in value:remove_additional_properties(child)
    remove_additional_properties(schema)
    return schema

class StrictModel(BaseModel):
    model_config = ConfigDict(extra='forbid')
class Block(StrictModel):
    block_id: str
    location: str
    text: str
    module: str | None = None
    use_case: str | None = None
class Document(StrictModel):
    document_id: str
    version: str
    sha256: str
    blocks: list[Block]
class Ref(StrictModel):
    block_id: str
    quote: str = Field(min_length=1)
class Rule(StrictModel):
    rule_id: str
    statement: str
    category: Literal['FUNCTIONAL','NON_FUNCTIONAL','BUSINESS_RULE']
    actors: list[str]
    preconditions: list[str]
    conditions: list[str]
    action: str
    outcome: str
    source_refs: list[Ref] = Field(min_length=1)
class Scenario(StrictModel):
    scenario_id: str
    title: str
    type: Literal['POSITIVE','NEGATIVE','BOUNDARY','AUTHORIZATION','STATE','NON_FUNCTIONAL']
    rule_ids: list[str] = Field(min_length=1)
    given: str
    when: str
    then: str | None
    expected_status: Literal['SUPPORTED','UNKNOWN']
class Issue(StrictModel):
    issue_id: str
    type: Literal['AMBIGUITY','MISSING_INFORMATION','CONFLICT','NOT_TESTABLE']
    description: str
    question: str
    source_refs: list[Ref] = Field(min_length=1)
    blocks_scenarios: list[str]
class Analysis(StrictModel):
    summary: str
    rules: list[Rule]
    scenarios: list[Scenario]
    issues: list[Issue]
