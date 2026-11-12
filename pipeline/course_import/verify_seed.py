#!/usr/bin/env python3
"""Optional QA helper: audit the generated V3-V6 seed migrations without MySQL.

This script is **not** part of the migration pipeline; it exists so the output of
``build_edu_seed.py`` can be checked reproducibly on a machine without a live
MySQL server. It parses the generated SQL as text and verifies:

* statement split (quote/escape aware) and per-file/per-table row counts;
* every INSERT column list matches ``V1__edu_schema.sql`` exactly;
* every string fits its declared ``varchar``/``char`` length and every numeric
  fits its ``decimal`` precision;
* every foreign key target exists inside the generated files;
* no self-referencing prerequisite rows, all unique keys of V1 hold;
* the id policy (``100000 + rank`` for courses, ``1000 + rank`` for entities);
* BOM-free UTF-8, LF endings, banner, ``SET NAMES``, FK-check wrapping,
  absence of transaction control, chunk size and statement size limits;
* semantic spot checks (single current term, teacher row, enrollments for
  student 101, ``passed`` never set for F/W/IP/U, competency score >= 6.0);
* determinism: re-runs the generator and compares SHA-256 of every artefact.

Usage::

    python pipeline/course_import/verify_seed.py

Exit code 0 means every check passed; 1 lists the failures.
"""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from decimal import Decimal, InvalidOperation
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_DIR = REPO_ROOT / "backend" / "sql" / "edu"
V1_SCHEMA = SQL_DIR / "V1__edu_schema.sql"
FILES = {
    "V3": SQL_DIR / "V3__edu_seed_course.sql",
    "V4": SQL_DIR / "V4__edu_seed_curriculum.sql",
    "V5": SQL_DIR / "V5__edu_seed_student.sql",
    "V6": SQL_DIR / "V6__edu_seed_competency.sql",
}
REPORT = REPO_ROOT / "pipeline" / "course_import" / "seed-report.json"
GENERATOR = REPO_ROOT / "pipeline" / "course_import" / "build_edu_seed.py"

EXPECTED_TABLES = {
    "V3": ["edu_course", "edu_course_prereq"],
    "V4": ["edu_major", "edu_curriculum", "edu_curriculum_item"],
    "V5": ["edu_term", "edu_teacher", "edu_student", "edu_course_offering",
           "edu_enrollment", "edu_course_record"],
    "V6": ["edu_skill_tag", "edu_course_competency"],
}

UNIQUE_KEYS = [
    ("edu_course", ["code"]),
    ("edu_curriculum", ["major_id", "version_year"]),
    ("edu_term", ["term_code"]),
    ("edu_student", ["student_no"]),
    ("edu_student", ["sys_user_id"]),
    ("edu_teacher", ["teacher_no"]),
    ("edu_teacher", ["sys_user_id"]),
    ("edu_course_offering", ["course_id", "term_id"]),
    ("edu_enrollment", ["student_id", "offering_id"]),
    ("edu_course_prereq", ["course_id", "prereq_course_id"]),
    ("edu_skill_tag", ["code"]),
    ("edu_course_competency", ["course_id", "skill_tag_id"]),
    ("edu_course_record", ["student_id", "course_code", "term_id"]),
]

FOREIGN_KEYS = [
    ("edu_course_prereq.course_id", "edu_course_prereq", "course_id", "edu_course", "course_id"),
    ("edu_course_prereq.prereq_course_id", "edu_course_prereq", "prereq_course_id",
     "edu_course", "course_id"),
    ("edu_curriculum.major_id", "edu_curriculum", "major_id", "edu_major", "major_id"),
    ("edu_curriculum_item.curriculum_id", "edu_curriculum_item", "curriculum_id",
     "edu_curriculum", "curriculum_id"),
    ("edu_curriculum_item.course_id", "edu_curriculum_item", "course_id",
     "edu_course", "course_id"),
    ("edu_student.major_id", "edu_student", "major_id", "edu_major", "major_id"),
    ("edu_course_offering.course_id", "edu_course_offering", "course_id",
     "edu_course", "course_id"),
    ("edu_course_offering.term_id", "edu_course_offering", "term_id", "edu_term", "term_id"),
    ("edu_course_offering.teacher_id", "edu_course_offering", "teacher_id",
     "edu_teacher", "teacher_id"),
    ("edu_enrollment.student_id", "edu_enrollment", "student_id", "edu_student", "student_id"),
    ("edu_enrollment.offering_id", "edu_enrollment", "offering_id",
     "edu_course_offering", "offering_id"),
    ("edu_course_record.student_id", "edu_course_record", "student_id",
     "edu_student", "student_id"),
    ("edu_course_record.course_id", "edu_course_record", "course_id",
     "edu_course", "course_id"),
    ("edu_course_record.term_id", "edu_course_record", "term_id", "edu_term", "term_id"),
    ("edu_course_competency.course_id", "edu_course_competency", "course_id",
     "edu_course", "course_id"),
    ("edu_course_competency.skill_tag_id", "edu_course_competency", "skill_tag_id",
     "edu_skill_tag", "skill_tag_id"),
]

