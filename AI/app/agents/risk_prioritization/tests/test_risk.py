import pytest

from ..config.config import Config
from ..data.models import DefectRecord, RiskSignals
from ..core.pipeline import run_pipeline
from ..core.rationale import valid_llm_text
from ..core.scoring import combine, to_priority
from ..core.validation import GuardrailError, validate


def tc(i, prio="Cao", rules=None, **extra):
    return {"test_case_id": i, "title": f"t{i}", "type": "POSITIVE", "priority": prio, "preconditions": [],
            "steps": ["x"], "expected_result": "ok", "trace_scenario_ids": [], "trace_rule_ids": rules or [], **extra}


ANALYSIS = {"source_document": {"sha256": "x", "blocks": [
    {"block_id": "B1", "text": "Use Case ID | UC-14\nPriority | Cao", "module": "M07", "use_case": "UC-14"},
    {"block_id": "B2", "text": "Use Case ID | UC-3\nPriority | Thông thường", "module": "M01", "use_case": "UC-3"}]},
    "analysis": {"rules": [
        {"rule_id": "C008-BR1", "category": "BUSINESS_RULE", "source_refs": [{"block_id": "B1"}]},
        {"rule_id": "C002-BR-UC3-01", "category": "BUSINESS_RULE", "source_refs": [{"block_id": "B2"}]}]}}


def test_formula_and_thresholds():
    assert combine(5, 5, 5, 5, Config()) == (5.0, 100.0)
    assert combine(1, 1, 1, 1, Config())[1] == 0
    c = Config()
    assert to_priority(75, c) == "P1" and to_priority(74.99, c) == "P2"
    assert to_priority(55, c) == "P2" and to_priority(35, c) == "P3" and to_priority(34.99, c) == "P4"


def test_preserves_all_tcs_and_is_deterministic():
    cases = [tc("TC-01"), tc("TC-02", "Thấp"), tc("TC-10", "Thông thường")]
    a, b = run_pipeline(cases), run_pipeline(cases)
    assert [i.test_case_id for i in a.items] == [i.test_case_id for i in b.items]
    assert a.input_hash == b.input_hash and a.total == 3


def test_module_mapping_and_critical_boost():
    r = run_pipeline([tc("TC-01", rules=["C008-BR1"]), tc("TC-02", rules=["C002-BR-UC3-01"])], ANALYSIS)
    m = {i.test_case_id: i for i in r.items}
    assert m["TC-01"].module == "M07" and m["TC-01"].R == 5
    assert m["TC-02"].module == "M01" and m["TC-02"].R == 3


def test_dependency_promotion_and_order():
    r = run_pipeline([tc("TC-01", "Thấp"), tc("TC-02", "Cao")], None, RiskSignals(dependencies={"TC-02": ["TC-01"]}))
    ids = [i.test_case_id for i in r.items]
    assert ids.index("TC-01") < ids.index("TC-02")
    assert next(i for i in r.items if i.test_case_id == "TC-01").promoted_by_dependency


def test_cycle_keeps_all_tcs():
    sig = RiskSignals(dependencies={"TC-01": ["TC-02"], "TC-02": ["TC-01"]})
    r = run_pipeline([tc("TC-01"), tc("TC-02"), tc("TC-03")], None, sig)
    assert r.total == 3 and any("cycle" in w for w in r.warnings)
    assert {i.test_case_id for i in r.items if i.in_cycle} == {"TC-01", "TC-02"}


def test_defect_history_h():
    cases = [tc("TC-01", rules=["C008-BR1"]), tc("TC-02", rules=["C002-BR-UC3-01"])]
    sig = RiskSignals(defects=[DefectRecord(defect_id="DEF-1", module="M07", severity=4, age_releases=0)])
    m = {i.test_case_id: i for i in run_pipeline(cases, ANALYSIS, sig).items}
    assert m["TC-01"].H == 5 and m["TC-02"].H == 1


def test_guardrail_detects_tampering():
    cases = [tc("TC-01"), tc("TC-02")]
    r = run_pipeline(cases)
    r.items[0].test_case["title"] = "changed"
    with pytest.raises(GuardrailError):
        validate(r, cases, Config())


def test_llm_rationale_checker():
    item = run_pipeline([tc("TC-01", rules=["C008-BR1"])], ANALYSIS).items[0]
    good = f"{item.test_case_id} là {item.priority} với S100={item.S100:g}, nguồn C008-BR1."
    assert valid_llm_text(good, item)
    assert not valid_llm_text(good + " theo C999-X", item)
    assert not valid_llm_text("không có id", item)

def test_blocked_by_follows_blocked_dependency():
    from datetime import datetime, timezone
    from ..orchestrator.models import ExecutionResult, ExecutionState
    from ..orchestrator.orchestrator import _find_blocking_tc
    now = datetime.now(timezone.utc)
    tc_map = {"A": {"depends_on": []}, "B": {"depends_on": ["A"]}, "C": {"depends_on": ["B"]}, "D": {"depends_on": []}}
    def r(tid, state, blocked_by=None):
        return ExecutionResult(test_case_id=tid, state=state, attempt=1, start_time=now, end_time=now,
                               duration_ms=0, blocked_by=blocked_by)
    results = {"A": r("A", ExecutionState.ERROR), "B": r("B", ExecutionState.BLOCKED, ["A"])}
    assert _find_blocking_tc("B", tc_map, results) == ["A"]
    assert _find_blocking_tc("C", tc_map, {"B": r("B", ExecutionState.BLOCKED, ["A"])}) == ["A"]
    assert _find_blocking_tc("D", tc_map, {}) == []
