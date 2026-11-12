import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type {
  CourseQuery,
  EduCourse,
  EduCourseOption,
  EduCoursePrereq,
  PrereqCheckResult
} from '@/types/education'

// 查询课程列表
export function listCourse(query?: CourseQuery): Promise<TableDataInfo<EduCourse>> {
  return request({
    url: '/edu/courses/list',
    method: 'get',
    params: query
  })
}

// 查询课程详细
export function getCourse(courseId: number | string): Promise<AjaxResult<EduCourse>> {
  return request({
    url: '/edu/courses/' + courseId,
    method: 'get'
  })
}

// 按课程代码查询（转录 / 规划补全用）
export function getCourseByCode(code: string): Promise<AjaxResult<EduCourse>> {
  return request({
    url: '/edu/courses/byCode/' + code,
    method: 'get'
  })
}

// 新增课程
export function addCourse(data: EduCourse): Promise<AjaxResult> {
  return request({
    url: '/edu/courses',
    method: 'post',
    data: data
  })
}

// 修改课程
export function updateCourse(data: EduCourse): Promise<AjaxResult> {
  return request({
    url: '/edu/courses',
    method: 'put',
    data: data
  })
}

// 删除课程（courseIds 支持单值、字符串或数组，逗号分隔提交）
export function delCourse(courseIds: number | string | Array<number | string>): Promise<AjaxResult> {
  return request({
    url: '/edu/courses/' + courseIds,
    method: 'delete'
  })
}

// 查询课程筛选选项（schools / categories / levels）
export function getCourseOptions(): Promise<AjaxResult<EduCourseOption>> {
  return request({
    url: '/edu/courses/options',
    method: 'get'
  })
}

// 查询课程先修关系
export function listPrereq(courseId: number): Promise<AjaxResult<EduCoursePrereq[]>> {
  return request({
    url: '/edu/courses/' + courseId + '/prereqs',
    method: 'get'
  })
}

// 覆盖式更新先修关系，body: [{ prereqCourseId, groupNo, rawText }]
export function updatePrereq(courseId: number, data: EduCoursePrereq[]): Promise<AjaxResult> {
  return request({
    url: '/edu/courses/' + courseId + '/prereqs',
    method: 'put',
    data: data
  })
}

// 以当前学生已修课程判断先修是否满足
export function checkPrereq(courseId: number): Promise<AjaxResult<PrereqCheckResult>> {
  return request({
    url: '/edu/courses/' + courseId + '/prereq-check',
    method: 'get'
  })
}
