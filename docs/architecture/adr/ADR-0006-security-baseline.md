# ADR-0006：资源级授权为唯一授权真相，身份只从 JWT 取

## 状态（Status）

已接受（Accepted）。2026-11-12。SSO 与外部身份对接**延后**，先建映射层。

## 背景（Context）

若依自带两级权限：菜单/按钮权限（`sys_menu.perms` + `@PreAuthorize("@ss.hasPermi('...')")`）
与部门数据范围（`@DataScope` + `sys_role.data_scope`）。这两级都不足以保护本平台的数据：

| 若依能力 | 能控制什么 | 在本平台上为什么不够 |
|---|---|---|
| 菜单/按钮权限 | **入口**：谁能看到哪个菜单、哪个按钮 | 学生 A 与学生 B 的 `perms` 完全相同（都是 `edu:record:list`）。菜单权限无法区分「A 只能看 A 的成绩」 |
| 部门数据范围 | 按 `sys_dept` 的组织树过滤 | 选课关系不是组织关系。同一个学生跨院系选课、同一个教师给外院上课，部门树给不出正确答案；`V2` 里 STUDENT 的 `data_scope=5`（仅本人）、TEACHER 的 `data_scope=4`（本部门及以下）只是若依的默认值，教育表不使用 `@DataScope`（`backend-conventions.md` §8） |

同时存在几处默认配置带来的现实风险（均为仓库现状）：

| 位置 | 现状 | 风险 |
|---|---|---|
| `backend/ruoyi-admin/src/main/resources/application.yml` | `token.secret: abcdefghijklmnopqrstuvwxyz` | 官方示例密钥；公开可知即可伪造 Token |
| 同上 | `token.expireTime: 30`（分钟） | 教学场景可接受，但需明确无刷新机制 |
| `backend/sql/edu/V2__edu_permissions.sql` | 9 个演示账号密码统一为 `admin123`（BCrypt 哈希与若依默认 `admin` 相同） | 演示可见范围含成绩单；上线前必须改 |
| `backend/ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java` | `/swagger-ui/**`、`/v3/api-docs/**`、`/druid/**` 为 `permitAll` | 生产环境暴露接口文档与数据库监控页 |

方案 ch.7.2 还要求 SSO 先做映射模型、不替换若依权限体系；ch.7.3 要求
「服务端严格做资源级授权；若依菜单权限仅是入口权限」。

## 决策（Decision）

### 1. 授权：资源级校验是唯一真相

- `@PreAuthorize("@ss.hasPermi('edu:xxx:yyy')")` **只用于入口控制**（是否能看到这个功能），
  一律不作为数据归属的判据。
- 每个返回「本人」数据的接口在 Service 层做归属校验，越权统一抛
  `ServiceException("无权访问该资源")`。
- 越权与不存在**返回同样的错误**，不提供数据存在性差异（避免用 403/404 的差别探测
  「某个 ID 是否存在」）。

### 2. 身份：永远从 JWT 派生，不接受请求参数

身份链路固定为：

```text
Authorization: Bearer <token>
  → 若依 TokenService 解析 → LoginUser
  → SecurityUtils.getUserId()                    // backend/ruoyi-common/.../SecurityUtils.java
  → edu_student.sys_user_id = ?                  // uk_student_sys_user（唯一，一对一）
  → edu_teacher.sys_user_id = ?                  // uk_teacher_sys_user（唯一，一对一）
  → 得到 student_id / teacher_id，再查业务数据
```

硬性要求：

- **禁止**从 request body、query、path 里读 `studentId` / `teacherId` 作为身份依据。
  前端可以传课程 ID、开课 ID、规划 ID，但绝不能传「我是谁」。
- 带 `me` / `mine` 的接口（`GET /edu/students/me`、`/edu/students/me/progress`、
  `/edu/students/me/records`、`/edu/offerings/mine`、`/edu/plans/list`（学生视角））
  一律执行上述链路。
- **映射失败即失败**：`sys_user` 存在但 `edu_student` 没有对应行时，抛
  `ServiceException("当前账号未绑定学籍信息")`，**不得**返回空列表或空对象
  （空结果会被前端误渲染成「没有成绩」，掩盖了配置错误）。

### 3. 归属校验的具体规则

| 资源 | 校验规则 | 依据列 |
|---|---|---|
| 教师读自己授课的课程 | `edu_course_offering.teacher_id = 当前 teacher_id` | `idx_offering_teacher` |
| 教师读/改作业与评分 | 作业所属开课的 `teacher_id = 当前 teacher_id`（经 `edu_assignment.offering_id` 反查） | `fk_assignment_offering` |
| 学生读成绩 | `edu_course_record.student_id = 当前 student_id` | `idx_record_student` |
| 学生读/改自己的规划 | `edu_study_plan.student_id = 当前 student_id` | `uk_plan_student_term` |
| 学生提交作业 | `edu_submission.student_id = 当前 student_id`，且该学生已选该开课 | `uk_submission` |
| 学生选课 | 只能给自己建 `edu_enrollment`（`student_id` 由后端填充） | `uk_enrollment` |
| 管理员 | 走 `SecurityUtils.isAdmin()` 或 `edu:*` 权限标识；**仍然不接受前端传身份** | — |

