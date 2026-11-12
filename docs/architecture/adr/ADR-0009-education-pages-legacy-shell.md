# ADR-0009：教育平台页面脱离若依 Layout，改用旧版顶部导航外壳

- **状态**：Accepted
- **日期**：2026-11-13
- **相关**：ADR-0008（学生端保留旧设计）、`docs/education-portal.md`

## 背景

ADR-0008 只把 `/student/**`（学生端）换成了旧版设计，管理/教师侧的 `/education/**`
仍套在若依 `Layout` 里：左侧深色侧边栏 + 面包屑 + 标签页 + Element Plus 表格。

产品要求是**整个产品统一为旧版视觉**，不能出现"学生端一套、管理端一套"的分裂。
但 `/education/**` 的路由是由后端菜单（`sys_menu`）驱动的，`component: 'Layout'`
这一层决定了外壳，所以必须处理"换壳而不破坏菜单驱动"的问题。

## 决策

**保留菜单驱动，只替换 `/education` 这一层的容器组件。**

```ts
// frontend/src/store/modules/permission.ts
const rewriteRoutes = filterAsyncRouter(rdata, false, true)
applyLegacyShell(rewriteRoutes)   // 仅把 path === '/education' 的 component 换成 LegacyShell
```

- `frontend/src/layout/LegacyShell.vue`：旧版顶部导航（`.topnav`，深蓝 `#0070e0`）
  + `.lg-main` 内容区 + `<router-view/>`；导航项按 `userStore.permissions` 过滤，
  没有权限的入口不显示。
- `frontend/src/assets/student/education.css`：`.sp-root` 设计令牌（与旧版逐值一致）
  + `.topnav` 样式（从旧版 CSS 原样复制）+ **Element Plus 主题变量覆盖**
  （主色、圆角、表格头、卡片阴影、分页、弹窗）。
- 子路由（8 个教育页面 + 导师看板）**完全不变**，后端菜单、按钮权限、
  `loadView` 的动态组件解析、`dynamicRoutes` 全部照旧。
- 若依自带的 `system` / `monitor` / `tool` 页面仍然走 `Layout`（它们是框架功能，不是本产品界面）。

## 后果

**正向**

- 一次改动让 9 个页面同时换成旧版外壳，不需要逐页重写模板。
- 菜单与权限体系保持若依原样：新页面只要在 `sys_menu` 加一行（参考
  `backend/sql/edu/V7__edu_advisor_permission.sql`），路由与按钮权限自动生效。
- 页面内容仍用 Element Plus 组件（表格、分页、弹窗、表单），
  只靠 CSS 变量主题化，维护成本低。

**代价 / 必须记住的约束**

1. **教育平台页面没有侧边栏、面包屑、标签页了**。这是刻意的；
   代价是导航只有顶部一行文字链，页面数量继续增长时需要分组。
2. **新增教育页面必须在 `LegacyShell.vue` 的 `ALL_LINKS` 里登记**，
   否则路由能命中但没有入口（菜单驱动只解决路由，不解决导航展示）。
3. 顶部导航是**单行**的，超过 10 项会溢出；若继续增加，需要改成"分组下拉"或二级导航。
4. 学生端（`/student/**`）与教育平台（`/education/**`）**两套外壳并存**：
   学生端页面自带导航（旧版原样），教育平台用 `LegacyShell`。
   两者共享同一份设计令牌来源（`assets/student/*.css` 的 `.sp-root` 块）。
5. `LegacyShell` 没有实现若依的"锁屏""消息通知""多标签"等能力；
   需要回到这些功能时，用顶栏的「控制台」链接进 `/index`（管理员可见）。

## 备选方案

| 方案 | 为什么不选 |
|---|---|
| 只把若依侧边栏改色（保留 Layout） | 侧边栏结构与旧版顶部导航差异太大，达不到"原本的风格" |
| 把 9 个页面全部重写为旧版自绘组件 | 4600+ 行模板重写，收益仅剩"不用 Element Plus"，风险与工期不成比例 |
| 教育平台做成独立 SPA | 需要拆分构建、独立鉴权与部署，与"一个前端工程"的现状冲突 |
