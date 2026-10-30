"""Chọn kỹ thuật thiết kế test case cho từng module.

Tri thức trong file này lấy từ hai tài liệu:
- ``5.Kiemthuhopden.pdf`` — kiểm thử hộp đen: phân vùng tương đương (EP),
  phân tích giá trị biên (BVA), bảng quyết định, chuyển trạng thái, use case.
- ``6-Kiemthuhoptrang.pdf`` — kiểm thử hộp trắng: đường cơ sở (McCabe), các
  mức bao phủ câu lệnh/nhánh/điều kiện, kiểm thử dòng dữ liệu.

Cách hoạt động: engine chạy **trước** LLM. Nó đọc ``analysis.json``, nhận diện
tín hiệu đặc trưng của từng module (khoảng số, tổ hợp điều kiện, vòng đời trạng
thái, phân quyền, luồng use case, yêu cầu phi chức năng...) rồi quyết định:

* ``PRIMARY`` — kỹ thuật chính, quyết định dạng test case của module.
* ``SUPPORTING`` — kỹ thuật bổ trợ (tối đa 2) để không bỏ sót lớp dữ liệu.
* ``obligations`` — nghĩa vụ bao phủ cụ thể mà LLM phải hiện thực hoá.

Nhờ vậy việc "khi nào dùng kỹ thuật nào" là quy tắc kiểm chứng được, không phụ
thuộc vào việc LLM tự đoán.
"""

from __future__ import annotations

import re
import unicodedata
from collections import Counter
from dataclasses import dataclass, field

from .models import (
    ModulePlan,
    NumericRange,
    TechniqueChoice,
    TechniquePlan,
    WhiteBoxPlan,
)
from .report import module_for

# --------------------------------------------------------------------------
# Thư viện kỹ thuật: nguồn duy nhất cho cả prompt và báo cáo
# --------------------------------------------------------------------------


@dataclass(frozen=True)
class TechniqueSpec:
    name: str
    family: str
    title: str
    use_when: str
    counter: str
    obligations: tuple[str, ...]
    source: str


