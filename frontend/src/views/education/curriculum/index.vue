<template>
  <div class="app-container">
    <el-row :gutter="16">
      <!-- 左：专业选择 + 版本列表 -->
      <el-col :xs="24" :md="10" :lg="9">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">专业与方案版本</span>
              <el-button link type="primary" icon="Refresh" @click="reloadAll">刷新</el-button>
            </div>
          </template>

          <el-form label-width="56px" @submit.prevent>
            <el-form-item label="专业">
              <el-select
                v-model="majorId"
                filterable
                clearable
                placeholder="请选择专业"
                style="width: 100%"
                :loading="majorLoading"
                @change="handleMajorChange"
              >
                <el-option v-for="item in majorOptions" :key="item.majorId" :label="majorLabel(item)" :value="item.majorId" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-alert v-if="majorError" :title="majorError" type="error" show-icon :closable="false" class="mb8" />

          <el-divider content-position="left">按入学年份匹配版本</el-divider>
          <div class="match-row">
            <el-input-number
              v-model="entryYear"
              :min="2000"
              :max="2100"
              :step="1"
              step-strictly
              controls-position="right"
              placeholder="入学年份"
              class="match-input"
            />
            <el-button type="primary" :loading="matchLoading" icon="Search" @click="handleMatchVersion">匹配版本</el-button>
          </div>
          <el-alert v-if="matchError" :title="matchError" type="error" show-icon :closable="false" class="mt8" />
          <el-alert
            v-else-if="matchDone && !matchedCurriculum"
            title="该专业在此入学年份没有匹配的培养方案版本（接口返回 null）"
            type="warning"
            show-icon
            :closable="false"
            class="mt8"
          />
          <div v-else-if="matchDone && matchedCurriculum" class="match-result">
            <div class="match-result-title">{{ matchedCurriculum.versionLabel || '未命名版本' }}</div>
            <div class="match-result-meta">
              生效入学年：{{ matchedCurriculum.versionYear ?? '-' }}
              <span v-if="matchedCurriculum.majorCredits != null"> · 专业学分：{{ matchedCurriculum.majorCredits }}</span>
            </div>
            <el-button type="primary" link @click="viewMatched">查看该版本</el-button>
          </div>

          <el-divider content-position="left">方案版本列表</el-divider>
          <el-alert v-if="listError" :title="listError" type="error" show-icon :closable="false" class="mb8" />
          <el-table
            ref="curriculumTableRef"
            v-loading="listLoading"
            :data="curriculumList"
            size="small"
            border
            highlight-current-row
            empty-text="暂无方案版本"
            style="width: 100%"
            @current-change="handleCurrentChange"
          >
            <el-table-column label="版本标签" prop="versionLabel" min-width="150" :show-overflow-tooltip="true" />
            <el-table-column label="生效年" prop="versionYear" width="80" align="center" />
            <el-table-column label="总学分" prop="totalCredits" width="80" align="center" />
          </el-table>
        </el-card>
      </el-col>

      <!-- 右：方案详情 + 条目 -->
      <el-col :xs="24" :md="14" :lg="15">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">方案详情</span>
              <el-button
                v-if="selectedCurriculum"
                type="primary"
                plain
                size="small"
                icon="Edit"
                @click="openItemEditor"
                v-hasPermi="['edu:curriculum:item:edit']"
              >编辑方案条目</el-button>
            </div>
          </template>

          <div v-loading="detailLoading">
            <el-alert v-if="detailError" :title="detailError" type="error" show-icon :closable="false" class="mb8" />
            <el-empty v-else-if="!selectedCurriculum" description="请在左侧选择培养方案版本" :image-size="80" />
            <template v-else>
              <el-descriptions :column="3" border size="small" class="mb16">
                <el-descriptions-item label="版本标签">{{ selectedCurriculum.versionLabel || '-' }}</el-descriptions-item>
                <el-descriptions-item label="生效入学年">{{ selectedCurriculum.versionYear ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="所属专业">{{ majorName(selectedCurriculum.majorId) }}</el-descriptions-item>
                <el-descriptions-item label="毕业总学分">{{ selectedCurriculum.totalCredits ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="专业学分">{{ selectedCurriculum.majorCredits ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="条目数">{{ detailItems.length }}</el-descriptions-item>
              </el-descriptions>

              <el-empty v-if="!detailItems.length" description="该版本暂无课程条目" :image-size="70" />
              <el-collapse v-else v-model="activeGroups">
                <el-collapse-item v-for="group in itemGroups" :key="group.type" :name="group.type">
                  <template #title>
                    <div class="group-title">
                      <el-tag size="small" :type="group.tagType">{{ group.label }}</el-tag>
                      <span class="group-meta">{{ group.items.length }} 门 · 最低 {{ group.minUnits }} 学分</span>
                    </div>
                  </template>
                  <el-table :data="group.items" size="small" border>
                    <el-table-column label="课程代码" prop="courseCode" width="120" />
                    <el-table-column label="课程名称" prop="courseTitle" min-width="160" :show-overflow-tooltip="true" />
                    <el-table-column label="章节" prop="section" min-width="140" :show-overflow-tooltip="true" />
                    <el-table-column label="方向" prop="stream" width="110" :show-overflow-tooltip="true" />
                    <el-table-column label="最低学分" prop="minUnits" width="90" align="center" />
                    <el-table-column label="排序" prop="sortOrder" width="70" align="center" />
                  </el-table>
                </el-collapse-item>
              </el-collapse>
            </template>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 方案条目编辑对话框 -->
    <el-dialog v-model="itemOpen" title="编辑方案条目（覆盖式保存）" width="1000px" append-to-body>
      <el-alert
        title="保存后将以当前列表整体覆盖该版本的条目。条目可只填课程代码 + 最低学分，表示纯学分池（courseId 为空）。"
        type="warning"
        show-icon
        :closable="false"
        class="mb8"
      />
      <el-table :data="itemDraft" border size="small" max-height="420" empty-text="暂无条目">
        <el-table-column label="课程" min-width="240">
          <template #default="scope">
            <el-select
              v-model="scope.row.courseId"
              filterable
              remote
              reserve-keyword
              clearable
              placeholder="搜索课程代码/名称"
              :remote-method="searchCourses"
              :loading="courseOptionsLoading"
              style="width: 100%"
              @change="handleItemCourseChange(scope.row)"
            >
              <el-option v-for="opt in courseSelectOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="课程代码" width="130">
          <template #default="scope">
            <el-input v-model="scope.row.courseCode" placeholder="如 CSC1001" />
          </template>
        </el-table-column>
        <el-table-column label="要求类型" width="130">
          <template #default="scope">
            <el-select v-model="scope.row.requirementType" placeholder="请选择" style="width: 100%">
              <el-option v-for="item in REQUIREMENT_TYPES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="章节" width="150">
          <template #default="scope">
            <el-input v-model="scope.row.section" placeholder="如 School Package" />
          </template>
        </el-table-column>
        <el-table-column label="方向" width="120">
          <template #default="scope">
            <el-input v-model="scope.row.stream" placeholder="可选" />
          </template>
        </el-table-column>
        <el-table-column label="最低学分" width="120" align="center">
          <template #default="scope">
            <el-input-number v-model="scope.row.minUnits" :min="0" :max="60" :precision="1" :step="0.5" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="排序" width="90" align="center">
          <template #default="scope">
            <el-input-number v-model="scope.row.sortOrder" :min="0" :step="1" step-strictly controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="scope">
            <el-button link type="danger" icon="Delete" @click="removeItemRow(scope.$index)" />
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-toolbar">
        <el-button type="primary" plain icon="Plus" @click="addItemRow">新增条目</el-button>
        <span class="dialog-toolbar-tip">共 {{ itemDraft.length }} 条，合计最低学分 {{ draftUnits }} 分</span>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="itemSaving" @click="submitItems">确 定</el-button>
          <el-button @click="itemOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="EduCurriculum">
import { getCurriculum, getCurriculumByMajor, listCurriculum, listMajor, updateCurriculumItems } from '@/api/education/curriculum'
import { listCourse } from '@/api/education/course'
import type { CourseQuery, EduCourse, EduCurriculum, EduCurriculumItem, EduMajor } from '@/types/education'

/** 分页查询参数（课程远程搜索用） */
type CourseListQuery = CourseQuery & { pageNum: number; pageSize: number }

interface RequirementTypeOption {
  value: string
  label: string
  tagType: 'primary' | 'success' | 'info' | 'warning' | 'danger'
}

interface ItemGroup {
  type: string
  label: string
  tagType: 'primary' | 'success' | 'info' | 'warning' | 'danger'
  minUnits: number
  items: EduCurriculumItem[]
}

/** 契约 2：培养方案条目要求类型 */
const REQUIREMENT_TYPES: RequirementTypeOption[] = [
  { value: 'REQUIRED', label: '必修', tagType: 'danger' },
  { value: 'ELECTIVE', label: '选修', tagType: 'primary' },
  { value: 'GE', label: '通识', tagType: 'success' },
  { value: 'BREADTH', label: '广度', tagType: 'warning' },
  { value: 'DEPTH', label: '深度', tagType: 'warning' },
  { value: 'STREAM', label: '方向', tagType: 'info' }
]

const proxy = getCurrentInstance()!.proxy as any

/* ------------------------------ 专业 / 版本 ------------------------------ */

const majorOptions = ref<EduMajor[]>([])
const majorId = ref<number | undefined>(undefined)
const majorLoading = ref(false)
const majorError = ref('')

const curriculumList = ref<EduCurriculum[]>([])
const listLoading = ref(false)
const listError = ref('')

const entryYear = ref<number | undefined>(new Date().getFullYear())
const matchLoading = ref(false)
const matchError = ref('')
const matchDone = ref(false)
const matchedCurriculum = ref<EduCurriculum | null>(null)

function majorLabel(item: EduMajor): string {
  if (!item) {
    return ''
  }
  const name = item.majorNameZh || item.majorNameEn || item.majorCode || ''
  return item.majorCode && item.majorNameZh ? `${item.majorCode} ${item.majorNameZh}` : name || '未命名专业'
}

function majorName(id?: number): string {
  if (id == null) {
    return '-'
  }
  const item = majorOptions.value.find((major) => major.majorId === id)
  return item ? majorLabel(item) : `专业 #${id}`
}

/** 专业列表（GET /edu/majors/list） */
function loadMajors(): void {
  majorLoading.value = true
  majorError.value = ''
  listMajor({ pageNum: 1, pageSize: 200 })
    .then((res) => {
      majorOptions.value = res.rows ?? []
      if (majorId.value == null && majorOptions.value.length) {
        majorId.value = majorOptions.value[0].majorId
      }
      if (majorId.value != null) {
        loadCurricula()
      }
    })
    .catch(() => {
      majorOptions.value = []
      majorError.value = '专业列表加载失败，请稍后重试。'
    })
    .finally(() => {
      majorLoading.value = false
    })
}

/** 培养方案版本列表（GET /edu/curricula/list） */
function loadCurricula(): void {
  if (majorId.value == null) {
    curriculumList.value = []
    return
  }
  listLoading.value = true
  listError.value = ''
  listCurriculum({ majorId: majorId.value, pageNum: 1, pageSize: 100 })
    .then((res) => {
      curriculumList.value = res.rows ?? []
    })
    .catch(() => {
      curriculumList.value = []
      listError.value = '方案版本列表加载失败，请稍后重试。'
    })
    .finally(() => {
      listLoading.value = false
    })
}

function handleMajorChange(): void {
  selectedCurriculum.value = null
  detailItems.value = []
  activeGroups.value = []
  matchDone.value = false
  matchedCurriculum.value = null
  matchError.value = ''
  loadCurricula()
}

function reloadAll(): void {
  loadMajors()
}

/** 按入学年份匹配版本（GET /edu/curricula/by-major） */
function handleMatchVersion(): void {
  if (majorId.value == null) {
    proxy.$modal.msgWarning('请先选择专业')
    return
  }
  if (entryYear.value == null) {
    proxy.$modal.msgWarning('请填写入学年份')
    return
  }
  matchLoading.value = true
  matchError.value = ''
  matchDone.value = false
  matchedCurriculum.value = null
  getCurriculumByMajor(majorId.value, entryYear.value)
    .then((res) => {
      matchedCurriculum.value = res.data ?? null
      matchDone.value = true
    })
    .catch(() => {
      matchedCurriculum.value = null
      matchError.value = '版本匹配失败，请稍后重试。'
    })
    .finally(() => {
      matchLoading.value = false
    })
}

function viewMatched(): void {
  const matched = matchedCurriculum.value
  if (!matched) {
    return
  }
  const inList = curriculumList.value.find((item) => item.curriculumId === matched.curriculumId)
  const table = proxy.$refs['curriculumTableRef']
  if (inList && table && typeof table.setCurrentRow === 'function') {
    table.setCurrentRow(inList)
    return
  }
  if (matched.curriculumId != null) {
    loadDetail(matched.curriculumId)
  }
}

/* ------------------------------ 方案详情 ------------------------------ */

const selectedCurriculum = ref<EduCurriculum | null>(null)
const detailItems = ref<EduCurriculumItem[]>([])
const detailLoading = ref(false)
const detailError = ref('')
const activeGroups = ref<string[]>([])

function handleCurrentChange(row: EduCurriculum | null): void {
  if (!row || row.curriculumId == null) {
    selectedCurriculum.value = null
    detailItems.value = []
    return
  }
  loadDetail(row.curriculumId)
}

/** 方案详情（GET /edu/curricula/{curriculumId}，含 items） */
function loadDetail(curriculumId: number): void {
  detailLoading.value = true
  detailError.value = ''
  getCurriculum(curriculumId)
    .then((res) => {
      selectedCurriculum.value = res.data ?? null
      detailItems.value = res.data?.items ?? []
      activeGroups.value = itemGroups.value.map((group) => group.type)
    })
    .catch(() => {
      selectedCurriculum.value = null
      detailItems.value = []
      detailError.value = '方案详情加载失败，请稍后重试。'
    })
    .finally(() => {
      detailLoading.value = false
    })
}

/** 按 requirementType 分组（契约 2） */
const itemGroups = computed<ItemGroup[]>(() => {
  const grouped = new Map<string, EduCurriculumItem[]>()
  detailItems.value.forEach((item) => {
    const type = item.requirementType || 'OTHER'
    const rows = grouped.get(type) ?? []
    rows.push(item)
    grouped.set(type, rows)
  })
  const order = REQUIREMENT_TYPES.map((item) => item.value)
  const types = Array.from(grouped.keys()).sort((a, b) => {
    const indexA = order.indexOf(a)
    const indexB = order.indexOf(b)
    return (indexA === -1 ? order.length : indexA) - (indexB === -1 ? order.length : indexB)
  })
  return types.map((type) => {
    const items = grouped.get(type) ?? []
    const configured = REQUIREMENT_TYPES.find((item) => item.value === type)
    const minUnits = items.reduce((sum, item) => sum + (item.minUnits ?? 0), 0)
    return {
      type,
      label: configured?.label ?? type,
      tagType: configured?.tagType ?? 'info',
      minUnits: Math.round(minUnits * 10) / 10,
      items
    }
  })
})

/* ---------------------------- 方案条目编辑 ---------------------------- */

const itemOpen = ref(false)
const itemSaving = ref(false)
const itemDraft = ref<EduCurriculumItem[]>([])
const courseOptions = ref<EduCourse[]>([])
const courseOptionsLoading = ref(false)

const courseSelectOptions = computed(() =>
  courseOptions.value
    .filter((item) => item.courseId != null)
    .map((item) => ({ value: Number(item.courseId), label: [item.code, item.titleZh || item.titleEn].filter((part) => !!part).join(' ') }))
)

const draftUnits = computed(() => Math.round(itemDraft.value.reduce((sum, row) => sum + (row.minUnits ?? 0), 0) * 10) / 10)

function openItemEditor(): void {
  const curriculum = selectedCurriculum.value
  if (!curriculum || curriculum.curriculumId == null) {
    return
  }
  itemDraft.value = detailItems.value.map((item) => ({ ...item }))
  // 已关联课程需出现在下拉框中
  courseOptions.value = detailItems.value
    .filter((item) => item.courseId != null)
    .map((item) => ({ courseId: item.courseId, code: item.courseCode, titleZh: item.courseTitle }))
  itemOpen.value = true
}

function addItemRow(): void {
  itemDraft.value.push({ requirementType: 'REQUIRED', minUnits: 0, sortOrder: itemDraft.value.length + 1 })
}

function removeItemRow(index: number): void {
  itemDraft.value.splice(index, 1)
}

function handleItemCourseChange(row: EduCurriculumItem): void {
  const course = courseOptions.value.find((item) => item.courseId === row.courseId)
  if (course && course.code) {
    row.courseCode = course.code
  }
}

/** 课程远程搜索（复用课程列表接口） */
function searchCourses(keyword: string): void {
  courseOptionsLoading.value = true
  const query: CourseListQuery = { pageNum: 1, pageSize: 20, code: keyword || undefined, title: keyword || undefined }
  listCourse(query)
    .then((res) => {
      courseOptions.value = res.rows ?? []
    })
    .catch(() => {
      courseOptions.value = []
    })
    .finally(() => {
      courseOptionsLoading.value = false
    })
}

/** 覆盖式保存条目（PUT /edu/curricula/{curriculumId}/items） */
function submitItems(): void {
  const curriculum = selectedCurriculum.value
  if (!curriculum || curriculum.curriculumId == null) {
    return
  }
  if (itemDraft.value.some((row) => row.courseId == null && !row.courseCode)) {
    proxy.$modal.msgWarning('每条条目至少需要选择课程或填写课程代码')
    return
  }
  const payload: EduCurriculumItem[] = itemDraft.value.map((row, index) => ({
    courseId: row.courseId,
    courseCode: row.courseCode ?? '',
    requirementType: row.requirementType || 'REQUIRED',
    section: row.section ?? '',
    stream: row.stream ?? '',
    minUnits: row.minUnits ?? 0,
    sortOrder: row.sortOrder ?? index + 1
  }))
  itemSaving.value = true
  updateCurriculumItems(curriculum.curriculumId, payload)
    .then(() => {
      proxy.$modal.msgSuccess('方案条目已保存')
      itemOpen.value = false
      loadDetail(curriculum.curriculumId as number)
    })
    .catch(() => {})
    .finally(() => {
      itemSaving.value = false
    })
}

loadMajors()
</script>

<style scoped lang="scss">
.panel {
  margin-bottom: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-weight: 600;
  font-size: 14px;
}

.match-row {
  display: flex;
  gap: 8px;
  align-items: center;

  .match-input {
    flex: 1;
    width: auto;
  }
}

.match-result {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid #d9ecff;
  background: #f4f9ff;
  border-radius: 6px;

  .match-result-title {
    font-weight: 600;
    color: #303133;
  }

  .match-result-meta {
    font-size: 12px;
    color: #909399;
    margin: 4px 0;
  }
}

.group-title {
  display: flex;
  align-items: center;
  gap: 10px;

  .group-meta {
    font-size: 12px;
    color: #909399;
  }
}

.mb16 {
  margin-bottom: 16px;
}

.mt8 {
  margin-top: 8px;
}

.dialog-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;

  .dialog-toolbar-tip {
    font-size: 12px;
    color: #909399;
  }
}
</style>
