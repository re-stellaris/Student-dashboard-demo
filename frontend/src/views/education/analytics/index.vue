<template>
  <div class="app-container">
    <!-- 筛选 -->
    <el-card shadow="never" class="mb16">
      <el-form :model="queryParams" ref="queryRef" :inline="true" @submit.prevent>
        <el-form-item label="学院缩写" prop="schoolAbbr">
          <el-input v-model="queryParams.schoolAbbr" placeholder="如 SDS / SME" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="关键字" prop="keyword">
          <el-input v-model="queryParams.keyword" placeholder="技能标签 / 课程名" clearable style="width: 220px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        title="本页为只读视图：学院能力池来自课程大纲的关键词匹配与关联度评分，仅展示，不支持编辑。"
        type="info"
        show-icon
        :closable="false"
      />
    </el-card>

    <el-row :gutter="16">
      <!-- 左：能力池（技能标签） -->
      <el-col :xs="24" :md="11" :lg="10">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">学院能力池</span>
              <span class="panel-subtitle">共 {{ poolTotal }} 条</span>
            </div>
          </template>

          <el-alert v-if="poolError" :title="poolError" type="error" show-icon :closable="false" class="mb8" />
          <el-table
            v-loading="poolLoading"
            :data="poolRows"
            border
            stripe
            highlight-current-row
            empty-text="暂无能力池数据"
            @row-click="handlePoolRowClick"
          >
            <el-table-column label="技能标签" prop="name" min-width="140" :show-overflow-tooltip="true" />
            <el-table-column label="分类" prop="category" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="学院" prop="schoolAbbr" width="90" align="center" />
            <el-table-column label="关联课程数" prop="courseCount" width="110" align="center">
              <template #default="scope">
                <span>{{ scope.row.courseCount ?? '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="课程" min-width="150" :show-overflow-tooltip="true">
              <template #default="scope">
                <span v-if="scope.row.courseCode || scope.row.courseTitle">{{ scope.row.courseCode }} {{ scope.row.courseTitle }}</span>
                <span v-else class="text-muted">—</span>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-show="poolTotal > 0"
            :total="poolTotal"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="loadPool"
          />
        </el-card>
      </el-col>

      <!-- 右：课程—技能明细（ECharts） -->
      <el-col :xs="24" :md="13" :lg="14">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">课程能力明细</span>
              <el-tag v-if="selectedCourse" size="small" type="info">{{ selectedCourse.code }} {{ selectedCourse.titleZh || selectedCourse.titleEn }}</el-tag>
            </div>
          </template>

          <div class="course-picker">
            <el-select
              v-model="courseId"
              filterable
              remote
              reserve-keyword
              clearable
              placeholder="输入课程代码或名称，查看该课程的能力关联"
              :remote-method="searchCourses"
              :loading="courseOptionsLoading"
              style="width: 100%"
              @change="handleCourseChange"
            >
              <el-option v-for="opt in courseSelectOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </div>

          <el-alert v-if="detailError" :title="detailError" type="error" show-icon :closable="false" class="mb8" />
          <el-empty v-if="courseId == null" description="请选择课程，或点击左侧能力池中的课程行" :image-size="70" />
          <template v-else>
            <div v-loading="detailLoading">
              <el-empty v-if="!detailLoading && !detailRows.length" description="该课程暂未关联技能标签" :image-size="70" />
              <template v-else>
                <div ref="chartRef" v-show="detailRows.length" class="chart-box" />
                <el-table :data="detailRows" border stripe size="small" empty-text="暂无关联明细">
                  <el-table-column label="技能标签" prop="name" min-width="130" :show-overflow-tooltip="true" />
                  <el-table-column label="分类" prop="category" width="110" :show-overflow-tooltip="true" />
                  <el-table-column label="关联度" prop="score" width="90" align="center">
                    <template #default="scope">
                      <span class="score-text">{{ scope.row.score ?? '—' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="等级" prop="level" width="110" align="center" />
                  <el-table-column label="判断依据" prop="rationale" min-width="200" :show-overflow-tooltip="true" />
                  <el-table-column label="命中关键词" prop="matchedKeywords" min-width="140" :show-overflow-tooltip="true" />
                </el-table>
              </template>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="EduAnalytics">
import * as echarts from 'echarts'
import { listCourse } from '@/api/education/course'
import { getCompetencyPool, getCourseCompetency } from '@/api/education/analytics'
import type { CompetencyQuery, CourseCompetency, CourseQuery, EduCourse } from '@/types/education'

/** 能力池分页参数（分页字段必填，便于 pagination 绑定） */
type CompetencyListQuery = CompetencyQuery & { pageNum: number; pageSize: number }
/** 课程远程搜索参数 */
type CourseListQuery = CourseQuery & { pageNum: number; pageSize: number }

const proxy = getCurrentInstance()!.proxy as any

/* ------------------------------ 学院能力池 ------------------------------ */

const queryParams = ref<CompetencyListQuery>({
  pageNum: 1,
  pageSize: 10,
  schoolAbbr: undefined,
  keyword: undefined
})
const poolRows = ref<CourseCompetency[]>([])
const poolTotal = ref(0)
const poolLoading = ref(false)
const poolError = ref('')

/** 学院能力池（GET /edu/analytics/competency-pool） */
function loadPool(): Promise<void> {
  poolLoading.value = true
  poolError.value = ''
  return getCompetencyPool(queryParams.value)
    .then((res) => {
      poolRows.value = res.rows ?? []
      poolTotal.value = res.total ?? 0
    })
    .catch(() => {
      poolRows.value = []
      poolTotal.value = 0
      poolError.value = '能力池加载失败，请稍后重试。'
    })
    .finally(() => {
      poolLoading.value = false
    })
}

function handleQuery(): void {
  queryParams.value.pageNum = 1
  loadPool()
}

function resetQuery(): void {
  queryParams.value = {
    pageNum: 1,
    pageSize: queryParams.value.pageSize,
    schoolAbbr: undefined,
    keyword: undefined
  }
  proxy.resetForm('queryRef')
  loadPool()
}

/* ------------------------------ 课程选择 ------------------------------ */

const courseId = ref<number | undefined>(undefined)
const courseOptions = ref<EduCourse[]>([])
const courseOptionsLoading = ref(false)
const selectedCourse = computed(() => courseOptions.value.find((item) => item.courseId === courseId.value) ?? null)

const courseSelectOptions = computed(() =>
  courseOptions.value
    .filter((item) => item.courseId != null)
    .map((item) => ({
      value: Number(item.courseId),
      label: [item.code, item.titleZh || item.titleEn].filter((part) => !!part).join(' ') || `课程 #${item.courseId}`
    }))
)

/** 课程远程搜索（复用课程列表接口，仅用于选择课程） */
function searchCourses(keyword: string): void {
  courseOptionsLoading.value = true
  const query: CourseListQuery = {
    pageNum: 1,
    pageSize: 20,
    code: keyword || undefined,
    title: keyword || undefined
  }
  listCourse(query)
    .then((res) => {
      const rows = res.rows ?? []
      // 保留已选课程，避免下拉项丢失导致只显示 ID
      const selected = selectedCourse.value
      courseOptions.value = selected && !rows.some((item) => item.courseId === selected.courseId) ? [selected, ...rows] : rows
    })
    .catch(() => {
      courseOptions.value = selectedCourse.value ? [selectedCourse.value] : []
    })
    .finally(() => {
      courseOptionsLoading.value = false
    })
}

function handleCourseChange(): void {
  loadDetail()
}

/** 点击能力池行 → 若行内带课程则直接下钻 */
function handlePoolRowClick(row: CourseCompetency): void {
  if (row.courseId == null) {
    return
  }
  if (!courseOptions.value.some((item) => item.courseId === row.courseId)) {
    courseOptions.value = [
      { courseId: row.courseId, code: row.courseCode, titleZh: row.courseTitle },
      ...courseOptions.value
    ]
  }
  courseId.value = row.courseId
  loadDetail()
}

/* ------------------------------ 课程能力明细 ------------------------------ */

const detailRows = ref<CourseCompetency[]>([])
const detailLoading = ref(false)
const detailError = ref('')

const chartRef = ref<HTMLDivElement>()
let chartInstance: ReturnType<typeof echarts.init> | null = null

function renderChart(rows: CourseCompetency[]): void {
  if (!chartRef.value) {
    return
  }
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value, 'macarons')
  }
  const top = [...rows]
    .sort((a, b) => (b.score ?? 0) - (a.score ?? 0))
    .slice(0, 10)
  chartInstance.setOption(
    {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: '{b}<br/>关联度：{c}'
      },
      grid: { left: 8, right: 24, top: 28, bottom: 8, containLabel: true },
      xAxis: {
        type: 'category',
        data: top.map((item) => item.name || item.skillCode || '未命名标签'),
        axisLabel: { interval: 0, rotate: 30, fontSize: 11 }
      },
      yAxis: {
        type: 'value',
        name: '关联度',
        min: 0,
        max: 10
      },
      series: [
        {
          name: '关联度',
          type: 'bar',
          barMaxWidth: 32,
          itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
          label: { show: true, position: 'top', fontSize: 11 },
          data: top.map((item) => item.score ?? 0)
        }
      ]
    },
    true
  )
  chartInstance.resize()
}

function disposeChart(): void {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

function resizeChart(): void {
  chartInstance?.resize()
}

/** 单门课程的技能关联明细（GET /edu/analytics/course-competency?courseId=） */
function loadDetail(): Promise<void> {
  const targetCourseId = courseId.value
  if (targetCourseId == null) {
    detailRows.value = []
    detailError.value = ''
    disposeChart()
    return Promise.resolve()
  }
  detailLoading.value = true
  detailError.value = ''
  return getCourseCompetency(targetCourseId)
    .then((res) => {
      detailRows.value = res.data ?? []
      return nextTick().then(() => renderChart(detailRows.value))
    })
    .catch(() => {
      detailRows.value = []
      detailError.value = '课程能力明细加载失败，请稍后重试。'
      disposeChart()
    })
    .finally(() => {
      detailLoading.value = false
    })
}

onMounted(() => {
  loadPool()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  disposeChart()
})
</script>

<style scoped lang="scss">
.panel {
  margin-bottom: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.panel-title {
  font-weight: 600;
  font-size: 14px;
}

.panel-subtitle {
  font-size: 12px;
  color: #909399;
}

.course-picker {
  margin-bottom: 12px;
}

.chart-box {
  width: 100%;
  height: 320px;
  margin-bottom: 12px;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.score-text {
  font-weight: 600;
  color: #409eff;
}

.mb16 {
  margin-bottom: 16px;
}
</style>
