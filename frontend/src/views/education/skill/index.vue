<template>
  <div class="app-container edu-skill">
    <!-- 页面标题区 -->
    <div class="lg-head">
      <div>
        <h2>技能画像</h2>
        <p>依据已修课程聚合技能画像，并在本机自评、记录笔记与备份个人技能数据。</p>
      </div>
    </div>

    <!-- 隐私与本地存储风险提示（必须始终可见） -->
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
      title="技能自评只保存在本机浏览器，不会上传服务器"
      description="自评星级、笔记与自定义标签全部存放在浏览器 IndexedDB 中。清理浏览器数据、使用无痕模式、更换浏览器或更换设备都会导致画像丢失，请定期点击「备份导出」保存 JSON 文件。"
    />

    <el-row :gutter="12">
      <!-- 服务端聚合画像 -->
      <el-col :xs="24" :lg="13">
        <el-card v-loading="serverLoading" shadow="never" class="skill-card">
          <template #header>
            <div class="skill-card__header">
              <span>课程技能画像</span>
              <span class="skill-card__hint">依据本人已修课程聚合，来自教务数据</span>
            </div>
          </template>

          <el-alert v-if="serverError" type="error" :closable="false" show-icon class="mb8">
            <template #title>
              <span>{{ serverError }}</span>
              <el-button type="primary" link class="ml8" @click="loadServerProfile">重新加载</el-button>
            </template>
          </el-alert>

          <div v-if="hasRadar" ref="radarRef" class="radar-box" />
          <el-empty
            v-else-if="!serverLoading && !serverError"
            description="暂无技能画像数据，请先完成课程学习后再查看"
            :image-size="80"
          />

          <div v-if="sortedProfile.length > 0" class="skill-tags">
            <el-tooltip
              v-for="item in sortedProfile"
              :key="item.skillTagId ?? item.name"
              :content="item.rationale || '暂无判断依据说明'"
              placement="top"
            >
              <el-tag class="skill-tags__item" type="primary" effect="light">
                {{ item.name }}
                <span class="skill-tags__score">{{ formatScore(item.score) }}</span>
              </el-tag>
            </el-tooltip>
          </div>
          <div v-if="sortedProfile.length > 0" class="skill-meta">
            共 {{ sortedProfile.length }} 项技能标签，同标签关联课程数见下方自评列表说明。
          </div>
        </el-card>
      </el-col>

      <!-- 本地自评 -->
      <el-col :xs="24" :lg="11">
        <el-card shadow="never" class="skill-card">
          <template #header>
            <div class="skill-card__header">
              <span>本地技能自评</span>
              <el-tag type="info" size="small" effect="plain">仅本机可见</el-tag>
            </div>
          </template>

          <el-alert v-if="localError" type="error" :closable="false" show-icon class="mb8" :title="localError" />

          <div class="local-meta">
            <span>归属：{{ localOwner || '未登录用户' }}</span>
            <span>最近保存：{{ formatDateTime(savedAt) }}</span>
            <span>已评 {{ ratedCount }} / {{ ratingRows.length }} 项，平均 {{ formatScore(selfAverage) }} 星</span>
          </div>

          <div v-if="ratingRows.length === 0" class="local-empty">
            <el-empty :image-size="70" description="暂无可自评的技能标签，可先添加自定义标签" />
          </div>

          <div v-else class="rating-list">
            <div v-for="row in ratingRows" :key="row.key" class="rating-item">
              <div class="rating-item__head">
                <span class="rating-item__name">
                  {{ row.name }}
                  <el-tag v-if="row.source === 'custom'" size="small" type="info" effect="plain">自定义</el-tag>
                </span>
                <el-rate
                  :model-value="ratingMap[row.key] || 0"
                  :max="5"
                  allow-half
                  @change="setRating(row, $event)"
                />
              </div>
              <div v-if="row.source === 'custom'" class="rating-item__ops">
                <el-button link type="danger" size="small" @click="removeCustomTag(row)">删除标签</el-button>
              </div>
            </div>
          </div>

          <div class="custom-tag">
            <el-input
              v-model="newTag"
              placeholder="添加自定义技能标签，如 数据分析"
              maxlength="40"
              clearable
              @keyup.enter="addCustomTag"
            />
            <el-button type="primary" plain @click="addCustomTag">添加</el-button>
          </div>

          <div class="note-box">
            <div class="note-box__label">自评笔记</div>
            <el-input
              v-model="note"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="记录你的技能积累、项目经历或待提升方向（仅保存在本机）"
            />
          </div>

          <div class="local-actions">
            <el-button type="primary" :loading="localSaving" @click="handleSave">保存自评</el-button>
            <el-button plain @click="handleExport">备份导出</el-button>
            <el-button plain @click="triggerImport">备份恢复</el-button>
            <el-button type="danger" plain @click="handleClear">清空本地画像</el-button>
          </div>
          <div class="local-tip">
            备份文件为 JSON 格式，可保存到网盘或 U 盘；恢复时会覆盖当前账号在本机的自评数据。清空操作不可撤销。
          </div>
          <input
            ref="importInputRef"
            type="file"
            accept=".json,application/json"
            class="hidden-input"
            @change="handleImportFile"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="EduSkill">
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { saveAs } from 'file-saver'
import { getSkillProfile } from '@/api/education/analytics'
import {
  clearProfile,
  exportProfile,
  getProfileId,
  importProfile,
  loadProfile,
  saveProfile,
  type SkillSelfRating
} from '@/utils/education/skillProfileDb'
import type { SkillProfileItem } from '@/types/education'