FAILURES: list[str] = []
NOTES: list[str] = []


def fail(message: str) -> None:
    FAILURES.append(message)


# ---------------------------------------------------------------------------
# V1 schema (column names, declared lengths, decimal precision)
# ---------------------------------------------------------------------------

def parse_schema() -> dict[str, dict]:
    text = V1_SCHEMA.read_text(encoding="utf-8")
    tables: dict[str, dict] = {}
    for match in re.finditer(r"create table (\w+) \((.*?)\n\) engine", text, re.S):
        name, body = match.group(1), match.group(2)
        columns: dict[str, dict] = {}
        for line in body.splitlines():
            line = line.strip()
            column = re.match(
                r"^(\w+)\s+(varchar\((\d+)\)|char\((\d+)\)|decimal\((\d+),(\d+)\)|"
                r"bigint\(\d+\)|int\(\d+\)|date|datetime|text)",
                line,
                re.I,
            )
            if not column:
                continue
            sqltype = column.group(2).lower()
            info = {"type": sqltype, "maxlen": None, "precision": None, "scale": None}
            if sqltype.startswith(("varchar", "char")):
                info["maxlen"] = int(column.group(3) or column.group(4))
            elif sqltype.startswith("decimal"):
                info["precision"] = int(column.group(5))
                info["scale"] = int(column.group(6))
            columns[column.group(1)] = info
        tables[name] = {"columns": columns}
    if not tables:
        raise SystemExit(f"could not parse {V1_SCHEMA.name}")
    return tables


SCHEMA = parse_schema()


# ---------------------------------------------------------------------------
# Minimal SQL parser
# ---------------------------------------------------------------------------

def split_statements(text: str):
    """Yield statements, respecting quoted strings, escapes and `--` comments."""
    start = 0
    index = 0
    length = len(text)
    in_quote = False
    in_comment = False
    while index < length:
        char = text[index]
        if in_comment:
            if char == "\n":
                in_comment = False
            index += 1
            continue
        if in_quote:
            if char == "\\":
                index += 2
                continue
            if char == "'":
                if index + 1 < length and text[index + 1] == "'":
                    index += 2
                    continue
                in_quote = False
            index += 1
            continue
        if char == "'":
            in_quote = True
            index += 1
            continue
        if char == "-" and index + 1 < length and text[index + 1] == "-":
            in_comment = True
            index += 2
            continue
        if char == ";":
            statement = _strip_leading_comments(text[start:index].strip())
            if statement:
                yield statement
            start = index + 1
        index += 1
    tail = _strip_leading_comments(text[start:].strip())
    if tail:
        yield tail


def _strip_leading_comments(statement: str) -> str:
    lines = statement.splitlines()
    while lines and lines[0].lstrip().startswith("--"):
        lines.pop(0)
    return "\n".join(lines).strip()


def split_top_level(body: str) -> list[str]:
    parts: list[str] = []
    depth = 0
    in_quote = False
    current: list[str] = []
    index = 0
    while index < len(body):
        char = body[index]
        if in_quote:
            current.append(char)
            if char == "\\":
                if index + 1 < len(body):
                    current.append(body[index + 1])
                    index += 2
                    continue
            elif char == "'":
                if index + 1 < len(body) and body[index + 1] == "'":
                    current.append("'")
                    index += 2
                    continue
                in_quote = False
            index += 1
            continue
        if char == "'":
            in_quote = True
            current.append(char)
            index += 1
            continue
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
        if char == "," and depth == 0:
            parts.append("".join(current).strip())
            current = []
            index += 1
            continue
        current.append(char)
        index += 1
    tail = "".join(current).strip()
    if tail:
        parts.append(tail)
    return parts


