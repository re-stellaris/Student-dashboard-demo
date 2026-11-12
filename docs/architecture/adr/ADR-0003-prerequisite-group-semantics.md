# ADR-0003：先修关系用 `group_no` 编码（组内 AND、组间 OR）

## 状态（Status）

已接受（Accepted）。2026-11-12。

## 背景（Context）

SIS 课程大纲里的先修条件是一段自然语言／半结构化文本，例如：

```text
(ACT3111 and ACT3112) or ACT3011
CSC1001 or CSC1002, and MAT1011
```

平台必须能用它做三件确定的事：

1. **自动判定**：给定学生已通过的课程集合，判断「先修是否满足」，并列出缺少的课
   （`GET /edu/courses/{courseId}/prereq-check` 与 `POST /edu/plans/validate`）。
2. **可解释**：规划项要给出人能看懂的理由（`edu_study_plan_item.reason`，
   例如「缺少先修课程：数据结构（CSC3100）」）。
3. **可管理**：管理员能在管理端维护先修关系（`PUT /edu/courses/{courseId}/prereqs`），
   并且数据错误（自依赖、环）必须被挡住。

原始数据的形态决定了两个难点：

- **文本不完全规整**。有的课写「or equivalent」，有的写「minimum grade B」，有的写共修
  （co-requisite），有的干脆只有一句「Consent of instructor」。
- **布尔结构嵌套**，但嵌套深度很浅。实际语料里绝大多数是「若干组，组内与、组间或」的二层结构。

旧实现（[`docs/legacy-reference/study_headway_rules.js`](../../legacy-reference/study_headway_rules.js)）
在培养方案侧用的正是「组件 → 组 → 选项数组」的二层编码；本决策把同一语义搬到关系表。

## 决策（Decision）

用一张边表 `edu_course_prereq` 承载先修关系，用 `group_no` 编码布尔结构：

| 语义 | 编码 |
|---|---|
| **组内 AND** | 同一 `group_no` 的所有行都必须满足 |
| **组间 OR** | 任一 `group_no` 被完整满足，整体即满足 |
| 无先修 | 该课程在表中**没有行**（不是「有一行 group_no=0」） |

例：`(ACT3111 & ACT3112) or ACT3011` → `group_no=1 {ACT3111, ACT3112}`、`group_no=2 {ACT3011}`。

配套约束：

1. **表结构**（V1 已实现）
   - `unique key uk_course_prereq (course_id, prereq_course_id)`：一对课程只有一条边，
     保证先修图是**无重边的有向图**。
   - `check (course_id <> prereq_course_id)`：禁止自依赖，数据库层兜底。
   - `key idx_prereq_target (prereq_course_id)`：支撑反向遍历（谁以我为先修）与环检测。
   - `raw_text`：该行对应的 SIS 原文片段，保留可追溯性。
2. **写入侧校验**（`PUT /edu/courses/{courseId}/prereqs`，权限 `edu:prereq:edit`）
   - 覆盖式更新：在同一事务内删除旧边、插入新边。
   - 自依赖：先按参数校验拦一次，再由 `ck_prereq_not_self` 兜底。
   - **环检测**：写入后（或写入前，用「拟插入的边集 + 现有边集」）调用
     `PrerequisiteGraph.detectCycles()`，非空则回滚并抛
     `ServiceException("先修关系存在环：CSC1001 → CSC2001 → CSC1001")`。
3. **计算侧校验**
   - `PrerequisiteGraph.loadGraph()` 一次性载入全部边并按 `course_id` + `group_no` 分组。
   - `detectCycles()` 在规则计算入口再跑一次（数据可能被 SQL 手工改过、
     或由种子生成器写入）。检测到环时**抛异常而不是跳过**：静默跳过会让「先修校验通过」
     变成假阴性，比报错危险得多。
   - `isSatisfied(courseId, passedCourseIds)`：按组判定，任一组全通过即返回 `true`。
   - `missingPrerequisites(courseId, passedCourseIds)`：返回「最少阻力」的那一组缺少的课程
     （即缺课数量最少的组），用于「缺少先修课程」提示。
