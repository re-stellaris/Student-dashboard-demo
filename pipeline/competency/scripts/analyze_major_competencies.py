from __future__ import annotations

import csv
import re
import zipfile
from collections import OrderedDict
from pathlib import Path
from tempfile import TemporaryDirectory

from pypdf import PdfReader


ZIP_PATH = Path("outputs/registry_curricula/Major Programmes.zip")
OUT_CSV = Path("outputs/major_competency_summary.csv")
OUT_PROGRAMME_CSV = Path("outputs/major_competency_programme_summary.csv")
OUT_MD = Path("outputs/major_competency_summary.md")


COMPETENCY_RULES = OrderedDict(
    [
        (
            "Foundational disciplinary knowledge",
            [
                "principles",
                "introduction",
                "foundation",
                "theory",
                "概论",
                "导论",
                "原理",
                "理论",
                "基础",
            ],
        ),
        (
            "Mathematical or quantitative reasoning",
            [
                "mathematics",
                "calculus",
                "linear algebra",
                "probability",
                "optimization",
                "mathematical",
                "quantitative",
                "数学",
                "微积分",
                "线性代数",
                "概率",
                "优化",
            ],
        ),
        (
            "Statistical reasoning and empirical analysis",
            [
                "statistics",
                "statistical",
                "regression",
                "econometrics",
                "inference",
                "survey",
                "experimental",
                "统计",
                "回归",
                "计量",
                "推断",
                "调查",
            ],
        ),
        (
            "Programming and computing ability",
            [
                "programming",
                "computer programming",
                "computing",
                "software",
                "algorithm",
                "data structures",
                "c/c++",
                "python",
                "java",
                "程序设计",
                "编程",
                "计算",
                "软件",
                "算法",
                "数据结构",
            ],
        ),
        (
            "Data, digital, and AI literacy",
            [
                "data",
                "database",
                "big data",
                "machine learning",
                "deep learning",
                "artificial intelligence",
                "natural language processing",
                "large language model",
                "analytics",
                "visualization",
                "数据",
                "数据库",
                "大数据",
                "机器学习",
                "深度学习",
                "人工智能",
                "自然语言处理",
                "大语言模型",
                "可视化",
            ],
        ),
        (
            "Research and methodological ability",
            [
                "research",
                "method",
                "methodology",
                "laboratory",
                "experiment",
                "thesis",
                "capstone",
                "project",
                "seminar",
                "研究",
                "方法",
                "实验",
                "实验室",
                "论文",
                "项目",
                "研讨",
            ],
        ),
        (
            "Communication, writing, and presentation",
            [
                "communication",
                "writing",
                "presentation",
                "academic writing",
                "corporate communication",
                "public relations",
                "传播",
                "传意",
                "写作",
                "表达",
                "汇报",
                "企业传讯",
                "公共关系",
            ],
        ),
        (
            "Language, textual, and cross-cultural competence",
            [
                "language",
                "translation",
                "interpreting",
                "linguistics",
                "phonetics",
                "semantics",
                "culture",
                "literary",
                "textual",
                "foreign language",
                "语言",
                "翻译",
                "传译",
                "语义",
                "语音",
                "文化",
                "文学",
                "文本",
                "第二外语",
            ],
        ),
        (
            "Domain-specific professional knowledge",
            [
                "finance",
                "financial",
                "accounting",
                "business",
                "economics",
                "marketing",
                "management",
                "legal",
                "law",
                "clinical",
                "medicine",
                "biomedical",
                "biology",
                "chemistry",
                "physics",
                "materials",
                "energy",
                "engineering",
                "urban",
                "governance",
                "policy",
                "medical",
                "金融",
                "财务",
                "会计",
                "商务",
                "经济",
                "营销",
                "管理",
                "法律",
                "医学",
                "生物",
                "化学",
                "物理",
                "材料",
                "能源",
                "工程",
                "城市",
                "治理",
                "政策",
            ],
        ),
        (
            "Practical application and industry readiness",
            [
                "internship",
                "practicum",
                "practice",
                "studio",
                "workshop",
                "field",
                "applied",
                "case",
                "professional",
                "clinical practice",
                "实习",
                "实践",
                "工作坊",
                "应用",
                "案例",
                "专业",
                "临床",
            ],
        ),
    ]
)


def clean(text: str) -> str:
    return re.sub(r"\s+", " ", text or "").strip()


def read_pdf_text(path: Path) -> str:
    reader = PdfReader(str(path))
    return "\n".join(page.extract_text() or "" for page in reader.pages)


