import hashlib,json
from pathlib import Path
from .chunking import split_document,context_document
from .models import Analysis,Document
from .merge import merge_analyses
from .validation import validate
from .prompt import PROMPT_VERSION

def analyze_full(document:Document,key:str,model:str,output:Path,chunk_chars:int=12000,resume:bool=False,skip_cross:bool=False,cross_chars:int=26000,generator=None)->tuple[Analysis,list[str],dict]:
    from .gemini import generate
    generator=generator or generate
    batches=split_document(document,chunk_chars);parts=[];warnings=[]
    checkpoint=output/'checkpoints';checkpoint.mkdir(parents=True,exist_ok=True)
    for n,batch in enumerate(batches,1):
        subset=context_document(document,batch);target_ids=[b.block_id for b in batch['blocks']]
        signature=hashlib.sha256(json.dumps({'input':subset.model_dump(),'targets':target_ids,'model':model,'prompt':PROMPT_VERSION},sort_keys=True,ensure_ascii=False).encode()).hexdigest()
        cache=checkpoint/f'{signature}.json'
        print(f'Nhóm {n}/{len(batches)}: {batch["module"]}, {len(target_ids)} blocks',flush=True)
        if resume and cache.exists():a=Analysis.model_validate_json(cache.read_text(encoding='utf-8'))
        else:a=generator(subset,key,model,target_ids)
        validate(a,subset)
        # Reject rules grounded only in background context.
        for rule in a.rules:
            if not any(ref.block_id in target_ids for ref in rule.source_refs):raise ValueError('Rule không tham chiếu target block.')
        cache.write_text(a.model_dump_json(indent=2),encoding='utf-8')
        parts.append(a)
    result=merge_analyses(parts)
    if not skip_cross:
        from .cross_review import review
        result.issues.extend(review(document,key,model,cross_chars))
    else:warnings.append('Đã bỏ cross-review; cần đối chiếu liên module thủ công.')
    warnings.extend(validate(result,document))
    covered=[b.block_id for batch in batches for b in batch['blocks']]
    if len(covered)!=len(set(covered)) or set(covered)!={b.block_id for b in document.blocks}:raise ValueError('Lỗi coverage source blocks.')
    stats={'chunks':len(batches),'source_blocks':len(document.blocks),'submitted_target_blocks':len(covered),'input_chars':sum(len(b.text) for b in document.blocks),'cross_review':not skip_cross}
    return result,warnings,stats