def parse_insert(statement: str):
    match = re.match(r"insert into (\w+) \(([^)]*)\) values\s*(.*)$", statement, re.S | re.I)
    if not match:
        raise ValueError(f"not an INSERT: {statement[:80]}")
    table = match.group(1)
    columns = [column.strip() for column in match.group(2).split(",")]
    body = match.group(3).strip()
    if body.endswith(";"):
        body = body[:-1]
    rows: list[list[str]] = []
    for chunk in split_top_level(body):
        chunk = chunk.strip()
        if not chunk.startswith("(") or not chunk.endswith(")"):
            raise ValueError(f"bad row: {chunk[:60]}")
        rows.append(split_top_level(chunk[1:-1]))
    return table, columns, rows


def unquote(literal: str):
    """Decode a MySQL literal emitted by build_edu_seed.py ('' and \\ escapes)."""
    if literal == "NULL":
        return None
    if not (literal.startswith("'") and literal.endswith("'")):
        return literal
    body = literal[1:-1]
    out: list[str] = []
    index = 0
    while index < len(body):
        char = body[index]
        if char == "\\":
            index += 1
            nxt = body[index]
            out.append({"n": "\n", "t": "\t", "0": "\0", "\\": "\\", "'": "'"}.get(nxt, nxt))
            index += 1
            continue
        if char == "'" and index + 1 < len(body) and body[index + 1] == "'":
            out.append("'")
            index += 2
            continue
        out.append(char)
        index += 1
    return "".join(out)


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------

def check_file_structure(code: str, path: Path) -> list[str]:
    raw = path.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        fail(f"{path.name}: has a UTF-8 BOM")
    if b"\r" in raw:
        fail(f"{path.name}: contains CR (expected LF only)")
    text = raw.decode("utf-8")
    if not any("generated by pipeline/course_import/build_edu_seed.py" in line
               for line in text.splitlines()[:20]):
        fail(f"{path.name}: missing the '-- generated by ...' banner")
    if "SET NAMES utf8mb4;" not in text:
        fail(f"{path.name}: missing SET NAMES utf8mb4;")
    if "SET FOREIGN_KEY_CHECKS = 0;" not in text:
        fail(f"{path.name}: missing SET FOREIGN_KEY_CHECKS = 0;")
    if not text.rstrip().endswith("SET FOREIGN_KEY_CHECKS = 1;"):
        fail(f"{path.name}: does not end with SET FOREIGN_KEY_CHECKS = 1;")
    lowered = text.lower()
    for forbidden in ("begin;", "commit;", "start transaction", "set autocommit"):
        if forbidden in lowered:
            fail(f"{path.name}: contains transaction control {forbidden!r}")
    return text


