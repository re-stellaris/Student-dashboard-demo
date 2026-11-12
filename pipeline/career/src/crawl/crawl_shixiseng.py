from __future__ import annotations
import argparse,csv,html,json,re,time,urllib.parse,urllib.request
from html.parser import HTMLParser
from pathlib import Path

BASE="https://www.shixiseng.com"
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124 Safari/537.36"
SOFT=("沟通","责任心","抗压","团队","主动性","执行力","价值观","性格","细心","认真","学习能力")
TECH=("SQL","Python","Java","C++","Excel","Tableau","PowerBI","Power BI","R语言","SPSS","SAS","Matlab","Hadoop","Spark","Hive","TensorFlow","PyTorch","Sklearn","Axure","CAD","Photoshop","PS","AI","数据库","算法","模型","统计","数据分析","产品架构","需求分析","测试","开发","设计","运营")

class Text(HTMLParser):
 def __init__(self): super().__init__(); self.parts=[]
 def handle_data(self,d):
  d=re.sub(r"\s+"," ",d).strip()
  if d:self.parts.append(d)

def fetch(url,retries=3):
 for n in range(retries):
  try:
   req=urllib.request.Request(url,headers={"User-Agent":UA,"Accept-Language":"zh-CN,zh;q=0.9"})
   with urllib.request.urlopen(req,timeout=45) as r:return r.read().decode("utf-8","replace")
  except Exception:
   if n==retries-1:raise
   time.sleep(3*(2**n))

def textify(source):
 source=re.sub(r"<(script|style)[^>]*>.*?</\1>","",source,flags=re.I|re.S)
 p=Text();p.feed(source);return "\n".join(p.parts)

def parse_detail(source,url,job_id):
 t=textify(source); title_tag=html.unescape(re.search(r"<title[^>]*>(.*?)</title>",source,re.I|re.S).group(1)) if re.search(r"<title[^>]*>(.*?)</title>",source,re.I|re.S) else ""
 title=title_tag.split("实习招聘-")[0].strip()
 company="";m=re.search(r"实习招聘-(.*?)实习生招聘",title_tag);company=m.group(1) if m else ""
 a=t.find("职位描述：");b=t.find("投递要求：",a+1);jd=t[a+5:b if b>a else None].strip() if a>=0 else ""
 seg=[x.strip() for x in re.split(r"[。；;\n]+",jd) if x.strip()]
 majors=[x for x in seg if "专业" in x and not any(s in x for s in SOFT)]
 skills=[x for x in seg if any(k.lower() in x.lower() for k in TECH) and not (any(s in x for s in SOFT) and not any(k.lower() in x.lower() for k in TECH[:22]))]
 return {"job_id":job_id,"job_title":title,"company":company,"job_url":url,"raw_jd":jd,"major_requirement":"；".join(majors),"technical_requirements":"；".join(skills)}

def main():
 ap=argparse.ArgumentParser();ap.add_argument("--keywords",type=Path,required=True);ap.add_argument("--output",type=Path,required=True);ap.add_argument("--max-per-keyword",type=int,default=100);ap.add_argument("--delay",type=float,default=2.5);ap.add_argument("--only");a=ap.parse_args()
 a.output.mkdir(parents=True,exist_ok=True);cache=a.output/"jobs";cache.mkdir(exist_ok=True)
 kws=list(dict.fromkeys(x.strip() for x in a.keywords.read_text(encoding="utf-8-sig").splitlines() if x.strip()));kws=[a.only] if a.only else kws
 maps=[];fail=[]
 for ki,kw in enumerate(kws,1):
  links=[]
  for page in range(1,6):
   q=urllib.parse.urlencode({"page":page,"type":"intern","keyword":kw,"area":"","months":"","days":"","degree":"","official":"","enterprise":"","salary":"-0","publishTime":"","sortType":""})
   try:src=fetch(BASE+"/interns?"+q)
   except Exception as e:fail.append({"keyword":kw,"url":BASE+"/interns?"+q,"error":repr(e)});break
   new=[]
   for jid in re.findall(r"/intern/(inn_[A-Za-z0-9]+)",src):
    if jid not in links:new.append(jid);links.append(jid)
   print(f"[{ki}/{len(kws)}] {kw} page={page} total={len(links)}",flush=True)
   if not new or len(links)>=a.max_per_keyword:break
   time.sleep(a.delay)
  for rank,jid in enumerate(links[:a.max_per_keyword],1):
   maps.append({"search_keyword":kw,"rank":rank,"job_id":jid})
   p=cache/(jid+".json")
   if p.exists():continue
   url=BASE+"/intern/"+jid
   try:p.write_text(json.dumps(parse_detail(fetch(url),url,jid),ensure_ascii=False,indent=2),encoding="utf-8")
   except Exception as e:fail.append({"keyword":kw,"url":url,"error":repr(e)})
   time.sleep(a.delay)
  with (a.output/"keyword_job_map.csv").open("w",encoding="utf-8-sig",newline="") as f:
   w=csv.DictWriter(f,fieldnames=["search_keyword","rank","job_id"]);w.writeheader();w.writerows(maps)
  with (a.output/"crawl_failures.csv").open("w",encoding="utf-8-sig",newline="") as f:
   w=csv.DictWriter(f,fieldnames=["keyword","url","error"]);w.writeheader();w.writerows(fail)
 rows=[json.loads(p.read_text(encoding="utf-8")) for p in cache.glob("*.json")]
 with (a.output/"jobs_extracted.csv").open("w",encoding="utf-8-sig",newline="") as f:
  fields=["job_id","job_title","company","job_url","raw_jd","major_requirement","technical_requirements"];w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(rows)
 with (a.output/"jobs_raw.jsonl").open("w",encoding="utf-8") as f:
  for r in rows:f.write(json.dumps(r,ensure_ascii=False)+"\n")
 print(json.dumps({"keywords":len(kws),"mappings":len(maps),"unique_jobs":len(rows),"failures":len(fail)},ensure_ascii=False),flush=True)
if __name__=="__main__":main()
