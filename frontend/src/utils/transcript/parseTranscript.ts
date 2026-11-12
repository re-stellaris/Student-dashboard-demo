/**
 * 成绩单解析（纯函数，无 DOM 依赖，可在 Node 里直接跑）
 *
 * 供 `views/student/transcript/index.vue` 使用：把用户上传的 Excel/CSV 成绩单
 * 解析成可直接提交给 `POST /edu/transcripts/import`（见 api-contract §10）的行。
 *
 * 设计要点：
 * 1. 表头自动定位 —— 不假设固定行号，扫描前若干行寻找「课程代码 + 成绩」表头；
 * 2. 学期归一化 —— `2022-23 Term 1` → `2022-23-1`、`Term 2` → `-2`、`Summer` → `-3`；
 * 3. 识别不到成绩表时抛 `TranscriptParseError`，页面直接把 message 展示给用户；
 * 4. PDF / 图片本轮不支持（旧版用 pdf.js + Tesseract OCR），直接抛出可读提示。
 */
import * as XLSX from 'xlsx'

/** 提交给后端的一行成绩（字段与 api-contract §10.1 一一对应） */
export interface TranscriptRow {
  /** 课程代码，如 CSC3100 */
  courseCode: string
  /** 课程名称 */
  courseTitle: string
  /** 归一化后的学期代码，如 2022-23-1 */
  termCode: string
  /** 学分 */
  units: number
  /** 字母成绩，如 A- */
  grade: string
  /** 百分制分数（Score %），缺省为 null */
  score: number | null
  /** 课程类别，如 Major / GenEd */
  category: string
  /** 是否通过 */
  passed: boolean
  /** 状态，如 Completed */
  status: string
}

/** 解析结果 */
export interface ParsedTranscript {
  /** 学生姓名（识别不到为 undefined） */
  studentName?: string
  /** 学号（识别不到为 undefined） */
  studentNo?: string
  /** 专业 / 主修（识别不到为 undefined） */
  major?: string
  /** 课程行（已按 课程代码 + 学期 去重） */
  rows: TranscriptRow[]
  /** 需要展示给用户的可读提示（不是错误） */
  warnings: string[]
}

/** PDF 提示文案（本轮不支持，需求指定文案） */
export const PDF_NOT_SUPPORTED = 'PDF 暂不支持，请上传 Excel 或 CSV'

/** 解析类错误：message 面向用户，页面可直接展示 */
export class TranscriptParseError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'TranscriptParseError'
  }
}

/* =====================================================================
 * 常量
 * ===================================================================== */

/** 支持的文件后缀 */
const SUPPORTED_FILE_PATTERN = /\.(xlsx|xls|csv)$/i
/** 表头最多往下扫描这么多行（前几行是 STUDENT INFORMATION / CREDIT BREAKDOWN 区块） */
const HEADER_SCAN_LIMIT = 60
/** 课程代码：CSC3100 / ENG2002S / MIS23012 / ITE1000 */
const COURSE_CODE_PATTERN = /^[A-Z]{2,6}\d{3,5}[A-Z]{0,3}$/
/** 不计入通过的字母成绩 */
const FAILED_GRADES = new Set(['F', 'FA', 'U', 'DI', 'W', 'I', 'IP', 'NR', 'NC', 'ABS', 'XF'])

/** 可识别的列 */
type ColumnKey = 'term' | 'code' | 'title' | 'units' | 'grade' | 'score' | 'category' | 'status'
type ColumnMap = Partial<Record<ColumnKey, number>>

/** 识别到的成绩表 */
interface DetectedTable {
  headerIndex: number
  columns: ColumnMap
  rows: unknown[][]
}

/* =====================================================================
 * 小工具
 * ===================================================================== */

function cellText(value: unknown): string {
  if (value === null || value === undefined) return ''
  return String(value).replace(/\s+/g, ' ').trim()
}

/** 元信息键名归一化：「Student ID:」→「studentid」 */
function metadataKey(value: unknown): string {
  return cellText(value)
    .toLowerCase()
    .replace(/[\s:：.·_\-]/g, '')
}

/** 课程代码归一化：去空白 + 大写 */
function compactCode(value: unknown): string {
  return cellText(value).replace(/\s+/g, '').toUpperCase()
}

