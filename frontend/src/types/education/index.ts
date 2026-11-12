/**
 * 教育业务模块类型定义（education）
 * =====================================================================
 * 约定：
 *   1. 字段名 = 数据库字段 snake_case → camelCase（如 title_zh → titleZh、min_units → minUnits），
 *      与 backend/sql/edu/V1__edu_schema.sql 一一对应；
 *   2. 所有字段均为可选（沿用若依 SysNotice 的写法），
 *      便于「新增/修改表单」「列表行」「后端 DTO」共用同一个类型；
 *   3. 联表补充字段（termCode、courseTitle、studentName 等）同样可选，
 *      后端未返回时为 undefined，视图层必须容忍缺省；
 *   4. 数值型字段（学分、GPA、分数）统一为 number，后端 BigDecimal 由 Jackson 序列化为数字；
 *   5. 状态枚举以字符串字面量注释说明，实际类型用 string 以兼容后端字典扩展。
 * 权威接口契约见 docs/architecture/api-contract.md。
 */
import type { BaseEntity, PageDomain } from '@/types'

/* =====================================================================
 * 1. 课程中心（course）
 * ===================================================================== */

/** 课程目录（edu_course） */
export interface EduCourse extends BaseEntity {
  /** 课程ID */
  courseId?: number
  /** 课程代码，如 CSC1001 */
  code?: string
  /** 课程中文名 */
  titleZh?: string
  /** 课程英文名 */
  titleEn?: string
  /** 开课学科（SIS subject） */
  subject?: string
  /** 课程序号，如 1001 */
  courseNumber?: string
  /** 首字母分组（A-Z） */
  letter?: string
  /** 学分 */
  credits?: number
  /** 课程层级（由课程代码推算，如 1001 → 1000） */
  level?: number
  /** 课程类别（Major/GenEd/English/Chinese/IT/PE/Other） */
  category?: string
  /** 评分方式 */
  gradingBasis?: string
  /** 课程组成部分 */
  component?: string
  /** 校区 */
  campus?: string
  /** 开课学院 */
  school?: string
  /** 教学组织 */
  academicOrg?: string
  /** 大纲状态（saved_outline / ghost_outline_unavailable） */
  outlineStatus?: string
  /** 开课学期状态 */
  termsStatus?: string
  /** 历史开课学期（换行分隔的原始文本） */
  offeredTerms?: string
  /** 先修课原始文本（SIS 原文） */
  prereqText?: string
  /** 共修课原始文本 */
  coreqText?: string
  /** 课程简介（中文） */
  descriptionZh?: string
  /** 课程简介（英文） */
  descriptionEn?: string
  /** 状态（0正常 1停用） */
  status?: string
  /** 删除标志（0存在 2删除） */
  delFlag?: string
}

/**
 * 课程筛选选项（GET /edu/courses/options）
 * 后端返回 schools / categories / levels 三个集合，用于列表页下拉筛选。
 */
export interface EduCourseOption {
  /** 开课学院列表 */
  schools: string[]
  /** 课程类别列表 */
  categories: string[]
  /** 课程层级列表 */
  levels: number[]
}

/** 课程先修关系（edu_course_prereq，有向边；组内 AND，组间 OR） */
export interface EduCoursePrereq {
  /** 主键 */
  id?: number
  /** 课程ID */
  courseId?: number
  /** 先修课程ID */
  prereqCourseId?: number
  /** 先修组号：组内 AND，组间 OR */
  groupNo?: number
  /** SIS 原始先修文本片段（可追溯） */
  rawText?: string
  /** 先修课程代码（联表补充） */
  prereqCourseCode?: string
  /** 先修课程名称（联表补充） */
  prereqCourseTitle?: string
  /** 先修课程代码（部分实现使用短字段名） */
  code?: string
  /** 先修课程名称（部分实现使用短字段名） */
  title?: string
}

/** 缺失的先修课程（先修检查 / 推荐结果共用） */
export interface MissingPrereq {
  courseId?: number
  code?: string
  title?: string
}

