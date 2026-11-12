# 迁移对照表（旧静态原型 → RuoYi-Vue 重构）

> 本文记录 `feat/ruoyi-vue-refactor` 分支上发生的一切搬迁，供三个方面使用：
> 想知道「旧东西去哪了」的人、想核对「新东西有没有对应旧功能」的人、
> 以及判断「这次重构到底做完了没有」的人。

## 0. 分支策略

| 分支 | 角色 | 说明 |
|---|---|---|
| `legacy/static-prototype-v1` | **旧版本的唯一存证** | 静态多页原型：`students-interface/`、`aa_dashboard_v2.html`、`api_server.py`（FastAPI）、`course_db_api_tables.sql`、React/Vite 登录页、Cloudflare/Netlify 部署配置。**只读**，不再接受功能改动；作为回滚与历史查证入口 |
| `feat/ruoyi-vue-refactor` | **本次重构** | `backend/`（RuoYi-Vue v3.9.2）+ `frontend/`（RuoYi-Vue3 TS）+ `ruoyi-education` + `data/` + `pipeline/` + `deploy/` + `docs/` |
| `main` | 合并目标 | 重构验收通过后由 `feat/ruoyi-vue-refactor` 合入 |

搬迁方式：**能用 `git mv` 的一律用 `git mv`**（保留 `git blame` 与文件历史），
删除的旧文件不做「注释掉」处理，历史靠 `legacy/static-prototype-v1` 分支保留。
因此「这个文件以前长什么样」的正确答案是 `git show legacy/static-prototype-v1:<旧路径>`，
而不是在当前工作区里找残留。

## 1. 旧 → 新 文件映射

| 旧路径（legacy 分支保留） | 新路径 | 处理方式 |
|---|---|---|
| `frontend/`（React/Vite 登录页） | 删除 | 由官方 RuoYi-Vue3 TS 前端取代；`frontend/` 目录名被官方前端复用，旧的 React 工程不再保留 |
| `students-interface/*.html`、`data.js`、`competency_data.js`、`intern_prep_data.js` | 删除 | 生成物与静态页；业务逻辑迁入 Java/Vue，不再由 `.js` 数据文件承载 |
| `students-interface/study_headway_engine.js` | `docs/legacy-reference/study_headway_engine.js` | 作为规则语义参考保留；培养方案进度的判定语义以它为基准 |
| `students-interface/study_headway_rules.js` | `docs/legacy-reference/study_headway_rules.js` | 规则数据现由 `edu_*` 表承载（`edu_curriculum` / `edu_curriculum_item`） |
| `api_server.py` | 删除 | FastAPI 由 `ruoyi-education` 的 Controller 取代（路径前缀 `/edu/...`，见 `api-contract.md`） |
| `course_db_api_tables.sql` | 删除 | 由 `backend/sql/edu/V1..V6` 取代（理由见 ADR-0005） |
| `aa_dashboard_v2.html`、`login+guidance+transcript_v8.html`、`shared.js`、`assets/` | 删除 | 旧静态界面；页面结构由 `frontend/src/views/education/` 重建 |
| `_redirects`、`netlify.toml`、`wrangler.jsonc` | `deploy/`（nginx + docker compose） | 部署方式改为自托管；Cloudflare Pages / Netlify 的路由规则不再适用 |
| `package.json`、`package-lock.json`、`tests/` | 删除 | 根级 Node 工程不再需要；前端依赖由 `frontend/package.json` 管理 |
| `tools/import_sis_courses.py` | `pipeline/course_import/import_sis_courses.py`（被 `build_edu_seed.py` 取代） | 导入目标由旧 `courses` 表改为 `edu_course`；新链路改为「生成 SQL 种子」而不是「直连写库」 |
| `tools/build_competency_data.py` | `pipeline/competency/build_competency_data.py` | `git mv`；产物由 `students-interface/competency_data.js` 改为 `V6__edu_seed_competency.sql` |
| `tools/build_intern_prep_data.py` | `pipeline/career/build_intern_prep_data.py` | `git mv`；产物由 `intern_prep_data.js` 改为 `data/career/releases/` 下的工作簿（不进运行时） |
| `tools/{compile_headway_rules,extract_graduation_rules,extract_headway_sources,download_registry_study_schemes,study_scheme_parser}.py` | `pipeline/graduation_rules/` | `git mv`；产物由 `study_headway_rules.js` 改为 `V4__edu_seed_curriculum.sql` |
| `tools/sis-crawler/` | `pipeline/sis-crawler/` | `git mv`；爬虫仍由授权用户手动执行，不属于运行时 |
| `tests/study_headway.test.cjs` | `pipeline/graduation_rules/tests/` | `git mv`；随规则编译脚本一起走，确保规则生成的回归测试与生成器同目录 |
| `competency-analysis/`（scripts） | `pipeline/competency/scripts/` | `git mv`；分析脚本与数据产物分离，脚本进 `pipeline/`，产物进 `data/` |
| `competency-analysis/`（数据产物） | `data/competency/` | `git mv`；含 `evidence/`、`source/` 与两份 xlsx |
| `job-hunting-module/`（源码/技能） | `pipeline/career/` | 离线管道，**不是运行时模块**（ADR-0007） |
| `job-hunting-module/releases/`、`config/keywords.txt` | `data/career/` | `git mv`；产物与抓取关键词留下，源码搬到 `pipeline/career/` |
| `course_list_database/` | `data/course_catalog/` | `git mv`；`sis_course_outlines_export.xlsx`、`All_Course_Lists.xlsx`、`Course_List_byInitial.xlsx` 成为 `V3` 的输入 |
| `dataset/` | `data/demo/` | `git mv`；演示学生与成绩工作簿，`V5` 的输入 |
| `graduation_rules/` | `data/graduation_rules/` | `git mv`；`SDS_major_rules_index.json`、`2026-07/` 成为 `V4` 的输入 |
| `CUSZ_TSCRPT.pdf` | `data/reference/CUSZ_TSCRPT.pdf` | `git mv`；成绩单样本，只作参考，不参与生成 |
| `docs/study-progress-workflow.md` | `docs/workflows/study-progress.md` | `git mv`；文件名与目录同时规范化 |
| `.agents/skills/` | `.agents/skills/`（保留） | AI 分析工作流保持在根目录，作为规则抽取的入口（被 `README.md` 与 `docs/workflows/study-progress.md` 引用） |

