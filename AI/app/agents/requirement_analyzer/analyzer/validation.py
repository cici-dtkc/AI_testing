from .models import Analysis,Document

def validate(analysis:Analysis,document:Document)->list[str]:
    blocks={b.block_id:b.text for b in document.blocks}
    for group in (analysis.rules,analysis.scenarios,analysis.issues):
        ids=[getattr(x,'rule_id',getattr(x,'scenario_id',getattr(x,'issue_id',None))) for x in group]
        if len(ids)!=len(set(ids)):raise ValueError('ID trùng trong output.')
    rule_ids={r.rule_id for r in analysis.rules};scenario_ids={s.scenario_id for s in analysis.scenarios}
    for obj in [*analysis.rules,*analysis.issues]:
        for ref in obj.source_refs:
            if ref.block_id not in blocks:raise ValueError(f'Nguồn không tồn tại: {ref.block_id}')
            if ref.quote not in blocks[ref.block_id]:raise ValueError(f'Quote không nguyên văn tại {ref.block_id}')
    for issue in analysis.issues:
        if not set(issue.blocks_scenarios)<=scenario_ids:raise ValueError('Issue tham chiếu scenario không tồn tại.')
    for s in analysis.scenarios:
        if not set(s.rule_ids)<=rule_ids:raise ValueError('Scenario tham chiếu rule không tồn tại.')
        if s.expected_status=='UNKNOWN':
            if s.then is not None:raise ValueError('UNKNOWN phải có then=null.')
            if not any(s.scenario_id in i.blocks_scenarios for i in analysis.issues):raise ValueError('UNKNOWN phải có issue tương ứng.')
        elif not s.then or not s.then.strip():raise ValueError('SUPPORTED phải có expected.')
    covered={r for s in analysis.scenarios for r in s.rule_ids}
    return [f'Rule chưa có scenario: {r.rule_id}' for r in analysis.rules if r.rule_id not in covered]
