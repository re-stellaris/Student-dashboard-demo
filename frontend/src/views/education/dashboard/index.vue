<template>
  <div class="app-container edu-dashboard">
    <!-- 错误状态 -->
    <el-alert v-if="errorMessage" type="error" :closable="false" show-icon class="mb8">
      <template #title>
        <span>{{ errorMessage }}</span>
        <el-button type="primary" link class="ml8" @click="loadDashboard">重新加载</el-button>
      </template>
    </el-alert>

    <!-- 空状态 -->
    <el-alert
      v-else-if="!loading && !dashboard"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
      title="暂无学情数据"
      description="可能是尚未导入成绩单或未匹配到培养方案，请联系教务老师确认后再查看。"
    />

    <!-- 学籍概览 -->
    <el-card v-loading="loading" shadow="never" class="mb8 student-card">
      <div class="student-card__body">
        <div class="student-card__main">
          <div class="student-card__name">{{ studentName }}</div>
          <div class="student-card__meta">
            <span v-for="item in studentMeta" :key="item.label" class="student-card__meta-item">
              <em>{{ item.label }}</em>{{ item.value }}
            </span>
            <span v-if="studentMeta.length === 0" class="student-card__meta-item is-muted">
              暂无学籍信息
            </span>
          </div>
        </div>
        <div class="student-card__progress">
          <el-progress type="dashboard" :percentage="progressPercent" :width="112">
            <template #default>
              <div class="student-card__progress-value">{{ progressPercent }}%</div>
              <div class="student-card__progress-label">毕业进度</div>
            </template>
          </el-progress>
        </div>
      </div>
    </el-card>

    <!-- KPI 指标 -->
    <el-row :gutter="12" class="kpi-row">
      <el-col v-for="card in kpiCards" :key="card.key" :xs="12" :sm="12" :md="6">
        <el-card shadow="never" :class="['kpi-card', 'kpi-card--' + card.key]">
          <div class="kpi-card__label">{{ card.label }}</div>
          <div class="kpi-card__value">
            {{ card.value }}<span class="kpi-card__unit">{{ card.unit }}</span>
          </div>
          <div class="kpi-card__hint">{{ card.hint }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="12" class="mb8">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-card__header">
              <span>GPA 趋势</span>
              <span class="chart-card__hint">按学期统计</span>
            </div>
          </template>
          <div v-if="hasGpaTrend" ref="gpaTrendRef" class="chart-box" />
          <el-empty v-else description="暂无 GPA 趋势数据" :image-size="72" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-card__header">
              <span>分类学分占比</span>
              <span class="chart-card__hint">已修学分</span>
            </div>
          </template>
          <div v-if="hasCategoryChart" ref="categoryChartRef" class="chart-box" />
          <el-empty v-else description="暂无分类学分数据" :image-size="72" />
          <div v-if="categoryRows.length > 0" class="category-list">
            <div v-for="row in categoryRows" :key="row.category ?? 'unknown'" class="category-list__item">
              <span class="category-list__name">{{ categoryLabel(row.category) }}</span>
              <span class="category-list__value">
                {{ formatNumber(row.earned, 1) }} / {{ formatNumber(row.required, 1) }}
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 规划与作业 -->
    <el-row :gutter="12">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-card__header">
              <span>选课规划概览</span>
              <span class="chart-card__hint">本学期规划状态</span>
            </div>
          </template>
          <div class="plan-summary">
            <div v-for="item in planSummaryCards" :key="item.label" class="plan-summary__item">
              <div class="plan-summary__value">{{ item.value }}</div>
              <div class="plan-summary__label">{{ item.label }}</div>
            </div>
          </div>
          <div class="plan-summary__footer">
            <span class="plan-summary__note">
              规划需通过先修与学分上限校验后才能提交，提交后仍可查看校验说明。
            </span>
            <el-button type="primary" plain size="small" @click="goPlanner">前往选课规划</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-card__header">
              <span>待提交作业</span>
              <span class="chart-card__hint">按截止时间提醒</span>
            </div>
          </template>
          <div v-if="pendingAssignments > 0" class="pending-card">
            <div class="pending-card__value">{{ pendingAssignments }}</div>
            <div class="pending-card__desc">项作业尚未提交，请尽快完成以免影响平时成绩。</div>
          </div>
          <el-empty v-else description="暂无待提交作业" :image-size="72" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="EduDashboard">
import * as echarts from 'echarts'
import { getDashboard } from '@/api/education/analytics'
import { getMyStudent } from '@/api/education/enrollment'
import type { CreditCategoryRow, CreditProgress, DashboardStat, EduStudent, GpaTrendPoint } from '@/types/education'

type ChartInstance = ReturnType<typeof echarts.init>

interface KpiCard {
  key: string
  label: string
  value: string
  unit: string
  hint: string
}

interface MetaItem {
  label: string
  value: string
}

interface PlanSummaryCard {
  label: string
  value: number
}

const router = useRouter()

const loading = ref<boolean>(true)
const errorMessage = ref<string>('')
const dashboard = ref<DashboardStat | null>(null)
const student = ref<EduStudent | null>(null)

const gpaTrendRef = ref<HTMLDivElement | null>(null)
const categoryChartRef = ref<HTMLDivElement | null>(null)
let gpaTrendChart: ChartInstance | null = null
let categoryChart: ChartInstance | null = null

const credits = computed<CreditProgress | null>(() => dashboard.value?.credits ?? null)
const gpaTrend = computed<GpaTrendPoint[]>(() => dashboard.value?.gpaTrend ?? [])
const categoryRows = computed<CreditCategoryRow[]>(
  () => dashboard.value?.categoryBreakdown ?? dashboard.value?.credits?.categoryBreakdown ?? []
)
const earnedUnits = computed<number>(() => credits.value?.earned ?? 0)
const requiredUnits = computed<number>(() => credits.value?.curriculumTotal ?? 0)
const remainingUnits = computed<number>(() => credits.value?.remaining ?? 0)
const cumulativeGpa = computed<number>(() => dashboard.value?.gpa ?? 0)
const pendingAssignments = computed<number>(() => dashboard.value?.pendingAssignments ?? 0)
const hasGpaTrend = computed<boolean>(() => gpaTrend.value.length > 0)
const hasCategoryChart = computed<boolean>(() => categoryRows.value.some((row) => (row.earned ?? 0) > 0))

const progressPercent = computed<number>(() => {
  if (requiredUnits.value <= 0) return 0
  const ratio = (earnedUnits.value / requiredUnits.value) * 100
  if (!Number.isFinite(ratio)) return 0
  return Math.max(0, Math.min(100, Math.round(ratio * 10) / 10))
})

const studentName = computed<string>(() => student.value?.name || '未获取到学籍信息')

const studentMeta = computed<MetaItem[]>(() => {
  const list: MetaItem[] = []
  const info = student.value
  if (!info) return list
  if (info.studentNo) list.push({ label: '学号', value: info.studentNo })
  if (info.majorNameZh) {
    list.push({ label: '专业', value: info.majorNameZh })
  } else if (info.majorId) {
    list.push({ label: '专业', value: `#${info.majorId}` })
  }
  if (info.entryYear) list.push({ label: '入学年份', value: `${info.entryYear}` })
  if (info.college) list.push({ label: '书院', value: info.college })
  return list
})

const kpiCards = computed<KpiCard[]>(() => [
  {
    key: 'earned',
    label: '已修学分',
    value: formatNumber(earnedUnits.value, 1),
    unit: '学分',
    hint: '已通过课程的学分合计'
  },
  {
    key: 'required',
    label: '毕业要求',
    value: formatNumber(requiredUnits.value, 1),
    unit: '学分',
    hint: '适用培养方案的毕业总学分'
  },
  {
    key: 'remaining',
    label: '剩余学分',
    value: formatNumber(remainingUnits.value, 1),
    unit: '学分',
    hint: '距离毕业仍需修读的学分'
  },
  {
    key: 'gpa',
    label: 'GPA',
    value: formatNumber(cumulativeGpa.value, 2),
    unit: '',
    hint: '累计平均绩点'
  }
])

const planSummaryCards = computed<PlanSummaryCard[]>(() => {
  const summary = dashboard.value?.planSummary
  return [
    { label: '草稿', value: summary?.draft ?? 0 },
    { label: '已校验', value: summary?.validated ?? 0 },
    { label: '已提交', value: summary?.submitted ?? 0 }
  ]
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

/** 把后端/网络异常转成可读文案 */
function describeError(error: unknown): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'string') return error
  return ''
}

/** GPA 趋势折线图 */
function renderGpaTrendChart(): void {
  const container = gpaTrendRef.value
  if (!container) return
  if (gpaTrendChart) {
    gpaTrendChart.dispose()
    gpaTrendChart = null
  }
  const points = gpaTrend.value
  if (points.length === 0) return
  gpaTrendChart = echarts.init(container)
  gpaTrendChart.setOption({
    grid: { left: 8, right: 16, top: 36, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value: unknown) => (typeof value === 'number' ? value.toFixed(2) : String(value ?? ''))
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((point) => point.termName || point.termCode || '')
    },
    yAxis: {
      type: 'value',
      name: 'GPA',
      min: 0,
      axisLabel: { formatter: '{value}' }
    },
    series: [
      {
        name: 'GPA',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: points.map((point) => point.gpa ?? 0),
        itemStyle: { color: '#409eff' },
        lineStyle: { width: 2 },
        areaStyle: { opacity: 0.16 }
      }
    ]
  })
}

