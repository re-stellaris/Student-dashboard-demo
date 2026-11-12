# 教育平台接口契约（MVP）

> 状态：**开发前确认稿**。路径、参数、返回结构一经确认即为前后端联调依据。
> 变更必须同步更新本文 + `docs/architecture/backend-conventions.md` 并通知前后端负责人。

## 0. 通用约定

| 项目 | 约定 |
|---|---|
| 后端内部路径 | `@RequestMapping("/edu/...")`（若依 `server.servlet.context-path=/`） |
| 对外生产路径 | `/api/edu/...`（Nginx 将 `/api/` 与 `/prod-api/` 反代到后端根路径，见 `deploy/nginx/`） |
| 前端调用路径 | `import { ... } from '@/api/education/course'`，请求用 `request({ url: '/edu/courses/list', ... })` |
| 认证 | 若依 JWT，请求头 `Authorization: Bearer <token>` |
| 统一响应 | `AjaxResult`：`{"code":200,"msg":"操作成功","data":...}` |
| 分页响应 | `TableDataInfo`：`{"code":200,"msg":"查询成功","total":3358,"rows":[...]}` |
| 分页参数 | `pageNum`（默认 1）、`pageSize`（默认 10）、`orderByColumn`、`isAsc` |
| 错误码 | 沿用若依：`200` 成功、`401` 未认证、`403` 无权限、`500` 业务异常（`msg` 为可读中文） |
| 时间格式 | `yyyy-MM-dd HH:mm:ss`（`spring.jackson.date-format`） |

**访问控制原则**：菜单权限只控制入口；每个接口在服务端做资源级归属校验。
凡带 `me`/`mine` 的接口，身份只取自当前登录用户，**不接受前端传入的 `studentId` / `teacherId`**。

## 1. 课程中心（course）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/edu/courses/list` | 分页查询课程。参数：`code`、`title`、`school`、`category`、`level`、`hasOutline` | 登录 |
| GET | `/edu/courses/{courseId}` | 课程详情 | 登录 |
| GET | `/edu/courses/byCode/{code}` | 按课程代码查询（转录/规划补全用） | 登录 |
| POST | `/edu/courses` | 新增课程 | `edu:course:add` |
| PUT | `/edu/courses` | 修改课程 | `edu:course:edit` |
| DELETE | `/edu/courses/{courseIds}` | 删除课程（逗号分隔 ID） | `edu:course:remove` |
| GET | `/edu/courses/options` | 筛选选项：`schools`、`categories`、`levels` | 登录 |
| GET | `/edu/courses/{courseId}/prereqs` | 先修关系（含 `groupNo`、`rawText`、先修课程代码/名称） | 登录 |
| PUT | `/edu/courses/{courseId}/prereqs` | 覆盖式更新先修关系，body：`[{"prereqCourseId":100123,"groupNo":1}]`；写入前做自依赖与环检测 | `edu:prereq:edit` |
| GET | `/edu/courses/{courseId}/prereq-check` | 以当前学生已修课程判断先修是否满足：`{"satisfied":true,"missing":[{"courseId":..,"code":"CSC1001","title":".."}],"reason":".."}` | 登录 |

## 2. 培养方案（curriculum）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/edu/majors/list` | 专业列表 | 登录 |
| GET | `/edu/curricula/list` | 培养方案列表，参数 `majorId`、`versionYear` | `edu:curriculum:list` |
| GET | `/edu/curricula/{curriculumId}` | 方案详情（含 `items`） | `edu:curriculum:list` |
| GET | `/edu/curricula/by-major` | 按专业 + 入学年份解析适用版本，参数 `majorId`、`entryYear`；返回 `null` 表示无匹配版本 | 登录 |
| POST/PUT | `/edu/curricula` | 新增/修改方案 | `edu:curriculum:add` / `edu:curriculum:edit` |
| DELETE | `/edu/curricula/{curriculumIds}` | 删除方案 | `edu:curriculum:remove` |
| PUT | `/edu/curricula/{curriculumId}/items` | 覆盖式更新方案条目，body：`[{"courseId":..,"courseCode":"..","requirementType":"REQUIRED","section":"..","stream":"","minUnits":3.0,"sortOrder":1}]` | `edu:curriculum:item:edit` |