4. **原文保留**：`edu_course.prereq_text` 保存完整 SIS 原文，`edu_course_prereq.raw_text`
   保存每行的片段。结构化表达不了的部分（「or equivalent」「B 级以上」）留在原文里，
   由管理员人工判断后决定是否退化处理。

## 后果（Consequences）

**正面**

- 先修判定退化成「按 `group_no` 分组后做集合比较」，实现短、可测、无递归 ——
  `PrerequisiteGraph` 可以在不启动 Spring 容器的情况下单测（`backend-conventions.md` §7）。
- 环检测与自依赖检测都能用一次 DFS 完成，且两个方向（写、算）复用同一段代码。
- 管理端 UI 可以按组渲染：一组一个折叠块，组内加课程，组间加组。不需要树形编辑器。
- 与旧引擎语义一致：`study_headway_rules.js` 的 `groups[].options[][]` 与本表同构，
  迁移期可以直接把旧 JSON 编译成插入语句。

**需要承担的代价**

- **表达力上限是 DNF（析取范式）**。以下形态无法直接表达：
  - 嵌套：「(A or B) and (C or D)」在语义上可以展开成
    `{A,C} / {A,D} / {B,C} / {B,D}` 四组，但**行数是指数级**；
  - 否定：「not CSC1001」不能表达；
  - 阈值：「至少 2 门 3000 级以上课程」「GPA ≥ 3.0」不能表达；
  - 等级要求：「CSC1001 需 B 级以上」只能记在 `raw_text` 里；
  - 共修（co-requisite）：单独放在 `edu_course.coreq_text`，不参与 `isSatisfied`。
- **`group_no` 的语义没有 schema 强制**。SQL 只能保证「同一对课程只有一条边」，
  不能保证「组内是 AND」。任何读取 `edu_course_prereq` 的代码都必须先按
  `(course_id, group_no)` 分组，然后交给 `PrerequisiteGraph` 判定。
  绕过 `PrerequisiteGraph` 自己拼 SQL 判定的做法一律视为缺陷。
- **DNF 展开会造成行数膨胀**。极端情况下 `(A or B) and (C or D) and (E or F)` 需要 8 行/组。
  当前语料的实际形态（多数是「必修清单 + 一个替代项」）不会触发，但导入时必须记录
  「展开行数 / 原文条件数」的比值，超过阈值（例如 4 倍）时告警并转人工。
- **环的语义是「数据错误」而不是「非法选课」**。检测到环必须报错，因为任何一门课的先修
  都无法满足，学生端会出现无法解释的空结果。

## 备选方案（Alternatives considered）

| 方案 | 为什么没选 |
|---|---|
| **布尔表达式树**（`prereq_node(parent_id, operator AND/OR, course_id)`） | 表达力最强，但代价全面更高：查询需要递归 CTE；环检测要在树上做而不是图上；管理端必须提供树形编辑器（团队没有这个 UI 预算）；对当前语料是过度设计 —— 实际数据里几乎没有三层嵌套 |
| **单列存 JSON 表达式**（`edu_course.prereq_expr json`） | 无法用外键保证「先修课程真实存在」；无法用 `uk` 去重；无法用 `check` 禁止自依赖；图算法要全部在应用层解析 JSON。而且它把「数据完整性」降级为「应用层自觉」 |
| **只存 `prereq_text` 原文，运行时人工/LLM 判断** | 结果不可复现、不可测试、不可解释；同一份数据两次运行可能给出不同答案，与方案 ch.5.1「透明规则」的要求直接冲突 |
| **把先修改写成 Java 常量/配置文件** | 违反方案 ch.5.1「依赖关系及学分规则应可由管理员配置，避免写死在 Java 代码中」；课程目录有 3,358 门，硬编码不可维护 |
| **把「至少 N 门」这类阈值也塞进 `edu_course_prereq`**（例如加 `min_count` 列） | 会让一张表同时表达「边」和「聚合约束」两种语义，读取方必须判断该行是边还是约束。当前把这类规则交给培养方案侧的 `edu_curriculum_item.min_units` 与后续的规则扩展，先修表保持纯粹 |
