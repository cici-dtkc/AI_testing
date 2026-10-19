# risk_prioritization (demo)

---
### CD in folder

```commandline
cd /AI_testing/AI/app/agents/risk_prioritization
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```
If venv isn’t available, install it first:
```commandline
sudo apt install python3-venv
```
---

Deterministic risk scoring + ordering of generated test cases. LLM (optional) only writes `rationale`.

Run: `python -m app.agents.risk_prioritization.main --test-cases <test_cases.json|report.html> --analysis <analysis.json> --signals-dir app/agents/risk_prioritization/examples --out outputs`

---
example:
```
python -m AI.app.agents.risk_prioritization.main \
  --test-cases AI/app/agents/test_case_generator/outputs/comic/test_cases/test_cases.json \
  --analysis AI/app/agents/test_case_generator/outputs/comic/analysis.json \
  --signals-dir AI/app/agents/risk_prioritization/examples \
  --out AI/app/agents/risk_prioritization/outputs
```

success run
```
65 TC | {'P1': 6, 'P2': 2, 'P3': 9, 'P4': 48} | hash ff71f928f16a
{'json': 'AI/app/agents/risk_prioritization/outputs/prioritized_tests.json', 'html': 'AI/app/agents/risk_prioritization/outputs/prioritized_report.html'}
```

Pipeline: signals -> mapping (analysis.json: rule -> block -> module/use case) -> scoring (R,C,H,D,S,S100)
-> rank -> dependency order (Kahn, S_eff) -> rationale -> guardrails (validation.py).

Output `prioritized_tests.json`: per TC `execution_order`, `priority`, `S`, `S100`, `R/C/H/D`, `defaults_used`,
`depends_on`, `promoted_by_dependency`, `suggested_runner` (ui|api hint for the future Orchestrator), original `test_case`.

Assumptions to review: weights/thresholds (config.py), `critical_modules` R boost, demo signal files in `examples/`.

---
### Testing
Put pytest.ini at src AI_testing/pytest.init root folder

```
[pytest]
pythonpath = .
testpaths = AI
```

command to test 

```commandline
python -m pytest AI/app/agents/risk_prioritization/tests -v
```

success test 

```
================================================================================ test session starts =================================================================================
platform linux -- Python 3.12.3, pytest-9.1.1, pluggy-1.7.0 -- /media/thaniel/Materials/intellijworkspace/AI_testing/AI/app/agents/risk_prioritization/.venv/bin/python
cachedir: .pytest_cache
rootdir: /media/thaniel/Materials/intellijworkspace/AI_testing
configfile: pytest.ini
plugins: anyio-4.15.1, langsmith-0.14.11
collected 8 items

AI/app/agents/risk_prioritization/tests/test_risk.py::test_formula_and_thresholds PASSED                                                                                       [ 12%]
AI/app/agents/risk_prioritization/tests/test_risk.py::test_preserves_all_tcs_and_is_deterministic PASSED                                                                       [ 25%]
AI/app/agents/risk_prioritization/tests/test_risk.py::test_module_mapping_and_critical_boost PASSED                                                                            [ 37%]
AI/app/agents/risk_prioritization/tests/test_risk.py::test_dependency_promotion_and_order PASSED                                                                               [ 50%]
AI/app/agents/risk_prioritization/tests/test_risk.py::test_cycle_keeps_all_tcs PASSED                                                                                          [ 62%]
AI/app/agents/risk_prioritization/tests/test_risk.py::test_defect_history_h PASSED                                                                                             [ 75%]
AI/app/agents/risk_prioritization/tests/test_risk.py::test_guardrail_detects_tampering PASSED                                                                                  [ 87%]
AI/app/agents/risk_prioritization/tests/test_risk.py::test_llm_rationale_checker PASSED                                                                                        [100%]

================================================================================= 8 passed in 0.05s ==================================================================================
```