/** 先修检查结果（GET /edu/courses/{courseId}/prereq-check） */
export interface PrereqCheckResult {
  /** 先修条件是否已满足 */
  satisfied?: boolean
  /** 未满足的先修课程（最少阻力路径） */
  missing?: MissingPrereq[]
  /** 可读结论（用于页面提示） */
  reason?: string
}

/** 课程列表查询参数 */
export interface CourseQuery extends PageDomain {
  /** 课程代码（模糊） */
  code?: string
  /** 课程名称（模糊） */
  title?: string
  /** 开课学院 */
  school?: string
  /** 课程类别 */
  category?: string
  /** 课程层级（后端按数字解析，兼容 '1000' 字符串） */
  level?: number | string
  /** 是否已有大纲；后端按布尔解析，兼容 'true' / 'false' 字符串 */
  hasOutline?: boolean | string
}

/* =====================================================================
 * 2. 培养方案（curriculum）
 * ===================================================================== */

/** 专业（edu_major） */
export interface EduMajor extends BaseEntity {
  /** 专业ID */
  majorId?: number
  /** 专业编码（唯一，如 CSE） */
  majorCode?: string
  /** 专业中文名 */
  majorNameZh?: string
  /** 专业英文名 */
  majorNameEn?: string
  /** 所属学院缩写，如 SDS / SME */
  schoolAbbr?: string
  /** 所属学院名称 */
  schoolName?: string
  /** 学位类型 */
  degreeType?: string
  /** 标准学制（年） */
  durationYears?: number
  /** 状态（0正常 1停用） */
  status?: string
  /** 删除标志（0存在 2删除） */
  delFlag?: string
}

/** 培养方案（edu_curriculum，按版本保存，避免历史规则被覆盖） */
export interface EduCurriculum extends BaseEntity {
  /** 培养方案ID */
  curriculumId?: number
  /** 专业ID */
  majorId?: number
  /** 版本标签，如 2025-26 and thereafter */
  versionLabel?: string
  /** 生效入学年份 */
  versionYear?: number
  /** 适用起始入学年 */
  effectiveFrom?: number
  /** 适用结束入学年（null 表示一直有效） */
  effectiveTo?: number
  /** 毕业总学分要求 */
  totalCredits?: number
  /** 专业学分要求 */
  majorCredits?: number
  /** 来源文件名 */
  sourceDoc?: string
  /** 状态（0启用 1停用） */
  status?: string
  /** 删除标志（0存在 2删除） */
  delFlag?: string
  /** 专业中文名（联表补充） */
  majorNameZh?: string
  /** 专业编码（联表补充） */
  majorCode?: string
  /** 方案条目（详情接口返回） */
  items?: EduCurriculumItem[]
}

/** 培养方案课程要求（edu_curriculum_item） */
export interface EduCurriculumItem extends BaseEntity {
  /** 主键 */
  itemId?: number
  /** 培养方案ID */
  curriculumId?: number
  /** 课程ID（可为空，表示纯学分池） */
  courseId?: number
  /** 课程代码快照（课程未入库时保留） */
  courseCode?: string
  /** 要求类型：REQUIRED/ELECTIVE/GE/BREADTH/DEPTH/STREAM */
  requirementType?: string
  /** 方案中的章节名称（如 School Package） */
  section?: string
  /** 方向/流派（可选） */
  stream?: string
  /** 该条目最低学分 */
  minUnits?: number
  /** 排序 */
  sortOrder?: number
  /** 课程中文名（联表补充） */
  courseTitle?: string
}

/** 培养方案列表查询参数 */
export interface CurriculumQuery extends PageDomain {
  /** 专业ID */
  majorId?: number
  /** 生效入学年份 */
  versionYear?: number
}

/** 专业列表查询参数 */
export interface MajorQuery extends PageDomain {
  /** 专业名称/编码关键字 */
  keyword?: string
}