/** 提取数字（「3 units」→ 3、「45.7」→ 45.7、「A-」→ null） */
function toNumber(value: unknown): number | null {
  const text = cellText(value).replace(/,/g, '')
  if (!text) return null
  const matched = text.match(/-?\d+(\.\d+)?/)
  if (!matched) return null
  const parsed = Number(matched[0])
  return Number.isFinite(parsed) ? parsed : null
}

function describeError(error: unknown): string {
  if (error instanceof Error && error.message) return error.message
  return String(error ?? '未知错误')
}

function valueAt(row: unknown[] | undefined, index: number | undefined): unknown {
  if (!row || index === undefined) return ''
  return row[index]
}

/* =====================================================================
 * 表头识别
 * ===================================================================== */

/** 把表头单元格映射成列标识；不认识的返回 null */
function matchColumn(text: string): ColumnKey | null {
  const value = text.toLowerCase()
  if (!value) return null
  if (/^(course\s*(code|no\.?|id|number)|code|courseid|课程代码|课程编号|科目代码)$/.test(value)) return 'code'
  if (/^(course\s*(title|name)|title|name|course|课程名称|课程名|课程)$/.test(value)) return 'title'
  if (/^(semester|term|term\s*code|academic\s*term|学期|学年学期)$/.test(value)) return 'term'
  if (/^(units?|credits?|credit\s*hours?|unit\s*taken|学分)$/.test(value)) return 'units'
  if (/^(grade|letter\s*grade|letter|成绩|等级)$/.test(value)) return 'grade'
  if (/^(score(\s*%)?|mark(\s*%)?|percentage|score\s*percent|分数)$/.test(value)) return 'score'
  if (/^(category|area|course\s*type|course\s*category|类别|模块)$/.test(value)) return 'category'
  if (/^(status|result|course\s*status|状态)$/.test(value)) return 'status'
  return null
}

/** 在前若干行里找成绩表表头（必须同时有课程代码列与成绩列） */
function detectTable(rows: unknown[][]): DetectedTable | null {
  const limit = Math.min(rows.length, HEADER_SCAN_LIMIT)
  for (let index = 0; index < limit; index += 1) {
    const row = rows[index]
    if (!row || !row.length) continue
    const columns: ColumnMap = {}
    row.forEach((cell, cellIndex) => {
      const key = matchColumn(cellText(cell))
      if (key && columns[key] === undefined) columns[key] = cellIndex
    })
    if (columns.code === undefined || columns.grade === undefined) continue
    // 至少 3 列可识别，避免把零星单元格误判成表头
    if (Object.keys(columns).length < 3) continue
    return { headerIndex: index, columns, rows }
  }
  return null
}

/* =====================================================================
 * 学期归一化
 * ===================================================================== */

/** 学期名称 → 学期序号：Fall/Term 1 → 1、Spring/Term 2 → 2、Summer → 3 */
export function termIndexOf(text: string): number {
  const value = String(text ?? '').toLowerCase()
  if (!value) return 0
  if (/summer|夏季|夏日|暑期|term\s*3|semester\s*3|sem\s*3/.test(value)) return 3
  if (/spring|春季|term\s*2|semester\s*2|sem\s*2/.test(value)) return 2
  if (/fall|autumn|秋季|term\s*1|semester\s*1|sem\s*1/.test(value)) return 1
  return 0
}

function pad2(value: number): string {
  return String(Math.abs(value)).padStart(2, '0')
}

/**
 * 学期归一化：
 * - `2022-23 Term 1` → `2022-23-1`
 * - `2022-23 Term 2` → `2022-23-2`
 * - `2023-24 Summer` → `2023-24-3`
 * - 已规范的 `2022-23-1` 原样返回
 * - 只有学期名的 `Term 2` / `Summer` → `-2` / `-3`
 * - 完全识别不到学期时返回原文（页面/后端会把它计入 unknownTerms）
 */
