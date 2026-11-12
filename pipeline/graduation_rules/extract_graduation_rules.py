from __future__ import annotations

import argparse
import json
import re
from collections import defaultdict
from dataclasses import dataclass, asdict
from pathlib import Path


try:
    import pdfplumber
except Exception:  # pragma: no cover
    pdfplumber = None

try:
    from pypdf import PdfReader
except Exception:  # pragma: no cover
    PdfReader = None


YEAR_LIMIT = 2026
COURSE_RE = re.compile(r"\b[A-Z]{2,5}\s?\d{4}[A-Z]?\b")
KEYWORDS = [
    "Major Requirements",
    "Major Programme Requirement",
    "Major Required Courses",
    "Required Courses",
    "Required Major Courses",
    "Required Programme Courses",
    "Elective Courses",
    "Major Elective Courses",
    "University Core",
    "General Education",
    "Chinese",
    "English",
    "Physical Education",
    "U-Core",
    "UCore",
    "Units",
    "Credits",
]


@dataclass
class RuleDoc:
    school: str
    major: str
    program_title: str
    pdf: str
    relative_pdf: str
    admitted_years: list[int]
    title_scope: str
    total_units_candidates: list[str]
    course_codes: list[str]
    mandatory_candidates: list[str]
    elective_candidates: list[str]
    ucore_candidates: list[str]
    evidence_snippets: list[str]


def read_pdf_text(path: Path) -> str:
    if pdfplumber is not None:
        parts: list[str] = []
        with pdfplumber.open(path) as pdf:
            for page in pdf.pages:
                parts.append(page.extract_text(x_tolerance=1, y_tolerance=3) or "")
        return "\n".join(parts)
    if PdfReader is not None:
        reader = PdfReader(str(path))
        return "\n".join((page.extract_text() or "") for page in reader.pages)
    raise RuntimeError("Install pdfplumber or pypdf to extract PDF text.")


def clean_text(text: str) -> str:
    text = text.replace("\u00a0", " ")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def normalize_year(two_or_four: str) -> int:
    value = int(two_or_four)
    if value < 100:
        return 2000 + value
    return value


def filename_scope(path: Path) -> str:
    name = path.stem
    parent = path.parent.name
    return f"{parent} {name}"


def normalize_path_name(name: str) -> str:
    if name == "���ڹ���":
        return "Joint"
    if name == "鏂板缓鏂囦欢澶?":
        return "Pharmacy"
    return name


def extract_program_title(text: str, fallback: str) -> str:
    patterns = [
        r"Programme Title\s+(.+?)\s+Study Scheme",
        r"Program Title\s+(.+?)\s+Study Scheme",
        r"Programme Title\s+(.+?)\s+Major Programme Requirement",
    ]
    for pattern in patterns:
        match = re.search(pattern, text, re.I | re.S)
        if match:
            title = re.sub(r"\s+", " ", match.group(1)).strip()
            if 2 <= len(title) <= 120:
                return title
    return fallback


def admission_years_from_title(path: Path) -> tuple[list[int], str]:
    scope = filename_scope(path)
    compact = scope.replace("_", " ").replace("-", "-")
    years: set[int] = set()

    # 2020-21 to 2024-25
    for m in re.finditer(r"20(\d{2})\s*-\s*(\d{2})\s*(?:to|至|到)\s*20?(\d{2})\s*-\s*(\d{2})", compact, re.I):
        start = 2000 + int(m.group(1))
        end = 2000 + int(m.group(3))
        for y in range(max(start, 2023), min(end, YEAR_LIMIT) + 1):
            years.add(y)

    # 2023-24 and thereafter
    for m in re.finditer(r"20(\d{2})\s*[-–—]\s*(\d{2})(?:\s*(?:and|及|&)?\s*(?:thereafter|以后|及以后|onward|onwards))", compact, re.I):
        start = 2000 + int(m.group(1))
        for y in range(max(start, 2023), YEAR_LIMIT + 1):
            years.add(y)

    # 2024-25 and 2025-26
    for m in re.finditer(r"20(\d{2})\s*[-–—]\s*(\d{2})", compact):
        start = 2000 + int(m.group(1))
        if start >= 2023:
            years.add(start)

    # 2023-2024
    for m in re.finditer(r"20(2[3-9])\s*[-–—]\s*20(2[4-9])", compact):
        years.add(2000 + int(m.group(1)))

    # Explicit "2023, 2024, 2025" style dates in filenames.
    if not years:
        for m in re.finditer(r"\b20(2[3-9])\b", compact):
            y = 2000 + int(m.group(1))
            if 2023 <= y <= YEAR_LIMIT:
                years.add(y)

    return sorted(years), scope


