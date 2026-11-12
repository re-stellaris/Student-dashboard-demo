# course_import — offline ETL from spreadsheets to the `edu_*` seed migrations

`build_edu_seed.py` turns the project's existing spreadsheets into the four
generated demo-data migrations of the RuoYi-Vue education module
(`backend/sql/edu/V3`–`V6`). It is fully offline: no MySQL connection, no
network, no third-party libraries beyond `openpyxl`.

It **supersedes the legacy `pipeline/course_import/import_sis_courses.py`**,
which required a live MySQL server (`mysql.connector`, `MYSQL_URL`) and only
imported the SIS course catalogue into the old `courses` table. The legacy
script is kept for reference only; do not extend it.

## Command

```bash
# from the repository root
python pipeline/course_import/build_edu_seed.py --all
```

On the bundled Windows runtime:

```powershell
$env:PYTHONIOENCODING = "utf-8"
& "C:\Users\httcqwz\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe" `
    pipeline/course_import/build_edu_seed.py --all
```

`PYTHONIOENCODING=utf-8` matters on a GBK console; the generator itself only
prints ASCII progress lines, but the read/write of Chinese text is UTF-8.

### Options

| flag | writes |
|---|---|
| `--all` | `V3`–`V6` **and** `seed-report.json` |
| `--courses` | `backend/sql/edu/V3__edu_seed_course.sql` |
| `--curricula` | `backend/sql/edu/V4__edu_seed_curriculum.sql` |
| `--students` | `backend/sql/edu/V5__edu_seed_student.sql` |
| `--competency` | `backend/sql/edu/V6__edu_seed_competency.sql` |
| `--report` | `pipeline/course_import/seed-report.json` only |

Every invocation loads and models **all** inputs (so the report always matches
the SQL) and writes only the selected artefacts. Flags combine, e.g.
`--courses --students`.

Exit code is non-zero if an input column is missing or if internal validation
finds a dangling foreign key, a self-referencing prerequisite or a duplicate
unique key — it never writes broken SQL silently.

## Inputs (all already in the repository)

| file | used for |
|---|---|
| `data/course_catalog/sis_course_outlines_export.xlsx`, sheet `All Courses` | `edu_course` (one row per course code) + prerequisite free text |
| `data/course_catalog/All_Course_Lists.xlsx`, sheet `All Courses` | English/Chinese course titles, programme→course requirements (`edu_curriculum_item`) |
| `data/demo/student_demo_data.xlsx` (4 sheets) | 4 demo students' profiles and transcripts |
| `data/demo/stu_demo_SME.xlsx` (`Stu_basic_info` + 4 sheets) | 4 more demo students (email/year come from `Stu_basic_info`) |
| `data/graduation_rules/SDS_major_rules_index.json` | `edu_major` + `edu_curriculum` (one row per admission-year variant) |
| `data/competency/a_to_z_course_competency_relevance.xlsx` (`Competency Pools`, `All Course Scores`) | `edu_skill_tag` + `edu_course_competency` (scores ≥ 6.0 only) |
| `backend/sql/edu/V2__edu_permissions.sql` | read-only: validates the demo `sys_user` ids/emails, never modified |

`backend/sql/edu/V1__edu_schema.sql` is the authoritative schema: column names,
declared lengths and decimal precision are copied into the generator
(`TABLE_COLUMNS`) and are what truncation/`NULL` handling is based on.

## Outputs

| migration | tables | rows (current inputs) |
|---|---|---|
| `V3__edu_seed_course.sql` | `edu_course`, `edu_course_prereq` | 3425 / 2048 |
| `V4__edu_seed_curriculum.sql` | `edu_major`, `edu_curriculum`, `edu_curriculum_item` | 6 / 7 / 612 |
| `V5__edu_seed_student.sql` | `edu_term`, `edu_teacher`, `edu_student`, `edu_course_offering`, `edu_enrollment`, `edu_course_record` | 11 / 1 / 8 / 6 / 3 / 288 |
| `V6__edu_seed_competency.sql` | `edu_skill_tag`, `edu_course_competency` | 67 / 3910 |
| `seed-report.json` | counts, skips, defaults, data-quality notes | — |

`seed-report.json` is the authoritative record of what was generated; the table
above is only a snapshot of the current inputs.

Migration order is unchanged (see `versions.md` §4):
`ry_20260417.sql` → `quartz.sql` → `edu/V1` → `V2` → `V3` → `V4` → `V5` → `V6`.

## Determinism

Two runs on the same inputs produce **byte-identical** files:

* every surrogate id is a pure function of the business key —
  `course_id = 100000 + rank` after sorting courses by code,
  `major_id`/`curriculum_id`/`term_id`/`student_id`/`teacher_id`/`offering_id`/
  `enrollment_id`/`record_id`/`skill_tag_id = 1000 + rank` after sorting by the
  natural key, and `1 + rank` for the purely technical ids (`edu_course_prereq.id`,
  `edu_curriculum_item.item_id`, `edu_course_competency.id`);
* all row lists are sorted explicitly before ids are assigned — no reliance on
  dict iteration order, locale collation or file-system order;
* no clock and no randomness: every `create_time`/`update_time` is the constant
  `2026-11-15 00:00:00`, and timestamps never appear in the report;
* output is UTF-8 **without BOM** with LF line endings, so a `sha256sum`
  comparison across two runs is meaningful.

Check it yourself:

```powershell
Get-FileHash backend\sql\edu\V3__edu_seed_course.sql, backend\sql\edu\V4__edu_seed_curriculum.sql,
  backend\sql\edu\V5__edu_seed_student.sql, backend\sql\edu\V6__edu_seed_competency.sql,
  pipeline\course_import\seed-report.json -Algorithm SHA256
