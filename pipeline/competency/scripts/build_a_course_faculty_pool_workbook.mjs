import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = path.resolve(".");
const targetLetter = (process.argv[2] || "A").trim().toUpperCase();
const inputPath = path.join(root, "outputs", `${targetLetter.toLowerCase()}_course_faculty_pool_relevance.json`);
const outputDir = path.join(root, "outputs");
const outputPath = path.join(outputDir, `${targetLetter.toLowerCase()}_course_competency_relevance.xlsx`);
const previewPath = path.join(outputDir, `${targetLetter.toLowerCase()}_course_competency_relevance_preview.png`);

const payload = JSON.parse(await fs.readFile(inputPath, "utf8"));
const rows = payload.scored_rows;
const usedSchools = new Set(rows.map((r) => r.competency_pool_school));

function fit(text, max = 32000) {
  if (text == null) return "";
  const s = String(text);
  return s.length > max ? s.slice(0, max - 20) + " ...[truncated]" : s;
}

function writeTable(sheet, headers, matrix) {
  const all = [headers, ...matrix];
  const range = sheet.getRangeByIndexes(0, 0, all.length, headers.length);
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
  range.format.autofitRows();
  return range;
}

const workbook = Workbook.create();

const pools = workbook.worksheets.add("Competency Pools");
const poolHeaders = [
  "School", "School Abbr", "Competency Pool Item", "Definition",
  "Keywords", "Merged From Programmes",
];
const poolRows = payload.competency_pools
  .filter((p) => usedSchools.has(p.school))
  .map((p) => [
  p.school, p.school_abbr, p.competency, p.definition,
  (p.keywords || []).join(", "), p.source_programmes_text,
]);
writeTable(pools, poolHeaders, poolRows);
["A:A", "B:B", "C:C", "D:D", "E:E", "F:F"].forEach((addr, i) => {
  const widths = [32, 12, 44, 70, 70, 72];
  pools.getRange(addr).format.columnWidth = widths[i];
});
pools.getRange(`A2:F${poolRows.length + 1}`).format.rowHeight = 54;

const detail = workbook.worksheets.add(`${targetLetter} Course Scores`);
const detailHeaders = [
  "Course Code", "Course Title", "Offering School", "Competency Pool School",
  "Competency Pool Item", "Score / 10", "Level", "Rationale", "Matched Keywords",
  "Keyword Evidence", "Prefix Match", "Title Signal", "Subject", "Academic Org",
];
const detailRows = rows.map((r) => [
  r.course_code, r.course_title, r.offering_school, r.competency_pool_school,
  r.competency, r.score, r.level, fit(r.rationale, 900), fit(r.matched_keywords, 700),
  r.keyword_component, r.prefix_component, r.title_component, r.subject, r.academic_org,
]);
writeTable(detail, detailHeaders, detailRows);
["A:A", "B:B", "C:C", "D:D", "E:E", "F:F", "G:G", "H:H", "I:I", "J:L", "M:N"].forEach((addr, i) => {
  const widths = [13, 30, 18, 32, 48, 10, 12, 76, 44, 13, 13, 13, 28, 18];
  detail.getRange(addr).format.columnWidth = widths[i] ?? 18;
});
detail.getRange("A1:N1").format.rowHeight = 24;
detail.getRange(`A2:N${detailRows.length + 1}`).format.rowHeight = 42;

const byCourse = new Map();
for (const r of rows) {
  if (!byCourse.has(r.course_code)) byCourse.set(r.course_code, []);
  byCourse.get(r.course_code).push(r);
}
const summaryRows = [];
for (const [code, group] of byCourse.entries()) {
  group.sort((a, b) => b.score - a.score || a.competency.localeCompare(b.competency));
  const top = group[0];
  const high = group.filter((r) => r.score >= 6).map((r) => `${r.competency} (${r.score})`).join("; ");
  const moderate = group.filter((r) => r.score >= 4 && r.score < 6).map((r) => `${r.competency} (${r.score})`).join("; ");
  summaryRows.push([
    code, top.course_title, top.offering_school, group.length,
    top.competency, top.score, top.level, high || "None >= 6",
    moderate || "None 4-5.9", fit(top.rationale, 800),
  ]);
}
summaryRows.sort((a, b) => a[0].localeCompare(b[0]));
const summary = workbook.worksheets.add("Course Summary");
writeTable(summary, [
  "Course Code", "Course Title", "Offering School", "Pool Items Compared",
  "Top Competency Added", "Top Score / 10", "Top Level",
  "High-Relevance Competencies", "Moderate-Relevance Competencies", "Top Rationale",
], summaryRows);
["A:A", "B:B", "C:C", "D:D", "E:E", "F:F", "G:G", "H:H", "I:I", "J:J"].forEach((addr, i) => {
  const widths = [13, 30, 20, 16, 48, 13, 12, 88, 88, 80];
  summary.getRange(addr).format.columnWidth = widths[i];
});
summary.getRange("A1:J1").format.rowHeight = 24;
summary.getRange(`A2:J${summaryRows.length + 1}`).format.rowHeight = 42;

