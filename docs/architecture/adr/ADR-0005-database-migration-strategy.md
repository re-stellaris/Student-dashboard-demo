# ADR-0005：编号式 `V1..V6` SQL 迁移，种子文件是构建产物

## 状态（Status）

已接受（Accepted）。2026-11-12。

## 背景（Context）

旧原型用一个文件建库：`course_db_api_tables.sql`（旧版，只保留在 `legacy/static-prototype-v1`
分支，见 `migration-map.md`）同时包含建表语句与演示数据，被 FastAPI 服务直接读取。
迁移到若依后出现四个新情况：

1. **表更多、模型更细**：18 张 `edu_` 表 + 若依自带 `sys_*` 表，且表之间有 23 条外键。
2. **数据量大且要重生**：课程目录 3,358 门（`data/course_catalog/sis_course_outlines_export.xlsx`），
   培养方案来自 `data/graduation_rules/`，能力关联度来自 `data/competency/`。
   这些数据会随源文件更新而重新生成，不能靠人工维护 INSERT。
3. **权限也要版本化**：角色、菜单、按钮权限（`menu_id` 2000–2080）必须能与代码一起评审。
4. **环境要可复现**：方案 ch.9.1 的验收清单要求「新成员依据 README 从零启动成功」。

方案 ch.4.2 的要求是「每张新业务表配套数据库迁移脚本」，ch.6.1 的要求是
「用迁移脚本追踪结构变更」。没有明确要求用 Flyway/Liquibase。

## 决策（Decision）

1. **编号式 SQL 迁移**，文件名 `V<n>__<用途>.sql`，放在
   `backend/sql/edu/`（若依基础表沿用官方 `backend/sql/ry_20260417.sql`、`backend/sql/quartz.sql`）。
2. **固定执行顺序**，记录在 [`versions.md`](../../../versions.md) §4：

   ```text
   backend/sql/ry_20260417.sql                 若依基础表 sys_*
   backend/sql/quartz.sql                      定时任务表（未启用可跳过）
   backend/sql/edu/V1__edu_schema.sql          教育库表结构（DDL）
   backend/sql/edu/V2__edu_permissions.sql     部门/角色/演示账号/菜单/按钮权限
   backend/sql/edu/V3__edu_seed_course.sql     课程目录与先修关系（生成）
   backend/sql/edu/V4__edu_seed_curriculum.sql 专业与培养方案（生成）
   backend/sql/edu/V5__edu_seed_student.sql    学期、开课、演示学生与成绩（生成）
   backend/sql/edu/V6__edu_seed_competency.sql 技能标签与课程关联度（生成）
   ```

3. **`V1`/`V2` 是面向空库的一次性脚本，不要求幂等**：
   - `V1` 每条 `create table` 前有 `drop table if exists`，用于从零重建；开头结尾包
     `SET FOREIGN_KEY_CHECKS = 0/1` 以解决删表顺序问题。
   - `V2` 用固定主键插入（`sys_dept` 110/111、`sys_role` 3/4、`sys_user` 101–108 与 201、
     `sys_menu` 2000–2080），**重复执行会因主键冲突失败**。
   - 这条限制必须写进部署手册：`V1`/`V2` 不得在生产环境重跑。
4. **`V3..V6` 是生成式种子，必须幂等**。由 `pipeline/course_import/build_edu_seed.py`
   从 `data/` 下的源文件生成（`versions.md` §4），**禁止手工编辑**。幂等规则：
   - 以业务唯一键为锚点做 `insert ... on duplicate key update`，例如
     `edu_course.uk_course_code`、`edu_term.uk_term_code`、`edu_skill_tag.uk_skill_tag_code`、
     `edu_course_prereq.uk_course_prereq`、`edu_course_competency.uk_course_competency`；
   - **不硬编码自增主键**（`edu_course` 从 100000 起、其余从 1000 起由 `auto_increment` 决定），
     表间引用通过业务键子查询解析；
   - 同一份 `data/` 重复生成必须得到**逐字节相同**的 SQL（生成器内不得有时间戳、随机数、
     无序迭代）；
   - 生成器可整体重跑，`git diff` 为空即视为合格；CI 里用「重跑后 diff」做校验。
5. **外键与 CHECK 一律写进 DDL**，不用应用层代替：
   - 23 条外键，删除策略按「历史事实保行、关系与派生清理」划分，逐条理由见
     [`er-diagram.md`](../er-diagram.md) §5.1；
   - 2 条 CHECK（`ck_prereq_not_self`、`ck_record_units`）依赖 MySQL 8.0.16+ 的真正强制执行；
   - 只在 `V1` 的建表段落关闭外键检查，**种子脚本不得关闭**（若确因加载顺序必须关闭，
     必须在同一文件末尾 `SET FOREIGN_KEY_CHECKS = 1` 并附一致性校验语句）。
6. **`V1..V6` 之外不新增「顺手改一下」的脚本**。任何结构变更都是一个新的 `V7` 并更新
   `versions.md` §4 与本文；不允许在已有 `V<n>` 上原地改字段（生产环境已执行过，改了不会生效）。
7. **旧的 `course_db_api_tables.sql` 退役**：从当前分支删除，内容保留在
   `legacy/static-prototype-v1` 分支（见 `migration-map.md` §1）。

