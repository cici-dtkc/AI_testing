"""Parse the explicit scope table, not historical or strategy references."""
import re, unicodedata
from pathlib import Path
from pydantic import Field
from .models import StrictModel, Document, Ref
from .parser import parse

def norm(text):
    text=unicodedata.normalize('NFD',text.lower()).replace('đ','d')
    return ''.join(c for c in text if unicodedata.category(c)!='Mn')

class PlanItem(StrictModel):
    item_id: str
    kind: str
    requirement_id: str
    title: str
    outline: str
    estimated_test_cases: int | None
    note: str
    module: str | None
    source_refs: list[Ref]

class PlanConflict(StrictModel):
    code: str
    description: str
    source_refs: list[Ref]

class Plan(StrictModel):
    document: Document
    scope_authority: str = 'section_2.1'
    scope_notice: str = 'Mục 2.1 là baseline lọc do cấu hình; không phải quyết định phê duyệt kế hoạch.'
    items: list[PlanItem]
    conflicts: list[PlanConflict] = Field(default_factory=list)

def load_plan(path:Path)->Plan:
    doc=parse(path,'TEST-PLAN','file-baseline')
    doc.blocks=[b.model_copy(update={'block_id':'TP-'+b.block_id,'module':None,'use_case':None}) for b in doc.blocks]
    items=[];active=False
    for block in doc.blocks:
        # Ignore the auto TOC, require an exact heading at body level.
        if re.fullmatch(r'2\.1\s+.+',block.text) and 'hang muc kiem thu' in norm(block.text) and '|' not in block.text:
            active=True;continue
        if active and re.match(r'^2\.2\s',block.text):break
        if not active:continue
        for line in block.text.splitlines():
            cells=[x.strip() for x in line.split('|')]
            if len(cells)<6 or not re.fullmatch(r'\d+',cells[0]) or cells[1] not in ('FR','NFR','PR'):continue
            rid=cells[2]
            if not re.fullmatch(r'(?:FR-M\d{2}(?:-\d+)?|NFR-[A-Za-z0-9-]+|PR-[A-Za-z0-9-]+)',rid):
                raise ValueError('Mã yêu cầu không hợp lệ trong bảng 2.1: '+rid)
            module=re.search(r'\bM\d{2}\b',rid)
            items.append(PlanItem(item_id='TP-'+rid,kind=cells[1],requirement_id=rid,title=cells[3],outline=cells[4],estimated_test_cases=int(cells[5]) if cells[5].isdigit() else None,note=' | '.join(cells[6:]),module=module.group() if module else None,source_refs=[Ref(block_id=block.block_id,quote=line)]))
    if not items:raise ValueError('Không tìm thấy bảng phạm vi mục 2.1 đúng định dạng. Không tự suy đoán từ toàn văn Test Plan.')
    if len({i.item_id for i in items})!=len(items):raise ValueError('Mã hạng mục Test Plan trùng; cần sửa baseline trước khi lọc.')
    plan=Plan(document=doc,items=items)
    def conflict(code,description,matching):
        refs=[Ref(block_id=b.block_id,quote=b.text) for b in matching[:3]]
        if refs:plan.conflicts.append(PlanConflict(code=code,description=description,source_refs=refs))
    broad=[b for b in doc.blocks if re.search(r'M01\s*[–—-]\s*M20|M01 đến M20|M01 den M20',b.text)]
    if len({i.module for i in items if i.module})<20:
        conflict('BROAD_SCOPE_VS_TABLE','Mục 2.1 đã thu hẹp nhưng các đoạn khác còn nhắc M01–M20. Lọc theo 2.1; phạm vi rộng hơn cần reviewer xác nhận.',broad)
    excel=[b for b in doc.blocks if 'excel' in norm(b.text) and ('bug' in norm(b.text) or 'loi' in norm(b.text))]
    jira=[b for b in doc.blocks if 'jira' in norm(b.text) and ('defect' in norm(b.text) or 'loi' in norm(b.text))]
    if excel and jira:conflict('DEFECT_TOOL_MISMATCH','Có tham chiếu Excel và Jira/DMS cho quản lý lỗi; cần xác nhận công cụ chính thức.',[excel[0],jira[0]])
    narrow=[b for b in doc.blocks if 'chrome' in norm(b.text) and 'edge' in norm(b.text) and 'firefox' not in norm(b.text)]
    wide=[b for b in doc.blocks if 'firefox' in norm(b.text) and 'safari' in norm(b.text)]
    if narrow and wide:conflict('BROWSER_SCOPE_MISMATCH','Bảng nêu Chrome/Edge nhưng môi trường còn Firefox/Safari. Không tự mở rộng phạm vi NFR-05.',[narrow[0],wide[0]])
    placeholders=[b for b in doc.blocks if '<Tên>' in b.text or '<Ngày>' in b.text]
    conflict('APPROVAL_NOT_EVIDENCED','Trang phê duyệt còn placeholder; không suy ra Test Plan đã được ký duyệt.',placeholders)
    return plan