TECHNIQUE_LIBRARY: dict[str, TechniqueSpec] = {
    'EQUIVALENCE_PARTITIONING': TechniqueSpec(
        name='EQUIVALENCE_PARTITIONING',
        family='BLACK_BOX',
        title='Phân vùng tương đương (EP)',
        use_when=(
            'Đầu vào được mô tả bằng điều kiện/kiểu dữ liệu rời rạc: mỗi điều kiện '
            'tách thành 1 lớp hợp lệ và ít nhất 1 lớp không hợp lệ.'
        ),
        counter='Mỗi lớp cần 1 giá trị đại diện; không sinh mọi giá trị trong lớp.',
        obligations=(
            'Mỗi lớp hợp lệ (V) xuất hiện ít nhất 1 lần.',
            'Mỗi lớp không hợp lệ (X) nằm trong một test case riêng, các biến khác giữ giá trị hợp lệ.',
            'Không trộn 2 giá trị không hợp lệ trong cùng một test case (single-fault assumption).',
        ),
        source='5.Kiemthuhopden.pdf — Phân vùng tương đương',
    ),
    'BOUNDARY_VALUE_ANALYSIS': TechniqueSpec(
        name='BOUNDARY_VALUE_ANALYSIS',
        family='BLACK_BOX',
        title='Phân tích giá trị biên (BVA)',
        use_when=(
            'Có ràng buộc số/độ dài/thời gian với biên rõ ràng (min, max), vì lỗi '
            'tập trung ở sát biên.'
        ),
        counter='2 giá trị: 4n+1 · 3 giá trị: 6n+1 · worst-case: 5^n · robust worst-case: 7^n.',
        obligations=(
            'Mỗi biến có khoảng giá trị: kiểm tra tại biên và ngay ngoài biên.',
            'Với mỗi biến, các biến còn lại giữ giá trị danh nghĩa (nom).',
            'Không gộp 2 giá trị ngoài biên vào cùng một test case.',
        ),
        source='5.Kiemthuhopden.pdf — Phân tích giá trị biên',
    ),
    'DECISION_TABLE': TechniqueSpec(
        name='DECISION_TABLE',
        family='BLACK_BOX',
        title='Bảng quyết định',
        use_when=(
            'Kết quả phụ thuộc tổ hợp nhiều điều kiện (và/hoặc), đặc biệt khi các '
            'tổ hợp cho ra kết quả khác nhau; dùng để loại bỏ tổ hợp dư thừa.'
        ),
        counter='Mỗi cột rule khả thi = 1 test case; gộp các cột cùng kết quả khi có thể.',
        obligations=(
            'Liệt kê đủ cause (điều kiện) và effect (kết quả) trước khi sinh case.',
            'Mỗi rule (cột) khả thi có ít nhất 1 test case; rule không khả thi phải ghi rõ lý do loại.',
            'Ưu tiên rút gọn bảng: các tổ hợp cho cùng kết quả có thể dùng chung test case.',
        ),
        source='5.Kiemthuhopden.pdf — Bảng quyết định',
    ),
    'STATE_TRANSITION': TechniqueSpec(
        name='STATE_TRANSITION',
        family='BLACK_BOX',
        title='Chuyển đổi trạng thái',
        use_when=(
            'Đối tượng có vòng đời hữu hạn (đơn hàng, tài khoản, thanh toán, khiếu nại) '
            'và hành vi phụ thuộc trạng thái hiện tại.'
        ),
        counter='Mỗi chuyển đổi hợp lệ = 1 case; thêm case cho chuyển đổi không hợp lệ quan trọng.',
        obligations=(
            'Liệt kê trạng thái hữu hạn, sự kiện kích hoạt và chuyển đổi hợp lệ (T1..Tn).',
            'Mỗi chuyển đổi hợp lệ có test case; mỗi chuyển đổi không hợp lệ quan trọng có test case riêng.',
            'Kiểm tra precondition/postcondition của từng trạng thái, không chỉ thông báo lỗi.',
        ),
        source='5.Kiemthuhopden.pdf — Chuyển đổi trạng thái',
    ),
    'USE_CASE': TechniqueSpec(
        name='USE_CASE',
        family='BLACK_BOX',
        title='Kiểm thử theo use case',
        use_when=(
            'Có luồng tương tác actor–hệ thống với luồng chính và các luồng ngoại lệ/'
            'thay thế; phù hợp kiểm thử hệ thống, hồi quy, chấp nhận.'
        ),
        counter='1 case cho luồng chính + 1 case cho mỗi luồng ngoại lệ/thay thế.',
        obligations=(
            'Luồng chính (basic flow) có 1 test case end-to-end (F1).',
            'Mỗi luồng ngoại lệ/thay thế (F2..Fn) có test case riêng với dữ liệu gây ra nhánh đó.',
            'Kiểm tra precondition trước luồng và postcondition sau luồng.',
        ),
        source='5.Kiemthuhopden.pdf — Kiểm thử dựa trên use case',
    ),
    'BASIS_PATH': TechniqueSpec(
        name='BASIS_PATH',
        family='WHITE_BOX',
        title='Đường cơ sở (Basis Path / McCabe)',
        use_when=(
            'Có mã nguồn của module và cần đảm bảo mọi đường độc lập đều được thực thi; '
            'V(G) cho biết số test case tối thiểu.'
        ),
        counter='V(G) = E - N + 2p = số nút quyết định + 1.',
        obligations=(
            'Vẽ đồ thị luồng điều khiển và tính V(G) trước khi sinh case.',
            'Số test case tối thiểu bằng V(G); mỗi case đi qua một đường cơ sở độc lập.',
        ),
        source='6-Kiemthuhoptrang.pdf — Basis Path Testing',
    ),
    'STATEMENT_COVERAGE': TechniqueSpec(
        name='STATEMENT_COVERAGE',
        family='WHITE_BOX',
        title='Bao phủ câu lệnh (mức 1)',
        use_when='Cần mức bao phủ tối thiểu: mọi câu lệnh được thực thi ít nhất 1 lần.',
        counter='Không có công thức cố định; V(G) là giới hạn trên số case cần thiết.',
        obligations=('Mỗi câu lệnh trong module được thực thi ít nhất 1 lần.',),
        source='6-Kiemthuhoptrang.pdf — Control-flow/Coverage Testing',
    ),
    'BRANCH_COVERAGE': TechniqueSpec(
        name='BRANCH_COVERAGE',
        family='WHITE_BOX',
        title='Bao phủ nhánh/quyết định (mức 2)',
        use_when='Có điểm quyết định (if/while/for) và cần cả hai phía True/False.',
        counter='Mỗi điểm quyết định cần đủ 2 phía; thực tế hệ thống lớn đạt 75–85%.',
        obligations=('Mỗi điểm quyết định được kiểm tra với cả giá trị True và False.',),
        source='6-Kiemthuhoptrang.pdf — Decision/Branch Coverage',
    ),
    'CONDITION_COVERAGE': TechniqueSpec(
        name='CONDITION_COVERAGE',
        family='WHITE_BOX',
        title='Bao phủ điều kiện (mức 3)',
        use_when='Điều kiện phức hợp có nhiều điều kiện con (a && b, c || d).',
        counter='Mỗi điều kiện con cần đủ True/False; kết hợp nhánh + điều kiện ở mức 4.',
        obligations=('Mỗi điều kiện con trong biểu thức phức hợp được kiểm tra True và False.',),
        source='6-Kiemthuhoptrang.pdf — Condition Coverage',
    ),
    'DATA_FLOW': TechniqueSpec(
        name='DATA_FLOW',
        family='WHITE_BOX',
        title='Kiểm thử dòng dữ liệu',
        use_when=(
            'Cần bắt lỗi vòng đời biến: dùng biến chưa khởi tạo, dùng sau khi huỷ, '
            'định nghĩa không bao giờ dùng.'
        ),
        counter='Mỗi cặp def-use (d…u) là một hạng mục bao phủ (ADUP/AU/AD).',
        obligations=(
            'Mỗi cặp định nghĩa–sử dụng (def-use) của từng biến được phủ ít nhất 1 lần.',
            'Kiểm tra nhánh p-use (dùng trong điều kiện) và c-use (dùng trong tính toán).',
        ),
        source='6-Kiemthuhoptrang.pdf — Data-flow Testing',
    ),
    'NON_FUNCTIONAL_METRIC': TechniqueSpec(
        name='NON_FUNCTIONAL_METRIC',
        family='BLACK_BOX',
        title='Kiểm thử phi chức năng theo ngưỡng',
        use_when=(
            'Yêu cầu là ngưỡng đo được (thời gian phản hồi, tải đồng thời, mã hoá, '
            'dung lượng) chứ không phải logic nghiệp vụ.'
        ),
        counter='1 case cho mỗi ngưỡng; đo và so sánh với giá trị cam kết.',
        obligations=(
            'Nêu rõ ngưỡng cam kết và cách đo (công cụ, thời điểm, tải).',
            'Test case phải ghi giá trị đo được và kết luận Đạt/Không đạt.',
        ),
        source='Ngoài phạm vi 2 tài liệu — bổ sung cho rule NON_FUNCTIONAL',
    ),
}

