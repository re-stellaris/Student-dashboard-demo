[Normal] EDUCATION PLATFORM  ·  TECHNICAL PROPOSAL
[Title] 教育平台基于 RuoYi-Vue 的
技术架构与实施方案
[Subtitle] 面向大学生开发团队的 Java 前后端分离建设方案

=== TABLE 1 ===
核心决策 / 选择官方 RuoYi-Vue 单体后端 + 官方 Vue 3 前端；复用权限、菜单、日志与代码生成能力；新增教育业务模块；不在 MVP 阶段引入 Spring Cloud 微服务。
=== END TABLE ===


=== TABLE 2 ===
项目 | 内容
文档版本 | V1.0（讨论稿）
编制日期 | 2026 年 11 月 12 日
适用阶段 | MVP 至首期正式上线
团队假设 | 约 5—6 名大学生，允许兼岗
目标用户 | 学生、教师、平台管理员
=== END TABLE ===

[SmallNote] 方案依据：用户提供《平台人才需求》及此前《教育平台系统框架设计方案》；若依技术信息参考官方文档与官方代码仓库。
[Heading 1] 一、执行摘要
[Normal] 本项目定位为高校/教育机构的教学与学习规划平台。除常见的课程、作业和学生管理外，重点打造学习仪表盘、课程先修约束检查、学分规划、技能画像，以及可在获得授权后对接学校统一登录（SSO）的能力。

=== TABLE 3 ===
一句话架构 / 以 RuoYi 负责“后台底座”，以自建教育业务模块负责“平台特色”：Spring Boot + Spring Security + MyBatis + MySQL + Redis；前端采用 Vue 3 + Element Plus + Vite，并优先使用官方 TypeScript 版本。
=== END TABLE ===

[Heading 2] 1.1 决策边界

=== TABLE 4 ===
事项 | 本方案采用 | 不建议在第一版采用
后端形态 | RuoYi-Vue 多模块单体 | RuoYi-Cloud / 全量微服务
数据库 | MySQL 8.x，沿用若依原有体系 | 未经验证直接替换成 PostgreSQL
教育推荐 | 先修关系 + 学分规则 + 冲突检查 | 直接部署复杂 AI 推荐模型
统一登录 | 保留兼容接口，获得校方授权后接入 | 未经授权模拟学校 SSO
个人隐私 | 服务端共享业务数据 + 本地个人画像 | 将所有个人画像明文上传
=== END TABLE ===

[Heading 2] 1.2 MVP 验收目标
[List Bullet] 学生、教师、管理员可通过不同角色访问对应页面；越权请求受到阻止。
[List Bullet] 课程目录、培养方案、课程先修关系、学分统计能够完成完整演示。
[List Bullet] 学生可创建学期课程计划，系统能显示推荐理由、缺少的先修课和学分情况。
[List Bullet] 个人技能画像能在浏览器本地保存、展示并导出备份。
[List Bullet] 项目具备可复现的本地启动过程、基础自动化测试、在线演示环境及数据备份。
[Heading 1] 二、框架选型与版本策略
[Heading 2] 2.1 若依家族的选择

=== TABLE 5 ===
方案 | 优点 | 适配判断
RuoYi-Vue（选用） | Spring Boot 单体、多模块、权限管理和代码生成齐全 | 最适合当前人数和教育业务开发
RuoYi-Cloud | 微服务基础设施及分布式组件更完整 | 早期成本高，暂不采用
RuoYi-Vue-Plus | 功能丰富、技术栈不同于原版 | 属于另一套生态，首期不混用
=== END TABLE ===

[Normal] 重要说明：RuoYi-Vue3 是前端仓库/前端形态，后端仍应搭配官方 RuoYi-Vue 对应分支；不要误把 RuoYi-Vue3 当作完整的独立 Java 后端。
[Heading 2] 2.2 推荐技术组合

=== TABLE 6 ===
层次 | 选型 | 说明
后端基座 | 官方 RuoYi-Vue；优先稳定的 Spring Boot 4.x 分支 | 保留若依既有 Spring Security、JWT、MyBatis 架构
Java | JDK 21（须与选定若依提交兼容） | 统一开发、测试与服务器运行时
前端 | 官方 RuoYi-Vue3-TypeScript；Vue 3 / Vite / Element Plus | 前端同学若暂不熟悉 TS，可使用 Vue 3 JS 版作过渡
数据库 | MySQL 8.x | 最大限度复用若依已有建库脚本和代码生成器
缓存 | Redis | 若依 Token/缓存等现有功能依赖，部署时保留
可视化 | ECharts | 学分、进度、成绩趋势、技能雷达图
本地存储 | IndexedDB + Dexie.js | 个人技能画像、私人笔记及导出恢复
部署 | Nginx + Docker Compose + Linux | 中小规模部署，不引入 Kubernetes
团队工具 | Git、GitHub/Gitee、Apifox、JUnit、Postman | 代码审查、API 测试、自动化检验
=== END TABLE ===

