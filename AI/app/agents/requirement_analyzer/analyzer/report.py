import html,json
from pathlib import Path

def write_report(payload:dict,folder:Path):
    folder.mkdir(parents=True,exist_ok=True)
    (folder/'analysis.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding='utf-8')
    a=payload['analysis'];e=lambda s:html.escape(str(s))
    def cards(items):return ''.join('<pre>'+e(json.dumps(x,ensure_ascii=False,indent=2))+'</pre>' for x in items) or '<p>Không có.</p>'
    body=f'<h1>Requirement Analyzer</h1><p>Provider: {e(payload["provider"])} | Model: {e(payload["model"])} | REVIEW_REQUIRED</p><p>{e(a["summary"])}</p>'
    for key,label in [('rules','Business rules'),('scenarios','Test scenarios'),('issues','Issues cần xác nhận')]:body+=f'<h2>{label} ({len(a[key])})</h2>'+cards(a[key])
    body+='<h2>Pipeline</h2>'+cards([payload.get('pipeline_stats',{})])
    modules={b['block_id']:b.get('module') or 'GENERAL' for b in payload['source_document']['blocks']}
    groups={}
    for rule in a['rules']:
        module=modules.get(rule['source_refs'][0]['block_id'],'GENERAL')
        groups.setdefault(module,[]).append(rule)
    for module,rules in groups.items():body+=f'<h2>Module {e(module)}</h2>'+cards(rules)
    body+='<h2>Source blocks</h2>'+cards(payload['source_document']['blocks'])
    body+='<h2>Validation</h2><p>Schema và liên kết hợp lệ. Ý nghĩa nghiệp vụ vẫn cần review.</p>'+cards(payload['validation_warnings'])
    (folder/'report.html').write_text('<!doctype html><html lang="vi"><meta charset="utf-8"><title>Requirement Analysis</title><style>body{font:16px system-ui;max-width:1000px;margin:40px auto;padding:0 20px;color:#17243a}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#f2f6fa;padding:16px;border-radius:8px}h2{margin-top:32px}</style>'+body+'</html>',encoding='utf-8')