教师权限**不是**「拥有 `edu:offering:list` 就能看所有开课」：`GET /edu/offerings/mine`
必须在 SQL 里带上 `teacher_id` 条件，参数化拼接，不用字符串拼接。

### 4. SSO 延后，但先建映射层

- 本期不接学校 SSO（方案 ch.7.2：需要校方授权的 OIDC/CAS 协议、测试环境、回调地址）。
- 表结构已预留 `edu_sso_identity`：
  - `uk_sso_provider_subject (provider, external_subject)` —— 一个外部身份只能绑一个人；
  - `uk_sso_user_provider (sys_user_id, provider)` —— 一个平台用户在同一个 IdP 下只有一个身份；
  - 只存映射，**不存学校密码**。
- 接入顺序固定为：`provider + external_subject` → 查 `edu_sso_identity` → 命中则登录为对应
  `sys_user`；未命中则按批准的规则开通账号后写绑定。**禁止**按「学号匹配姓名」自动开通。
- 前端传来的角色一律不可信；角色只由 `sys_user_role` 决定。

### 5. 密钥与账号加固

| 事项 | 要求 | 落点 |
|---|---|---|
| Token 密钥 | 生产环境必须由环境变量覆盖 `token.secret`，不得提交真实值；每个环境不同 | `application.yml`（dev 用占位值）/ 部署环境变量 |
| Token 有效期 | `expireTime: 30` 分钟保持；无 refresh token，过期即重新登录 | `application.yml` |
| 演示账号密码 | 上线前全部修改：8 个演示学生 + 1 个演示教师（`V2` 中的 `stu_*` / `teacher_li`）与 `admin` 共 10 个账号一并处理 | `V2__edu_permissions.sql` 的账号段落 |
| 接口文档与监控 | 生产关闭 `springdoc` 与 Druid 控制台（或加 IP 白名单 + 认证） | `SecurityConfig` 的 `permitAll` 列表（仅生产 profile） |
| 全站 HTTPS | Nginx 终止 TLS；HSTS 与安全响应头 | `deploy/`（Nginx 配置） |
| 日志脱敏 | `@Log` 只记业务动作，禁记密码、Token、画像与笔记内容 | `@Log(title=..., businessType=...)` |
| 数据库账号 | 应用账号只授予 `edu_*` 与必要 `sys_*` 的 DML 权限，不给 DDL | 部署时的初始化脚本/账号创建 |
| 跨域 | 前后端同域部署（Nginx 反代），不开放 `*` 的 CORS | `frontend/.env.production` 的 `/prod-api` |

## 后果（Consequences）

**正面**

- 越权只能通过「改数据库」或「伪造 Token（需密钥）」实现，不能靠改前端参数。
- 判据唯一：出问题时只需检查 `SecurityUtils.getUserId()` 链路与归属 SQL 条件。
- 菜单权限与数据权限职责清楚：产品加菜单不改授权模型，加资源类型才需要新增归属校验。
- SSO 接入时不需要重构业务代码，只需替换「怎么拿到 `sys_user`」这一步。

**需要承担的代价**

- **每个 `me` 接口多一次映射查询**（`sys_user_id` → `student_id`）。可按 `userId` 做请求级
  或短 TTL 缓存；数据量小，先不加缓存，避免缓存不一致带来的「改了学籍没生效」问题。
- **越权测试是必须项**：跨学生访问、跨教师访问、未登录访问、非法 ID、越权改成绩，
  每条都要有测试用例（方案 ch.9.1 的安全验收项）。这类用例不能靠人工点。
- **多了一个失败模式**：账号存在但学籍未绑定会报错而不是空列表。需要在部署检查清单里
  加一条「演示账号均已绑定 `edu_student`/`edu_teacher`」。
- **演示环境与生产配置必须分离**：`admin123`、默认 `token.secret`、开放的 Swagger
  只允许存在于开发/演示环境。

## 备选方案（Alternatives considered）

| 方案 | 为什么没选 |
|---|---|
| **只用菜单/按钮权限** | 学生 A 与 B 的权限字符串完全相同，无法阻止 A 看 B 的成绩。等于没有数据保护 |
| **前端传 `studentId`，后端信任** | 改一个 query 参数就能读全校成绩。方案 ch.6.1 与 ch.7.1 都明确禁止 |
| **用若依 `@DataScope` 部门数据范围** | 选课关系不是组织关系：跨院选课、外院授课都会判错；且教育表刻意不参与若依部门体系（`backend-conventions.md` §8 要求移除生成代码里的 `@DataScope`） |
| **Spring Security ACL / ABAC 引擎** | 需要额外的 `acl_*` 表与表达式引擎，对一个只有 6 类归属规则的平台是过度设计；归属条件用一条 `where` 就能表达 |
| **行级安全用数据库视图 + 会话变量** | 依赖连接池的会话状态，容易被绕过且难以测试 |
| **先接学校 SSO 再开发** | 方案 ch.7.2 要求先明确映射模型；校方协议、测试环境、回调地址都未确认，阻塞整个 MVP |
| **按学号/姓名自动开通账号** | 姓名不唯一、学号可被猜；自动开通等于把「谁能注册」交给攻击者 |
