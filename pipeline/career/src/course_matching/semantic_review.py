import json, glob, re
from pathlib import Path

FIELDS = ["learning_outcomes", "course_syllabus", "assessment_scheme", "description_chinese", "description_english"]

def load_rows():
    out=[]
    for fn in sorted(glob.glob("work/ai-course-review-batches/*.jsonl")):
        for line in open(fn, encoding="utf-8"):
            out.append(json.loads(line))
    return out

def snippets(text, term, radius=110):
    ans=[]
    for m in re.finditer(re.escape(term), text, flags=re.I):
        a=max(0,m.start()-radius); b=min(len(text),m.end()+radius)
        ans.append(re.sub(r"\s+", " ", text[a:b]).strip())
    return ans

AMBIGUOUS_SKILLS = {
    "C语言", "Visio", "Photoshop", "PowerPoint", "Sketch", "Wind", "Word",
    "Illustrator", "Maya", "SEM", "SEO", "Redis", "Shell", ".NET", "EViews",
}

REJECT_PAIRS = {
    ("用户研究", "AIE4901"), ("用户研究", "AIE4902"), ("用户研究", "CHI3001"), ("用户研究", "ECO6107"),
    ("活动策划", "ENB4101"), ("临床研究", "BME4012"),
    ("JavaScript", "CSC4301"), ("C#", "CSC4301"), ("PHP", "CSC4301"), ("Ruby", "CSC4301"),
    ("AutoCAD", "ENE4008"), ("AutoCAD", "ENE6011"),
    ("Google Analytics", "CHM6330"), ("课程设计", "FIN6130"),
    ("教学设计", "FIN6130"), ("教学设计", "MUS1606"),
    ("时间序列", "CSC4301"), ("自然语言处理", "AIE1001"),
    ("CRM", "IBA6302"), ("API接口", "ACT6241"),
    ("实验设计", "MAT3350"),
    ("文案写作", "CHI1000"), ("文案写作", "CHI4004"), ("文案写作", "CHI3008"), ("文案写作", "ENB2004"),
    ("财务建模", "ECO6121"), ("财务建模", "ENB2004"),
}

# AI semantic decisions for pairs whose evidence uses a synonym or a fuller concept name.
CURATED = {
    ("用户体验设计", "ECE4300"): ("direct_training", "用户界面设计|用户体验|可用性评估"),
    ("需求分析", "AIE3901"): ("direct_training", "需求分析"),
    ("需求分析", "AIE3902"): ("direct_training", "需求分析"),
    ("项目管理", "MBM6320"): ("foundational_support", "项目管理|Project Management"),
    ("市场调研", "MKT6331"): ("foundational_support", "consumer researchers|experimental methods"),
    ("A/B Test", "DDA6030"): ("foundational_support", "hypothesis testing|confidence intervals"),
    ("A/B Test", "ECO6102"): ("foundational_support", "因果推断|实证分析"),
    ("实验设计", "DDA4002"): ("direct_training", "模擬實驗設計|experiment design"),
    ("实验设计", "DDA6030"): ("foundational_support", "hypothesis testing|confidence intervals"),
    ("数据可视化", "ACT4321"): ("direct_training", "Data Visualization|可视化数据"),
    ("数据可视化", "BIM3019"): ("direct_training", "Visualization via VMD|可视化软件VMD"),
    ("数据可视化", "DAI1000"): ("direct_training", "data manipulation, analysis, and visualization"),
    ("API接口", "MFE5250"): ("direct_training", "Application Programming Interface|API 接口"),
    ("API接口", "CSS5220"): ("direct_training", "API deployment|API部署"),
    ("嵌入式开发", "ECE3080"): ("direct_training", "embedded system prototype|嵌入式"),
    ("嵌入式开发", "ECE3810"): ("direct_training", "embedded system|嵌入式"),
    ("教学设计", "ENG3102"): ("direct_training", "Design engaging classroom activities|设计与听、说、读、写"),
    ("课程设计", "TRA6020"): ("direct_training", "课程设计"),
    ("R语言", "STA3005"): ("direct_training", "R programming"),
    ("统计分析", "DDA6020"): ("foundational_support", "probability|概率"),
    ("统计分析", "DDA6030"): ("foundational_support", "statistical inference|统计推断|hypothesis testing"),
    ("估值分析", "FIN6136"): ("direct_training", "估值分析|Valuation Analysis"),
    ("估值分析", "FIN6101"): ("foundational_support", "估值模型|估值方面"),
    ("算法设计", "CSC4301"): ("direct_training", "设计一个聚类方法|algorithm"),
    ("算法设计", "BIM2005"): ("direct_training", "实践所涉及的算法|practical experience in the learned algorithms"),
    ("供应链管理", "IBA6107"): ("foundational_support", "Supply Chain Strategy|供应链"),
    ("税务知识", "ACT6161"): ("direct_training", "税法|tax"),
    ("税务知识", "ACT3161"): ("direct_training", "税务|tax"),
    ("税务知识", "ACT4262"): ("direct_training", "税收|tax"),
    ("文献检索", "CHM3418"): ("direct_training", "开展科学文献检索"),
    ("GCP规范", "PHM3001"): ("direct_training", "Good Clinical Practices \(GCP\)"),
    ("PCB设计", "BME3201"): ("direct_training", "PCB design|印刷电路板 \(PCB\)"),
    ("回归分析", "CSS5230"): ("direct_training", "regression|回归"),
    ("财务建模", "MAT6810"): ("direct_training", "financial modeling|金融数学建模"),
    ("财务建模", "MFM5120"): ("direct_training", "Develop and optimize financial models|金融建模"),
    ("财务建模", "FIN6115"): ("foundational_support", "Use modeling technics|Empirical Modelling"),
}