BVA_FORMULA_HINT = (
    'Standard BVA 2 giá trị = 4n+1; Robust 3 giá trị = 6n+1; '
    'Worst-case = 5^n; Robust worst-case = 7^n (n = số biến có ràng buộc biên).'
)


def render_playbook() -> str:
    """Sinh phần playbook gọn cho system prompt từ thư viện ở trên."""
    lines: list[str] = []
    for spec in TECHNIQUE_LIBRARY.values():
        family = 'hộp đen' if spec.family == 'BLACK_BOX' else 'hộp trắng'
        lines.append(f'### {spec.title} — {spec.name} ({family})')
        lines.append(f'- Khi dùng: {spec.use_when}')
        lines.append(f'- Số lượng: {spec.counter}')
        lines.append('- Nghĩa vụ bao phủ: ' + ' '.join(spec.obligations))
    return '\n'.join(lines)


# --------------------------------------------------------------------------
# Nhận diện tín hiệu
# --------------------------------------------------------------------------

# Chỉ tín hiệu vòng đời thực sự; các từ như "từ chối"/"huỷ bỏ" là hành động
# đơn lẻ nên không tính (tránh module nào cũng thành State Transition).
_STATE_CUES = (
    'trang thai', 'chuyen sang', 'chuyen tu', 'huy don', 'da huy',
    'khoa tai khoan', 'mo khoa', 'het han', 'cho xu ly', 'cho duyet',
    'dang giao', 'dang xu ly', 'hoan thanh', 'da giao',
    'kich hoat', 'tam dung', 'khoi phuc', 'quay lai trang thai',
    'da doc', 'chua doc', 'da thanh toan', 'chua thanh toan',
)
_AUTHZ_CUES = (
    'phan quyen', 'quyen truy cap', 'khong duoc phep', 'chi admin',
    'chi quan tri', 'admin', 'chu tai khoan', 'chua dang nhap', 'vai tro',
    'role', 'nguoi dung thong thuong', 'bi chan', 'khong co quyen',
)
_FLOW_CUES = (
    'dang nhap', 'dang ky', 'dat hang', 'thanh toan', 'gio hang', 'checkout',
    'quy trinh', 'luong chinh', 'luong ngoai le', 'xac nhan don', 'them vao gio',
    'rut tien', 'gui yeu cau', 'quen mat khau',
)
_NFR_CUES = (
    'thoi gian phan hoi', 'hieu nang', 'hieu suat', 'bao mat', 'ma hoa',
    'hash', 'tai dong thoi', 'uptime', 'sla', 'dung luong', 'backup',
    'nhat ky', 'audit log',
)
_ENUM_CUES = (
    'mot trong cac', 'bao gom', 'cac loai', 'loai san pham', 'phuong thuc',
    'hinh thuc', 'vai tro', 'danh muc', 'trang thai', 'kieu', 'dinh dang',
)
_VALIDATION_CUES = (
    'thong bao loi', 'bao loi', 'khong hop le', 'tu choi', 'chan lai',
    'khong the', 'yeu cau nhap lai', 'kiem tra dinh dang', 'bat buoc',
)
_COMBINATION_CUES = (
    'va', 'hoac', 'dong thoi', 'ket hop', 'cung luc', 'neu', 'truong hop',
    'khong the cung',
)


def fold(text: str) -> str:
    """Bỏ dấu tiếng Việt + lowercase để so khớp tín hiệu ổn định."""
    if not text:
        return ''
    text = text.replace('đ', 'd').replace('Đ', 'D')
    text = unicodedata.normalize('NFD', text)
    text = ''.join(ch for ch in text if unicodedata.category(ch) != 'Mn')
    return re.sub(r'\s+', ' ', text).strip().lower()


def _count_cues(text: str, cues: tuple[str, ...]) -> int:
    return sum(1 for cue in cues if cue in text)


_RANGE_RE = re.compile(
    r'(?<![\d.,])(\d{1,6})\s*(?:-|–|—|đến|den|tới|toi)\s*(\d{1,6})(?![\d.,])',
    re.IGNORECASE,
)
_RANGE_LE_RE = re.compile(
    r'(?<![\d.,])(\d{1,6})\s*(?:<=|≤)\s*[^\d\n]{0,25}?(?:<=|≤)\s*(\d{1,6})(?![\d.,])'
)
_MIN_CUE_RE = re.compile(
    r'(?:tối thiểu|toi thieu|ít nhất|it nhat|không dưới|khong duoi|>=|≥)\s*(?:là\s*)?(\d{1,6})',
    re.IGNORECASE,
)
_MAX_CUE_RE = re.compile(
    r'(?:tối đa|toi da|nhiều nhất|nhieu nhat|không vượt quá|khong vuot qua|'
    r'không quá|khong qua|<=|≤)\s*(?:là\s*)?(\d{1,6})',
    re.IGNORECASE,
)

_UNIT_MAP = (
    ('ký tự', 'ký tự'), ('ky tu', 'ký tự'), ('ki tu', 'ký tự'),
    ('giây', 'giây'), ('giay', 'giây'), ('phút', 'phút'), ('phut', 'phút'),
    ('vnd', 'VND'), ('đồng', 'đồng'), ('dong', 'đồng'),
    ('ngày', 'ngày'), ('ngay', 'ngày'),
    ('điểm', 'điểm'), ('diem', 'điểm'),
    ('%', '%'), ('kb', 'KB'), ('mb', 'MB'), ('gb', 'GB'),
)