const ucore = workbook.worksheets.add("UCore Exclusion");
const ucoreCodes = payload.ucore_codes.all_codes.map((code) => [code]);
writeTable(ucore, ["UCore Course Codes Found In Supplied PDFs"], ucoreCodes);
ucore.getRange("A:A").format.columnWidth = 30;
ucore.getRange("C1:D6").values = [
  [`${targetLetter}-letter courses excluded`, payload.excluded_ucore_courses.length],
  ["Note", "Excluded rows include UCore codes detected from supplied curriculum PDFs plus CEC/GEB/GEC/GED/GEW-prefix courses by user rule."],
  ["PDF count", Object.keys(payload.ucore_codes.by_pdf).length],
  ["Total UCore codes detected", payload.ucore_codes.all_codes.length],
  ["Rule", "Courses in this list are not scored for competency contribution."],
  ["Additional rule", "All CEC*, GEB*, GEC*, GED*, and GEW* courses are excluded even if a specific code is not detected in the PDFs."],
];
ucore.getRange("C1:D6").format = {
  font: { name: "Aptos", size: 10 },
  alignment: { horizontal: "left", vertical: "top" },
  wrapText: true,
  borders: { preset: "all", style: "thin", color: "#D9E2EC" },
};
ucore.getRange("C:C").format.columnWidth = 25;
ucore.getRange("D:D").format.columnWidth = 70;

const method = workbook.worksheets.add("Method Notes");
method.showGridLines = false;
method.getRange("A1").values = [["Method Notes"]];
method.getRange("A1").format = {
  fill: "#284B63",
  font: { bold: true, color: "#FFFFFF", size: 13 },
};
method.getRange("A3:B11").values = [
  ["Scope", `${targetLetter}-letter SIS courses from sis_course_outlines_export.xlsx, restricted to rows where course_syllabus is non-empty.`],
  ["UCore Rule", "Course codes appearing in the supplied Academic Curriculum PDFs are treated as UCore and excluded from competency scoring; all CEC*, GEB*, GEC*, GED*, and GEW* courses are also excluded by user rule."],
  ["Competency Pool", "For each offering school, programme competencies from major_competency_ai_review.xlsx are merged into school-level competency pool items."],
  ["Unit of Scoring", "Each course is scored against competency pool items of its own offering school, not against individual programmes."],
  ["Score Scale", "0-10, where 8+ is very high, 6-7.9 high, 4-5.9 moderate, 2-3.9 low, below 2 very low."],
  ["Score Components", "Course outline keyword/phrase evidence; course prefix alignment; course title signal."],
  ["Interpretation", "Scores estimate how strongly a course supports a competency area, not course quality, workload, or career value."],
  ["UCore Status", `${payload.excluded_ucore_courses.length} ${targetLetter}-letter UCore courses excluded; ${payload.skipped_no_syllabus_courses?.length ?? 0} ${targetLetter}-letter courses skipped because course_syllabus is empty.`],
  ["Generated From", inputPath],
];
method.getRange("A3:A11").format = { fill: "#EEF2F7", font: { bold: true } };
method.getRange("A3:B11").format = {
  font: { name: "Aptos", size: 10 },
  alignment: { horizontal: "left", vertical: "top" },
  wrapText: true,
  borders: { preset: "all", style: "thin", color: "#D9E2EC" },
};
method.getRange("A:A").format.columnWidth = 22;
method.getRange("B:B").format.columnWidth = 95;
method.getRange("A3:B11").format.autofitRows();

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
  range: "A1:J16",
  scale: 1,
  format: "png",
});
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));

await fs.mkdir(outputDir, { recursive: true });
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputPath);
console.log(`saved ${outputPath}`);
