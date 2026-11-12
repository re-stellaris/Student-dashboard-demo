# ADR-0001：以 RuoYi-Vue 为底座，锁定后端 `master` 与前端 `typescript` 分支

## 状态（Status）

已接受（Accepted）。2026-11-12。

## 背景（Context）

平台需要一个能立刻提供「用户、角色、菜单、按钮权限、部门、字典、操作日志、代码生成器」的
Java 后端起点的同时，把工程精力集中在教育规则（先修图、学分、规划校验、学情）上。
方案 ch.2.1 与 ch.2.2 把选型收敛为若依家族，但若依有多个分支形态，选错会带来两种不同方向的浪费：

- 选重了（Cloud / Plus）：为了 MVP 用不到的能力引入分布式或另一套生态；
- 选轻了（纯 Spring Boot 自建）：把 6 人团队的时间花在重建权限与代码生成上。

同时存在一个具体的认知陷阱：`RuoYi-Vue3` 是**前端仓库**，不是完整的 Java 后端。
把它当成后端会直接导致项目无法启动。

## 决策（Decision）

1. **底座选 `RuoYi-Vue` 单体多模块**（Spring Boot 4.x 分支），不选 Cloud，不选 Vue-Plus。
   理由见下表与「备选方案」。
2. **后端锁 `yangzongzhuan/RuoYi-Vue` 的 `master` 分支**，提交号
   `091a97bded748f0ddbececcd08f8503bd5aa4a25`，版本 v3.9.2，提交日期 2026-11-10。
   该提交带 Spring Boot `4.1.2`、MyBatis Spring Boot Starter `4.1.0`、Druid `1.2.28`、
   PageHelper `4.1.1`、jjwt `0.13.0`、springdoc `3.2.0`。
3. **前端锁 `yangzongzhuan/RuoYi-Vue3` 的 `typescript` 分支**，提交号
   `6054b23b9358fc64ef7c74b79ec1e42d968aa51c`，版本 v3.9.2，提交日期 2026-11-12。
   选 TS 分支而不是 JS 分支，是为了让 `npm run build:prod`（内含 `vue-tsc`）在 CI 阶段
   挡住「接口字段拼错」这类最常见的前后端联调错误。
4. **一个 `versions.md` 记录全部锁定**，包含 SHA、JDK、Node、MySQL、Redis、Docker，
   以及前端 8 个核心依赖与后端 6 个核心依赖的精确版本（见
   [`versions.md`](../../../versions.md) §1–§2）。新依赖进 `package.json` 必须同时进 `versions.md`。
5. **不跟随 `master` 自动升级**。升级必须单独 PR，附 PoC 结论（登录、权限、代码生成、
   自定义业务表、Docker 部署五项跑通）后，再改 `versions.md` 的 SHA。
6. 目录对应关系固定：`backend/` ← 后端仓库（剔除 `.git`、`.github`）；
   `frontend/` ← 前端仓库（同）；官方 README 保留为
   `backend/README.ruoyi-vue.md`、`frontend/README.ruoyi-vue3.md`，许可证保留为
   `backend/LICENSE`、`frontend/LICENSE`（MIT）。

为什么是这三个（来自方案 ch.2.1 的评估，本项目实测确认）：

| 方案 | 优点 | 本项目的适配判断 |
|---|---|---|
| **RuoYi-Vue（选用）** | Spring Boot 单体、多模块清晰、权限管理与代码生成齐全、文档与教程量最大 | 5–6 人团队、单校单院系、MVP 至首期上线，匹配度最高 |
| RuoYi-Cloud | 微服务基础设施与分布式组件更完整（注册中心、网关、熔断） | 需要额外的 Nacos/Gateway/Sentinel 运维与跨服务调试成本；本项目的数据量（3,358 门课程、十几个演示学生）远不到拆分阈值 |
| RuoYi-Vue-Plus | 功能更丰富、插件化更彻底 | 属于另一套技术生态（不同的 ORM/缓存/工作流选型），与「先锁官方版本」的原则冲突；混用会导致文档、教程、社区答案都对不上 |

## 后果（Consequences）

**正面**

- 权限、菜单、按钮、日志、字典、代码生成器「开箱可用」，团队把时间放在教育规则上。
- 单体部署：一个可执行 jar + 一份 Nginx 配置 + 一个 MySQL + 一个 Redis，方案 ch.2.2 的
  「Nginx + Docker Compose + Linux，不引入 Kubernetes」得以成立。
- 代码生成器可以把管理端标准 CRUD（课程、专业、培养方案）先生成再按
  `backend-conventions.md` §8 改造，省掉大量样板。
- 提交号锁定后，任何成员在任何时间 `git clone` 到的底座完全一致，排障时不会出现「你这版跟我这版不一样」。

**需要承担的代价**

- **Spring Boot 4.1.2 很新**：`tools.jackson`（Jackson 3）取代 `com.fasterxml.jackson`，
  第三方 starter 的兼容性没有老版本成熟。因此 `versions.md` §5 明确要求新代码不得混用
  `com.fasterxml.jackson`，遇到不兼容的库要换库而不是降级底座。
- **`<java.version>17</java.version>` 与 JDK 21 的组合**：以 `source/target 17` 编译、
  运行在 JDK 21 上。团队若统一要求 21，需要改这一项并重跑全量回归（已写入 `versions.md` §5）。
- **前端 TS 分支更严格**：`npm run build:prod` 含 `vue-tsc`，类型错误会阻塞构建。
  对不熟悉 TS 的成员有学习成本，但换来的是联调期更少的运行时错误。
- **单体意味着无法按模块独立扩缩容**：`analytics` 的重查询与普通 CRUD 共享同一个 JVM。
  缓解方式是在 Mapper 层控制查询规模（分页、只查需要的列），而不是提前拆服务。
- **升级成本集中在合并冲突**：因此 `ruoyi-admin` / `ruoyi-system` / `ruoyi-framework`
  的改动被限制为「依赖装配」，见 ADR-0002 与 `module-boundaries.md` §3。

## 备选方案（Alternatives considered）

| 方案 | 为什么没选 |
|---|---|
| **RuoYi-Cloud** | 为 MVP 引入注册中心、网关、配置中心、熔断与分布式事务的运维面；团队 5–6 人且允许兼岗，运维能力是瓶颈而不是功能 |
| **RuoYi-Vue-Plus** | 是另一套生态。方案 ch.2.1 明确「首期不混用」；混用的直接后果是官方文档与社区答案不再适用，排障只能靠读源码 |
| **纯 Spring Boot + 自建权限** | 需要自己实现 JWT、菜单树、按钮权限、数据权限、操作日志、代码生成，保守估计吃掉前 4 周（方案 ch.9 的第 1–2 周只够搭底座） |
| **用 `RuoYi-Vue3` 当后端** | 事实错误：该仓库只有 `src/`（Vue）与 `vite/`，没有 `pom.xml`，无法启动 Java 服务 |
| **跟随 `master` 最新提交** | 提交内容不可预期，团队会花时间在「昨天还能跑」上；方案 ch.2.2 明确要求锁定而非跟随 |
| **前端用 JS 分支** | 少一层类型检查，但把字段错误推到联调与运行时；TS 分支的 `vue-tsc` 在 CI 里免费做这件事 |
