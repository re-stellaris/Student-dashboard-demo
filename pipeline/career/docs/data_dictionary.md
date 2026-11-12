# Data Dictionary

## 职位关键词-工作技能映射

| 字段 | 含义 |
| --- | --- |
| `category` | 职业大类，例如产品、运营、数据、金融等 |
| `keyword` | 搜索关键词，例如产品经理、数据分析师 |
| `skill` | 从 JD 中抽取的可培训工作技能 |
| `skill_category` | 技能类别，例如工具、编程语言、统计方法、业务方法等 |
| `mention_jds` | 提到该技能的 JD 数量 |
| `sample_jds` | 用于人工核查的样例 JD |
| `coverage` | 该技能在当前关键词 JD 样本中的覆盖比例 |
| `example_job` | 样例岗位名称 |
| `evidence` | JD 中支持该技能抽取的文本证据 |
| `source_url` | JD 来源链接 |

## AI 语义审核课程推荐

| 字段 | 含义 |
| --- | --- |
| `category` | 职业大类 |
| `keyword` | 职业关键词 |
| `skill` | 需要被训练的工作技能 |
| `jd_source_url` | 支撑该技能来自 JD 的证据链接 |
| `relation` | `直接培养` 或 `基础支撑` |
| `ai_reason` | AI 语义审核通过该课程推荐的理由 |
| `course_evidence` | 课程大纲中支持推荐的证据文本 |
| `evidence_field` | 证据来自课程大纲的哪个字段 |
| `confidence` | 语义判断置信度 |
| `course_code` | 课程代码 |
| `course_name_zh` | 官方中文课程名 |
| `course_name_en` | 官方英文课程名 |
| `latest_term` | 最近开课学期 |

## 课程匹配类型

| 类型 | 定义 |
| --- | --- |
| `直接培养` | 课程明确教授或训练该技能、工具、方法或核心任务 |
| `基础支撑` | 课程不直接训练岗位技能，但提供该技能所需的核心基础 |
| `不相关` | 仅关键词表面重合、语义不一致、过于泛化或无法支持训练关系 |