/** 分类学分环形图 */
function renderCategoryChart(): void {
  const container = categoryChartRef.value
  if (!container) return
  if (categoryChart) {
    categoryChart.dispose()
    categoryChart = null
  }
  const rows = categoryRows.value.filter((row) => (row.earned ?? 0) > 0)
  if (rows.length === 0) return
  categoryChart = echarts.init(container)
  categoryChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>已修 {c} 学分（{d}%）'
    },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        name: '分类学分',
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#ffffff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{c}' },
        data: rows.map((row) => ({ name: categoryLabel(row.category), value: row.earned ?? 0 }))
      }
    ]
  })
}

/** 窗口尺寸变化时重绘 */
function resizeCharts(): void {
  gpaTrendChart?.resize()
  categoryChart?.resize()
}

/** 加载仪表盘数据 */
async function loadDashboard(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const [dashboardRes, studentRes] = await Promise.all([getDashboard(), getMyStudent()])
    dashboard.value = dashboardRes?.data ?? null
    student.value = studentRes?.data ?? null
    await nextTick()
    renderGpaTrendChart()
    renderCategoryChart()
  } catch (error) {
    const message = describeError(error)
    errorMessage.value = message || '学情数据加载失败，请稍后重试'
    dashboard.value = null
    student.value = null
  } finally {
    loading.value = false
  }
}