type ChartInstance = ReturnType<typeof echarts.init>

interface SelfRatingRow {
  /** 稳定业务主键：tag:<skillTagId> 或 custom:<标签名> */
  key: string
  /** 公共技能标签ID（自定义标签为空） */
  skillTagId?: number
  /** 展示名称 */
  name: string
  /** 技能分类 */
  category?: string
  /** 来源：服务端公共标签 or 本地自定义标签 */
  source: 'server' | 'custom'
}

/* ------------------------------ 服务端画像 ------------------------------ */
const serverLoading = ref<boolean>(true)
const serverError = ref<string>('')
const serverProfile = ref<SkillProfileItem[]>([])
const radarRef = ref<HTMLDivElement | null>(null)
let radarChart: ChartInstance | null = null

/* ------------------------------ 本地自评 ------------------------------ */
const localOwner = ref<string>('')
const savedAt = ref<string>('')
const ratings = ref<SkillSelfRating[]>([])
const customTags = ref<string[]>([])
const note = ref<string>('')
const newTag = ref<string>('')
const localSaving = ref<boolean>(false)
const localError = ref<string>('')
const importInputRef = ref<HTMLInputElement | null>(null)

const sortedProfile = computed<SkillProfileItem[]>(() =>
  [...serverProfile.value].sort((a, b) => (b.score ?? 0) - (a.score ?? 0))
)

/** 雷达图最多展示 8 个能力维度，取关联度最高的标签 */
const radarItems = computed<SkillProfileItem[]>(() => sortedProfile.value.slice(0, 8))

/** 雷达图至少需要 3 个维度才有意义，否则只展示标签列表 */
const hasRadar = computed<boolean>(() => radarItems.value.length >= 3)

/** 自评星级查找表 */
const ratingMap = computed<Record<string, number>>(() => {
  const map: Record<string, number> = {}
  ratings.value.forEach((item) => {
    map[item.key] = item.score
  })
  return map
})

