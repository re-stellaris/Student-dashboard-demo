from __future__ import annotations

import json
import re
import zipfile
from pathlib import Path
from tempfile import TemporaryDirectory

from pypdf import PdfReader


ZIP_PATH = Path("outputs/registry_curricula/Major Programmes.zip")
OUT_JSON = Path("outputs/study_scheme_evidence.json")


def clean(text: str) -> str:
    return re.sub(r"[ \t]+", " ", (text or "").replace("\r", "\n")).strip()


def read_pdf(path: Path) -> str:
    reader = PdfReader(str(path))
    return "\n".join(page.extract_text() or "" for page in reader.pages)


def programme_name(text: str, fallback: str) -> str:
    patterns = [
        r"Programme Title\s*\n+\s*([^\n]+)",
        r"Major Programme Requirement of\s+([^\n]+)",
        r"課程名稱\s*\n+\s*([^\n]+)",
        r"课程名称\s*\n+\s*([^\n]+)",
    ]
    for pat in patterns:
        m = re.search(pat, text, re.I)
        if m:
            name = clean(m.group(1))
            if 2 <= len(name) <= 100:
                return name
    return fallback


def total_units(text: str) -> str:
    patterns = [
        r"minimum of\s+(\d+)\s+units",
        r"Total(?:\s*\([^)]*\))?\s*:\s*(\d+)",
        r"共[：:]\s*(\d+)",
        r"合共.*?[：:]\s*(\d+)",
    ]
    for pat in patterns:
        m = re.search(pat, text, re.I | re.S)
        if m:
            return m.group(1)
    return ""


def section(text: str, start_patterns: list[str], end_patterns: list[str]) -> str:
    starts = []
    for pat in start_patterns:
        m = re.search(pat, text, re.I)
        if m:
            starts.append(m.start())
    if not starts:
        return ""
    start = min(starts)
    rest = text[start:]
    end = len(rest)
    for pat in end_patterns:
        m = re.search(pat, rest, re.I)
        if m and m.start() > 40:
            end = min(end, m.start())
    return rest[:end]


def course_rows(text: str) -> list[str]:
    rows = []
    for raw in text.splitlines():
        line = clean(raw)
        if not line:
            continue
        if re.search(r"\b[A-Z]{2,5}\d{4}\b", line) and len(line) < 240:
            if not re.search(r"Last Update|Course Code|Unit\(s\)|學分|学分", line, re.I):
                rows.append(line)
    seen = set()
    out = []
    for r in rows:
        if r not in seen:
            seen.add(r)
            out.append(r)
    return out


def extract_streams(text: str) -> str:
    return section(
        text,
        ["Streams of Specialization", "Concentration", "Stream", "專修範疇", "专修范畴"],
        ["Recommended Course Pattern", "Course List", "修課推介", "课程列表"],
    )


def summarize_pdf(pdf: Path, rel: str) -> dict:
    text = read_pdf(pdf)
    folder = Path(rel).parts[1] if len(Path(rel).parts) > 2 else pdf.parent.name
    rows = course_rows(text)
    required_block = section(
        text,
        ["Major Required Courses", "Required Courses", "必修科目", "主修必修科目"],
        ["Major Elective", "Elective Courses", "III\\.", "Recommended Course Pattern", "Course List"],
    )
    elective_block = section(
        text,
        ["Major Elective", "Elective Courses", "主修選修科目", "选修科目", "選修科目"],
        ["Recommended Course Pattern", "Course List", "修課推介"],
    )
    return {
        "source_pdf": rel,
        "source_folder": folder,
        "programme": programme_name(text, folder),
        "total_units": total_units(text),
        "streams_text": clean(extract_streams(text))[:2500],
        "required_text": clean(required_block)[:2500],
        "elective_text": clean(elective_block)[:3000],
        "course_rows": rows[:180],
    }


def main() -> None:
    items = []
    with TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        with zipfile.ZipFile(ZIP_PATH) as z:
            pdfs = [n for n in z.namelist() if n.lower().endswith(".pdf")]
            z.extractall(tmp_path, pdfs)
        for rel in pdfs:
            items.append(summarize_pdf(tmp_path / rel, rel))

    OUT_JSON.write_text(json.dumps(items, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"wrote {OUT_JSON} programmes={len(items)}")


if __name__ == "__main__":
    main()