def get_programme_name(text: str, fallback: str) -> str:
    patterns = [
        r"Programme Title\s*\n?\s*([^\n]+)",
        r"Major Programme Requirement of\s+([^\n]+)",
        r"Study Scheme\s*[-–]\s*([A-Za-z &/]+)",
        r"課程名稱\s*\n?\s*([^\n]+)",
    ]
    for pat in patterns:
        m = re.search(pat, text, flags=re.I)
        if m:
            name = clean(m.group(1))
            name = re.sub(r"\s*Study Scheme.*", "", name, flags=re.I).strip()
            if 2 <= len(name) <= 90:
                return name
    return fallback


def get_total_units(text: str) -> str:
    patterns = [
        r"Total:\s*(\d+)",
        r"Total \(Major Requirement[^)]*\):\s*(\d+)",
        r"minimum of\s+(\d+)\s+units",
        r"共[：:]\s*(\d+)",
        r"合共.*?[：:]\s*(\d+)",
    ]
    for pat in patterns:
        m = re.search(pat, text, flags=re.I | re.S)
        if m:
            return m.group(1)
    return ""


def extract_sections(text: str) -> dict[str, str]:
    section_patterns = {
        "required": r"(?:Major Required Courses|Required Courses|必修科目|主修必修科目)(.*?)(?:III\.|Major Elective|Elective Courses|選修|Recommended Course Pattern|Course List|$)",
        "elective": r"(?:Major Elective|Elective Courses|主修選修科目|選修科目)(.*?)(?:Recommended Course Pattern|Course List|$)",
        "streams": r"(?:Streams of Specialization|stream|concentration|專修範疇|专修)(.*?)(?:Recommended Course Pattern|Course List|$)",
        "course_list": r"(?:Course List)(.*)$",
    }
    out = {}
    for key, pat in section_patterns.items():
        m = re.search(pat, text, flags=re.I | re.S)
        out[key] = m.group(1) if m else ""
    return out


def extract_course_evidence(text: str) -> list[str]:
    lines = []
    for raw in text.splitlines():
        line = clean(raw)
        if not line:
            continue
        if re.search(r"\b[A-Z]{2,5}\d{4}\b", line) or re.search(r"\b[A-Z]{2,5}\d{4}/\s*[A-Z]{2,5}\d{4}\b", line):
            # Keep compact course-like rows only.
            if len(line) < 220 and not re.search(r"Last Update|Course Code|Unit\(s\)|學分", line, flags=re.I):
                lines.append(line)
    seen = set()
    unique = []
    for line in lines:
        if line not in seen:
            seen.add(line)
            unique.append(line)
    return unique[:120]


def infer_competencies(text: str, course_lines: list[str]) -> list[dict[str, str]]:
    evidence_text = "\n".join(course_lines) if course_lines else text
    competencies = []
    for competency, keywords in COMPETENCY_RULES.items():
        hits = []
        for line in course_lines:
            low = line.lower()
            if any(k.lower() in low for k in keywords):
                hits.append(line)
        if not hits:
            low_text = evidence_text.lower()
            if any(k.lower() in low_text for k in keywords):
                hits = [f"Study scheme text contains related terms: {', '.join([k for k in keywords if k.lower() in low_text][:4])}"]

        if hits:
            status = "Core" if len(hits) >= 3 or any(re.search(r"required|必修|capstone|project", h, re.I) for h in hits) else "Supporting / Optional"
            interpretation = interpret_competency(competency)
            competencies.append(
                {
                    "competency": competency,
                    "status": status,
                    "evidence": "; ".join(hits[:5]),
                    "interpretation": interpretation,
                }
            )
    return competencies


def interpret_competency(competency: str) -> str:
    mapping = {
        "Foundational disciplinary knowledge": "Students are expected to build a structured foundation in the major's core concepts and disciplinary language.",
        "Mathematical or quantitative reasoning": "The curriculum expects students to handle mathematical, quantitative, or formal reasoning tasks.",
        "Statistical reasoning and empirical analysis": "Students are expected to work with empirical evidence, statistical models, or quantitative inference.",
        "Programming and computing ability": "Students are expected to use computational tools or programming to solve disciplinary problems.",
        "Data, digital, and AI literacy": "The programme includes data-centric, digital, analytics, or AI-related training.",
        "Research and methodological ability": "Students are expected to conduct projects, apply methods, or synthesize knowledge independently.",
        "Communication, writing, and presentation": "The curriculum cultivates academic, professional, or public communication abilities.",
        "Language, textual, and cross-cultural competence": "Students are expected to work with language, texts, translation, culture, or intercultural meaning.",
        "Domain-specific professional knowledge": "The programme prepares students for specialized disciplinary or professional domains.",
        "Practical application and industry readiness": "The curriculum includes applied, professional, practice-oriented, or workplace-facing training.",
    }
    return mapping.get(competency, "This competency is supported by the curriculum evidence.")


def summarize_structure(sections: dict[str, str]) -> str:
    parts = []
    if sections.get("required"):
        parts.append("required courses")
    if sections.get("elective"):
        parts.append("major electives")
    if sections.get("streams"):
        parts.append("streams/specializations")
    if not parts:
        return "programme requirements and course list"
    return ", ".join(parts)


