import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type {
  AdvisorOverview,
  AdvisorStudent,
  AdvisorStudentDetail,
  AdvisorStudentQuery
} from '@/types/education/advisor'

// 导师看板总览（学生数、平均 GPA/学分、GPA 分布与趋势、风险与进度分布）
export function getAdvisorOverview(): Promise<AjaxResult<AdvisorOverview>> {
  return request({
    url: '/edu/advisors/overview',
    method: 'get'
  })
}

// 学生列表（分页）：entryYear / majorId / riskLevel / keyword
export function getAdvisorStudents(query?: AdvisorStudentQuery): Promise<TableDataInfo<AdvisorStudent>> {
  return request({
    url: '/edu/advisors/students',
    method: 'get',
    params: query
  })
}

// 单个学生画像：基本信息 + 分类学分 + 学期 GPA 趋势 + 已修课程数 + 风险明细
export function getAdvisorStudent(studentId: number): Promise<AjaxResult<AdvisorStudentDetail>> {
  return request({
    url: '/edu/advisors/students/' + studentId,
    method: 'get'
  })
}
