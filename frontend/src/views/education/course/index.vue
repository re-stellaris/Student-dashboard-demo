<template>
  <div class="app-container">
    <!-- 检索条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="课程代码" prop="code">
        <el-input v-model="queryParams.code" placeholder="如 CSC1001" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="课程名称" prop="title">
        <el-input v-model="queryParams.title" placeholder="中文名或英文名" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="开课学院" prop="school">
        <el-select v-model="queryParams.school" placeholder="全部学院" clearable filterable style="width: 200px">
          <el-option v-for="item in optionSet.schools" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="课程类别" prop="category">
        <el-select v-model="queryParams.category" placeholder="全部类别" clearable style="width: 160px">
          <el-option v-for="item in optionSet.categories" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="课程层级" prop="level">
        <el-select v-model="queryParams.level" placeholder="全部层级" clearable style="width: 140px">
          <el-option v-for="item in optionSet.levels" :key="item" :label="String(item)" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="大纲" prop="hasOutline">
        <el-select v-model="queryParams.hasOutline" placeholder="不限" clearable style="width: 140px">
          <el-option label="已有大纲" :value="true" />
          <el-option label="缺少大纲" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['edu:course:add']">新增课程</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['edu:course:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['edu:course:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" class="mb8" />
    <el-alert
      v-else
      title="先修关系语义：同一组号内的课程需全部通过（AND），不同组号之间满足任一组即可（OR）。"
      type="info"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-table v-loading="loading" :data="courseList" border stripe empty-text="暂无课程数据" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="课程代码" align="center" prop="code" width="130">
        <template #default="scope">
          <el-link type="primary" :underline="false" @click="openDetail(scope.row)">{{ scope.row.code }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="课程中文名" align="left" prop="titleZh" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="课程英文名" align="left" prop="titleEn" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="学分" align="center" prop="credits" width="90" />
      <el-table-column label="层级" align="center" prop="level" width="90" />
      <el-table-column label="类别" align="center" prop="category" width="110" />
      <el-table-column label="开课学院" align="left" prop="school" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="openDetail(scope.row)">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['edu:course:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['edu:course:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 课程详情抽屉 -->
    <el-drawer v-model="detailVisible" title="课程详情" direction="rtl" size="620px" append-to-body class="detail-drawer">
      <div v-loading="detailLoading" class="drawer-content">
        <h4 class="section-header">课程信息</h4>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="课程代码">{{ detail.code || '-' }}</el-descriptions-item>
          <el-descriptions-item label="课程ID">{{ detail.courseId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="中文名">{{ detail.titleZh || '-' }}</el-descriptions-item>
          <el-descriptions-item label="英文名">{{ detail.titleEn || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学分">{{ detail.credits ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="层级">{{ detail.level ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="类别">{{ detail.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开课学院">{{ detail.school || '-' }}</el-descriptions-item>
          <el-descriptions-item label="大纲状态">{{ detail.outlineStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开课学期">{{ detail.termsStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="先修原文" :span="2">{{ detail.prereqText || '-' }}</el-descriptions-item>
          <el-descriptions-item label="课程简介" :span="2">{{ detail.descriptionZh || detail.descriptionEn || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="section-header">
          先修要求
          <el-button
            v-if="detail.courseId != null"
            class="section-action"
            type="primary"
            link
            icon="Edit"
            @click="openPrereqEditor"
            v-hasPermi="['edu:prereq:edit']"
          >编辑先修关系</el-button>
        </h4>
        <el-alert
          title="组内 AND、组间 OR：同一组号内的课程必须全部通过；不同组号之间满足任一组即可。"
          type="info"
          show-icon
          :closable="false"
          class="mb8"
        />
        <div v-loading="prereqLoading">
          <el-alert v-if="prereqError" :title="prereqError" type="error" show-icon :closable="false" class="mb8" />
          <el-empty v-else-if="!prereqGroups.length" description="该课程未配置先修要求" :image-size="70" />
          <template v-else>
            <div v-for="(group, index) in prereqGroups" :key="group.groupNo">
              <div class="prereq-group">
                <div class="prereq-group-head">
                  <el-tag size="small" type="info">组 {{ group.groupNo }}</el-tag>
                  <span class="prereq-group-hint">组内需全部满足（AND）</span>
                </div>
                <div class="prereq-chips">
                  <el-tag v-for="(item, idx) in group.items" :key="`${group.groupNo}-${idx}`" type="primary" effect="plain" class="prereq-chip">
                    {{ prereqLabel(item) }}
                  </el-tag>
                </div>
              </div>
              <div v-if="index < prereqGroups.length - 1" class="prereq-or">— 或（OR）—</div>
            </div>
            <div v-if="prereqRawText" class="prereq-raw">原始先修文本：{{ prereqRawText }}</div>
          </template>
        </div>
      </div>
    </el-drawer>

    <!-- 先修关系编辑对话框 -->
    <el-dialog v-model="prereqOpen" title="编辑先修关系（覆盖式保存）" width="720px" append-to-body>
      <el-alert
        title="组号相同表示 AND（组内全部满足），组号不同表示 OR（满足任一组即可）。示例：(A 且 B) 或 C → A、B 组号 1，C 组号 2。"
        type="warning"
        show-icon
        :closable="false"
        class="mb8"
      />
      <el-table :data="prereqDraft" border size="small" empty-text="暂未配置先修课程">
        <el-table-column label="先修课程" min-width="320">
          <template #default="scope">
            <el-select
              v-model="scope.row.prereqCourseId"
              filterable
              remote
              reserve-keyword
              clearable
              placeholder="输入课程代码或名称搜索"
              :remote-method="searchPrereqCourses"
              :loading="prereqOptionsLoading"
              style="width: 100%"
            >
              <el-option v-for="opt in prereqSelectOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="先修组号" width="150" align="center">
          <template #default="scope">
            <el-input-number v-model="scope.row.groupNo" :min="1" :max="20" :step="1" step-strictly controls-position="right" style="width: 110px" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="scope">
            <el-button link type="danger" icon="Delete" @click="removePrereqRow(scope.$index)" />
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-toolbar">
        <el-button type="primary" plain icon="Plus" @click="addPrereqRow">添加先修课程</el-button>
        <span class="dialog-toolbar-tip">保存时会由后端做自依赖与环检测。</span>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="prereqSaving" @click="submitPrereq">确 定</el-button>
          <el-button @click="prereqOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新增 / 修改课程对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="courseFormRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="课程代码" prop="code">
              <el-input v-model="form.code" placeholder="如 CSC1001" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程层级" prop="level">
              <el-input-number v-model="form.level" :min="0" :max="9999" :step="1000" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="课程中文名" prop="titleZh">
              <el-input v-model="form.titleZh" placeholder="请输入课程中文名" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程英文名" prop="titleEn">
              <el-input v-model="form.titleEn" placeholder="请输入课程英文名" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="学分" prop="credits">
              <el-input-number v-model="form.credits" :min="0" :max="60" :precision="1" :step="0.5" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程类别" prop="category">
              <el-select v-model="form.category" placeholder="请选择课程类别" clearable filterable allow-create style="width: 100%">
                <el-option v-for="item in optionSet.categories" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="开课学院" prop="school">
          <el-select v-model="form.school" placeholder="请选择开课学院" clearable filterable allow-create style="width: 100%">
            <el-option v-for="item in optionSet.schools" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="saving" @click="submitForm">确 定</el-button>
          <el-button @click="open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="EduCourse">
import {
  addCourse,
  delCourse,
  getCourse,
  getCourseOptions,
  listCourse,
  listPrereq,
  updateCourse,
  updatePrereq
} from '@/api/education/course'
import type { CourseQuery, EduCourse, EduCourseOption, EduCoursePrereq } from '@/types/education'

/** 课程列表查询参数：分页字段必填，便于 pagination 双向绑定 */
type CourseListQuery = CourseQuery & { pageNum: number; pageSize: number }

/** 先修编辑草稿行（契约 1：body [{ prereqCourseId, groupNo }]） */
interface PrereqDraftRow {
  prereqCourseId?: number
  groupNo: number
}

const proxy = getCurrentInstance()!.proxy as any

const courseList = ref<EduCourse[]>([])
const loading = ref(false)
const errorMsg = ref('')
const showSearch = ref(true)
const total = ref(0)
const courseIds = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)

const queryParams = ref<CourseListQuery>({
  pageNum: 1,
  pageSize: 10,
  code: undefined,
  title: undefined,
  school: undefined,
  category: undefined,
  level: undefined,
  hasOutline: undefined
})
const optionSet = ref<EduCourseOption>({ schools: [], categories: [], levels: [] })

const rules = {
  code: [{ required: true, message: '课程代码不能为空', trigger: 'blur' }],
  titleZh: [{ required: true, message: '课程中文名不能为空', trigger: 'blur' }],
  credits: [{ required: true, message: '学分不能为空', trigger: 'blur' }]
}

/** 课程列表（GET /edu/courses/list） */
function getList(): void {
  loading.value = true
  errorMsg.value = ''
  listCourse(queryParams.value)
    .then((res) => {
      courseList.value = res.rows ?? []
      total.value = res.total ?? 0
    })
    .catch(() => {
      courseList.value = []
      total.value = 0
      errorMsg.value = '课程列表加载失败，请稍后重试。'
    })
    .finally(() => {
      loading.value = false
    })
}

/** 筛选选项（GET /edu/courses/options） */
function loadOptions(): void {
  getCourseOptions()
    .then((res) => {
      if (res.data) {
        optionSet.value = res.data
      }
    })
    .catch(() => {
      optionSet.value = { schools: [], categories: [], levels: [] }
    })
}

function handleQuery(): void {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery(): void {
  queryParams.value = {
    pageNum: 1,
    pageSize: queryParams.value.pageSize,
    code: undefined,
    title: undefined,
    school: undefined,
    category: undefined,
    level: undefined,
    hasOutline: undefined
  }
  proxy.resetForm('queryRef')
  getList()
}

function handleSelectionChange(selection: EduCourse[]): void {
  courseIds.value = selection.map((item) => item.courseId).filter((id): id is number => id != null)
  single.value = selection.length !== 1
  multiple.value = selection.length === 0
}

/* ------------------------------ 课程详情 ------------------------------ */

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<EduCourse>({} as EduCourse)
const prereqList = ref<EduCoursePrereq[]>([])
const prereqLoading = ref(false)
const prereqError = ref('')

function openDetail(row: EduCourse): void {
  detailVisible.value = true
  detail.value = row
  prereqList.value = []
  prereqError.value = ''
  const courseId = row.courseId
  if (courseId == null) {
    return
  }
  detailLoading.value = true
  getCourse(courseId)
    .then((res) => {
      if (res.data) {
        detail.value = res.data
      }
    })
    .catch(() => {
      detail.value = row
    })
    .finally(() => {
      detailLoading.value = false
    })
  loadPrereqs(courseId)
}

/** 先修关系（GET /edu/courses/{courseId}/prereqs） */
function loadPrereqs(courseId: number): Promise<void> {
  prereqLoading.value = true
  prereqError.value = ''
  return listPrereq(courseId)
    .then((res) => {
      prereqList.value = res.data ?? []
    })
    .catch(() => {
      prereqList.value = []
      prereqError.value = '先修关系加载失败，请稍后重试。'
    })
    .finally(() => {
      prereqLoading.value = false
    })
}

/** 按组号聚合：组号相同为 AND，组号不同为 OR */
const prereqGroups = computed(() => {
  const grouped = new Map<number, EduCoursePrereq[]>()
  prereqList.value.forEach((item) => {
    const groupNo = item.groupNo ?? 1
    const rows = grouped.get(groupNo) ?? []
    rows.push(item)
    grouped.set(groupNo, rows)
  })
  return Array.from(grouped.entries())
    .sort((a, b) => a[0] - b[0])
    .map(([groupNo, items]) => ({ groupNo, items }))
})

const prereqRawText = computed(() => {
  const texts = prereqList.value.map((item) => item.rawText).filter((text): text is string => !!text)
  return Array.from(new Set(texts)).join(' / ')
})

function prereqLabel(row: EduCoursePrereq): string {
  const code = row.prereqCourseCode || row.code || ''
  const title = row.prereqCourseTitle || row.title || ''
  if (code && title) {
    return `${code} ${title}`
  }
  if (code || title) {
    return code || title
  }
  return row.prereqCourseId != null ? `课程 #${row.prereqCourseId}` : '未知课程'
}

/* ---------------------------- 先修关系编辑 ---------------------------- */

const prereqOpen = ref(false)
const prereqSaving = ref(false)
const prereqDraft = ref<PrereqDraftRow[]>([])
const prereqOptions = ref<EduCourse[]>([])
const prereqOptionsLoading = ref(false)

const prereqSelectOptions = computed(() =>
  prereqOptions.value
    .filter((item) => item.courseId != null)
    .map((item) => ({ value: Number(item.courseId), label: courseLabel(item) }))
)

function courseLabel(item: EduCourse): string {
  const label = [item.code, item.titleZh || item.titleEn].filter((part) => !!part).join(' ')
  if (label) {
    return label
  }
  return item.courseId != null ? `课程 #${item.courseId}` : '未知课程'
}

function openPrereqEditor(): void {
  prereqDraft.value = prereqGroups.value.flatMap((group) =>
    group.items.map((item) => ({ prereqCourseId: item.prereqCourseId, groupNo: group.groupNo }))
  )
  if (!prereqDraft.value.length) {
    prereqDraft.value = [{ prereqCourseId: undefined, groupNo: 1 }]
  }
  // 已选先修课程必须立即出现在下拉框中（避免只显示 ID）
  prereqOptions.value = prereqGroups.value.flatMap((group) =>
    group.items
      .filter((item) => item.prereqCourseId != null)
      .map((item) => ({
        courseId: item.prereqCourseId,
        code: item.prereqCourseCode || item.code,
        titleZh: item.prereqCourseTitle || item.title
      }))
  )
  prereqOpen.value = true
}

function addPrereqRow(): void {
  const lastGroup = prereqDraft.value.reduce((max, row) => Math.max(max, row.groupNo), 0)
  prereqDraft.value.push({ prereqCourseId: undefined, groupNo: lastGroup || 1 })
}

function removePrereqRow(index: number): void {
  prereqDraft.value.splice(index, 1)
}

/** 先修课程远程搜索（复用课程列表接口） */
function searchPrereqCourses(keyword: string): void {
  prereqOptionsLoading.value = true
  const query: CourseListQuery = {
    pageNum: 1,
    pageSize: 20,
    code: keyword || undefined,
    title: keyword || undefined
  }
  listCourse(query)
    .then((res) => {
      prereqOptions.value = res.rows ?? []
    })
    .catch(() => {
      prereqOptions.value = []
    })
    .finally(() => {
      prereqOptionsLoading.value = false
    })
}

/** 覆盖式保存先修关系（PUT /edu/courses/{courseId}/prereqs） */
function submitPrereq(): void {
  const courseId = detail.value.courseId
  if (courseId == null) {
    return
  }
  const rows = prereqDraft.value.filter((row) => row.prereqCourseId != null)
  if (rows.some((row) => row.prereqCourseId === courseId)) {
    proxy.$modal.msgWarning('先修课程不能是课程自身')
    return
  }
  const seen = new Set<number>()
  for (const row of rows) {
    const id = Number(row.prereqCourseId)
    if (seen.has(id)) {
      proxy.$modal.msgWarning('同一先修课程只能出现一次')
      return
    }
    seen.add(id)
  }
  prereqSaving.value = true
  updatePrereq(
    courseId,
    rows.map((row) => ({ prereqCourseId: Number(row.prereqCourseId), groupNo: row.groupNo }))
  )
    .then(() => {
      proxy.$modal.msgSuccess('先修关系已保存')
      prereqOpen.value = false
      loadPrereqs(courseId)
    })
    .catch(() => {})
    .finally(() => {
      prereqSaving.value = false
    })
}

/* ---------------------------- 新增 / 修改 ---------------------------- */

const open = ref(false)
const saving = ref(false)
const title = ref('')
const form = ref<EduCourse>({} as EduCourse)

function reset(): void {
  form.value = {} as EduCourse
  proxy.resetForm('courseFormRef')
}

function handleAdd(): void {
  reset()
  title.value = '新增课程'
  open.value = true
}

function handleUpdate(row?: EduCourse): void {
  const courseId = row?.courseId ?? courseIds.value[0]
  if (courseId == null) {
    return
  }
  reset()
  getCourse(courseId)
    .then((res) => {
      form.value = res.data ?? ({} as EduCourse)
      title.value = '修改课程'
      open.value = true
    })
    .catch(() => {})
}

function submitForm(): void {
  proxy.$refs['courseFormRef'].validate((valid: boolean) => {
    if (!valid) {
      return
    }
    saving.value = true
    const isUpdate = form.value.courseId != null
    const request = isUpdate ? updateCourse(form.value) : addCourse(form.value)
    request
      .then(() => {
        proxy.$modal.msgSuccess(isUpdate ? '修改成功' : '新增成功')
        open.value = false
        getList()
      })
      .catch(() => {})
      .finally(() => {
        saving.value = false
      })
  })
}

function handleDelete(row?: EduCourse): void {
  const ids = row?.courseId ?? courseIds.value
  if (ids == null || (Array.isArray(ids) && ids.length === 0)) {
    return
  }
  proxy.$modal
    .confirm(`是否确认删除课程编号为"${Array.isArray(ids) ? ids.join(',') : ids}"的数据项？`)
    .then(() => delCourse(ids))
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(() => {})
}

getList()
loadOptions()
</script>

<style scoped lang="scss">
.section-action {
  float: right;
  font-size: 13px;
}

.prereq-group {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fafcff;

  .prereq-group-head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }

  .prereq-group-hint {
    font-size: 12px;
    color: #909399;
  }

  .prereq-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

.prereq-or {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  padding: 6px 0;
}

.prereq-raw {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  word-break: break-all;
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