# rerun, hash again, compare
```

or run the bundled checker, which re-generates and compares hashes for you:

```bash
python pipeline/course_import/verify_seed.py
```

It also parses the SQL without a MySQL server and checks per-table row counts,
INSERT column lists against V1, declared string/decimal limits, foreign keys,
self-references, unique keys, id policy, chunk/statement sizes, encoding and
banner. Exit code 0 means everything passed.

## Idempotency

Each file is a plain, **transaction-less** script that:

1. starts with a `-- generated by pipeline/course_import/build_edu_seed.py` banner,
2. sets `SET NAMES utf8mb4;` and `SET FOREIGN_KEY_CHECKS = 0;`,
3. deletes every table it owns, child before parent,
4. re-inserts the same explicit ids, then sets `SET FOREIGN_KEY_CHECKS = 1;`.

Re-running V3→V6 (or a single file) therefore never duplicates rows and cannot
be broken by file order, because no foreign key is checked during the load and
every id is stable. Re-run **all four files in order** after a change: V3/V4
delete rows that V5 still references (`edu_course_record.course_id`,
`edu_student.major_id`), so applying only V3 or only V4 leaves those references
dangling until V5 is applied again.

Multi-row INSERTs are chunked by **row count and rendered size**
(200 rows for `edu_course`, 500 for the other tables, ≈1.2 MB / 0.8 MB caps), so
no single statement comes near the few-MB mark.

## SQL escaping and `NULL` handling

* single quotes are doubled, backslashes are escaped for MySQL's default mode,
* control characters other than `\n`/`\t` are stripped, CRLF is normalised,
* every string is truncated to the length declared in `V1__edu_schema.sql`,
* empty numerics and empty `TEXT`/`DATE`/`DATETIME` columns become `NULL`
  (`''` is only used for `varchar` columns whose schema default is `''`).

## Prerequisite parsing

`edu_course_prereq` keeps a disjunction of conjunction groups: rows that share
`group_no` must **all** be met (AND); different `group_no` values are
alternatives (OR). Examples of the transformation:

| SIS text | rows |
|---|---|
| `ACT2111 Introductory Financial Accounting` | g1: ACT2111 |
| `(ACT3111 & ACT3112) or ACT3011` | g1: ACT3111, ACT3112 · g2: ACT3011 |
| `ACT3111 Financial Reporting I\nACT3112 …\nOr\nACT3011 …` | g1: ACT3111, ACT3112 · g2: ACT3011 |
| `(CSC1001 or CSC1003 or CSC1005) or ECO3121 or ACT3311 or MIS2051` | g1: CSC1001 · g2: CSC1003 · g3: CSC1005 · g4: ECO3121 · g5: ACT3311 · g6: MIS2051 |
| `No prerequisite.` / `N/A` / `NA` / `None` / `Nil.` / `No prerequisite needed.` | no rows |
| `GFH1000/GFN1000` | g1: GFH1000 · g2: GFN1000 (`/` between two codes is an OR) |

Implementation notes:

* codes are extracted with `(?<![A-Z0-9])([A-Z]{3,5}\d{3,4}[A-Z]?)(?![A-Z0-9])`;
  the task's "4 letters + 4 digits" rule is implemented as this superset because
  the SIS catalogue also contains 3-letter subjects (`ACT2111`, `CSC3002`) and
  5-letter ones (`FEMBA6101`), plus honours suffixes (`ENG2002S`);
* `ACT 2111` (space inside the code) is normalised before matching;
* alternatives split on `or` / `OR` / `<newline>Or`, and `CODE/CODE`;
  codes inside one alternative share a `group_no`, which is exactly the
  "`&`/`and` inside one alternative = same group" rule;
* the raw fragment is kept in `raw_text` (whitespace-collapsed, ≤ 500 chars);
* codes that are not in the catalogue produce **no** row — they are listed in
  `seed-report.json` (`prerequisites.unresolved_codes`) instead of creating a
  broken foreign key;
* self-references are dropped (and counted).

## Demo identities

`edu_student` rows are keyed by the demo `sys_user` ids from
`V2__edu_permissions.sql`; the sheet → `sys_user_id` mapping is fixed:

| workbook | sheet | sys_user_id | student_no |
|---|---|---|---|
| `student_demo_data.xlsx` | `CHEN MIC` | 101 | 122040156 |
| `student_demo_data.xlsx` | `WANG SOPHIA` | 102 | 122040267 |
| `student_demo_data.xlsx` | `LIU JAMES` | 103 | 122040384 |
| `student_demo_data.xlsx` | `EAN JIE` | 104 | 122040491 |
| `stu_demo_SME.xlsx` | `Zixuan Wang` | 105 | 122010001 |
| `stu_demo_SME.xlsx` | `Muyang Liu` | 106 | 123030001 |
| `stu_demo_SME.xlsx` | `Zihan Chen` | 107 | 124010001 |
| `stu_demo_SME.xlsx` | `Ruohan Zhou` | 108 | 125030003 |

`entry_year` is decoded from the student number (`1YYxxxxxx` → 20YY, e.g.
`122040156` → 2022) and cross-checked against `Stu_basic_info.Year`; `email` is
taken from `Stu_basic_info` when present, otherwise derived as
`<student_no>@link.cuhk.edu.cn` and checked against V2. The year token is the
`YY` pair right after the leading undergraduate flag `1`; reading the literal
first two characters instead would turn `122040156` into 2012, which contradicts
the required 2022 and the `Year` column of `stu_demo_SME.xlsx`.

The teacher identity is fixed too: `sys_user_id` 201, `teacher_no` `T2026001`,
name `李老师`, `dept_id` 110 (数据科学学院). Six current-term
(`2026-27-1`) offerings are seeded from SDS/SME courses and three of them are
`ENROLLED` for student 101 so the teacher workspace and the dashboard have data.

## `seed-report.json`

The report is regenerated with every invocation and is deterministic too (no
timestamps). Its top-level keys:

| key | content |
|---|---|
| `inputs` | path/size/sha256 of the six input workbooks/JSON |
| `outputs` | path/size/sha256/statement-count/per-table row counts of V3–V6 |
| `row_counts` | rows per table in the model |
| `validation` | unique-key and foreign-key checks that passed before writing |
| `skipped_course_codes` | codes dropped per stage (prerequisites, course records, competency scores, curriculum items) |
| `unmatched_programmes` | rule programmes without a course list, and course-list programmes without a rule |
| `courses`, `prerequisites`, `curriculum`, `students`, `course_records`, `terms`, `offerings`, `students_vs_v2`, `competency` | per-stage detail, including `*_unresolved_codes`, fallbacks and counts |
| `defaulted_columns` | every column whose value had to be defaulted, with the reason |
| `data_quality_notes` | the source-workbook gaps listed below |

## Known data-quality gaps

The generator records every gap it hit in `seed-report.json`; the important ones:

* `title_en` exists for only ~955 of 3425 courses (the SIS export is Chinese;
  English titles come from `All_Course_Lists`).
* 4 SIS rows are duplicate spellings of the same course (`FIN(6106)` vs
  `FIN6106`); they are collapsed.
* Units such as `0.00 - 10.00` and blank units become `0.00`.
* `total_credits` is left at `0.0`: the general/college requirement is not in
  the listed inputs, so the 120-credit total cannot be derived (70 major + 48
  assumed = 118).
* `Score %` in the transcripts is a scaled cohort score (~30–52), not a
  percentage; pass/fail rows have no score (`NULL`).
* `Cumulative GPA` in the `STUDENT INFORMATION` block can differ from the last
  `SEMESTER SUMMARY` value; the header value is used and rounded to the two
  decimals the schema allows (`decimal(3,2)`).
* Some prerequisite text is qualitative (`Basic knowledge of Python`) or uses
  codes that do not exist (`STA2003`, `MATH2040`, `EIE3001`); no edges are
  created for those.
* Nested logic such as `(A or B) and (C or D)` cannot be expressed by
  `group_no`; it is flattened to OR alternatives (documented approximation).
* Only 3 of the 33 programmes in `All_Course_Lists` are covered by the SDS
  rules index, so the SME demo majors get an `edu_major` row but no curriculum.

## Files

```
pipeline/course_import/
├── README.md               # this guide
├── build_edu_seed.py       # the generator (stdlib + openpyxl only)
├── verify_seed.py          # optional offline checker for the generated SQL
├── seed-report.json        # generated audit report
└── import_sis_courses.py   # legacy, MySQL-bound importer (superseded)
```