def path_school_major(root: Path, pdf: Path) -> tuple[str, str]:
    rel = pdf.relative_to(root)
    parts = rel.parts
    if len(parts) >= 3:
        return normalize_path_name(parts[0]), normalize_path_name(parts[1])
    if len(parts) >= 2:
        return normalize_path_name(parts[0]), normalize_path_name(parts[0])
    return "Unknown", pdf.stem


def extract_snippets(text: str, keywords: list[str], window: int = 520) -> list[str]:
    snippets: list[str] = []
    low = text.lower()
    for kw in keywords:
        idx = low.find(kw.lower())
        if idx < 0:
            continue
        start = max(0, idx - 160)
        end = min(len(text), idx + window)
        snippet = re.sub(r"\s+", " ", text[start:end]).strip()
        if snippet and snippet not in snippets:
            snippets.append(snippet)
    return snippets[:12]


def unit_candidates(text: str) -> list[str]:
    patterns = [
        r"(?:Major|Programme|Program|毕业|专业)[^\n]{0,80}?(?:Units|Credits|学分)[^\n]{0,40}",
        r"(?:Total|Minimum|at least)[^\n]{0,60}?(?:\d{2,3})\s*(?:units|credits)",
        r"(?:\d{2,3})\s*(?:units|credits)[^\n]{0,80}?(?:Major|Programme|Program|Requirement)",
    ]
    out: list[str] = []
    for pat in patterns:
        for m in re.finditer(pat, text, re.I):
            item = re.sub(r"\s+", " ", m.group(0)).strip()
            if item not in out:
                out.append(item)
    return out[:20]


def lines_with_courses(text: str, section_words: list[str]) -> list[str]:
    lines = [re.sub(r"\s+", " ", line).strip() for line in text.splitlines()]
    out: list[str] = []
    active = False
    budget = 0
    for line in lines:
        low = line.lower()
        if any(w.lower() in low for w in section_words):
            active = True
            budget = 18
            out.append(line)
            continue
        if active and budget > 0:
            if COURSE_RE.search(line) or re.search(r"\b\d+(?:\.\d+)?\s*(?:units|credits)\b", line, re.I):
                out.append(line)
            budget -= 1
        if len(out) >= 40:
            break
    return out


def analyze_pdf(root: Path, pdf: Path) -> RuleDoc:
    school, major = path_school_major(root, pdf)
    years, scope = admission_years_from_title(pdf)
    text = clean_text(read_pdf_text(pdf))
    program_title = extract_program_title(text, major)
    codes = sorted({c.replace(" ", "") for c in COURSE_RE.findall(text)})
    return RuleDoc(
        school=school,
        major=major,
        program_title=program_title,
        pdf=pdf.name,
        relative_pdf=str(pdf.relative_to(root)),
        admitted_years=years,
        title_scope=scope,
        total_units_candidates=unit_candidates(text),
        course_codes=codes,
        mandatory_candidates=lines_with_courses(text, ["Required Courses", "Major Required", "Required Major", "Core Courses", "Compulsory"]),
        elective_candidates=lines_with_courses(text, ["Elective", "Major Elective", "Area", "Concentration", "Stream"]),
        ucore_candidates=lines_with_courses(text, ["University Core", "General Education", "Chinese", "English", "Physical Education", "U-Core", "UCore"]),
        evidence_snippets=extract_snippets(text, KEYWORDS),
    )


