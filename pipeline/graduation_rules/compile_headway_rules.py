"""Compile the interpreted July 2026 revision. No network or PDF dependency.
Inputs preserve main-scheme evidence, source hashes and per-course catalogue units.
Run: python tools/compile_headway_rules.py
"""
import json,re
from pathlib import Path
from study_scheme_parser import course_rows,expand_row
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'graduation_rules/2026-07/source_documents.json'
PROGRAMMES={
 'music_composition':('Music Composition and Theory','MUS',range(1,5)),
 'musicology':('Musicology','MUS',range(5,8)),
 'music_performance':('Music Performance','MUS',range(8,12)),
 'dsbdt':('Data Science and Big Data Technology','SDS',range(13,17)),
 'statistics':('Statistics','SDS',range(17,21)),
 'cse':('Computer Science and Engineering','SDS',range(21,25)),
 'chemistry':('Chemistry','SSE',range(25,29)),
 'math':('Mathematics and Applied Mathematics','SSE',range(29,32)),
 'nese':('New Energy Science and Engineering','SSE',range(32,35)),
 'physics':('Physics','SSE',range(35,38)),
 'ece':('Electrical and Computer Engineering','SSE',range(38,41))}
SECTION=re.compile(r'^\d+\.\s*(School Package|Required Courses|(?:Major )?Elective(?: Courses)?):')

def clean(d):
 lines=[]
 for p in d['mainPages']:
  ls=p['text'].splitlines()
  for i,s in enumerate(ls):
   if s.startswith('Last ') or re.match(r'^Page \d+',s):continue
   if i>=len(ls)-2 and s.strip()==str(p['page']):continue
   if re.fullmatch(r'\[[a-z]\]',s.strip()):continue
   lines.append((p['page'],s))
 return lines

def groups(text):
 # The English ECE2025 table is missing a comma; Chinese p5 confirms the split.
 text=text.replace('5023ECE3040','5023, ECE3040')
 result=[]
 for row in course_rows(text):
  _,expanded=expand_row(row)
  expanded=re.sub(r'\[[a-z]\]|#','',expanded)
  for part in re.split(r'[,;]',expanded):
   codes=re.findall(r'\b[A-Z]{2,5}[1-6]\d{3}[A-Z]?\b',part)
   if not codes:continue
   # Slashes in these sources denote cross-listed aliases (e.g. MSE3011/6002).
   if len(codes)>1 and not re.search(r'\bor\b|/',part):
    raise ValueError('Uninterpreted course expression: '+part)
   result.append({'options':[[c] for c in dict.fromkeys(codes)]})
 return result

def parse(d):
 rows=clean(d);blocks=[];notes=[];cur=None;stream='common';note=False;minimum=0
 for i,(page,s) in enumerate(rows):
  nextline=rows[i+1][1] if i+1<len(rows) else ''
  if re.search(r'(Stream|Concentration)$',s) and nextline.startswith('Students are required'):
   if cur:blocks.append(cur);cur=None
   stream=re.sub(r' (?:Stream|Concentration)$','',s);note=False
  m=re.search(r'minimum of (\d+) units',s)
  if m:minimum=int(m[1])
  m=SECTION.match(s)
  if m:
   if cur:blocks.append(cur)
   cur={'stream':stream,'role':'school' if m[1]=='School Package' else 'required' if m[1]=='Required Courses' else 'elective',
        'lines':[(page,s)],'printedMinimum':minimum};note=False
  elif re.match(r'^(?:Explanatory )?Notes?:',s):
   if cur:blocks.append(cur);cur=None
   note=True
  elif s.startswith('Total'):
   if cur:blocks.append(cur);cur=None
  elif cur:cur['lines'].append((page,s))
  elif note:notes.append((page,s))
 if cur:blocks.append(cur)
 for b in blocks:
  b['pages']=sorted({p for p,s in b['lines']});b['text']='\n'.join(s for p,s in b.pop('lines'))
  # The first unit-column value is the component total except the hand-interpreted ECE pools.
  values=re.findall(r'(?<!\d)(\d{1,2})\s*$',b['text'],re.M)
  b['units']=int(values[0]) if values else None
  b['groups']=groups(b['text']) if b['role']!='elective' else []
 return blocks,notes

