import request from '@/utils/request'
import type { AjaxResult } from '@/types'
import type { TranscriptRow } from '@/utils/transcript/parseTranscript'

/** 导入请求体（api-contract §10.1） */
export interface TranscriptImportRequest {
  /** 原始文件名，仅用于后端留痕 */
  fileName: string
  /** 前端解析出的课程行，上限 500 行 */
  rows: TranscriptRow[]
}

/** 被跳过的行（api-contract §10.2） */
export interface TranscriptSkippedDetail {
  courseCode: string
  termCode: string
  reason: string
}

/** 导入结果（api-contract §10.2） */
export interface TranscriptImportResult {
  /** 提交的行数 */
  rowCount: number
  /** 新增行数 */
  inserted: number
  /** 覆盖行数（仅覆盖 source = TRANSCRIPT_UPLOAD 的记录） */
  updated: number
  /** 跳过行数（已存在官方/演示记录，不覆盖） */
  skipped: number
  /** 课程库未收录的课程代码 */
  unknownCourses: string[]
  /** 未命中 edu_term 的学期代码 */
  unknownTerms: string[]
  /** 跳过原因明细 */
  skippedDetails?: TranscriptSkippedDetail[]
}

/**
 * 导入前端解析出的成绩单行。
 * 身份只取 JWT（后端按 token 找 edu_student.sys_user_id），前端不传 studentId。
 */
export function importTranscript(data: TranscriptImportRequest): Promise<AjaxResult<TranscriptImportResult>> {
  return request({
    url: '/edu/transcripts/import',
    method: 'post',
    data
  })
}
