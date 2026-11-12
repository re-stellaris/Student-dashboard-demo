# Job Hunting Module

本模块用于把“职业关键词 → 招聘 JD → 可培训工作技能 → 校内课程推荐”串成一条可审计的流程。当前版本主要面向中国大陆/中国香港就业市场，JD 样本来自公开招聘网站，课程推荐基于校内课程大纲，并经过 AI 语义审核以减少关键词误判。

## 当前成果

- 覆盖职业关键词：437 个
- 实习僧抓取 JD：12,023 条去重 JD
- 关键词-JD 关系：33,927 条
- 最终课程推荐记录：11,755 行
- 有课程推荐的技能：62 个
- 覆盖职业关键词：403 个
- 课程匹配类型：
  - 直接培养：课程明确训练该技能或其核心方法/工具
  - 基础支撑：课程提供该技能所需的理论、数学、编程、统计或专业基础

## 主要产物

- `releases/职位关键词-工作技能映射.xlsx`
  - 职位关键词、技能、技能类别、JD 证据 URL、样例 JD 等。
- `releases/职位技能-校内课程推荐_AI语义审核版.xlsx`
  - 职位关键词、技能、JD 来源链接、推荐课程、开课学期、匹配类型、AI 判断理由、课程大纲证据。

## 工作流程

1. 收集职业关键词
   - 来源为实习僧职业分类截图整理出的 437 个关键词。
   - 配置文件：`config/keywords.txt`

2. 抓取 JD
   - 对每个关键词抓取前 100 条 JD；不足 100 条则抓取全部可见结果。
   - 脚本：`src/crawl/crawl_shixiseng.py`
   - 注意：仓库不包含完整原始 JD JSON，避免版权、隐私和体积风险。

3. 提取工作技能
   - 从 JD 中抽取“需要学习/训练的专业技能”，排除责任心、沟通能力、抗压能力等泛化性格描述。
   - 脚本：`src/skill_extraction/build_skill_mapping.py`

4. 整理校内课程
   - 仅使用 `with syllabus` 的课程。
   - 英文课程名来自学校课程列表文件，不手动翻译。
   - 脚本：`src/course_matching/extract_course_data.py`

5. 高召回候选课程检索
   - 先用规则和文本检索生成“可能相关”的技能-课程候选对。
   - 脚本：`src/course_matching/match_courses.py`

6. AI 语义审核
   - 对候选对进行语义判断，保留 `direct_training` 和 `foundational_support`，拒绝关键词假阳性。
   - Skill：`skills/evaluate-course-skill-fit/`
   - 脚本：`src/course_matching/semantic_review.py`

7. 导出 Excel
   - 将通过语义审核的 `skill × course` 关系扩展回职业关键词维度。
   - 脚本：
     - `src/course_matching/build_semantic_course_data.py`
     - `src/export/build_semantic_course_recommendations.mjs`

## 重要限制

- 本模块不是职位推荐系统，不对公司或岗位做优劣推荐。
- JD 样本来自公开网页，结果只代表抓取时点的公开招聘文本。
- 课程推荐依据课程大纲文本和 AI 语义判断，不等同于学院官方培养方案建议。
- 公开仓库中不应上传完整原始 JD、学校原始课程大纲文件、微信群记录、个人信息或截图视频。

更多字段说明见 `docs/data_dictionary.md`，方法边界见 `docs/limitations.md`。
