#!/usr/bin/env python3
"""Quick test script to verify the 4 fixed test cases."""

import json
import sys
from pathlib import Path
from datetime import datetime

sys.path.insert(0, str(Path(__file__).parent.parent))

from ..executors.playwright.playwright_executor import PlaywrightExecutor

def run_test(tc_id, test_case, executor):
    print(f"\n{'='*60}")
    print(f"Running: {tc_id}")
    print(f"Title: {test_case.get('title', 'N/A')}")
    print(f"Steps: {test_case.get('steps', [])}")
    print(f"Expected: {test_case.get('expected_result', 'N/A')}")
    print(f"{'='*60}")

    run_id = datetime.now().strftime("%Y%m%d-%H%M%S")
    tc_with_id = {**test_case, "test_case_id": tc_id}

    result = executor.execute_test_case(tc_with_id, run_id, attempt=1)

    status = "✓ PASS" if result.state == "PASSED" else f"✗ {result.state}"
    print(f"\nResult: {status}")
    if result.error_message:
        print(f"Error: {result.error_message}")
    print(f"Duration: {result.duration_ms}ms")

    return result

def main():
    prioritized_path = Path(__file__).resolve().parent.parent / "outputs" / "prioritized_tests.json"

    with open(prioritized_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    target_ids = ['QuanLyWebsite-TC-01', 'TimKiem-TC-04', 'QuanLyTonKho-TC-01', 'GiamGia-TC-03']
    executor = PlaywrightExecutor()

    results = {}
    for item in data['items']:
        tc_id = item['test_case_id']
        if tc_id in target_ids:
            test_case = item['test_case']
            results[tc_id] = run_test(tc_id, test_case, executor)

    print(f"\n\n{'='*60}")
    print("SUMMARY")
    print(f"{'='*60}")
    for tc_id in target_ids:
        if tc_id in results:
            r = results[tc_id]
            status = "✓ PASS" if r.state == "PASSED" else f"✗ {r.state}"
            print(f"{tc_id}: {status}")

    passed = sum(1 for r in results.values() if r.state == "PASSED")
    print(f"\nTotal: {passed}/{len(results)} passed")

if __name__ == "__main__":
    main()
