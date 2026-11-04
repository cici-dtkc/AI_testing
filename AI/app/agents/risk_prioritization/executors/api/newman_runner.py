import json
import subprocess
from pathlib import Path
from .config import NEWMAN_TIMEOUT


def _extract_failures(report: dict) -> list[dict]:
    failures = []

    for execution in report.get("run", {}).get("executions", []):
        failed_assertions = [a for a in execution.get("assertions", []) if a.get("error") is not None]
        if not failed_assertions:
            continue

        item = execution.get("item", {})
        request = execution.get("request", {})
        response = execution.get("response", {})

        failure = {
            "request_name": item.get("name", "Unknown"),
            "request_method": request.get("method", "GET"),
            "request_url": request.get("url", {}).get("raw", ""),
            "response_code": response.get("code", 0),
            "response_status": response.get("status", "Unknown"),
            "failed_assertions": [
                {
                    "assertion_name": a.get("assertion", "Unknown assertion"),
                    "error_type": a.get("error", {}).get("name", "Error"),
                    "error_message": a.get("error", {}).get("message", "Unknown error")
                }
                for a in failed_assertions
            ]
        }

        if request.get("body"):
            failure["request_body"] = request["body"]
        if response.get("body"):
            failure["response_body"] = response["body"]

        failures.append(failure)

    return failures


def run_collection(collection_path: str, environment_vars: dict, output_dir: str) -> dict:
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    json_report = output_path / "newman-report.json"
    html_report = output_path / "newman-report.html"

    cmd = [
        "newman", "run", collection_path,
        "--reporters", "cli,json,html",
        "--reporter-json-export", str(json_report),
        "--reporter-html-export", str(html_report),
        "--timeout-request", str(NEWMAN_TIMEOUT * 1000)
    ]

    for key, value in environment_vars.items():
        cmd.extend(["--env-var", f"{key}={value}"])

    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=NEWMAN_TIMEOUT,
            check=False
        )

        if not json_report.exists():
            return {
                "passed": False,
                "total_requests": 0,
                "failed_requests": 0,
                "duration_ms": 0,
                "report_paths": [],
                "error": "Newman report not generated"
            }

        with open(json_report) as f:
            report = json.load(f)

        stats = report.get("run", {}).get("stats", {})
        timings = report.get("run", {}).get("timings", {})

        total = stats.get("requests", {}).get("total", 0)
        failed = stats.get("requests", {}).get("failed", 0)
        duration = timings.get("completed", 0)

        failures = _extract_failures(report) if failed > 0 else []

        return {
            "passed": failed == 0,
            "total_requests": total,
            "failed_requests": failed,
            "duration_ms": duration,
            "report_paths": [str(json_report), str(html_report)],
            "error": None,
            "failures": failures
        }

    except subprocess.TimeoutExpired:
        return {
            "passed": False,
            "total_requests": 0,
            "failed_requests": 0,
            "duration_ms": NEWMAN_TIMEOUT * 1000,
            "report_paths": [],
            "error": f"Newman timeout after {NEWMAN_TIMEOUT}s"
        }
    except Exception as e:
        return {
            "passed": False,
            "total_requests": 0,
            "failed_requests": 0,
            "duration_ms": 0,
            "report_paths": [],
            "error": str(e)
        }

try:
    subprocess.run(["newman", "--version"], capture_output=True, check=True)
except (subprocess.CalledProcessError, FileNotFoundError):
    raise ImportError("Newman CLI not found. Install with: npm install -g newman newman-reporter-html")
