# 版本锁定矩阵

> 原则（方案 2.2）：先锁定官方后端与前端的一个明确版本/提交号，运行通过后把实际版本写入本文件，
> **不要**直接跟随 `master` 自动升级。升级需单独提交 PR 并附 PoC 结论。

## 1. 官方底座

| 组件 | 来源仓库 | 分支 | 提交号（SHA） | 版本 | 提交日期 |
|---|---|---|---|---|---|
| 后端（若依） | https://github.com/yangzongzhuan/RuoYi-Vue | `master` | `091a97bded748f0ddbececcd08f8503bd5aa4a25` | v3.9.2 | 2026-11-10 |
| 前端（若依 Vue3 + TS） | https://github.com/yangzongzhuan/RuoYi-Vue3 | `typescript` | `6054b23b9358fc64ef7c74b79ec1e42d968aa51c` | v3.9.2 | 2026-11-12 |

目录对应关系：

- `backend/` ← RuoYi-Vue `master`（已剔除 `.git`、`.github`）
- `frontend/` ← RuoYi-Vue3 `typescript` 分支（已剔除 `.git`、`.github`）
- 官方 README 保留为 `backend/README.ruoyi-vue.md`、`frontend/README.ruoyi-vue3.md`
- 许可证保留为 `backend/LICENSE`、`frontend/LICENSE`（MIT）

> 说明：`RuoYi-Vue3` 只是前端仓库，**不是**完整的 Java 后端；
> 后端必须搭配 `RuoYi-Vue`。方案 2.1 已明确这一点。

## 2. 运行时与工具链

| 组件 | 锁定版本 | 说明 |
|---|---|---|
| JDK | 21.0.4 (Microsoft OpenJDK, LTS) | 后端 `pom.xml` 的 `<java.version>17</java.version>` 编译目标在 JDK 21 上可正常构建；如需改用 17 见下方「兼容性说明」 |
| Maven | 3.9.x（建议） | 通过 `mvn -v` 确认；仓库未内置 wrapper |
| Node.js | 24.16.0 | 前端 `vite 6.4.1` 要求 Node ≥ 18 |
| npm | 随 Node 24 附带 | 前端依赖使用 `npm install` 安装 |
| MySQL | 8.0.43 | 需启用 `utf8mb4`；DDL 使用 MySQL 8 的 `check` 约束 |
| Redis | 7.x（建议） | 若依 Token/缓存依赖；Docker Compose 中锁定 `redis:7-alpine` |
| Docker / Compose | Docker Desktop 4.x / Compose v2 | 见 `deploy/docker-compose.yml` |

前端核心依赖版本（来自 `frontend/package.json`，**不要**随手升级）：

| 依赖 | 版本 |
|---|---|
| Vue | 3.5.26 |
| Vue Router | 4.6.4 |
| Pinia | 3.0.4 |
| Element Plus | 2.13.1 |
| ECharts | 5.6.0 |
| Vite | 6.4.1 |
| TypeScript | 5.6.3 |
| axios | 1.13.2 |

后端核心依赖版本（来自 `backend/pom.xml`）：

| 依赖 | 版本 |
|---|---|
| Spring Boot | 4.1.2 |
| MyBatis Spring Boot Starter | 4.1.0 |
| Druid | 1.2.28 |
| PageHelper | 4.1.1 |
| Fastjson2 | 2.0.66 |
| jjwt | 0.13.0 |
| springdoc | 3.2.0 |

## 3. 本项目新增

| 组件 | 位置 | 版本 |
|---|---|---|
| 教育业务模块 | `backend/ruoyi-education` | 与若依同版本 `3.9.2` |
| 教育库表迁移 | `backend/sql/edu/` | `V1`~`V6`（见下） |
| 前端教育页面 | `frontend/src/views/education/` | — |
| 本地画像存储 | `frontend/src/utils/education/skillProfileDb.ts` | `dexie ^4.0.11` |
| 离线数据管道 | `pipeline/` | Python 3.10+ / Node 18+ |

## 4. 数据库迁移顺序

```text
backend/sql/ry_20260417.sql      若依基础表（sys_*）
backend/sql/quartz.sql           定时任务表（未启用可跳过）
backend/sql/edu/V1__edu_schema.sql      教育库表结构
backend/sql/edu/V2__edu_permissions.sql 角色、演示账号、菜单与按钮权限
backend/sql/edu/V3__edu_seed_course.sql     课程目录与先修关系（生成）
backend/sql/edu/V4__edu_seed_curriculum.sql 专业与培养方案（生成）
backend/sql/edu/V5__edu_seed_student.sql    学期、开课、演示学生与成绩（生成）
backend/sql/edu/V6__edu_seed_competency.sql 技能标签与课程关联度（生成）
```

`V3`~`V6` 由 `pipeline/course_import/build_edu_seed.py` 生成，**不要手工改动**：

```bash
python pipeline/course_import/build_edu_seed.py --all
```

## 5. 兼容性说明

- 后端 `pom.xml` 声明 `<java.version>17</java.version>`（若依 v3.9.2 原始值），
  在 JDK 21 上以 `source/target 17` 编译，运行于 JDK 21。
  若团队统一要求 21，将该项改为 `21` 并重跑构建与回归测试。
- Spring Boot 4.x 使用 `tools.jackson`（Jackson 3），与若依 3.9.2 一致，勿混用 `com.fasterxml.jackson` 新代码。
- 前端为 TypeScript 分支，新增页面请遵守 `frontend/tsconfig.json` 的严格度设置，
  并确保 `npm run build:prod`（内部含 `vue-tsc`）通过。

## 6. 变更记录

| 日期 | 变更 | 负责人 |
|---|---|---|
| 2026-11-12 | 锁定 RuoYi-Vue v3.9.2 与 RuoYi-Vue3 typescript 分支，新增 ruoyi-education | 技术负责人 |
