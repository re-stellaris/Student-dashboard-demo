import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = path.resolve(".");
const markdownPath = path.join(root, "outputs", "major_competency_ai_review.md");
const outputDir = path.join(root, "outputs");
const outputPath = path.join(outputDir, "major_competency_ai_review.xlsx");
const previewPath = path.join(outputDir, "major_competency_ai_review_preview.png");

function splitMarkdownRow(line) {
  const trimmed = line.trim();
  const inner = trimmed.startsWith("|") ? trimmed.slice(1, -1) : trimmed;
  return inner.split("|").map((cell) => cell.trim());
}

const md = await fs.readFile(markdownPath, "utf8");
const rows = md
  .split(/\r?\n/)
  .filter((line) => line.trim().startsWith("|") && !/^\|\s*-/.test(line.trim()))
  .map(splitMarkdownRow);

if (rows.length < 2) {
  throw new Error(`No markdown table rows found in ${markdownPath}`);
}

const headers = rows[0];
const schoolMap = new Map([
  ["X & Aerospace Science and Earth Informatics Double Major", ["Cross-school / CUHK-CUHK(SZ) double major", "1st major may be SDS/HSS/SSE; ASEI is the CUHK 2nd major."]],
  ["X & Interdisciplinary Data Analytics Double Major", ["Cross-school / CUHK-CUHK(SZ) double major", "Listed by SDS as a 2+2 double major; 1st major may be SME/SDS/SSE."]],
  ["Clinical Medicine", ["School of Medicine", ""]],
  ["Artificial Intelligence", ["School of Artificial Intelligence", ""]],
  ["Professional Accountancy", ["School of Management and Economics", ""]],
  ["Music Composition and Theory", ["School of Music", ""]],
  ["Chemistry", ["School of Science and Engineering", ""]],
  ["Global Business Studies", ["School of Management and Economics", ""]],
  ["International Organizations and Global Governance", ["School of Humanities and Social Science", ""]],
  ["Urban Management", ["School of Humanities and Social Science", ""]],
  ["Big Data Management and Applications", ["School of Management and Economics", ""]],
  ["Marketing and Communication", ["School of Management and Economics", ""]],
  ["Applied Psychology", ["School of Humanities and Social Science", ""]],
  ["Mathematics and Applied Mathematics", ["School of Science and Engineering", ""]],
  ["Data Science and Big Data Technology", ["School of Data Science", ""]],
  ["New Energy Science and Engineering", ["School of Science and Engineering", ""]],
  ["Materials Science and Engineering", ["School of Science and Engineering", ""]],
  ["X & Materials Science and Engineering Double Major", ["Cross-school / CUHK-CUHK(SZ) double major", "1st major may include Chemistry, New Energy, Physics, or BME; MSE is the CUHK 2nd major."]],
  ["Physics", ["School of Science and Engineering", ""]],
  ["Bioinformatics", ["School of Medicine", ""]],
  ["Biomedical Science and Engineering", ["School of Medicine", ""]],
  ["Biological Sciences", ["School of Medicine", ""]],
  ["Electrical and Computer Engineering", ["School of Science and Engineering", ""]],
  ["Electronic Information Engineering", ["School of Science and Engineering", "Older/early study scheme in the downloaded set."]],
  ["Economics", ["School of Management and Economics", ""]],
  ["Statistics", ["School of Data Science", ""]],
  ["Translation", ["School of Humanities and Social Science", ""]],
  ["English Studies (for Professional Purposes)", ["School of Humanities and Social Science", ""]],
  ["Computer Science and Engineering", ["School of Data Science", ""]],
  ["Finance", ["School of Management and Economics", ""]],
  ["Financial Engineering", ["Joint programme", "Jointly listed by School of Management and Economics, School of Science and Engineering, and School of Data Science."]],
  ["Musicology", ["School of Music", ""]],
  ["Music Performance", ["School of Music", ""]],
]);
const enrichedHeaders = [headers[0], "School / Faculty", "School Notes", ...headers.slice(1)];
const data = rows.slice(1).map((row) => {
  const [school, note] = schoolMap.get(row[0]) ?? ["Unknown", "Needs manual verification."];
  return [row[0], school, note, ...row.slice(1)];
});