## 3. 学期 / 开课 / 选课 / 成绩（enrollment）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/edu/terms/list` | 学期列表 | 登录 |
| GET | `/edu/terms/current` | 当前学期 | 登录 |
| GET | `/edu/offerings/list` | 开课列表，参数 `termId`、`courseId`、`teacherId` | 登录 |
| GET | `/edu/offerings/mine` | 当前教师所授课程（含学生数、作业数） | `edu:offering:list` |
| POST/PUT | `/edu/offerings` | 维护开课 | `edu:offering:edit` |
| GET | `/edu/students/me` | 当前学生学籍信息（学号、专业、入学年、书院） | 登录 |
| GET | `/edu/students/me/progress` | 学分进度：`{"curriculumTotal":..,"majorRequired":..,"earned":..,"remaining":..,"categoryBreakdown":[{"category":"Major","earned":..,"required":..}]}` | 登录 |
| GET | `/edu/students/me/records` | 本人成绩单，参数 `termCode`、`category` | `edu:record:list` |
| POST | `/edu/enrollments` | 学生选课，body：`{"offeringId":1001}`；校验容量、重复选课、先修 | 登录 |
| DELETE | `/edu/enrollments/{enrollmentId}` | 退课 | 登录 |
| PUT | `/edu/records` | 管理员/教师修正成绩 | `edu:record:edit` |

## 4. 选课规划（planning）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/edu/plans/list` | 规划列表（学生仅本人） | `edu:plan:list` |
| GET | `/edu/plans/{planId}` | 规划详情：`{"plan":{...},"items":[{"courseId":..,"code":..,"title":..,"units":..,"reason":"..","validationStatus":"VALID","validationMessage":".."}]}` | `edu:plan:list` |
| POST | `/edu/plans` | 新建草稿，body：`{"termId":1,"title":"2026-27 秋季计划"}` | `edu:plan:add` |
| PUT | `/edu/plans/{planId}/items` | 覆盖式保存规划课程，body：`[{"courseId":..}]`（保存即重新校验并写入 `reason`） | `edu:plan:add` |
| POST | `/edu/plans/validate` | **不落库**的试算：body `{"termId":1,"courseIds":[..]}` → 见 4.1 | `edu:plan:validate` |
| POST | `/edu/plans/{planId}/validate` | 校验并持久化校验结果 | `edu:plan:validate` |
| POST | `/edu/plans/recommend` | 规则推荐：body `{"termId":1,"limit":10}` → `[{"courseId":..,"code":..,"title":..,"units":..,"reason":"..","missingPrereqs":[..]}]` | `edu:plan:list` |
| POST | `/edu/plans/{planId}/submit` | 提交规划（`DRAFT`→`SUBMITTED`，提交前强制校验） | `edu:plan:submit` |
| DELETE | `/edu/plans/{planIds}` | 删除规划 | `edu:plan:add` |

### 4.1 校验结果结构 `PlanValidationResult`

```json
{
  "termId": 1,
  "unitLimit": 21.0,
  "totalUnits": 18.0,
  "state": "WARN",
  "messages": ["超出学分上限 21.0：当前 24.0"],
  "items": [
    {
      "courseId": 100234,
      "code": "CSC3100",
      "title": "数据结构",
      "units": 3.0,
      "reason": "专业必修，且已通过先修课程 CSC1001",
      "validationStatus": "VALID",
      "validationMessage": ""
    },
    {
      "courseId": 100456,
      "code": "CSC4001",
      "title": "算法设计",
      "units": 3.0,
      "reason": "缺少先修课程：数据结构（CSC3100）",
      "validationStatus": "INVALID",
      "validationMessage": "缺少先修课程 CSC3100"
    }
  ]
}
```

规则顺序（方案 5.1）：① 读取已通过课程 → ② 先修依赖图判定 → ③ 培养方案未修筛选
→ ④ 学分上限 / 开课学期 / 时间冲突 → ⑤ 为每条生成可解释说明。

## 5. 作业与提交（assignment）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/edu/assignments/list` | 作业列表，参数 `offeringId` | 登录 |
| POST | `/edu/assignments` | 教师发布作业 | `edu:assignment:add` |
| PUT | `/edu/assignments` | 修改作业 | `edu:assignment:edit` |
| DELETE | `/edu/assignments/{assignmentIds}` | 删除作业 | `edu:assignment:edit` |
| GET | `/edu/submissions/list` | 提交列表（教师看本课程，学生看本人） | 登录 |
| POST | `/edu/submissions` | 学生提交，body：`{"assignmentId":..,"content":"..","attachmentUrl":""}` | 登录 |
| PUT | `/edu/submissions/grade` | 教师评分，body：`{"submissionId":..,"grade":92.5,"feedback":".."}` | `edu:submission:grade` |

