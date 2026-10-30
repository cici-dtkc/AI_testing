"""Lập kế hoạch kiểm thử hộp trắng từ mã nguồn Python.

Kiến thức lấy từ ``6-Kiemthuhoptrang.pdf``:

* **Basis Path** — V(G) = E − N + 2p; tài liệu dùng cách tính nhanh
  ``V(G) = số nút quyết định + 1`` và coi V(G) là số test case tối thiểu.
* **Control-flow/Coverage** — các mức 1 (câu lệnh), 2 (nhánh), 3 (điều kiện),
  4 (kết hợp nhánh + điều kiện).
* **Data-flow** — theo dõi vòng đời biến (def-use) để bắt lỗi như dùng biến
  chưa khởi tạo hoặc dùng sau khi huỷ.

Đây là phân tích tĩnh bằng ``ast``: đếm nút quyết định, điều kiện con, câu lệnh
và các cặp định nghĩa–sử dụng. Kết quả là **kế hoạch** (số case tối thiểu, hạng
mục cần phủ), không thay thế công cụ đo coverage thực tế.
"""

from __future__ import annotations

import ast
from pathlib import Path

from .models import CoverageTarget, TechniqueChoice, WhiteBoxPlan

_SKIP_DIRS = {'__pycache__', '.git', '.venv', 'venv', 'node_modules', '.mypy_cache', '.pytest_cache'}
_MAX_FUNCTION_LINES = 400


def _iter_scope_nodes(root: ast.AST):
    """Duyệt node trong một function, dừng ở function/class lồng bên trong."""
    for child in ast.iter_child_nodes(root):
        yield child
        if isinstance(child, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef, ast.Lambda)):
            continue
        yield from _iter_scope_nodes(child)


def _atomic_conditions(test: ast.AST | None) -> int:
    if test is None:
        return 1
    if isinstance(test, ast.BoolOp):
        return sum(_atomic_conditions(value) for value in test.values)
    return 1


def _is_decision(node: ast.AST) -> bool:
    return isinstance(node, (ast.If, ast.While, ast.For, ast.AsyncFor, ast.IfExp, ast.Assert))


def _data_flow(fn: ast.AST) -> tuple[int, int]:
    """Đếm cặp def-use đơn giản (theo thứ tự nguồn) và số biến có def + use."""
    events: list[tuple[int, int, str, str]] = []
    args = getattr(fn, 'args', None)
    if args is not None:
        for arg in list(getattr(args, 'posonlyargs', [])) + list(args.args) + list(args.kwonlyargs):
            events.append((fn.lineno, 0, arg.arg, 'd'))

    for node in _iter_scope_nodes(fn):
        if isinstance(node, ast.Name):
            kind = 'd' if isinstance(node.ctx, (ast.Store, ast.Del)) else 'u'
            events.append((node.lineno, node.col_offset, node.id, kind))
    events.sort()

    per_variable: dict[str, dict[str, int]] = {}
    for _, _, name, kind in events:
        bucket = per_variable.setdefault(name, {'d': 0, 'u': 0})
        bucket[kind] += 1

    pairs = 0
    used_variables = 0
    for bucket in per_variable.values():
        if bucket['d'] and bucket['u']:
            used_variables += 1
            pairs += bucket['d'] * bucket['u']
    return pairs, used_variables


def _loc(source: str, node: ast.AST) -> str:
    line = getattr(node, 'lineno', 1)
    end = getattr(node, 'end_lineno', line) or line
    if end - line > _MAX_FUNCTION_LINES:
        return f'{source}:{line}'
    return f'{source}:{line}-{end}'