def main() -> int:
    table_rows: dict[str, list[dict]] = {}
    per_file: dict[str, dict] = {}
    statement_count = 0
    statement_bytes = 0
    max_rows_per_statement = 0
    statements_by_kind: dict[str, int] = {}

    for code, path in FILES.items():
        if not path.exists():
            fail(f"{path.name}: missing (run --all first)")
            continue
        text = check_file_structure(code, path)
        order: list[str] = []
        file_rows = 0
        counts = {"SET": 0, "DELETE": 0, "INSERT": 0}
        for statement in split_statements(text):
            statement_count += 1
            kind = statement.split(None, 1)[0].upper()
            statements_by_kind[kind] = statements_by_kind.get(kind, 0) + 1
            size = len(statement.encode("utf-8"))
            statement_bytes = max(statement_bytes, size)
            if size > 4_000_000:
                fail(f"{path.name}: a statement of {size} bytes exceeds 4MB")
            if kind == "SET":
                counts["SET"] += 1
                continue
            if kind == "DELETE":
                counts["DELETE"] += 1
                match = re.match(r"delete from (\w+);?$", statement, re.I)
                if not match:
                    fail(f"{path.name}: unexpected DELETE form {statement[:60]!r}")
                else:
                    order.append(match.group(1))
                continue
            if kind != "INSERT":
                fail(f"{path.name}: unexpected statement {statement[:80]!r}")
                continue
            try:
                table, columns, rows = parse_insert(statement)
            except ValueError as error:
                fail(f"{path.name}: {error}")
                continue
            counts["INSERT"] += 1
            max_rows_per_statement = max(max_rows_per_statement, len(rows))
            schema = SCHEMA.get(table)
            if schema is None:
                fail(f"{path.name}: INSERT into unknown table {table}")
                continue
            expected = list(schema["columns"])
            if columns != expected:
                missing = [column for column in expected if column not in columns]
                unknown = [column for column in columns if column not in expected]
                fail(f"{path.name}: {table} column list mismatch "
                     f"(missing={missing[:5]}, unknown={unknown[:5]})")
            order.append(table)
            bucket = table_rows.setdefault(table, [])
            for row in rows:
                if len(row) != len(columns):
                    fail(f"{path.name}: {table} row has {len(row)} values "
                         f"for {len(columns)} columns")
                    continue
                record: dict = {}
                for column, literal in zip(columns, row):
                    value = unquote(literal)
                    info = schema["columns"][column]
                    if value is not None and info["maxlen"] is not None \
                            and len(value) > info["maxlen"]:
                        fail(f"{path.name}: {table}.{column} value of {len(value)} chars "
                             f"exceeds declared length {info['maxlen']}")
                    if value is not None and info["precision"] is not None:
                        try:
                            dec = Decimal(value)
                        except InvalidOperation:
                            fail(f"{path.name}: {table}.{column} non-numeric {value!r}")
                        else:
                            limit = Decimal(10) ** (info["precision"] - info["scale"])
                            if abs(dec) >= limit:
                                fail(f"{path.name}: {table}.{column} {value} exceeds "
                                     f"decimal({info['precision']},{info['scale']})")
                    record[column] = value
                bucket.append(record)
                file_rows += 1
        per_file[code] = {"statements": counts, "rows": file_rows}

        expected_tables = EXPECTED_TABLES[code]
        if sorted(set(order)) != sorted(expected_tables):
            fail(f"{path.name}: touches {sorted(set(order))}, expected {sorted(expected_tables)}")
        delete_phase = order[:len(expected_tables)]
        insert_phase = order[len(expected_tables):]
        if sorted(delete_phase) != sorted(expected_tables):
            fail(f"{path.name}: first phase is not a full delete sweep: {delete_phase}")
        if insert_phase and sorted(set(insert_phase)) != sorted(expected_tables):
            fail(f"{path.name}: insert phase lacks tables {sorted(set(insert_phase))}")
        NOTES.append(f"{code}: {sum(counts.values())} statements, {file_rows} rows, "
                     f"deletes={delete_phase}")

    # ---- report consistency ------------------------------------------------
    if not REPORT.exists():
        fail("seed-report.json is missing")
    else:
        report = json.loads(REPORT.read_text(encoding="utf-8"))
        for table, count in report["row_counts"].items():
            actual = len(table_rows.get(table, []))
            if actual != count:
                fail(f"row count mismatch for {table}: sql={actual} report={count}")
        for output in report["outputs"]:
            path = REPO_ROOT / output["path"]
            if not path.exists():
                fail(f"report references missing file {output['path']}")
                continue
            if hashlib.sha256(path.read_bytes()).hexdigest() != output["sha256"]:
                fail(f"report sha256 stale for {output['path']}")
        NOTES.append("row counts and output hashes in seed-report.json match the SQL")

    # ---- foreign keys ------------------------------------------------------
    def values_of(table: str, column: str) -> set[str]:
        return {row[column] for row in table_rows.get(table, []) if row[column] is not None}

    referencing = 0
    for label, table, column, target_table, target_column in FOREIGN_KEYS:
        targets = values_of(target_table, target_column)
        rows = table_rows.get(table, [])
        referencing += len(rows)
        dangling = {row[column] for row in rows
                    if row[column] is not None and row[column] not in targets}
        if dangling:
            fail(f"dangling FK {label}: {sorted(dangling)[:5]}")
    NOTES.append(f"FK targets verified: {len(FOREIGN_KEYS)} relationships, "
                 f"{referencing} referencing rows")

    self_refs = [row for row in table_rows.get("edu_course_prereq", [])
                 if row["course_id"] == row["prereq_course_id"]]
    if self_refs:
        fail(f"{len(self_refs)} self-referencing prerequisite rows")
    NOTES.append(f"self-referencing prerequisites: {len(self_refs)}")

    # ---- unique keys -------------------------------------------------------
    for table, keys in UNIQUE_KEYS:
        seen: set[tuple] = set()
        duplicates = 0
        for row in table_rows.get(table, []):
            value = tuple(row.get(key) for key in keys)
            if value in seen:
                duplicates += 1
            seen.add(value)
        if duplicates:
            fail(f"unique key {table}{tuple(keys)} violated {duplicates} times")
    NOTES.append(f"unique keys verified: {len(UNIQUE_KEYS)}")

    # ---- id policy ---------------------------------------------------------
    courses = table_rows.get("edu_course", [])
    if [row["course_id"] for row in courses] != [str(100000 + i) for i in range(len(courses))]:
        fail("edu_course ids are not 100000+rank in file order")
    if [row["code"] for row in courses] != sorted(row["code"] for row in courses):
        fail("edu_course rows are not sorted by code")
    students = table_rows.get("edu_student", [])
    if [row["sys_user_id"] for row in students] != [str(101 + i) for i in range(8)]:
        fail(f"demo student sys_user ids are {[row['sys_user_id'] for row in students]}")
    if [row["student_id"] for row in students] != [str(1000 + i) for i in range(8)]:
        fail("edu_student ids are not 1000+rank by sys_user_id")
    NOTES.append("id policy verified (100000+rank for courses, 1000+rank for entities)")

    # ---- semantic spot checks ---------------------------------------------
    current = [row for row in table_rows.get("edu_term", []) if row["is_current"] == "1"]
    if len(current) != 1 or current[0]["term_code"] != "2026-27-1":
        fail("current term is not exactly 2026-27-1")
    teacher = table_rows.get("edu_teacher", [])
    if len(teacher) != 1 or (teacher[0]["sys_user_id"], teacher[0]["teacher_no"],
                             teacher[0]["name"]) != ("201", "T2026001", "李老师"):
        fail(f"teacher row wrong: {teacher}")
    enrollments_101 = [row for row in table_rows.get("edu_enrollment", [])
                       if row["student_id"] == "1000"]
    if not 2 <= len(enrollments_101) <= 3:
        fail(f"expected 2-3 enrollments for student 101, got {len(enrollments_101)}")
    current_term_id = current[0]["term_id"] if current else None
    enrolled_offering_ids = {row["offering_id"] for row in enrollments_101}
    for offering in table_rows.get("edu_course_offering", []):
        if offering["offering_id"] in enrolled_offering_ids \
                and offering["term_id"] != current_term_id:
            fail("an enrollment points at a non-current-term offering")
    failing_marked_passed = [row for row in table_rows.get("edu_course_record", [])
                             if row["grade"] in {"F", "W", "IP", "U"} and row["passed"] == "1"]
    if failing_marked_passed:
        fail(f"{len(failing_marked_passed)} failing records marked passed")
    unresolved_records = [row for row in table_rows.get("edu_course_record", [])
                          if row["course_id"] is None]
    NOTES.append(f"course records without a catalogue course: {len(unresolved_records)} "
                 f"({sorted({row['course_code'] for row in unresolved_records})})")
    competencies = table_rows.get("edu_course_competency", [])
    if competencies and min(Decimal(row["score"]) for row in competencies) < Decimal("6.0"):
        fail("a competency score below 6.0 was seeded")
    NOTES.append(f"competency rows: {len(competencies)} (min score "
                 f"{min(Decimal(row['score']) for row in competencies) if competencies else '-'})")

    NOTES.append(f"max rows in one INSERT: {max_rows_per_statement}, "
                 f"max statement bytes: {statement_bytes}")
    if max_rows_per_statement > 500:
        fail(f"INSERT chunk too large: {max_rows_per_statement} rows")

    # ---- determinism -------------------------------------------------------
    before = {path.name: hashlib.sha256(path.read_bytes()).hexdigest()
              for path in FILES.values() if path.exists()}
    before[REPORT.name] = hashlib.sha256(REPORT.read_bytes()).hexdigest() if REPORT.exists() else ""
    result = subprocess.run([sys.executable, str(GENERATOR), "--all"],
                            capture_output=True, cwd=str(REPO_ROOT))
    if result.returncode != 0:
        fail(f"second generation failed: {result.stderr.decode('utf-8', 'replace')[-500:]}")
    after = {path.name: hashlib.sha256(path.read_bytes()).hexdigest()
             for path in FILES.values() if path.exists()}
    after[REPORT.name] = hashlib.sha256(REPORT.read_bytes()).hexdigest() if REPORT.exists() else ""
    for name, digest in before.items():
        if after.get(name) != digest:
            fail(f"non-deterministic output: {name}")
    NOTES.append("re-run byte-identical: "
                 + ", ".join(f"{name}={digest[:12]}" for name, digest in before.items()))

    # ---- summary -----------------------------------------------------------
    print("=" * 78)
    for note in NOTES:
        print("OK   " + note)
    print("=" * 78)
    print(f"statements total: {statement_count} {statements_by_kind}")
    print("per file:", json.dumps(per_file, indent=1, sort_keys=True))
    print("rows per table:", json.dumps({name: len(rows) for name, rows in table_rows.items()},
                                        indent=1))
    print("=" * 78)
    if FAILURES:
        print(f"FAILURES ({len(FAILURES)}):")
        for failure in FAILURES:
            print("  FAIL " + failure)
        return 1
    print("ALL CHECKS PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
