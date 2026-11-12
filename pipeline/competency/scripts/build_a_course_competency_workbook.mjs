import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = path.resolve(".");
const inputPath = path.join(root, "outputs", "a_course_competency_relevance.json");
const outputDir = path.join(root, "outputs");
const outputPath = path.join(outputDir, "a_course_competency_relevance.xlsx");
const previewPath = path.join(outputDir, "a_course_competency_relevance_preview.png");

const payload = JSON.parse(await fs.readFile(inputPath, "utf8"));
const rows = payload.scored_rows;

function fit(text, max = 32000) {
  if (text == null) return "";
  const s = String(text);
  return s.length > max ? s.slice(0, max - 20) + " ...[truncated]" : s;
}

function writeTable(sheet, headers, matrix, start = "A1") {
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

const detail = workbook.worksheets.add("A Course Scores");
const detailHeaders = [
  "Course Code", "Course Title", "Offering School", "Programme", "Programme School",
  "Score / 10", "Level", "Rationale", "Matched Terms", "Text Similarity",
  "Phrase Match", "Prefix Match", "Subject", "Academic Org",
];
const detailRows = rows.map((r) => [
  r.course_code, r.course_title, r.offering_school, r.programme, r.programme_school,
  r.score, r.relevance_level, fit(r.rationale, 900), fit(r.matched_terms, 600),
  r.text_similarity_component, r.phrase_component, r.prefix_component, r.subject, r.academic_org,
]);
writeTable(detail, detailHeaders, detailRows);
["A:A", "B:B", "C:C", "D:D", "E:E", "F:F", "G:G", "H:H", "I:I", "J:L", "M:N"].forEach((addr, i) => {
  const widths = [13, 28, 18, 34, 28, 10, 12, 70, 36, 12, 12, 12, 26, 18];
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
  group.sort((a, b) => b.score - a.score || a.programme.localeCompare(b.programme));
  const top = group[0];
  const high = group.filter((r) => r.score >= 6).map((r) => `${r.programme} (${r.score})`).join("; ");
  summaryRows.push([
    code, top.course_title, top.offering_school, group.length, top.programme,
    top.score, top.relevance_level, high || "None >= 6", fit(top.rationale, 800),
  ]);
}
summaryRows.sort((a, b) => a[0].localeCompare(b[0]));
const summary = workbook.worksheets.add("Course Summary");
writeTable(summary, [
  "Course Code", "Course Title", "Offering School", "Programmes Compared",
  "Best-Matching Programme", "Best Score / 10", "Best Level",
  "High-Relevance Programmes", "Best-Match Rationale",
], summaryRows);
["A:A", "B:B", "C:C", "D:D", "E:E", "F:F", "G:G", "H:H", "I:I"].forEach((addr, i) => {
  const widths = [13, 30, 20, 16, 36, 13, 12, 70, 80];
  summary.getRange(addr).format.columnWidth = widths[i];
});
summary.getRange("A1:I1").format.rowHeight = 24;
summary.getRange(`A2:I${summaryRows.length + 1}`).format.rowHeight = 42;

const ucore = workbook.worksheets.add("UCore Exclusion");
const ucoreCodes = payload.ucore_codes.all_codes.map((code) => [code]);
writeTable(ucore, ["UCore Course Codes Found In Supplied PDFs"], ucoreCodes);
ucore.getRange("A:A").format.columnWidth = 30;
ucore.getRange("C1:D5").values = [
  ["A-letter courses excluded", payload.excluded_ucore_courses.length],
  ["Note", "No A-letter course code appeared in the two supplied UCore curriculum PDFs."],
  ["PDF count", Object.keys(payload.ucore_codes.by_pdf).length],
  ["Total UCore codes detected", payload.ucore_codes.all_codes.length],
  ["Rule", "Courses in this list are not scored for competency contribution."],
];
ucore.getRange("C1:D5").format = {
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
method.getRange("A3:B10").values = [
  ["Scope", "A-letter SIS courses with saved course outlines from sis_course_outlines_export.xlsx."],
  ["UCore Rule", "Course codes appearing in the two supplied Academic Curriculum PDFs are treated as UCore and excluded from competency scoring."],
  ["Programme Basis", "Competency profiles come from outputs/major_competency_ai_review.xlsx."],
  ["School Rule", "Each course is compared with programmes belonging to the same offering school; joint/cross-school programmes are included when the school appears in notes."],
  ["Score Scale", "0-10, where 8+ is very high, 6-7.9 high, 4-5.9 moderate, 2-3.9 low, below 2 very low."],
  ["Score Components", "Text similarity between course outline and programme competency profile; domain phrase matches; course prefix/programme alignment."],
  ["Interpretation", "Scores estimate curriculum competency support, not course quality, difficulty, workload, or career value."],
  ["Generated From", inputPath],
];
method.getRange("A3:A10").format = { fill: "#EEF2F7", font: { bold: true } };
method.getRange("A3:B10").format = {
  font: { name: "Aptos", size: 10 },
  alignment: { horizontal: "left", vertical: "top" },
  wrapText: true,
  borders: { preset: "all", style: "thin", color: "#D9E2EC" },
};
method.getRange("A:A").format.columnWidth = 22;
method.getRange("B:B").format.columnWidth = 95;
method.getRange("A3:B10").format.autofitRows();

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
  range: "A1:I16",
  scale: 1,
  format: "png",
});
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));

await fs.mkdir(outputDir, { recursive: true });
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputPath);
console.log(`saved ${outputPath}`);
