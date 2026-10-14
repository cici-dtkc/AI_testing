import argparse,json,os,sys
from pathlib import Path
from datetime import datetime,timezone
from dotenv import load_dotenv
from analyzer.parser import parse
from analyzer.models import Analysis
from analyzer.validation import validate
from analyzer.report import write_report
from analyzer.prompt import PROMPT_VERSION
ROOT=Path(__file__).resolve().parent

def main():
    load_dotenv(ROOT/'.env')
    p=argparse.ArgumentParser(description='Một Requirement Analyzer Agent dùng Gemini')
    p.add_argument('--input',type=Path,default=ROOT/'examples/coupon_srs.txt')
    p.add_argument('--document-id',default='SRS-ECOMMERCE')
    p.add_argument('--version',default='1.0')
    p.add_argument('--output',type=Path,default=ROOT/'outputs/latest')
    p.add_argument('--model',default=os.getenv('GEMINI_MODEL','gemini-flash-latest'))
    p.add_argument('--chunk-chars',type=int,default=12000)
    p.add_argument('--cross-chars',type=int,default=26000)
    p.add_argument('--resume',action='store_true')
    p.add_argument('--skip-cross',action='store_true')
    p.add_argument('--parse-only',action='store_true',help='Xuất source blocks và chunk plan, không gọi Gemini')
    p.add_argument('--demo',action='store_true',help='Fixture minh họa, KHÔNG gọi Gemini; chỉ dùng với coupon_srs.txt đi kèm')
    args=p.parse_args()
    try:
        doc=parse(args.input,args.document_id,args.version)
        if args.parse_only:
            from analyzer.chunking import split_document
            batches=split_document(doc,args.chunk_chars)
            args.output.mkdir(parents=True,exist_ok=True)
            (args.output/'source_blocks.json').write_text(doc.model_dump_json(indent=2),encoding='utf-8')
            (args.output/'chunk_plan.json').write_text(json.dumps([{'module':b['module'],'block_ids':[x.block_id for x in b['blocks']]} for b in batches],indent=2),encoding='utf-8')
            print(f'Parsed {len(doc.blocks)} blocks, {len(batches)} chunks. Không gọi Gemini.');return 0
        stats={}
        if args.demo:
            expected=parse(ROOT/'examples/coupon_srs.txt',args.document_id,args.version)
            if doc.sha256!=expected.sha256:raise ValueError('--demo chỉ dùng với sample coupon không chỉnh sửa.')
            analysis=Analysis.model_validate_json((ROOT/'examples/demo_analysis.json').read_text(encoding='utf-8'))
        else:
            from analyzer.pipeline import analyze_full
            key=os.getenv('GEMINI_API_KEY','')
            if not key or key=='your_gemini_api_key_here':raise ValueError('Điền GEMINI_API_KEY trong .env trước khi chạy.')
            analysis,warnings,stats=analyze_full(doc,key,args.model,args.output,args.chunk_chars,args.resume,args.skip_cross,args.cross_chars)
        if args.demo:warnings=validate(analysis,doc)
        payload={'provider':'DEMO_FIXTURE' if args.demo else 'GEMINI','model':'none' if args.demo else args.model,'created_at':datetime.now(timezone.utc).isoformat(),'prompt_version':PROMPT_VERSION,'review_status':'REVIEW_REQUIRED','source_document':doc.model_dump(),'analysis':analysis.model_dump(),'validation_warnings':warnings,'pipeline_stats':stats}
        write_report(payload,args.output)
        print(f'Rules: {len(analysis.rules)} | Scenarios: {len(analysis.scenarios)} | Issues: {len(analysis.issues)}')
        print(f'JSON: {(args.output/"analysis.json").resolve()}')
        print(f'HTML: {(args.output/"report.html").resolve()}')
        return 0
    except (ValueError,RuntimeError,OSError) as exc:
        print(f'Lỗi: {exc}',file=sys.stderr);return 1
if __name__=='__main__':raise SystemExit(main())
