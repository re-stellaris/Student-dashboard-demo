/**
 * 本地技能画像存储（IndexedDB / Dexie.js）
 * =====================================================================
 * 这是**唯一**保存个人画像（自评星级、笔记、自定义标签）的地方。
 * 严禁把这里的任何内容 POST 到后端：服务端只提供公共技能标签与课程关联度，
 * 不存储明文个人画像（见 docs/architecture/api-contract.md §6 与
 * ADR-0004-local-first-skill-profile.md）。
 *
 * 风险提示：IndexedDB 属于「清除浏览器数据」的清理范围（隐私模式、换设备、
 * 换浏览器同样会丢失），因此页面必须提供显式「备份导出 / 备份恢复」入口。
 *
 * 主键约定：profiles 表以字符串 id 为主键；默认按当前登录用户区分，
 * 形如 `self-assessment:stu_chen`，未登录时退化为 `self-assessment`，
 * 避免同一浏览器多账号互相覆盖。
 */
import Dexie, { type Table } from 'dexie'
import useUserStore from '@/store/modules/user'

/** 本地画像数据结构版本；导入备份时用于兼容旧格式 */
export const PROFILE_SCHEMA_VERSION = 1

/** 画像主键前缀 */
const PROFILE_ID_PREFIX = 'self-assessment'

/** 单条自评记录（一项技能标签 + 1~5 星） */
export interface SkillSelfRating {
  /** 稳定业务主键：tag:<skillTagId> 或 custom:<标签名> */
  key: string
  /** 对应的公共技能标签ID（自定义标签为空） */
  skillTagId?: number
  /** 技能标签名称 */
  name: string
  /** 技能分类 */
  category?: string
  /** 自评星级（0~5，0 表示未评价） */
  score: number
  /** 备注 */
  note?: string
}

/** profiles 表的一行：一位用户在本地浏览器中的完整自评画像 */
export interface SkillProfileRecord {
  /** 主键，见文件头「主键约定」 */
  id: string
  /** 画像归属的登录用户名 */
  owner?: string
  /** 自评条目 */
  ratings: SkillSelfRating[]
  /** 自定义标签 */
  tags: string[]
  /** 整体备注 */
  note?: string
  /** 最后保存时间（ISO 字符串） */
  updatedAt?: string
  /** 数据结构版本 */
  schemaVersion: number
}

/** 导出文件结构（exportProfile 生成的 JSON） */
export interface SkillProfileExport {
  /** 数据结构版本 */
  schemaVersion: number
  /** 导出时间（ISO 字符串） */
  exportedAt: string
  /** 画像内容，未保存过时为 null */
  profile: SkillProfileRecord | null
}

/** saveProfile 入参：允许只提交部分字段（其余沿用已保存的值） */
export type SkillProfileInput = Partial<Omit<SkillProfileRecord, 'id' | 'updatedAt' | 'schemaVersion'>>

/** Dexie 数据库定义 */
class SkillProfileDatabase extends Dexie {
  /** 画像表，主键为字符串 id */
  profiles!: Table<SkillProfileRecord, string>

  constructor() {
    super('edu_skill_profile')
    this.version(PROFILE_SCHEMA_VERSION).stores({
      profiles: 'id, updatedAt'
    })
  }
}

/** 全局唯一的本地画像数据库实例 */
export const skillProfileDb = new SkillProfileDatabase()

/** 当前登录用户名（取不到时返回空串） */
function resolveOwnerName(): string {
  try {
    const userStore = useUserStore()
    return userStore.name || ''
  } catch {
    return ''
  }
}

/** 当前用户的画像主键 */
export function getProfileId(): string {
  const owner = resolveOwnerName()
  return owner ? `${PROFILE_ID_PREFIX}:${owner}` : PROFILE_ID_PREFIX
}

function describeError(error: unknown): string {
  if (error instanceof Error) return error.message
  return String(error)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

/** 归一化星级：非数字按 0，四舍五入到 0.5 星并截断到 0~5 */
function normalizeScore(value: unknown): number {
  const num = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(num)) return 0
  return Math.min(5, Math.max(0, Math.round(num * 2) / 2))
}