def _unit_near(text: str, end: int) -> str:
    window = fold(text[end:end + 30])
    for cue, unit in _UNIT_MAP:
        if cue in window:
            return unit
    return ''


def _label_near(text: str, start: int, end: int) -> str:
    """Lấy cụm mô tả quanh ràng buộc, cắt ở ranh giới từ để không bị cụt chữ."""
    window_start = max(0, start - 60)
    left = text[window_start:start]
    left = re.split(r'[.;:\n]', left)[-1].strip()
    if window_start > 0 and ' ' in left:
        left = left.split(' ', 1)[1]  # bỏ từ đầu tiên có thể đã bị cắt
    right = text[end:end + 20]
    if len(right) == 20 and ' ' in right:
        right = right.rsplit(' ', 1)[0]
    label = re.sub(r'\s+', ' ', f'{left} {text[start:end]} {right}').strip()
    if len(label) > 90:
        label = label[-90:]
        if ' ' in label:
            label = label.split(' ', 1)[1]
    return label


def extract_ranges(text: str, rule_id: str) -> list[NumericRange]:
    """Tìm các ràng buộc số/độ dài trong một rule để phục vụ BVA."""
    found: list[NumericRange] = []
    seen: set[tuple[int | None, int | None, str]] = set()

    def add(lo: int | None, hi: int | None, start: int, end: int) -> None:
        if lo is not None and hi is not None:
            if lo > hi:
                lo, hi = hi, lo
            if hi - lo > 1_000_000:
                return
        unit = _unit_near(text, end)
        key = (lo, hi, unit)
        if key in seen:
            return
        seen.add(key)
        found.append(
            NumericRange(
                label=_label_near(text, start, end),
                min=lo,
                max=hi,
                unit=unit,
                rule_ids=[rule_id],
                boundary_points=[],
            )
        )

    for match in _RANGE_RE.finditer(text):
        add(int(match.group(1)), int(match.group(2)), match.start(), match.end())
    for match in _RANGE_LE_RE.finditer(text):
        add(int(match.group(1)), int(match.group(2)), match.start(), match.end())
    for match in _MIN_CUE_RE.finditer(text):
        add(int(match.group(1)), None, match.start(), match.end())
    for match in _MAX_CUE_RE.finditer(text):
        add(None, int(match.group(1)), match.start(), match.end())
    return found


def boundary_points(numeric_range: NumericRange, variant: str) -> list[int]:
    """Sinh các điểm biên cụ thể theo biến thể BVA của tài liệu."""
    lo, hi = numeric_range.min, numeric_range.max
    points: list[int] = []
    if variant == 'WORST_CASE':
        mid = (lo + hi) // 2 if lo is not None and hi is not None else None
        if lo is not None:
            points += [lo, lo + 1]
        if mid is not None:
            points.append(mid)
        if hi is not None:
            points += [hi - 1, hi]
    elif variant == 'ROBUST_WORST_CASE':
        mid = (lo + hi) // 2 if lo is not None and hi is not None else None
        if lo is not None:
            points += [lo - 1, lo, lo + 1]
        if mid is not None:
            points.append(mid)
        if hi is not None:
            points += [hi - 1, hi, hi + 1]
    elif variant == 'THREE_VALUE':
        if lo is not None:
            points += [lo - 1, lo, lo + 1]
        if hi is not None:
            points += [hi - 1, hi, hi + 1]
    else:  # TWO_VALUE
        if lo is not None:
            points += [lo - 1, lo]
        if hi is not None:
            points += [hi, hi + 1]
    if lo is not None and hi is not None:
        points = [p for p in points if p <= hi + 1]
    order: list[int] = []
    for point in sorted(points):
        if point not in order:
            order.append(point)
    return order


def _bva_count(variant: str, n: int) -> int:
    if variant == 'TWO_VALUE':
        return 4 * n + 1
    if variant == 'THREE_VALUE':
        return 6 * n + 1
    if variant == 'WORST_CASE':
        return 5 ** n
    return 7 ** n


# --------------------------------------------------------------------------
# Đặc trưng module + quyết định kỹ thuật
# --------------------------------------------------------------------------


@dataclass
class ModuleFeatures:
    module: str
    rule_count: int = 0
    scenario_count: int = 0
    ranges: list[NumericRange] = field(default_factory=list)
    max_ranges_per_rule: int = 0
    max_conditions: int = 0
    distinct_outcomes: int = 0
    business_rules: int = 0
    nfr_rules: int = 0
    state_scenarios: int = 0
    authz_scenarios: int = 0
    negative_scenarios: int = 0
    state_cues: int = 0
    state_rules: int = 0
    authz_cues: int = 0
    authz_rules: int = 0
    flow_cues: int = 0
    nfr_cues: int = 0
    enum_cues: int = 0
    validation_cues: int = 0
    combination_cues: int = 0
    has_combination_logic: bool = False


def _rule_module(rule: dict) -> str:
    """Chỉ dùng statement/condition để tránh câu action chung chung kéo lệch module."""
    text = ' '.join(
        [str(rule.get('statement', ''))]
        + [str(x) for x in rule.get('conditions') or []]
        + [str(x) for x in rule.get('preconditions') or []]
    )
    return module_for(text)


def _scenario_module(scenario: dict, rule_to_module: dict[str, str]) -> str:
    for rule_id in scenario.get('rule_ids') or []:
        module = rule_to_module.get(str(rule_id))
        if module:
            return module
    text = ' '.join(
        str(scenario.get(key, '')) for key in ('title', 'given', 'when', 'then')
    )
    return module_for(text)


