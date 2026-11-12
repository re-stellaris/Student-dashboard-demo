#!/usr/bin/env python3
"""Build the reproducible offline ETL that turns the project's spreadsheets into MySQL seed migrations.

Why this file exists
--------------------
RuoYi-Vue needs demo data before the education module is showable: a course
catalogue with prerequisite edges, majors and curricula, terms/offerings and the
demo students with their transcripts, plus the competency (skill tag) map.
All of that already exists as spreadsheets under ``data/``; this script is the
single, deterministic, offline bridge from those workbooks to the four seed
migrations ``V3``-``V6`` of ``backend/sql/edu``.

It supersedes the legacy ``pipeline/course_import/import_sis_courses.py``, which
talked to a live MySQL server and only imported the SIS course catalogue.

Usage
-----
    python pipeline/course_import/build_edu_seed.py --all
    python pipeline/course_import/build_edu_seed.py --courses
    python pipeline/course_import/build_edu_seed.py --courses --students
    python pipeline/course_import/build_edu_seed.py --report     # report only

Every invocation loads and models *all* inputs (so the report is always
consistent with the generated SQL) and writes only the selected artefacts.
``--all`` writes V3-V6 plus ``seed-report.json``.

Outputs (UTF-8, no BOM, LF line endings)
----------------------------------------
    backend/sql/edu/V3__edu_seed_course.sql       edu_course, edu_course_prereq
    backend/sql/edu/V4__edu_seed_curriculum.sql   edu_major, edu_curriculum, edu_curriculum_item
    backend/sql/edu/V5__edu_seed_student.sql      edu_term, edu_teacher, edu_student,
                                                  edu_course_offering, edu_enrollment,
                                                  edu_course_record
    backend/sql/edu/V6__edu_seed_competency.sql   edu_skill_tag, edu_course_competency
    pipeline/course_import/seed-report.json       counts, skips, defaults, data-quality notes

Inputs (all already in the repository)
--------------------------------------
    data/course_catalog/sis_course_outlines_export.xlsx   sheet "All Courses"
    data/course_catalog/All_Course_Lists.xlsx             sheet "All Courses"
    data/demo/student_demo_data.xlsx                      4 student sheets
    data/demo/stu_demo_SME.xlsx                           "Stu_basic_info" + 4 student sheets
    data/graduation_rules/SDS_major_rules_index.json      SDS majors/curricula
    data/competency/a_to_z_course_competency_relevance.xlsx
                                                          "Competency Pools", "All Course Scores"
    backend/sql/edu/V2__edu_permissions.sql               read-only: sys_user ids/emails

Determinism
-----------
* Every surrogate id is a pure function of the business key: courses get
  ``course_id = 100000 + rank`` after sorting by course code; majors, curricula,
  terms, students, teachers, offerings, skill tags etc. get ``1000 + rank`` after
  sorting by their natural key (see ``assign_ids``).
* No clock, no randomness, no locale-dependent formatting, no dict-ordering
  reliance: all row lists are explicitly sorted before ids are assigned.
* All seeded create_time/update_time values are the constant ``SEED_TIMESTAMP``.
* Two runs over the same inputs therefore produce byte-identical files (the
  verification step hashes the output twice).

Idempotency
-----------
Each file is a plain, transaction-less script that starts with
``SET FOREIGN_KEY_CHECKS = 0;``, deletes the tables it owns child-first and then
re-inserts them with the same explicit ids, so re-running the four migrations in
order is safe and leaves no duplicates. ``SET FOREIGN_KEY_CHECKS = 1;`` closes
each file.

SQL escaping
------------
Single quotes are doubled, backslashes escaped, control characters stripped,
empty numerics become ``NULL`` and every string is truncated to the declared
column length of ``V1__edu_schema.sql``. Multi-row INSERTs are chunked by both
row count and rendered size so no statement approaches the few-MB mark.

Prerequisite semantics (see ``parse_prerequisite_text``)
--------------------------------------------------------
``edu_course_prereq`` stores a disjunction of conjunction groups: rows with the
same ``group_no`` must all be satisfied (AND); different ``group_no`` values are
alternatives (OR). Free text is split into alternatives on ``or`` / ``OR`` /
``<newline>Or`` and on ``CODE/CODE`` pairs; the course codes inside one
alternative share a group. Codes not present in the catalogue are not emitted
(no broken foreign keys) - they are listed in the report. Self-references are
dropped. Because the SIS catalogue mixes 3-, 4- and 5-letter subject prefixes
(``ACT2111``, ``FEMBA6101``) and honours suffixes (``ENG2002S``), the extraction
regex accepts 3-5 letters + 3-4 digits + an optional suffix letter, i.e. a
superset of the "4 letters + 4 digits" rule.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from dataclasses import dataclass, field
from decimal import Decimal, InvalidOperation
from pathlib import Path

from openpyxl import load_workbook

# ---------------------------------------------------------------------------
# 0. Repository layout, fixed seeding constants, input/output paths
# ---------------------------------------------------------------------------

REPO_ROOT = Path(__file__).resolve().parents[2]
PIPELINE_DIR = REPO_ROOT / "pipeline" / "course_import"
SQL_DIR = REPO_ROOT / "backend" / "sql" / "edu"

IN_SIS = REPO_ROOT / "data" / "course_catalog" / "sis_course_outlines_export.xlsx"
IN_COURSE_LISTS = REPO_ROOT / "data" / "course_catalog" / "All_Course_Lists.xlsx"
IN_DEMO = REPO_ROOT / "data" / "demo" / "student_demo_data.xlsx"
IN_DEMO_SME = REPO_ROOT / "data" / "demo" / "stu_demo_SME.xlsx"
IN_RULES = REPO_ROOT / "data" / "graduation_rules" / "SDS_major_rules_index.json"
IN_COMPETENCY = REPO_ROOT / "data" / "competency" / "a_to_z_course_competency_relevance.xlsx"
IN_PERMISSIONS = SQL_DIR / "V2__edu_permissions.sql"

OUT_V3 = SQL_DIR / "V3__edu_seed_course.sql"
OUT_V4 = SQL_DIR / "V4__edu_seed_curriculum.sql"
OUT_V5 = SQL_DIR / "V5__edu_seed_student.sql"
OUT_V6 = SQL_DIR / "V6__edu_seed_competency.sql"
REPORT_PATH = PIPELINE_DIR / "seed-report.json"

GENERATOR = "pipeline/course_import/build_edu_seed.py"
GENERATOR_VERSION = "1.0.0"

# Constant timestamps keep the generated files byte-identical across runs.
SEED_TIMESTAMP = "2026-11-15 00:00:00"
SEED_AUTHOR = "seed:build_edu_seed"

CURRENT_TERM_CODE = "2026-27-1"
CURRENT_TERM_NAME = "2026-27 Term 1"

# Surrogate id bases (mirror the auto_increment values declared in V1).
COURSE_ID_BASE = 100000
ENTITY_ID_BASE = 1000
SEQ_ID_BASE = 1

# demo sys_user ids (V2__edu_permissions.sql) mapped to workbook sheets
DEMO_STUDENTS = [
    # (workbook, sheet, sys_user_id)
    (IN_DEMO, "CHEN MIC", 101),
    (IN_DEMO, "WANG SOPHIA", 102),
    (IN_DEMO, "LIU JAMES", 103),
    (IN_DEMO, "EAN JIE", 104),
    (IN_DEMO_SME, "Zixuan Wang", 105),
    (IN_DEMO_SME, "Muyang Liu", 106),
    (IN_DEMO_SME, "Zihan Chen", 107),
    (IN_DEMO_SME, "Ruohan Zhou", 108),
]

TEACHER_SYS_USER_ID = 201
TEACHER_NO = "T2026001"
TEACHER_NAME = "李老师"
TEACHER_TITLE = "副教授"
TEACHER_DEPT_ID = 110  # sys_dept 110 = 数据科学学院 (see V2)

# The current-term offerings used to make the teacher workspace / dashboard demoable.
# Only codes present in the SIS catalogue are seeded; the rest are reported.
DEMO_OFFERING_CODES = ["ACT2121", "CSC3100", "DDA3020", "ECO2011", "MGT2020", "STA2001"]
DEMO_ENROLLMENT_STUDENT_SYS_USER = 101
DEMO_ENROLLMENT_COUNT = 3
DEMO_OFFERING_CAPACITY = 60
DEMO_SCHEDULE_SLOTS = [
    "Mon 10:00-11:50, Teaching Building A 101",
    "Tue 14:00-15:50, Teaching Building A 203",
    "Wed 09:00-10:50, Teaching Building B 305",
    "Thu 16:00-17:50, Teaching Building B 102",
    "Fri 10:00-11:50, Teaching Building C 401",
    "Fri 14:00-15:50, Teaching Building C 208",
]

# English -> Chinese programme names as they appear in All_Course_Lists.xlsx.
# Only the SDS programmes covered by SDS_major_rules_index.json are needed here.
PROGRAMME_ALIASES = {
    "Statistics": "统计学",
    "Computer Science and Engineering": "计算机科学与技术",
    "Data Science and Big Data Technology": "数据科学与大数据技术",
}

# Short major codes for the demo students' majors that are not part of the SDS index.
# (ADA/AFR/FE match the remarks used for the same students in V2.)
STUDENT_MAJOR_CODES = {
    "Accounting Data and Analytics": "ADA",
    "Accounting and Financial Reporting": "AFR",
    "Financial Engineering - Quantitative Finance": "FE",
}

SCHOOL_ZH = {
    "SDS": "数据科学学院",
    "SME": "经管学院",
}

# requirement_type heuristics for edu_curriculum_item.section (first match wins).
SECTION_RULES = [
    ("BREADTH", re.compile(r"breadth", re.I)),
    ("DEPTH", re.compile(r"depth", re.I)),
    ("STREAM", re.compile(r"stream|concentration", re.I)),
    ("GE", re.compile(r"gen\s*ed|general\s+education|\bGE\b", re.I)),
    ("ELECTIVE", re.compile(r"elective", re.I)),
    ("REQUIRED", re.compile(r"required|school\s+package|\bcore\b|\bmandatory\b|foundation", re.I)),
]
DEFAULT_REQUIREMENT_TYPE = "REQUIRED"

# edu_course.category heuristics from the SIS subject code.
SUBJECT_CATEGORY_RULES = [
    ("IT", re.compile(r"^ITE$")),
    ("PE", re.compile(r"^PED$")),
    ("Chinese", re.compile(r"^(CHI|CLC|CLCC)$")),
    ("English", re.compile(r"^(ENG|ENL|ENB)$")),
    ("GenEd", re.compile(r"^(GE[A-Z]|GGE|GFH|GFN|BGE)$")),
]
DEFAULT_CATEGORY = "Major"

# Course codes: the SIS catalogue uses 3-letter subjects (ACT/CSC/ECO...) but a
# handful of 4-5 letter ones (FEMBA, EXGBE) and a trailing honours/suffix letter.
# The task's "4 letters + 4 digits" rule is therefore implemented as its superset.
COURSE_CODE_RE = re.compile(r"(?<![A-Z0-9])([A-Z]{3,5}\d{3,4}[A-Z]?)(?![A-Z0-9])")
SPACED_CODE_RE = re.compile(r"(?<![A-Z0-9])([A-Z]{3,5})[ \t\u00a0]+(\d{3,4})([A-Z]?)(?![A-Z0-9])")
ALT_SPLIT_RE = re.compile(r"(?:(?<![A-Za-z])or(?![A-Za-z]))", re.I)
COURSE_CODE_FULL_RE = re.compile(r"^[A-Z]{3,5}\d{3,4}[A-Z]?$")

# Prefixes that are ordinary English words, so "and1004" is not read as a course code.
CODE_STOP_PREFIXES = {
    "AND", "ARE", "ALL", "ANY", "BUT", "CAN", "ETC", "FOR", "HAS", "HAV", "NOT",
    "ONE", "SEE", "THE", "TWO", "USE", "WITH", "YOU", "MUST", "NEED", "PLUS",
}

# Prerequisite text that states "there is none" (never an edge).
# Matched against a punctuation-stripped, lower-cased copy so that "N/A", "N.A.",
# "Nil.", "No.", "None", "No prerequisite needed." and
# "There are no prerequisites for this course." all take the same path.
NO_PREREQ_PATTERNS = [
    r"no(?:ne)?",                                   # no / no. / none
    r"nil", r"null", r"n/?a", r"n a", r"/",
    r"not applicable", r"not required",
    r"no (?:pre ?|co ?)?requisite",                 # no prerequisite
    r"no (?:pre ?|co ?)?requisites",                # no prerequisites
    r"no (?:pre ?|co ?)?requisites? (?:is |are )?(?:needed|required|necessary|specified|listed)",
    r"(?:there are )?no (?:pre ?|co ?)?requisites? (?:for|in) this course",
]

# "A/B" between two catalogue-shaped codes means OR; this pattern rewrites only
# the unambiguous code/code case, so titles such as "C/C++ Programming" survive.
SLASH_ALT_RE = re.compile(
    r"(?<![A-Z0-9])([A-Z]{3,5}\d{3,4}[A-Z]?)[ \t]*/[ \t]*"
    r"([A-Z]{3,5}\d{3,4}[A-Z]?)(?![A-Z0-9])"
)

# ---------------------------------------------------------------------------
# 1. Column specifications (authoritative: backend/sql/edu/V1__edu_schema.sql)
#    name, kind, arg   where kind is int | dec | str | text
#    dec: arg = decimal places; str/text: arg = declared max character length.
# ---------------------------------------------------------------------------

TABLE_COLUMNS: dict[str, list[tuple[str, str, int]]] = {
    "edu_course": [
        ("course_id", "int", 0),
        ("code", "str", 32),
        ("title_zh", "str", 255),
        ("title_en", "str", 255),
        ("subject", "str", 128),
        ("course_number", "str", 32),
        ("letter", "str", 1),
        ("credits", "dec", 2),
        ("level", "int", 0),
        ("category", "str", 32),
        ("grading_basis", "str", 32),
        ("component", "str", 32),
        ("campus", "str", 64),
        ("school", "str", 128),
        ("academic_org", "str", 128),
        ("outline_status", "str", 32),
        ("terms_status", "str", 32),
        ("offered_terms", "text", 0),
        ("prereq_text", "str", 1000),
        ("coreq_text", "str", 500),
        ("description_zh", "text", 0),
        ("description_en", "text", 0),
        ("status", "str", 1),
        ("del_flag", "str", 1),
        ("create_by", "str", 64),
        ("create_time", "str", 19),
        ("update_by", "str", 64),
        ("update_time", "ts", 19),
        ("remark", "str", 500),
    ],
    "edu_course_prereq": [
        ("id", "int", 0),
        ("course_id", "int", 0),
        ("prereq_course_id", "int", 0),
        ("group_no", "int", 0),
        ("raw_text", "str", 500),
        ("create_by", "str", 64),
        ("create_time", "str", 19),
    ],
    "edu_major": [
        ("major_id", "int", 0),
        ("major_code", "str", 64),
        ("major_name_zh", "str", 128),
        ("major_name_en", "str", 128),
        ("school_abbr", "str", 32),
        ("school_name", "str", 128),
        ("degree_type", "str", 32),
        ("duration_years", "dec", 1),
        ("status", "str", 1),
        ("del_flag", "str", 1),
        ("create_by", "str", 64),
        ("create_time", "str", 19),
        ("update_by", "str", 64),
        ("update_time", "ts", 19),
        ("remark", "str", 500),
    ],
    "edu_curriculum": [
        ("curriculum_id", "int", 0),
        ("major_id", "int", 0),
        ("version_label", "str", 64),
        ("version_year", "int", 0),
        ("effective_from", "int", 0),
        ("effective_to", "int", 0),
        ("total_credits", "dec", 1),
        ("major_credits", "dec", 1),
        ("source_doc", "str", 255),
        ("status", "str", 1),
        ("del_flag", "str", 1),
        ("create_by", "str", 64),
        ("create_time", "str", 19),
        ("update_by", "str", 64),
        ("update_time", "ts", 19),
        ("remark", "str", 500),
    ],
    "edu_curriculum_item": [
        ("item_id", "int", 0),
        ("curriculum_id", "int", 0),
        ("course_id", "int", 0),
        ("course_code", "str", 32),
        ("requirement_type", "str", 32),
        ("section", "str", 255),
        ("stream", "str", 128),
        ("min_units", "dec", 1),
        ("sort_order", "int", 0),
        ("create_by", "str", 64),
        ("create_time", "str", 19),
    ],
    "edu_term": [
        ("term_id", "int", 0),
        ("term_code", "str", 32),
        ("term_name", "str", 64),
        ("academic_year", "str", 16),
        ("term_index", "int", 0),
        ("start_date", "ts", 10),
        ("end_date", "ts", 10),
        ("is_current", "str", 1),
        ("status", "str", 1),
        ("create_time", "str", 19),
    ],
    "edu_teacher": [
        ("teacher_id", "int", 0),
        ("sys_user_id", "int", 0),
        ("teacher_no", "str", 32),
        ("name", "str", 64),
        ("dept_id", "int", 0),
        ("school", "str", 128),
        ("title", "str", 64),
        ("status", "str", 1),
        ("del_flag", "str", 1),
        ("create_by", "str", 64),
        ("create_time", "str", 19),
        ("update_by", "str", 64),
        ("update_time", "ts", 19),
    ],
    "edu_student": [
        ("student_id", "int", 0),
        ("sys_user_id", "int", 0),
        ("student_no", "str", 32),
        ("name", "str", 64),
        ("major_id", "int", 0),
        ("entry_year", "int", 0),
        ("college", "str", 64),
        ("email", "str", 128),
        ("cum_gpa", "dec", 2),
        ("total_credits", "dec", 1),
        ("status", "str", 1),
        ("del_flag", "str", 1),
        ("create_by", "str", 64),
        ("create_time", "str", 19),
        ("update_by", "str", 64),
        ("update_time", "ts", 19),
        ("remark", "str", 500),
    ],
    "edu_course_offering": [
        ("offering_id", "int", 0),
        ("course_id", "int", 0),
        ("term_id", "int", 0),
        ("teacher_id", "int", 0),
        ("capacity", "int", 0),
        ("enrolled_count", "int", 0),
        ("schedule_text", "str", 255),
        ("status", "str", 1),
        ("create_by", "str", 64),
        ("create_time", "str", 19),
        ("update_by", "str", 64),
        ("update_time", "ts", 19),
    ],
    "edu_enrollment": [
        ("enrollment_id", "int", 0),
        ("student_id", "int", 0),
        ("offering_id", "int", 0),
        ("status", "str", 16),
        ("enrolled_at", "str", 19),
        ("create_by", "str", 64),
        ("create_time", "str", 19),
        ("update_by", "str", 64),
        ("update_time", "ts", 19),
    ],
    "edu_course_record": [
        ("record_id", "int", 0),
        ("student_id", "int", 0),
        ("course_id", "int", 0),
        ("course_code", "str", 32),
        ("course_title", "str", 255),
        ("term_id", "int", 0),
        ("grade", "str", 8),
        ("score", "dec", 2),
        ("units", "dec", 2),
        ("category", "str", 32),
        ("passed", "str", 1),
        ("record_status", "str", 16),
        ("source", "str", 32),
        ("del_flag", "str", 1),
        ("create_by", "str", 64),
        ("create_time", "str", 19),
        ("update_by", "str", 64),
        ("update_time", "ts", 19),
    ],
    "edu_skill_tag": [
        ("skill_tag_id", "int", 0),
        ("code", "str", 64),
        ("name", "str", 200),
        ("category", "str", 64),
        ("school_abbr", "str", 32),
        ("description", "str", 1000),
        ("keywords", "str", 1000),
        ("sort_order", "int", 0),
        ("status", "str", 1),
        ("del_flag", "str", 1),
        ("create_time", "str", 19),
    ],
    "edu_course_competency": [
        ("id", "int", 0),
        ("course_id", "int", 0),
        ("skill_tag_id", "int", 0),
        ("score", "dec", 1),
        ("level", "str", 32),
        ("rationale", "str", 1000),
        ("matched_keywords", "str", 500),
        ("create_time", "str", 19),
    ],
}

# Tables owned by each generated file, in the order they are DELETE-ed
# (child before parent, so the sweep also works when FK checks are enabled).
DELETE_ORDER = {
    "V3": ["edu_course_prereq", "edu_course"],
    "V4": ["edu_curriculum_item", "edu_curriculum", "edu_major"],
    "V5": ["edu_course_record", "edu_enrollment", "edu_course_offering",
           "edu_student", "edu_teacher", "edu_term"],
    "V6": ["edu_course_competency", "edu_skill_tag"],
}

# Tables of each file in dependency order for the data load.
INSERT_ORDER = {
    "V3": ["edu_course", "edu_course_prereq"],
    "V4": ["edu_major", "edu_curriculum", "edu_curriculum_item"],
    "V5": ["edu_term", "edu_teacher", "edu_student", "edu_course_offering",
           "edu_enrollment", "edu_course_record"],
    "V6": ["edu_skill_tag", "edu_course_competency"],
}

# Per-table INSERT chunking: (max rows, max rendered bytes per statement).
CHUNK_LIMITS = {
    "edu_course": (200, 1_200_000),
    "edu_course_prereq": (500, 800_000),
    "edu_curriculum_item": (500, 800_000),
    "edu_course_record": (500, 800_000),
    "edu_course_competency": (500, 800_000),
}
DEFAULT_CHUNK = (500, 800_000)


# ---------------------------------------------------------------------------
# 2. Small text / SQL helpers
# ---------------------------------------------------------------------------

def clean_text(value) -> str:
    """Normalise a cell/JSON value into a control-character-free string."""
    if value is None:
        return ""
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, float) and value.is_integer():
        return str(int(value))
    text = str(value)
    text = text.replace("\ufeff", "").replace("\r\n", "\n").replace("\r", "\n")
    # Drop control characters except newline/tab; collapse NUL and friends.
    text = "".join(ch for ch in text if ch in "\n\t" or ord(ch) >= 32)
    text = text.replace("\x7f", "")
    return text.strip()


def collapse_ws(text: str) -> str:
    """Collapse all whitespace runs (including newlines) into single spaces."""
    return " ".join(str(text).split())


def code_key(value) -> str:
    """Lookup key for a course code: upper-case, alphanumerics only.

    ``FIN(6106)`` and ``ACT 2111`` normalise to ``FIN6106`` / ``ACT2111`` so that
    slightly malformed source cells still resolve to the catalogue.
    """
    return re.sub(r"[^A-Z0-9]", "", str(value or "").upper())


def quote_sql(text: str) -> str:
    """Quote a MySQL string literal (MySQL default mode: backslash escapes)."""
    return "'" + text.replace("\\", "\\\\").replace("'", "''") + "'"


def to_decimal(value):
    """Parse a numeric cell into Decimal, or return None."""
    text = collapse_ws(clean_text(value))
    if not text:
        return None
    text = text.replace(",", "").replace("%", "").strip()
    try:
        return Decimal(text)
    except (InvalidOperation, ValueError):
        return None


def sql_literal(value, kind: str, arg: int) -> str:
    """Render one column value as a SQL literal, honouring the schema length."""
    if kind == "int":
        if value is None or value == "":
            return "NULL"
        return str(int(value))
    if kind == "dec":
        if value is None or value == "":
            return "NULL"
        dec = value if isinstance(value, Decimal) else to_decimal(value)
        if dec is None:
            return "NULL"
        return f"{dec:.{arg}f}"
    text = clean_text(value)
    if kind == "ts":
        # DATE/DATETIME columns must be NULL rather than '' (strict SQL mode).
        return quote_sql(text) if text else "NULL"
    if kind == "text":
        if not text:
            return "NULL"
        if arg:
            text = text[:arg]
        return quote_sql(text)
    # "str": schema columns have default '', so empty stays '' (never NULL).
    if arg:
        text = text[:arg]
    return quote_sql(text)


def sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def rel_posix(path: Path) -> str:
    """Repository-relative POSIX path (stable across platforms)."""
    try:
        return path.resolve().relative_to(REPO_ROOT).as_posix()
    except ValueError:
        return path.name


def read_sheet(path: Path, sheet_name: str) -> list[tuple]:
    """Read a worksheet into a list of value tuples (read-only, values only)."""
    workbook = load_workbook(path, read_only=True, data_only=True)
    try:
        if sheet_name not in workbook.sheetnames:
            raise SystemExit(f"sheet {sheet_name!r} not found in {rel_posix(path)}")
        return list(workbook[sheet_name].iter_rows(values_only=True))
    finally:
        workbook.close()


def header_index(header: list[str], name: str) -> int:
    lowered = [h.strip().lower() for h in header]
    try:
        return lowered.index(name.strip().lower())
    except ValueError:
        raise SystemExit(f"missing column {name!r} in header {header!r}") from None


# ---------------------------------------------------------------------------
# 3. INSERT rendering with per-table chunking
# ---------------------------------------------------------------------------

class TableInserter:
    """Accumulate rows for one table and render chunked multi-row INSERTs."""

    def __init__(self, table: str):
        self.table = table
        self.columns = [name for name, _kind, _arg in TABLE_COLUMNS[table]]
        self.specs = TABLE_COLUMNS[table]
        self.max_rows, self.max_bytes = CHUNK_LIMITS.get(table, DEFAULT_CHUNK)
        self._chunk: list[str] = []
        self._chunk_bytes = 0
        self._statements: list[str] = []
        self.row_count = 0

    def add(self, row: dict) -> None:
        missing = [c for c in self.columns if c not in row]
        if missing:
            raise SystemExit(f"{self.table}: row is missing columns {missing}")
        # Helper keys used for lookups (e.g. "code_key") never reach the SQL.
        extra = [c for c in row
                 if c not in self.columns and c != "code_key" and not c.startswith("_")]
        if extra:
            raise SystemExit(f"{self.table}: row has unknown columns {extra}")
        values = ",".join(
            sql_literal(row[name], kind, arg) for name, kind, arg in self.specs
        )
        rendered = f"({values})"
        self._chunk.append(rendered)
        self._chunk_bytes += len(rendered) + 2
        self.row_count += 1
        if len(self._chunk) >= self.max_rows or self._chunk_bytes >= self.max_bytes:
            self._flush()

    def _flush(self) -> None:
        if not self._chunk:
            return
        columns = ", ".join(self.columns)
        body = ",\n".join(self._chunk)
        self._statements.append(
            f"insert into {self.table} ({columns}) values\n{body};"
        )
        self._chunk = []
        self._chunk_bytes = 0

    def render(self) -> str:
        self._flush()
        return "\n\n".join(self._statements)

    @property
    def statement_count(self) -> int:
        return len(self._statements)


# ---------------------------------------------------------------------------
# 4. Input loaders
# ---------------------------------------------------------------------------

@dataclass
class Model:
    courses: list[dict] = field(default_factory=list)
    prereqs: list[dict] = field(default_factory=list)
    majors: list[dict] = field(default_factory=list)
    curricula: list[dict] = field(default_factory=list)
    curriculum_items: list[dict] = field(default_factory=list)
    terms: list[dict] = field(default_factory=list)
    teachers: list[dict] = field(default_factory=list)
    students: list[dict] = field(default_factory=list)
    offerings: list[dict] = field(default_factory=list)
    enrollments: list[dict] = field(default_factory=list)
    records: list[dict] = field(default_factory=list)
    skill_tags: list[dict] = field(default_factory=list)
    course_competencies: list[dict] = field(default_factory=list)

    def table_rows(self) -> dict[str, list[dict]]:
        return {
            "edu_course": self.courses,
            "edu_course_prereq": self.prereqs,
            "edu_major": self.majors,
            "edu_curriculum": self.curricula,
            "edu_curriculum_item": self.curriculum_items,
            "edu_term": self.terms,
            "edu_teacher": self.teachers,
            "edu_student": self.students,
            "edu_course_offering": self.offerings,
            "edu_enrollment": self.enrollments,
            "edu_course_record": self.records,
            "edu_skill_tag": self.skill_tags,
            "edu_course_competency": self.course_competencies,
        }


def load_course_titles() -> tuple[dict[str, dict], list[dict]]:
    """Read All_Course_Lists.xlsx: per-code EN/ZH titles and the flat row list."""
    rows = read_sheet(IN_COURSE_LISTS, "All Courses")
    header = [clean_text(cell) for cell in rows[0]]
    idx_prog = header_index(header, "Programme")
    idx_sec = header_index(header, "Section")
    idx_code = header_index(header, "Course Code")
    idx_en = header_index(header, "Course Title (English)")
    idx_zh = header_index(header, "Course Title (Chinese)")
    idx_units = header_index(header, "Units")
    idx_pdf = header_index(header, "Source PDF")
    idx_page = header_index(header, "Page")

    titles: dict[str, dict] = {}
    entries: list[dict] = []
    for raw in rows[1:]:
        code = clean_text(raw[idx_code] if idx_code < len(raw) else "")
        if not code:
            continue
        key = code_key(code)
        entry = {
            "programme": clean_text(raw[idx_prog] if idx_prog < len(raw) else ""),
            "section": clean_text(raw[idx_sec] if idx_sec < len(raw) else ""),
            "code": key,
            "title_en": clean_text(raw[idx_en] if idx_en < len(raw) else ""),
            "title_zh": clean_text(raw[idx_zh] if idx_zh < len(raw) else ""),
            "units": to_decimal(raw[idx_units] if idx_units < len(raw) else None),
            "source_pdf": clean_text(raw[idx_pdf] if idx_pdf < len(raw) else ""),
            "page": clean_text(raw[idx_page] if idx_page < len(raw) else ""),
        }
        entries.append(entry)
        if key not in titles:
            titles[key] = {"title_en": entry["title_en"], "title_zh": entry["title_zh"]}
        else:
            existing = titles[key]
            if not existing["title_en"] and entry["title_en"]:
                existing["title_en"] = entry["title_en"]
            if not existing["title_zh"] and entry["title_zh"]:
                existing["title_zh"] = entry["title_zh"]
    return titles, entries


def course_level(code: str) -> int:
    """Derive the 1000-bucket level from the numeric part of a course code."""
    digits = re.search(r"\d{3,4}", code)
    if not digits:
        return 0
    return (int(digits.group(0)) // 1000) * 1000


def course_category(subject_code: str) -> str:
    for category, pattern in SUBJECT_CATEGORY_RULES:
        if pattern.match(subject_code):
            return category
    return DEFAULT_CATEGORY


def load_catalogue(titles: dict[str, dict]) -> tuple[list[dict], dict]:
    """Read the SIS course outline export into edu_course row dicts."""
    rows = read_sheet(IN_SIS, "All Courses")
    header = [clean_text(cell) for cell in rows[0]]
    idx = {name: header_index(header, name) for name in (
        "letter", "subject", "course_code", "course_number", "course_title", "status",
        "units", "grading_basis", "component", "campus", "school", "academic_org",
        "description_english", "description_chinese", "prerequisites", "co_requisites",
        "terms_status", "offered_terms",
    )}

    stats = {
        "source_rows": len(rows) - 1,
        "with_code": 0,
        "duplicate_codes": 0,
        "duplicate_code_examples": [],
        "non_numeric_units": [],
        "blank_units": 0,
        "title_en_from_course_lists": 0,
        "title_en_missing": 0,
        "category_counts": {},
        "outline_status_counts": {},
    }
    category_counts: dict[str, int] = {}
    outline_counts: dict[str, int] = {}
    non_numeric_units: set[str] = set()
    duplicate_examples: list[str] = []

    courses: list[dict] = []
    seen: dict[str, int] = {}
    for raw in rows[1:]:
        def cell(name: str):
            position = idx[name]
            return raw[position] if position < len(raw) else None

        raw_code = clean_text(cell("course_code"))
        if not raw_code:
            continue
        stats["with_code"] += 1
        key = code_key(raw_code)
        if not key:
            continue
        if key in seen:
            stats["duplicate_codes"] += 1
            if len(duplicate_examples) < 10:
                duplicate_examples.append(f"{courses[seen[key]]['code']} / {raw_code}")
            continue
        seen[key] = len(courses)

        subject = clean_text(cell("subject"))
        subject_code = subject.split(" - ", 1)[0].strip().upper() if subject else ""
        units_raw = cell("units")
        credits = to_decimal(units_raw)
        if credits is None:
            text = collapse_ws(clean_text(units_raw))
            if text:
                non_numeric_units.add(text)
            else:
                stats["blank_units"] += 1
            credits = Decimal("0.00")
        elif credits > Decimal("99.99"):
            # varchar decimal(4,2) cannot hold it; clamp and report.
            non_numeric_units.add(f"{credits} (clamped to 99.99)")
            credits = Decimal("99.99")

        title_zh = clean_text(cell("course_title")) or titles.get(key, {}).get("title_zh", "")
        title_en = titles.get(key, {}).get("title_en", "")
        if title_en:
            stats["title_en_from_course_lists"] += 1
        else:
            stats["title_en_missing"] += 1

        category = course_category(subject_code)
        category_counts[category] = category_counts.get(category, 0) + 1
        outline_status = clean_text(cell("status"))
        outline_counts[outline_status] = outline_counts.get(outline_status, 0) + 1

        courses.append({
            "code": raw_code,
            "code_key": key,
            "title_zh": title_zh,
            "title_en": title_en,
            "subject": subject,
            "course_number": clean_text(cell("course_number")),
            "letter": clean_text(cell("letter"))[:1],
            "credits": credits,
            "level": course_level(raw_code),
            "category": category,
            "grading_basis": clean_text(cell("grading_basis")),
            "component": clean_text(cell("component")),
            "campus": clean_text(cell("campus")),
            "school": clean_text(cell("school")),
            "academic_org": clean_text(cell("academic_org")),
            "outline_status": outline_status,
            "terms_status": clean_text(cell("terms_status")),
            "offered_terms": clean_text(cell("offered_terms")),
            "prereq_text": clean_text(cell("prerequisites")),
            "coreq_text": clean_text(cell("co_requisites")),
            "description_zh": clean_text(cell("description_chinese")),
            "description_en": clean_text(cell("description_english")),
            "status": "0",
            "del_flag": "0",
            "create_by": SEED_AUTHOR,
            "create_time": SEED_TIMESTAMP,
            "update_by": "",
            "update_time": None,
            "remark": None,
        })

    stats["non_numeric_units"] = sorted(non_numeric_units)
    stats["duplicate_code_examples"] = duplicate_examples
    stats["category_counts"] = dict(sorted(category_counts.items()))
    stats["outline_status_counts"] = dict(sorted(outline_counts.items()))
    return courses, stats


def load_rules() -> dict:
    with IN_RULES.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def entry_year_from_student_no(student_no: str):
    """Derive the admission year from a CUHKSZ student number.

    The number has the shape ``1YYxxxxxx`` where the leading ``1`` flags an
    undergraduate record and ``YY`` encodes the admission year, so
    ``122040156`` -> ``2022`` (and 123030001 -> 2023, 125030003 -> 2025, which
    matches the ``Year`` column of stu_demo_SME.xlsx). Numbers that do not follow
    that shape fall back to their first two digits.
    """
    digits = re.sub(r"\D", "", student_no or "")
    if len(digits) < 2:
        return None
    if len(digits) >= 3 and digits[0] == "1":
        return 2000 + int(digits[1:3])
    return 2000 + int(digits[:2])


def load_demo_students() -> tuple[list[dict], dict]:
    """Parse the eight demo student sheets plus stu_demo_SME basic info."""
    # Basic info from stu_demo_SME.xlsx "Stu_basic_info" (name -> row dict)
    basic: dict[str, dict] = {}
    for raw in read_sheet(IN_DEMO_SME, "Stu_basic_info")[1:]:
        name = clean_text(raw[0] if raw else "")
        if not name:
            continue
        basic[name] = {
            "year": clean_text(raw[1] if len(raw) > 1 else ""),
            "school": clean_text(raw[2] if len(raw) > 2 else ""),
            "major": clean_text(raw[3] if len(raw) > 3 else ""),
            "email": clean_text(raw[4] if len(raw) > 4 else ""),
            "hometown": clean_text(raw[5] if len(raw) > 5 else ""),
        }

    students: list[dict] = []
    stats = {
        "sheets_parsed": 0,
        "record_rows": 0,
        "skipped_transcript_rows": [],
        "duplicate_record_keys": 0,
        "entry_year_mismatch": [],
        "derived_emails": [],
        "derived_entry_years": [],
        "distinct_course_codes": 0,
        "grade_counts": {},
        "status_counts": {},
        "blank_scores": 0,
        "semesters": [],
    }
    grade_counts: dict[str, int] = {}
    status_counts: dict[str, int] = {}
    semesters: set[str] = set()
    transcript_codes: set[str] = set()
    skipped: list[dict] = []

    for workbook_path, sheet_name, sys_user_id in DEMO_STUDENTS:
        rows = read_sheet(workbook_path, sheet_name)
        info: dict[str, str] = {}
        header_at = None
        for position, raw in enumerate(rows):
            first = clean_text(raw[0] if raw else "")
            if first == "ACADEMIC RECORD":
                header_at = position + 1
                break
            # The STUDENT INFORMATION / CREDIT BREAKDOWN blocks are laid out as
            # label/value pairs repeated across the row ("Name:" | value |
            # "Student ID:" | value), so scan every cell, not just columns A/B.
            for column, cell in enumerate(raw[:-1]):
                label = clean_text(cell)
                if not label.endswith(":") or len(label) > 40:
                    continue
                key = label.rstrip(":").strip()
                if key and key not in info:
                    info[key] = clean_text(raw[column + 1])
        if header_at is None:
            raise SystemExit(f"no ACADEMIC RECORD block in {rel_posix(workbook_path)}/{sheet_name}")

        header = [clean_text(cell) for cell in rows[header_at]]
        idx_sem = header_index(header, "Semester")
        idx_code = header_index(header, "Course Code")
        idx_title = header_index(header, "Course Title")
        idx_units = header_index(header, "Units")
        idx_grade = header_index(header, "Grade")
        idx_score = header_index(header, "Score %")
        idx_cat = header_index(header, "Category")
        idx_status = header_index(header, "Status")

        student_no = clean_text(info.get("Student ID"))
        name = clean_text(info.get("Name")) or sheet_name
        major_en = clean_text(info.get("Major"))
        college = clean_text(info.get("College"))
        cum_gpa = to_decimal(info.get("Cumulative GPA"))
        total_credits = to_decimal(info.get("Total Credits"))

        entry_year = entry_year_from_student_no(student_no)
        if entry_year is not None:
            stats["derived_entry_years"].append({"student_no": student_no, "entry_year": entry_year})
        else:
            stats["skipped_transcript_rows"].append(
                {"sheet": sheet_name, "reason": "student number has no 2-digit year prefix",
                 "value": student_no}
            )

        email = basic.get(name, {}).get("email", "")
        if not email:
            email = f"{student_no}@link.cuhk.edu.cn"
            stats["derived_emails"].append({"sheet": sheet_name, "email": email})

        basic_year = basic.get(name, {}).get("year", "")
        if basic_year and entry_year is not None and basic_year.isdigit() and int(basic_year) != entry_year:
            stats["entry_year_mismatch"].append(
                {"sheet": sheet_name, "student_no": student_no,
                 "from_student_no": entry_year, "from_basic_info": int(basic_year)}
            )

        records: list[dict] = []
        for raw in rows[header_at + 1:]:
            first = clean_text(raw[0] if raw else "")
            if not first:
                continue
            if first.upper().startswith("SEMESTER SUMMARY") or first == "Semester":
                break
            code = code_key(raw[idx_code] if idx_code < len(raw) else "")
            if not code:
                continue
            if not re.search(r"[A-Z]", code):
                # Guards against syllabus summary rows that leak a bare number
                # into the course-code column.
                skipped.append({"sheet": sheet_name, "semester": first, "course_code": code,
                                "reason": "course code contains no letters"})
                continue

            grade = clean_text(raw[idx_grade] if idx_grade < len(raw) else "")
            score = to_decimal(raw[idx_score] if idx_score < len(raw) else None)
            status = clean_text(raw[idx_status] if idx_status < len(raw) else "")
            category = clean_text(raw[idx_cat] if idx_cat < len(raw) else "")
            units = to_decimal(raw[idx_units] if idx_units < len(raw) else None)
            if score is None:
                stats["blank_scores"] += 1
            grade_counts[grade] = grade_counts.get(grade, 0) + 1
            status_counts[status] = status_counts.get(status, 0) + 1
            semesters.add(first)

            records.append({
                "semester": first,
                "course_code": code,
                "course_title": clean_text(raw[idx_title] if idx_title < len(raw) else ""),
                "units": units if units is not None else Decimal("0.00"),
                "grade": grade,
                "score": score,
                "category": category,
                "status": status,
            })

        stats["sheets_parsed"] += 1
        stats["record_rows"] += len(records)
        students.append({
            "sys_user_id": sys_user_id,
            "sheet": sheet_name,
            "workbook": workbook_path,
            "student_no": student_no,
            "name": name,
            "major_en": major_en,
            "college": college,
            "email": email,
            "entry_year": entry_year,
            "cum_gpa": cum_gpa,
            "total_credits": total_credits,
            "records": records,
            "hometown": basic.get(name, {}).get("hometown", ""),
        })
        for record in records:
            transcript_codes.add(record["course_code"])

    stats["skipped_transcript_rows"] = skipped
    stats["grade_counts"] = dict(sorted(grade_counts.items()))
    stats["status_counts"] = dict(sorted(status_counts.items()))
    stats["semesters"] = sorted(semesters)
    stats["distinct_course_codes"] = len(transcript_codes)
    return students, stats


def load_competency() -> tuple[list[dict], list[dict], dict]:
    """Read the competency pools and the course/pool relevance scores."""
    pool_rows = read_sheet(IN_COMPETENCY, "Competency Pools")
    header = [clean_text(cell) for cell in pool_rows[0]]
    idx_school = header_index(header, "School")
    idx_abbr = header_index(header, "School Abbr")
    idx_item = header_index(header, "Competency Pool Item")
    idx_def = header_index(header, "Definition")
    idx_kw = header_index(header, "Keywords")
    idx_merged = header_index(header, "Merged From Programmes")

    pools: list[dict] = []
    seen_pools: set[tuple[str, str]] = set()
    for raw in pool_rows[1:]:
        def cell(position: int) -> str:
            return clean_text(raw[position] if position < len(raw) else "")

        item = cell(idx_item)
        abbr = cell(idx_abbr).upper()
        if not item or not abbr:
            continue
        if (abbr, item) in seen_pools:
            continue
        seen_pools.add((abbr, item))
        pools.append({
            "school": cell(idx_school),
            "school_abbr": abbr,
            "name": item,
            "description": cell(idx_def),
            "keywords": cell(idx_kw),
            "merged_from": cell(idx_merged),
        })

    score_rows = read_sheet(IN_COMPETENCY, "All Course Scores")
    score_header = [clean_text(cell) for cell in score_rows[0]]
    idx_code = header_index(score_header, "Course Code")
    idx_pool_item = header_index(score_header, "Competency Pool Item")
    idx_pool_school = header_index(score_header, "Competency Pool School")
    idx_score = header_index(score_header, "Score / 10")
    idx_level = header_index(score_header, "Level")
    idx_rationale = header_index(score_header, "Rationale")
    idx_keywords = header_index(score_header, "Matched Keywords")

    stats = {
        "pools": len(pools),
        "pool_schools": {},
        "score_rows_total": 0,
        "score_rows_at_or_above_threshold": 0,
        "score_rows_below_threshold": 0,
        "min_score_kept": "6.0",
        "unresolved_course_codes": [],
    }
    school_counts: dict[str, int] = {}
    for pool in pools:
        school_counts[pool["school_abbr"]] = school_counts.get(pool["school_abbr"], 0) + 1
    stats["pool_schools"] = dict(sorted(school_counts.items()))

    scores: list[dict] = []
    for raw in score_rows[1:]:
        code = code_key(raw[idx_code] if idx_code < len(raw) else "")
        if not code:
            continue
        stats["score_rows_total"] += 1
        score = to_decimal(raw[idx_score] if idx_score < len(raw) else None)
        if score is None or score < Decimal("6.0"):
            stats["score_rows_below_threshold"] += 1
            continue
        stats["score_rows_at_or_above_threshold"] += 1
        scores.append({
            "code": code,
            "pool_item": clean_text(raw[idx_pool_item] if idx_pool_item < len(raw) else ""),
            "pool_school": clean_text(raw[idx_pool_school] if idx_pool_school < len(raw) else ""),
            "score": score,
            "level": clean_text(raw[idx_level] if idx_level < len(raw) else ""),
            "rationale": clean_text(raw[idx_rationale] if idx_rationale < len(raw) else ""),
            "matched_keywords": clean_text(raw[idx_keywords] if idx_keywords < len(raw) else ""),
        })

    return pools, scores, stats


def load_v2_accounts() -> dict[int, dict]:
    """Read the demo sys_user rows from V2 so the student mapping can be checked."""
    if not IN_PERMISSIONS.exists():
        return {}
    text = IN_PERMISSIONS.read_text(encoding="utf-8", errors="replace")
    accounts: dict[int, dict] = {}
    pattern = re.compile(
        r"\(\s*(\d+),\s*(\d+),\s*'([^']*)',\s*'([^']*)',\s*'[^']*',\s*'([^']*)'", re.S
    )
    for match in pattern.finditer(text):
        user_id = int(match.group(1))
        if user_id < 100:
            continue
        accounts[user_id] = {
            "dept_id": int(match.group(2)),
            "user_name": match.group(3),
            "nick_name": match.group(4),
            "email": match.group(5),
        }
    return accounts


# ---------------------------------------------------------------------------
# 5. Course id assignment + prerequisite parsing
# ---------------------------------------------------------------------------

def assign_course_ids(courses: list[dict]) -> dict[str, int]:
    """course_id = 100000 + rank, ranked by course code (0-based)."""
    code_to_id: dict[str, int] = {}
    for rank, course in enumerate(sorted(courses, key=lambda item: item["code"])):
        course["course_id"] = COURSE_ID_BASE + rank
        code_to_id[course["code_key"]] = course["course_id"]
    courses.sort(key=lambda item: item["course_id"])
    return code_to_id


def normalise_spaced_codes(text: str) -> str:
    """Join 'ACT 2111' style codes so the code regex can see them."""
    return SPACED_CODE_RE.sub(lambda m: f"{m.group(1)}{m.group(2)}{m.group(3)}", text)


def normalise_alternatives(text: str) -> str:
    """Rewrite 'CODE/CODE' alternations as 'CODE or CODE' before splitting."""
    normalised = normalise_spaced_codes(text)
    previous = None
    while previous != normalised:
        previous = normalised
        normalised = SLASH_ALT_RE.sub(r"\1 or \2", normalised)
    return normalised


def is_no_prerequisite(text: str) -> bool:
    """True when the whole prerequisite cell says that there is no prerequisite."""
    cleaned = text.lower().replace("-", " ")
    cleaned = re.sub(r"[^a-z/ ]+", " ", cleaned)
    cleaned = " ".join(cleaned.split())
    return any(re.fullmatch(pattern, cleaned) for pattern in NO_PREREQ_PATTERNS)


def parse_prerequisite_text(text: str, course_code_key: str, code_to_id: dict[str, int]):
    """Parse SIS prerequisite free text into (group_no, prereq_code) alternatives.

    Returns ``(alternatives, info)`` where ``alternatives`` is a list of
    ``(group_no, [prereq_course_id, ...], raw_fragment)`` and ``info`` carries
    per-text bookkeeping for the report.
    """
    info = {
        "no_prereq": False,
        "self_refs": 0,
        "unresolved": [],
        "alternatives": 0,
        "drop_prefixes": [],
    }
    stripped = clean_text(text)
    if not stripped:
        info["no_prereq"] = True
        return [], info
    if is_no_prerequisite(stripped):
        info["no_prereq"] = True
        return [], info

    parsed = normalise_alternatives(stripped)
    alternatives: list[tuple[int, list[int], str]] = []
    group_no = 0
    for fragment in ALT_SPLIT_RE.split(parsed):
        fragment = fragment.strip()
        if not fragment:
            continue
        found: list[int] = []
        found_keys: list[str] = []
        for candidate in COURSE_CODE_RE.findall(fragment.upper()):
            prefix = re.match(r"[A-Z]+", candidate).group(0)
            if prefix in CODE_STOP_PREFIXES:
                info["drop_prefixes"].append(candidate)
                continue
            if candidate == course_code_key:
                info["self_refs"] += 1
                continue
            course_id = code_to_id.get(candidate)
            if course_id is None:
                info["unresolved"].append(candidate)
                continue
            if candidate in found_keys:
                continue
            found_keys.append(candidate)
            found.append(course_id)
        if not found:
            continue
        group_no += 1
        alternatives.append((group_no, found, collapse_ws(fragment)[:500]))
    info["alternatives"] = len(alternatives)
    return alternatives, info


def build_prereq_rows(courses: list[dict], code_to_id: dict[str, int]) -> tuple[list[dict], dict]:
    """Build edu_course_prereq rows for every course with parseable prerequisites."""
    stats = {
        "texts_total": 0,
        "texts_blank": 0,
        "no_prerequisite_texts": 0,
        "texts_without_code": 0,
        "texts_with_edges": 0,
        "groups": 0,
        "rows_emitted": 0,
        "self_references_skipped": 0,
        "duplicate_edges_skipped": 0,
        "unresolved_codes": [],
        "unparsed_examples": [],
    }
    unresolved: dict[str, int] = {}
    rows: list[dict] = []
    used_pairs: set[tuple[int, int]] = set()

    for course in courses:
        text = course["prereq_text"]
        if not text:
            continue
        stats["texts_total"] += 1
        if not text.strip():
            stats["texts_blank"] += 1
            continue
        alternatives, info = parse_prerequisite_text(text, course["code_key"], code_to_id)
        stats["self_references_skipped"] += info["self_refs"]
        for code in info["unresolved"]:
            unresolved[code] = unresolved.get(code, 0) + 1
        if info["no_prereq"]:
            stats["no_prerequisite_texts"] += 1
            continue
        if not alternatives:
            stats["texts_without_code"] += 1
            if len(stats["unparsed_examples"]) < 25:
                stats["unparsed_examples"].append(
                    {"course": course["code"], "text": collapse_ws(text)[:200]}
                )
            continue
        stats["texts_with_edges"] += 1
        for group_no, prereq_ids, fragment in alternatives:
            stats["groups"] += 1
            for prereq_id in prereq_ids:
                pair = (course["course_id"], prereq_id)
                if pair in used_pairs:
                    stats["duplicate_edges_skipped"] += 1
                    continue
                used_pairs.add(pair)
                rows.append({
                    "course_id": course["course_id"],
                    "prereq_course_id": prereq_id,
                    "group_no": group_no,
                    "raw_text": fragment,
                    "create_by": SEED_AUTHOR,
                    "create_time": SEED_TIMESTAMP,
                })
                stats["rows_emitted"] += 1

    stats["unresolved_codes"] = [
        {"code": code, "occurrences": count}
        for code, count in sorted(unresolved.items())
    ]
    rows.sort(key=lambda row: (row["course_id"], row["group_no"], row["prereq_course_id"]))
    for rank, row in enumerate(rows):
        row["id"] = SEQ_ID_BASE + rank
    return rows, stats


# ---------------------------------------------------------------------------
# 6. Majors, curricula and curriculum items
# ---------------------------------------------------------------------------

def slug_major_code(name: str, mapping: dict[str, str]) -> str:
    if name in mapping:
        return mapping[name]
    slug = re.sub(r"[^A-Za-z0-9]+", "-", name).strip("-").upper()
    return slug[:64] or "MAJOR"


def build_majors_and_curricula(
    rules: dict,
    students: list[dict],
    course_list: list[dict],
    code_to_id: dict[str, int],
) -> tuple[list[dict], list[dict], list[dict], dict]:
    """Seed edu_major / edu_curriculum / edu_curriculum_item from the SDS index."""
    stats = {
        "majors_from_rules": 0,
        "majors_student_only": 0,
        "curricula": 0,
        "curriculum_items": 0,
        "rule_programmes_without_course_list": [],
        "course_list_programmes_without_rule": [],
        "variants_with_source_matched": [],
        "variants_without_source_matched": [],
        "total_credits_unresolved": [],
        "requirement_type_counts": {},
        "requirement_type_fallback": {},
        "items_without_catalogue_course": {},
        "curriculum_major_missing": [],
    }

    # --- majors that carry curricula (from the rules index) -----------------
    major_specs: dict[str, dict] = {}
    for rule in rules.get("rules", []):
        program = clean_text(rule.get("program"))
        if not program:
            continue
        major_key = clean_text(rule.get("majorKey"))
        major_specs[program] = {
            "major_code": (major_key.upper() or slug_major_code(program, {})),
            "major_name_en": program,
            "major_name_zh": PROGRAMME_ALIASES.get(program, ""),
            "school_abbr": clean_text(rules.get("school")) or "SDS",
            "school_name": SCHOOL_ZH.get(clean_text(rules.get("school")) or "SDS", ""),
            "remark": "Seeded from data/graduation_rules/SDS_major_rules_index.json",
            "from_rules": True,
            "variants": rule.get("variants", []),
        }
        stats["majors_from_rules"] += 1

    # --- majors that only appear in the demo student sheets -----------------
    for student in students:
        major_en = student["major_en"]
        if not major_en or major_en in major_specs:
            continue
        major_specs[major_en] = {
            "major_code": slug_major_code(major_en, STUDENT_MAJOR_CODES),
            "major_name_en": major_en,
            "major_name_zh": "",
            "school_abbr": "SME",
            "school_name": SCHOOL_ZH["SME"],
            "remark": "Student major only: not present in SDS_major_rules_index.json",
            "from_rules": False,
            "variants": [],
        }
        stats["majors_student_only"] += 1

    majors: list[dict] = []
    for rank, name in enumerate(sorted(major_specs, key=lambda value: value.lower())):
        spec = major_specs[name]
        majors.append({
            "major_id": ENTITY_ID_BASE + rank,
            "major_code": spec["major_code"][:64],
            "major_name_zh": spec["major_name_zh"],
            "major_name_en": spec["major_name_en"][:128],
            "school_abbr": spec["school_abbr"],
            "school_name": spec["school_name"],
            "degree_type": "",
            "duration_years": Decimal("4.0"),
            "status": "0",
            "del_flag": "0",
            "create_by": SEED_AUTHOR,
            "create_time": SEED_TIMESTAMP,
            "update_by": "",
            "update_time": None,
            "remark": spec["remark"],
        })
    major_id_by_name = {major["major_name_en"]: major["major_id"] for major in majors}
    stats["curriculum_major_missing"] = [
        name for name in major_specs if name not in major_id_by_name
    ]

    # --- course rows per programme (for curriculum items) -------------------
    entries_by_programme: dict[str, list[dict]] = {}
    for entry in course_list:
        entries_by_programme.setdefault(entry["programme"], []).append(entry)

    aliases_reverse = {zh: en for en, zh in PROGRAMME_ALIASES.items()}
    claimed_programmes: set[str] = set()

    # --- curricula + items -------------------------------------------------
    curricula: list[dict] = []
    items: list[dict] = []
    requirement_counts: dict[str, int] = {}
    requirement_fallback: dict[str, int] = {}
    sort_order_by_curriculum: dict[int, int] = {}
    item_order = 0

    for program in sorted(major_specs, key=lambda value: value.lower()):
        spec = major_specs[program]
        if not spec["from_rules"]:
            continue
        major_id = major_id_by_name[program]
        programme_zh = PROGRAMME_ALIASES.get(program, "")
        programme_entries = entries_by_programme.get(programme_zh, [])
        if not programme_entries:
            stats["rule_programmes_without_course_list"].append(
                {"program": program, "expected_programme": programme_zh}
            )

        for variant in spec["variants"]:
            year_from = variant.get("from")
            year_to = variant.get("to")
            source = clean_text(variant.get("source"))
            major_units = to_decimal(variant.get("totalMajorUnits"))
            if year_from is None:
                continue
            if year_to is None:
                label = f"{year_from}-{(year_from + 1) % 100:02d} and thereafter"
            elif int(year_to) == int(year_from):
                label = f"{year_from}-{(year_from + 1) % 100:02d}"
            else:
                label = (f"{year_from}-{(year_from + 1) % 100:02d} to "
                         f"{year_to}-{(int(year_to) + 1) % 100:02d}")
            curriculum_id = ENTITY_ID_BASE + len(curricula)
            curricula.append({
                "curriculum_id": curriculum_id,
                "major_id": major_id,
                "version_label": label,
                "version_year": int(year_from),
                "effective_from": int(year_from),
                "effective_to": None if year_to is None else int(year_to),
                "total_credits": Decimal("0.0"),
                "major_credits": major_units if major_units is not None else Decimal("0.0"),
                "source_doc": source,
                "status": "0",
                "del_flag": "0",
                "create_by": SEED_AUTHOR,
                "create_time": SEED_TIMESTAMP,
                "update_by": "",
                "update_time": None,
                "remark": ("total_credits unresolved: general/college requirement is not in "
                           "the listed inputs"),
            })
            stats["curricula"] += 1
            stats["total_credits_unresolved"].append({
                "curriculum_id": curriculum_id,
                "major": program,
                "version_year": int(year_from),
                "major_credits": str(major_units),
                "total_credits": "0.0",
            })

            # Prefer the rows extracted from the variant's own source PDF.
            source_key = source.strip().lower()
            variant_entries = [
                entry for entry in programme_entries
                if entry["source_pdf"].strip().lower() == source_key
            ]
            if variant_entries:
                stats["variants_with_source_matched"].append(
                    {"major": program, "version_year": int(year_from), "rows": len(variant_entries)}
                )
            else:
                variant_entries = programme_entries
                stats["variants_without_source_matched"].append(
                    {"major": program, "version_year": int(year_from),
                     "source_doc": source, "fallback_rows": len(variant_entries)}
                )

            for entry in variant_entries:
                section = entry["section"]
                requirement_type = DEFAULT_REQUIREMENT_TYPE
                matched = False
                for candidate, pattern in SECTION_RULES:
                    if section and pattern.search(section):
                        requirement_type = candidate
                        matched = True
                        break
                if not matched:
                    key = section or "<empty section>"
                    requirement_fallback[key] = requirement_fallback.get(key, 0) + 1
                requirement_counts[requirement_type] = requirement_counts.get(requirement_type, 0) + 1

                course_id = code_to_id.get(entry["code"])
                if course_id is None:
                    stats["items_without_catalogue_course"][entry["code"]] = (
                        stats["items_without_catalogue_course"].get(entry["code"], 0) + 1
                    )
                item_order += 1
                sort_order_by_curriculum[curriculum_id] = (
                    sort_order_by_curriculum.get(curriculum_id, 0) + 1)
                items.append({
                    "item_id": SEQ_ID_BASE + item_order - 1,
                    "curriculum_id": curriculum_id,
                    "course_id": course_id,
                    "course_code": entry["code"],
                    "requirement_type": requirement_type,
                    "section": section,
                    "stream": section if requirement_type == "STREAM" else "",
                    "min_units": entry["units"] if entry["units"] is not None else Decimal("0.0"),
                    "sort_order": sort_order_by_curriculum[curriculum_id],
                    "create_by": SEED_AUTHOR,
                    "create_time": SEED_TIMESTAMP,
                })
            claimed_programmes.add(programme_zh)

    stats["curriculum_items"] = len(items)
    stats["requirement_type_counts"] = dict(sorted(requirement_counts.items()))
    stats["requirement_type_fallback"] = dict(
        sorted(((key, value) for key, value in requirement_fallback.items()),
               key=lambda pair: (-pair[1], pair[0]))
    )
    stats["items_without_catalogue_course"] = dict(
        sorted(stats["items_without_catalogue_course"].items(), key=lambda pair: (-pair[1], pair[0]))
    )
    stats["course_list_programmes_without_rule"] = [
        {"programme": name, "rows": len(entries),
         "english_guess": aliases_reverse.get(name, "")}
        for name, entries in sorted(entries_by_programme.items())
        if name not in claimed_programmes
    ]
    return majors, curricula, items, stats


# ---------------------------------------------------------------------------
# 7. Terms, teacher, students, offerings, enrollments, course records
# ---------------------------------------------------------------------------

def term_from_semester(semester: str):
    """'2022-23 Term 1' -> ('2022-23-1', '2022-23', 1); summer -> index 3."""
    text = collapse_ws(semester)
    match = re.match(r"^(\d{4})-(\d{2})\s+(Term|Summer)\s*(\d?)$", text, re.I)
    if match:
        academic_year = f"{match.group(1)}-{match.group(2)}"
        kind = match.group(3).lower()
        if kind == "summer":
            index = 3
        else:
            index = int(match.group(4) or 1)
        return f"{academic_year}-{index}", academic_year, index
    return None, None, None


def build_terms(students: list[dict]) -> tuple[list[dict], dict]:
    stats = {"from_transcripts": 0, "unparsed_semesters": [], "current": CURRENT_TERM_CODE}
    term_specs: dict[str, dict] = {}
    for student in students:
        for record in student["records"]:
            code, academic_year, index = term_from_semester(record["semester"])
            if code is None:
                if record["semester"] not in stats["unparsed_semesters"]:
                    stats["unparsed_semesters"].append(record["semester"])
                continue
            term_specs.setdefault(code, {
                "term_code": code,
                "term_name": collapse_ws(record["semester"]),
                "academic_year": academic_year,
                "term_index": index,
                "is_current": "0",
            })
            stats["from_transcripts"] += 1
    term_specs[CURRENT_TERM_CODE] = {
        "term_code": CURRENT_TERM_CODE,
        "term_name": CURRENT_TERM_NAME,
        "academic_year": "2026-27",
        "term_index": 1,
        "is_current": "1",
    }
    terms: list[dict] = []
    for rank, code in enumerate(sorted(term_specs)):
        spec = term_specs[code]
        terms.append({
            "term_id": ENTITY_ID_BASE + rank,
            "term_code": spec["term_code"],
            "term_name": spec["term_name"],
            "academic_year": spec["academic_year"],
            "term_index": spec["term_index"],
            "start_date": None,
            "end_date": None,
            "is_current": spec["is_current"],
            "status": "0",
            "create_time": SEED_TIMESTAMP,
        })
    return terms, stats


def build_teacher() -> list[dict]:
    return [{
        "teacher_id": ENTITY_ID_BASE,
        "sys_user_id": TEACHER_SYS_USER_ID,
        "teacher_no": TEACHER_NO,
        "name": TEACHER_NAME,
        "dept_id": TEACHER_DEPT_ID,
        "school": SCHOOL_ZH["SDS"],
        "title": TEACHER_TITLE,
        "status": "0",
        "del_flag": "0",
        "create_by": SEED_AUTHOR,
        "create_time": SEED_TIMESTAMP,
        "update_by": "",
        "update_time": None,
    }]


def build_students(students: list[dict], major_id_by_name: dict[str, int],
                   accounts: dict[int, dict]) -> tuple[list[dict], dict]:
    stats = {
        "students": 0,
        "missing_sys_user": [],
        "unknown_major": [],
        "email_mismatch_with_v2": [],
        "name_mismatch_with_v2": [],
    }
    rows: list[dict] = []
    for rank, student in enumerate(sorted(students, key=lambda item: item["sys_user_id"])):
        account = accounts.get(student["sys_user_id"])
        if account is None:
            stats["missing_sys_user"].append(student["sys_user_id"])
        else:
            if account["email"] and student["email"] and account["email"] != student["email"]:
                stats["email_mismatch_with_v2"].append({
                    "sys_user_id": student["sys_user_id"],
                    "workbook": student["email"], "v2": account["email"],
                })
            if account["nick_name"] and account["nick_name"].upper() != student["name"].upper():
                stats["name_mismatch_with_v2"].append({
                    "sys_user_id": student["sys_user_id"],
                    "workbook": student["name"], "v2": account["nick_name"],
                })
        major_id = major_id_by_name.get(student["major_en"])
        if major_id is None:
            stats["unknown_major"].append(student["major_en"])
        rows.append({
            "student_id": ENTITY_ID_BASE + rank,
            "sys_user_id": student["sys_user_id"],
            "student_no": student["student_no"],
            "name": student["name"],
            "major_id": major_id,
            "entry_year": student["entry_year"],
            "college": student["college"],
            "email": student["email"],
            "cum_gpa": student["cum_gpa"],
            "total_credits": student["total_credits"],
            "status": "0",
            "del_flag": "0",
            "create_by": SEED_AUTHOR,
            "create_time": SEED_TIMESTAMP,
            "update_by": "",
            "update_time": None,
            "remark": (f"demo transcript sheet: {student['sheet']}"
                       + (f"; hometown: {student['hometown']}" if student["hometown"] else "")),
        })
        stats["students"] += 1
    return rows, stats


def build_records(students: list[dict], student_rows: list[dict], terms: list[dict],
                  courses: list[dict], code_to_id: dict[str, int]) -> tuple[list[dict], dict]:
    """Seed edu_course_record from every transcript row."""
    stats = {
        "rows": 0,
        "unresolved_course_codes": [],
        "malformed_course_codes": [],
        "duplicate_keys_dropped": [],
        "passed_true": 0,
        "passed_false": 0,
        "record_status_counts": {},
    }
    student_id_by_sys_user = {row["sys_user_id"]: row["student_id"] for row in student_rows}
    term_id_by_code = {row["term_code"]: row["term_id"] for row in terms}
    catalogue_code_by_key = {course["code_key"]: course["code"] for course in courses}
    unresolved: dict[str, int] = {}
    malformed: dict[str, int] = {}
    status_counts: dict[str, int] = {}

    passing_grades = {"F", "W", "IP", "U"}
    rows: list[dict] = []
    used_keys: dict[tuple, int] = {}
    for student in sorted(students, key=lambda item: item["sys_user_id"]):
        student_id = student_id_by_sys_user[student["sys_user_id"]]
        for record in student["records"]:
            term_code, _academic_year, _index = term_from_semester(record["semester"])
            term_id = term_id_by_code.get(term_code)
            key = (student_id, record["course_code"], term_id)
            if key in used_keys:
                stats["duplicate_keys_dropped"].append({
                    "student_no": student["student_no"],
                    "course_code": record["course_code"],
                    "semester": record["semester"],
                })
                continue
            used_keys[key] = 1

            course_id = code_to_id.get(record["course_code"])
            snapshot_code = catalogue_code_by_key.get(record["course_code"], record["course_code"])
            if course_id is None:
                unresolved[record["course_code"]] = unresolved.get(record["course_code"], 0) + 1
            if not COURSE_CODE_FULL_RE.match(record["course_code"]):
                malformed[record["course_code"]] = malformed.get(record["course_code"], 0) + 1

            grade = record["grade"].strip().upper()
            status = record["status"].strip()
            record_status = "COMPLETED" if status.lower() == "completed" else (
                status.upper().replace(" ", "_") or "COMPLETED")
            status_counts[record_status] = status_counts.get(record_status, 0) + 1
            passed = "1" if (grade not in passing_grades and status.lower() == "completed") else "0"
            if passed == "1":
                stats["passed_true"] += 1
            else:
                stats["passed_false"] += 1

            rows.append({
                "record_id": 0,  # assigned below
                "student_id": student_id,
                "course_id": course_id,
                "course_code": snapshot_code[:32],
                "course_title": record["course_title"],
                "term_id": term_id,
                "grade": record["grade"],
                "score": record["score"],
                "units": record["units"],
                "category": record["category"],
                "passed": passed,
                "record_status": record_status,
                "source": "DEMO",
                "del_flag": "0",
                "create_by": SEED_AUTHOR,
                "create_time": SEED_TIMESTAMP,
                "update_by": "",
                "update_time": None,
            })

    rows.sort(key=lambda row: (row["student_id"], row["term_id"] or 0, row["course_code"]))
    for rank, row in enumerate(rows):
        row["record_id"] = ENTITY_ID_BASE + rank
    stats["rows"] = len(rows)
    stats["unresolved_course_codes"] = [
        {"code": code, "occurrences": count}
        for code, count in sorted(unresolved.items())
    ]
    stats["malformed_course_codes"] = [
        {"code": code, "occurrences": count}
        for code, count in sorted(malformed.items())
    ]
    stats["record_status_counts"] = dict(sorted(status_counts.items()))
    return rows, stats


def build_offerings_and_enrollments(terms: list[dict], students: list[dict],
                                    student_rows: list[dict], courses: list[dict],
                                    code_to_id: dict[str, int], teacher_rows: list[dict],
                                    records: list[dict]) -> tuple[list[dict], list[dict], dict]:
    stats = {
        "requested_codes": list(DEMO_OFFERING_CODES),
        "missing_codes": [],
        "offerings": 0,
        "enrollments": 0,
        "enrollment_courses": [],
        "term": CURRENT_TERM_CODE,
    }
    term_id = next(row["term_id"] for row in terms if row["term_code"] == CURRENT_TERM_CODE)
    teacher_id = teacher_rows[0]["teacher_id"]
    catalogue_code_by_key = {course["code_key"]: course["code"] for course in courses}

    offering_specs = []
    for code in DEMO_OFFERING_CODES:
        course_id = code_to_id.get(code_key(code))
        if course_id is None:
            stats["missing_codes"].append(code)
            continue
        offering_specs.append({
            "course_id": course_id,
            "code_key": code_key(code),
            "display_code": catalogue_code_by_key.get(code_key(code), code),
        })
    offering_specs.sort(key=lambda spec: spec["display_code"])
    stats["offerings"] = len(offering_specs)

    demo_student = next(item for item in students
                        if item["sys_user_id"] == DEMO_ENROLLMENT_STUDENT_SYS_USER)
    student_id = next(row["student_id"] for row in student_rows
                      if row["sys_user_id"] == DEMO_ENROLLMENT_STUDENT_SYS_USER)
    completed_codes = {row["course_code"] for row in records if row["student_id"] == student_id}
    transcript_codes = {record["course_code"] for record in demo_student["records"]}

    candidates = [spec for spec in offering_specs
                  if spec["code_key"] not in completed_codes
                  and spec["code_key"] not in transcript_codes]
    if len(candidates) < DEMO_ENROLLMENT_COUNT:
        candidates = offering_specs
    enrollment_specs = candidates[:DEMO_ENROLLMENT_COUNT]
    enrollment_by_course = {spec["course_id"] for spec in enrollment_specs}

    offerings: list[dict] = []
    for rank, spec in enumerate(offering_specs):
        offerings.append({
            "offering_id": ENTITY_ID_BASE + rank,
            "course_id": spec["course_id"],
            "term_id": term_id,
            "teacher_id": teacher_id,
            "capacity": DEMO_OFFERING_CAPACITY,
            "enrolled_count": 1 if spec["course_id"] in enrollment_by_course else 0,
            "schedule_text": DEMO_SCHEDULE_SLOTS[rank % len(DEMO_SCHEDULE_SLOTS)],
            "status": "0",
            "create_by": SEED_AUTHOR,
            "create_time": SEED_TIMESTAMP,
            "update_by": "",
            "update_time": None,
        })

    enrollments: list[dict] = []
    offering_id_by_course = {row["course_id"]: row["offering_id"] for row in offerings}
    for rank, spec in enumerate(enrollment_specs):
        enrollments.append({
            "enrollment_id": ENTITY_ID_BASE + rank,
            "student_id": student_id,
            "offering_id": offering_id_by_course[spec["course_id"]],
            "status": "ENROLLED",
            "enrolled_at": SEED_TIMESTAMP,
            "create_by": SEED_AUTHOR,
            "create_time": SEED_TIMESTAMP,
            "update_by": "",
            "update_time": None,
        })
        stats["enrollment_courses"].append(spec["display_code"])
    stats["enrollments"] = len(enrollments)
    return offerings, enrollments, stats


# ---------------------------------------------------------------------------
# 8. Skill tags and course competencies
# ---------------------------------------------------------------------------

def build_skill_tags(pools: list[dict], code_to_id: dict[str, int],
                     scores: list[dict]) -> tuple[list[dict], list[dict], dict]:
    stats = {
        "pools": len(pools),
        "tags": 0,
        "competencies": 0,
        "duplicate_pairs_dropped": 0,
        "unresolved_course_codes": [],
        "score_rows_unresolved_course": 0,
        "pools_without_scores": [],
    }
    tags: list[dict] = []
    tag_id_by_key: dict[tuple[str, str], int] = {}
    per_school_index: dict[str, int] = {}
    for rank, pool in enumerate(sorted(pools, key=lambda item: (item["school_abbr"], item["name"].lower()))):
        abbr = pool["school_abbr"]
        per_school_index[abbr] = per_school_index.get(abbr, 0) + 1
        code = f"{abbr.lower()}-{per_school_index[abbr]:02d}"
        tags.append({
            "skill_tag_id": ENTITY_ID_BASE + rank,
            "code": code,
            "name": pool["name"],
            "category": pool["school"] or abbr,
            "school_abbr": abbr,
            "description": pool["description"],
            "keywords": pool["keywords"],
            "sort_order": per_school_index[abbr],
            "status": "0",
            "del_flag": "0",
            "create_time": SEED_TIMESTAMP,
        })
        tag_id_by_key[(abbr, pool["name"])] = ENTITY_ID_BASE + rank
    stats["tags"] = len(tags)

    unresolved: dict[str, int] = {}
    used_pairs: dict[tuple[int, int], dict] = {}
    occupied_tags: set[int] = set()
    for score in scores:
        pool_school = score["pool_school"]
        abbr = None
        for candidate in {pool["school_abbr"] for pool in pools}:
            if SCHOOL_ZH.get(candidate, "") == pool_school or candidate == pool_school:
                abbr = candidate
                break
        tag_id = tag_id_by_key.get((abbr, score["pool_item"])) if abbr else None
        if tag_id is None:
            # Fall back to a global item-name match (pool items are unique).
            matches = [key for key in tag_id_by_key if key[1] == score["pool_item"]]
            tag_id = tag_id_by_key[matches[0]] if len(matches) == 1 else None
        if tag_id is None:
            continue
        course_id = code_to_id.get(score["code"])
        if course_id is None:
            unresolved[score["code"]] = unresolved.get(score["code"], 0) + 1
            stats["score_rows_unresolved_course"] += 1
            continue
        occupied_tags.add(tag_id)
        pair = (course_id, tag_id)
        previous = used_pairs.get(pair)
        if previous is not None:
            stats["duplicate_pairs_dropped"] += 1
            if score["score"] > previous["score"]:
                previous.update({
                    "score": score["score"],
                    "level": score["level"],
                    "rationale": score["rationale"],
                    "matched_keywords": score["matched_keywords"],
                })
            continue
        used_pairs[pair] = {
            "id": 0,
            "course_id": course_id,
            "skill_tag_id": tag_id,
            "score": score["score"],
            "level": score["level"],
            "rationale": score["rationale"],
            "matched_keywords": score["matched_keywords"],
            "create_time": SEED_TIMESTAMP,
        }

    competencies = sorted(used_pairs.values(),
                          key=lambda row: (row["course_id"], row["skill_tag_id"]))
    for rank, row in enumerate(competencies):
        row["id"] = SEQ_ID_BASE + rank
    stats["competencies"] = len(competencies)
    stats["unresolved_course_codes"] = [
        {"code": code, "occurrences": count}
        for code, count in sorted(unresolved.items())
    ]
    stats["pools_without_scores"] = sorted(
        f"{tag['school_abbr']}:{tag['name']}" for tag in tags
        if tag["skill_tag_id"] not in occupied_tags
    )
    return tags, competencies, stats


# ---------------------------------------------------------------------------
# 9. SQL file rendering
# ---------------------------------------------------------------------------

def render_file(code: str, title: str, description: str, inputs: list[str],
                chunk_note: str, tables: list[list[dict]]) -> tuple[str, int]:
    """Render one generated migration; returns (content, statement_count)."""
    lines: list[str] = []
    lines.append("-- =====================================================================")
    lines.append(f"-- generated by {GENERATOR}")
    lines.append(f"-- {title}")
    for line in description.splitlines():
        lines.append(f"-- {line}" if line else "--")
    lines.append("-- ")
    lines.append("-- inputs:")
    for item in inputs:
        lines.append(f"--   {item}")
    lines.append(f"-- row chunking: {chunk_note}")
    lines.append("-- DO NOT EDIT: regenerate with the command in versions.md section 4.")
    lines.append("-- =====================================================================")
    lines.append("")
    lines.append("SET NAMES utf8mb4;")
    lines.append("SET FOREIGN_KEY_CHECKS = 0;")
    lines.append("")

    statements = 2  # SET NAMES + SET FOREIGN_KEY_CHECKS = 0
    for table in DELETE_ORDER[code]:
        lines.append(f"delete from {table};")
        statements += 1
    lines.append("")

    inserters: dict[str, TableInserter] = {}
    by_table = {table: rows for table, rows in zip(INSERT_ORDER[code], tables)}
    for table in INSERT_ORDER[code]:
        inserter = TableInserter(table)
        for row in by_table[table]:
            inserter.add(row)
        body = inserter.render()
        if body:
            lines.append(body)
            lines.append("")
        inserters[table] = inserter
        statements += inserter.statement_count

    lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    statements += 1
    lines.append("")
    content = "\n".join(lines)
    return content, statements


@dataclass
class GeneratedFile:
    code: str
    path: Path
    content: str
    statements: int
    rows: dict[str, int]

    @property
    def byte_size(self) -> int:
        return len(self.content.encode("utf-8"))

    @property
    def sha256(self) -> str:
        return sha256_bytes(self.content.encode("utf-8"))


def build_generated_files(model: Model) -> list[GeneratedFile]:
    files: list[GeneratedFile] = []

    # --- V3 -----------------------------------------------------------------
    content, statements = render_file(
        "V3",
        "课程目录与先修关系（生成）",
        "edu_course: one row per SIS catalogue course (sheet All Courses).\n"
        "edu_course_prereq: prerequisite edges parsed from the SIS free text.\n"
        "group_no semantics: same group = AND, different groups = OR alternatives.",
        [rel_posix(IN_SIS), rel_posix(IN_COURSE_LISTS)],
        "200 rows for edu_course, 500 for edu_course_prereq",
        [model.courses, model.prereqs],
    )
    files.append(GeneratedFile("V3", OUT_V3, content, statements,
                               {"edu_course": len(model.courses),
                                "edu_course_prereq": len(model.prereqs)}))

    # --- V4 -----------------------------------------------------------------
    content, statements = render_file(
        "V4",
        "专业与培养方案（生成）",
        "edu_major: SDS programme majors (rules index) + demo-student majors.\n"
        "edu_curriculum: one row per admission-year variant of a major.\n"
        "edu_curriculum_item: course requirements extracted from All_Course_Lists.",
        [rel_posix(IN_RULES), rel_posix(IN_COURSE_LISTS), rel_posix(IN_DEMO),
         rel_posix(IN_DEMO_SME)],
        "500 rows per INSERT for every table in this file",
        [model.majors, model.curricula, model.curriculum_items],
    )
    files.append(GeneratedFile("V4", OUT_V4, content, statements,
                               {"edu_major": len(model.majors),
                                "edu_curriculum": len(model.curricula),
                                "edu_curriculum_item": len(model.curriculum_items)}))

    # --- V5 -----------------------------------------------------------------
    content, statements = render_file(
        "V5",
        "学期、开课、演示学生与成绩（生成）",
        "edu_term: every distinct transcript semester + the current 2026-27-1 term.\n"
        "edu_teacher/edu_student: sys_user 201 and the demo students 101-108.\n"
        "edu_course_offering/edu_enrollment: current-term demo teaching load.\n"
        "edu_course_record: every transcript row with a catalogue course_id when resolved.",
        [rel_posix(IN_DEMO), rel_posix(IN_DEMO_SME), rel_posix(IN_PERMISSIONS),
         rel_posix(IN_SIS)],
        "500 rows per INSERT for every table in this file",
        [model.terms, model.teachers, model.students, model.offerings,
         model.enrollments, model.records],
    )
    files.append(GeneratedFile("V5", OUT_V5, content, statements,
                               {"edu_term": len(model.terms),
                                "edu_teacher": len(model.teachers),
                                "edu_student": len(model.students),
                                "edu_course_offering": len(model.offerings),
                                "edu_enrollment": len(model.enrollments),
                                "edu_course_record": len(model.records)}))

    # --- V6 -----------------------------------------------------------------
    content, statements = render_file(
        "V6",
        "技能标签与课程关联度（生成）",
        "edu_skill_tag: one row per competency pool item of the eight schools.\n"
        "edu_course_competency: course/pool relevance, only scores >= 6.0.",
        [rel_posix(IN_COMPETENCY), rel_posix(IN_SIS)],
        "500 rows per INSERT for every table in this file",
        [model.skill_tags, model.course_competencies],
    )
    files.append(GeneratedFile("V6", OUT_V6, content, statements,
                               {"edu_skill_tag": len(model.skill_tags),
                                "edu_course_competency": len(model.course_competencies)}))
    return files


# ---------------------------------------------------------------------------
# 10. Integrity validation (fails fast instead of emitting broken SQL)
# ---------------------------------------------------------------------------

def validate_model(model: Model) -> list[str]:
    checks: list[str] = []
    course_ids = {row["course_id"] for row in model.courses}
    major_ids = {row["major_id"] for row in model.majors}
    curriculum_ids = {row["curriculum_id"] for row in model.curricula}
    term_ids = {row["term_id"] for row in model.terms}
    student_ids = {row["student_id"] for row in model.students}
    teacher_ids = {row["teacher_id"] for row in model.teachers}
    offering_ids = {row["offering_id"] for row in model.offerings}
    tag_ids = {row["skill_tag_id"] for row in model.skill_tags}

    problems: list[str] = []
    for row in model.prereqs:
        if row["course_id"] not in course_ids:
            problems.append(f"prereq.course_id {row['course_id']} missing")
        if row["prereq_course_id"] not in course_ids:
            problems.append(f"prereq.prereq_course_id {row['prereq_course_id']} missing")
        if row["course_id"] == row["prereq_course_id"]:
            problems.append(f"prereq self reference {row['course_id']}")
    for row in model.curricula:
        if row["major_id"] not in major_ids:
            problems.append(f"curriculum.major_id {row['major_id']} missing")
    for row in model.curriculum_items:
        if row["curriculum_id"] not in curriculum_ids:
            problems.append(f"item.curriculum_id {row['curriculum_id']} missing")
        if row["course_id"] is not None and row["course_id"] not in course_ids:
            problems.append(f"item.course_id {row['course_id']} missing")
    for row in model.students:
        if row["major_id"] is not None and row["major_id"] not in major_ids:
            problems.append(f"student.major_id {row['major_id']} missing")
    for row in model.offerings:
        if row["course_id"] not in course_ids:
            problems.append(f"offering.course_id {row['course_id']} missing")
        if row["term_id"] not in term_ids:
            problems.append(f"offering.term_id {row['term_id']} missing")
        if row["teacher_id"] is not None and row["teacher_id"] not in teacher_ids:
            problems.append(f"offering.teacher_id {row['teacher_id']} missing")
    for row in model.enrollments:
        if row["student_id"] not in student_ids:
            problems.append(f"enrollment.student_id {row['student_id']} missing")
        if row["offering_id"] not in offering_ids:
            problems.append(f"enrollment.offering_id {row['offering_id']} missing")
    for row in model.records:
        if row["student_id"] not in student_ids:
            problems.append(f"record.student_id {row['student_id']} missing")
        if row["course_id"] is not None and row["course_id"] not in course_ids:
            problems.append(f"record.course_id {row['course_id']} missing")
        if row["term_id"] is not None and row["term_id"] not in term_ids:
            problems.append(f"record.term_id {row['term_id']} missing")
    for row in model.course_competencies:
        if row["course_id"] not in course_ids:
            problems.append(f"competency.course_id {row['course_id']} missing")
        if row["skill_tag_id"] not in tag_ids:
            problems.append(f"competency.skill_tag_id {row['skill_tag_id']} missing")

    # unique keys declared in V1
    checks.extend(_check_unique(model.courses, ["code"], "edu_course.code"))
    checks.extend(_check_unique(model.curricula, ["major_id", "version_year"],
                                "edu_curriculum(major_id,version_year)"))
    checks.extend(_check_unique(model.terms, ["term_code"], "edu_term.term_code"))
    checks.extend(_check_unique(model.students, ["student_no"], "edu_student.student_no"))
    checks.extend(_check_unique(model.students, ["sys_user_id"], "edu_student.sys_user_id"))
    checks.extend(_check_unique(model.teachers, ["teacher_no"], "edu_teacher.teacher_no"))
    checks.extend(_check_unique(model.teachers, ["sys_user_id"], "edu_teacher.sys_user_id"))
    checks.extend(_check_unique(model.offerings, ["course_id", "term_id"],
                                "edu_course_offering(course_id,term_id)"))
    checks.extend(_check_unique(model.enrollments, ["student_id", "offering_id"],
                                "edu_enrollment(student_id,offering_id)"))
    checks.extend(_check_unique(model.prereqs, ["course_id", "prereq_course_id"],
                                "edu_course_prereq(course_id,prereq_course_id)"))
    checks.extend(_check_unique(model.skill_tags, ["code"], "edu_skill_tag.code"))
    checks.extend(_check_unique(model.course_competencies, ["course_id", "skill_tag_id"],
                                "edu_course_competency(course_id,skill_tag_id)"))
    checks.extend(_check_unique(model.records, ["student_id", "course_code", "term_id"],
                                "edu_course_record(student_id,course_code,term_id)"))

    if problems:
        preview = "\n  ".join(problems[:20])
        raise SystemExit(
            f"model validation failed ({len(problems)} problems):\n  {preview}"
        )
    checks.append("all foreign keys resolve inside the generated files")
    checks.append("no self-referencing prerequisite rows")
    return checks


def _check_unique(rows: list[dict], keys: list[str], label: str) -> list[str]:
    seen: set[tuple] = set()
    duplicates = 0
    for row in rows:
        value = tuple(row[key] for key in keys)
        if value in seen:
            duplicates += 1
        seen.add(value)
    if duplicates:
        raise SystemExit(f"duplicate values for unique key {label}: {duplicates} rows")
    return [f"unique key {label} holds ({len(rows)} rows)"]


# ---------------------------------------------------------------------------
# 11. Report
# ---------------------------------------------------------------------------

def build_report(model: Model, files: list[GeneratedFile], stats: dict,
                 validation: list[str]) -> dict:
    inputs = []
    for path in (IN_SIS, IN_COURSE_LISTS, IN_DEMO, IN_DEMO_SME, IN_RULES, IN_COMPETENCY):
        inputs.append({
            "path": rel_posix(path),
            "size_bytes": path.stat().st_size,
            "sha256": sha256_file(path),
        })

    outputs = []
    for generated in files:
        outputs.append({
            "path": rel_posix(generated.path),
            "migration": generated.code,
            "size_bytes": generated.byte_size,
            "sha256": generated.sha256,
            "statements": generated.statements,
            "rows": generated.rows,
        })

    table_rows = {name: len(rows) for name, rows in model.table_rows().items()}

    defaulted = [
        {"table": "edu_course", "column": "title_en",
         "default": "'' (empty string)",
         "reason": "the SIS export has no English title; filled from "
                   "All_Course_Lists 'Course Title (English)' when the code is listed, "
                   "otherwise left empty"},
        {"table": "edu_course", "column": "category",
         "default": DEFAULT_CATEGORY,
         "reason": "heuristic from the SIS subject prefix "
                   "(ITE->IT, PED->PE, CHI/CLC->Chinese, ENG/ENL/ENB->English, "
                   "GE*/GGE/GFH/GFN/BGE->GenEd, everything else->Major)"},
        {"table": "edu_course", "column": "credits",
         "default": "0.00",
         "reason": "SIS units that are blank or ranges such as '0.00 - 10.00'"},
        {"table": "edu_course", "column": "level",
         "default": "0",
         "reason": "courses whose code carries no 3-4 digit numeric part"},
        {"table": "edu_curriculum", "column": "total_credits",
         "default": "0.0",
         "reason": "the general/college requirement is not part of the listed inputs, "
                   "so a 120-credit total cannot be derived (70 major + 48 assumed = 118)"},
        {"table": "edu_curriculum_item", "column": "requirement_type",
         "default": DEFAULT_REQUIREMENT_TYPE,
         "reason": "Section text is empty or matches none of the V1 vocabulary, "
                   "e.g. the extracted table header 'Code Course Title (English) ...'"},
        {"table": "edu_major", "column": "degree_type",
         "default": "'' (empty string)",
         "reason": "degree type is not present in the listed inputs"},
        {"table": "edu_major", "column": "major_name_zh",
         "default": "'' (empty string)",
         "reason": "SME student majors have no Chinese programme name in the listed inputs"},
        {"table": "edu_student", "column": "email",
         "default": "'<student_no>@link.cuhk.edu.cn'",
         "reason": "the four demo sheets of student_demo_data.xlsx carry no email column; "
                   "the address is derived and cross-checked against V2 sys_user"},
        {"table": "edu_student", "column": "entry_year",
         "default": "decoded from student_no (1YYxxxxxx -> 20YY)",
         "reason": "the admission year is only implicit in the student number; the decoded "
                   "value is cross-checked against Stu_basic_info.Year for the SME students"},
        {"table": "edu_term", "column": "start_date / end_date",
         "default": "NULL",
         "reason": "the transcripts carry no calendar dates, only 'YYYY-YY Term N'"},
        {"table": "edu_course_record", "column": "score",
         "default": "NULL",
         "reason": "pass/fail style rows (e.g. ITE1000 PA) and blank Score % cells"},
        {"table": "edu_course_record", "column": "record_status",
         "default": "COMPLETED",
         "reason": "every transcript row is marked Completed; passed='1' when the grade "
                   "is not F/W/IP/U and the status is Completed"},
    ]

    notes = [
        "Score % in the demo transcripts is a scaled cohort score (about 30-52), not a "
        "0-100 percentage, and it does not track the letter grade consistently.",
        "The STUDENT INFORMATION 'Cumulative GPA' of a sheet can differ from the last "
        "'Cumulative GPA' of its SEMESTER SUMMARY block (e.g. CHEN MIC 3.111 vs 3.153); "
        "edu_student.cum_gpa uses the STUDENT INFORMATION value, rounded to the two "
        "decimals that decimal(3,2) allows.",
        "Only 955 of the 3425 SIS catalogue codes appear in All_Course_Lists, so "
        "edu_course.title_en stays empty for the other 2470; conversely 496 codes listed "
        "in All_Course_Lists are absent from the SIS export and are not seeded as courses.",
        "Some SIS data cells are Chinese prose (grading_basis, component, school, "
        "academic_org) and are stored verbatim, including mixed '已评分'/'Pass/Fail'.",
        "Nested AND/OR prerequisite logic cannot be represented by the group_no model; "
        "text such as '(A or B) and (C or D)' is flattened to OR alternatives of the "
        "codes inside each split fragment (documented approximation).",
        "Prerequisite text is sometimes qualitative ('Basic knowledge of Python') or "
        "uses non-catalogue codes (STA2003, MATH2040, EIE3001): these produce no edges "
        "and are listed above instead of broken foreign keys.",
        "Competency scores cover 13 course codes that are not SIS codes: 12 are "
        "bracketed spellings such as 'ECO(2011)' that normalise to a real code, and "
        "DDA5002 (10 rows) does not exist in the export and is skipped.",
        "Only 3 of the 33 programmes in All_Course_Lists are covered by "
        "SDS_major_rules_index.json; the SME programmes (会计学/金融工程/...) have no rule "
        "variants, so no curriculum is generated for the SME demo students' majors.",
    ]

    report = {
        "generator": GENERATOR,
        "generator_version": GENERATOR_VERSION,
        "seed_timestamp": SEED_TIMESTAMP,
        "current_term": CURRENT_TERM_CODE,
        "id_policy": {
            "course_id": "100000 + rank after sorting courses by code (0-based)",
            "major_id / curriculum_id / term_id / student_id / teacher_id / "
            "offering_id / enrollment_id / record_id / skill_tag_id": "1000 + rank",
            "prereq_id / curriculum_item_id / competency_id": "1 + rank (insert order)",
        },
        "determinism": "no clock, no randomness; every list is sorted before ids are assigned",
        "idempotency": "delete-then-insert per table, FOREIGN_KEY_CHECKS disabled per file",
        "inputs": inputs,
        "outputs": outputs,
        "row_counts": table_rows,
        "validation": validation,
        "skipped_course_codes": {
            "prerequisites_not_in_catalogue": stats["prereqs"]["unresolved_codes"],
            "course_records_not_in_catalogue": stats["records"]["unresolved_course_codes"],
            "competency_scores_not_in_catalogue": stats["competency"]["unresolved_course_codes"],
            "curriculum_items_not_in_catalogue": [
                {"code": code, "occurrences": count}
                for code, count in stats["curriculum"]["items_without_catalogue_course"].items()
            ],
        },
        "unmatched_programmes": {
            "rule_programmes_without_course_list":
                stats["curriculum"]["rule_programmes_without_course_list"],
            "course_list_programmes_without_rule": [
                item["programme"]
                for item in stats["curriculum"]["course_list_programmes_without_rule"]
            ],
        },
        "courses": stats["courses"],
        "prerequisites": stats["prereqs"],
        "curriculum": stats["curriculum"],
        "students": stats["students"],
        "course_records": stats["records"],
        "terms": stats["terms"],
        "offerings": stats["offerings"],
        "students_vs_v2": stats["students_vs_v2"],
        "competency": stats["competency"],
        "defaulted_columns": defaulted,
        "data_quality_notes": notes,
    }
    return report


def write_text_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(content)


# ---------------------------------------------------------------------------
# 12. CLI
# ---------------------------------------------------------------------------

def build_model() -> tuple[Model, dict]:
    """Run every loader, assign ids and build the row model."""
    print("[1/7] reading All_Course_Lists (programme course lists) ...")
    titles, course_list = load_course_titles()

    print("[2/7] reading SIS course outlines ...")
    courses, course_stats = load_catalogue(titles)
    code_to_id = assign_course_ids(courses)
    print(f"      {len(courses)} courses, {len(course_list)} curriculum course rows")

    print("[3/7] parsing prerequisites ...")
    prereqs, prereq_stats = build_prereq_rows(courses, code_to_id)
    print(f"      {len(prereqs)} prerequisite edges")

    print("[4/7] reading demo students and transcripts ...")
    demo_students, transcript_stats = load_demo_students()
    accounts = load_v2_accounts()

    print("[5/7] building majors, curricula and course requirements ...")
    rules = load_rules()
    majors, curricula, curriculum_items, curriculum_stats = build_majors_and_curricula(
        rules, demo_students, course_list, code_to_id
    )
    major_id_by_name = {row["major_name_en"]: row["major_id"] for row in majors}
    print(f"      {len(majors)} majors, {len(curricula)} curricula, "
          f"{len(curriculum_items)} curriculum items")

    print("[6/7] building terms, students, offerings and course records ...")
    terms, term_stats = build_terms(demo_students)
    teachers = build_teacher()
    student_rows, student_stats = build_students(demo_students, major_id_by_name, accounts)
    records, record_stats = build_records(demo_students, student_rows, terms, courses, code_to_id)
    offerings, enrollments, offering_stats = build_offerings_and_enrollments(
        terms, demo_students, student_rows, courses, code_to_id, teachers, records
    )
    print(f"      {len(terms)} terms, {len(student_rows)} students, {len(records)} records, "
          f"{len(offerings)} offerings, {len(enrollments)} enrollments")

    print("[7/7] building skill tags and course competencies ...")
    pools, scores, competency_stats = load_competency()
    skill_tags, competencies, skill_stats = build_skill_tags(pools, code_to_id, scores)
    print(f"      {len(skill_tags)} skill tags, {len(competencies)} course competencies")

    model = Model(
        courses=courses,
        prereqs=prereqs,
        majors=majors,
        curricula=curricula,
        curriculum_items=curriculum_items,
        terms=terms,
        teachers=teachers,
        students=student_rows,
        offerings=offerings,
        enrollments=enrollments,
        records=records,
        skill_tags=skill_tags,
        course_competencies=competencies,
    )

    competency_summary = {
        "pools": competency_stats["pools"],
        "pool_schools": competency_stats["pool_schools"],
        "min_score_kept": competency_stats["min_score_kept"],
        "score_rows_total": competency_stats["score_rows_total"],
        "score_rows_at_or_above_threshold": competency_stats["score_rows_at_or_above_threshold"],
        "score_rows_below_threshold": competency_stats["score_rows_below_threshold"],
        "score_rows_unresolved_course": skill_stats["score_rows_unresolved_course"],
        "skill_tags": skill_stats["tags"],
        "competencies": skill_stats["competencies"],
        "duplicate_pairs_dropped": skill_stats["duplicate_pairs_dropped"],
        "unresolved_course_codes": skill_stats["unresolved_course_codes"],
        "pools_without_scores": skill_stats["pools_without_scores"],
    }
    stats = {
        "courses": course_stats,
        "prereqs": prereq_stats,
        "curriculum": curriculum_stats,
        "students": transcript_stats,
        "terms": term_stats,
        "offerings": offering_stats,
        "students_vs_v2": student_stats,
        "records": record_stats,
        "competency": competency_summary,
    }
    return model, stats


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="build_edu_seed.py",
        description="Generate backend/sql/edu/V3..V6 seed migrations from the offline "
                    "spreadsheets (deterministic, idempotent, no database required).",
    )
    group = parser.add_argument_group("what to write")
    group.add_argument("--all", action="store_true",
                       help="write V3-V6 plus pipeline/course_import/seed-report.json")
    group.add_argument("--courses", action="store_true",
                       help=f"write {rel_posix(OUT_V3)}")
    group.add_argument("--curricula", action="store_true",
                       help=f"write {rel_posix(OUT_V4)}")
    group.add_argument("--students", action="store_true",
                       help=f"write {rel_posix(OUT_V5)}")
    group.add_argument("--competency", action="store_true",
                       help=f"write {rel_posix(OUT_V6)}")
    group.add_argument("--report", action="store_true",
                       help=f"write {rel_posix(REPORT_PATH)}")
    args = parser.parse_args(argv)

    selected: list[str] = []
    if args.all:
        selected = ["courses", "curricula", "students", "competency"]
    else:
        for name in ("courses", "curricula", "students", "competency"):
            if getattr(args, name):
                selected.append(name)
    if not selected and not args.report:
        parser.error("choose --all, or at least one of "
                     "--courses/--curricula/--students/--competency/--report")

    model, stats = build_model()
    validation = validate_model(model)
    files = build_generated_files(model)
    by_code = {generated.code: generated for generated in files}

    written: list[Path] = []
    if "courses" in selected:
        generated = by_code["V3"]
        write_text_file(generated.path, generated.content)
        written.append(generated.path)
    if "curricula" in selected:
        generated = by_code["V4"]
        write_text_file(generated.path, generated.content)
        written.append(generated.path)
    if "students" in selected:
        generated = by_code["V5"]
        write_text_file(generated.path, generated.content)
        written.append(generated.path)
    if "competency" in selected:
        generated = by_code["V6"]
        write_text_file(generated.path, generated.content)
        written.append(generated.path)

    if args.all or args.report:
        report = build_report(model, files, stats, validation)
        write_text_file(REPORT_PATH,
                        json.dumps(report, ensure_ascii=False, indent=2) + "\n")
        written.append(REPORT_PATH)

    print()
    print("generated files:")
    for generated in files:
        tables = ", ".join(f"{name}={count}" for name, count in generated.rows.items())
        print(f"  {rel_posix(generated.path)}  statements={generated.statements}"
              f"  bytes={generated.byte_size}")
        print(f"      {tables}")
    if args.all or args.report:
        print(f"  {rel_posix(REPORT_PATH)}")
    print()
    print("written in this run:")
    for path in written:
        print(f"  {rel_posix(path)}")
    print()
    print("validation: OK (" + "; ".join(validation[:2]) + ")")
    return 0


if __name__ == "__main__":
    sys.exit(main())