export function normalizeTermCode(raw: string | null | undefined): string {
  const text = String(raw ?? '').replace(/\s+/g, ' ').trim()
  if (!text) return ''

  // 已经是规范形式
  const canonical = text.match(/^(\d{4})\s*[-–—]\s*(\d{2})\s*[-–—]\s*([1-3])$/)
  if (canonical) return `${canonical[1]}-${canonical[2]}-${canonical[3]}`

  // 短码：F23 / S24 / SUM24（与 dashboard 的手动录入学期限定列表一致）
  const short = text.match(/^(F|S|SUM)\s*(\d{2})$/i)
  if (short) {
    const flag = short[1].toUpperCase()
    const year = Number(short[2])
    if (flag === 'F') return `20${pad2(year)}-${pad2(year + 1)}-1`
    if (flag === 'S') return `20${pad2(year - 1)}-${pad2(year)}-2`
    return `20${pad2(year - 1)}-${pad2(year)}-3`
  }

  // 学年：2022-23 / 2022-2023 / 2022–23
  let academicYear = ''
  const yearRange = text.match(/(\d{4})\s*[-–—/]\s*(\d{2,4})/)
  if (yearRange) {
    const start = Number(yearRange[1])
    const endText = yearRange[2]
    const end = endText.length === 4 ? Number(endText) : Number(String(Math.floor(start / 100)) + endText)
    academicYear = `${start}-${pad2(end % 100)}`
  } else {
    const singleYear = text.match(/(20\d{2})/)
    if (singleYear) {
      const start = Number(singleYear[1])
      academicYear = `${start}-${pad2((start + 1) % 100)}`
    }
  }

  const termIndex = termIndexOf(text)
  if (!termIndex) return text
  if (!academicYear) return `-${termIndex}`
  return `${academicYear}-${termIndex}`
}

/* =====================================================================
 * 行解析
 * ===================================================================== */

function isPassedGrade(grade: string): boolean {
  if (!grade) return false
  return !FAILED_GRADES.has(grade)
}

/** 从识别到的表里逐行读取课程 */
function readCourseRows(table: DetectedTable, warnings: string[]): TranscriptRow[] {
  const { columns, rows, headerIndex } = table
  const out: TranscriptRow[] = []
  const seen = new Set<string>()
  let duplicates = 0
  let unstampedTerms = 0
  let failedCount = 0

  for (let index = headerIndex + 1; index < rows.length; index += 1) {
    const row = rows[index]
    if (!row || row.every((cell) => cellText(cell) === '')) continue

    const courseCode = compactCode(valueAt(row, columns.code))
    // 汇总区（SEMESTER SUMMARY）等非课程行会被这里挡掉
    if (!COURSE_CODE_PATTERN.test(courseCode)) {
      failedCount += 1
      continue
    }

    const rawTerm = cellText(valueAt(row, columns.term))
    const termCode = normalizeTermCode(rawTerm)
    if (rawTerm && !/^\d{4}-\d{2}-[1-3]$/.test(termCode)) unstampedTerms += 1

    const grade = cellText(valueAt(row, columns.grade)).toUpperCase()
    const passed = isPassedGrade(grade)
    const status = cellText(valueAt(row, columns.status)) || (passed ? 'Completed' : 'In Progress')

    const key = `${courseCode}|${termCode}`
    if (seen.has(key)) {
      duplicates += 1
      continue
    }
    seen.add(key)

    out.push({
      courseCode,
      courseTitle: cellText(valueAt(row, columns.title)),
      termCode,
      units: toNumber(valueAt(row, columns.units)) ?? 0,
      grade,
      score: toNumber(valueAt(row, columns.score)),
      category: cellText(valueAt(row, columns.category)),
      passed,
      status
    })
  }

  if (duplicates > 0) {
    warnings.push(`已合并 ${duplicates} 行重复记录（同一课程 + 学期只保留一条）`)
  }
  if (unstampedTerms > 0) {
    warnings.push(`有 ${unstampedTerms} 行的学期无法归一化成 学年-学期 形式，已按原文保留（后端会记为未知学期）`)
  }
  if (!out.length && failedCount > 0) {
    warnings.push(`已定位表头，但 ${failedCount} 行都没读出课程代码，请检查表头列是否对应正确`)
  }
  return out
}

/** 从任意「键: 值」单元格对里取学生信息（旧版 parseWorkbook 的同一套策略） */
function extractStudentInfo(
  rows: unknown[][],
  headerIndex: number
): { studentName?: string; studentNo?: string; major?: string } {
  const info: { studentName?: string; studentNo?: string; major?: string } = {}
  for (let index = 0; index < rows.length; index += 1) {
    if (index === headerIndex) continue
    const row = rows[index]
    if (!row) continue
    for (let cellIndex = 0; cellIndex < row.length - 1; cellIndex += 1) {
      const key = metadataKey(row[cellIndex])
      if (!key) continue
      let value = ''
      for (let next = cellIndex + 1; next < row.length; next += 1) {
        const candidate = cellText(row[next])
        if (candidate) {
          value = candidate
          break
        }
      }
      if (!value) continue
      if (info.studentName === undefined && /^(student)?name$/.test(key)) {
        info.studentName = value
        continue
      }
      if (info.studentNo === undefined && /^(student)?(id|idno|no|number)$/.test(key)) {
        info.studentNo = value
        continue
      }
      if (info.major === undefined && /^(major|programme|program|majorprogramme|majorname|主修|专业)$/.test(key)) {
        info.major = value
      }
    }
  }
  return info
}