/* =====================================================================
 * 3. 学期 / 开课 / 选课 / 成绩（enrollment）
 * ===================================================================== */

/** 学期（edu_term） */
export interface EduTerm extends BaseEntity {
  /** 学期ID */
  termId?: number
  /** 学期代码，如 2026-27-1 */
  termCode?: string
  /** 学期名称 */
  termName?: string
  /** 学年 */
  academicYear?: string
  /** 学期序号（1/2/3=暑期） */
  termIndex?: number
  /** 开始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
  /** 是否当前学期（0否 1是） */
  isCurrent?: string
  /** 状态（0正常 1停用） */
  status?: string
}

/** 学期开课（edu_course_offering） */
export interface EduCourseOffering extends BaseEntity {
  /** 开课ID */
  offeringId?: number
  /** 课程ID */
  courseId?: number
  /** 学期ID */
  termId?: number
  /** 授课教师ID */
  teacherId?: number
  /** 容量 */
  capacity?: number
  /** 已选人数 */
  enrolledCount?: number
  /** 上课时间地点 */
  scheduleText?: string
  /** 状态（0开放 1关闭） */
  status?: string
  /** 课程代码（联表补充） */
  courseCode?: string
  /** 课程名称（联表补充） */
  courseTitle?: string
  /** 学分（联表补充） */
  units?: number
  /** 学期代码（联表补充） */
  termCode?: string
  /** 学期名称（联表补充） */
  termName?: string
  /** 教师姓名（联表补充） */
  teacherName?: string
  /** 选课学生数（教师授课列表补充） */
  studentCount?: number
  /** 作业数（教师授课列表补充） */
  assignmentCount?: number
}

/** 学生学籍（edu_student） */
export interface EduStudent extends BaseEntity {
  /** 学生ID */
  studentId?: number
  /** 关联的系统用户ID */
  sysUserId?: number
  /** 学号（唯一） */
  studentNo?: string
  /** 姓名 */
  name?: string
  /** 专业ID */
  majorId?: number
  /** 入学年份 */
  entryYear?: number
  /** 书院/学院 */
  college?: string
  /** 邮箱 */
  email?: string
  /** 累计 GPA */
  cumGpa?: number
  /** 已修总学分 */
  totalCredits?: number
  /** 状态（0正常 1停用） */
  status?: string
  /** 删除标志（0存在 2删除） */
  delFlag?: string
  /** 专业中文名（联表补充） */
  majorNameZh?: string
  /** 专业编码（联表补充） */
  majorCode?: string
  /** 学院名称（联表补充） */
  schoolName?: string
}

/** 选课关系（edu_enrollment） */
export interface EduEnrollment extends BaseEntity {
  /** 选课ID */
  enrollmentId?: number
  /** 学生ID */
  studentId?: number
  /** 开课ID */
  offeringId?: number
  /** 状态：ENROLLED/DROPPED/COMPLETED */
  status?: string
  /** 选课时间 */
  enrolledAt?: string
  /** 课程ID（联表补充） */
  courseId?: number
  /** 课程代码（联表补充） */
  courseCode?: string
  /** 课程名称（联表补充） */
  courseTitle?: string
  /** 学期ID（联表补充） */
  termId?: number
  /** 学分（联表补充） */
  units?: number
  /** 上课时间地点（联表补充） */
  scheduleText?: string
}

/** 已修课程与成绩（edu_course_record） */
export interface EduCourseRecord extends BaseEntity {
  /** 成绩记录ID */
  recordId?: number
  /** 学生ID */
  studentId?: number
  /** 课程ID（课程目录命中时写入） */
  courseId?: number
  /** 课程代码快照 */
  courseCode?: string
  /** 课程名称快照 */
  courseTitle?: string
  /** 学期ID */
  termId?: number
  /** 等级制成绩，如 A-/B+ */
  grade?: string
  /** 百分制成绩 */
  score?: number
  /** 学分 */
  units?: number
  /** 成绩类别（Major/GenEd/English/Chinese/IT/PE） */
  category?: string
  /** 是否通过（0否 1是） */
  passed?: string
  /** 状态：COMPLETED/IN_PROGRESS/WITHDRAWN */
  recordStatus?: string
  /** 数据来源：SIS_IMPORT/DEMO/MANUAL */
  source?: string
  /** 删除标志（0存在 2删除） */
  delFlag?: string
  /** 学期代码（联表补充，成绩单展示列） */
  termCode?: string
  /** 学期名称（联表补充） */
  termName?: string
}

