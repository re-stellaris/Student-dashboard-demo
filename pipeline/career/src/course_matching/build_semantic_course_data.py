import json, re
from pathlib import Path

root=Path(__file__).resolve().parents[1]
old=json.loads((root/'work/course_recommendations.json').read_text(encoding='utf-8'))
decisions=json.loads((root/'work/ai_course_review_decisions.json').read_text(encoding='utf-8'))
courses=json.loads((root/'work/course_data.json').read_text(encoding='utf-8'))['rows']
course_by={c['course_code']:c for c in courses}
accepted={(d['skill'],d['course_code']):d for d in decisions if d['label']!='not_related'}

def latest_term(s):
    terms=[x.strip() for x in re.split(r'[\n；;]+',s or '') if x.strip()]
    def key(t):
        m=re.search(r'(\d{4})-(\d{2})\s+(Term\s*([12])|Summer)',t,re.I)
        if not m:return (0,0)
        rank=3 if m.group(3).lower()=='summer' else int(m.group(4))
        return (int(m.group(1)),rank)
    return max(terms,key=key) if terms else ''

out=[]; seen=set()
for r in old['rows']:
    d=accepted.get((r['skill'],r['course_code']))
    if not d: continue
    k=(r['category'],r['keyword'],r['skill'],r['course_code'])
    if k in seen: continue
    seen.add(k)
    c=course_by.get(r['course_code'],{})
    relation='直接培养' if d['label']=='direct_training' else '基础支撑'
    reason=d['reason']
    if d.get('foundation_bridge'):
        reason += ' ' + d['foundation_bridge']
    out.append({
        'category':r['category'],'keyword':r['keyword'],'skill':r['skill'],
        'jd_source_url':r['jd_source_url'],'relation':relation,
        'ai_reason':reason,'course_evidence':d['evidence_quote'],
        'evidence_field':d['evidence_field'],'confidence':d['confidence'],
        'course_code':r['course_code'],'course_name_zh':r['course_name_zh'],
        'course_name_en':r['course_name_en'],'latest_term':latest_term(c.get('offered_terms','')),
    })

stats={
 'candidate_pairs':len(decisions),
 'accepted_pairs':len(accepted),
 'direct_pairs':sum(d['label']=='direct_training' for d in decisions),
 'foundation_pairs':sum(d['label']=='foundational_support' for d in decisions),
 'rejected_pairs':sum(d['label']=='not_related' for d in decisions),
 'low_confidence_pairs':sum(d['label']!='not_related' and d['confidence']<0.75 for d in decisions),
 'output_rows':len(out),
 'skills_with_recommendations':len({r['skill'] for r in out}),
 'keywords_covered':len({r['keyword'] for r in out}),
}
(root/'work/semantic_course_recommendations.json').write_text(json.dumps({'rows':out,'stats':stats},ensure_ascii=False),encoding='utf-8')
print(json.dumps(stats,ensure_ascii=False))
