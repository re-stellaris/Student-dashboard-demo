from __future__ import annotations
import csv, json, re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "data" / "shixiseng-crawl"
OUT = ROOT / "data" / "processed" / "skill_mapping.json"
KW = ROOT / "config" / "keywords.txt"
OUT.parent.mkdir(parents=True, exist_ok=True)

starts={
"产品":"产品","运营":"运营","技术管理":"技术管理","后端开发":"后端开发","移动开发":"移动开发","前端开发":"前端开发","游戏":"游戏","测试":"测试","运维/技术支持":"运维/技术支持","数据":"数据","客服":"客服","人工智能":"人工智能","电子/半导体":"电子/半导体","硬件开发":"硬件开发","通信":"通信","视觉/交互设计":"视觉/交互设计","影视传媒":"影视传媒","非视觉设计":"非视觉设计","游戏界面设计":"游戏设计","广告":"广告","采编/写作/出版":"采编/写作/出版","行政":"行政","人力资源":"人力资源","财会":"财会","中后台":"金融中后台","投融资":"投融资","证券/基金/期货":"证券/基金/期货","互联网金融":"互联网金融","保险":"保险","银行":"银行","法务":"法务","律师":"律师","公共媒介":"公共媒介","会务会展":"会务会展","品牌推广":"品牌推广","市场/营销":"市场/营销","销售":"销售","采购":"采购","进出口贸易":"进出口贸易","供应链/物流":"供应链/物流","教务行政":"教务行政","教师":"教师","教育产品研发":"教育产品研发","职业培训":"职业培训","特长培训":"特长培训","翻译":"翻译","咨询调研":"咨询调研","护士/护理":"护士/护理","医生/医技":"医生/医技","健康整形":"健康整形","生物制药":"生物制药","药店":"药店","临床试验":"临床试验","环境科学":"环境科学","能源/矿产/地质":"能源/矿产/地质","农牧渔林":"农牧渔林","消费品管理":"消费品管理","物业管理":"物业管理","房地产规划开发":"房地产规划开发","房地产销售/招商":"房地产销售/招商","设计装修与市政建设":"设计装修与市政建设","园林/景观设计":"园林/景观设计","土木/土建/结构工程师":"土木/土建/结构工程","公共事业":"公共事业","管培生":"管培生","其他":"其他"}

specs=[]
def add(cat,name,*patterns): specs.append((cat,name,[re.compile(p,re.I) for p in patterns]))

# Programming, data and AI
for name,pats in {
"C++":[r"(?<![A-Za-z])C\+\+(?![A-Za-z])",r"C/C\+\+"],"C语言":[r"(?<![A-Za-z+#])C语言",r"(?<![A-Za-z+#])C(?![A-Za-z+#])"],"Java":[r"\bJava\b"],"Python":[r"\bPython\b"],"R语言":[r"\bR语言\b",r"(?<![A-Za-z])R(?![A-Za-z])"],"SQL":[r"\bSQL\b"],"JavaScript":[r"JavaScript",r"\bJS\b"],"TypeScript":[r"TypeScript",r"\bTS\b"],"PHP":[r"\bPHP\b"],"Go/Golang":[r"\bGolang\b",r"\bGo语言\b"],"C#":[r"C#"],".NET":[r"\.NET"],"Ruby":[r"\bRuby\b"],"Perl":[r"\bPerl\b"],"Swift":[r"\bSwift\b"],"Kotlin":[r"\bKotlin\b"],"Shell":[r"\bShell\b",r"Bash"],"MATLAB":[r"MATLAB"],"Scala":[r"\bScala\b"]}.items(): add("编程语言",name,*pats)
for name,pats in {
"Excel":[r"Excel"],"PowerPoint":[r"PowerPoint",r"\bPPT\b"],"Word":[r"Microsoft Word",r"\bWord\b"],"Tableau":[r"Tableau"],"Power BI":[r"Power\s*BI"],"SPSS":[r"SPSS"],"SAS":[r"\bSAS\b"],"Stata":[r"Stata"],"EViews":[r"EViews"],"MySQL":[r"MySQL"],"Oracle":[r"Oracle"],"PostgreSQL":[r"PostgreSQL"],"MongoDB":[r"MongoDB"],"Redis":[r"Redis"],"Hadoop":[r"Hadoop"],"Spark":[r"\bSpark\b"],"Hive":[r"\bHive\b"],"ETL":[r"\bETL\b"],"数据仓库":[r"数据仓库",r"数仓"],"数据清洗":[r"数据清洗"],"数据可视化":[r"数据可视化",r"可视化分析"],"统计分析":[r"统计分析",r"统计学知识"],"A/B Test":[r"A\s*/?\s*B\s*(?:Test|测试|实验)",r"AB测试"],"回归分析":[r"回归分析",r"回归模型"],"分类模型":[r"分类模型",r"分类算法"],"聚类分析":[r"聚类分析",r"聚类算法"],"时间序列":[r"时间序列"],"机器学习":[r"机器学习"],"深度学习":[r"深度学习"],"自然语言处理":[r"自然语言处理",r"\bNLP\b"],"计算机视觉":[r"计算机视觉",r"图像识别"],"TensorFlow":[r"TensorFlow"],"PyTorch":[r"PyTorch"],"scikit-learn":[r"scikit.?learn",r"sklearn"],"Pandas":[r"pandas"],"NumPy":[r"numpy"]}.items(): add("数据与分析",name,*pats)

