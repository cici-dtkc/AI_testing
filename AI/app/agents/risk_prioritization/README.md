# Risk Prioritization & Test Orchestration

End-to-end test prioritization, scheduling, and execution system. Prioritizes test cases deterministically, schedules by dependency graph, and executes via Playwright (UI) or Newman (API).

## Quick Start

### Prerequisites

```bash
# Python dependencies
cd AI_testing/AI/app/agents/risk_prioritization
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
pip install -r executors/requirements.txt
playwright install

# Newman for API testing
npm install -g newman newman-reporter-html

# Start test target (Java web app)
cd testing
./gradlew run
# Verify at http://localhost:8080
```

If venv unavailable: `sudo apt install python3-venv`

### End-to-End Workflow

**Step 1: Generate test cases** (via test_case_generator agent - see parent directory)

**Step 2: Prioritize by risk**

```bash
python -m AI.app.agents.risk_prioritization.main \
  --test-cases AI/app/agents/test_case_generator/outputs/comic/test_cases/test_cases.json \
  --analysis AI/app/agents/test_case_generator/outputs/comic/analysis.json \
  --signals-dir AI/app/agents/risk_prioritization/examples \
  --out AI/app/agents/risk_prioritization/outputs
```

Output: `prioritized_tests.json` with execution order, priority (P1-P4), dependencies.

**Step 3: Execute tests**

```python
from AI.app.agents.risk_prioritization.orchestrator.orchestrator import run_orchestrator

report = run_orchestrator(
    prioritized_tests_path="outputs/prioritized_tests.json",
    max_workers=2
)

print(f"Completed: {report.state_counts}")
print(f"Report: outputs/execution_report_{report.run_id}.html")
```

View HTML report with embedded failure screenshots and evidence links.

## How It Works

### Risk Scoring

Deterministic formula: **Score = R × C × H × D × S**

- **R** (Requirements): complexity from SRS analysis, critical module boost
- **C** (Code Churn): recent changes in affected modules
- **H** (Defect History): past failures in related areas
- **D** (Dependency Count): prerequisites increase risk
- **S** (Severity): mapped from test case impact level

Priority thresholds: P1 (S≥70), P2 (≥40), P3 (≥15), P4 (rest). Dependencies promoted to dependent's priority.

### Execution Flow

1. **Scheduler** groups tests by dependency level (topological sort via Kahn's algorithm)
2. **Orchestrator** executes level 0 (no prereqs) in parallel, then level 1, etc.
3. **Executor routing**: `suggested_runner="ui"` → Playwright, `"api"` → Newman
4. **Retry logic**: Failed tests re-run once; pass on retry → `FLAKY`, fail twice → `FAILED`
5. **Dependency blocking**: Failed test blocks all dependents transitively
6. **Evidence store**: All artifacts saved with SHA-256 hashing, metadata indexed

### Test Case Format

**UI tests** need Vietnamese step descriptions:
```json
{
  "id": "TC-001",
  "test_steps": [
    "Mở /login",
    "Nhập username = admin",
    "Nhập password = 123456",
    "Nhấn Đăng nhập"
  ],
  "expected_result": "Hiển thị trang chủ",
  "suggested_runner": "ui"
}
```

**API tests** reference Postman collections:
```json
{
  "id": "TC-002",
  "collection_path": "collections/checkout.postman_collection.json",
  "environment": {"base_url": "http://localhost:8080/api"},
  "suggested_runner": "api"
}
```

### Executors

**Playwright**: Regex-based Vietnamese parser (`step_parser.py`) maps phrases to browser actions. Selector strategy: `data-testid` > `id` > `name` > text content. Captures screenshots on failure, full trace for debugging.

**Newman**: Runs Postman collections via subprocess, parses JSON report. Pass/fail based on failed request count (0 = PASSED). Generates HTML report with request/response details.

## Architecture

**Orchestrator** (`orchestrator/`): Dependency scheduler, state manager, retry handler. Groups test cases by dependency level, executes via topological order (Kahn's algorithm).

**Executors** (`executors/`): Isolated runners for UI (Playwright) and API (Newman). Return `ExecutionResult` with state, evidence URIs, duration.

**Risk Scoring**: Deterministic formula using signals (defect history, code churn, requirements complexity). LLM only writes rationale text.

Pipeline: signals → mapping (analysis.json: rule → block → module/use case) → scoring (R,C,H,D,S,S100) → rank → dependency order (Kahn, S_eff) → rationale → guardrails (validation.py).

## Evidence & Traceability

### Evidence Store

Centralized storage with SHA-256 integrity checking and metadata indexing.

```python
from AI.app.agents.risk_prioritization.evidence_store.api import store_evidence, get_evidence

uri = store_evidence(
    run_id="run-123",
    tc_id="TC-001",
    attempt=1,
    artifact_type="screenshot",
    source_path="/tmp/failure.png"
)

artifacts = get_evidence(run_id="run-123", tc_id="TC-001")
```

**Storage structure:** `outputs/evidence/{run_id}/{tc_id}/attempt_{n}/{type}_{timestamp}.{ext}`

**Metadata:** `outputs/evidence/{run_id}/metadata.json` tracks all artifacts with SHA-256 hashes for integrity verification.

**Retention policy:** Configurable cleanup keeps last N runs, with separate retention rules for passes vs failures.

### Artifact Types

- **UI tests**: screenshots (failure only), trace.zip, video (optional)
- **API tests**: newman-report.json, newman-report.html, request/response logs

Evidence URIs tracked in `ExecutionResult` for full traceability from test case to artifacts.

## Testing & Verification

**Prioritization report** (`outputs/prioritized_report.html`): Risk scores, priority assignments, dependency graph, execution order.

**Execution report** (`outputs/execution_report_{run_id}.html`): Test results table with:
- State counts (PASSED/FAILED/BLOCKED/FLAKY/ERROR)
- Per-test: ID, priority, state, duration, evidence links
- Inline screenshots for failures
- Total execution time

Reports use relative paths for portability.

Create `pytest.ini` at repository root:

```ini
[pytest]
pythonpath = .
testpaths = AI
```

Run tests:

```bash
python -m pytest AI/app/agents/risk_prioritization/tests -v
```

Expected: 8 passed tests covering formula, dependency promotion, cycle detection, guardrails.

## Configuration

**Orchestrator** (`orchestrator/config.py`):
- `MAX_WORKERS = 2` - Parallel execution limit
- `EXECUTION_TIMEOUT = 300` - Per-test timeout (seconds)
- `RETRY_ENABLED = True` - Flaky test detection

**Playwright** (`executors/playwright/config.py`):
- `BASE_URL = "http://localhost:8080"`
- `BROWSER_TYPE = "chromium"`
- `HEADLESS = True`
- `SELECTOR_MAP` - Vietnamese field name → CSS selector mappings

**Newman** (`executors/api/config.py`):
- `BASE_URL = "http://localhost:8080/api"`
- `NEWMAN_TIMEOUT = 120`
- `NEWMAN_REPORTERS = ["cli", "json", "html"]`

**Risk scoring** (`config/config.py`):
- Weights: `W_R = 3.0`, `W_C = 2.0`, `W_H = 2.5`, `W_D = 1.5`
- Priority thresholds: P1/P2/P3/P4 boundaries
- Critical modules list (higher R scores)
