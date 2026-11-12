import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type {
  CreditProgress,
  EduCourseOffering,
  EduCourseRecord,
  EduEnrollment,
  EduStudent,
  EduTerm,
  OfferingQuery,
  RecordQuery,
  TermQuery
} from '@/types/education'

// 查询学期列表
export function listTerm(query?: TermQuery): Promise<TableDataInfo<EduTerm>> {
  return request({
    url: '/edu/terms/list',
    method: 'get',
    params: query
  })
}

// 查询当前学期
export function getCurrentTerm(): Promise<AjaxResult<EduTerm>> {
  return request({
    url: '/edu/terms/current',
    method: 'get'
  })
}

// 查询开课列表
export function listOffering(query?: OfferingQuery): Promise<TableDataInfo<EduCourseOffering>> {
  return request({
    url: '/edu/offerings/list',
    method: 'get',
    params: query
  })
}

// 查询当前教师所授课程（含学生数、作业数）
export function listMyOffering(query?: OfferingQuery): Promise<TableDataInfo<EduCourseOffering>> {
  return request({
    url: '/edu/offerings/mine',
    method: 'get',
    params: query
  })
}

// 新增开课
export function addOffering(data: EduCourseOffering): Promise<AjaxResult> {
  return request({
    url: '/edu/offerings',
    method: 'post',
    data: data
  })
}

// 修改开课
export function updateOffering(data: EduCourseOffering): Promise<AjaxResult> {
  return request({
    url: '/edu/offerings',
    method: 'put',
    data: data
  })
}

// 查询当前学生学籍信息（学号、专业、入学年、书院）
export function getMyStudent(): Promise<AjaxResult<EduStudent>> {
  return request({
    url: '/edu/students/me',
    method: 'get'
  })
}

// 查询本人学分进度
export function getMyProgress(): Promise<AjaxResult<CreditProgress>> {
  return request({
    url: '/edu/students/me/progress',
    method: 'get'
  })
}

// 查询本人成绩单
export function listMyRecord(query?: RecordQuery): Promise<TableDataInfo<EduCourseRecord>> {
  return request({
    url: '/edu/students/me/records',
    method: 'get',
    params: query
  })
}

// 学生选课，body: { offeringId }；也接受直接传入开课ID
export function addEnrollment(offeringIdOrData: number | EduEnrollment): Promise<AjaxResult> {
  const data: EduEnrollment = typeof offeringIdOrData === 'number' ? { offeringId: offeringIdOrData } : offeringIdOrData
  return request({
    url: '/edu/enrollments',
    method: 'post',
    data: data
  })
}

// 退课
export function delEnrollment(enrollmentId: number | string): Promise<AjaxResult> {
  return request({
    url: '/edu/enrollments/' + enrollmentId,
    method: 'delete'
  })
}

// 管理员/教师修正成绩
export function updateRecord(data: EduCourseRecord): Promise<AjaxResult> {
  return request({
    url: '/edu/records',
    method: 'put',
    data: data
  })
}
