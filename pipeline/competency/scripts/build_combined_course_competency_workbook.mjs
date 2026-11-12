import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = path.resolve(".");
const lettersArg = (process.argv[2] || "IJKLMNOPQRSTUVWXYZ").trim().toUpperCase();
const letters = [...lettersArg];
const outputDir = path.join(root, "outputs");
const rangeLabel = letters.join("") === "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  ? "a_to_z"
  : `${letters[0].toLowerCase()}_to_${letters.at(-1).toLowerCase()}`;
const outputPath = path.join(outputDir, `${rangeLabel}_course_competency_relevance.xlsx`);
const previewPath = path.join(outputDir, `${rangeLabel}_course_competency_relevance_preview.png`);

function fit(text, max = 32000) {
  if (text == null) return "";
  const s = String(text);
  return s.length > max ? s.slice(0, max - 20) + " ...[truncated]" : s;
}

function writeTable(sheet, headers, matrix) {
  const all = [headers, ...matrix];
  const range = sheet.getRangeByIndexes(0, 0, Math.max(all.length, 1), headers.length);
  range.values = all;
  range.format = {
    font: { name: "Aptos", size: 10, color: "#1F2937" },
    alignment: { horizontal: "left", vertical: "top" },
    wrapText: true,
  };
  sheet.getRangeByIndexes(0, 0, 1, headers.length).format = {
    fill: "#284B63",
    font: { bold: true, color: "#FFFFFF", name: "Aptos", size: 10 },
    alignment: { horizontal: "center", vertical: "middle" },
  };
  range.format.borders = { preset: "all", style: "thin", color: "#D9E2EC" };
  sheet.freezePanes.freezeRows(1);
  sheet.showGridLines = false;
  return range;
}

const payloads = [];
for (const letter of letters) {
  const inputPath = path.join(outputDir, `${letter.toLowerCase()}_course_faculty_pool_relevance.json`);
  const text = await fs.readFile(inputPath, "utf8");
  payloads.push({ letter, path: inputPath, data: JSON.parse(text) });
}

const rows = payloads.flatMap(({ letter, data }) =>
  data.scored_rows.map((row) => ({ ...row, letter })),
);
rows.sort((a, b) => a.course_code.localeCompare(b.course_code) || b.score - a.score || a.competency.localeCompare(b.competency));

const excluded = payloads.flatMap(({ letter, data }) =>
  (data.excluded_ucore_courses || []).map((row) => ({ ...row, letter })),
);
excluded.sort((a, b) => String(a.course_code).localeCompare(String(b.course_code)));

const poolMap = new Map();
for (const { data } of payloads) {
  for (const pool of data.competency_pools || []) {
    const key = `${pool.school}||${pool.competency}`;
    if (!poolMap.has(key)) poolMap.set(key, pool);
  }
}
const usedSchools = new Set(rows.map((row) => row.competency_pool_school));
const pools = [...poolMap.values()]
  .filter((pool) => usedSchools.has(pool.school))
  .sort((a, b) => a.school.localeCompare(b.school) || a.competency.localeCompare(b.competency));

const workbook = Workbook.create();

const filterSummary = workbook.worksheets.add("Filter Summary");
const filterRows = payloads.map(({ letter, data }) => [
  letter,
  data.summary.courses_scored,
  data.summary.score_rows,
  data.summary.ucore_courses_excluded,
  data.summary.courses_skipped_no_syllabus,
]);
filterRows.push([
  "TOTAL",
  filterRows.reduce((sum, row) => sum + row[1], 0),
  filterRows.reduce((sum, row) => sum + row[2], 0),
  filterRows.reduce((sum, row) => sum + row[3], 0),
  filterRows.reduce((sum, row) => sum + row[4], 0),
]);
writeTable(filterSummary, ["Letter", "Courses Scored", "Score Rows", "Excluded UCore/Rule", "Skipped No Syllabus"], filterRows);
filterSummary.getRange("A:E").format.columnWidth = 20;

const detail = workbook.worksheets.add("All Course Scores");
const detailHeaders = [
  "Letter", "Course Code", "Course Title", "Offering School", "Competency Pool School",
  "Competency Pool Item", "Score / 10", "Level", "Rationale", "Matched Keywords",
  "Keyword Evidence", "Prefix Match", "Title Signal", "Subject", "Academic Org",
];
const detailRows = rows.map((r) => [
  r.letter, r.course_code, r.course_title, r.offering_school, r.competency_pool_school,
  r.competency, r.score, r.level, fit(r.rationale, 900), fit(r.matched_keywords, 700),
  r.keyword_component, r.prefix_component, r.title_component, r.subject, r.academic_org,
]);
writeTable(detail, detailHeaders, detailRows);
["A:A", "B:B", "C:C", "D:D", "E:E", "F:F", "G:G", "H:H", "I:I", "J:J", "K:M", "N:O"].forEach((addr, i) => {
  const widths = [8, 13, 30, 20, 32, 48, 10, 12, 76, 44, 13, 28];
  detail.getRange(addr).format.columnWidth = widths[i] ?? 18;
});
detail.getRange(`A2:O${detailRows.length + 1}`).format.rowHeight = 42;