# Product, design, engineering and business
for name,pats in {
"Axure":[r"Axure"],"Figma":[r"Figma"],"Sketch":[r"\bSketch\b"],"XMind":[r"XMind"],"MindManager":[r"MindManager"],"Visio":[r"Visio"],"Jira":[r"Jira"],"PRD文档":[r"\bPRD\b",r"产品需求文档"],"原型设计":[r"原型设计",r"产品原型"],"需求分析":[r"需求分析",r"需求调研"],"用户研究":[r"用户研究",r"用户调研"],"产品规划":[r"产品规划",r"产品策划"],"项目管理":[r"项目管理",r"项目推进"],"用户体验设计":[r"用户体验",r"交互设计"],"Photoshop":[r"Photoshop",r"\bPS\b"],"Illustrator":[r"Illustrator",r"Adobe AI"],"InDesign":[r"InDesign"],"After Effects":[r"After Effects",r"\bAE\b"],"Premiere":[r"Premiere",r"\bPR\b"],"Final Cut Pro":[r"Final Cut"],"AutoCAD":[r"AutoCAD",r"\bCAD\b"],"SolidWorks":[r"SolidWorks"],"3ds Max":[r"3ds\s*Max"],"Maya":[r"\bMaya\b"],"Blender":[r"Blender"],"Cinema 4D":[r"Cinema\s*4D",r"\bC4D\b"]}.items(): add("产品与设计",name,*pats)
for name,pats in {
"Linux":[r"Linux"],"Git":[r"\bGit\b"],"Docker":[r"Docker"],"Kubernetes":[r"Kubernetes",r"\bK8s\b"],"REST API":[r"REST(?:ful)?\s*API"],"API接口":[r"\bAPI\b",r"接口开发"],"JSON":[r"\bJSON\b"],"自动化测试":[r"自动化测试"],"软件测试":[r"软件测试",r"功能测试"],"嵌入式开发":[r"嵌入式"],"FPGA":[r"FPGA"],"ARM":[r"\bARM\b"],"PCB设计":[r"PCB"],"Verilog":[r"Verilog"],"VHDL":[r"VHDL"],"PLC":[r"\bPLC\b"],"Simulink":[r"Simulink"],"LabVIEW":[r"LabVIEW"],"Altium Designer":[r"Altium",r"\bAD\b.*(?:电路|PCB)"],"Cadence":[r"Cadence"]}.items(): add("工程技术",name,*pats)
for name,pats in {
"SEO":[r"\bSEO\b",r"搜索引擎优化"],"SEM":[r"\bSEM\b",r"搜索引擎营销"],"Google Analytics":[r"Google Analytics",r"\bGA\b"],"市场调研":[r"市场调研",r"市场研究"],"品牌策划":[r"品牌策划",r"品牌规划"],"内容运营":[r"内容运营"],"新媒体运营":[r"新媒体运营",r"公众号运营"],"社群运营":[r"社群运营"],"活动策划":[r"活动策划"],"文案写作":[r"文案写作",r"文案撰写"],"CRM":[r"\bCRM\b",r"客户关系管理"],"供应链管理":[r"供应链管理"],"财务建模":[r"财务建模",r"financial model"],"估值分析":[r"估值分析",r"估值模型"],"会计准则":[r"会计准则"],"审计":[r"审计工作",r"内部审计"],"税务知识":[r"税务",r"税法"],"Wind":[r"\bWind\b",r"万得"],"Bloomberg":[r"Bloomberg"]}.items(): add("业务专业技能",name,*pats)
for name,pats in {"实验设计":[r"实验设计"],"临床研究":[r"临床研究",r"临床试验"],"GCP规范":[r"\bGCP\b"],"PCR":[r"\bPCR\b"],"细胞培养":[r"细胞培养"],"文献检索":[r"文献检索",r"文献调研"],"课程设计":[r"课程设计",r"课程开发"],"教学设计":[r"教学设计",r"教案"],"英语":[r"英语(?:六级|四级|能力|水平|读写|听说)",r"\bCET-?[46]\b"],"日语":[r"日语(?:能力|水平|N[1-5])"]}.items(): add("领域知识",name,*pats)

