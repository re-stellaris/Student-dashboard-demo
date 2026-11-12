# 教育平台（管理 / 教师侧）

> 面向 `/education/**` 的 9 个页面。学生端见 [`student-portal.md`](student-portal.md)。
> 决策背景见 [`architecture/adr/ADR-0009-education-pages-legacy-shell.md`](architecture/adr/ADR-0009-education-pages-legacy-shell.md)。

## 1. 外壳与主题

| 组成 | 位置 | 作用 |
|---|---|---|
| 外壳组件 | `frontend/src/layout/LegacyShell.vue` | 旧版深蓝顶栏 + `.lg-main` 内容区 + `<router-view/>`，替代若依 `Layout` |
| 路由接管 | `frontend/src/store/modules/permission.ts` → `applyLegacyShell()` | 只把 `path === '/education'` 的容器组件换成 `LegacyShell`，子路由与菜单不动 |
| 主题 | `frontend/src/assets/student/education.css` | `.sp-root` 令牌 + `.topnav` 原版样式 + Element Plus 主题变量（主色/圆角/表格头/卡片） |
| 页面标题区 | 各页面 `<div class="lg-head"><h2>…</h2><p>…</p></div>` | 原版卡片标题的排版 |

外壳的导航项在 `LegacyShell.vue` 的 `ALL_LINKS` 里声明，**按权限过滤**：

| 路径 | 标题 | 权限 |
|---|---|---|
| `/education/dashboard` | 学习仪表盘 | `edu:dashboard:view` |
| `/education/course` | 课程中心 | `edu:course:list` |
| `/education/curriculum` | 培养方案 | `edu:curriculum:list` |
| `/education/planner` | 选课规划 | `edu:plan:list` |
| `/education/records` | 我的成绩 | `edu:record:list` |
| `/education/teaching` | 教师授课 | `edu:offering:list` |
| `/education/skill` | 技能画像 | `edu:skill:view` |
| `/education/analytics` | 学情分析 | `edu:analytics:view` |
| `/education/advisor` | 导师看板 | `edu:advisor:view` |

## 2. 新增一个教育平台页面的完整步骤

1. **建页面**：`frontend/src/views/education/<name>/index.vue`，根元素 `<div class="app-container">`，
   顶部放 `.lg-head`，不要自己写导航（外壳已有）。
2. **登记菜单**（决定路由存在与否）：新增迁移 `backend/sql/edu/V8__xxx.sql`，往 `sys_menu`
   插一行 `menu_type='C'`、`parent_id=2000`、`component='education/<name>/index'`、`perms='edu:<域>:<动作>'`，
   再按角色插 `sys_role_menu`。参考 `V7__edu_advisor_permission.sql`。
3. **登记导航**（决定有没有入口）：在 `LegacyShell.vue` 的 `ALL_LINKS` 里加一行，`permission` 与上一步一致。
   漏了这步的表现是：**能通过 URL 打开，但顶栏没有链接**。
4. **权限**：接口 `@PreAuthorize("@ss.hasPermi('edu:<域>:<动作>')")`，前端按钮 `v-hasPermi`。

## 3. 与旧版的关系

- 顶栏、配色、圆角、字体、卡片阴影：来自旧版 CSS，逐值一致。
- 页面内容仍用 Element Plus 的 `el-table` / `el-pagination` / `el-dialog` / `el-form`，
  只通过 CSS 变量主题化成旧版观感 —— **不是**把表格重画成旧版组件。
  这样业务功能（分页、表单校验、弹窗）不用重写，风险最低。
- 旧版没有管理端界面，所以这些页面的**信息架构**是新设计的（旧版只有学生端 + 导师看板）。

## 4. 布局约束（写页面时注意）

- 没有侧边栏后，页面可用宽度从 `calc(100% - 210px)` 变成容器宽度（`.lg-main` 最大 1500px）。
  若某页面写死了 `height: calc(100vh - 50px)` 这类值，需要自行调整。
- 顶栏是 `position: sticky; top: 0; height: 52px`，页面内不要再用 `position: fixed` 的头部。
- 顶栏是**单行**的，目前 9 项已接近上限；继续增加请改成分组下拉。

## 5. 本地验收

```powershell
# 服务已起（见 scripts/local-start.ps1）
# admin / admin123  -> 顶栏应有：学习仪表盘 课程中心 培养方案 选课规划 我的成绩 教师授课 技能画像 学情分析 导师看板 + 控制台
# teacher_li / admin123 -> 顶栏应只有：学习仪表盘 课程中心 培养方案 教师授课 导师看板 学情分析（无"控制台"）
```

顶栏出现"控制台"链接说明当前账号是 admin（可回到若依自带的 system/monitor/tool 页面）。
