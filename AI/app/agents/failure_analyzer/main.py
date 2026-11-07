from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

from .core.analyzer import (
    FailureInputError,
    analyze_input,
    apply_gemini_assessments,
    load_input,
    write_outputs,
)
from .core.run_adapter import load_run_evidence
from .llm.gemini_provider import GeminiAnalysisError, analyze_with_gemini


def _load_local_env_files() -> None:
    env_paths = (
        Path(__file__).resolve().parents[3] / ".env",
        Path(__file__).resolve().parent / ".env",
    )
    for env_path in env_paths:
        if not env_path.is_file():
            continue
        for line in env_path.read_text(encoding="utf-8").splitlines():
            stripped = line.strip()
            if not stripped or stripped.startswith("#") or "=" not in stripped:
                continue
            name, _, value = stripped.partition("=")
            name = name.strip()
            value = value.strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
                value = value[1:-1]
            if name:
                os.environ.setdefault(name, value)


def main() -> int:
    _load_local_env_files()
    parser = argparse.ArgumentParser(description="Analyze test failures from a JSON input file.")
    source = parser.add_mutually_exclusive_group()
    source.add_argument(
        "--input",
        type=Path,
        help="Normalized failure input JSON; defaults to the included sample.",
    )
    source.add_argument(
        "--run-dir",
        type=Path,
        help="Evidence-only run directory under AI/app/storage/runs.",
    )
    parser.add_argument("--project-id", default="unknown-project")
    parser.add_argument(
        "--provider",
        choices=("rules", "gemini"),
        default="rules",
        help="Use Gemini for Vietnamese analysis; requires GEMINI_API_KEY.",
    )
    parser.add_argument(
        "--model",
        default=os.getenv("GEMINI_MODEL", "gemini-flash-latest"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).resolve().parent / "outputs" / "latest",
        help="Directory for failure_analysis.json and failure_analysis.html.",
    )
    args = parser.parse_args()
    try:
        if args.run_dir:
            payload = load_run_evidence(args.run_dir, args.project_id)
        else:
            input_path = args.input or (
                Path(__file__).resolve().parent / "examples" / "sample_failure_input.json"
            )
            payload = load_input(input_path)
        result = analyze_input(payload)
        if args.run_dir:
            args.output.mkdir(parents=True, exist_ok=True)
            (args.output / "prepared_input.json").write_text(
                json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
        if args.provider == "gemini":
            api_key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY", "")
            remote = analyze_with_gemini(payload, api_key, args.model)
            result = apply_gemini_assessments(result, remote, args.model)
        paths = write_outputs(result, args.output)
    except (FailureInputError, GeminiAnalysisError, OSError, ValueError) as exc:
        print(f"Failure analysis failed: {exc}", file=sys.stderr)
        return 1

    for analysis in result["analyses"]:
        print(
            f"{analysis['test_case_id']} attempt={analysis['attempt_id']} "
            f"category={analysis['primary_category']} status={analysis['analysis_status']}"
        )
    print(f"JSON: {paths['json']}")
    print(f"HTML: {paths['html']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