def append_doc_summary(lines: list[str], doc: RuleDoc) -> None:
    title = doc.program_title if doc.program_title != doc.major else doc.major
    lines.append(f"### {doc.school} / {title}")
    lines.append(f"- Source: `{doc.relative_pdf}`")
    lines.append(f"- Title scope: {doc.title_scope}")
    lines.append(f"- Parsed admission years: {', '.join(str(y) for y in doc.admitted_years) if doc.admitted_years else 'UNKNOWN'}")
    lines.append(f"- Course codes found: {', '.join(doc.course_codes[:100]) if doc.course_codes else '-'}")
    if doc.total_units_candidates:
        lines.append("- Unit / credit clues:")
        for item in doc.total_units_candidates[:10]:
            lines.append(f"  - {item}")
    if doc.mandatory_candidates:
        lines.append("- Mandatory / core clues:")
        for item in doc.mandatory_candidates[:14]:
            lines.append(f"  - {item}")
    if doc.elective_candidates:
        lines.append("- Elective / area clues:")
        for item in doc.elective_candidates[:14]:
            lines.append(f"  - {item}")
    if doc.ucore_candidates:
        lines.append("- U-core / university requirement clues:")
        for item in doc.ucore_candidates[:14]:
            lines.append(f"  - {item}")
    if doc.evidence_snippets:
        lines.append("- Evidence snippets:")
        for item in doc.evidence_snippets[:3]:
            lines.append(f"  - {item}")
    lines.append("")


def write_markdown(docs: list[RuleDoc], output: Path, title: str) -> None:
    by_year: dict[int, list[RuleDoc]] = defaultdict(list)
    no_year: list[RuleDoc] = []
    for doc in docs:
        if doc.admitted_years:
            for year in doc.admitted_years:
                by_year[year].append(doc)
        else:
            no_year.append(doc)

    lines: list[str] = [f"# {title}", ""]
    lines.append("This is the first-pass rule extraction for manual checking. It favors recall over final precision.")
    lines.append("")
    for year in sorted(by_year):
        lines.append(f"## Admitted In {year}")
        lines.append("")
        for doc in sorted(by_year[year], key=lambda d: (d.school, d.program_title, d.pdf)):
            append_doc_summary(lines, doc)

    if no_year:
        lines.append("## No Admission Year Parsed")
        lines.append("")
        for doc in sorted(no_year, key=lambda d: (d.school, d.program_title, d.pdf)):
            append_doc_summary(lines, doc)

    output.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", required=True)
    parser.add_argument("--out-dir", default="scratch/graduation_rules")
    parser.add_argument("--skip-school", action="append", default=["MUS"])
    args = parser.parse_args()

    root = Path(args.root)
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    docs: list[RuleDoc] = []
    errors: list[dict[str, str]] = []
    for pdf in sorted(root.rglob("*.pdf")):
        school, _major = path_school_major(root, pdf)
        if school in set(args.skip_school or []):
            continue
        try:
            docs.append(analyze_pdf(root, pdf))
        except Exception as exc:
            errors.append({"pdf": str(pdf), "error": str(exc)})

    (out_dir / "graduation_rules_extracted.json").write_text(
        json.dumps([asdict(d) for d in docs], ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    (out_dir / "graduation_rules_errors.json").write_text(
        json.dumps(errors, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    major_docs = [doc for doc in docs if doc.school != "UcoreForAllUndergrad"]
    ucore_docs = [doc for doc in docs if doc.school == "UcoreForAllUndergrad"]
    write_markdown(docs, out_dir / "graduation_rules_review.md", "Graduation Rule Extraction Draft")
    write_markdown(major_docs, out_dir / "major_rules_review.md", "Major Rule Review Draft")
    write_markdown(ucore_docs, out_dir / "ucore_rules_review.md", "U-core Rule Review Draft")
    print(f"PDFs parsed: {len(docs)}")
    print(f"Errors: {len(errors)}")
    print(f"Output: {out_dir}")


if __name__ == "__main__":
    main()
