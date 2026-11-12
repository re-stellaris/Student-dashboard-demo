# ADR-0002：一个 `ruoyi-education` Maven 模块 + 6 个业务域包，Controller 留在业务模块内

## 状态（Status）

已接受（Accepted）。2026-11-12。

## 背景（Context）

方案 ch.4.1 给出的组织方式是：

```text
ruoyi-admin       # 启动与外部 API
ruoyi-framework   # 若依底层安全与框架配置（尽量少改）
ruoyi-system      # 原有用户、角色、菜单、数据权限
ruoyi-common      # 共享基础工具
ruoyi-education   # 新增教育业务模块
  ├─ course / curriculum / enrollment / assignment / planning / analytics
ruoyi-ui-vue3-ts  # 独立前端项目
```

方案同时留了一句「可按业务复杂度再拆为多个 Maven 子模块」。于是真正要决策的是三件事：

1. `ruoyi-education` 是**一个** Maven 模块还是**六个**？
2. 教育业务的 Controller 放哪？若依的既有惯例是把 Web 层集中在 `ruoyi-admin` 的
   `com.ruoyi.web.controller.*` 下（`system` / `monitor` / `tool` / `common` 四个子包）。
3. Mapper XML、实体、DTO 放哪，跨域调用怎么约束。

约束条件：

- 硬约束：`ruoyi-admin` / `ruoyi-system` / `ruoyi-framework` 不改（`module-boundaries.md` §3）。
- 团队：5–6 人，允许兼岗（方案 ch.8）。
- 业务域之间真实存在跨域读：`planning` 必须同时读课程、培养方案、成绩（方案 ch.5.1 的规则顺序）。
- 目前只需支撑 18 张表、8 个前端页面。

## 决策（Decision）

1. **一个 Maven 模块** `backend/ruoyi-education`，`<artifactId>ruoyi-education</artifactId>`，
   与若依同版本 `3.9.2`，只依赖 `ruoyi-common`（+ `spring-boot-starter-test`）。
2. **模块内按 6 个业务域分包**（`course` / `curriculum` / `enrollment` / `assignment` /
   `planning` / `analytics`）+ 一个 `common`（枚举、`PrerequisiteGraph`、`CreditCalculator`）。
3. **Controller 放在业务模块内**，即 `com.ruoyi.education.<域>.controller`，
   而**不是** `ruoyi-admin` 的 `com.ruoyi.web.controller.*`。
4. 每个域内部统一六层：`domain / dto / mapper / service / service/impl / controller`；
   Mapper XML 放 `backend/ruoyi-education/src/main/resources/mapper/education/`。
5. 域之间的依赖方向由 `module-boundaries.md` §5 的矩阵约束，落地手段是评审 + 只暴露 `I*Service`。

### 为什么 Controller 不回 `ruoyi-admin`（对若依惯例的正面反对）

若依惯例之所以把 Controller 放 `ruoyi-admin`，是因为它的业务模块只有 `ruoyi-system` 一个，
且 `ruoyi-admin` 就是「Web 层 + 启动」。这两条前提在本项目都不成立：

| 论据 | 说明 |
|---|---|
| **与「内核零修改」直接冲突** | 每加一个教育页面都要在 `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/` 下新增文件，等于持续修改一个官方模块。`module-boundaries.md` §3 把允许的 `ruoyi-admin` 改动限制为「pom 里的一个 dependency」，Controller 放进去会让这条规则名存实亡 |
| **权限与逻辑被拆到两个模块** | 一个 `EduPlanController` 若在 `admin`、`IEduPlanService` 在 `education`，则 `@PreAuthorize` 的教育权限标识、资源归属校验（`SecurityUtils.getUserId()` → `edu_student`）与规则计算分居两地，评审时要跨目录对照，最容易漏掉的是「归属校验写在 Controller 里、Service 假设已被校验」这类漏洞 |
| **模块可移除性** | 教育模块是「加在若依上的东西」。它的 Controller 在自己模块里时，删掉 `ruoyi-education` 依赖后 `ruoyi-admin` 立刻回到官方状态；Controller 在 `admin` 里则永远有残留 |
| **构建与审查面** | `mvn -pl ruoyi-admin -am package` 会把整个 Web 层都编一遍；一个 PR 只应触碰 `ruoyi-education/**`，评审 diff 才可读 |
| **扫描机制天然支持** | 已验证 `RuoYiApplication` 的 `@SpringBootApplication` 基包是 `com.ruoyi`，`ApplicationConfig` 的 `@MapperScan("com.ruoyi.**.mapper")` 与 `application.yml` 的 `typeAliasesPackage: com.ruoyi.**.domain` / `mapperLocations: classpath*:mapper/**/*Mapper.xml` 三处通配都覆盖 `com.ruoyi.education.**`。**不需要为「Controller 在业务模块」写任何额外配置** |

