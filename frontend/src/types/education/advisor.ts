/**
 * 导师看板（advisor）类型定义
 * =====================================================================
 * 与 docs/architecture/api-contract.md 第 9 节一一对应；字段命名沿用
 * education 模块约定：后端 camelCase，数值型字段为 number，缺省值可选。
 */

import type { PageDomain } from '@/types'

/** 单条风险原因（后端规则命中明细） */
export interface AdvisorRiskReason {
  /** 规则编码：GPA_LOW / CREDIT_BEHIND / FAILED_COURSE / NO_RECENT_TERM */
  code?: string
  /** 规则可读标题 */
  label?: string
  /** 可读明细（含具体数值） */
  detail?: string
  /** 后端基础权重（默认 1.0） */
  weight?: number
}

/** GPA 分布桶 */
export interface AdvisorGpaBucket {
  /** 桶标签，如 3.5-4.0 */
  label?: string
  /** 学生数 */
  count?: number
}

/** 学期平均 GPA 趋势点 */
export interface AdvisorGpaTrendPoint {
  /** 学期代码，如 2022-23-1 */
  termCode?: string
  /** 平均 GPA */
  avgGpa?: number
}

/** 风险等级分布 */
export interface AdvisorRiskCount {
  /** 高风险学生数 */
  high?: number
  /** 中风险学生数 */
  medium?: number
  /** 低风险学生数 */
  low?: number
}

/** 学分进度分布 */
export interface AdvisorProgress {
  /** 进度正常 */
  onTrack?: number
  /** 进度滞后 */
  behind?: number
  /** 数据不足 */
  unknown?: number
}

/** 分类学分均值（overview 扩展字段） */
export interface AdvisorCategoryAverage {
  /** 成绩类别（Major/GenEd/English/Chinese/IT/PE） */
  category?: string
  /** 平均已修学分 */
  avgCredits?: number
  /** 修读记录数 */
  courseCount?: number
}

/** 导师看板总览（GET /edu/advisors/overview） */
export interface AdvisorOverview {
  /** 学生总数 */
  studentCount?: number
  /** 平均累计 GPA */
  avgGpa?: number
  /** 平均已修学分 */
  avgCredits?: number
  /** GPA 分布 */
  gpaBuckets?: AdvisorGpaBucket[]
  /** 学期 GPA 趋势 */
  gpaTrend?: AdvisorGpaTrendPoint[]
  /** 风险等级分布 */
  risk?: AdvisorRiskCount
  /** 学分进度分布 */
  progress?: AdvisorProgress
  /** 分类学分均值 */
  categoryBreakdown?: AdvisorCategoryAverage[]
}

/** 学生列表行（GET /edu/advisors/students） */
export interface AdvisorStudent {
  /** 学生ID */
  studentId?: number
  /** 学号 */
  studentNo?: string
  /** 姓名 */
  name?: string
  /** 专业名称 */
  majorName?: string
  /** 入学年份 */
  entryYear?: number
  /** 累计 GPA */
  cumGpa?: number
  /** 已修总学分 */
  totalCredits?: number
  /** 学分完成度（%） */
  creditProgressPct?: number
  /** 平均分 */
  avgScore?: number
  /** 风险等级：high / medium / low */
  riskLevel?: string
  /** 风险分（0-100） */
  riskScore?: number
  /** 命中的风险原因 */
  riskReasons?: AdvisorRiskReason[]
}

/** 分类学分（学生画像） */
export interface AdvisorCategoryCredit {
  /** 成绩类别 */
  category?: string
  /** 已获得学分 */
  credits?: number
  /** 已通过课程数 */
  courseCount?: number
}

/** 单个学生画像（GET /edu/advisors/students/{studentId}） */
export interface AdvisorStudentDetail {
  /** 学生ID */
  studentId?: number
  /** 学号 */
  studentNo?: string
  /** 姓名 */
  name?: string
  /** 专业ID */
  majorId?: number
  /** 专业名称 */
  majorName?: string
  /** 专业编码 */
  majorCode?: string
  /** 入学年份 */
  entryYear?: number
  /** 书院/学院 */
  college?: string
  /** 累计 GPA */
  cumGpa?: number
  /** 已修总学分 */
  totalCredits?: number
  /** 毕业要求学分 */
  graduationCredits?: number
  /** 按入学年推算的应修学分 */
  expectedCredits?: number
  /** 学分完成度（%） */
  creditProgressPct?: number
  /** 平均分 */
  avgScore?: number
  /** 已通过课程数 */
  completedCourseCount?: number
  /** 未通过课程数 */
  failedCourseCount?: number
  /** 最近学期 GPA */
  latestTermGpa?: number
  /** 风险等级 */
  riskLevel?: string
  /** 风险分 */
  riskScore?: number
  /** 风险明细 */
  riskReasons?: AdvisorRiskReason[]
  /** 分类学分 */
  categoryCredits?: AdvisorCategoryCredit[]
  /** 学期 GPA 趋势 */
  gpaTrend?: AdvisorGpaTrendPoint[]
}

/** 导师看板学生列表查询参数 */
export interface AdvisorStudentQuery extends PageDomain {
  /** 入学年份 */
  entryYear?: number
  /** 专业ID */
  majorId?: number
  /** 风险等级：high / medium / low */
  riskLevel?: string
  /** 姓名 / 学号关键字 */
  keyword?: string
}
