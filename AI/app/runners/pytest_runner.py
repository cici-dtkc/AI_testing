from __future__ import annotations

import subprocess
import sys


def run_pytest(tests_dir: str) -> tuple[str, str]:
    result = subprocess.run(
        [sys.executable, "-m", "pytest", "-q", tests_dir],
        capture_output=True,
        text=True,
        timeout=600,
    )
    status = "PASS" if result.returncode == 0 else "FAIL"
    return status, (result.stdout + result.stderr)[-2000:]