## 6. 学情与技能画像（analytics）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/edu/analytics/dashboard` | 仪表盘聚合：`{"credits":{...},"gpa":3.11,"gpaTrend":[{"termCode":"2022-23-1","gpa":3.2}],"categoryBreakdown":[...],"planSummary":{"draft":1,"submitted":0},"pendingAssignments":2}` | `edu:dashboard:view` |
| GET | `/edu/analytics/skill-profile` | 依据本人已修课程聚合技能画像：`[{"skillTagId":..,"name":"..","category":"..","score":8.4,"courseCount":3,"rationale":".."}]` | `edu:skill:view` |
| GET | `/edu/analytics/competency-pool` | 学院能力池，参数 `schoolAbbr`、`keyword` | `edu:analytics:view` |
| GET | `/edu/analytics/course-competency?courseId=` | 单门课程的技能关联明细 | 登录 |

> 个人技能画像的“自评标签、笔记、备份导出”只保存在浏览器 IndexedDB（Dexie.js），
> 服务端仅提供公共技能标签与课程关联度，**不存储明文个人画像**。

## 7. 前端页面与接口对照

| 菜单 | 组件 | 依赖接口 |
|---|---|---|
| 学习仪表盘 | `views/education/dashboard/index.vue` | `/edu/analytics/dashboard`、`/edu/students/me` |
| 课程中心 | `views/education/course/index.vue` | `/edu/courses/list`、`/edu/courses/options`、`/edu/courses/{id}/prereqs` |
| 培养方案 | `views/education/curriculum/index.vue` | `/edu/majors/list`、`/edu/curricula/list`、`/edu/curricula/{id}` |
| 选课规划 | `views/education/planner/index.vue` | `/edu/plans/*`、`/edu/courses/list` |
| 我的成绩 | `views/education/records/index.vue` | `/edu/students/me/records`、`/edu/students/me/progress` |
| 教师授课 | `views/education/teaching/index.vue` | `/edu/offerings/mine`、`/edu/assignments/*`、`/edu/submissions/*` |
| 技能画像 | `views/education/skill/index.vue` | `/edu/analytics/skill-profile`、本地 Dexie |
| 学情分析 | `views/education/analytics/index.vue` | `/edu/analytics/competency-pool`、`/edu/analytics/course-competency` |

## 8. 演示账号

| 账号 | 密码 | 角色 | 可见范围 |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | 全部 |
| `stu_chen` | `admin123` | STUDENT | 本人成绩/规划 |
| `teacher_li` | `admin123` | TEACHER | 本人授课课程 |

> 初始密码与若依默认一致，**上线前必须修改**（见 `docs/architecture/adr/ADR-0006-security-baseline.md`）。

## 9. 导师看板（advisor）

