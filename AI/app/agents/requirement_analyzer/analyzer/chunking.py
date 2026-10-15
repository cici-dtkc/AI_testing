from collections import OrderedDict
from .models import Document

def split_document(document:Document,max_chars:int=12000)->list[dict]:
    if max_chars<2000:raise ValueError('Chunk size tối thiểu 2000 ký tự.')
    groups=OrderedDict()
    for b in document.blocks:groups.setdefault(b.module or 'GENERAL',[]).append(b)
    # Preserve each block/table. Never silently cut a use case in half.
    batches=[]
    for module,blocks in groups.items():
        current=[];size=0
        for b in blocks:
            if len(b.text)>max_chars:raise ValueError(f'{b.block_id} dài hơn chunk size; tăng --chunk-chars, không cắt bảng UC.')
            if current and size+len(b.text)>max_chars:
                batches.append({'module':module,'blocks':current});current=[];size=0
            current.append(b);size+=len(b.text)
        if current:batches.append({'module':module,'blocks':current})
    return batches

def context_document(document:Document,batch:dict)->Document:
    targets=batch['blocks'];ids={b.block_id for b in targets}
    # Bounded global scope/assumptions. All target blocks are analyzed once.
    context=[];size=0
    for b in document.blocks:
        if b.block_id not in ids and b.module is None and any(word in b.text.lower() for word in ('phạm vi','ngoài dự án','giả định','m21','không thuộc')):
            if size+len(b.text)<=4000:context.append(b);size+=len(b.text)
    return document.model_copy(update={'blocks':targets+context})