映射的三条规律：

1. **旧实现的代码进 `docs/legacy-reference/` 或 `pipeline/`**，永不进 `backend/` / `frontend/`。
   `backend/` 与 `frontend/` 里只有官方代码 + 本项目的新实现。
2. **数据进 `data/`，脚本进 `pipeline/`**。混在一起会让「哪个文件可以重跑」无法判断。
3. **删除的都是「可被重新实现的东西」**：静态页、生成好的 `.js` 数据文件、FastAPI 路由、
   旧建库脚本、托管平台配置。没有任何业务规则被删除 —— 规则要么迁进 Java，
   要么作为语义参考留在 `docs/legacy-reference/`。

## 2. 能力对照表

「新后端」与「新前端」列是**约定路径**（见 [`README.md`](README.md) §5 的路径标记说明）；
接口路径以 [`api-contract.md`](api-contract.md) 为准。

| 能力 | 旧实现（legacy 分支） | 新后端（`ruoyi-education`） | 新前端（`frontend/src`） | 数据 / 接口 |
|---|---|---|---|---|
| **学习仪表盘** | `students-interface/student_dashboard_index.html` + `data.js`（前端算学分与 GPA） | `analytics` 域：`EduAnalyticsController` + `CreditCalculator`（学分、GPA 趋势、分类统计统一由后端算） | `views/education/dashboard/index.vue` | `edu_course_record`、`edu_study_plan`、`edu_assignment`；`GET /edu/analytics/dashboard`、`GET /edu/students/me` |
| **课程目录** | `api_server.py` 的 `/api/courses` + `students-interface/course_detail.html` | `course` 域：`EduCourseController`、`EduCoursePrereqController` | `views/education/course/index.vue` + `api/education/course.ts` | `edu_course`、`edu_course_prereq`；`GET /edu/courses/list`、`/edu/courses/{id}`、`/edu/courses/options`、`/edu/courses/{id}/prereqs` |
| **先修校验** | `students-interface/study_headway_engine.js` + 前端内联判断（`isPassing` / `earnedRecords`） | `common.PrerequisiteGraph`（`loadGraph` / `isSatisfied` / `missingPrerequisites` / `detectCycles`），由 `course`、`planning` 共用 | 规划页内联展示 `validationStatus` / `validationMessage`，**不自己算** | `edu_course_prereq`（`group_no` 组内 AND / 组间 OR）；`GET /edu/courses/{id}/prereq-check`、`POST /edu/plans/validate` |
| **培养方案（Study Headway）** | `students-interface/study_headway_rules.js`（生成的规则 JSON）+ `study_headway_engine.js`（变体匹配、组件/池/约束判定） | `curriculum` 域：`EduCurriculumController`；规则语义参考 `docs/legacy-reference/study_headway_engine.js`（`variantFor`、`evaluate`） | `views/education/curriculum/index.vue` | `edu_major`、`edu_curriculum`、`edu_curriculum_item`；`GET /edu/curricula/list`、`/edu/curricula/by-major`、`/edu/curricula/{id}` |
| **选课规划** | 静态页内的「课程篮」+ 前端即时提示，刷新即丢 | `planning` 域：`EduStudyPlanController`（草稿、校验、提交、推荐），规则顺序按方案 ch.5.1；`reason` / `validation_*` 落库 | `views/education/planner/index.vue` + `api/education/planning.ts` | `edu_study_plan`、`edu_study_plan_item`；`/edu/plans/list`、`/edu/plans/validate`、`/edu/plans/recommend`、`/edu/plans/{id}/submit` |
| **技能画像** | `students-interface/competency_data.js` + `intern_prep_data.js`（生成物）+ 浏览器内存 | `analytics` 域：`GET /edu/analytics/skill-profile`（按本人成绩现算，不落库）+ 公共标签与关联度接口 | `views/education/skill/index.vue` + **本地 `utils/education/skillProfileDb.ts`（Dexie）** | 服务端 `edu_skill_tag`、`edu_course_competency`；私人自评/笔记只在 IndexedDB（ADR-0004） |
| **教师端** | 无（`aa_dashboard_v2.html` 是学术顾问视图，不是教师授课视图） | `enrollment` 域的 `GET /edu/offerings/mine` + `assignment` 域的作业/提交/评分 | `views/education/teaching/index.vue` | `edu_course_offering`、`edu_assignment`、`edu_submission`；归属校验按 `teacher_id`（ADR-0006） |
| **管理端** | 无（旧原型没有后台） | 若依**代码生成器**生成标准 CRUD，再按 `backend-conventions.md` §8 改包名、去掉 `@DataScope`、补 `edu:*` 权限与归属校验 | 若依代码生成器生成的 Vue 页面，落到 `views/education/` 对应目录 | 全部 18 张 `edu_` 表；菜单与按钮权限来自 `V2__edu_permissions.sql`（`menu_id` 2000–2080） |

