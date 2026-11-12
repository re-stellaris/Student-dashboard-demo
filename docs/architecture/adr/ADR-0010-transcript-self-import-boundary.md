# ADR-0010：成绩单自填导入的数据边界

- **状态**：Accepted
- **日期**：2026-11-13
- **相关**：ADR-0006（安全基线）、`docs/architecture/api-contract.md` §10、`docs/student-portal.md`

## 背景

旧版 `login+guidance+transcript_v8.html` 有一个成绩单上传区：拖入 Excel/PDF，
前端用 SheetJS 解析、PDF 走 pdf.js + Tesseract OCR，解析结果**只进浏览器内存/本地存储**，
用于填充选课篮与能力面板，不落库。

新架构里成绩在服务端（`edu_course_record`），因此"上传成绩单"必然涉及一个问题：
**允许学生写自己的成绩记录吗？** 这与 ADR-0006 的"学生不能写 `edu_course_record`"直接冲突，
需要显式决策。

## 决策

**允许，但严格限定在「自填来源」这一层：**

1. 新增接口 `POST /edu/transcripts/import`，权限 `edu:transcript:upload`，**只授予 STUDENT**。
2. 身份只从 JWT 派生（`edu_student.sys_user_id`），请求体里没有 `studentId`。
3. 写入的每一行 `source` 恒为 `TRANSCRIPT_UPLOAD`，与 `SIS_IMPORT` / `DEMO` 明确区分。
4. **不覆盖非自填记录**：同一 `(student_id, course_code, term_id)` 已存在时，
   只有原记录 `source='TRANSCRIPT_UPLOAD'` 才允许更新；否则跳过并在响应的
   `skippedDetails[].reason` 里说明（「已存在官方记录，未覆盖」）。
5. 未知课程代码允许插入（`course_id = null`，保留代码/名称快照），但要在
   `unknownCourses` 里回给前端；未知学期直接跳过（没有 `term_id` 无法定位学期）。
6. 导入后重算 `edu_student.total_credits` 与 `cum_gpa`，口径与 `enrollment` 域一致
   （只统计 `passed='1'` 且 `record_status='COMPLETED'`）。
7. **不做 PDF/OCR**：旧版依赖 pdf.js + Tesseract（数百 KB 到数 MB，且识别不稳定）。
   本轮只支持 `.xlsx` / `.xls` / `.csv`，选到 PDF 时给出明确提示而不是静默失败。
8. 单次上限 500 行，超出直接拒绝（防误传整库）。

## 后果

**正向**

- 学生可以把"学校系统里还没有的"课程记录补进自己的规划，而不需要管理员介入。
- 官方数据与自填数据在库层面可区分（`source` 列），任何报表都能按来源过滤。
- 服务端有记录，所以学分进度 / Study Headway / GPA 趋势都能真实反映导入结果，
  比旧版"只进浏览器内存"更可用。

**代价 / 必须遵守的约束**

1. **`source` 是数据可信度的唯一标记**。任何新增的统计、导出、审计功能都必须考虑
   "要不要包含 TRANSCRIPT_UPLOAD"。当前 `enrollment` 与 `advisor` 域的聚合**包含**它，
   因为导入的目的就是让它参与计算。
2. **不能把这个接口复用到"改官方成绩"**。要修正官方记录仍然走
   `PUT /edu/records`（`edu:record:edit`，管理员/教师）。
3. 学生反复导入不同版本的成绩单会产生"自填记录漂移"（旧行还在）。
   当前不做自动清理，只做覆盖更新；需要时提供"清空我的自填记录"接口再处理。
4. PDF 支持被砍掉，属于**功能回退**，必须在 `docs/student-portal.md` 的差异清单里写明。

## 备选方案

| 方案 | 为什么不选 |
|---|---|
| 完全禁止学生写成绩表（维持 ADR-0006 原状） | 上传页就只剩"前端预览"，价值很低；学生无法把缺的记录补进来 |
| 允许学生写任意 `edu_course_record` | 官方成绩可被学生自己篡改，破坏数据可信度 |
| 学生写入但进入"待审核"状态，管理员批准后生效 | 需要审批流、状态机与教师端界面，超出本轮范围；`source` 方案是它的最小可用子集，后续可在此之上加 `status='PENDING'` |