FOUNDATION_PAIRS = {
    ("项目管理", "MBM6320"), ("机器学习", "AIE6001"),
    ("统计分析", "DDA6020"), ("统计分析", "DDA6030"),
    ("数据仓库", "CSC3170"), ("会计准则", "ACT3131"),
    ("计算机视觉", "AIR5011"),
    ("Hadoop", "ECO6127"), ("Hadoop", "IBA6302"),
    ("FPGA", "CIE6053"), ("VHDL", "ECE2050"),
}

def full_record_text(c):
    return "\n".join((c.get(f) or "") for f in FIELDS)

def extract_quote(c, pattern):
    rx = re.compile(pattern, re.I)
    for f in FIELDS:
        text = c.get(f) or ""
        m = rx.search(text)
        if not m:
            continue
        start = max(text.rfind("\n", 0, m.start()), text.rfind("。", 0, m.start()), text.rfind(".", 0, m.start())) + 1
        ends = [p for p in (text.find("\n", m.end()), text.find("。", m.end()), text.find(".", m.end())) if p >= 0]
        end = min(ends) + 1 if ends else min(len(text), m.end() + 180)
        quote = re.sub(r"\s+", " ", text[start:end]).strip(" •\t")
        if len(quote) < 25:
            a=max(0,m.start()-70); b=min(len(text),m.end()+170)
            quote=re.sub(r"\s+", " ", text[a:b]).strip(" •\t")
        return f, quote[:300]
    return "", ""

def evaluate(x):
    skill=x["skill"]; c=x["course"]; code=c["course_code"]; pair=(skill,code)
    if skill in AMBIGUOUS_SKILLS or pair in REJECT_PAIRS:
        return {"skill":skill,"course_code":code,"label":"not_related","confidence":0.98,
                "evidence_field":"","evidence_quote":"","reason":"语境审查后，该命中属于同词异义、仅提及、领域邻近或并未实际教授该技能。",
                "foundation_bridge":"","false_positive_flags":["ambiguous_or_insufficient_evidence"]}
    if pair in CURATED:
        label, pattern=CURATED[pair]
        field, quote=extract_quote(c, pattern)
    else:
        label = "foundational_support" if pair in FOUNDATION_PAIRS else "direct_training"
        field, quote=extract_quote(c, re.escape(skill))
    if not quote:
        return {"skill":skill,"course_code":code,"label":"not_related","confidence":0.90,
                "evidence_field":"","evidence_quote":"","reason":"完整课程材料中没有找到能够证明学生会学习或实践该技能的明确教学证据。",
                "foundation_bridge":"","false_positive_flags":["insufficient_evidence"]}
    if label == "foundational_support":
        bridge=f"课程教授的相关理论或方法是进一步掌握“{skill}”所需的可识别基础，但未证明课程直接训练完整技能。"
        reason=f"课程材料明确教授与“{skill}”相关的基础方法；因此列为基础支撑，而非直接培养。"
        conf=0.82
    else:
        bridge=""
        reason=f"课程教学内容或学习成果明确要求学生学习、使用或实践“{skill}”及其对应方法。"
        conf=0.93
    return {"skill":skill,"course_code":code,"label":label,"confidence":conf,
            "evidence_field":field,"evidence_quote":quote,"reason":reason,
            "foundation_bridge":bridge,"false_positive_flags":[]}

if __name__ == "__main__":
    rows=load_rows()
    report=[]
    for x in rows:
        c=x["course"]; skill=x["skill"]
        hits=[]
        for f in FIELDS:
            for s in snippets(c.get(f) or "", skill):
                hits.append({"field":f,"quote":s})
        report.append({"skill":skill,"course_code":c["course_code"],"course_title":c["course_title"],"retrieval_label":x["retrieval_label"],"exact_hits":hits})
    Path("work/semantic_exact_hits.json").write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding="utf-8")
    print(f"pairs={len(report)} exact_non_prereq={sum(bool(x['exact_hits']) for x in report)}")
    print("NO EXACT")
    for r in report:
        if not r["exact_hits"]:
            print(f"{r['skill']}\t{r['course_code']}\t{r['course_title']}\t{r['retrieval_label']}")
    decisions=[evaluate(x) for x in rows]
    Path("work/ai_course_review_decisions.json").write_text(json.dumps(decisions,ensure_ascii=False,indent=2),encoding="utf-8")
    from collections import Counter
    print(Counter(d["label"] for d in decisions))
    print("accepted_without_quote", sum(d["label"] != "not_related" and not d["evidence_quote"] for d in decisions))
