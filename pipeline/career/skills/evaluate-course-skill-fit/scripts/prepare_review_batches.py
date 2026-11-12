from __future__ import annotations

import argparse
import json
from pathlib import Path


COURSE_FIELDS = [
    "course_code",
    "course_title",
    "course_title_en",
    "description_chinese",
    "description_english",
    "learning_outcomes",
    "course_syllabus",
    "assessment_scheme",
    "prerequisites",
]


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--recommendations", required=True)
    parser.add_argument("--courses", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--batch-size", type=int, default=30)
    args = parser.parse_args()

    recommendations_payload = load_json(Path(args.recommendations))
    course_payload = load_json(Path(args.courses))
    recommendation_rows = recommendations_payload.get("rows", recommendations_payload)
    course_rows = course_payload.get("rows", course_payload)

    courses = {str(c.get("course_code", "")).upper(): c for c in course_rows}
    pairs = {}
    for row in recommendation_rows:
        skill = str(row.get("skill", "")).strip()
        code = str(row.get("course_code", "")).strip().upper()
        if not skill or not code or code not in courses:
            continue
        key = (skill, code)
        if key in pairs:
            continue
        course = courses[code]
        pairs[key] = {
            "skill": skill,
            "retrieval_label": row.get("relation", ""),
            "course": {field: course.get(field, "") for field in COURSE_FIELDS},
        }

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    pair_rows = list(pairs.values())
    for index in range(0, len(pair_rows), args.batch_size):
        batch = pair_rows[index : index + args.batch_size]
        batch_path = output_dir / f"batch_{index // args.batch_size + 1:04d}.jsonl"
        with batch_path.open("w", encoding="utf-8") as handle:
            for item in batch:
                handle.write(json.dumps(item, ensure_ascii=False) + "\n")

    print(
        json.dumps(
            {
                "unique_pairs": len(pair_rows),
                "batches": (len(pair_rows) + args.batch_size - 1) // args.batch_size,
                "batch_size": args.batch_size,
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    main()
