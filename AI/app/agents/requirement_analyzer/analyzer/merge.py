from .models import Analysis

def merge_analyses(parts:list[Analysis])->Analysis:
    rules=[];scenarios=[];issues=[]
    for n,a in enumerate(parts,1):
        prefix=f'C{n:03}-'
        rm={x.rule_id:prefix+x.rule_id for x in a.rules}
        sm={x.scenario_id:prefix+x.scenario_id for x in a.scenarios}
        rules.extend(x.model_copy(update={'rule_id':rm[x.rule_id]}) for x in a.rules)
        scenarios.extend(x.model_copy(update={'scenario_id':sm[x.scenario_id],'rule_ids':[rm[i] for i in x.rule_ids]}) for x in a.scenarios)
        issues.extend(x.model_copy(update={'issue_id':prefix+x.issue_id,'blocks_scenarios':[sm[i] for i in x.blocks_scenarios]}) for x in a.issues)
    # Keep variants rather than silently discarding possibly distinct scenarios.
    return Analysis(summary=f'Đã gộp {len(parts)} nhóm. Kết quả cần review nghiệp vụ; không tự deduplicate theo ngữ nghĩa.',rules=rules,scenarios=scenarios,issues=issues)
