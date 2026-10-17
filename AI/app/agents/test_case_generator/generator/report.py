import html, json
from pathlib import Path

def write_test_report(payload: dict, folder: Path):
    folder.mkdir(parents=True, exist_ok=True)
    (folder / 'test_cases.json').write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
    
    suite = payload['test_suite']
    e = lambda s: html.escape(str(s))
    def cards(items): return ''.join('<pre>' + e(json.dumps(x, ensure_ascii=False, indent=2)) + '</pre>' for x in items) or '<p>Không có.</p>'
    
    body = f'<h1>Test Case Generator Report</h1><p>Provider: {e(payload["provider"])} | Model: {e(payload["model"])}</p><p>{e(suite["summary"])}</p>'
    body += f'<h2>Danh sách Test Cases ({len(suite["test_cases"])})</h2>' + cards(suite['test_cases'])
    body += '<h2>Validation Warnings</h2>' + cards(payload['validation_warnings'])
    
    html_content = f'<!doctype html><html lang="vi"><meta charset="utf-8"><title>Test Cases Report</title><style>body{{font:16px system-ui;max-width:1000px;margin:40px auto;padding:0 20px;color:#17243a}}pre{{white-space:pre-wrap;overflow-wrap:anywhere;background:#f2f6fa;padding:16px;border-radius:8px}}h2{{margin-top:32px}}</style>{body}</html>'
    (folder / 'test_cases_report.html').write_text(html_content, encoding='utf-8')