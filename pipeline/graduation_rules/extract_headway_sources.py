"""Re-extract the reviewed source revision; reject changed PDFs pending re-analysis.
Requires pdfplumber. Run from any directory with --root PATH_TO_SCHEMES.
The tracked snapshot records exact page boundaries and SHA-256 source identities.
"""
import argparse,hashlib,json,re
from pathlib import Path
import pdfplumber
parser=argparse.ArgumentParser()
parser.add_argument('--root',required=True,type=Path)
args=parser.parse_args()
source=Path(__file__).resolve().parents[1]/'graduation_rules/2026-07/source_documents.json'
templates=json.loads(source.read_text(encoding='utf8'))
docs=[]
for t in templates:
 file=args.root/t['file']
 if hashlib.sha256(file.read_bytes()).hexdigest()!=t['sha256']:
  raise ValueError('Changed source needs review before recompilation: '+str(file))
 with pdfplumber.open(file) as pdf:
  pages=[{'page':i+1,'text':p.extract_text(x_tolerance=1,y_tolerance=3) or ''} for i,p in enumerate(pdf.pages)]
 main_numbers={p['page'] for p in t['mainPages']}
 docs.append({**t,'pages':pages,'mainPages':[p for p in pages if p['page'] in main_numbers]})
result=[]
for d in docs:
 if d['id'] in ['u12','u41','u42','u43','u44','u45']:continue
 units={};evidence={};active=False;pending=None
 for p in d['pages']:
  for line in p['text'].splitlines():
   if re.search(r'Course (?:List|Code)',line):active=True
   if not active:continue
   m=re.match(r'^(?:Option [A-Z]#?\s+)?([A-Z]{2,5}[1-6]\d{3}[A-Z]?)(?:\[[^\]]+\]|\*|[ivx]+)*(?:\s+|$)',line)
   if m:pending=(m[1],[],p['page'])
   if not pending:continue
   pending[1].append(line)
   u=re.search(r'\s([0-9](?:\.5)?)\s*$',line)
   if u:
    code,ls,page=pending;value=float(u[1]);value=int(value) if value.is_integer() else value
    if code in units and units[code]!=value:raise ValueError((d['id'],code,units[code],value))
    units[code]=value;evidence[code]={'page':page,'text':'\n'.join(ls)}
    for alias in re.findall(r'\(([A-Z]{2,5}[1-6]\d{3}[A-Z]?)\)', '\n'.join(ls)):
     units[alias]=value;evidence[alias]=evidence[code]
    pending=None
   elif len(pending[1])>4:pending=None
 # Two wrapped catalogue cells need explicit evidence; never fill unknown units with 3.
 overrides={'u06':{'MUS2500':(2,'School Package: MUS2500 2')},'u35':{'PHY4510':(3,'Statistical Mechanics and its')}}
 for code,(value,needle) in overrides.get(d['id'],{}).items():
  p=next(p for p in d['pages'] if needle in p['text'])
  units[code]=value;evidence[code]={'page':p['page'],'text':p['text']}
 # Keep catalogue footnotes as well: they contain official course-code changes.
 supplemental=[p for p in d['pages'] if any(s in p['text'] for s in ['course code','course codes','codes change','equal to'])]
 aliases=[]
 full=' '.join(p['text'].replace('\n',' ') for p in supplemental)
 for a,b in [('STA2003','STA2001H'),('STA2004','STA2002H')]+[(f'AIR{a}',f'AIR{b}') for a,b in [('6021','5021'),('6023','5023'),('6064','5064'),('6202','5202'),('6205','5205'),('6206','5206'),('6207','5207'),('6211','5211'),('6212','5212')]]:
  if a in full and b in full:aliases.append([a,b])
 if 'codes change to' in full:
  for pair in [('MUS2500','MUS3530'),('MUS2501','MUS3531'),('MUS2401','MUS3532')]:
   if all(x in full for x in pair):aliases.append(list(pair))
 if 'equal to CHM2001' in full:aliases.append(['CHM2001','CHM2310'])
 result.append({k:d[k] for k in ['id','file','sha256','mainPages']}|{'courseUnits':units,'courseUnitEvidence':evidence,'codeAliases':aliases,'supplementalPages':supplemental})
dest=source.parent;dest.mkdir(parents=True,exist_ok=True)
(dest/'source_documents.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print([(d['id'],len(d['courseUnits'])) for d in result])