def _build_features(
    module: str,
    rules: list[dict],
    scenarios: list[dict],
) -> ModuleFeatures:
    raw_text = fold(
        ' '.join(
            str(x)
            for r in rules
            for x in (r.get('conditions') or []) + (r.get('preconditions') or [])
        )
    )
    sentence_text = fold(
        ' '.join(
            [str(r.get('statement', '')) for r in rules]
            + [str(r.get('action', '')) for r in rules]
            + [str(r.get('outcome', '')) for r in rules]
        )
    )
    scenario_text = fold(
        ' '.join(str(s.get(key, '')) for s in scenarios for key in ('title', 'given', 'when', 'then'))
    )
    full_text = f'{sentence_text} {raw_text} {scenario_text}'

    ranges: list[NumericRange] = []
    seen_ranges: set[tuple[int | None, int | None, str]] = set()
    max_conditions = 0
    max_ranges_per_rule = 0
    for rule in rules:
        rule_text = ' '.join(
            [str(rule.get('statement', '')), str(rule.get('action', ''))]
            + [str(x) for x in rule.get('conditions') or []]
        )
        rule_ranges = extract_ranges(rule_text, str(rule.get('rule_id', '')))
        max_ranges_per_rule = max(max_ranges_per_rule, len(rule_ranges))
        for numeric_range in rule_ranges:
            key = (numeric_range.min, numeric_range.max, numeric_range.unit)
            if key in seen_ranges:
                continue
            seen_ranges.add(key)
            ranges.append(numeric_range)
        max_conditions = max(max_conditions, len(rule.get('conditions') or []))

    # Chỉ tin vào danh sách `conditions` do analyzer trích xuất; chữ "và/hoặc" trong
    # câu văn tự nhiên không đủ chứng minh có tổ hợp điều kiện (tránh chọn nhầm
    # Decision Table cho module chỉ có một điều kiện).

    outcomes = {fold(str(rule.get('outcome', ''))) for rule in rules if rule.get('outcome')}
    categories = Counter(str(rule.get('category', '')) for rule in rules)
    scenario_types = Counter(str(scenario.get('type', '')) for scenario in scenarios)
    negative_scenarios = sum(
        count for kind, count in scenario_types.items() if 'NEGATIVE' in kind.upper()
    )

    state_rules = sum(
        1 for rule in rules
        if _count_cues(
            fold(' '.join([str(rule.get('statement', ''))] + [str(x) for x in rule.get('conditions') or []])),
            _STATE_CUES,
        )
    )
    authz_rules = sum(
        1 for rule in rules
        if _count_cues(
            fold(' '.join([str(rule.get('statement', ''))] + [str(x) for x in rule.get('conditions') or []])),
            _AUTHZ_CUES,
        )
    )

    features = ModuleFeatures(
        module=module,
        rule_count=len(rules),
        scenario_count=len(scenarios),
        ranges=ranges,
        max_ranges_per_rule=max_ranges_per_rule,
        max_conditions=max_conditions,
        distinct_outcomes=len(outcomes),
        business_rules=categories.get('BUSINESS_RULE', 0),
        nfr_rules=categories.get('NON_FUNCTIONAL', 0),
        state_scenarios=scenario_types.get('STATE', 0),
        authz_scenarios=scenario_types.get('AUTHORIZATION', 0),
        negative_scenarios=negative_scenarios,
        state_cues=_count_cues(full_text, _STATE_CUES),
        state_rules=state_rules,
        authz_cues=_count_cues(full_text, _AUTHZ_CUES),
        authz_rules=authz_rules,
        flow_cues=_count_cues(full_text, _FLOW_CUES),
        nfr_cues=_count_cues(full_text, _NFR_CUES),
        enum_cues=_count_cues(full_text, _ENUM_CUES),
        validation_cues=_count_cues(full_text, _VALIDATION_CUES),
        combination_cues=_count_cues(full_text, _COMBINATION_CUES),
    )
    features.has_combination_logic = (
        features.max_conditions >= 2
        and features.distinct_outcomes >= 2
        and features.business_rules >= 1
    )
    return features


def _ep_obligations(variant: str) -> list[str]:
    obligations = [TECHNIQUE_LIBRARY['EQUIVALENCE_PARTITIONING'].obligations[0]]
    if variant in ('WEAK_ROBUST', 'STRONG_ROBUST'):
        obligations.append(TECHNIQUE_LIBRARY['EQUIVALENCE_PARTITIONING'].obligations[1])
        obligations.append(TECHNIQUE_LIBRARY['EQUIVALENCE_PARTITIONING'].obligations[2])
    if variant == 'STRONG_NORMAL':
        obligations.append('Lấy tích Descartes các lớp hợp lệ của mọi biến (multiple-fault assumption).')
    if variant == 'STRONG_ROBUST':
        obligations.append('Lấy tích Descartes toàn bộ lớp hợp lệ + không hợp lệ; số case rất lớn, chỉ dùng khi có công cụ.')
    return obligations


def _bva_obligations(variant: str, ranges: list[NumericRange]) -> list[str]:
    obligations: list[str] = []
    for numeric_range in ranges:
        points = ', '.join(str(p) for p in boundary_points(numeric_range, variant))
        lo = '−∞' if numeric_range.min is None else str(numeric_range.min)
        hi = '+∞' if numeric_range.max is None else str(numeric_range.max)
        unit = f' {numeric_range.unit}' if numeric_range.unit else ''
        obligations.append(f'Ràng buộc [{lo}, {hi}]{unit}: kiểm tra các giá trị {points}.')
    obligations.append('Với mỗi biến, các biến còn lại giữ giá trị danh nghĩa (nom).')
    if variant in ('WORST_CASE', 'ROBUST_WORST_CASE'):
        obligations.append('Lấy tích Descartes các điểm biên của mọi biến (multiple-fault assumption).')
    return obligations