/** 跳转选课规划 */
function goPlanner(): void {
  router.push('/education/planner').catch(() => undefined)
}

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  loadDashboard()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  gpaTrendChart?.dispose()
  categoryChart?.dispose()
  gpaTrendChart = null
  categoryChart = null
})
</script>

<style scoped lang="scss">
.edu-dashboard {
  .ml8 {
    margin-left: 8px;
  }
}

.student-card__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.student-card__main {
  min-width: 200px;
  flex: 1;
}

.student-card__name {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}

.student-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.student-card__meta-item {
  em {
    font-style: normal;
    color: var(--el-text-color-secondary);
    margin-right: 4px;
  }

  &.is-muted {
    color: var(--el-text-color-secondary);
  }
}

.student-card__progress {
  display: flex;
  align-items: center;
  justify-content: center;
}

.student-card__progress-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.student-card__progress-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.kpi-row {
  margin-bottom: 12px;
}

.kpi-card {
  position: relative;
  height: 100%;
  border-left: 3px solid var(--el-color-primary);
  margin-bottom: 12px;

  &--earned {
    border-left-color: #409eff;
  }

  &--required {
    border-left-color: #67c23a;
  }

  &--remaining {
    border-left-color: #e6a23c;
  }

  &--gpa {
    border-left-color: #909399;
  }
}

.kpi-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.kpi-card__value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--el-text-color-primary);
  margin-top: 6px;
}

.kpi-card__unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
  margin-left: 4px;
}

.kpi-card__hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.chart-card {
  margin-bottom: 12px;
  height: 100%;
}

.chart-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
}

.chart-card__hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.chart-box {
  width: 100%;
  height: 320px;
}

.category-list {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 8px;
  margin-top: 4px;
  max-height: 140px;
  overflow-y: auto;
}

.category-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  padding: 3px 0;
  color: var(--el-text-color-regular);
}

.category-list__value {
  color: var(--el-text-color-secondary);
}

.plan-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.plan-summary__item {
  flex: 1 1 100px;
  text-align: center;
  padding: 12px 8px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}

.plan-summary__value {
  font-size: 22px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.plan-summary__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.plan-summary__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.plan-summary__note {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  flex: 1;
  min-width: 180px;
}

.pending-card {
  text-align: center;
  padding: 12px 0;
}

.pending-card__value {
  font-size: 40px;
  font-weight: 600;
  color: var(--el-color-warning);
  line-height: 1.2;
}

.pending-card__desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 8px;
}

@media (max-width: 768px) {
  .chart-box {
    height: 240px;
  }

  .kpi-card__value {
    font-size: 22px;
  }

  .student-card__progress {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