/** 自评列表：公共技能标签 + 本地自定义标签 */
const ratingRows = computed<SelfRatingRow[]>(() => {
  const rows: SelfRatingRow[] = []
  const seen = new Set<string>()
  serverProfile.value.forEach((item) => {
    const key = item.skillTagId !== undefined ? `tag:${item.skillTagId}` : `custom:${item.name ?? ''}`
    if (seen.has(key)) return
    seen.add(key)
    rows.push({
      key,
      skillTagId: item.skillTagId,
      name: item.name || key,
      category: item.category,
      source: 'server'
    })
  })
  customTags.value.forEach((tag) => {
    const key = `custom:${tag}`
    if (seen.has(key)) return
    seen.add(key)
    rows.push({ key, name: tag, source: 'custom' })
  })
  return rows
})

const ratedCount = computed<number>(() => ratings.value.filter((item) => item.score > 0).length)

const selfAverage = computed<number>(() => {
  const scored = ratings.value.filter((item) => item.score > 0)
  if (scored.length === 0) return 0
  const sum = scored.reduce((total, item) => total + item.score, 0)
  return Math.round((sum / scored.length) * 10) / 10
})

/** 数字格式化，缺省值显示占位符 */
function formatScore(value: number | null | undefined): string {
  if (value === null || value === undefined || !Number.isFinite(value)) return '—'
  return Number.isInteger(value) ? `${value}` : value.toFixed(1)
}

/** ISO 时间转本地可读时间 */
function formatDateTime(value?: string): string {
  if (!value) return '尚未保存'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

/** 把后端/网络异常转成可读文案 */
function describeError(error: unknown): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'string') return error
  return ''
}

/** 服务端技能画像雷达图 */
function renderRadarChart(): void {
  const container = radarRef.value
  if (!container) return
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
  const items = radarItems.value
  if (items.length === 0) return
  radarChart = echarts.init(container)
  radarChart.setOption({
    tooltip: { trigger: 'item' },
    radar: {
      indicator: items.map((item) => ({ name: item.name || '未命名技能', max: 10 })),
      radius: '64%',
      splitNumber: 4,
      axisName: { color: '#606266', fontSize: 12 }
    },
    series: [
      {
        type: 'radar',
        symbolSize: 5,
        data: [
          {
            name: '课程关联度',
            value: items.map((item) => item.score ?? 0),
            areaStyle: { opacity: 0.25 },
            lineStyle: { width: 2 }
          }
        ]
      }
    ]
  })
}

/** 加载服务端技能画像 */
async function loadServerProfile(): Promise<void> {
  serverLoading.value = true
  serverError.value = ''
  try {
    const response = await getSkillProfile()
    serverProfile.value = response?.data ?? []
    await nextTick()
    renderRadarChart()
  } catch (error) {
    const message = describeError(error)
    serverError.value = message || '技能画像加载失败，请稍后重试'
    serverProfile.value = []
  } finally {
    serverLoading.value = false
  }
}

/** 读取本地自评（唯一数据来源：IndexedDB） */
async function loadLocalProfile(): Promise<void> {
  try {
    const record = await loadProfile()
    localOwner.value = record?.owner || getProfileId()
    savedAt.value = record?.updatedAt ?? ''
    ratings.value = (record?.ratings ?? []).map((item) => ({ ...item }))
    customTags.value = [...(record?.tags ?? [])]
    note.value = record?.note ?? ''
    localError.value = ''
  } catch (error) {
    localError.value = describeError(error) || '当前浏览器无法使用本地存储，自评将无法保存'
  }
}

/** 设置某一项的自评星级 */
function setRating(row: SelfRatingRow, score: number): void {
  const normalized = Number.isFinite(score) ? Math.max(0, Math.min(5, score)) : 0
  const existing = ratings.value.find((item) => item.key === row.key)
  if (existing) {
    existing.score = normalized
    return
  }
  const rating: SkillSelfRating = { key: row.key, name: row.name, score: normalized }
  if (row.skillTagId !== undefined) rating.skillTagId = row.skillTagId
  if (row.category) rating.category = row.category
  ratings.value.push(rating)
}

