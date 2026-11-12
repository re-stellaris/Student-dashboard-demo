import json
from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parents[1]
WORKBOOK = ROOT / "competency-analysis" / "a_to_z_course_competency_relevance.xlsx"
OUT = ROOT / "students-interface" / "competency_data.js"


def norm_code(value):
    text = str(value or "").strip().upper()
    if not text:
        return ""
    letters = "".join(ch for ch in text if ch.isalpha())
    rest = text[len(letters) :].replace(" ", "")
    return f"{letters} {rest}".strip()


def compact_code(value):
    return norm_code(value).replace(" ", "")


def row_text(row, key):
    value = row.get(key, "")
    if pd.isna(value):
        return ""
    return str(value).strip()


def main():
    pools = pd.read_excel(WORKBOOK, sheet_name="Competency Pools").fillna("")
    scores = pd.read_excel(WORKBOOK, sheet_name="All Course Scores").fillna("")

    pool_items = []
    pool_by_item = {}
    for _, row in pools.iterrows():
        item = row_text(row, "Competency Pool Item")
        if not item:
            continue
        data = {
            "school": row_text(row, "School"),
            "abbr": row_text(row, "School Abbr"),
            "item": item,
            "definition": row_text(row, "Definition"),
            "keywords": row_text(row, "Keywords"),
            "programmes": row_text(row, "Merged From Programmes"),
        }
        pool_items.append(data)
        pool_by_item[item] = data

    courses = {}
    by_item = {item["item"]: [] for item in pool_items}

    for _, row in scores.iterrows():
        code = norm_code(row_text(row, "Course Code"))
        item = row_text(row, "Competency Pool Item")
        if not code or not item:
            continue
        try:
            score = float(row.get("Score / 10", 0) or 0)
        except (TypeError, ValueError):
            score = 0.0
        if score <= 0:
            continue
        entry = {
            "item": item,
            "school": row_text(row, "Competency Pool School") or pool_by_item.get(item, {}).get("school", ""),
            "abbr": pool_by_item.get(item, {}).get("abbr", ""),
            "score": round(score, 1),
            "level": row_text(row, "Level"),
            "rationale": row_text(row, "Rationale"),
        }
        courses.setdefault(compact_code(code), {"code": code, "scores": []})["scores"].append(entry)
        if score >= 4:
            by_item.setdefault(item, []).append(
                {
                    "code": code,
                    "score": round(score, 1),
                    "level": row_text(row, "Level"),
                    "rationale": row_text(row, "Rationale"),
                }
            )

    for course in courses.values():
        course["scores"].sort(key=lambda x: (-x["score"], x["item"]))

    for item, rows in by_item.items():
        rows.sort(key=lambda x: (-x["score"], x["code"]))
        by_item[item] = rows[:120]

    payload = {
        "generatedFrom": "competency-analysis/a_to_z_course_competency_relevance.xlsx",
        "scoreScale": 10,
        "pools": pool_items,
        "courses": courses,
        "byItem": by_item,
    }
    OUT.write_text(
        "window.COMPETENCY_DATA = "
        + json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
        + ";\n",
        encoding="utf-8",
    )
    print(f"Wrote {OUT} ({len(pool_items)} competencies, {len(courses)} courses)")


if __name__ == "__main__":
    main()