def analyze_one(pdf_path: Path, major_folder: str) -> list[dict[str, str]]:
    text = read_pdf_text(pdf_path)
    sections = extract_sections(text)
    course_lines = extract_course_evidence(text)
    programme = get_programme_name(text, major_folder)
    total_units = get_total_units(text)
    competencies = infer_competencies(text, course_lines)
    if not competencies:
        competencies = [
            {
                "competency": "Foundational disciplinary knowledge",
                "status": "Insufficiently specified",
                "evidence": "Course evidence could not be reliably extracted from the PDF text.",
                "interpretation": "Manual review is recommended.",
            }
        ]

    rows = []
    for item in competencies:
        rows.append(
            {
                "programme": programme,
                "source_folder": major_folder,
                "total_major_units": total_units,
                "requirement_structure": summarize_structure(sections),
                "competency": item["competency"],
                "status": item["status"],
                "curriculum_evidence": item["evidence"],
                "interpretation": item["interpretation"],
                "source_pdf": str(pdf_path.name),
                "limitations": "Based on study scheme requirements and course titles; course descriptions and learning outcomes are not included unless present in the PDF.",
            }
        )
    return rows


def main() -> None:
    all_rows = []
    with TemporaryDirectory() as tmp:
        tmp_dir = Path(tmp)
        with zipfile.ZipFile(ZIP_PATH) as z:
            pdf_names = [n for n in z.namelist() if n.lower().endswith(".pdf")]
            z.extractall(tmp_dir, pdf_names)

        for rel in pdf_names:
            pdf = tmp_dir / rel
            parts = Path(rel).parts
            major_folder = parts[1] if len(parts) > 2 else pdf.parent.name
            all_rows.extend(analyze_one(pdf, major_folder))

    OUT_CSV.parent.mkdir(parents=True, exist_ok=True)
    headers = [
        "programme",
        "source_folder",
        "total_major_units",
        "requirement_structure",
        "competency",
        "status",
        "curriculum_evidence",
        "interpretation",
        "source_pdf",
        "limitations",
    ]
    with OUT_CSV.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=headers)
        writer.writeheader()
        writer.writerows(all_rows)

    grouped: OrderedDict[str, list[dict[str, str]]] = OrderedDict()
    for row in all_rows:
        grouped.setdefault(row["programme"], []).append(row)

    programme_headers = [
        "programme",
        "source_folder",
        "total_major_units",
        "requirement_structure",
        "core_competencies",
        "supporting_or_optional_competencies",
        "evidence_summary",
        "overall_interpretation",
        "limitations",
    ]
    programme_rows = []
    for programme, rows in grouped.items():
        core = [r["competency"] for r in rows if r["status"] == "Core"]
        support = [r["competency"] for r in rows if r["status"] != "Core"]
        evidence_bits = [f"{r['competency']}: {r['curriculum_evidence']}" for r in rows[:8]]
        if core:
            overall = f"This programme appears to train students primarily in {', '.join(core[:4])}."
        else:
            profile = ", ".join([r["competency"] for r in rows[:4]])
            overall = f"This programme appears to provide a profile centered on {profile}."
        programme_rows.append(
            {
                "programme": programme,
                "source_folder": rows[0]["source_folder"],
                "total_major_units": rows[0]["total_major_units"],
                "requirement_structure": rows[0]["requirement_structure"],
                "core_competencies": "; ".join(dict.fromkeys(core)),
                "supporting_or_optional_competencies": "; ".join(dict.fromkeys(support)),
                "evidence_summary": " || ".join(evidence_bits),
                "overall_interpretation": overall,
                "limitations": rows[0]["limitations"],
            }
        )

    with OUT_PROGRAMME_CSV.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=programme_headers)
        writer.writeheader()
        writer.writerows(programme_rows)

    lines = ["# Major Competency Summary", ""]
    lines.append("This table is generated from study scheme PDFs using `major_competency_identifier.skill`.")
    lines.append("")
    lines.append("| Programme | Total Units | Competency | Status | Evidence | Interpretation |")
    lines.append("|---|---:|---|---|---|---|")
    for row in all_rows:
        evidence = row["curriculum_evidence"].replace("|", "/")
        interp = row["interpretation"].replace("|", "/")
        lines.append(
            f"| {row['programme']} | {row['total_major_units']} | {row['competency']} | {row['status']} | {evidence} | {interp} |"
        )
    OUT_MD.write_text("\n".join(lines), encoding="utf-8")
    print(f"wrote {OUT_CSV} rows={len(all_rows)} programmes={len(grouped)}")
    print(f"wrote {OUT_PROGRAMME_CSV} rows={len(programme_rows)}")
    print(f"wrote {OUT_MD}")


if __name__ == "__main__":
    main()