keywords=[x.strip() for x in KW.read_text(encoding="utf-8-sig").splitlines() if x.strip()]
category={};cur="其他"
for kw in keywords:
 if kw in starts: cur=starts[kw]
 category[kw]=cur

jobs={}
for p in (SRC/"jobs").glob("*.json"):
 try: jobs[p.stem]=json.loads(p.read_text(encoding="utf-8"))
 except Exception: pass
mapping=defaultdict(list)
with (SRC/"keyword_job_map.csv").open(encoding="utf-8-sig",newline="") as f:
 for r in csv.DictReader(f):
  if r["job_id"] in jobs: mapping[r["search_keyword"]].append(r["job_id"])

rows=[]; summaries=[]
for kw in keywords:
 ids=list(dict.fromkeys(mapping.get(kw,[]))); found=defaultdict(dict)
 for jid in ids:
  job=jobs[jid]; text=job.get("raw_jd","")
  sentences=[s.strip() for s in re.split(r"[。；;\n]+",text) if s.strip()]
  for scat,skill,patterns in specs:
   if any(p.search(text) for p in patterns):
    ev=next((s for s in sentences if any(p.search(s) for p in patterns)),"")[:240]
    found[(scat,skill)][jid]=ev
 ranked=sorted(found.items(),key=lambda x:(-len(x[1]),x[0][1]))
 for (scat,skill),mentions in ranked:
  sample_jid=next(iter(mentions));j=jobs[sample_jid]
  rows.append({"category":category.get(kw,"其他"),"keyword":kw,"skill":skill,"skill_category":scat,"mention_jds":len(mentions),"sample_jds":len(ids),"coverage":len(mentions)/len(ids) if ids else 0,"example_job":j.get("job_title",""),"evidence":mentions[sample_jid],"source_url":j.get("job_url","")})
 summaries.append({"category":category.get(kw,"其他"),"keyword":kw,"sample_jds":len(ids),"skill_count":len(ranked),"top_skills":"、".join(k[1] for k,v in ranked[:10])})

OUT.write_text(json.dumps({"rows":rows,"summaries":summaries,"lexicon":[{"skill_category":c,"skill":s,"patterns":" | ".join(p.pattern for p in ps)} for c,s,ps in specs],"stats":{"jobs":len(jobs),"keywords":len(keywords),"mapping_rows":sum(len(v) for v in mapping.values()),"skill_rows":len(rows)}},ensure_ascii=False),encoding="utf-8")
print(json.dumps({"jobs":len(jobs),"keywords":len(keywords),"skill_rows":len(rows)},ensure_ascii=False))