两个能力发生了**质变**，而不是简单平移：

- **先修校验从「前端算」变成「后端算」**。旧实现把规则放在浏览器里，学生改一下
  localStorage 就能绕过；新实现里规则是 Java 单测覆盖的对象，前端只渲染结论。
- **选课规划从「刷新即丢」变成「有状态」**。旧页面没有持久化，新实现有
  `edu_study_plan` / `edu_study_plan_item` 与 `DRAFT → VALIDATED → SUBMITTED → ARCHIVED`
  的状态机，并能保存「当时的推荐理由」。

## 3. 尚未完成（有意延后）

以下五项**不是遗漏**，而是方案明确要求在产品/合规条件满足后再启动的工作。
它们的共同特征是：需要外部授权、需要安全评审、或需要当前数据量之外的新能力。

| 延后项 | 现状 | 为什么现在不做 | 启动条件 |
|---|---|---|---|
| **学校 SSO** | 表结构已预留 `edu_sso_identity`（`provider` + `external_subject` 唯一、`sys_user_id` + `provider` 唯一），只存映射不存密码；业务代码全部走 `SecurityUtils.getUserId()`，替换身份来源不需改业务 | 方案 ch.7.2：需确认校方支持的 OIDC/OAuth2 或 CAS 协议、测试环境、回调地址、可访问的用户字段与授权范围。未获授权前「模拟学校 SSO」是明确禁止项（方案 ch.2.1） | 校方书面授权 + 测试环境可用 + ADR-0006 的映射流程评审通过 |
| **跨设备画像同步（端到端加密）** | 画像只在浏览器 IndexedDB；导出/导入备份是本期功能（`edu:skill:export`） | 方案 ch.7.3 要求先评审密钥生成、丢失恢复、冲突合并、威胁模型四项；在没有 SSO 身份锚点时做同步，会把账号风险叠加到画像上 | ADR-0004 末尾的四项复评清单全部有书面答案并通过安全评审 |
| **真实 SIS 写回**（选课写回学校系统、成绩回传） | 当前只读：`edu_course_record.source` 区分 `SIS_IMPORT` / `DEMO` / `MANUAL`；演示环境全部是 `DEMO` | 写回需要校方接口授权与责任划分；方案 ch.6.1 明确「正式成绩和学籍信息只有获得合法数据来源及授权后才能接入」 | 校方接口开放 + 写操作审计与回滚方案 + 数据责任协议 |
| **机器学习推荐** | 推荐是透明规则：先修图 → 方案未修筛选 → 学分上限/开课学期/时间冲突 → 生成可解释理由（方案 ch.5.1） | 方案 ch.2.1 明确第一版不部署复杂 AI 推荐模型；无可信训练数据、无离线评估、无解释能力 | 有足够历史选课与成绩数据 + 可解释性方案 + 与规则结果的对照评估 |
| **预测性学情分析**（挂科风险、毕业风险预测） | 现有学情只做**回顾性**统计：已修学分、GPA 趋势、分类进度、能力分布 | 预测需要多年纵向数据与统计口径确认；错误的预测会直接影响学生决策（方案 ch.10 的「课程数据不准确」风险在此放大） | 多年数据就绪 + 口径与免责说明 + 人工复核流程 |