const byCourse = new Map();
for (const row of rows) {
  if (!byCourse.has(row.course_code)) byCourse.set(row.course_code, []);
  byCourse.get(row.course_code).push(row);
}
const summaryRows = [];
for (const [code, group] of byCourse.entries()) {
  group.sort((a, b) => b.score - a.score || a.competency.localeCompare(b.competency));
  const top = group[0];
  const high = group.filter((r) => r.score >= 6).map((r) => `${r.competency} (${r.score})`).join("; ");
  const moderate = group.filter((r) => r.score >= 4 && r.score < 6).map((r) => `${r.competency} (${r.score})`).join("; ");
  summaryRows.push([
    top.letter, code, top.course_title, top.offering_school, group.length,
    top.competency, top.score, top.level, high || "None >= 6",
    moderate || "None 4-5.9", fit(top.rationale, 800),
  ]);
}
summaryRows.sort((a, b) => a[1].localeCompare(b[1]));
const summary = workbook.worksheets.add("Course Summary");
writeTable(summary, [
  "Letter", "Course Code", "Course Title", "Offering School", "Pool Items Compared",
  "Top Competency Added", "Top Score / 10", "Top Level",
  "High-Relevance Competencies", "Moderate-Relevance Competencies", "Top Rationale",
], summaryRows);
["A:A", "B:B", "C:C", "D:D", "E:E", "F:F", "G:G", "H:H", "I:I", "J:J", "K:K"].forEach((addr, i) => {
  const widths = [8, 13, 30, 20, 16, 48, 13, 12, 88, 88, 80];
  summary.getRange(addr).format.columnWidth = widths[i];
});
summary.getRange(`A2:K${summaryRows.length + 1}`).format.rowHeight = 42;

const poolsSheet = workbook.worksheets.add("Competency Pools");
const poolRows = pools.map((p) => [
  p.school, p.school_abbr, p.competency, p.definition,
  (p.keywords || []).join(", "), p.source_programmes_text,
]);
writeTable(poolsSheet, ["School", "School Abbr", "Competency Pool Item", "Definition", "Keywords", "Merged From Programmes"], poolRows);
["A:A", "B:B", "C:C", "D:D", "E:E", "F:F"].forEach((addr, i) => {
  const widths = [32, 12, 44, 70, 70, 72];
  poolsSheet.getRange(addr).format.columnWidth = widths[i];
});
poolsSheet.getRange(`A2:F${poolRows.length + 1}`).format.rowHeight = 54;

const excludedSheet = workbook.worksheets.add("Excluded Courses");
const excludedRows = excluded.map((r) => [
  r.letter, r.course_code, r.course_title, r.school, r.exclude_reason,
]);
writeTable(excludedSheet, ["Letter", "Course Code", "Course Title", "School", "Exclude Reason"], excludedRows);
["A:A", "B:B", "C:C", "D:D", "E:E"].forEach((addr, i) => {
  const widths = [8, 14, 34, 22, 76];
  excludedSheet.getRange(addr).format.columnWidth = widths[i];
});

const method = workbook.worksheets.add("Method Notes");
method.showGridLines = false;
method.getRange("A1").values = [["Method Notes"]];
method.getRange("A1").format = {
  fill: "#284B63",
  font: { bold: true, color: "#FFFFFF", size: 13 },
};
method.getRange("A3:B10").values = [
  ["Scope", `SIS courses from letters ${letters.join(", ")}, restricted to rows where course_syllabus is non-empty.`],
  ["Exclusion Rules", "UCore course codes from supplied curriculum PDFs are excluded; CEC*, GEB*, GEC*, GED*, and GEW* are also excluded by user rule."],
  ["Competency Pools", "Courses are scored against competency pool items for their own offering school. Music and Public Policy pools were added so MUS, PUB and URB courses are included."],
  ["Unit of Scoring", "Each row in All Course Scores is one course-to-faculty-competency-pool item."],
  ["Score Scale", "0-10, where 8+ is very high, 6-7.9 high, 4-5.9 moderate, 2-3.9 low, below 2 very low."],
  ["Interpretation", "Scores estimate how strongly a course supports a competency area, not course quality, workload, or career value."],
  ["Letters Included", letters.join(", ")],
  ["Generated From", payloads.map((p) => p.path).join("; ")],
];
method.getRange("A3:A10").format = { fill: "#EEF2F7", font: { bold: true } };
method.getRange("A3:B10").format = {
  font: { name: "Aptos", size: 10 },
  alignment: { horizontal: "left", vertical: "top" },
  wrapText: true,
  borders: { preset: "all", style: "thin", color: "#D9E2EC" },
};
method.getRange("A:A").format.columnWidth = 22;
method.getRange("B:B").format.columnWidth = 100;

const inspect = await workbook.inspect({
  kind: "sheet,table",
  maxChars: 3500,
  tableMaxRows: 5,
  tableMaxCols: 8,
});
console.log(inspect.ndjson);

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 50 },
  maxChars: 2000,
});
console.log(errors.ndjson);

const preview = await workbook.render({
  sheetName: "Course Summary",
  range: "A1:K18",
  scale: 1,
  format: "png",
});
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));

await fs.mkdir(outputDir, { recursive: true });
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputPath);
console.log(`saved ${outputPath}`);
