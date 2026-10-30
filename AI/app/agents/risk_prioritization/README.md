# Risk Prioritization & Test Orchestration

End-to-end test prioritization, scheduling, and execution system. Prioritizes test cases deterministically, schedules by dependency graph, and executes via Playwright (UI) or Newman (API).

## Quick Start

### Prerequisites

```bash
# Python dependencies
cd AI/app/agents/risk_prioritization
pip install -r requirements.txt
pip install -r executors/requirements.txt
playwright install

# Newman for API testing
npm install -g newman newman-reporter-html

# Start test target (Java web app)
cd testing
./gradlew appRun  # or deploy WAR to Tomcat
# Verify at http://localhost:8080/TTLTW_Nhom6/home
```

**Important:** Ensure web app is accessible before running tests. The executor uses `BASE_URL = http://localhost:8080/TTLTW_Nhom6`.

### End-to-End Workflow

**Step 1: Generate test cases** (via test_case_generator agent)

**Step 2: Prioritize by risk**

```bash
cd AI
python -m app.agents.risk_prioritization.main \
  --test-cases app/agents/test_case_generator/outputs/comic/test_cases/test_cases.json \
  --analysis app/agents/requirement_analyzer/outputs/comic/analysis.json \
  --signals-dir app/agents/risk_prioritization/examples \
  --out app/agents/risk_prioritization/outputs
```

Output: `outputs/prioritized_tests.json` with execution order, priority (P1-P4), dependencies.

**Step 3: Execute tests**

```bash
cd AI
python -m app.agents.risk_prioritization.run_execution
```

View HTML report: `outputs/execution_report_{run_id}.html` with embedded failure screenshots and evidence links.

## How It Works

### Risk Scoring

Deterministic formula: **S = wR·R + wC·C + wH·H + wD·D**

- **R** (Requirements): SRS priority + critical module boost (+1.0) + test type boost (NEGATIVE +0.2, BOUNDARY +0.15)
- **C** (Code Churn): git change frequency + structural complexity (preconditions × 0.05 + steps × 0.03)
- **H** (Defect History): past failures in related modules (exponential decay)
- **D** (Dependency): prerequisite count normalized across all TCs

**Weights:** R=0.35, C=0.25, H=0.25, D=0.15 (sum to 1.0)

**Priority thresholds:** P1 (S100≥75), P2 (≥55), P3 (≥35), P4 (rest). S100 = 25×(S-1) scales [1,5]→[0,100].

Dependencies promoted to dependent's priority.

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
- `BASE_URL = "http://localhost:8080/TTLTW_Nhom6"`
- `BROWSER_TYPE = "chromium"`
- `HEADLESS = True`
- `SELECTOR_MAP` - Vietnamese field name → CSS selector mappings

**Newman** (`executors/api/config.py`):
- `BASE_URL = "http://localhost:8080/api"`
- `NEWMAN_TIMEOUT = 120`
- `NEWMAN_REPORTERS = ["cli", "json", "html"]`

**Risk scoring** (`config/config.py`):
- Weights: `W_R = 0.35`, `W_C = 0.25`, `W_H = 0.25`, `W_D = 0.15`
- Complexity weights: `preconditions = 0.05`, `steps = 0.03`
- Type boost: `NEGATIVE = +0.2`, `BOUNDARY = +0.15`
- Priority thresholds: P1/P2/P3/P4 boundaries
- Critical modules list (higher R scores)

## Troubleshooting

### All Tests Return ERROR

**Symptom:** 100+ ERROR states, screenshots show 404 pages

**Cause:** `BASE_URL` mismatch with actual web app deployment path

**Fix:** Update `executors/playwright/config.py`:
```python
BASE_URL = "http://localhost:8080/TTLTW_Nhom6"  # Match your deployment context path
```

Verify web app is accessible: `curl http://localhost:8080/TTLTW_Nhom6/home`

### Step Parser Failures

**Symptom:** Tests fail with "Unknown step pattern" errors

**Cause:** Vietnamese test steps too abstract or missing explicit actions

**Solutions:**
1. **Add explicit actions to test cases:** "Nhập username = admin", "Nhấn Đăng nhập"
2. **Add selector mappings:** Update `SELECTOR_MAP` in `executors/playwright/config.py`
3. **Check step parser patterns:** Review `executors/playwright/step_parser.py`

**Supported patterns:**
- Navigation: `Mở /path`, `Truy cập URL`
- Input: `Nhập field = value`
- Click: `Nhấn button`
- Select: `Chọn option`
- Wait: `Chờ N giây`
- Abstract: `Thực hiện`, `Kiểm tra` (limited support)

### Evidence Not Found

**Symptom:** HTML report shows broken evidence links

**Cause:** Evidence stored in `AI/outputs/evidence/` but report expects different location

**Fix:** Ensure report is generated in correct location:
```bash
cd AI
python -m app.agents.risk_prioritization.run_execution
```

Evidence paths are relative from report location: `outputs/execution_report.html`

### Test Case ID Mismatch

**Symptom:** Evidence stored as "unknown" test case ID

**Cause:** Test case JSON uses different field name for ID

**Fix:** Already applied - executor checks both `test_case_id` and `id` fields

### Web App Not Running

**Symptom:** Connection refused errors, all tests timeout

**Solution:**
```bash
cd testing
./gradlew appRun  # Start Tomcat with auto-deploy
# Or deploy WAR manually to Tomcat
```

Verify: `curl http://localhost:8080/TTLTW_Nhom6/home` should return HTML

## Known Limitations

- **Vietnamese step parsing:** Limited to common patterns, abstract phrases may not parse
- **Dynamic content:** Tests may fail on AJAX-heavy pages without explicit waits
- **Authentication:** Tests requiring login need session management or API token setup
- **Test data:** Tests assume clean database state (no data isolation between runs)
- **Parallel execution:** Max 2 workers to avoid database contention

## Version History

**2026-10-30:**
- ✅ Phase 0 complete: Test case columns wired into scoring (preconditions, steps, type)
- ✅ Execution layer fixes: ID field, BASE_URL, step parser enhancements, auto-navigation
- ✅ Evidence store working with SHA-256 integrity checking
- 🔄 Test case generation improvements needed for better step specificity