/** 添加自定义标签 */
function addCustomTag(): void {
  const tag = newTag.value.trim()
  if (!tag) {
    ElMessage.warning('请输入标签名称')
    return
  }
  if (ratingRows.value.some((row) => row.name === tag)) {
    ElMessage.warning('该标签已存在')
    return
  }
  customTags.value.push(tag)
  newTag.value = ''
}

/** 删除自定义标签（同时清理其自评记录） */
function removeCustomTag(row: SelfRatingRow): void {
  customTags.value = customTags.value.filter((tag) => tag !== row.name)
  ratings.value = ratings.value.filter((item) => item.key !== row.key)
}

/** 保存到本机 IndexedDB（绝不调用后端接口） */
async function handleSave(): Promise<void> {
  localSaving.value = true
  localError.value = ''
  try {
    const record = await saveProfile({
      ratings: ratings.value,
      tags: customTags.value,
      note: note.value
    })
    localOwner.value = record.owner || getProfileId()
    savedAt.value = record.updatedAt ?? ''
    ElMessage.success('自评已保存到本机浏览器')
  } catch (error) {
    localError.value = describeError(error) || '保存失败，请检查浏览器存储权限'
    ElMessage.error(localError.value)
  } finally {
    localSaving.value = false
  }
}

/** 导出本地画像为 JSON 文件 */
async function handleExport(): Promise<void> {
  try {
    const blob = await exportProfile()
    const stamp = new Date().toISOString().slice(0, 10)
    saveAs(blob, `技能画像备份-${stamp}.json`)
    ElMessage.success('备份文件已开始下载，请妥善保存')
  } catch (error) {
    ElMessage.error(describeError(error) || '导出失败')
  }
}

/** 打开文件选择框 */
function triggerImport(): void {
  importInputRef.value?.click()
}

/** 从备份文件恢复 */
async function handleImportFile(event: Event): Promise<void> {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  try {
    await importProfile(file)
    await loadLocalProfile()
    ElMessage.success('备份恢复成功')
  } catch (error) {
    ElMessage.error(describeError(error) || '备份恢复失败')
  } finally {
    target.value = ''
  }
}

/** 清空本机画像 */
async function handleClear(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定清空本机保存的技能自评吗？该操作不可撤销，建议先导出备份。', '提示', {
      type: 'warning',
      confirmButtonText: '确定清空',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await clearProfile()
    ratings.value = []
    customTags.value = []
    note.value = ''
    savedAt.value = ''
    ElMessage.success('本地画像已清空')
  } catch (error) {
    ElMessage.error(describeError(error) || '清空失败')
  }
}

/** 窗口尺寸变化时重绘雷达图 */
function resizeChart(): void {
  radarChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', resizeChart)
  loadLocalProfile()
  loadServerProfile()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  radarChart?.dispose()
  radarChart = null
})
</script>


<style scoped lang="scss">
.edu-skill {
  .ml8 {
    margin-left: 8px;
  }
}

.skill-card {
  height: 100%;
  margin-bottom: 12px;
}

.skill-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 14px;
  font-weight: 600;
}

.skill-card__hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.radar-box {
  width: 100%;
  height: 320px;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.skill-tags__item {
  cursor: default;
}

.skill-tags__score {
  margin-left: 6px;
  font-weight: 600;
}

.skill-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 10px;
}

.local-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.rating-list {
  max-height: 300px;
  overflow-y: auto;
  padding-right: 4px;
}

.rating-item {
  padding: 6px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.rating-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.rating-item__name {
  font-size: 13px;
  color: var(--el-text-color-regular);
  display: flex;
  align-items: center;
  gap: 6px;
}

.rating-item__ops {
  text-align: right;
}

.custom-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
}

.note-box {
  margin-top: 14px;
}

.note-box__label {
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 6px;
}

.local-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.local-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 10px;
  line-height: 1.6;
}

.hidden-input {
  display: none;
}

@media (max-width: 768px) {
  .radar-box {
    height: 260px;
  }

  .local-actions {
    :deep(.el-button) {
      flex: 1 1 45%;
      margin-left: 0;
    }
  }
}
</style>
