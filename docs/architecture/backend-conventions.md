# 后端开发约定（ruoyi-education）

> 本文是 `backend/ruoyi-education` 的唯一编码约定来源。任何新增业务代码都必须遵守，
> 以便多人并行开发时不产生风格与结构分叉。相关架构决策见 `docs/architecture/adr/`。

## 1. 模块与包结构

一个 Maven 模块 `ruoyi-education`，内部按方案 4.1 划分为 6 个业务域：

```text
backend/ruoyi-education/src/main/java/com/ruoyi/education/
├── common/        域内共享：枚举、常量、先修图工具、学分计算工具
├── course/        课程目录与先修关系
├── curriculum/    专业培养方案与学分要求
├── enrollment/    学期、开课、选课、成绩记录
├── assignment/    作业与提交
├── planning/      规则推荐、冲突与先修校验
└── analytics/     学分进度、成绩趋势、技能画像统计
```

每个业务域内部统一分层：

```text
course/
├── domain/            EduCourse.java、EduCoursePrereq.java   （继承 BaseEntity）
├── dto/               入参/出参对象（*Query、*Body、*VO）
├── mapper/            EduCourseMapper.java
├── service/           IEduCourseService.java
├── service/impl/      EduCourseServiceImpl.java
└── controller/        EduCourseController.java
```

MyBatis XML 放在：

```text
backend/ruoyi-education/src/main/resources/mapper/education/EduCourseMapper.xml
```

**不要**把教育业务代码放进 `ruoyi-admin` / `ruoyi-system` / `ruoyi-framework`。
若依内核模块只允许“新增依赖”级别的改动（`backend/pom.xml`、`ruoyi-admin/pom.xml`）。

## 2. 命名

| 类型 | 规则 | 示例 |
|---|---|---|
| 实体 | `Edu` + 表名驼峰 | `edu_course` → `EduCourse` |
| Mapper | 实体 + `Mapper` | `EduCourseMapper` |
| Service 接口 | `I` + 实体 + `Service` | `IEduCourseService` |
| Service 实现 | 实体 + `ServiceImpl`，加 `@Service` | `EduCourseServiceImpl` |
| Controller | 实体 + `Controller`，路径用复数业务资源名 | `EduCourseController` → `/edu/courses` |
| DTO | 用途后缀 | `CourseQuery`、`PlanValidateBody`、`CreditProgressVO` |
| 权限标识 | `edu:<资源>:<动作>` | `edu:course:list` |

## 3. 通用约定

- 实体继承 `com.ruoyi.common.core.domain.BaseEntity`（自带 `createBy/createTime/updateBy/updateTime/remark/params`）。
- 主键字段名与表一致（`courseId` ↔ `course_id`），并在 `domain` 上加 `@Excel`（导出用，可选）。
- Controller 继承 `com.ruoyi.common.core.controller.BaseController`：
  - 列表：`startPage(); List<T> list = service.selectXxxList(query); return getDataTable(list);`
  - 单条：`return success(obj);`
  - 写操作：`return toAjax(service.insertXxx(entity));`
  - 当前用户：`getUserId()` / `getUsername()`；禁止从前端接收 `studentId` 作为身份依据。
- Service 层用 `@Transactional` 保证多表一致性；业务错误抛
  `com.ruoyi.common.exception.ServiceException("可读的中文原因")`，禁止返回 `null` 表示失败。
- 参数校验用 `jakarta.validation`（`@NotNull`、`@NotBlank`、`@DecimalMin`），Controller 上用 `@Validated`。
- 写操作加 `@Log(title = "...", businessType = BusinessType.INSERT|UPDATE|DELETE)`。
- 分页统一走 PageHelper，不要手写 `limit`。
- 时间字段类型：`java.util.Date`（若依既有风格），时间格式由 `spring.jackson.date-format` 统一。
- 金额/学分：`java.math.BigDecimal`。

## 4. 权限与数据边界（硬性要求）

