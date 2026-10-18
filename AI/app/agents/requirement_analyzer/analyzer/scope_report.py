"""Escape all document/model values; report is local, with no external resources."""
import csv,html,json
from pathlib import Path

def write_scope_report(payload:dict,folder:Path):
    scope=payload['scope_comparison'];esc=lambda x:html.escape(str(x))
    plan={i['item_id']:i for i in payload['test_plan']['items']}
    def cards(items):return ''.join('<pre>'+esc(json.dumps(i,ensure_ascii=False,indent=2))+'</pre>' for i in items) or '<p>Không có.</p>'
    rows=''
    for row in scope['coverage_matrix']:
        item=plan[row['plan_item_id']]
        rows+='<tr>'+''.join('<td>'+esc(x)+'</td>' for x in (row['requirement_id'],item['outline'],row['estimated_test_cases'],str(len(row['rule_ids']))+' + '+str(len(row['dependency_rule_ids'])),len(row['scenario_ids']),len(row['pending_ids']),row['coverage_status']))+'</tr>'
    decrows=''.join('<tr>'+''.join('<td>'+esc(x)+'</td>' for x in (d['item_type'],d['item_id'],d['status'],', '.join(d['plan_item_ids']),d['reason']))+'</tr>' for d in scope['decisions'])
    body='<h1>Requirement Analyzer v3 đối chiếu Test Plan</h1><p>REVIEW_REQUIRED · Method: '+esc(scope['method'])+'</p>'
    body+='<p>Baseline lọc: bảng 2.1. Nội dung rule/scenario giữ nguyên từ SRS. Số TC ước tính không phải quota. MAPPED không chứng minh coverage đầy đủ hoặc test đã PASS.</p><h2>Trước và sau</h2>'+cards([scope['stats']])
    body+='<h2>Xung đột Test Plan cần xác nhận</h2>'+cards(payload['test_plan']['conflicts'])
    body+='<h2>Ma trận đối chiếu</h2><table><tr><th>Mã</th><th>Outline</th><th>TC ước tính</th><th>Rule trực tiếp + phụ thuộc</th><th>Scenario</th><th>Chờ review</th><th>Trạng thái</th></tr>'+rows+'</table>'
    body+='<h2>Toàn bộ quyết định giữ loại review</h2><table><tr><th>Loại</th><th>ID</th><th>Quyết định</th><th>Hạng mục plan</th><th>Lý do</th></tr>'+decrows+'</table>'
    body+='<h2>Rule giữ làm phụ thuộc</h2>'+cards(scope['dependency_rule_ids'])
    for key,label in [('rules','Rules trong bộ rút gọn'),('scenarios','Scenarios trong bộ rút gọn'),('issues','Issues liên quan')]:body+='<h2>'+label+'</h2>'+cards(payload['analysis'][key])
    body+='<h2>Hàng chờ review không bị mất</h2>'+cards([scope['review_queue']])
    body+='<h2>PR checklist không tự đánh giá đạt</h2>'+cards(scope['process_checks'])
    body+='<h2>Validation warnings</h2>'+cards(payload['validation_warnings'])
    body+='<h2>Plan nguồn và bằng chứng</h2>'+cards(payload['test_plan']['items'])
    (folder/'scope_report.html').write_text('<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>Scope Comparison v3</title><style>body{font:15px Arial,sans-serif;margin:28px;color:#17243a}table{border-collapse:collapse;width:100%;margin:16px 0}td,th{border:1px solid #ccc;padding:8px;text-align:left;vertical-align:top;overflow-wrap:anywhere}th{background:#edf3f9}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#f4f6f9;padding:12px}h2{margin-top:32px}</style></head><body>'+body+'</body></html>',encoding='utf-8')
    for filename,rows in [('scope_decisions.csv',scope['decisions']),('coverage_matrix.csv',scope['coverage_matrix'])]:
        with (folder/filename).open('w',encoding='utf-8-sig',newline='') as f:
            writer=csv.DictWriter(f,fieldnames=list(rows[0]) if rows else []);writer.writeheader()
            for row in rows:
                flat={k:json.dumps(v,ensure_ascii=False) if isinstance(v,(list,dict)) else v for k,v in row.items()}
                # Spreadsheet formula injection defense.
                flat={k:("'"+v if isinstance(v,str) and v.startswith(('=','+','-','@','\t','\r')) else v) for k,v in flat.items()};writer.writerow(flat)
