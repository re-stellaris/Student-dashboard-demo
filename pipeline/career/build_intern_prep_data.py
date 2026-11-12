import json
from collections import defaultdict
from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parents[1]
JOB_DIR = ROOT / "job-hunting-module" / "releases"
MAPPING_BOOK = JOB_DIR / "职位关键词-工作技能映射.xlsx"
COURSE_BOOK = JOB_DIR / "职位技能-校内课程推荐_AI语义审核版.xlsx"
OUT = ROOT / "students-interface" / "intern_prep_data.js"


def clean(value):
    if value is None or (isinstance(value, float) and pd.isna(value)):
        return ""
    text = str(value).strip()
    return "" if text.lower() == "nan" else text


def num(value):
    try:
        if pd.isna(value):
            return 0
        return float(value)
    except Exception:
        return 0


def course_href(code):
    text = clean(code).upper().replace(" ", "")
    letters = "".join(ch for ch in text if ch.isalpha())
    digits = text[len(letters) :]
    return f"{letters} {digits}".strip()


def main():
    map_rows = pd.read_excel(MAPPING_BOOK, sheet_name=0, header=2).fillna("")
    overview = pd.read_excel(MAPPING_BOOK, sheet_name=1, header=2).fillna("")
    recs = pd.read_excel(COURSE_BOOK, sheet_name=0, header=2).fillna("")

    categories = sorted(clean(x) for x in overview["一级类别"].dropna().unique() if clean(x))
    output = {
        "generatedFrom": [
            "job-hunting-module/releases/职位关键词-工作技能映射.xlsx",
            "job-hunting-module/releases/职位技能-校内课程推荐_AI语义审核版.xlsx",
        ],
        "dataDate": "2026.7.1",
        "sourceName": "实习僧",
        "defaultCategory": "产品" if "产品" in categories else (categories[0] if categories else ""),
        "categories": {},
    }

    for category in categories:
        ov = overview[overview["一级类别"].map(clean) == category]
        mr = map_rows[map_rows["一级类别"].map(clean) == category]
        cr = recs[recs["一级类别"].map(clean) == category]

        role_scores = []
        for _, row in ov.iterrows():
            keyword = clean(row.get("职位关键词"))
            if not keyword:
                continue
            k_rows = mr[mr["职位关键词"].map(clean) == keyword]
            mention_total = int(k_rows["提及JD数"].map(num).sum()) if not k_rows.empty else 0
            sample_jds = int(num(row.get("JD样本数")))
            skill_count = int(num(row.get("识别技能数")))
            role_scores.append({
                "keyword": keyword,
                "category": category,
                "sampleJds": sample_jds,
                "skillCount": skill_count,
                "mentionTotal": mention_total,
                "topSkills": [x for x in clean(row.get("高频技能（前10项）")).replace("，", "、").split("、") if x][:10],
            })
        role_scores.sort(key=lambda x: (-x["mentionTotal"], -x["skillCount"], x["keyword"]))

        skill_stats = {}
        evidence_urls = {}
        for _, row in mr.iterrows():
            skill = clean(row.get("可培训技能"))
            if not skill:
                continue
            if skill not in skill_stats:
                skill_stats[skill] = {
                    "skill": skill,
                    "skillType": clean(row.get("技能类型")),
                    "mentionJds": 0,
                    "keywordCount": 0,
                    "coverageSum": 0.0,
                }
            skill_stats[skill]["mentionJds"] += int(num(row.get("提及JD数")))
            skill_stats[skill]["keywordCount"] += 1
            skill_stats[skill]["coverageSum"] += num(row.get("覆盖率"))
            url = clean(row.get("来源URL"))
            if url and skill not in evidence_urls:
                evidence_urls[skill] = url

        skills = []
        for skill, stat in skill_stats.items():
            stat["avgCoverage"] = round(stat["coverageSum"] / max(1, stat["keywordCount"]), 3)
            stat["sourceUrl"] = evidence_urls.get(skill, "")
            del stat["coverageSum"]
            skills.append(stat)
        skills.sort(key=lambda x: (-x["mentionJds"], -x["keywordCount"], x["skill"]))

        direct = defaultdict(lambda: {"score": 0.0, "count": 0, "skills": set(), "urls": set(), "courseCode": "", "courseNameEn": "", "courseNameZh": "", "latestTerm": ""})
        support = defaultdict(lambda: {"score": 0.0, "count": 0, "skills": set(), "urls": set(), "courseCode": "", "courseNameEn": "", "courseNameZh": "", "latestTerm": ""})
        relation_buckets = {"直接培养": direct, "基础支撑": support}

        for _, row in cr.iterrows():
            relation = clean(row.get("培养关系"))
            bucket = relation_buckets.get(relation)
            if bucket is None:
                continue
            code = clean(row.get("课程代码"))
            if not code:
                continue
            rec = bucket[code]
            rec["courseCode"] = course_href(code)
            rec["courseNameEn"] = clean(row.get("课程英文名"))
            rec["courseNameZh"] = clean(row.get("课程中文名"))
            rec["latestTerm"] = clean(row.get("最近开课学期"))
            rec["score"] += num(row.get("置信度"))
            rec["count"] += 1
            skill = clean(row.get("工作技能"))
            if skill:
                rec["skills"].add(skill)
            url = clean(row.get("JD证据链接"))
            if url:
                rec["urls"].add(url)

        def finalize_courses(bucket):
            rows = []
            for rec in bucket.values():
                avg = rec["score"] / max(1, rec["count"])
                rows.append({
                    "courseCode": rec["courseCode"],
                    "courseNameEn": rec["courseNameEn"],
                    "courseNameZh": rec["courseNameZh"],
                    "latestTerm": rec["latestTerm"],
                    "matchCount": rec["count"],
                    "avgConfidence": round(avg, 3),
                    "skills": sorted(rec["skills"])[:8],
                    "sourceUrls": sorted(rec["urls"])[:5],
                })
            rows.sort(key=lambda x: (-x["matchCount"], -x["avgConfidence"], x["courseCode"]))
            return rows

        direct_courses = finalize_courses(direct)
        support_courses = finalize_courses(support)

        output["categories"][category] = {
            "roleSignals": role_scores[:12],
            "skills": skills[:18],
            "directCourses": direct_courses[:8],
            "supportCourses": support_courses[:8],
            "supportSkills": [x["skill"] for x in skills if x.get("skillType") in ("统计与分析", "工具", "编程语言", "产品与设计")][:10],
            "sourceUrls": sorted({u for x in skills[:10] for u in [x.get("sourceUrl", "")] if u})[:8],
        }

    OUT.write_text(
        "window.INTERN_PREP_DATA = "
        + json.dumps(output, ensure_ascii=False, separators=(",", ":"))
        + ";\n",
        encoding="utf-8",
    )
    print(f"Wrote {OUT} ({len(output['categories'])} categories)")


if __name__ == "__main__":
    main()