[SmallNote] 版本锁定原则：先下载官方后端与前端的一个已发布版本或明确提交号，运行通过后将 Git SHA、JDK、Node、MySQL、Redis 的实际版本写入 versions.md 与锁文件；不要直接跟随 master 自动升级。
[Heading 1] 三、平台总体架构
[Heading 2] 3.1 逻辑分层

=== TABLE 7 ===
层次 | 承担的功能 | 实现方式
访问层 | 学生端 / 教师端 / 管理员端 | Vue 3 + Router + Pinia + Element Plus
网关/静态资源 | HTTPS、前端静态文件、反向代理 | Nginx；同域 API 转发
若依基础能力 | 用户、角色、菜单、权限、日志、字典、代码生成 | RuoYi 既有模块
教育业务能力 | 课程、培养方案、选课、作业、规划、学情 | 新建教育模块，与若依底座隔离
持久化 | 用户/课程/作业/学分等共享数据 | MySQL；Redis 保存框架缓存
本地优先数据 | 个人技能画像、私人笔记 | IndexedDB；跨设备同步留到后期
=== END TABLE ===

[Heading 2] 3.2 数据流与边界
[List Number] 前端登录后通过若依原有权限机制取得允许访问的菜单和接口权限。
[List Number] 前端访问教育业务 API；Controller 校验参数，Service 执行课程规则，Mapper 查询 MySQL。
[List Number] 课程规划服务从培养方案、课程表和先修依赖中计算推荐结果，返回可解释的规则说明。
[List Number] 私人技能画像默认仅写入浏览器 IndexedDB；若需跨设备同步，先设计密钥恢复与加密方案再开放。
[Normal] 由于基础版主要依托若依后台，学生端可能需要独立的导航布局和交互页面；不应强行把所有学生使用场景套进管理员表格界面。
[Heading 1] 四、若依复用与二次开发清单

=== TABLE 8 ===
能力 | 处理策略 | 注意事项
用户、角色、菜单、按钮权限 | 直接复用并配置 STUDENT / TEACHER / ADMIN 角色 | 新建业务权限标识，避免只做前端隐藏
部门与数据范围 | 按校区/院系试点复用；必要时扩展课程组织模型 | 部门不是天然的选课关系，不能代替学生授权
字典、通知公告、操作日志 | 直接复用，适配教育领域分类 | 敏感日志不得记录个人笔记明文或完整 Token
代码生成器 | 用于管理端标准 CRUD 页面和 Mapper | 复杂规划规则及权限过滤仍需手写
定时任务 | 仅在任务提醒等场景确认需要时启用 | 暂时不需要可停用可选模块
文件上传 | 依据课件/作业附件需求配置 | 校验文件类型、大小、权限和下载地址
SSO 接入 | 新增外部身份映射层；不替换掉若依所有权限功能 | 协议与校方提供的 OIDC/CAS 能力匹配
=== END TABLE ===

[Heading 2] 4.1 推荐目录组织
[Normal] ruoyi-admin       # 启动与外部 API
ruoyi-framework   # 若依底层安全与框架配置（尽量少改）
ruoyi-system      # 原有用户、角色、菜单、数据权限
ruoyi-common      # 共享基础工具
ruoyi-education   # 新增教育业务模块
  ├─ course       # 课程与先修关系
  ├─ curriculum   # 专业培养方案、毕业学分
  ├─ enrollment   # 选课与计划
  ├─ assignment   # 作业与成绩
  ├─ planning     # 规则推荐与冲突检查
  └─ analytics    # 学情统计
ruoyi-ui-vue3-ts  # 独立前端项目
[SmallNote] 说明：这是建议的二次开发组织方式，不代表官方仓库已自带 education 模块；可按业务复杂度再拆为多个 Maven 子模块。
[Heading 2] 4.2 代码改造规范
[List Bullet] 优先通过新增业务包/模块实现功能，不直接修改若依核心权限源码；必要修改保留原因和测试。
[List Bullet] 每个业务模块采用 Controller → Service → Mapper 的清晰层次，统一 DTO、参数校验、异常处理和事务。
[List Bullet] 每张新业务表配套数据库迁移脚本；代码生成后的 SQL 和权限查询仍需人工审核。
[List Bullet] 前后端接口以 OpenAPI/Apifox 记录，统一分页、错误码、时间格式和鉴权规则。
[Heading 1] 五、业务功能与建设优先级

