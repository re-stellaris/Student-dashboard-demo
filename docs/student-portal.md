# 学生端门户（保留旧版设计）

> 决策背景见 [`adr/ADR-0008-student-portal-preserves-legacy-design.md`](architecture/adr/ADR-0008-student-portal-preserves-legacy-design.md)。
> 一句话：**学生端保留旧版 HTML 的视觉与交互（1:1），数据与鉴权换成新架构**；管理端仍是若依表格界面。

## 1. 路由

| 路由 | 组件 | 说明 | 可见角色 |
|---|---|---|---|
| `/login` | `views/login.vue` | 玻璃拟态登录页（旧设计 + 若依账号/密码/验证码） | 匿名 |
| `/student` | `views/student/dashboard/index.vue` | 学习主控台：课程浏览 + Study Headway + 选课篮 + 成绩洞察 + 能力面板 | STUDENT |
| `/student/course/:code` | `views/student/course-detail/index.vue` | 课程详情（含先修分组与评论区） | 登录即可 |
| `/student/ge/:area` | `views/student/ge-area/index.vue` | 通识教育 B/C/D 区 | 登录即可 |
| `/student/guide` | `views/student/guide/index.vue` | 使用指南 | 登录即可 |
| `/no-access` | `views/education/no-access/index.vue` | 角色不匹配提示页 | 登录即可 |

登录后按角色落地（`permission.ts`）：**STUDENT → `/student`**、TEACHER → `/education/teaching`、admin → 若依控制台首页。

## 2. 设计资产

样式从旧版 HTML 的 `<style>` 块原样抽出，只把 `:root` / `body` 换成 `.sp-root` 以避免污染管理端：

```text
frontend/src/assets/student/
├── dashboard.css        375 行   ← student_dashboard_index.html
├── login.css            197 行   ← login.html
├── course-detail.css    116 行   ← course_detail.html
├── ge.css                32 行   ← ge_area_b/c/d.html（三页样式完全一致）
├── guide.css             44 行   ← guide.html
├── Image_20260513001106_704_33.png   ← 品牌图 / 登录页 logo
├── login-logo.svg
└── favicon.svg
```

**约定**：学生端页面的根元素必须是 `<div class="sp-root">`，否则 CSS 变量与基础样式不生效。
SFC 里用 `<style scoped src="../../assets/student/<page>.css"></style>` 引用，Vue 的 scoped 会自动加上作用域属性。

## 3. 数据来源迁移

| 旧数据源 | 新来源 |
|---|---|
| `students-interface/data.js`（393 KB 内嵌课程数据） | `GET /edu/courses/list`、`GET /edu/courses/options` |
| `students-interface/competency_data.js`（2.9 MB） | `GET /edu/analytics/skill-profile`、`GET /edu/analytics/competency-pool` |
| `students-interface/intern_prep_data.js`（11 MB） | 暂缓（职位技能推荐面板未纳入本轮范围） |
| `students-interface/study_headway_{rules,engine}.js` | `GET /edu/students/me/progress`、`GET /edu/curricula/by-major` |
| 浏览器 localStorage 的选课篮 | `GET /edu/students/me/records`（导入本人成绩）+ 本地草稿 localStorage |
| 旧 `login.html`（姓名 + 邮箱，仅前端门禁） | 若依 `/login`（账号 + 密码 + 验证码 + JWT） |

## 4. 与旧版的差异（如实记录）

### 4.1 保留下来的部分

- **视觉**：CSS 令牌/配色/间距/圆角/字体栈、卡片与面板结构、`.topnav`、`.school-bar`、`.chip-sec`、`.panel`、
  `.basket`、`.ps-card`、`.score-bar-*` 等全部沿用原样式文件（未改一行内容）。
- **i18n**：EN / 简中 / 繁中 三按钮真实切换，持久化键沿用 `cuhksz_lang`。
  键数：主控台 57 × 3、课程详情 45 × 3、通识 13 × 3、指南 7 × 3（原页 39/37/13/5 的键逐字保留）。
- **交互**：选课篮拖拽 + tag 删除、课程行跳详情 + 勾选加篮、Study Headway 的入学年/专业联动、
  GPA 目标线、评分条排序、评论区（仍只存 localStorage，键名 `course_comments_<code>`）。

### 4.2 因新后端无对应数据而未搬（**功能缺口，需后续补**）