def _choice(
    technique: str,
    role: str,
    rationale: str,
    variant: str | None = None,
    obligations: list[str] | None = None,
    estimated_cases: int = 1,
) -> TechniqueChoice:
    spec = TECHNIQUE_LIBRARY[technique]
    return TechniqueChoice(
        technique=technique,  # type: ignore[arg-type]
        variant=variant,
        family=spec.family,  # type: ignore[arg-type]
        role=role,  # type: ignore[arg-type]
        rationale=rationale,
        obligations=obligations or list(spec.obligations),
        estimated_cases=max(0, estimated_cases),
    )


def _bva_variables(features: ModuleFeatures) -> int:
    """Số biến có ràng buộc biên: ưu tiên số ràng buộc trong cùng một rule.

    Một rule như "1 ≤ a, b, c ≤ 200" có 3 biến dù biên trùng nhau, nên không thể
    chỉ đếm các ràng buộc đã khử trùng lặp.
    """
    return max(1, len(features.ranges), features.max_ranges_per_rule)


def _bva_variant(features: ModuleFeatures) -> tuple[str, list[str]]:
    warnings: list[str] = []
    n = _bva_variables(features)
    if n >= 2 and (features.max_ranges_per_rule >= 2 or features.has_combination_logic):
        robust = features.validation_cues >= 2 or features.negative_scenarios >= 2
        variant = 'ROBUST_WORST_CASE' if robust else 'WORST_CASE'
        base = 7 if robust else 5
        if base ** n <= 60:
            return variant, warnings
        warnings.append(
            f'BVA {variant} cần {base}^{n} = {base ** n} test case (>60) — tự động hạ '
            'xuống 3 giá trị (6n+1) để giữ bộ case khả thi; nếu vẫn cần phủ tương tác '
            'biến, dùng công cụ sinh tổ hợp.'
        )
        return 'THREE_VALUE', warnings
    if features.validation_cues >= 2 or features.negative_scenarios >= 2:
        return 'THREE_VALUE', warnings
    return 'TWO_VALUE', warnings


def _dominant(rules_with_signal: int, rule_count: int) -> bool:
    """Tín hiệu chỉ được coi là đặc trưng module khi xuất hiện ở ≥ 50% số rule."""
    return rules_with_signal * 2 >= rule_count > 0