/* =====================================================================
 * 入口
 * ===================================================================== */

/** 读工作簿（ArrayBuffer 走二进制；string 走 CSV 文本） */
function readWorkbook(data: ArrayBuffer | string): XLSX.WorkBook {
  if (typeof data === 'string') {
    if (!data.trim()) throw new TranscriptParseError('文件内容为空，请重新上传成绩单')
    try {
      return XLSX.read(data, { type: 'string' })
    } catch (error) {
      throw new TranscriptParseError('CSV 解析失败：' + describeError(error))
    }
  }
  // 兼容调用方直接传 Uint8Array（Node Buffer 亦然）
  const bytes = new Uint8Array(data as ArrayBuffer)
  if (!bytes.byteLength) throw new TranscriptParseError('文件内容为空，请重新上传成绩单')
  try {
    return XLSX.read(bytes, { type: 'array' })
  } catch (error) {
    throw new TranscriptParseError('Excel 解析失败：' + describeError(error))
  }
}

/** 读取一个工作表的二维数组（raw:false → 数字按显示文本给，便于统一清洗） */
function readSheetRows(sheet: XLSX.WorkSheet): unknown[][] {
  const rows = XLSX.utils.sheet_to_json<unknown[]>(sheet, {
    header: 1,
    defval: '',
    raw: false,
    blankrows: true
  })
  return Array.isArray(rows) ? rows : []
}

/**
 * 解析成绩单工作簿。
 *
 * @param data Excel 二进制（.xlsx/.xls）或 CSV 文本
 * @param fileName 原始文件名（用于类型判断与错误提示）
 * @throws TranscriptParseError 文件类型不支持 / 识别不到成绩表 / 没有有效课程行
 */
export function parseTranscriptWorkbook(data: ArrayBuffer | string, fileName: string): ParsedTranscript {
  const name = String(fileName ?? '').trim()
  if (/\.pdf$/i.test(name)) throw new TranscriptParseError(PDF_NOT_SUPPORTED)
  if (name && !SUPPORTED_FILE_PATTERN.test(name)) {
    throw new TranscriptParseError(`仅支持 Excel（.xlsx / .xls）或 CSV 文件，当前文件：${name}`)
  }

  const workbook = readWorkbook(data)
  const sheetNames = Array.isArray(workbook.SheetNames) ? workbook.SheetNames : []
  if (!sheetNames.length) throw new TranscriptParseError('文件里没有可读取的工作表')

  const warnings: string[] = []
  const tables: Array<{ sheetName: string; table: DetectedTable }> = []

  for (const sheetName of sheetNames) {
    const sheet = workbook.Sheets[sheetName]
    if (!sheet) continue
    const rows = readSheetRows(sheet)
    if (!rows.length) continue
    const table = detectTable(rows)
    if (table) tables.push({ sheetName, table })
  }

  if (!tables.length) {
    throw new TranscriptParseError(
      '未能识别成绩表：请确认文件里存在「Semester / Course Code / Course Title / Units / Grade」这类表头'
    )
  }

  const chosen = tables[0]
  if (tables.length > 1) {
    warnings.push(`工作簿含 ${tables.length} 个可识别的成绩表，已导入第一个「${chosen.sheetName}」`)
  }

  const rows = readCourseRows(chosen.table, warnings)
  if (!rows.length) {
    throw new TranscriptParseError(`工作表「${chosen.sheetName}」里没有读到有效课程行，请检查表头与数据`)
  }

  const info = extractStudentInfo(chosen.table.rows, chosen.table.headerIndex)
  if (!info.studentName) warnings.push('未识别学生姓名，可在预览里手动补全')
  if (!info.studentNo) warnings.push('未识别学号，可在预览里手动补全')
  if (!info.major) warnings.push('未识别专业，可在预览里手动补全')

  return {
    studentName: info.studentName,
    studentNo: info.studentNo,
    major: info.major,
    rows,
    warnings
  }
}

export default parseTranscriptWorkbook