const workbook = Workbook.create();
const summary = workbook.worksheets.add("Competency Review");
summary.showGridLines = false;

summary.getRangeByIndexes(0, 0, 1, enrichedHeaders.length).values = [enrichedHeaders];
summary.getRangeByIndexes(1, 0, data.length, enrichedHeaders.length).values = data;

const used = summary.getRangeByIndexes(0, 0, data.length + 1, enrichedHeaders.length);
used.format = {
  font: { name: "Aptos", size: 10, color: "#1F2937" },
  alignment: { horizontal: "left", vertical: "top" },
  wrapText: true,
};

summary.getRangeByIndexes(0, 0, 1, enrichedHeaders.length).format = {
  fill: "#284B63",
  font: { bold: true, color: "#FFFFFF", name: "Aptos", size: 10 },
  alignment: { horizontal: "center", vertical: "middle" },
};
summary.getRangeByIndexes(0, 0, data.length + 1, enrichedHeaders.length).format.borders = {
  preset: "all",
  style: "thin",
  color: "#D9E2EC",
};

summary.freezePanes.freezeRows(1);
summary.getRange("A:A").format.columnWidth = 28;
summary.getRange("B:B").format.columnWidth = 28;
summary.getRange("C:C").format.columnWidth = 38;
summary.getRange("D:D").format.columnWidth = 42;
summary.getRange("E:E").format.columnWidth = 48;
summary.getRange("F:F").format.columnWidth = 46;
summary.getRange("G:G").format.columnWidth = 70;
summary.getRange(`A1:G${data.length + 1}`).format.autofitRows();

const notes = workbook.worksheets.add("Method Notes");
notes.showGridLines = false;
notes.getRange("A1").values = [["Method Notes"]];
notes.getRange("A1").format = {
  fill: "#284B63",
  font: { bold: true, color: "#FFFFFF", size: 13 },
};
notes.getRange("A3:B9").values = [
  ["Source", "Study scheme PDFs from outputs/registry_curricula/Major Programmes.zip"],
  ["Skill Used", "major_competency_identifier.skill"],
  ["Method", "Evidence-based qualitative interpretation from required courses, elective clusters, streams, capstone/project requirements, and course lists."],
  ["School Mapping", "School / Faculty was added from CUHK-Shenzhen school pages and local study scheme programme names. Double majors and joint programmes are labelled explicitly."],
  ["No Scoring", "The workbook intentionally does not assign numeric scores."],
  ["Old Drafts", "major_competency_summary*.csv/md were keyword-script drafts and should not be treated as final analysis."],
  ["Limitation", "The review uses study scheme PDFs. Full course outlines and detailed learning outcomes are not included unless present in the schemes."],
];
notes.getRange("A3:A9").format = {
  fill: "#EEF2F7",
  font: { bold: true },
};
notes.getRange("A3:B9").format = {
  font: { name: "Aptos", size: 10 },
  alignment: { horizontal: "left", vertical: "top" },
  wrapText: true,
  borders: { preset: "all", style: "thin", color: "#D9E2EC" },
};
notes.getRange("A:A").format.columnWidth = 22;
notes.getRange("B:B").format.columnWidth = 90;
notes.getRange("A3:B9").format.autofitRows();

const inspect = await workbook.inspect({
  kind: "sheet,table",
  maxChars: 4000,
  tableMaxRows: 5,
  tableMaxCols: 5,
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
  sheetName: "Competency Review",
  range: "A1:G12",
  scale: 1,
  format: "png",
});
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));

await fs.mkdir(outputDir, { recursive: true });
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputPath);
console.log(`saved ${outputPath}`);