/** 学期列表查询参数 */
export interface TermQuery extends PageDomain {
  /** 是否当前学期（0否 1是） */
  isCurrent?: string
}

/** 开课列表查询参数 */
export interface OfferingQuery extends PageDomain {
  /** 学期ID */
  termId?: number
  /** 课程ID */
  courseId?: number
  /** 教师ID */
  teacherId?: number
}

/** 本人成绩单查询参数 */
export interface RecordQuery extends PageDomain {
  /** 学期代码 */
  termCode?: string
  /** 成绩类别 */
  category?: string
}

/* =====================================================================
 * 4. 作业与提交（assignment）
 * ===================================================================== */

/** 作业任务（edu_assignment） */
export interface EduAssignment extends BaseEntity {
  /** 作业ID */
  assignmentId?: number
  /** 开课ID */
  offeringId?: number
  /** 作业标题 */
  title?: string
  /** 作业说明 */
  description?: string
  /** 截止时间 */
  dueAt?: string
  /** 满分 */
  totalScore?: number
  /** 状态：DRAFT/PUBLISHED/CLOSED */
  status?: string
  /** 删除标志（0存在 2删除） */
  delFlag?: string
  /** 课程代码（联表补充） */
  courseCode?: string
  /** 课程名称（联表补充） */
  courseTitle?: string
  /** 学期代码（联表补充） */
  termCode?: string
}

/** 作业提交（edu_submission） */
export interface EduSubmission extends BaseEntity {
  /** 提交ID */
  submissionId?: number
  /** 作业ID */
  assignmentId?: number
  /** 学生ID */
  studentId?: number
  /** 提交内容 */
  content?: string
  /** 附件地址 */
  attachmentUrl?: string
  /** 提交时间 */
  submittedAt?: string
  /** 得分 */
  grade?: number
  /** 教师评语 */
  feedback?: string
  /** 状态：SUBMITTED/GRADED/RETURNED */
  status?: string
  /** 学号（联表补充） */
  studentNo?: string
  /** 学生姓名（联表补充） */
  studentName?: string
  /** 作业标题（联表补充） */
  assignmentTitle?: string
  /** 作业满分（联表补充） */
  totalScore?: number
}

/** 作业列表查询参数 */
export interface AssignmentQuery extends PageDomain {
  /** 开课ID */
  offeringId?: number
}

/** 提交列表查询参数 */
export interface SubmissionQuery extends PageDomain {
  /** 作业ID */
  assignmentId?: number
  /** 开课ID */
  offeringId?: number
}

/* =====================================================================
 * 5. 选课规划（planning）
 * ===================================================================== */

/** 学习规划（edu_study_plan） */
export interface EduStudyPlan extends BaseEntity {
  /** 规划ID */
  planId?: number
  /** 学生ID */
  studentId?: number
  /** 目标学期ID */
  termId?: number
  /** 规划标题 */
  title?: string
  /** 状态：DRAFT/VALIDATED/SUBMITTED/ARCHIVED */
  status?: string
  /** 计划总学分 */
  totalUnits?: number
  /** 学分上限（可由管理员配置） */
  unitLimit?: number
  /** 最近一次校验结果：PASS/WARN/FAIL */
  validationState?: string
  /** 校验摘要 */
  validationNote?: string
  /** 删除标志（0存在 2删除） */
  delFlag?: string
  /** 学期代码（联表补充） */
  termCode?: string
  /** 学期名称（联表补充） */
  termName?: string
  /** 学号（联表补充） */
  studentNo?: string
  /** 学生姓名（联表补充） */
  studentName?: string
  /** 规划明细（详情接口返回） */
  items?: EduStudyPlanItem[]
}

