from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

course_payload = json.loads((ROOT / "data/processed/course_data.json").read_text(encoding="utf-8"))
courses = course_payload["rows"] if isinstance(course_payload, dict) else course_payload
skills = json.loads((ROOT / "data/processed/skill_mapping.json").read_text(encoding="utf-8"))


def compile_skill_patterns() -> dict[str, list[re.Pattern]]:
    out: dict[str, list[re.Pattern]] = {}
    for item in skills["lexicon"]:
        patterns = []
        for p in item["patterns"].split(" | "):
            try:
                patterns.append(re.compile(p, re.I))
            except re.error:
                pass
        out[item["skill"]] = patterns
    return out


lex = compile_skill_patterns()
skill_type = {x["skill"]: x["skill_category"] for x in skills["lexicon"]}

related_raw = {
    "A/B Test": [
        r"hypothesis test",
        r"假设检验",
        r"实验设计",
        r"controlled experiment",
        r"randomi[sz]ed experiment",
        r"随机实验",
        r"因果推断",
    ],
    "用户研究": [
        r"用户访谈",
        r"问卷设计",
        r"定性研究",
        r"定量研究",
        r"usability",
        r"consumer behavior",
        r"survey",
    ],
    "需求分析": [r"requirements? analysis", r"systems analysis", r"business analysis", r"需求.*分析"],
    "数据可视化": [r"visuali[sz]ation", r"统计图", r"数据展示", r"data.*dashboard"],
    "统计分析": [r"statistics?", r"statistical analysis", r"统计推断", r"概率统计"],
    "机器学习": [r"statistical learning", r"predictive model", r"数据挖掘"],
    "项目管理": [r"project management", r"项目计划", r"项目控制"],
    "市场调研": [r"marketing research", r"consumer research", r"市场研究"],
    "实验设计": [r"design of experiment", r"hypothesis test", r"实验设计"],
    "财务建模": [r"financial modeling", r"valuation", r"现金流折现", r"spreadsheet.*model"],
    "文案写作": [r"creative writing", r"business writing", r"写作"],
    "课程设计": [r"curriculum design", r"instructional design", r"课程开发"],
    "教学设计": [r"instructional design", r"pedagogy", r"教学法"],
}
related = {k: [re.compile(x, re.I) for x in v] for k, v in related_raw.items()}

strict_tools = {
    "Excel",
    "PowerPoint",
    "Word",
    "Tableau",
    "Power BI",
    "SPSS",
    "SAS",
    "Stata",
    "EViews",
    "MySQL",
    "Oracle",
    "PostgreSQL",
    "MongoDB",
    "Redis",
    "Hadoop",
    "Spark",
    "Hive",
    "TensorFlow",
    "PyTorch",
    "scikit-learn",
    "Pandas",
    "NumPy",
    "Axure",
    "Figma",
    "Sketch",
    "XMind",
    "MindManager",
    "Visio",
    "Jira",
    "Photoshop",
    "Illustrator",
    "InDesign",
    "After Effects",
    "Premiere",
    "Final Cut Pro",
    "AutoCAD",
    "SolidWorks",
    "3ds Max",
    "Maya",
    "Blender",
    "Cinema 4D",
    "Linux",
    "Git",
    "Docker",
    "Kubernetes",
    "MATLAB",
    "Simulink",
    "LabVIEW",
    "Altium Designer",
    "Cadence",
    "Wind",
    "Bloomberg",
}


def text(course: dict, key: str) -> str:
    return str(course.get(key) or "")


def hits(patterns: list[re.Pattern], value: str) -> bool:
    return any(p.search(value) for p in patterns)


def compact(value: str) -> str:
    return re.sub(r"\s+", " ", str(value or "")).strip()


def snippet(label: str, value: str, patterns: list[re.Pattern], fallback: bool = False) -> str:
    clean = compact(value)
    if not clean:
        return ""
    for pattern in patterns:
        match = pattern.search(clean)
        if match:
            start = max(0, match.start() - 70)
            end = min(len(clean), match.end() + 100)
            prefix = "…" if start > 0 else ""
            suffix = "…" if end < len(clean) else ""
            return f"[{label}] {prefix}{clean[start:end]}{suffix}"
    if fallback:
        return f"[{label}] {clean[:180]}{'…' if len(clean) > 180 else ''}"
    return ""


def course_evidence(
    patterns: list[re.Pattern],
    rel_patterns: list[re.Pattern],
    title: str,
    outcomes: str,
    syllabus: str,
    description: str,
    assessment: str,
) -> str:
    for label, value in [
        ("课程名称", title),
        ("学习成果", outcomes),
        ("教学大纲", syllabus),
        ("课程简介", description),
        ("考核方式", assessment),
    ]:
        found = snippet(label, value, patterns)
        if found:
            return found
    if rel_patterns:
        for label, value in [
            ("课程名称/相关词", title),
            ("学习成果/相关词", outcomes),
            ("教学大纲/相关词", syllabus),
            ("课程简介/相关词", description),
        ]:
            found = snippet(label, value, rel_patterns)
            if found:
                return found
    # Extremely rare fallback: keep the row auditable even when the relationship
    # came from a broad semantic/synonym rule and no short exact snippet was found.
    return snippet("教学大纲", syllabus, patterns, fallback=True) or snippet("课程简介", description, patterns, fallback=True)