def apply_alternatives(b,notes):
 text=' '.join(s for p,s in notes)
 for m in re.finditer(r'Students can choose to take either (.*?) to fulfil[l]? the graduation',text):
  options=[re.findall(r'\b[A-Z]{2,5}\d{4}[A-Z]?\b',part) for part in re.split(r'\s+or\s+',m[1])]
  assert all(options),m[0]
  union={c for a in options for c in a}
  hits=[g for g in b['groups'] if any(c in union for a in g['options'] for c in a)]
  if hits:
   b['groups']=[g for g in b['groups'] if g not in hits]+[{'options':options,'evidence':m[0]+' the graduation requirement.'}]

def pools(b,n,key):
 text=b['text'];parts=[]
 # Only these programme/category boundaries have independent eligibility pools.
 if key in ['chemistry','ece','cse']:
  markers=list(re.finditer(r'^\(([abc])\)(?!\.)\s*',text,re.M))
  for i,m in enumerate(markers):parts.append((m[1],text[m.end():markers[i+1].start() if i+1<len(markers) else len(text)]))
 elif key=='dsbdt':
  markers=list(re.finditer(r'^[iv]+\) (.+) Stream\s*$',text,re.M))
  for i,m in enumerate(markers):parts.append((m[1],text[m.end():markers[i+1].start() if i+1<len(markers) else len(text)]))
 elif key=='statistics':
  regex=r'^(Area [1-4] [^:]+|Complementary Electives):' if n in [17,18] else r'^(?:[1-5]\. (.+?) Stream\s*$|(Complementary Electives):)'
  markers=list(re.finditer(regex,text,re.M))
  for i,m in enumerate(markers):
   label=m[1] or m[2]
   parts.append((label,text[m.end():markers[i+1].start() if i+1<len(markers) else len(text)]))
 else:parts=[('Electives',text)]
 result=[]
 for name,body in parts:
  gs=groups(body);assert gs,(n,name)
  p={'id':name,'label':name,'groups':gs,'minUnits':0,'minCourses':0}
  if key=='chemistry':p['minUnits']={'a':2,'b':6,'c':9}[name]
  if key=='ece':
   p['minUnits']=(15 if b['stream']=='Microelectronics Science and Engineering' or (n==38 and b['stream']=='Computer Engineering') else 18) if name=='a' else (3 if b['stream']=='Microelectronics Science and Engineering' else 6)
  if key=='cse' and name=='a':
   if n==21:p['minCourses']=4
   else:p['minUnits']=18 if n==24 else 16
  result.append(p)
 return result

