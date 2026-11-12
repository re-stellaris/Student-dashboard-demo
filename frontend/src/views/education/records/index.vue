<template>
  <div class="app-container edu-records">
    <!-- 错误状态 -->
    <el-alert v-if="errorMessage" type="error" :closable="false" show-icon class="mb8">
      <template #title>
        <span>{{ errorMessage }}</span>
        <el-button type="primary" link class="ml8" @click="getList">重新加载</el-button>
      </template>
    </el-alert>

    <!-- 学分进度汇总 -->
    <el-card v-loading="progressLoading" shadow="never" class="mb8 progress-card">
      <template #header>
        <div class="progress-card__header">
          <span>学分进度</span>
          <span class="progress-card__hint">数据来源：本人成绩单与适用培养方案</span>
        </div>
      </template>
      <el-row :gutter="12">
        <el-col v-for="item in progressCards" :key="item.label" :xs="12" :sm="12" :md="6">
          <div class="progress-item">
            <div class="progress-item__label">{{ item.label }}</div>
            <div class="progress-item__value">
              {{ item.value }}<span class="progress-item__unit">{{ item.unit }}</span>
            </div>
          </div>
        </el-col>
      </el-row>
      <div class="progress-bar">
        <div class="progress-bar__label">
          <span>毕业学分完成度</span>
          <span>{{ progressPercent }}%（已修 {{ formatNumber(progress?.earned, 1) }} / 要求 {{ formatNumber(progress?.curriculumTotal, 1) }}）</span>
        </div>
        <el-progress :percentage="progressPercent" :stroke-width="12" :show-text="false" />
      </div>
      <div v-if="categoryRows.length > 0" class="category-progress">
        <div v-for="row in categoryRows" :key="row.category ?? 'unknown'" class="category-progress__item">
          <div class="category-progress__head">
            <span>{{ categoryLabel(row.category) }}</span>
            <span class="category-progress__value">
              {{ formatNumber(row.earned, 1) }} / {{ formatNumber(row.required, 1) }} 学分
            </span>
          </div>
          <el-progress
            :percentage="categoryPercent(row)"
            :stroke-width="8"
            :show-text="false"
            :color="categoryColor(row)"
          />
        </div>
      </div>
      <el-empty v-if="!progressLoading && !progress" description="暂无学分进度数据" :image-size="60" />
    </el-card>

    <!-- 查询条件 -->
    <el-form v-show="showSearch" :model="queryParams" :inline="true" class="query-form">
      <el-form-item label="学期" prop="termCode">
        <el-select v-model="queryParams.termCode" placeholder="全部学期" clearable style="width: 200px">
          <el-option v-for="item in termOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="课程类别" prop="category">
        <el-select v-model="queryParams.category" placeholder="全部类别" clearable style="width: 180px">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <span class="list-total">共 {{ total }} 条成绩记录</span>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 成绩单 -->
    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column label="学期" prop="termCode" min-width="120" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ termLabel(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="课程代码" prop="courseCode" min-width="110" :show-overflow-tooltip="true" />
      <el-table-column label="课程名称" prop="courseTitle" min-width="200" :show-overflow-tooltip="true" />
      <el-table-column label="学分" prop="units" width="80" align="center">
        <template #default="scope">
          <span>{{ formatNumber(scope.row.units, 1) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="等级成绩" prop="grade" width="90" align="center">
        <template #default="scope">
          <span>{{ scope.row.grade || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="百分制" prop="score" width="90" align="center">
        <template #default="scope">
          <span>{{ formatNumber(scope.row.score, 1) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类别" prop="category" width="110" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.category" size="small" effect="plain">{{ categoryLabel(scope.row.category) }}</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="recordStatus" width="110" align="center">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.recordStatus)" size="small">
            {{ statusLabel(scope.row.recordStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :image-size="80" description="暂无成绩记录" />
      </template>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup lang="ts" name="EduRecords">
import { getCourseOptions } from '@/api/education/course'
import { getMyProgress, listMyRecord, listTerm } from '@/api/education/enrollment'
import type {
  CreditCategoryRow,
  CreditProgress,
  EduCourseRecord,
  EduTerm,
  RecordQuery
} from '@/types/education'

interface SummaryCard {
  label: string
  value: string
  unit: string
}

interface SelectOption {
  label: string
  value: string
}

const loading = ref<boolean>(false)
const progressLoading = ref<boolean>(false)
const showSearch = ref<boolean>(true)
const errorMessage = ref<string>('')
const records = ref<EduCourseRecord[]>([])
const total = ref<number>(0)
const terms = ref<EduTerm[]>([])
const categories = ref<string[]>([])
const progress = ref<CreditProgress | null>(null)

const queryParams = reactive<RecordQuery>({
  pageNum: 1,
  pageSize: 10,
  termCode: undefined,
  category: undefined
})

const CATEGORY_LABELS: Record<string, string> = {
  Major: '专业',
  GenEd: '通识',
  English: '英语',
  Chinese: '中文',
  IT: '信息技术',
  PE: '体育',
  Other: '其他'
}

const STATUS_LABELS: Record<string, string> = {
  COMPLETED: '已完成',
  IN_PROGRESS: '修读中',
  WITHDRAWN: '已退课'
}

const categoryRows = computed<CreditCategoryRow[]>(() => progress.value?.categoryBreakdown ?? [])

const progressPercent = computed<number>(() => {
  const required = progress.value?.curriculumTotal ?? 0
  if (required <= 0) return 0
  const ratio = ((progress.value?.earned ?? 0) / required) * 100
  if (!Number.isFinite(ratio)) return 0
  return Math.max(0, Math.min(100, Math.round(ratio * 10) / 10))
})

const progressCards = computed<SummaryCard[]>(() => [
  { label: '已修学分', value: formatNumber(progress.value?.earned, 1), unit: '学分' },
  { label: '毕业要求', value: formatNumber(progress.value?.curriculumTotal, 1), unit: '学分' },
  { label: '剩余学分', value: formatNumber(progress.value?.remaining, 1), unit: '学分' },
  { label: '专业必修要求', value: formatNumber(progress.value?.majorRequired, 1), unit: '学分' }
])

const termOptions = computed<SelectOption[]>(() => {
  const options: SelectOption[] = []
  terms.value.forEach((term) => {
    if (term.termCode) {
      options.push({ label: term.termName || term.termCode, value: term.termCode })
    }
  })
  return options
})

const categoryOptions = computed<SelectOption[]>(() =>
  categories.value.map((category) => ({ label: categoryLabel(category), value: category }))
)

/** 数字格式化，缺省值显示占位符 */
function formatNumber(value: number | null | undefined, digits = 1): string {
  if (value === null || value === undefined || !Number.isFinite(value)) return '—'
  return value.toFixed(digits)
}

/** 学分类别的中文展示 */
function categoryLabel(category?: string): string {
  if (!category) return '未分类'
  return CATEGORY_LABELS[category] ?? category
}

/** 成绩记录状态的中文展示 */
function statusLabel(status?: string): string {
  if (!status) return '—'
  return STATUS_LABELS[status] ?? status
}

/** 成绩记录状态的标签配色 */
function statusTagType(status?: string): 'success' | 'primary' | 'danger' | 'info' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS') return 'primary'
  if (status === 'WITHDRAWN') return 'danger'
  return 'info'
}

/** 学期列展示文案 */
function termLabel(row: EduCourseRecord): string {
  return row.termName || row.termCode || '—'
}

/** 分类完成度百分比 */
function categoryPercent(row: CreditCategoryRow): number {
  const required = row.required ?? 0
  if (required <= 0) return 0
  const ratio = ((row.earned ?? 0) / required) * 100
  return Math.max(0, Math.min(100, Math.round(ratio)))
}

/** 分类进度条配色：完成后变绿，未完成保持主色 */
function categoryColor(row: CreditCategoryRow): string {
  return categoryPercent(row) >= 100 ? '#67c23a' : '#409eff'
}

/** 把后端/网络异常转成可读文案 */
function describeError(error: unknown): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'string') return error
  return ''
}

/** 查询成绩单 */
async function getList(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await listMyRecord(queryParams)
    records.value = response?.rows ?? []
    total.value = response?.total ?? 0
  } catch (error) {
    records.value = []
    total.value = 0
    const message = describeError(error)
    errorMessage.value = message || '成绩单加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 查询学分进度 */
async function getProgress(): Promise<void> {
  progressLoading.value = true
  try {
    const response = await getMyProgress()
    progress.value = response?.data ?? null
  } catch {
    progress.value = null
  } finally {
    progressLoading.value = false
  }
}

/** 加载学期下拉数据 */
async function getTerms(): Promise<void> {
  try {
    const response = await listTerm({ pageNum: 1, pageSize: 200 })
    terms.value = response?.rows ?? []
  } catch {
    terms.value = []
  }
}

/** 加载课程类别下拉数据 */
async function getCategoryOptions(): Promise<void> {
  try {
    const response = await getCourseOptions()
    const options = response?.data?.categories ?? []
    categories.value = options.filter((item) => typeof item === 'string' && item.length > 0)
  } catch {
    categories.value = []
  }
}

/** 搜索 */
function handleQuery(): void {
  queryParams.pageNum = 1
  getList()
}

/** 重置搜索条件 */
function resetQuery(): void {
  queryParams.termCode = undefined
  queryParams.category = undefined
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  getList()
}

onMounted(() => {
  getTerms()
  getCategoryOptions()
  getProgress()
  getList()
})
</script>

<style scoped lang="scss">
.edu-records {
  .ml8 {
    margin-left: 8px;
  }
}

.progress-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 14px;
  font-weight: 600;
}

.progress-card__hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.progress-item {
  padding: 8px 12px;
  margin-bottom: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}

.progress-item__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.progress-item__value {
  font-size: 22px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-top: 4px;
}

.progress-item__unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
  margin-left: 4px;
}

.progress-bar {
  margin-top: 4px;
}

.progress-bar__label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 6px;
}

.category-progress {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 8px 20px;
  margin-top: 14px;
}

.category-progress__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 4px;
}

.category-progress__value {
  color: var(--el-text-color-secondary);
}

.query-form {
  margin-bottom: 4px;
}

.list-total {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 32px;
}

@media (max-width: 768px) {
  .query-form {
    :deep(.el-form-item) {
      margin-right: 8px;
    }
  }

  .progress-item__value {
    font-size: 18px;
  }
}
</style>