=== TABLE 9 ===
模块 | MVP（必须） | 后续版本
账号与角色 | 测试账号、角色与数据访问限制 | 学校授权 SSO、账号映射与审计
课程中心 | 课程列表、学分、先修关系、课程查询 | 课程内容、章节、资料库
培养方案 | 专业、培养方案、必修/选修学分要求 | 多版本培养方案与规则变更
选课规划 | 手动选择课程、检查先修/学分冲突、推荐理由 | 时间冲突优化、智能路径规划
学习仪表盘 | 学分进度、任务数、成绩趋势的演示数据 | 更多维度学情统计与提醒
技能画像 | 自评标签、雷达图、本地存储与备份 | 加密同步及可解释画像生成
教师端 | 基础课程管理、作业发布、成绩录入 | 批改流程、资源库、班级分析
管理端 | 课程/专业/用户维护和基础审计 | 课程审核、系统监控、权限策略
=== END TABLE ===

[Heading 2] 5.1 课程推荐的规则设计
[Normal] 先不引入机器学习，采用透明规则：①读取学生已完成课程；②检查课程先修依赖图；③筛选当前培养方案中的未修课程；④判断学分上限、课程开设学期和时间冲突；⑤为每条推荐生成可解释说明。
[Normal] 示例：推荐“数据结构”，原因是该课程属于专业必修且已通过先修课程；不推荐“算法设计”，原因是仍缺少先修课程“数据结构”。所有依赖关系及学分规则应可由管理员配置，避免写死在 Java 代码中。
[Heading 1] 六、核心数据库设计（首版草案）
[Normal] 保留若依原有 sys_user、sys_role、sys_menu、sys_user_role 等管理表；教育业务采用 edu_ 前缀与系统表区分。以下为逻辑表设计，具体字段、约束与索引在 ER 图评审后确认。

=== TABLE 10 ===
表名 | 主要字段示例 | 作用
edu_student | id, sys_user_id, student_no, major_id, entry_year | 学籍侧资料映射；不以姓名作为唯一键
edu_teacher | id, sys_user_id, teacher_no, dept_id | 教师身份与院系映射
edu_course | id, code, title, credits, category | 课程目录
edu_course_prereq | course_id, prerequisite_course_id | 先修有向边；检测环路
edu_curriculum | id, major_id, version, total_credits | 培养方案及版本
edu_curriculum_item | curriculum_id, course_id, requirement_type | 培养方案课程要求
edu_course_offering | id, course_id, term_id, teacher_id, capacity | 学期开课信息
edu_enrollment | student_id, offering_id, status | 实际选课关系
edu_course_record | student_id, course_id, grade, passed | 已修课程与成绩记录
edu_assignment | id, offering_id, title, due_at | 作业任务
edu_submission | assignment_id, student_id, submitted_at, grade | 学生提交与评分
edu_study_plan | id, student_id, term_id, status | 学生规划主表
edu_study_plan_item | plan_id, course_id, reason, validation_status | 规划项及规则结果
edu_skill_tag | id, code, name, category | 公共技能标签
edu_sso_identity | sys_user_id, provider, external_subject | 学校身份绑定：后期启用
=== END TABLE ===

[Heading 2] 6.1 设计约束
[List Bullet] 敏感用户标识需要加密/脱敏；正式成绩和学籍信息只有获得合法数据来源及授权后才能接入。
[List Bullet] 学生查阅成绩时，后端必须绑定当前登录用户到 edu_student；不能信任前端传来的 student_id。
[List Bullet] 先修表添加“课程不能先修自身”、外键及环检测；培养方案按版本保存，避免历史规则被覆盖。
[List Bullet] 为用户关联、课程代码、学期、选课状态等常用查询建立必要索引；用迁移脚本追踪结构变更。
[List Bullet] 用户私人技能画像默认不存在 edu_user_profile 明文表；本地数据与学校正式成绩应严格区分。
[Heading 1] 七、接口与安全设计
[Heading 2] 7.1 API 示例（开发前确认）

=== TABLE 11 ===
HTTP 接口 | 使用场景 | 访问控制
GET /api/edu/courses | 分页、筛选课程 | 登录用户；公开课程可另设计公共接口
GET /api/edu/curricula/{id} | 读取培养方案 | 有权限的学生/教师/管理者
POST /api/edu/plans/validate | 校验选课方案与先修条件 | 当前学生本人
GET /api/edu/students/me/progress | 读取本人已修学分 | 当前学生本人
POST /api/edu/assignments | 教师新建作业 | 有授课资格的教师
POST /api/edu/submissions | 提交本人作业 | 课程内已授权学生
GET /api/edu/teachers/me/classes | 教师所授课程 | 当前教师本人
=== END TABLE ===