/** 学习规划明细（edu_study_plan_item，含可解释的规则说明） */
export interface EduStudyPlanItem extends BaseEntity {
  /** 主键 */
  itemId?: number
  /** 规划ID */
  planId?: number
  /** 课程ID */
  courseId?: number
  /** 推荐理由（可解释说明） */
  reason?: string
  /** 校验状态：VALID/WARN/INVALID/UNKNOWN */
  validationStatus?: string
  /** 校验信息（缺少的先修课等） */
  validationMessage?: string
  /** 排序 */
  sortOrder?: number
  /** 课程代码（联表补充） */
  code?: string
  /** 课程名称（联表补充） */
  title?: string
  /** 学分（联表补充） */
  units?: number
}

/** 规划详情（GET /edu/plans/{planId} 的 data 结构） */
export interface EduPlanDetail {
  /** 规划主表 */
  plan?: EduStudyPlan
  /** 规划明细 */
  items?: EduStudyPlanItem[]
}

/** 规划列表查询参数 */
export interface PlanQuery extends PageDomain {
  /** 目标学期ID */
  termId?: number
  /** 规划状态 */
  status?: string
}

/** 保存规划课程的请求体元素（PUT /edu/plans/{planId}/items） */
export interface EduPlanItemInput {
  /** 课程ID */
  courseId: number
}

/** 试算请求体（POST /edu/plans/validate，不落库） */
export interface PlanValidateBody {
  /** 目标学期ID */
  termId: number
  /** 待校验课程ID集合 */
  courseIds: number[]
}

/** 规则推荐请求体（POST /edu/plans/recommend） */
export interface PlanRecommendBody extends PageDomain {
  /** 目标学期ID */
  termId: number
  /** 返回条数，默认 10 */
  limit?: number
}

/** 规划校验的单项结果（PlanValidationResult.items 元素） */
export interface PlanItemValidation {
  /** 课程ID（校验结果必然包含课程ID，故为必填） */
  courseId: number
  /** 课程代码 */
  code?: string
  /** 课程名称 */
  title?: string
  /** 学分 */
  units?: number
  /** 推荐/可解释理由 */
  reason?: string
  /** 校验状态：VALID/WARN/INVALID/UNKNOWN */
  validationStatus?: string
  /** 校验信息（缺少的先修课等） */
  validationMessage?: string
}

/** 规划校验结果（POST /edu/plans/validate、/edu/plans/{planId}/validate） */
export interface PlanValidationResult {
  /** 目标学期ID */
  termId?: number
  /** 学分上限 */
  unitLimit?: number
  /** 计划总学分 */
  totalUnits?: number
  /** 总体状态：PASS/WARN/FAIL */
  state?: string
  /** 汇总提示信息 */
  messages?: string[]
  /** 逐条校验结果 */
  items?: PlanItemValidation[]
}

/** 规则推荐结果（POST /edu/plans/recommend） */
export interface RecommendedCourse {
  /** 课程ID */
  courseId?: number
  /** 课程代码 */
  code?: string
  /** 课程名称 */
  title?: string
  /** 学分 */
  units?: number
  /** 推荐理由 */
  reason?: string
  /** 尚未满足的先修课程 */
  missingPrereqs?: MissingPrereq[]
}

/* =====================================================================
 * 6. 学情与技能画像（analytics）
 * ===================================================================== */

/** 学分分类进度行（CreditProgress.categoryBreakdown 元素） */
export interface CreditCategoryRow {
  /** 学分类别（Major/GenEd/English/Chinese/IT/PE） */
  category?: string
  /** 已获得学分 */
  earned?: number
  /** 要求学分 */
  required?: number
}

