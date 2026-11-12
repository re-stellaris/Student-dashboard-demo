import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type {
  CurriculumQuery,
  EduCurriculum,
  EduCurriculumItem,
  EduMajor,
  MajorQuery
} from '@/types/education'

// 查询专业列表
export function listMajor(query?: MajorQuery): Promise<TableDataInfo<EduMajor>> {
  return request({
    url: '/edu/majors/list',
    method: 'get',
    params: query
  })
}

// 查询培养方案列表
export function listCurriculum(query?: CurriculumQuery): Promise<TableDataInfo<EduCurriculum>> {
  return request({
    url: '/edu/curricula/list',
    method: 'get',
    params: query
  })
}

// 查询培养方案详细（含 items）
export function getCurriculum(curriculumId: number): Promise<AjaxResult<EduCurriculum>> {
  return request({
    url: '/edu/curricula/' + curriculumId,
    method: 'get'
  })
}

// 按专业 + 入学年份解析适用版本；data 为 null 表示无匹配版本
// 支持两种调用方式：getCurriculumByMajor({ majorId, entryYear }) 或 getCurriculumByMajor(majorId, entryYear)
export function getCurriculumByMajor(params: { majorId: number; entryYear: number }): Promise<AjaxResult<EduCurriculum | null>>
export function getCurriculumByMajor(majorId: number, entryYear: number): Promise<AjaxResult<EduCurriculum | null>>
export function getCurriculumByMajor(
  paramsOrMajorId: { majorId: number; entryYear: number } | number,
  entryYear?: number
): Promise<AjaxResult<EduCurriculum | null>> {
  const majorId = typeof paramsOrMajorId === 'number' ? paramsOrMajorId : paramsOrMajorId.majorId
  const year = typeof paramsOrMajorId === 'number' ? entryYear : paramsOrMajorId.entryYear
  return request({
    url: '/edu/curricula/by-major',
    method: 'get',
    params: { majorId, entryYear: year }
  })
}

// 新增培养方案
export function addCurriculum(data: EduCurriculum): Promise<AjaxResult> {
  return request({
    url: '/edu/curricula',
    method: 'post',
    data: data
  })
}

// 修改培养方案
export function updateCurriculum(data: EduCurriculum): Promise<AjaxResult> {
  return request({
    url: '/edu/curricula',
    method: 'put',
    data: data
  })
}

// 删除培养方案（curriculumIds 支持单值、字符串或数组，逗号分隔提交）
export function delCurriculum(curriculumIds: number | string | Array<number | string>): Promise<AjaxResult> {
  return request({
    url: '/edu/curricula/' + curriculumIds,
    method: 'delete'
  })
}

// 覆盖式更新方案条目
export function updateCurriculumItems(curriculumId: number, data: EduCurriculumItem[]): Promise<AjaxResult> {
  return request({
    url: '/edu/curricula/' + curriculumId + '/items',
    method: 'put',
    data: data
  })
}