### 为什么是 6 个包而不是 6 个 Maven 模块

| 论据 | 说明 |
|---|---|
| **Maven 边界挡不住真实耦合** | 域之间的耦合是数据库层的（`planning` 要 JOIN `edu_course_prereq`）。真正需要拆的跨域 SQL 不会因为分成 6 个 jar 就消失，反而要在每个模块里开一个 `api` 包导出 DTO，文件数翻倍 |
| **构建成本对 6 人团队不划算** | 6 个子模块 = 6 份 `pom.xml` + reactor 拓扑 + 版本号同步 + `mvn -pl` 记不住模块名。每次跨域改动要同时改两个 pom |
| **包结构已经是「可拆形态」** | 目录按域隔离、只通过 `I*Service` 交互。将来真要拆模块，是机械操作（移动目录 + 加 pom + 声明依赖），不是重构。方案 ch.4.1 的「可按复杂度再拆」由此保留 |
| **一个 PR 一个目录** | 教育业务的改动几乎都落在 `backend/ruoyi-education/**`，评审范围稳定 |
| **Maven 拆分解决不了本项目最痛的问题** | 本项目最痛的是「规则语义」与「权限归属」；前者靠 `PrerequisiteGraph` 单测，后者靠 ADR-0006 的归属校验。两者都与模块数无关 |

### 依赖声明的实际结果

`backend/pom.xml`：`<dependencyManagement>` 增加 `ruoyi-education`，`<modules>` 增加
`<module>ruoyi-education</module>`。

`backend/ruoyi-admin/pom.xml`：`<dependencies>` 增加 `ruoyi-education`。

`backend/ruoyi-education/pom.xml`：只声明 `ruoyi-common` 与 `spring-boot-starter-test`。
**不声明** `ruoyi-system` / `ruoyi-framework` —— 这让「不许操作 `sys_user` 表」「不许改框架配置」
从口头约定变成编译期事实。

## 后果（Consequences）

**正面**

- 内核三个模块保持官方状态，升级时的合并冲突面接近零。
- 教育业务的全部代码在一个目录里，新成员只需要理解一个模块的六个包。
- `@PreAuthorize`、归属校验、规则计算、DTO 在同一域内相邻，评审时一眼能看完一条链路。
- 单模块测试命令简单：`mvn -pl ruoyi-education test`。

**需要承担的代价**

- **没有编译期强制**。`planning` 里的类可以 `@Autowired` 一个 `EduCourseMapper` 而编译器不会报错。
  靠三件事兜底：依赖方向矩阵（`module-boundaries.md` §5）、评审、以及 Mapper 命名带域前缀
  便于肉眼发现越界。ArchUnit 这类自动化架构测试目前没有引入（需要新增测试依赖，属后续可选项）。
- **单模块测试会一起跑**。6 个域的测试在同一个 Maven 模块里，`mvn test` 时间随域增长。
  缓解：纯规则测试（先修图、学分汇总）不启动 Spring 容器（`backend-conventions.md` §7）。
- **将来拆模块需要 ADR**。如果某个域真的需要独立部署（例如 `analytics` 的重查询要隔离），
  必须先写 ADR 说明拆分理由与迁移步骤，不允许「顺手拆一下」。
- **`common` 包有变成垃圾桶的风险**。`backend-conventions.md` §1 与 `module-boundaries.md` §7
  已明确 `common` 只放无状态纯逻辑（枚举、图算法、学分计算），放 Mapper 或 `@Transactional`
  的一律打回。

## 备选方案（Alternatives considered）

| 方案 | 为什么没选 |
|---|---|
| **6 个 Maven 子模块**（`ruoyi-edu-course` 等） | 构建拓扑与版本协调成本对 5–6 人团队过高；Maven 边界无法阻止跨域 SQL；包结构已保留将来拆分的可能 |
| **Controller 放 `ruoyi-admin`**（若依惯例） | 违反内核零修改；权限与逻辑分居两地；模块不可移除；且需要多写扫描配置才能工作 |
| **业务代码放 `ruoyi-system`** | 直接污染官方模块，升级时 `git merge` 必然冲突；`ruoyi-system` 的语义是「若依系统管理」，塞教育业务会让新人误以为是官方功能 |
| **每个域独立 Spring Boot 服务** | 相当于提前上微服务，与 ADR-0001 的决策矛盾；跨域查询会退化成 HTTP 调用，事务一致性无从谈起 |
| **按技术层分包**（`controller/`、`service/`、`mapper/` 平铺全局） | 6 个域的 Controller 会堆在同一个目录，`EduCourse*` 与 `EduStudyPlan*` 混排；改动一个业务要跨 6 个目录，评审无法按域收缩 |
| **单模块 + 单包（全塞 `com.ruoyi.education`）** | 18 张表的 Service 挤在一个包，命名冲突与误引用无法通过目录发现；失去按域评审的能力 |