/** 学分进度（GET /edu/students/me/progress） */
export interface CreditProgress {
  /** 培养方案毕业总学分 */
  curriculumTotal?: number
  /** 专业必修要求学分 */
  majorRequired?: number
  /** 已获得学分 */
  earned?: number
  /** 剩余学分 */
  remaining?: number
  /** 分类学分进度 */
  categoryBreakdown?: CreditCategoryRow[]
}

/** GPA 趋势点（DashboardStat.gpaTrend 元素） */
export interface GpaTrendPoint {
  /** 学期代码，如 2022-23-1 */
  termCode?: string
  /** 学期 GPA */
  gpa?: number
  /** 学期名称（可选补充） */
  termName?: string
}

/** 规划状态汇总（DashboardStat.planSummary） */
export interface PlanSummary {
  /** 草稿数 */
  draft?: number
  /** 已提交数 */
  submitted?: number
  /** 已校验数（可选，后端未返回时为 undefined） */
  validated?: number
  /** 已归档数（可选） */
  archived?: number
}

/** 学习仪表盘聚合数据（GET /edu/analytics/dashboard） */
export interface DashboardStat {
  /** 学分进度 */
  credits?: CreditProgress
  /** 累计 GPA */
  gpa?: number
  /** GPA 趋势（按学期升序） */
  gpaTrend?: GpaTrendPoint[]
  /** 分类学分进度 */
  categoryBreakdown?: CreditCategoryRow[]
  /** 规划状态汇总 */
  planSummary?: PlanSummary
  /** 待提交作业数 */
  pendingAssignments?: number
}

/** 技能画像条目（GET /edu/analytics/skill-profile，服务端按已修课程聚合） */
export interface SkillProfileItem {
  /** 技能标签ID */
  skillTagId?: number
  /** 技能标签名称 */
  name?: string
  /** 技能分类（学院/能力域） */
  category?: string
  /** 关联度评分（0-10） */
  score?: number
  /** 关联课程数 */
  courseCount?: number
  /** 判断依据说明 */
  rationale?: string
  /** 标签编码（可选） */
  code?: string
  /** 关联等级（Very high/High/Moderate/Low，可选） */
  level?: string
}

/** 公共技能标签（edu_skill_tag） */
export interface SkillTag extends BaseEntity {
  /** 技能标签ID */
  skillTagId?: number
  /** 标签编码（唯一） */
  code?: string
  /** 标签名称 */
  name?: string
  /** 分类（学院/能力域） */
  category?: string
  /** 所属学院缩写 */
  schoolAbbr?: string
  /** 定义说明 */
  description?: string
  /** 匹配关键词 */
  keywords?: string
  /** 排序 */
  sortOrder?: number
  /** 状态（0正常 1停用） */
  status?: string
  /** 删除标志（0存在 2删除） */
  delFlag?: string
}

/** 课程—技能关联度（edu_course_competency） */
export interface CourseCompetency {
  /** 主键 */
  id?: number
  /** 课程ID */
  courseId?: number
  /** 技能标签ID */
  skillTagId?: number
  /** 关联度评分（0-10） */
  score?: number
  /** 关联等级（Very high/High/Moderate/Low） */
  level?: string
  /** 判断依据 */
  rationale?: string
  /** 命中的关键词 */
  matchedKeywords?: string
  /** 技能标签名称（联表补充） */
  name?: string
  /** 技能分类（联表补充） */
  category?: string
  /** 技能标签编码（联表补充） */
  skillCode?: string
  /** 学院缩写（联表补充） */
  schoolAbbr?: string
  /** 课程代码（联表补充） */
  courseCode?: string
  /** 课程名称（联表补充） */
  courseTitle?: string
  /** 关联课程数（学院能力池聚合时补充） */
  courseCount?: number
}

/** 学院能力池查询参数 */
export interface CompetencyQuery extends PageDomain {
  /** 学院缩写，如 SDS */
  schoolAbbr?: string
  /** 关键字（技能标签/课程名） */
  keyword?: string
}