### 为什么不用 Flyway / Liquibase

| 理由 | 说明 |
|---|---|
| **违反内核零修改** | 引入 Flyway 需要往 `ruoyi-admin` 的 `application.yml` 加配置、加 starter 依赖，属于对官方模块的实质性改动（`module-boundaries.md` §3 只允许 pom 里的一个 dependency） |
| **执行者是人不只是 CI** | 本项目没有独立 DBA，也没有「一个命令部署到多环境」的流水线；迁移由开发/部署同学按 `versions.md` 的顺序手工执行。版本表带来的「自动比对」收益在这里用不上 |
| **幂等性可以靠约定达成** | 生成式种子的幂等由生成器保证（upsert + 稳定排序），比 Flyway 的 checksum 更贴合「源数据变了就重生」的工作方式 |
| **回滚本来就不靠工具** | 教育库的回滚手段是「重建空库 + 重跑 V1..V6」，而不是逐条 undo 脚本。Flyway 的 undo 能力在这里没有价值 |

### 为什么旧的 `course_db_api_tables.sql` 必须退役

| 问题 | 具体表现 |
|---|---|
| 结构与数据混在一个文件 | 表定义与演示记录写在一起，无法判断哪部分是「必须」、哪部分是「样例」 |
| 字段与 FastAPI 强绑定 | 表结构按 `api_server.py` 的 `SELECT` 语句设计（如单一的 `courses` 表），无法承载培养方案、选课、规划等 18 张表的关系 |
| 没有版本序列 | 文件名不带序号，改动无历史；两人同时改会互相覆盖且无法判断谁先 |
| 没有权限模型 | 角色、菜单、按钮权限在若依里是数据（`sys_role` / `sys_menu`），旧文件完全不涉及 |
| 无 `edu_` 前缀 | 与若依的 `sys_*` 命名体系并存时容易混淆，也不符合方案 ch.6 的「教育业务采用 `edu_` 前缀与系统表区分」 |
| 不可重放 | 演示记录含明文且不可幂等重跑，删除后无恢复路径（旧版本只能回退到 `legacy/static-prototype-v1` 分支） |

## 后果（Consequences）

**正面**

- 结构变更的历史就是文件名序列，任何人 `ls backend/sql/edu` 就能看懂演进顺序。
- 种子数据可重生成：改 `data/` 下的源文件 → 重跑生成器 → 得到新的 `V3..V6`，
  人工只审 diff，不手写 SQL。
- 外键与 CHECK 在数据库层兜底，应用层漏校验时不会静默产生脏数据。
- 从零启动的路径唯一且可复现，符合方案 ch.9.1 的验收要求。

**需要承担的代价**

- **没有迁移记录表**。数据库里没有 `schema_migration` 之类的行来证明「哪些脚本执行过」；
  环境间是否一致只能靠人工核对 + 部署清单。**这是本决策最大的债**，
  风险登记在 [`risks.md`](../risks.md)「数据库迁移无版本表」一行。
- **`V1`/`V2` 重跑即破坏数据**。任何人在已有数据的环境执行 `V1` 会丢失 `edu_*` 全部数据，
  执行 `V2` 会因主键冲突中断（部分成功）。需要在文件头注释与部署手册中双重警示。
- **`V3..V6` 已生成，体积可观**：`V3` 约 3.6 MB、`V6` 约 750 KB，由
  `pipeline/course_import/build_edu_seed.py` 从 `data/` 产出，并附带
  `pipeline/course_import/seed-report.json` 记录行数与来源。生成器可重跑，
  但**重跑一致性必须靠 CI 校验**（人眼无法在 3.6 MB 的 diff 里发现一行漂移）。
- **生成物 diff 噪音大**：课程数据规模上千行，每次源数据更新会产生巨大的 SQL diff。
  评审策略是「看行数变化与抽样」，并在 CI 里校验生成器可重跑一致。
- **多环境必须各自重建**：没有增量补丁，测试库与生产库的数据来源必须一致
  （都用 `data/` + 生成器），不允许「生产手工补几条」。

## 备选方案（Alternatives considered）

| 方案 | 为什么没选 |
|---|---|
| **Flyway** | 需要改 `ruoyi-admin` 的依赖与配置（违反内核零修改）；本项目没有多环境自动部署流水线，版本表的收益用不上；生成式种子与 checksum 机制契合度差 |
| **Liquibase** | 同上，且 changelog 的 XML/YAML 格式对「生成上千行课程数据」不友好 |
| **MyBatis / JPA 自动 DDL** | 无法表达外键删除策略、CHECK、`utf8mb4` 字符集与索引名；生成的 DDL 不可评审 |
| **单一 `init.sql`**（旧做法） | 无法区分结构变更与数据变更；无版本序列；正是本项目要摆脱的状态 |
| **`mysqldump` 快照** | 适合备份，不适合作为「可评审的结构定义」：diff 不可读，且会把演示数据与结构永久绑定 |
| **把种子数据改成运行时导入接口** | 需要一套导入 API + 文件上传 + 权限，且导入结果不可评审、不可回滚；数据负责人无法在 PR 里审 diff |
| **在已有 `V1` 上原地加字段** | 已经执行过的环境不会重新执行 `V1`，改了等于没改；必须新增 `V7` |
