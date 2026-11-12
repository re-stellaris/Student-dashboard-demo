import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type {
  EduPlanDetail,
  EduPlanItemInput,
  EduStudyPlan,
  PlanQuery,
  PlanRecommendBody,
  PlanValidateBody,
  PlanValidationResult,
  RecommendedCourse
} from '@/types/education'

// 查询规划列表（学生仅本人）
export function listPlan(query?: PlanQuery): Promise<TableDataInfo<EduStudyPlan>> {
  return request({
    url: '/edu/plans/list',
    method: 'get',
    params: query
  })
}

// 查询规划详情 { plan, items }
export function getPlan(planId: number): Promise<AjaxResult<EduPlanDetail>> {
  return request({
    url: '/edu/plans/' + planId,
    method: 'get'
  })
}

// 新建草稿规划，body: { termId, title }
export function addPlan(data: EduStudyPlan): Promise<AjaxResult<EduStudyPlan>> {
  return request({
    url: '/edu/plans',
    method: 'post',
    data: data
  })
}

// 覆盖式保存规划课程（保存即重新校验并写入 reason）
export function updatePlanItems(planId: number, data: EduPlanItemInput[]): Promise<AjaxResult> {
  return request({
    url: '/edu/plans/' + planId + '/items',
    method: 'put',
    data: data
  })
}

// 不落库的试算校验
export function validatePlan(data: PlanValidateBody): Promise<AjaxResult<PlanValidationResult>> {
  return request({
    url: '/edu/plans/validate',
    method: 'post',
    data: data
  })
}

// 校验并持久化校验结果
export function validateSavedPlan(planId: number): Promise<AjaxResult<PlanValidationResult>> {
  return request({
    url: '/edu/plans/' + planId + '/validate',
    method: 'post'
  })
}

// 规则推荐
export function recommendPlan(data: PlanRecommendBody): Promise<AjaxResult<RecommendedCourse[]>> {
  return request({
    url: '/edu/plans/recommend',
    method: 'post',
    data: data
  })
}

// 提交规划（DRAFT → SUBMITTED，提交前强制校验）
export function submitPlan(planId: number): Promise<AjaxResult> {
  return request({
    url: '/edu/plans/' + planId + '/submit',
    method: 'post'
  })
}

// 删除规划（planIds 支持单值、字符串或数组，逗号分隔提交）
export function delPlan(planIds: number | string | Array<number | string>): Promise<AjaxResult> {
  return request({
    url: '/edu/plans/' + planIds,
    method: 'delete'
  })
}
