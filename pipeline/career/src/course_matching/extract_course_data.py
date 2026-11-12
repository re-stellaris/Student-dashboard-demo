from __future__ import annotations

import json
import os
import re
from pathlib import Path

import pandas as pd

ROOT = Path(__file__).resolve().parents[2]
OUTLINE_FILE = Path(
    os.environ.get(
        "COURSE_OUTLINES_FILE",
        ROOT / "data" / "private" / "sis_course_outlines_export.xlsx",
    )
)
TITLE_FILE = Path(
    os.environ.get(
        "COURSE_TITLE_FILE",
        ROOT / "data" / "private" / "Course_List_byInitial.xlsx",
    )
)


def clean(value) -> str:
    if value is None or pd.isna(value):
        return ""
    text = str(value).strip()
    return "" if text.lower() == "nan" else text


def normalize_code(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9]", "", clean(value)).upper()


def build_course_code(subject: str, course_number: str) -> str:
    match = re.match(r"\s*([A-Za-z]{2,5})\s*-", clean(subject))
    prefix = match.group(1).upper() if match else ""
    number = clean(course_number).replace(".0", "")
    return normalize_code(prefix + number)


def load_official_titles() -> dict[str, dict[str, str]]:
    xls = pd.ExcelFile(TITLE_FILE)
    titles: dict[str, dict[str, str]] = {}
    for sheet in xls.sheet_names:
        df = pd.read_excel(TITLE_FILE, sheet_name=sheet, dtype=str)
        for _, row in df.iterrows():
            code = build_course_code(row.get("subject", ""), row.get("course_number", ""))
            if not code:
                continue
            zh = clean(row.get("course_title", ""))
            en = clean(row.get("course_title.1", ""))
            if code not in titles or (en and not titles[code].get("course_title_en")):
                titles[code] = {
                    "course_title_zh_official": zh,
                    "course_title_en": en,
                    "title_source_sheet": sheet,
                }
    return titles


def main() -> None:
    official_titles = load_official_titles()
    df = pd.read_excel(OUTLINE_FILE, sheet_name="With Outline", dtype=str)

    wanted = [
        "course_code",
        "course_title",
        "school",
        "academic_org",
        "detail_description",
        "description_english",
        "description_chinese",
        "prerequisites",
        "learning_outcomes",
        "course_syllabus",
        "assessment_scheme",
        "offered_terms",
        "terms_status",
        "status",
    ]

    rows = []
    for _, row in df.iterrows():
        item = {key: clean(row.get(key, "")) for key in wanted}
        if not item["course_syllabus"]:
            continue
        code = normalize_code(item["course_code"])
        item["course_code"] = code
        title_info = official_titles.get(code, {})
        item["course_title_en"] = title_info.get("course_title_en", "")
        item["course_title_zh_official"] = title_info.get("course_title_zh_official", "")
        item["title_source_sheet"] = title_info.get("title_source_sheet", "")
        rows.append(item)

    payload = {
        "rows": rows,
        "stats": {
            "source_sheet": "With Outline",
            "with_syllabus_courses": len(rows),
            "official_title_matches": sum(bool(r["course_title_en"]) for r in rows),
            "official_title_file": str(TITLE_FILE),
        },
    }
    out = ROOT / "work" / "course_data.json"
    out.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(payload["stats"], ensure_ascii=False))


if __name__ == "__main__":
    main()
