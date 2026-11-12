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