def select_techniques(features: ModuleFeatures) -> tuple[list[TechniqueChoice], list[str]]:
    """Chọn kỹ thuật chính + bổ trợ theo thang quyết định có thể kiểm chứng."""
    warnings: list[str] = []
    choices: list[TechniqueChoice] = []
    n_ranges = len(features.ranges)
    rule_estimate = max(2, min(40, 2 * features.rule_count))
    state_is_core = features.state_scenarios >= 1 or (
        features.state_cues >= 3 and _dominant(features.state_rules, features.rule_count)
    )
    authz_is_core = features.authz_scenarios >= 1 or (
        features.authz_cues >= 3 and _dominant(features.authz_rules, features.rule_count)
    )

    # R1 — Yêu cầu phi chức năng chiếm toàn bộ module.
    if features.nfr_rules and features.nfr_rules == features.rule_count:
        choices.append(
            _choice(
                'NON_FUNCTIONAL_METRIC',
                'PRIMARY',
                f'Cả {features.nfr_rules} rule của module là NON_FUNCTIONAL '
                f'({features.nfr_cues} tín hiệu hiệu năng/bảo mật) → kiểm thử theo ngưỡng thay vì '
                'kỹ thuật dữ liệu.',
                estimated_cases=max(1, features.nfr_rules),
            )
        )
        return choices, warnings

    # R2 — Phân quyền: bảng quyết định role × hành động (ma trận quyền).
    if authz_is_core and features.authz_cues >= 2:
        choices.append(
            _choice(
                'DECISION_TABLE',
                'PRIMARY',
                (
                    f'{features.authz_scenarios} scenario AUTHORIZATION'
                    if features.authz_scenarios
                    else f'{features.authz_cues} tín hiệu quyền/vai trò ở '
                         f'{features.authz_rules}/{features.rule_count} rule'
                )
                + ' → lập bảng quyết định (vai trò × hành động) thay vì liệt kê từng màn hình.',
                estimated_cases=max(2, min(16, 2 ** min(4, features.authz_cues))),
            )
        )
    # R3 — Vòng đời trạng thái.
    elif state_is_core:
        signal = (
            f'{features.state_scenarios} scenario kiểu STATE'
            if features.state_scenarios
            else f'{features.state_cues} tín hiệu trạng thái ở {features.state_rules}/'
                 f'{features.rule_count} rule'
        )
        choices.append(
            _choice(
                'STATE_TRANSITION',
                'PRIMARY',
                f'{signal} → hành vi phụ thuộc trạng thái hiện tại, phải phủ chuyển đổi '
                'hợp lệ và chuyển đổi bị chặn (5.Kiemthuhopden: State Transition).',
                estimated_cases=max(3, min(30, features.rule_count + features.state_cues)),
            )
        )
    # R4 — Tổ hợp điều kiện nghiệp vụ.
    elif features.has_combination_logic:
        choices.append(
            _choice(
                'DECISION_TABLE',
                'PRIMARY',
                f'{features.max_conditions} điều kiện, {features.distinct_outcomes} kết quả và '
                f'{features.combination_cues} cụm và/hoặc trong cùng module → kết quả phụ thuộc '
                'tổ hợp điều kiện; bảng quyết định giúp loại tổ hợp dư thừa '
                '(5.Kiemthuhopden: Decision Table).',
                estimated_cases=max(2, min(24, 2 ** min(5, features.max_conditions))),
            )
        )
    # R5 — Ràng buộc số/độ dài.
    elif n_ranges:
        variant, bva_warnings = _bva_variant(features)
        warnings += bva_warnings
        choices.append(
            _choice(
                'BOUNDARY_VALUE_ANALYSIS',
                'PRIMARY',
                f'Tìm thấy {n_ranges} ràng buộc số/độ dài có biên xác định → lỗi tập trung ở '
                f'sát biên, dùng BVA biến thể {variant} (5.Kiemthuhopden: BVA).',
                variant=variant,
                obligations=_bva_obligations(variant, features.ranges),
                estimated_cases=min(400, _bva_count(variant, _bva_variables(features))),
            )
        )
    # R6 — Luồng use case.
    elif features.flow_cues >= 2 and features.scenario_count >= 2:
        choices.append(
            _choice(
                'USE_CASE',
                'PRIMARY',
                f'Module mô tả {features.flow_cues} bước/luồng tương tác với '
                f'{features.scenario_count} scenario → kiểm thử theo use case, phủ luồng chính '
                'và luồng ngoại lệ (5.Kiemthuhopden: Use case testing).',
                estimated_cases=max(2, min(20, 1 + features.flow_cues)),
            )
        )
    # R7 — Mặc định: phân vùng tương đương.
    else:
        variant = (
            'WEAK_ROBUST'
            if features.validation_cues or features.negative_scenarios
            else 'WEAK_NORMAL'
        )
        if variant == 'WEAK_ROBUST':
            reason = 'có tín hiệu kiểm tra/báo lỗi đầu vào nên cần cả lớp không hợp lệ'
        elif features.enum_cues:
            reason = (
                f'có {features.enum_cues} tín hiệu liệt kê/định dạng nhưng không có biên số, '
                'tổ hợp điều kiện hay vòng đời trạng thái; chia lớp là phù hợp nhất'
            )
        else:
            reason = (
                'chưa có biên số, tổ hợp điều kiện hay vòng đời trạng thái rõ ràng; '
                'chia lớp hợp lệ/không hợp lệ là mức tối thiểu'
            )
        choices.append(
            _choice(
                'EQUIVALENCE_PARTITIONING',
                'PRIMARY',
                f'Không có tín hiệu đặc thù hơn — {reason} (5.Kiemthuhopden: EP).',
                variant=variant,
                obligations=_ep_obligations(variant),
                estimated_cases=max(2, min(40, rule_estimate)),
            )
        )

    primary = choices[0]
    supporting: list[TechniqueChoice] = []

    # EP luôn là lưới an toàn: đảm bảo mỗi lớp dữ liệu xuất hiện ít nhất một lần.
    if primary.technique != 'EQUIVALENCE_PARTITIONING' and primary.technique != 'NON_FUNCTIONAL_METRIC':
        variant = 'WEAK_ROBUST' if (features.ranges or features.validation_cues) else 'WEAK_NORMAL'
        supporting.append(
            _choice(
                'EQUIVALENCE_PARTITIONING',
                'SUPPORTING',
                'Bổ trợ: phân vùng tương đương cho từng trường dữ liệu để không bỏ sót lớp '
                'hợp lệ/không hợp lệ khi kỹ thuật chính chỉ tập trung vào biên hoặc tổ hợp.',
                variant=variant,
                obligations=_ep_obligations(variant),
                estimated_cases=max(2, min(30, rule_estimate)),
            )
        )

    if primary.technique != 'BOUNDARY_VALUE_ANALYSIS' and features.ranges:
        # Bổ trợ chỉ cần mức vừa phải: tránh 5^n/nhân bản khi kỹ thuật chính đã phủ tổ hợp.
        variant = (
            'THREE_VALUE'
            if features.validation_cues >= 2 or features.negative_scenarios >= 2
            else 'TWO_VALUE'
        )
        supporting.append(
            _choice(
                'BOUNDARY_VALUE_ANALYSIS',
                'SUPPORTING',
                f'Bổ trợ: {len(features.ranges)} ràng buộc số/độ dài nằm trong module — '
                'kỹ thuật chính không kiểm tra sát biên nên cần BVA bổ sung.',
                variant=variant,
                obligations=_bva_obligations(variant, features.ranges),
                estimated_cases=min(400, _bva_count(variant, _bva_variables(features))),
            )
        )

    # NFR được ưu tiên hơn USE_CASE trong danh sách bổ trợ: rule phi chức năng
    # không thể phủ bằng EP/BVA/DT, còn luồng use case thường đã có case end-to-end.
    if features.nfr_rules and len(supporting) < 3:
        supporting.append(
            _choice(
                'NON_FUNCTIONAL_METRIC',
                'SUPPORTING',
                f'Bổ trợ: module còn {features.nfr_rules} rule phi chức năng cần case đo ngưỡng '
                'riêng, không gộp vào case nghiệp vụ.',
                estimated_cases=max(1, features.nfr_rules),
            )
        )

    if primary.technique != 'USE_CASE' and features.flow_cues >= 2 and len(supporting) < 3:
        supporting.append(
            _choice(
                'USE_CASE',
                'SUPPORTING',
                'Bổ trợ: có luồng tương tác nhiều bước, cần thêm case end-to-end cho luồng '
                'chính và luồng ngoại lệ.',
                estimated_cases=max(2, min(12, 1 + features.flow_cues)),
            )
        )

    if (
        primary.technique != 'STATE_TRANSITION'
        and state_is_core
        and len(supporting) < 3
    ):
        supporting.append(
            _choice(
                'STATE_TRANSITION',
                'SUPPORTING',
                'Bổ trợ: module còn mô tả vòng đời trạng thái; kỹ thuật chính không kiểm tra '
                'việc chặn chuyển đổi sai trạng thái.',
                estimated_cases=max(2, min(20, features.rule_count + features.state_cues)),
            )
        )

    final_choices = choices + supporting[:3]
    # Điểm biên phải theo đúng biến thể BVA được chọn (kể cả khi BVA là bổ trợ).
    bva_choice = next(
        (choice for choice in final_choices if choice.technique == 'BOUNDARY_VALUE_ANALYSIS'),
        None,
    )
    if bva_choice:
        for numeric_range in features.ranges:
            numeric_range.boundary_points = boundary_points(
                numeric_range, bva_choice.variant or 'TWO_VALUE'
            )

    return final_choices, warnings


