# ADR-0007：单仓多目录布局（`backend/` + `frontend/` + `data/` + `pipeline/` + `deploy/` + `docs/`）

## 状态（Status）

已接受（Accepted）。2026-11-12。

## 背景（Context）

官方 RuoYi-Vue 的目录布局是「仓库根 = 后端根」：

```text
RuoYi-Vue/
├── pom.xml
├── ruoyi-admin/  ruoyi-framework/  ruoyi-system/  ruoyi-common/
├── ruoyi-quartz/  ruoyi-generator/
├── sql/  bin/  doc/
└── README.md
```

前端在**另一个仓库**（`RuoYi-Vue3`），数据与离线管道在原型的根目录里
（`tools/`、`competency-analysis/`、`job-hunting-module/`、`course_list_database/`、
`dataset/`、`graduation_rules/`、`students-interface/`、`api_server.py`）。

本项目的实际情况是：一个团队、一个部署目标、一条数据链——**从 SIS 数据到 Java 规则到 Vue 页面**。
方案 ch.4.1 给的目录清单已经把 `ruoyi-ui-vue3-ts` 与 `ruoyi-education` 并列，
但没有说明它们与数据、管道、部署配置的关系，也没说仓库该怎么切。

## 决策（Decision）

**一个仓库，按职责分顶层目录**：

```text
Student-dashboard-demo/
├── backend/          RuoYi-Vue（官方 master 提交，剔除 .git/.github）
│   ├── pom.xml                 ← 顶层 reactor，含 ruoyi-education
│   ├── ruoyi-admin/            ← 启动类与官方 Web 层
│   ├── ruoyi-framework/ ruoyi-system/ ruoyi-common/ ruoyi-quartz/ ruoyi-generator/
│   ├── ruoyi-education/        ← 唯一新增模块（6 个业务域包）
│   ├── sql/                    ← ry_20260417.sql、quartz.sql、edu/V1..V6
│   ├── README.ruoyi-vue.md     ← 官方 README 原文保留
│   └── LICENSE                 ← MIT 原文保留
├── frontend/         RuoYi-Vue3 typescript 分支（官方提交，剔除 .git/.github）
│   ├── src/  vite/  public/
│   ├── package.json
│   ├── README.ruoyi-vue3.md
│   └── LICENSE
├── data/             离线产物与源数据（不作为运行时输入）
│   ├── course_catalog/  demo/  competency/  graduation_rules/  career/  reference/
├── pipeline/         离线 ETL / 爬虫 / 生成器（Python 3.10+、Node 18+）
│   ├── course_import/  competency/  career/  graduation_rules/  sis-crawler/
├── deploy/           自托管部署（Nginx + Docker Compose）
├── docs/             架构、契约、方案、工作流、旧实现参考
│   ├── architecture/  proposal/  workflows/  legacy-reference/
├── .agents/skills/   AI 分析工作流（培养方案规则抽取）
├── versions.md       版本锁定矩阵（唯一真源）
└── README.md         项目入口
```

### 与方案 ch.4.1 的对应关系

| 方案 ch.4.1 的名称 | 本仓库位置 | 说明 |
|---|---|---|
| `ruoyi-admin`（启动与外部 API） | `backend/ruoyi-admin` | 官方模块，改动限于 pom 里的一个 dependency |
| `ruoyi-framework`（尽量少改） | `backend/ruoyi-framework` | 官方模块，零修改 |
| `ruoyi-system`（用户/角色/菜单/数据权限） | `backend/ruoyi-system` | 官方模块，零修改 |
| `ruoyi-common`（共享基础工具） | `backend/ruoyi-common` | 官方模块，零修改；`ruoyi-education` 唯一依赖 |
| `ruoyi-education`（新增业务模块，6 域） | `backend/ruoyi-education` | 见 ADR-0002 |
| `ruoyi-ui-vue3-ts`（独立前端项目） | `frontend/` | 官方 `typescript` 分支，去掉一层目录名 |
| 数据库迁移 | `backend/sql/edu/V1..V6` | 见 ADR-0005 |
| 数据与离线管道 | `data/`、`pipeline/` | 方案未展开，本 ADR 补充 |

### 为什么不分仓

| 理由 | 说明 |
|---|---|
| **一次 PR 跨端** | 一个业务改动往往同时动「接口契约 + Java Service + TS 页面 + SQL 种子」。分仓就要三个 PR，任一处漏改都会让主干不可运行，与方案 ch.8.2「Git 主干保持可运行」冲突 |
| **版本锁定只有一个文件** | `versions.md` 同时记录后端 SHA 与前端 SHA。分仓后「后端 v3.9.2 + 前端 v3.9.2」的一致性没有任何机制保证 |
| **数据链完整可见** | `data/` 的源文件 → `pipeline/` 的生成器 → `backend/sql/edu/V3..V6` 的种子 → 页面展示。同仓时这条链可以用 `grep` 追到底；分仓后只能靠文档描述 |
| **新成员一个 clone 就能跑** | 方案 ch.9.1 的第一条验收是「新成员依据 README 从零启动成功」。分仓意味着 3 个 clone、3 套权限、3 份 README |
| **团队 5–6 人** | 分仓带来的权限/CI/发布编排成本，远高于它带来的隔离收益 |

### 为什么不用 RuoYi 的原始布局

原始布局把 `ruoyi-*` 放在仓库根。如果照搬，`frontend/`、`data/`、`pipeline/`、`docs/`
会与 `ruoyi-admin/`、`ruoyi-system/` 混在同一层：`ls` 一下分不清哪些是官方模块、
哪些是本项目的资产。更实际的问题是——官方模块哪天要整体替换成新版本的目录，
根目录混放会让替换变成一次手工整理。