def compile_all(docs):
 out={};summaries=[]
 for key,(label,school,nums) in PROGRAMMES.items():
  programme={'label':label,'school':school,'variants':[]}
  for n in nums:
   d=next(x for x in docs if x['id']==f'u{n:02}')
   blocks,notes=parse(d)
   statement=next(s for p,s in clean(d) if 'Applicable to students admitted' in s)
   years=[int(y) for y in re.findall(r'(20\d\d)-\d\d',statement)]
   variant={'id':d['id'],'from':min(years),'to':None if 'thereafter' in statement else max(years),
       'source':{'file':d['file'],'sha256':d['sha256'],'pages':[p['page'] for p in d['mainPages']],
                 'applicability':statement},'paths':[],'courseUnits':d['courseUnits'],'codeAliases':d.get('codeAliases',[])}
   for stream in dict.fromkeys(b['stream'] for b in blocks):
    bs=[b for b in blocks if b['stream']==stream]
    path={'id':stream,'label':stream,'total':0,'components':[],'pools':[],'constraints':[],'issues':[],
          'notes':'\n'.join(s for p,s in notes),'sourcePages':sorted({p for b in bs for p in b['pages']})}
    for b in bs:
     if b['role']!='elective':
      apply_alternatives(b,notes)
      path['components'].append({'id':b['role'],'label':'School Package' if b['role']=='school' else 'Required Courses',
          'minUnits':b['units'],'groups':b['groups'],'sourcePages':b['pages']})
     else:
      ps=pools(b,n,key);path['pools']=ps
      units=sum(p['minUnits'] for p in ps) if key=='ece' else b['units']
      path['electiveUnits']=units
      if key=='music_performance':
       # Conducting2023 requires one entire instrumental series, not arbitrary 7 units.
       gs=[{'options':[['MUS1603','MUS1604','MUS2603','MUS2604','MUS3603'],['MUS1605','MUS1606','MUS2605','MUS2606','MUS3605']]}]
       path['components'].append({'id':'instrumental','label':'Instrumental series','minUnits':7,'groups':gs,'sourcePages':b['pages']})
       path['pools']=[];path['electiveUnits']=0
       path['issues'].append({'code':'instrumental_units_conflict','blocking':False,'message':'The secondary-instrument series totals 5 units in the course list, while the major-elective requirement is 7. That option needs programme confirmation; no extra credits are assumed.','pages':[3,23,24]})
    path.setdefault('electiveUnits',0)
    path['total']=sum(c['minUnits'] for c in path['components'])+path['electiveUnits']
    if path['total']!=bs[0]['printedMinimum']:
     assert key=='music_performance' and stream=='Conducting' and n in [10,11],(n,stream,path['total'])
     path['issues'].append({'code':'conflicting_total','blocking':True,'message':'The scheme states a minimum of 71 units but its table totals 69. Final completion requires confirmation.','pages':[3]})
    if key=='cse':
     if n==21:path['constraints'].append({'type':'course_count','min':6})
     electiveNotes=' '.join(s for p,s in notes)
     ai=electiveNotes.split('four courses from the following list:',1)[1].split('Students may declare',1)[0]
     path['optionalStreams']=[{'id':'ai','label':'Artificial Intelligence','minCourses':4,'groups':groups(ai)}]
    if key=='dsbdt':
     path['constraints'].append({'type':'max_level_count','max':2,'below':3000,'atLeast':1000 if n==16 else 2000})
     path['optionalStreams']=[{'id':p['id'],'label':p['label'],'minCourses':4,'groups':p['groups']} for p in path['pools']]
    if key=='statistics':
     if n in [17,18]:
      path['constraints'] += [{'type':'course_count','min':9},{'type':'breadth','min':3,'pools':[p['id'] for p in path['pools']]},
          {'type':'depth','min':3,'pools':[p['id'] for p in path['pools'] if p['id']!='Complementary Electives']}]
      path['optionalStreams']=[{'id':'financial','label':'Financial Statistics','requirements':[
          {'groups':[{'options':[['STA4003']]}],'minCourses':1},
          {'groups':path['pools'][3]['groups'],'minCourses':3},
          {'groups':groups('ECO3121, FIN2010, FIN2020, FIN3080, FIN3210, FIN4110, FIN4120'),'minCourses':3}]}]
     else:
      path['constraints'].append({'type':'depth','min':3,'pools':[p['id'] for p in path['pools'] if p['id']!='Complementary Electives']})
      path['optionalStreams']=[{'id':p['id'],'label':p['label'],'minCourses':4,'groups':p['groups']} for p in path['pools'] if p['id']!='Complementary Electives']
    if key=='physics':path['externalElectiveMaxUnits']=6
    if key in ['math','ece']:path['restrictedElectivesNotice']='Graduate electives require the approval specified in the scheme. Only officially earned transcript credits are counted; planned enrolment is not approval.'
    if n==40:path['issues'].append({'code':'normalized_missing_comma','blocking':False,'message':'English “5023ECE3040” normalized to AIR5023, ECE3040, confirmed by Chinese p5.','pages':[1,5]})
    variant['paths'].append(path)
   programme['variants'].append(variant)
   summaries.append({'id':d['id'],'programme':key,'source':variant['source'],'paths':[{k:p[k] for k in ['id','total','electiveUnits','issues']} for p in variant['paths']]})
  out[key]=programme
 return {'schemaVersion':2,'revision':'2026-07-20','programmes':out},summaries

if __name__=='__main__':
 docs=json.loads(SOURCE.read_text(encoding='utf8'))
 rules,summary=compile_all(docs)
 raw=json.dumps(rules,ensure_ascii=False,indent=2)
 (ROOT/'graduation_rules/2026-07/rules.json').write_text(raw+'\n',encoding='utf8')
 (ROOT/'graduation_rules/2026-07/analysis-index.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
 (ROOT/'students-interface/study_headway_rules.js').write_text('/* Generated by tools/compile_headway_rules.py. */\n(function(root){const data='+json.dumps(rules,ensure_ascii=False,separators=(',',':'))+';if(typeof module!=="undefined"&&module.exports)module.exports=data;else root.STUDY_HEADWAY_RULES=data;})(typeof globalThis!=="undefined"?globalThis:this);\n',encoding='utf8')
 print('Compiled',len(summary),'cohort documents;',sum(len(v['paths']) for p in rules['programmes'].values() for v in p['variants']),'programme/cohort/stream paths.')
