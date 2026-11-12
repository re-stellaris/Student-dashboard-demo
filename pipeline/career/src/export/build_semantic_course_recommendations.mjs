import fs from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { Workbook, SpreadsheetFile } from "@oai/artifact-tool";

const root = fileURLToPath(new URL("../../", import.meta.url)).replace(/\\/g, "/").replace(/\/$/, "");
const data = JSON.parse(await fs.readFile(`${root}/data/processed/semantic_course_recommendations.json`, "utf8"));
const wb = Workbook.create();
const s = wb.worksheets.add("AI语义审核推荐");
const n = wb.worksheets.add("审核说明");
s.showGridLines = false; n.showGridLines = false;

const navy="#17365D", blue="#2F75B5", white="#FFFFFF";
s.getRange("A1:M1").merge();
s.getRange("A1").values=[["职位技能—校内课程推荐（AI语义审核版）"]];
s.getRange("A1:M1").format={fill:navy,font:{bold:true,color:white,size:16}};
s.getRange("A1:M1").format.rowHeight=30;

const heads=["一级类别","职位关键词","工作技能","JD证据链接","培养关系","AI判断理由","课程证据原文","证据字段","置信度","课程代码","课程中文名","课程英文名","最近开课学期"];
s.getRange("A3:M3").values=[heads];
s.getRange("A3:M3").format={fill:blue,font:{bold:true,color:white},wrapText:true};
const rows=data.rows.map(r=>[r.category,r.keyword,r.skill,r.jd_source_url,r.relation,r.ai_reason,r.course_evidence,r.evidence_field,r.confidence,r.course_code,r.course_name_zh,r.course_name_en,r.latest_term]);
if(rows.length) s.getRangeByIndexes(3,0,rows.length,13).values=rows;
s.tables.add(`A3:M${rows.length+3}`,true,"SemanticCourseRecommendations").style="TableStyleMedium2";
s.freezePanes.freezeRows(3); s.freezePanes.freezeColumns(3);
if(rows.length){
  s.getRange(`A4:M${rows.length+3}`).format.rowHeight=54;
  s.getRange(`D4:G${rows.length+3}`).format.wrapText=true;
  s.getRange(`I4:I${rows.length+3}`).format.numberFormat="0%";
  s.getRange(`E4:E${rows.length+3}`).conditionalFormats.add("containsText",{text:"直接培养",format:{fill:"#C6E0B4",font:{color:"#1E4620",bold:true}}});
  s.getRange(`E4:E${rows.length+3}`).conditionalFormats.add("containsText",{text:"基础支撑",format:{fill:"#FFF2CC",font:{color:"#7F6000",bold:true}}});
}
for(const [c,w] of [["A",16],["B",20],["C",20],["D",42],["E",13],["F",58],["G",60],["H",20],["I",11],["J",14],["K",28],["L",38],["M",20]]) s.getRange(`${c}1:${c}${rows.length+3}`).format.columnWidth=w;

n.getRange("A1:B1").merge(); n.getRange("A1").values=[["AI语义审核说明"]];
n.getRange("A1:B1").format={fill:navy,font:{bold:true,color:white,size:16}};
const info=[
 ["指标","内容"],
 ["唯一候选技能—课程对",data.stats.candidate_pairs],
 ["通过审核",data.stats.accepted_pairs],
 ["直接培养",data.stats.direct_pairs],
 ["基础支撑",data.stats.foundation_pairs],
 ["不相关并剔除",data.stats.rejected_pairs],
 ["低置信度通过项",data.stats.low_confidence_pairs],
 ["最终推荐记录",data.stats.output_rows],
 ["有课程推荐的技能",data.stats.skills_with_recommendations],
 ["覆盖职位关键词",data.stats.keywords_covered],
 ["判断原则","不以关键词命中作为推荐依据；必须由学习成果、教学大纲、作业/实验或课程简介证明学生实际学习或实践该技能。"],
 ["直接培养","课程明确教授或要求实践该技能。"],
 ["基础支撑","课程教授进一步掌握该技能所需的可识别理论或方法，但未直接训练完整技能。"],
 ["已排除示例","survey course≠用户研究；computer vision≠Visio；SEM材料表征≠搜索营销；SEO证券增发≠搜索优化。"],
 ["课程范围","仅使用 With Outline 且 course_syllabus 非空的校内课程；英文名来自 Course_List_byInitial.xlsx 的官方字段。"],
 ["证据追溯","每行保留一条JD链接，以及课程材料中的原文证据。"],
];
n.getRange(`A3:B${info.length+2}`).values=info;
n.getRange("A3:B3").format={fill:blue,font:{bold:true,color:white}};
n.getRange(`A4:A${info.length+2}`).format={fill:"#D9EAF7",font:{bold:true,color:navy}};
n.getRange(`B4:B${info.length+2}`).format.wrapText=true;
n.getRange(`A1:A${info.length+2}`).format.columnWidth=28; n.getRange(`B1:B${info.length+2}`).format.columnWidth=100;
n.getRange(`A3:B${info.length+2}`).format.rowHeight=32;
n.freezePanes.freezeRows(3);

console.log((await wb.inspect({kind:"table",range:"AI语义审核推荐!A1:M12",include:"values,formulas",tableMaxRows:12,tableMaxCols:13,maxChars:8000})).ndjson);
console.log((await wb.inspect({kind:"match",searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",options:{useRegex:true,maxResults:100},summary:"formula errors"})).ndjson);

const outDir=`${root}/releases`; await fs.mkdir(outDir,{recursive:true});
const out=`${outDir}/职位技能-校内课程推荐_AI语义审核版.xlsx`;
const x=await SpreadsheetFile.exportXlsx(wb); await x.save(out);
console.log(JSON.stringify({out,rows:rows.length,stats:data.stats}));