用 `backend/` 包一层，收益是：

- 「官方资产」与「本项目资产」在路径前缀上就能区分；
- 替换官方版本 = 整体替换 `backend/` 与 `frontend/` 两个目录，本项目资产零迁移；
- `mvn -f backend/pom.xml` 与 `npm --prefix frontend` 的路径稳定，CI 脚本只写一次。

### 为什么离线 ETL 放 `pipeline/` 而不是做成运行时模块

| 理由 | 说明 |
|---|---|
| **依赖完全不同** | `pipeline/` 需要 Python（`openpyxl`、`mysql-connector-python`、爬虫）与 Node（`.mjs` 工作簿脚本）。做成 Spring Batch 模块会把 Python 生态的依赖等价物带进可运行的 jar |
| **运行频率低** | SIS 课程目录与培养方案一年更新几次；放进服务端意味着要写调度、监控、失败重试，收益为零 |
| **必须有人审核** | 能力关联度与培养方案规则通过 AI 辅助分析产出（`.agents/skills/extract-graduation-rules/`、`pipeline/competency/`），产物必须人工复核后才能入库。运行时装在服务里会绕过这道人工闸门 |
| **爬虫需要授权与人工在场** | `pipeline/sis-crawler/` 访问 SIS，必须由授权用户按学校政策执行（`README.md` 的「Scope and Data Safety」）。这类操作绝不能由服务进程自动发起 |
| **产物需要 review 后再提交** | 生成物是 `backend/sql/edu/V3..V6` 与 `data/` 下的工作簿，应以 PR diff 的形式被审阅（ADR-0005） |
| **可复现性** | 离线脚本的输入输出都在 `data/` 里，任何人重跑得到同样结果；运行时模块的输出依赖数据库当前状态，不可复现 |

`pipeline/` 的运行方式（数据负责人手动执行，见 `versions.md` §4；生成器产出 `V3..V6` 与
`seed-report.json`，重跑一致性由 CI 校验，风险见 [`risks.md`](../risks.md) R10）：

```bash
python pipeline/course_import/build_edu_seed.py --all
```

## 后果（Consequences）

**正面**

- 单一克隆、单一 `versions.md`、单一 PR 即可完成跨端改动。
- 官方资产（`backend/`、`frontend/`）与本项目资产（`data/`、`pipeline/`、`deploy/`、`docs/`）
  在路径上一眼可分，升级官方版本是目录级替换。
- 数据链（源文件 → 生成器 → 种子 SQL）在同一棵树下，可用 `grep` 追查。
- 离线 ETL 与运行时彻底隔离：生产进程不持有爬虫与 Excel 解析能力。

**需要承担的代价**

- **仓库体积**：`data/` 下有 xlsx 与 zip（`data/competency/source/Major Programmes.zip`、
  多个 `.xlsx`）。当前规模可接受，继续增长需要评估 Git LFS 或把大源文件移出仓库。
- **CI 必须按路径分流**：`backend/**` 跑 `mvn`，`frontend/**` 跑 `npm run build:prod`，
  `pipeline/**` 只在生成物校验时跑。单仓不等于单流水线，配置要写清楚。
- **数据更新产生大 diff**：课程数据上千行，PR 审阅策略是「看行数 + 抽样 + CI 重跑一致」。
- **`deploy/` 承担四种配置，容易各说各话**：Nginx 反代（`deploy/nginx/nginx.conf` 的
  `/prod-api/` → `http://backend:8080/`）、Compose 编排（`deploy/docker-compose.yml`）、
  镜像构建（`backend.Dockerfile` / `frontend.Dockerfile`）、环境变量样例（`deploy/.env.example`）。
  四者必须与 `frontend/.env.production`、`backend/ruoyi-admin/src/main/resources/application-docker.yml`
  保持同一套约定，任一处漂移都会让「从零启动」失败（风险见 `risks.md` R14）。
- **`.agents/skills/` 留在根目录**：它是 AI 分析工作流的入口（被 `README.md`
  与 `docs/workflows/study-progress.md` 引用），不放进 `pipeline/`，
  以免「工具链」与「AI 技能定义」混在一起。

## 备选方案（Alternatives considered）

| 方案 | 为什么没选 |
|---|---|
| **多仓**（`-backend` / `-frontend` / `-data` 三个仓库） | 跨端改动要三个 PR；版本一致性无机制保证；新成员三份 clone；对 5–6 人团队的收益为负 |
| **RuoYi 原始布局**（`ruoyi-*` 在仓库根） | 官方模块与本项目资产混层，`ls` 不可读；替换官方版本时无法整体替换 |
| **用 git submodule 引入官方仓库** | submodule 的提交号与 `versions.md` 会形成两个真源；子模块更新需要额外命令，新成员容易漏掉 `--recursive` |
| **把 `pipeline/` 做成 Spring Batch 模块** | 把 Python/Node 的分析能力重写成 Java 是巨大的无效工作；且会让「人工审核」这道闸门可以被服务调用绕过 |
| **把 `data/` 放到对象存储（S3/MinIO）** | 引入额外基础设施与凭证管理；数据需要与生成器、种子 SQL 一起被 review，放进对象存储后 diff 不可见 |
| **`frontend/` 保持官方仓库名 `RuoYi-Vue3/`** | 目录名带版本号会在升级时被迫改名，所有脚本与文档都要跟着改；`frontend/` 表达的是职责而不是来源 |
