import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type {
  AssignmentQuery,
  EduAssignment,
  EduSubmission,
  SubmissionQuery
} from '@/types/education'

// 查询作业列表
export function listAssignment(query?: AssignmentQuery): Promise<TableDataInfo<EduAssignment>> {
  return request({
    url: '/edu/assignments/list',
    method: 'get',
    params: query
  })
}

// 教师发布作业
export function addAssignment(data: EduAssignment): Promise<AjaxResult> {
  return request({
    url: '/edu/assignments',
    method: 'post',
    data: data
  })
}

// 修改作业
export function updateAssignment(data: EduAssignment): Promise<AjaxResult> {
  return request({
    url: '/edu/assignments',
    method: 'put',
    data: data
  })
}

// 删除作业（assignmentIds 支持单值、字符串或数组，逗号分隔提交）
export function delAssignment(assignmentIds: number | string | Array<number | string>): Promise<AjaxResult> {
  return request({
    url: '/edu/assignments/' + assignmentIds,
    method: 'delete'
  })
}

// 查询提交列表（教师看本课程，学生看本人）
export function listSubmission(query?: SubmissionQuery): Promise<TableDataInfo<EduSubmission>> {
  return request({
    url: '/edu/submissions/list',
    method: 'get',
    params: query
  })
}

// 学生提交作业，body: { assignmentId, content, attachmentUrl }
export function addSubmission(data: EduSubmission): Promise<AjaxResult> {
  return request({
    url: '/edu/submissions',
    method: 'post',
    data: data
  })
}

// 教师评分，body: { submissionId, grade, feedback }
export function gradeSubmission(data: EduSubmission): Promise<AjaxResult> {
  return request({
    url: '/edu/submissions/grade',
    method: 'put',
    data: data
  })
}