[SmallNote] 路径是建议的接口契约，非若依官方现成接口。可延续若依登录/鉴权规范与统一响应结构，但应避免只依赖角色判断：教师还必须属于该课程，学生还必须属于该选课关系。
[Heading 2] 7.2 SSO 接入策略
[List Number] 先使用若依内部账号完成业务开发，明确“外部学校身份”与“平台用户”映射模型。
[List Number] 确认校方支持的 OIDC/OAuth2 或 CAS 协议、测试环境、回调地址、可访问的用户字段和授权范围。
[List Number] 通过外部 subject / 学校标识建立唯一绑定关系；首次登录按批准的规则开通角色。
[List Number] 禁止直接信任前端传来的角色；处理登录失败、账户停用、登出和身份解绑流程。
[Heading 2] 7.3 隐私与安全基线
[List Bullet] 全站 HTTPS；生产环境关闭默认示例账号，修改初始密码及密钥，将环境变量与秘密配置分离。
[List Bullet] 服务端严格做资源级授权；若依菜单权限仅是入口权限，不能替代学生/教师数据行级访问控制。
[List Bullet] 限制上传文件类型与大小，避免任意路径访问；记录关键数据变更的审计日志，屏蔽密码/Token 等敏感字段。
[List Bullet] 个人画像通过 Dexie.js 存储于 IndexedDB，支持导出与恢复；提示浏览器清理数据风险。
[List Bullet] 跨设备“端到端加密同步”必须先评审密钥生成、丢失恢复、冲突合并和威胁模型，再单独上线。
[List Bullet] 生产数据库执行自动备份与恢复演练；不同环境严格隔离，使用脱敏测试数据。
[Heading 1] 八、团队配置与协作机制
[Normal] 基于现有“可由大学生兼岗”的招聘方向，建议约 5—6 人；人数是资源规划假设，不是招聘硬性要求。

=== TABLE 12 ===
角色 | 建议投入 | 具体交付物
技术负责人 / Java 后端 A | 1 人 | 若依搭建、模块边界、认证权限、安全审查、代码合并
Java 后端 B | 1 人 | 课程/培养方案/先修、作业、数据库与接口测试
Vue 前端 A | 1 人 | 学生端、课程规划器、Dexie、ECharts
Vue 前端 B | 1 人 | 教师/管理页面、接口联调、响应式适配
产品/课程数据 | 1 人 | 需求优先级、课程数据、规则样例、功能验收
UI/测试/部署（可兼任） | 0—1 人 | Figma 原型、测试用例、Docker/Nginx、上线检查
=== END TABLE ===

[Heading 2] 8.1 技能调整

=== TABLE 13 ===
方向 | 招聘时必会 | 项目内补充
Java 后端 | Java、Spring Boot、SQL、HTTP API | 若依结构、Spring Security、MyBatis、JUnit、权限过滤
Vue 前端 | Vue 3、JavaScript、API 对接 | TypeScript、Pinia、Element Plus、ECharts、Dexie.js
运维/协作 | Git、基础 Linux 命令 | Docker Compose、Nginx、HTTPS、日志与恢复
产品/数据 | Excel、课程规则梳理 | 数据字典、先修约束建模、验收用例
=== END TABLE ===

[Heading 2] 8.2 研发规则
[List Bullet] Git 主干保持可运行，功能在短分支开发并通过 Pull Request 合并。
[List Bullet] 每个任务必须有需求说明、接口契约、测试点、负责人及完成条件。
[List Bullet] 每周至少一次联调演示；发现数据模型或接口变更先沟通再改。
[List Bullet] 提交代码时同步数据库迁移和 API 文档，避免“只在自己电脑能运行”。
[Heading 1] 九、分阶段实施计划与里程碑
[Normal] 下表以团队每周可持续投入为前提给出参考周期，实际进度由课程资料准备、人员熟练度和校方接口审批决定。