旧版导师看板（`legacy/aa_dashboard_v2.html`）迁移后的只读聚合接口，页面为
`views/education/advisor/index.vue`（`/education/advisor`）。三个接口权限统一为
`edu:advisor:view`（菜单与 `V7__edu_advisor_permission.sql` 已就绪）。

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/edu/advisors/overview` | 总览：学生数、平均 GPA/学分、GPA 分布与趋势、风险与进度分布 | `edu:advisor:view` |
| GET | `/edu/advisors/students` | 学生列表（分页）。参数：`entryYear`、`majorId`、`riskLevel`、`keyword`、`pageNum`、`pageSize` | `edu:advisor:view` |
| GET | `/edu/advisors/students/{studentId}` | 单个学生画像：基本信息 + 分类学分 + 学期 GPA 趋势 + 已修课程数 + 风险明细 | `edu:advisor:view` |

### 9.1 风险规则（透明可解释）

风险分由后端规则函数计算，规则与权重集中在
`com.ruoyi.education.advisor.rule.AdvisorRiskRules`（常量可调，不写死在业务代码里）：

| 规则编码 | 命中条件 | 默认权重 |
|---|---|---|
| `GPA_LOW` | `cum_gpa < 2.0`（高危）或 `< 2.5`（警戒） | 1.0 |
| `CREDIT_BEHIND` | 已修学分 `<` 按入学年推算的应修学分 `× 0.8` | 1.0 |
| `FAILED_COURSE` | 存在 `passed='0'` 或等级 `F` 的成绩记录 | 1.0 |
| `NO_RECENT_TERM` | 最近两个“有成绩数据”的学期均无该生成绩记录 | 1.0 |

应修学分口径：`30 学分/学年 × 已完成学年数`，封顶毕业要求 120 学分
（`edu_curriculum.total_credits` 当前为 0，故用常量兜底）。

总分 = `Σ命中规则权重 / Σ全部规则权重 × 100`，四舍五入取整后分档：
`>= 60 high`、`>= 30 medium`、否则 `low`。每条命中原因都返回可读 `detail`，
前端还会拿到 `weight`，按导师在页面上输入的四个权重做展示层重新加权（不再次请求后端）。

**已知限制**：当前库没有 advisor-student（导师-学生）关系表，也没有联系记录表。
因此本轮 `TEACHER` / `ADMIN` 可以看到**全部**学生：接口不接受前端传入的
“我是谁”（`teacherId` / `advisorId`），身份只用于 `@PreAuthorize` 的权限校验。
补齐关系表后，把过滤条件下推到 `EduAdvisorMapper` 即可，本契约不变。

### 9.2 `GET /edu/advisors/overview` 返回示例

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "studentCount": 8,
    "avgGpa": 3.29,
    "avgCredits": 102.1,
    "gpaBuckets": [
      { "label": "<2.0", "count": 0 },
      { "label": "2.0-2.5", "count": 0 },
      { "label": "2.5-3.0", "count": 1 },
      { "label": "3.0-3.5", "count": 5 },
      { "label": "3.5-4.0", "count": 2 }
    ],
    "gpaTrend": [
      { "termCode": "2022-23-1", "avgGpa": 3.31 },
      { "termCode": "2025-26-2", "avgGpa": 3.36 }
    ],
    "risk": { "high": 0, "medium": 0, "low": 8 },
    "progress": { "onTrack": 8, "behind": 0, "unknown": 0 },
    "categoryBreakdown": [
      { "category": "Major", "avgCredits": 72.0, "courseCount": 195 },
      { "category": "GenEd", "avgCredits": 15.0, "courseCount": 40 }
    ]
  }
}
```

> `categoryBreakdown` 是扩展字段：培养方案暂无“分类要求学分”，因此给出
> “全体学生该分类平均已修学分 + 修读记录数”，供 Progress Overview 的分类进度条展示。

### 9.3 `GET /edu/advisors/students` 返回示例

```json
{
  "code": 200,
  "msg": "查询成功",
  "total": 8,
  "rows": [
    {
      "studentId": 1000,
      "studentNo": "122040156",
      "name": "CHEN MICHAEL",
      "majorName": "计算机科学与技术",
      "entryYear": 2022,
      "cumGpa": 3.11,
      "totalCredits": 126.0,
      "creditProgressPct": 100.0,
      "avgScore": 41.6,
      "riskLevel": "low",
      "riskScore": 0,
      "riskReasons": []
    }
  ]
}
```

`riskLevel` 是派生指标：服务层先算出全部学生的风险等级，再按 `riskLevel` 过滤并分页，
因此 `total` 与过滤后的结果一致（该参数不能直接下推 SQL）。

### 9.4 `GET /edu/advisors/students/{studentId}` 返回示例

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "studentId": 1000,
    "studentNo": "122040156",
    "name": "CHEN MICHAEL",
    "majorId": 1002,
    "majorName": "计算机科学与技术",
    "majorCode": "CSE",
    "entryYear": 2022,
    "college": "Harmonia College",
    "cumGpa": 3.11,
    "totalCredits": 126.0,
    "graduationCredits": 120.0,
    "expectedCredits": 120.0,
    "creditProgressPct": 100.0,
    "avgScore": 41.6,
    "completedCourseCount": 44,
    "failedCourseCount": 0,
    "latestTermGpa": 3.17,
    "riskLevel": "low",
    "riskScore": 0,
    "riskReasons": [],
    "categoryCredits": [
      { "category": "Major", "credits": 91.0, "courseCount": 31 },
      { "category": "GenEd", "credits": 18.0, "courseCount": 6 }
    ],
    "gpaTrend": [
      { "termCode": "2022-23-1", "avgGpa": 3.17 },
      { "termCode": "2025-26-2", "avgGpa": 3.17 }
    ]
  }
}
```

### 9.5 前端页面与接口对照

| 菜单 | 组件 | 依赖接口 |
|---|---|---|
| 导师看板 | `views/education/advisor/index.vue` | `/edu/advisors/overview`、`/edu/advisors/students`、`/edu/advisors/students/{id}`、`/edu/majors/list` |

> 联系人/沟通相关区块（Communication Overview、Last Contact）当前没有数据源：
> 数据库尚无 advisor-student 关系表与联系记录表，页面按空状态展示并给出说明。
