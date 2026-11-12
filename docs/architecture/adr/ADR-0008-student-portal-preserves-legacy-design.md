# ADR-0008：学生端保留旧版 HTML 设计，管理端沿用若依表格界面

- **状态**：Accepted
- **日期**：2026-11-13
- **相关**：ADR-0002（模块边界）、`docs/architecture/api-contract.md`、`versions.md`

## 背景

重构把前端从「静态 HTML + FastAPI」换成了「RuoYi-Vue3 TypeScript + Spring Boot」。
但旧版（`legacy/static-prototype-v1` 分支）里那套学生界面承载了真实的产品设计：

- `student_dashboard_index.html`：3672 行，1 个 376 行 `<style>` 块（26 个 CSS 令牌）、
  1 个 3001 行 `<script>`（i18n + 课程库 + Study Headway + 选课篮 + 成绩洞察 + 能力面板）
- `course_detail.html`、`ge_area_b/c/d.html`、`guide.html`、`login.html`（玻璃拟态登录）

如果直接把学生场景搬进若依的侧边栏 + Element Plus 表格界面，等于把产品设计丢掉。
方案文档 §3.2 本身就写了这一点：

> 由于基础版主要依托若依后台，学生端可能需要独立的导航布局和交互页面；
> 不应强行把所有学生使用场景套进管理员表格界面。

## 决策

**两套 UI 并存，按路由前缀分离，共享同一套后端与鉴权：**

| 前缀 | 使用者 | 界面来源 |
|---|---|---|
| `/student/**` | 学生（STUDENT） | **保留旧版 HTML 设计**，用 Vue 3 SFC 重建（视觉 1:1） |
| `/education/**` | 管理员 / 教师 | 若依 Element Plus 表格界面（适合课程、方案、权限等 CRUD） |

具体做法：

1. **设计资产抽取**：把各页 `<style>` 抽成 `frontend/src/assets/student/{login,dashboard,course-detail,ge,guide}.css`，
   唯一改动是把 `:root` 与 `body` 选择器改成 `.sp-root`——学生页根节点必须带 `class="sp-root"`。
   这样 CSS 变量与重置样式只作用在学生页内部，不会污染管理端。
2. **结构 1:1**：保留原 class 名、DOM 层级、配色、间距、圆角、字体栈与 i18n（EN / 简中 / 繁中）。
3. **数据换源**：`data.js`、`competency_data.js`、`intern_prep_data.js`、
   `study_headway_{rules,engine,view}.js` 全部下线，改调 `/edu/**` 的 Java 接口。
4. **鉴权换源**：旧登录页只有「姓名 + 邮箱」（原型门禁），改为若依账号 + 密码 + 验证码 + JWT；
   **玻璃拟态外观保留**，仅替换字段与文案（SSO 未接入前不能写成 "Continue with CUHK SSO"）。
5. **角色落地**：`permission.ts` 里按角色分发——STUDENT → `/student`，TEACHER → `/education/teaching`，
   admin → 若依控制台首页。角色不匹配时跳统一提示页 `/no-access`。

## 后果

**正向**

- 产品设计资产（品牌色、卡片语言、i18n、Study Headway 的呈现）完整保留，不需要重新设计。
- 学生端不引入若依侧边栏与表格，交互密度与旧版一致。
- 管理端仍享受若依的代码生成、权限、日志能力，两类使用者各取所需。
- CSS 用 `.sp-root` 隔离后，两套 UI 可共存于同一 SPA，互不干扰。

**代价 / 已知取舍**

- 学生端页面各自内联 `.topnav`，导航标记有重复（换取每页 1:1 保真，且这些页面导航项本就不同）。
- 图表库不统一：管理端用 ECharts，学生端原先用 Chart.js → 统一改用 ECharts（已在前端依赖内），
  需按原配色手调，属于视觉回归风险点。
- 导出从 `xlsx`（CDN 依赖）改为 CSV，避免新增大依赖；如需真 Excel 需再引库。
- i18n 字典从旧 JS 中抽取进 SFC，后续维护要同时改三份语言文案。
- 学生端「选课篮」原先纯本地状态，现在既要接 `planning` 接口又要保留本地草稿体验，
  存在「本地草稿 vs 服务端规划」两份状态需要人工同步，见 `docs/architecture/risks.md`。

## 备选方案

| 方案 | 为什么不选 |
|---|---|
| 直接把旧 HTML 作为静态资源托管，只把数据接口换掉 | 前端退回多页静态站，与「Vue3 + TS + Vite」的架构目标相悖，等于放弃本次重构 |
| 用 Element Plus 重画学生端 | 丢掉既有产品设计，代价是重新设计 + 用户重新学习 |
| 学生端也塞进若依 Layout | 与方案 §3.2 明确冲突，且表格界面不适合课程卡片、选课篮这类交互 |