除此之外，还有若干**本轮已完成、但仍需收尾**的工程项（不是产品延后）：

| 工程项 | 现状 | 收尾动作 |
|---|---|---|
| `backend/ruoyi-education/src/**` | 6 个业务域已实现（113 个 `.java` + 17 个 Mapper XML + 3 个规则单测） | 按 `backend-conventions.md` 逐域复核归属校验与 `edu:*` 权限标识 |
| `backend/sql/edu/V3..V6` 与生成器 | 已生成（`pipeline/course_import/build_edu_seed.py`，附带 `seed-report.json`） | 把生成物纳入版本控制，并在 CI 校验「重跑后 diff 为空」 |
| `frontend/src/{views,api,types,utils}/education/**` | 8 个页面 + 6 个 API 模块 + `skillProfileDb.ts` 已落地 | 清理类型检查探针文件（`frontend/src/__probe.ts`、`frontend/src/views/education/__probe.vue`、`tsc-report.txt`） |
| `deploy/` | Nginx / Compose / Dockerfile / `.env.example` 已落地 | 与 `application-docker.yml`、`frontend/.env.production` 做一次端到端启动验证 |

`V3..V6` 与前端 `education/` 目前仍是**未跟踪文件**（`git status` 显示为 `??`），
合入前需要与所属模块一起提交，否则新克隆的仓库缺少教育页面与演示数据。

## 4. 迁移验收方式

| 检查 | 方法 | 期望 |
|---|---|---|
| 旧功能无遗漏 | 按第 2 节「能力对照表」逐行走一遍新页面 | 8 项能力都有新落点 |
| 旧规则语义无漂移 | 用同一份学生成绩 + 同一份培养方案，比较旧引擎（`docs/legacy-reference/study_headway_engine.js`）与新 Java 实现的完成度结论 | 结论一致；差异必须能解释（例如旧引擎的 `codeAliases` 合并规则尚未迁移） |
| 路径映射无悬空 | 按第 1 节每行 `Test-Path` 新路径 | 全部存在，或明确标注为「约定路径 / 生成物」 |
| 没有规则被删掉 | 搜索 `legacy/static-prototype-v1` 中的规则常量与判定分支，确认每条都有对应实现或明确延后 | 无静默丢弃 |
