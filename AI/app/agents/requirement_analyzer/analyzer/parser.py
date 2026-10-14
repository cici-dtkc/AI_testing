import hashlib, re
from pathlib import Path
from .models import Document, Block
MAX_BYTES=10*1024*1024
MAX_CHARS=1000000

def parse(path:Path, document_id:str, version:str)->Document:
    if not path.is_file(): raise ValueError('Không tìm thấy file đầu vào.')
    if path.stat().st_size>MAX_BYTES: raise ValueError('File vượt giới hạn 10 MB.')
    raw=path.read_bytes(); parts=[]
    if path.suffix.lower() in ('.txt','.md'):
        parts=[(f'paragraph:{i}',text.strip()) for i,text in enumerate(raw.decode('utf-8-sig').split('\n\n'),1) if text.strip()]
    elif path.suffix.lower()=='.docx':
        from docx import Document as WordDocument
        from docx.oxml.ns import qn
        from docx.text.paragraph import Paragraph
        from docx.table import Table
        d=WordDocument(path)
        # Preserve paragraph/table order in the body. Table stays whole to retain column context.
        for i,el in enumerate(d.element.body,1):
            if el.tag==qn('w:p'):
                text=Paragraph(el,d).text
            elif el.tag==qn('w:tbl'):
                text='\n'.join(' | '.join(c.text for c in row.cells) for row in Table(el,d).rows)
            else: continue
            if text.strip():parts.append((f'body:{i}',text.strip()))
    elif path.suffix.lower()=='.pdf':
        from pypdf import PdfReader
        for i,page in enumerate(PdfReader(path).pages,1):
            text=(page.extract_text() or '').strip()
            if not text: raise ValueError(f'PDF trang {i} không có text; hãy OCR trước, không bỏ trang.')
            parts.append((f'page:{i}',text))
    else: raise ValueError('Chỉ hỗ trợ .txt, .md, .docx và PDF có text.')
    if not parts: raise ValueError('Tài liệu không có nội dung.')
    if sum(len(x[1]) for x in parts)>MAX_CHARS:raise ValueError('Vượt giới hạn 1000000 ký tự.')
    blocks=[];module=None
    for i,(location,text) in enumerate(parts,1):
        match=re.match(r'^3\.\d+\s+M(\d{2})\b',text)
        if match:module='M'+match.group(1)
        elif re.match(r'^[456]\.\s',text):module=None
        uc=re.search(r'Use Case ID\s*\|\s*(UC-\d+)',text)
        blocks.append(Block(block_id=f'B{i:04}',location=location,text=text,module=module,use_case=uc.group(1) if uc else None))
    return Document(document_id=document_id,version=version,sha256=hashlib.sha256(raw).hexdigest(),blocks=blocks)