def recent_terms(value: str) -> str:
    terms = [x.strip() for x in str(value or "").splitlines() if x.strip()]

    def key(term: str):
        match = re.search(r"(\d{4})-(\d{2}).*?Term\s*(\d+)", term, re.I)
        return (int(match.group(1)), int(match.group(3))) if match else (0, 0)

    return "；".join(sorted(set(terms), key=key, reverse=True)[:3])


def relation_for(skill: str, course: dict) -> tuple[int, str, str] | None:
    patterns = lex.get(skill, [])
    rel_patterns = related.get(skill, [])
    title = " ".join([text(course, "course_title"), text(course, "course_title_en")])
    outcomes = text(course, "learning_outcomes")
    syllabus = text(course, "course_syllabus")
    description = " ".join(
        [text(course, "detail_description"), text(course, "description_chinese"), text(course, "description_english")]
    )
    assessment = text(course, "assessment_scheme")
    prerequisites = text(course, "prerequisites")

    h_title = hits(patterns, title)
    h_outcomes = hits(patterns, outcomes)
    h_syllabus = hits(patterns, syllabus)
    h_description = hits(patterns, description)
    h_assessment = hits(patterns, assessment)
    h_prereq = hits(patterns, prerequisites)
    h_related = hits(rel_patterns, " ".join([title, outcomes, syllabus, description])) if rel_patterns else False

    if not any([h_title, h_outcomes, h_syllabus, h_description, h_assessment, h_related]):
        return None

    teaching_text = " ".join([outcomes, syllabus, description]).lower()
    if skill_type.get(skill) == "编程语言" and (
        "will not teach programming" in teaching_text or "不教授编程" in teaching_text
    ):
        return None

    practical = bool(
        re.search(
            r"project|lab|实验|编程|programming|assignment|作业|实践|case|案例|presentation|报告",
            assessment + " " + syllabus,
            re.I,
        )
    )
    strict = skill_type.get(skill) == "编程语言" or skill in strict_tools
    if strict and not (h_title or ((h_outcomes or h_syllabus) and practical)):
        return None

    # prerequisite-only mentions should not count as training.
    if h_prereq and not any([h_title, h_outcomes, h_syllabus, h_description, h_assessment, h_related]):
        return None

    score = 35 * h_title + 30 * h_outcomes + 25 * h_syllabus + 14 * h_description + 5 * h_assessment
    score += 8 * practical + 10 * h_related

    if h_title or ((h_outcomes or h_syllabus or h_related) and practical):
        relation = "直接培养"
    elif h_outcomes or h_syllabus or h_related:
        relation = "基础支撑"
    else:
        relation = "间接相关"
    evidence = course_evidence(patterns, rel_patterns, title, outcomes, syllabus, description, assessment)
    return int(score), relation, evidence


def main() -> None:
    unique_skills = sorted({r["skill"] for r in skills["rows"]})
    recommend: dict[str, list[tuple[int, str, str, dict]]] = {}

    for skill in unique_skills:
        candidates = []
        for course in courses:
            result = relation_for(skill, course)
            if result is None:
                continue
            score, relation, evidence = result
            candidates.append((score, relation, evidence, course))

        selected: list[tuple[int, str, str, dict]] = []
        for relation, limit in [("直接培养", 2), ("基础支撑", 2), ("间接相关", 1)]:
            selected.extend(sorted([x for x in candidates if x[1] == relation], key=lambda x: -x[0])[:limit])
        recommend[skill] = selected

    rows = []
    seen = set()
    for source_row in skills["rows"]:
        for score, relation, course_evidence_text, course in recommend.get(source_row["skill"], []):
            key = (
                source_row["category"],
                source_row["keyword"],
                source_row["skill"],
                relation,
                course.get("course_code"),
            )
            if key in seen:
                continue
            seen.add(key)
            rows.append(
                {
                    "category": source_row["category"],
                    "keyword": source_row["keyword"],
                    "skill": source_row["skill"],
                    "jd_source_url": source_row.get("source_url") or "",
                    "relation": relation,
                    "course_evidence": course_evidence_text,
                    "course_code": course.get("course_code") or "",
                    "course_name_zh": course.get("course_title") or "",
                    "course_name_en": course.get("course_title_en") or "",
                    "offered_terms": recent_terms(course.get("offered_terms") or ""),
                }
            )

    payload = {
        "rows": rows,
        "stats": {
            "skills": len(unique_skills),
            "recommended_skills": sum(bool(v) for v in recommend.values()),
            "rows": len(rows),
            "courses": len(courses),
            "official_title_matches": course_payload.get("stats", {}).get("official_title_matches", 0),
            "course_source": "sis_course_outlines_export.xlsx / With Outline / course_syllabus 非空",
            "english_title_source": "Course_List_byInitial.xlsx / course_title.1",
        },
    }
    (ROOT / "work/course_recommendations.json").write_text(
        json.dumps(payload, ensure_ascii=False), encoding="utf-8"
    )
    print(json.dumps(payload["stats"], ensure_ascii=False))


if __name__ == "__main__":
    main()
