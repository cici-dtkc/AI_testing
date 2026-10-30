import json
from pathlib import Path
from collections.abc import Callable

from .coverage import analyze_path
from .gemini import generate_test_cases_per_module
from .models import TestSuite, TechniquePlan
from .techniques import build_technique_plan
from .validation import validate_test_cases

# Giới hạn mã nguồn đưa vào prompt để không phình input của Gemini.
MAX_WHITE_BOX_SOURCE_CHARS = 12000


def _bounded_sources(
    sources: dict[str, str], limit: int = MAX_WHITE_BOX_SOURCE_CHARS
) -> tuple[dict[str, str], list[str]]:
    selected: dict[str, str] = {}
    warnings: list[str] = []
    used = 0
    for module, code in sources.items():
        if used + len(code) > limit:
            warnings.append(
                f'Bỏ qua mã nguồn của {module}: vượt giới hạn {limit} ký tự cho prompt.'
            )
            continue
        selected[module] = code
        used += len(code)
    return selected, warnings


def load_analysis(analysis_path: Path) -> dict:
    if not analysis_path.is_file():
        raise ValueError(f'Không tìm thấy file analysis đầu vào tại: {analysis_path}')
    return json.loads(analysis_path.read_text(encoding='utf-8'))


def build_plan(
    analysis_path: Path,
    code_path: Path | None = None,
) -> tuple[TechniquePlan, list[str], dict[str, str]]:
    """Chỉ chạy bước chọn kỹ thuật (không gọi LLM) để kiểm tra/quyết định trước."""
    analysis_data = load_analysis(analysis_path)
    white_box, warnings, sources = ([], [], {})
    if code_path is not None:
        white_box, warnings, sources = analyze_path(Path(code_path))
    plan = build_technique_plan(analysis_data, white_box)
    return plan, warnings, sources


def generate_full_test_cases(
    analysis_path: Path,
    key: str,
    model: str,
    code_path: Path | None = None,
    module_delay: float | None = None,
    progress: Callable[[str], None] | None = None,
    cache_dir: Path | None = None,
) -> tuple[TestSuite, TechniquePlan, list[str]]:
    analysis_data = load_analysis(analysis_path)

    # Bước 1: engine quyết định kỹ thuật cho từng module (hộp đen + hộp trắng).
    white_box, white_box_warnings, sources = ([], [], {})
    if code_path is not None:
        white_box, white_box_warnings, sources = analyze_path(Path(code_path))
    plan = build_technique_plan(analysis_data, white_box)

    # Bước 2: LLM hiện thực hoá nghĩa vụ bao phủ — mỗi module/hàm một request nhỏ
    # để không bỏ sót scenario vì input quá lớn.
    white_box_sources, source_warnings = _bounded_sources(sources)
    test_suite, generation_warnings = generate_test_cases_per_module(
        analysis_data,
        plan,
        key,
        model,
        white_box_sources=white_box_sources,
        delay=module_delay,
        progress=progress,
        cache_dir=cache_dir,
    )

    # Bước 3: kiểm tra truy vết, coverage tag và mức phủ tối thiểu.
    warnings = (
        white_box_warnings
        + source_warnings
        + generation_warnings
        + validate_test_cases(test_suite, analysis_data, plan)
    )

    return test_suite, plan, warnings