- 菜单权限用 `@PreAuthorize("@ss.hasPermi('edu:xxx:yyy')")`，
  权限标识必须与 `backend/sql/edu/V2__edu_permissions.sql` 中登记的一致。
- **菜单权限 ≠ 数据权限**。所有“本人”接口必须由后端按 `SecurityUtils.getUserId()`
  反查 `edu_student.sys_user_id`（或 `edu_teacher.sys_user_id`）后再查数据，
  绝不允许直接采信前端传入的 `studentId`。
- 教师接口必须额外校验“该课程确实由当前教师授课”（`edu_course_offering.teacher_id`）。
- 学生只能访问 `edu_course_record` / `edu_study_plan` / `edu_submission` 中属于本人的行。
- 越权访问统一抛 `ServiceException("无权访问该资源")`，不返回数据存在性差异。

开发期固定使用的账号（见 V2 迁移）：

| 账号 | 角色 | 说明 |
|---|---|---|
| `admin` | ADMIN | 若依超级管理员，全部权限 |
| `stu_chen` / `stu_wang` / ... | STUDENT | 演示学生，只能看本人数据 |
| `teacher_li` | TEACHER | 演示教师，只能看本人授课数据 |

## 5. 枚举与常量

放到 `com.ruoyi.education.common.enums`，用带 `code`/`name` 的枚举，避免魔法字符串：

- `RequirementType`：`REQUIRED` / `ELECTIVE` / `GE` / `BREADTH` / `DEPTH` / `STREAM`
- `PlanItemStatus`：`VALID` / `WARN` / `INVALID` / `UNKNOWN`
- `PlanStatus`：`DRAFT` / `VALIDATED` / `SUBMITTED` / `ARCHIVED`
- `EnrollmentStatus`：`ENROLLED` / `DROPPED` / `COMPLETED`
- `RecordStatus`：`COMPLETED` / `IN_PROGRESS` / `WITHDRAWN`
- `AssignmentStatus`：`DRAFT` / `PUBLISHED` / `CLOSED`

## 6. 先修规则的语义（必须一致实现）

`edu_course_prereq` 的行按 `group_no` 分组：

- **组内 AND**：同一 `group_no` 内的所有先修课程都必须通过。
- **组间 OR**：任一 `group_no` 组合被完整满足，即视为先修条件满足。
- 例：`(ACT3111 & ACT3112) or ACT3011` → `group_no=1{ACT3111,ACT3112}`、`group_no=2{ACT3011}`。

`com.ruoyi.education.common.PrerequisiteGraph` 必须提供：

| 方法 | 语义 |
|---|---|
| `Map<Long, List<EduCoursePrereq>> loadGraph()` | 一次性载入先修有向边并按 `group_no` 分组 |
| `List<EduCourse> missingPrerequisites(courseId, Set<Long> passedCourseIds)` | 返回未满足的最少阻力路径上的课程（用于“缺少先修课”提示） |
| `boolean isSatisfied(courseId, Set<Long> passedCourseIds)` | 先修条件是否满足 |
| `List<Long> detectCycles()` | 返回处于环中的课程 ID；非空即为数据错误 |

环检测、自依赖检测必须在**数据写入**与**规则计算**两侧都生效
（DDL 已有 `check (course_id <> prereq_course_id)`）。

## 7. 测试

- `backend/ruoyi-education/src/test/java/com/ruoyi/education/...` 下用 JUnit 5。
- 纯规则逻辑（先修图、学分汇总、校验原因生成）必须有单元测试，
  不依赖 Spring 容器与数据库。
- 运行：`mvn -pl ruoyi-education test`。

## 8. 与若依代码生成器配合

管理端标准 CRUD 页面允许先用若依代码生成器生成 `domain/mapper/service/controller`
与 Vue 页面，再按本约定改造：

1. 包名改为 `com.ruoyi.education.<域>`；
2. 移除生成代码里的 `@DataScope`（教育表不使用若依部门数据范围）；
3. 补上 `@PreAuthorize` 的 `edu:*` 权限标识；
4. 补上资源级归属校验（见第 4 节）。