def _story(features: ModuleFeatures, techniques: list[TechniqueChoice]) -> str:
    primary = techniques[0]
    parts = [
        f'{features.rule_count} rule / {features.scenario_count} scenario.',
        f'Kỹ thuật chính: {TECHNIQUE_LIBRARY[primary.technique].title}'
        + (f' ({primary.variant})' if primary.variant else '') + '.',
    ]
    if features.ranges:
        parts.append(f'{len(features.ranges)} ràng buộc biên.')
    if features.max_conditions >= 2:
        parts.append(f'tối đa {features.max_conditions} điều kiện/rule.')
    if len(techniques) > 1:
        extra = ', '.join(TECHNIQUE_LIBRARY[t.technique].title for t in techniques[1:])
        parts.append(f'Bổ trợ: {extra}.')
    return ' '.join(parts)


def build_module_plans(analysis: dict) -> list[ModulePlan]:
    """Nhóm rule/scenario theo module và chọn kỹ thuật cho từng module."""
    rules = analysis.get('rules') or []
    scenarios = analysis.get('scenarios') or []

    rule_to_module: dict[str, str] = {}
    grouped_rules: dict[str, list[dict]] = {}
    grouped_scenarios: dict[str, list[dict]] = {}

    for rule in rules:
        module = _rule_module(rule)
        rule_id = str(rule.get('rule_id', ''))
        if rule_id:
            rule_to_module[rule_id] = module
        grouped_rules.setdefault(module, []).append(rule)

    for scenario in scenarios:
        module = _scenario_module(scenario, rule_to_module)
        grouped_scenarios.setdefault(module, []).append(scenario)

    module_names = list(grouped_rules.keys())
    for module in grouped_scenarios:
        if module not in grouped_rules:
            module_names.append(module)

    plans: list[ModulePlan] = []
    for module in module_names:
        module_rules = grouped_rules.get(module, [])
        module_scenarios = grouped_scenarios.get(module, [])
        features = _build_features(module, module_rules, module_scenarios)
        techniques, warnings = select_techniques(features)
        plans.append(
            ModulePlan(
                module=module,
                rule_ids=[str(r.get('rule_id', '')) for r in module_rules if r.get('rule_id')],
                scenario_ids=[
                    str(s.get('scenario_id', '')) for s in module_scenarios if s.get('scenario_id')
                ],
                techniques=techniques,
                numeric_ranges=features.ranges,
                story=_story(features, techniques),
                warnings=warnings,
            )
        )
    return plans


def build_technique_plan(
    analysis_data: dict,
    white_box: list[WhiteBoxPlan] | None = None,
) -> TechniquePlan:
    """Tạo kế hoạch kỹ thuật đầy đủ từ analysis.json (+ kết quả phân tích mã nguồn)."""
    analysis = analysis_data.get('analysis', analysis_data) or {}
    modules = build_module_plans(analysis)

    counts = Counter(plan.techniques[0].technique for plan in modules)
    summary_parts = [
        f'{TECHNIQUE_LIBRARY[name].title}: {count}' for name, count in counts.most_common()
    ]
    summary = f'{len(modules)} module — ' + '; '.join(summary_parts) if modules else '0 module'
    if white_box:
        summary += f'. Hộp trắng: {len(white_box)} hàm phân tích từ mã nguồn.'

    return TechniquePlan(summary=summary, modules=modules, white_box=white_box or [])


# --------------------------------------------------------------------------
# Hỗ trợ validation / report
# --------------------------------------------------------------------------

COVERAGE_TAG_RE = re.compile(r'^[A-Z]{1,3}\d+$')


def is_invalid_coverage_tag(tag: str) -> bool:
    """Tag lớp không hợp lệ/biên không hợp lệ theo quy ước V/X/B/D của tài liệu.

    Chỉ Xn (invalid class) và Dn (invalid boundary) bị tính; DFn (data-flow)
    của kiểm thử hộp trắng không bị nhầm là biên không hợp lệ.
    """
    return bool(re.fullmatch(r'[XD]\d+', tag.strip().upper()))


def module_map(plan: TechniquePlan | None) -> dict[str, ModulePlan]:
    if not plan:
        return {}
    return {module.module: module for module in plan.modules}
