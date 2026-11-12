# 学习进度仪表盘（Education Platform）

面向 CUHK-Shenzhen 学生与学业导师的教学与学习规划平台：课程目录、培养方案、先修约束检查、学期选课规划、
学分与成绩趋势、学院能力画像，以及后续可在获得校方授权后接入的统一登录（SSO）。

本仓库已按《教育平台基于 RuoYi-Vue 的技术架构与实施方案》完成重构：
从「Python FastAPI + 静态 HTML + React 登录页」的混合原型，迁移为
**若依（RuoYi-Vue）单体后端 + 官方 Vue 3 + TypeScript 前端** 的前后端分离结构。

> ⚠️ 本项目是**教学与课程规划原型**，不是学校官方毕业审核系统，也不构成就业保证。
> 登录与会话为平台自有账号，**不是**学校统一身份认证（SSO 见 `docs/architecture/adr/ADR-0006-security-baseline.md`）。

## 目录

| 需求 | 入口 |
|---|---|
| 了解整体架构 | [`docs/architecture/README.md`](docs/architecture/README.md) |
| 本地把项目跑起来 | [快速开始](#快速开始) |
| 一条命令起全套服务 | [`deploy/README.md`](deploy/README.md) |
| 版本锁定矩阵 | [`versions.md`](versions.md) |
| 后端编码约定 | [`docs/architecture/backend-conventions.md`](docs/architecture/backend-conventions.md) |
| 接口契约（前后端联调依据） | [`docs/architecture/api-contract.md`](docs/architecture/api-contract.md) |
| 数据库结构与 ER 图 | [`docs/architecture/er-diagram.md`](docs/architecture/er-diagram.md) |
| 旧版 → 新版文件对照 | [`docs/architecture/migration-map.md`](docs/architecture/migration-map.md) |
| 原始架构方案 | [`docs/proposal/architecture-proposal-v1.docx`](docs/proposal/architecture-proposal-v1.docx) |

## 架构总览

```text
┌──────────────────────────────────────────────────────────────────────┐
│  访问层  Vue 3 + TypeScript + Vite + Element Plus + Pinia + ECharts   │
│          学生端 / 教师端 / 管理端（同一套前端，按菜单权限分流）           │
├──────────────────────────────────────────────────────────────────────┤
│  网关    Nginx：HTTPS、静态资源、SPA history 回退、/api 与 /prod-api 反代 │
├──────────────────────────────────────────────────────────────────────┤
│  若依基础能力（直接复用，尽量不改）                                      │
│          Spring Security + JWT、用户/角色/菜单/按钮权限、                 │
│          操作日志、字典、代码生成、定时任务、文件上传                      │
├──────────────────────────────────────────────────────────────────────┤
│  教育业务能力（新增 ruoyi-education，与底座隔离）                         │
│          course / curriculum / enrollment / assignment /               │
│          planning / analytics                                          │
├──────────────────────────────────────────────────────────────────────┤
│  持久化  MySQL 8（edu_* 业务表 + 复用 sys_* 管理表）、Redis（Token/缓存）  │
├──────────────────────────────────────────────────────────────────────┤
│  本地优先  个人技能画像 / 私人笔记 → 浏览器 IndexedDB（Dexie.js）         │
└──────────────────────────────────────────────────────────────────────┘
```

关键决策边界（详见 `docs/architecture/adr/`）：

- **后端形态**：若依多模块单体，MVP 阶段不引入 Spring Cloud 微服务。
- **不改内核**：`ruoyi-admin` / `ruoyi-system` / `ruoyi-framework` 只允许“加依赖”级别的改动，
  业务全部新增在 `ruoyi-education`。
- **推荐是规则不是模型**：先修关系 + 学分规则 + 开课学期/冲突检查，每条推荐都带可解释说明，
  不直接部署 AI 推荐模型。
- **个人画像不上服务端**：只在浏览器 IndexedDB 保存，可导出/恢复；跨设备加密同步留到后期评审。

## 仓库结构

```text
Student-dashboard-demo/
├── backend/                        若依 RuoYi-Vue 后端（多模块 Maven）
│   ├── ruoyi-admin/                启动入口与通用接口
│   ├── ruoyi-framework/            安全、数据源、拦截器等框架配置
│   ├── ruoyi-system/               用户、角色、菜单、字典、日志
│   ├── ruoyi-common/               共享基础工具
│   ├── ruoyi-generator/            代码生成器
│   ├── ruoyi-quartz/               定时任务
│   ├── ruoyi-education/            ★ 教育业务模块（本项目主要开发位置）
│   │   └── src/main/java/com/ruoyi/education/{course,curriculum,enrollment,
│   │                                             assignment,planning,analytics}
│   └── sql/
│       ├── ry_20260417.sql         若依基础表
│       ├── quartz.sql              定时任务表
│       └── edu/                    V1 结构 → V2 权限 → V3..V6 生成数据
├── frontend/                       官方 RuoYi-Vue3 TypeScript 前端
│   └── src/{api,views,types,utils}/education/   ★ 教育业务页面
├── data/                           源数据（唯一事实来源）
│   ├── course_catalog/             SIS 课程目录与大纲导出
│   ├── demo/                       演示学生成绩单
│   ├── graduation_rules/           培养方案规则与版本索引
│   ├── competency/                 学院能力池与课程关联度
│   ├── career/                     职位关键词与技能映射
│   └── reference/                  参考样本（成绩单 PDF）
├── pipeline/                       离线数据管道（非运行时模块）
│   ├── course_import/              ★ 生成 edu_* 种子 SQL
│   ├── competency/                 能力分析脚本
│   ├── graduation_rules/           规则抽取与编译（含旧 JS 引擎测试）
│   ├── career/                     职位 → 技能 → 课程匹配
│   └── sis-crawler/                授权范围内的 SIS 采集工作流
├── deploy/                         Docker Compose + Nginx 部署栈
├── docs/
│   ├── architecture/               分层、模块边界、ER 图、API、ADR、风险
│   ├── workflows/                  业务流程说明
│   ├── legacy-reference/           旧 JS 规则引擎（迁移参考，冻结）
│   └── proposal/                   原始架构方案
├── versions.md                     版本锁定矩阵（先跑通再锁定）
└── README.md
```

## 技术栈

| 层次 | 选型 |
|---|---|
| 后端基座 | RuoYi-Vue v3.9.2（Spring Boot 4.1.2、Spring Security、MyBatis、Druid、PageHelper） |
| 运行时 | JDK 21（编译目标 17，见 `versions.md` 兼容性说明） |
| 前端 | 官方 RuoYi-Vue3 TypeScript 分支：Vue 3.5 + Vite 6 + Element Plus 2.13 + Pinia + ECharts |
| 数据库 | MySQL 8.0（utf8mb4） |
| 缓存 | Redis 7（若依 Token/缓存依赖） |
| 本地存储 | IndexedDB + Dexie.js（个人技能画像、私人笔记） |
| 部署 | Nginx + Docker Compose + Linux |

精确版本与提交号见 [`versions.md`](versions.md)。**不要**直接跟随官方 `master` 自动升级。

## 快速开始

### 前置条件

- JDK 21、Maven 3.9+
- Node.js 18+（建议与 `versions.md` 一致）
- MySQL 8.0、Redis 7
- 可选：Docker Desktop（走 `deploy/` 一键启动）

### 方式一：Docker Compose（推荐首次体验）

```bash
cd deploy
cp .env.example .env      # 修改 DB_PASSWORD / REDIS_PASSWORD / TOKEN_SECRET
docker compose up -d --build
```

默认入口 <http://localhost/>，完整说明见 [`deploy/README.md`](deploy/README.md)。

### 方式二：本地开发

**1. 建库并初始化（顺序不能变）**

```bash
mysql -u root -p -e "CREATE DATABASE education_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

mysql -u root -p education_db < backend/sql/ry_20260417.sql
mysql -u root -p education_db < backend/sql/quartz.sql
mysql -u root -p education_db < backend/sql/edu/V1__edu_schema.sql
mysql -u root -p education_db < backend/sql/edu/V2__edu_permissions.sql
mysql -u root -p education_db < backend/sql/edu/V3__edu_seed_course.sql
mysql -u root -p education_db < backend/sql/edu/V4__edu_seed_curriculum.sql
mysql -u root -p education_db < backend/sql/edu/V5__edu_seed_student.sql
mysql -u root -p education_db < backend/sql/edu/V6__edu_seed_competency.sql
```

`V3`~`V6` 是生成文件，可由源数据重新生成（见[数据管道](#数据管道)），不要手工维护。

**2. 启动后端**

```bash
cd backend
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

或直接在 IDE 运行 `com.ruoyi.RuoYiApplication`。默认端口 `8080`。
数据库/Redis 连接在 `backend/ruoyi-admin/src/main/resources/application-druid.yml`
与 `application.yml` 中配置；容器部署请使用 `application-docker.yml`（环境变量注入）。

**3. 启动前端**

```bash
cd frontend
npm install
npm run dev            # http://localhost:80，/dev-api 代理到 127.0.0.1:8080
```

生产构建：`npm run build:prod`（含 `vue-tsc` 类型检查）。

**4. 登录**

| 账号 | 密码 | 角色 | 可见范围 |
|---|---|---|---|
| `admin` | `admin123` | 管理员 | 全部 |
| `stu_chen` | `admin123` | 学生 | 本人成绩与规划 |
| `teacher_li` | `admin123` | 教师 | 本人授课课程 |

> 初始密码与若依默认一致，**上线前必须修改**；`deploy/.env` 与生产密钥不得提交到仓库。

### 方式三：只跑数据管道

```bash
python pipeline/course_import/build_edu_seed.py --all
```

重新生成 `backend/sql/edu/V3`~`V6` 与 `pipeline/course_import/seed-report.json`。

## 教育业务模块

`ruoyi-education` 按业务域分包，每个域内统一 `domain → dto → mapper → service → controller`：

| 业务域 | 能力 | 核心表 |
|---|---|---|
| `course` | 课程目录、查询、先修关系维护与先修校验 | `edu_course`、`edu_course_prereq` |
| `curriculum` | 专业、培养方案版本、必修/选修学分要求 | `edu_major`、`edu_curriculum`、`edu_curriculum_item` |
| `enrollment` | 学期、开课、选课、成绩记录、学分进度 | `edu_term`、`edu_course_offering`、`edu_enrollment`、`edu_course_record` |
| `assignment` | 作业发布、学生提交、教师评分 | `edu_assignment`、`edu_submission` |
| `planning` | 学期规划、先修/学分/学期冲突校验、可解释推荐 | `edu_study_plan`、`edu_study_plan_item` |
| `analytics` | 学分进度、GPA 趋势、学院能力池与技能画像 | `edu_skill_tag`、`edu_course_competency` |

推荐规则的执行顺序（方案 5.1）：

```text
① 读取学生已通过课程
② 沿 edu_course_prereq 有向图判定先修（组内 AND / 组间 OR）
③ 过滤当前培养方案中尚未修读的课程
④ 校验学分上限、开课时学期、时间冲突
⑤ 为每条结果生成可解释说明（如「缺少先修课程：数据结构（CSC3100）」）
```

> `edu_study_plan` 的校验结果会连同 `reason` / `validation_message` 一起持久化，
> 便于学生和导师复核，而不是只返回一个“不可选”。

## 数据管道

离线数据处理与运行时解耦，`data/` 是唯一事实来源，`backend/sql/edu/` 是可复现的产物。

| 输入 | 管道 | 产物 |
|---|---|---|
| `data/course_catalog/sis_course_outlines_export.xlsx` | `pipeline/course_import/build_edu_seed.py` | `V3`（课程 + 先修边） |
| `data/graduation_rules/SDS_major_rules_index.json`、`data/course_catalog/All_Course_Lists.xlsx` | 同上 | `V4`（专业 / 培养方案 / 条目） |
| `data/demo/*.xlsx` | 同上 | `V5`（学期 / 学生 / 教师 / 开课 / 成绩） |
| `data/competency/a_to_z_course_competency_relevance.xlsx` | 同上 | `V6`（技能标签 / 课程关联度） |
| `data/career/keywords.txt` + 公开招聘信息 | `pipeline/career/` | 职位技能映射与课程推荐 |

> 生成脚本是幂等、确定性的：同样的输入会产生完全一致的文件，
> 因此这些 SQL 可以进代码评审，而不会产生噪音 diff。

## 生成物与事实来源

| 生成物 | 由谁生成 | 是否可手工编辑 |
|---|---|---|
| `backend/sql/edu/V3..V6__*.sql` | `pipeline/course_import/build_edu_seed.py` | ❌ |
| `pipeline/course_import/seed-report.json` | 同上 | ❌ |
| `deploy/mysql/init/` 挂载的 SQL | 直接绑定 `backend/sql/` | ❌（不要复制一份） |
| `data/competency/a_to_z_*.xlsx` | `pipeline/competency/scripts/` | ❌（改源数据后重跑） |

## 文档索引

- [`docs/architecture/README.md`](docs/architecture/README.md)：架构文档地图与阅读顺序
- [`docs/architecture/module-boundaries.md`](docs/architecture/module-boundaries.md)：分层、依赖方向、禁止事项
- [`docs/architecture/er-diagram.md`](docs/architecture/er-diagram.md)：18 张 `edu_` 表 ER 图与索引清单
- [`docs/architecture/api-contract.md`](docs/architecture/api-contract.md)：接口契约与访问控制
- [`docs/architecture/backend-conventions.md`](docs/architecture/backend-conventions.md)：编码约定
- [`docs/architecture/migration-map.md`](docs/architecture/migration-map.md)：旧版文件去向与能力对照
- [`docs/architecture/risks.md`](docs/architecture/risks.md)：风险登记册
- [`docs/workflows/study-progress.md`](docs/workflows/study-progress.md)：学习进度业务流程

## 分支策略

| 分支 | 用途 |
|---|---|
| `main` | 交付主干 |
| `legacy/static-prototype-v1` | **冻结保留**重构前的原型（静态页面 + FastAPI + React 登录页），随时可回溯 |
| `feat/ruoyi-vue-refactor` | 本次架构重构 |

研发规则：主干保持可运行，功能走短分支 + Pull Request；提交时同步数据库迁移与 API 文档，
避免“只在自己电脑能运行”。

## 安全与数据合规

- 全站 HTTPS；生产环境关闭默认示例账号，修改初始密码与 `token.secret`，密钥只用环境变量。
- **服务端做资源级鉴权**：菜单权限只控制入口；学生只能访问本人成绩/规划，教师只能访问本人授课课程。
  身份一律取自 JWT（`SecurityUtils.getUserId()`），**不采信前端传入的 `studentId`**。
- 个人技能画像只存浏览器 IndexedDB，服务端不保存明文；导出/恢复由用户主动操作。
- 成绩与学籍只有在获得校方数据来源与授权后才可接入，当前为演示数据。
- 不得提交密码、API Key、生产凭据、真实学生记录、完整 JD 原始语料、未授权 SIS 导出。
- 保留若依原项目版权与许可证（MIT），见 `backend/LICENSE`、`frontend/LICENSE`。

## 已知限制

- 仅覆盖 SDS / SME 部分专业的演示规则；转学分、交换、豁免需人工录入。
- 培养方案解析来自公开 Study Scheme，`total_credits` 等口径以教务最终要求为准。
- 学校 SSO、跨设备加密同步、智能推荐、复杂学情分析均为**后续独立阶段**，需先获得授权与安全评审。
- 详细逐项说明见 [`docs/architecture/migration-map.md`](docs/architecture/migration-map.md) 的「尚未完成」一节。
