import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type {
  CompetencyQuery,
  CourseCompetency,
  DashboardStat,
  SkillProfileItem
} from '@/types/education'

// 学习仪表盘聚合数据（学分进度、GPA 趋势、分类占比、规划与作业概览）
export function getDashboard(): Promise<AjaxResult<DashboardStat>> {
  return request({
    url: '/edu/analytics/dashboard',
    method: 'get'
  })
}

// 依据本人已修课程聚合技能画像
export function getSkillProfile(): Promise<AjaxResult<SkillProfileItem[]>> {
  return request({
    url: '/edu/analytics/skill-profile',
    method: 'get'
  })
}

// 学院能力池
export function getCompetencyPool(query?: CompetencyQuery): Promise<TableDataInfo<CourseCompetency>> {
  return request({
    url: '/edu/analytics/competency-pool',
    method: 'get',
    params: query
  })
}

// 单门课程的技能关联明细
export function getCourseCompetency(courseId: number): Promise<AjaxResult<CourseCompetency[]>> {
  return request({
    url: '/edu/analytics/course-competency',
    method: 'get',
    params: { courseId }
  })
}