/** 归一化自评条目：去掉非法项、按 key 去重 */
function normalizeRatings(input: unknown): SkillSelfRating[] {
  if (!Array.isArray(input)) return []
  const seen = new Set<string>()
  const result: SkillSelfRating[] = []
  input.forEach((raw) => {
    if (!isRecord(raw)) return
    const skillTagId = typeof raw.skillTagId === 'number' && Number.isFinite(raw.skillTagId) ? raw.skillTagId : undefined
    const rawName = typeof raw.name === 'string' ? raw.name.trim() : ''
    const key = typeof raw.key === 'string' && raw.key
      ? raw.key
      : (skillTagId !== undefined ? `tag:${skillTagId}` : `custom:${rawName}`)
    if (!key || seen.has(key)) return
    if (!rawName && skillTagId === undefined) return
    seen.add(key)
    const rating: SkillSelfRating = {
      key,
      name: rawName || `技能标签 ${skillTagId}`,
      score: normalizeScore(raw.score)
    }
    if (skillTagId !== undefined) rating.skillTagId = skillTagId
    if (typeof raw.category === 'string' && raw.category) rating.category = raw.category
    if (typeof raw.note === 'string' && raw.note) rating.note = raw.note
    result.push(rating)
  })
  return result
}

/** 归一化自定义标签：去空、去重、保留原始顺序 */
function normalizeTags(input: unknown): string[] {
  if (!Array.isArray(input)) return []
  const seen = new Set<string>()
  const result: string[] = []
  input.forEach((raw) => {
    if (typeof raw !== 'string') return
    const tag = raw.trim()
    if (!tag || seen.has(tag)) return
    seen.add(tag)
    result.push(tag)
  })
  return result
}

/**
 * 保存（新增或覆盖）当前用户的本地画像。
 * 只提交部分字段时，未提交的字段沿用已保存的值。
 */
export async function saveProfile(data: SkillProfileInput): Promise<SkillProfileRecord> {
  const id = getProfileId()
  try {
    const existing = await skillProfileDb.profiles.get(id)
    const record: SkillProfileRecord = {
      id,
      owner: resolveOwnerName() || existing?.owner || '',
      ratings: normalizeRatings(data.ratings !== undefined ? data.ratings : existing?.ratings),
      tags: normalizeTags(data.tags !== undefined ? data.tags : existing?.tags),
      note: data.note !== undefined ? data.note : (existing?.note ?? ''),
      updatedAt: new Date().toISOString(),
      schemaVersion: PROFILE_SCHEMA_VERSION
    }
    await skillProfileDb.profiles.put(record)
    return record
  } catch (error) {
    throw new Error('保存本地画像失败：' + describeError(error))
  }
}

/** 读取当前用户的本地画像；从未保存过时返回 undefined */
export async function loadProfile(): Promise<SkillProfileRecord | undefined> {
  try {
    return await skillProfileDb.profiles.get(getProfileId())
  } catch (error) {
    throw new Error('读取本地画像失败：' + describeError(error))
  }
}

/**
 * 导出当前画像为 JSON Blob（可直接用 file-saver 的 saveAs 下载）。
 * 导出文件包含 schemaVersion 与 exportedAt，便于日后迁移与追溯。
 */
export async function exportProfile(): Promise<Blob> {
  const profile = (await loadProfile()) ?? null
  const payload: SkillProfileExport = {
    schemaVersion: PROFILE_SCHEMA_VERSION,
    exportedAt: new Date().toISOString(),
    profile
  }
  return new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
}

/** 从备份文件内容中提取可保存的画像字段 */
function extractProfileInput(parsed: unknown): SkillProfileInput | null {
  if (!isRecord(parsed)) return null
  const candidate = isRecord(parsed.profile) ? parsed.profile : parsed
  const hasRatings = Array.isArray(candidate.ratings)
  const hasTags = Array.isArray(candidate.tags)
  if (!hasRatings && !hasTags) return null
  const input: SkillProfileInput = {
    ratings: normalizeRatings(candidate.ratings),
    tags: normalizeTags(candidate.tags)
  }
  if (typeof candidate.note === 'string') input.note = candidate.note
  if (typeof candidate.owner === 'string' && candidate.owner) input.owner = candidate.owner
  return input
}

/**
 * 从备份恢复本地画像（覆盖当前用户已有画像）。
 * @param json JSON 字符串，或用户选择的 File / Blob
 */
export async function importProfile(json: string | Blob): Promise<SkillProfileRecord> {
  const text = typeof json === 'string' ? json : await json.text()
  if (!text.trim()) throw new Error('备份内容为空')
  let parsed: unknown
  try {
    parsed = JSON.parse(text)
  } catch {
    throw new Error('备份文件不是合法的 JSON')
  }
  const input = extractProfileInput(parsed)
  if (!input) throw new Error('备份文件格式不正确：未找到本地技能画像数据')
  return await saveProfile(input)
}

/** 清空当前用户的本地画像（不影响其他账号在同一浏览器中的画像） */
export async function clearProfile(): Promise<void> {
  try {
    await skillProfileDb.profiles.delete(getProfileId())
  } catch (error) {
    throw new Error('清空本地画像失败：' + describeError(error))
  }
}