def analyze_function(
    fn: ast.FunctionDef | ast.AsyncFunctionDef,
    source_name: str,
) -> WhiteBoxPlan:
    """Phân tích một hàm và trả về kế hoạch hộp trắng tương ứng."""
    nodes = list(_iter_scope_nodes(fn))
    statements = sum(
        1 for node in nodes
        if isinstance(node, ast.stmt) and not isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef))
    )
    decisions = sum(1 for node in nodes if _is_decision(node))
    conditions = sum(
        _atomic_conditions(getattr(node, 'test', None))
        for node in nodes
        if _is_decision(node)
    )
    cyclomatic = decisions + 1
    data_flow_pairs, used_variables = _data_flow(fn)

    targets = [
        CoverageTarget(
            level='Mức 1 — Bao phủ câu lệnh',
            items=statements,
            note='Mỗi câu lệnh thực thi ≥ 1 lần; V(G) là giới hạn trên số case cần thiết.',
        ),
        CoverageTarget(
            level='Mức 2 — Bao phủ nhánh/quyết định',
            items=2 * decisions,
            note='Mỗi điểm quyết định cần cả True và False.',
        ),
        CoverageTarget(
            level='Mức 3 — Bao phủ điều kiện',
            items=2 * conditions,
            note='Mỗi điều kiện con của biểu thức phức hợp cần cả True và False.',
        ),
        CoverageTarget(
            level='Mức 4 — Kết hợp nhánh + điều kiện',
            items=2 * decisions + 2 * conditions,
            note='Kết hợp mức 2 và mức 3; dùng khi điều kiện phức hợp ảnh hưởng kết quả.',
        ),
    ]

    warnings: list[str] = []
    if decisions == 0:
        warnings.append('Hàm không có nhánh: 1 test case là đủ cho mọi mức bao phủ.')
    if cyclomatic > 20:
        warnings.append(
            f'V(G) = {cyclomatic} > 20 — hàm quá phức tạp, nên tách nhỏ trước khi thiết kế '
            'test case đường cơ sở.'
        )
    for node in nodes:
        if isinstance(node, (ast.Try, ast.ExceptHandler)):
            warnings.append('Có xử lý ngoại lệ: phân tích tĩnh không bao phủ hết luồng exception.')
            break

    techniques: list[TechniqueChoice] = [
        TechniqueChoice(
            technique='BASIS_PATH',
            variant='MCCABE',
            family='WHITE_BOX',
            role='PRIMARY',
            rationale=(
                f'{decisions} điểm quyết định → V(G) = P + 1 = {cyclomatic}; cần tối thiểu '
                f'{cyclomatic} test case cho các đường cơ sở độc lập.'
            ),
            obligations=[
                f'Vẽ đồ thị luồng điều khiển của hàm và xác nhận V(G) = {cyclomatic}.',
                f'Sinh tối thiểu {cyclomatic} test case, mỗi case đi một đường cơ sở độc lập.',
            ],
            estimated_cases=cyclomatic,
        ),
        TechniqueChoice(
            technique='BRANCH_COVERAGE',
            variant='LEVEL_2',
            family='WHITE_BOX',
            role='SUPPORTING',
            rationale=(
                f'{decisions} điểm quyết định, {conditions} điều kiện con → cần phủ đủ '
                'True/False ở từng nhánh và từng điều kiện con.'
            ),
            obligations=[
                f'Phủ {2 * decisions} hạng mục nhánh và {2 * conditions} hạng mục điều kiện.',
                'Đo lại bằng công cụ coverage (coverage.py/Emma...) sau khi chạy bộ case.',
            ],
            estimated_cases=max(2, min(40, decisions * 2)),
        ),
    ]
    if data_flow_pairs:
        techniques.append(
            TechniqueChoice(
                technique='DATA_FLOW',
                variant='ALL_USES',
                family='WHITE_BOX',
                role='SUPPORTING',
                rationale=(
                    f'{used_variables} biến có cả định nghĩa và sử dụng, ước tính '
                    f'{data_flow_pairs} cặp def-use → nên bổ sung case dòng dữ liệu.'
                ),
                obligations=[
                    'Mỗi cặp def-use ước tính phải được phủ ít nhất 1 lần (All-Uses).',
                    'Kiểm tra biến được dùng trước khi khởi tạo và sau khi huỷ nếu có.',
                ],
                estimated_cases=min(30, max(1, used_variables)),
            )
        )

    return WhiteBoxPlan(
        module=f'{source_name}::{fn.name}',
        source=_loc(source_name, fn),
        statements=statements,
        decisions=decisions,
        conditions=conditions,
        cyclomatic=cyclomatic,
        basis_paths=cyclomatic,
        coverage_targets=targets,
        data_flow_pairs=data_flow_pairs,
        techniques=techniques,
        warnings=warnings,
    )


def analyze_source_text(source_text: str, source_name: str) -> list[WhiteBoxPlan]:
    """Phân tích toàn bộ hàm trong một file nguồn."""
    tree = ast.parse(source_text, filename=source_name)
    plans: list[WhiteBoxPlan] = []
    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            plans.append(analyze_function(node, source_name))
    plans.sort(key=lambda plan: plan.source)
    return plans


def analyze_path(path: Path) -> tuple[list[WhiteBoxPlan], list[str], dict[str, str]]:
    """Phân tích file hoặc thư mục mã nguồn.

    Trả về (kế hoạch hộp trắng, cảnh báo, mã nguồn từng hàm để đưa vào prompt).
    """
    warnings: list[str] = []
    plans: list[WhiteBoxPlan] = []
    sources: dict[str, str] = {}

    if not path.exists():
        return plans, [f'Không tìm thấy mã nguồn tại: {path}'], sources

    if path.is_file():
        files = [path]
    else:
        files = sorted(
            file for file in path.rglob('*.py')
            if not any(part in _SKIP_DIRS for part in file.parts)
        )

    for file in files:
        try:
            text = file.read_text(encoding='utf-8')
            tree = ast.parse(text, filename=str(file))
        except (OSError, SyntaxError) as exc:
            warnings.append(f'Bỏ qua {file}: {exc}')
            continue
        source_name = file.as_posix()
        for node in ast.walk(tree):
            if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
                plan = analyze_function(node, source_name)
                plans.append(plan)
                segment = ast.get_source_segment(text, node)
                if segment:
                    sources[plan.module] = segment

    if not plans:
        warnings.append('Không tìm thấy hàm nào để phân tích hộp trắng.')
    return plans, warnings, sources