| 缺口 | 原实现 | 现状 | 补齐方式 |
|---|---|---|---|
| 实习准备面板（`#section-intern-prep`） | `intern_prep_data.js`（11 MB，仓库里已无此文件） | 仅保留标题 + 数据来源占位说明 | 需要新增实习/职位技能接口与 `edu_intern_*` 数据 |
| Skill Driven Recommendation | `COMPETENCY_DATA` 的 per-course × per-skill 分数 | 删除该卡（原页也因 `COMPETENCY_ENABLED=false` 隐藏） | 需要"课程×技能"矩阵接口 |
| Skill Matrix | 同上 | 删除该卡 | 同上 |
| U-core 细则引擎 | 独立 JS 引擎（CHI/English/PED/GE 槽位、breadth/depth/stream 判定） | 简化为按 `REQUIRED`/`ELECTIVE` + 学分汇总 | 需要在 Java 侧实现 `UcoreRuleEngine` 并暴露接口 |
| 学生手动录成绩 | 本地成绩并入 GPA 图 | 仍本地（`localStorage['cuhksz_portfolio']`），不进服务端 | 学生无 `PUT /edu/records` 权限（见 ADR-0006），如需落库要新增"自填记录"接口 |
| 课程详情四张表 | Learning Outcomes / Syllabus / Assessment Scheme / Components | 未渲染（`edu_course` 无这些字段） | 需要扩展课程大纲字段与导入管道 |

### 4.3 交互/口径上的有意调整

| 项目 | 旧版 | 现在 | 原因 |
|---|---|---|---|
| 图表 | Chart.js（CDN） | ECharts | 统一技术栈；y 轴改 0.5 等距刻度（原为非等距 4.0/3.7/3.3/3.0/2.7/2.4/2.0），x 轴两端各留 1 槽 |
| 导出 | SheetJS 双 sheet .xlsx | 单 CSV `portfolio.csv` | 不新增依赖 |
| 导入成绩单 | 解析 XLSX/PDF | 调 `listMyRecord()` 导入本人已通过课程 | 新架构成绩在服务端，无需前端解析 |
| 学期筛选 | 前端全量数据过滤 | 后端无 term 参数 → 仅对已加载页按 `offeredTerms` 文本匹配 | 需要后端补 term 过滤参数 |
| 类别/学院筛选 | 多选 | 单选 | 后端 `school`/`category` 为单值参数 |
| 课程列表 | 一次拉全量 3425 门 | 服务端分页 60/页 + 滚动加载 | 性能 |
| 技能分数 | 9+9 固定维度、0–100、红线=类别均值 | API 的 0–10 ×10，红线=当前分组可见项均值 | 后端不提供学院均值 |
| 能力池 chips | 静态 competency 池 | 来自 `getSkillProfile()` 的 category/name；明细区学生无 `edu:analytics:view` → 403 → 显示"无权限" | 权限模型（见下） |
| GE 卡片点击 | 无 syllabus 时 alert 拦截 | 总是跳详情 | 详情页现在能显示缺大纲状态 |
| 登录字段 | 姓名 + 邮箱 | 账号 + 密码 + 验证码 | 旧版是原型门禁，新架构必须真鉴权 |
| 登录页文案 | "Continue with CUHK SSO" | "登录 / Sign in" | SSO 未接入，不能误导 |
| 隐私条 | "Nothing is ever uploaded" | 区分「学业数据经后端鉴权」与「个人画像仅存本机」 | 新架构下旧文案不成立 |

### 4.4 一个需要产品决策的权限问题

`GET /edu/analytics/competency-pool` 需要 `edu:analytics:view`，而 **STUDENT 角色没有该权限**
（`backend/sql/edu/V2__edu_permissions.sql` 只授予了 TEACHER）。
结果：学生能看到技能画像 chips（`/skill-profile` 无需该权限），但点开能力池明细会 403。

建议二选一：

1. 给 STUDENT 授予 `edu:analytics:view`（放开学院能力池查询，符合"学生也能探索学院能力"的产品意图）；
2. 保持现状，前端把该区块对 STUDENT 隐藏（当前是显示"无权限"提示）。

## 5. 本地验收

```powershell
# 服务：Redis(6379) / 后端(8080) / 前端 dev(80) 均已就绪时
# 1) 学生登录后应直接落到 /student
Start-Process "http://127.0.0.1/"
# 2) 学生账号
#    stu_chen / admin123
# 3) 逐页检查
#    /student                 课程浏览、Study Headway、选课篮、成绩洞察、能力 chips
#    /student/course/CSC3100  先修 4 个 OR 组（CSC1001/CSC1003/CSC1005/AIE1001）
#    /student/ge/b            通识 B 区课程列表
#    /student/guide           使用指南
```

管理员用 `admin / admin123` 访问 `/student` 会被拦到 `/no-access`（学生端是「本人数据」页）。