=== TABLE 14 ===
阶段/周期（建议） | 关键任务 | 可验收结果
第 1–2 周：项目底座 | 锁版本；搭建若依前后端、MySQL/Redis；定义角色；初始化 CI 与数据库迁移 | 团队成员可一键启动、管理员可登录
第 3–4 周：课程业务 | 建课程/专业/培养方案/先修表；导入演示数据；课程 CRUD 与查询 | 课程目录及先修规则可管理、可检索
第 5–6 周：学生 MVP | 学分统计、学习仪表盘、个人画像 IndexedDB、基础规划校验 | 完整演示学生查看—规划—保存流程
第 7–8 周：教师与权限 | 教师课程管理、作业成绩、课程级数据隔离、接口与 UI 联调 | 教师/学生各自只能访问授权数据
第 9–10 周：稳定化 | 综合测试、HTTPS 部署、备份恢复演练、文档与演示 | 首版上线候选版本与验收报告
独立后续阶段 | 学校 SSO、跨设备同步、智能推荐、复杂学情分析 | 获得对应权限/数据/安全评审后分批启动
=== END TABLE ===

[Heading 2] 9.1 上线前验收清单
[List Bullet] 基础：新成员依据 README 从零启动成功；开发、测试、生产配置分离。
[List Bullet] 功能：管理员/教师/学生核心流程完整，至少包含一套专业课程数据与先修示例。
[List Bullet] 安全：未登录、跨学生访问、跨班级访问、非法文件、异常参数等测试通过。
[List Bullet] 数据：课程记录可追溯，数据库可恢复；本地画像可导出导入且清空浏览器风险有提示。
[List Bullet] 交付：源代码、ER 图、API 文档、部署手册、测试报告与需求变更清单齐全。
[Heading 1] 十、主要风险与应对

=== TABLE 15 ===
风险 | 可能影响 | 应对措施
直接改若依内核 | 升级困难，合并冲突 | 优先新增 ruoyi-education；修改原模块须有记录
版本不一致 | 后端启动失败、接口或前端依赖冲突 | 锁定可复现版本矩阵，先完成 PoC 再开发
学生与教师数据越权 | 泄露成绩或作业 | 以登录身份绑定资源所有权，权限测试自动化
课程数据不准确 | 推荐结果不可信 | 设置课程数据负责人、规则样例与人工验收
本地画像丢失 | 用户学习记录消失 | 导出/恢复、清晰风险提示、后期评估加密同步
校方 SSO 接入受限 | 无法按期使用学校账号登录 | 正式承诺前获得授权；MVP 使用内部账号
大学生时间碎片化 | 联调拖延、成员流失 | 缩小 MVP、明确模块负责人、每周可运行演示
源码许可证与第三方依赖 | 合规/商用风险 | 保留原项目版权与许可证，逐项核查依赖许可证
=== END TABLE ===

[Heading 1] 十一、启动前必须确认的决定
[Normal] 建议由产品负责人和技术负责人在第一周形成书面决策记录，避免后续频繁返工。
[List Number] 业务边界：第一版是否包含真实选课和正式成绩？如果无法取得校方数据，就明确使用演示/自填数据。
[List Number] 学校范围：先面向单校单院系，还是多学校？不同选择会影响权限与培养方案模型。
[List Number] 版本矩阵：若依后端分支、Vue3 TypeScript 前端分支、JDK/Node/MySQL/Redis 具体版本和提交号。
[List Number] 数据策略：哪些信息为公共课程数据、哪些为学校正式记录、哪些只保存在浏览器。
[List Number] 交付定义：上线环境、课程样例规模、所需测试覆盖程度，以及谁具备最终验收权限。

=== TABLE 16 ===
建议立即执行的技术验证（PoC） / 在正式分工前用一套最小可运行项目验证：若依后端 Spring Boot 4 分支 + 官方 Vue 3/TS 前端 + MySQL + Redis 的登录、权限、代码生成、1 张自定义业务表及 Docker 部署。PoC 通过后，再锁定版本并启动大规模开发。
=== END TABLE ===

[Heading 1] 附录 A：官方资料与依据
[Normal] 官方资料（查阅日期：2026-11-12）：
[List Bullet] 若依官方 RuoYi-Vue 文档：https://doc.ruoyi.vip/ruoyi-vue/
[List Bullet] RuoYi-Vue 官方 Java 后端仓库：https://github.com/yangzongzhuan/RuoYi-Vue
[List Bullet] RuoYi-Vue3 官方前端仓库：https://github.com/yangzongzhuan/RuoYi-Vue3
[List Bullet] 若依官方项目结构说明：https://doc.ruoyi.vip/ruoyi-vue/document/xmjs.html
[List Bullet] 若依官方部署说明：https://doc.ruoyi.vip/ruoyi-vue/document/hjbs.html
[SmallNote] 需求依据：用户提供《平台人才需求.docx》；此前《教育平台系统框架设计方案.docx》。上述教育业务模块划分、数据库表名、API 路径、人员分工和参考周期均为本方案建议，并非若依官方自带功能或承诺。