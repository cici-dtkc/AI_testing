"""Run end-to-end test execution with orchestrator."""
import json
from pathlib import Path
from .orchestrator.orchestrator import run_orchestrator
from .config.config import OrchestratorConfig

def main():
    # Load prioritized tests from phase 0 output
    prioritized_path = Path("app/agents/risk_prioritization/outputs/prioritized_tests.json")

    with open(prioritized_path, "r", encoding="utf-8") as f:
        prioritization_result = json.load(f)

    print(f"Loaded {len(prioritization_result['items'])} test cases")
    print(f"Distribution: {prioritization_result['distribution']}")

    # Configure orchestrator
    config = OrchestratorConfig(
        base_url="http://localhost:8080/TTLTW_Nhom6",
        max_workers=2,
        execution_timeout=300,
        retry_timeout=300
    )

    # Run execution
    print("\nStarting test execution...")
    report = run_orchestrator(
        prioritization_result=prioritization_result,
        config=config,
        report_path="app/agents/risk_prioritization/outputs/execution_report.html"
    )

    print(f"\nExecution complete!")
    print(f"Run ID: {report.run_id}")
    print(f"State counts: {report.state_counts}")
    print(f"Total duration: {report.total_duration_ms:.2f}s")
    print(f"Report: app/agents/risk_prioritization/outputs/execution_report_{report.run_id}.html")

    # Save execution results
    results_path = Path("app/agents/risk_prioritization/outputs/execution_results.json")
    with open(results_path, "w", encoding="utf-8") as f:
        json.dump({
            "run_id": report.run_id,
            "state_counts": report.state_counts,
            "total_duration": report.total_duration_ms,
            "started_at": report.started_at.isoformat(),
            "completed_at": report.completed_at.isoformat()
        }, f, indent=2)

    print(f"Results saved to {results_path}")

if __name__ == "__main__":
    main()
