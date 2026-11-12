<template>
  <div class="app-container">
    <!-- 页面标题区 -->
    <div class="lg-head">
      <div>
        <h2>教师授课</h2>
        <p>查看本人授课的课程与作业，发布作业并为学生提交评分。</p>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 左：我的授课 -->
      <el-col :xs="24" :md="8" :lg="7">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">我的授课</span>
              <el-button link type="primary" icon="Refresh" :loading="offeringLoading" @click="loadOfferings">刷新</el-button>
            </div>
          </template>
          <el-alert v-if="offeringError" :title="offeringError" type="error" show-icon :closable="false" class="mb8" />
          <div v-loading="offeringLoading">
            <el-empty v-if="!offerings.length && !offeringError" description="暂无授课任务" :image-size="70" />
            <div
              v-for="item in offerings"
              :key="item.offeringId"
              class="offering-card"
              :class="{ active: item.offeringId === offeringId }"
              @click="selectOffering(item.offeringId)"
            >
              <div class="offering-head">
                <span class="code">{{ item.courseCode || '—' }}</span>
                <el-tag size="small" :type="item.status === '0' ? 'success' : 'info'">{{ item.status === '0' ? '开放' : '关闭' }}</el-tag>
              </div>
              <div class="offering-title">{{ item.courseTitle || '未命名课程' }}</div>
              <div class="offering-meta">{{ item.termName || item.termCode || '未知学期' }} · {{ item.units ?? '-' }} 学分</div>
              <div class="offering-meta">{{ item.scheduleText || '未安排上课时间' }}</div>
              <div class="offering-stats">
                <span>学生 {{ item.studentCount ?? item.enrolledCount ?? 0 }}</span>
                <span>作业 {{ item.assignmentCount ?? 0 }}</span>
                <span>容量 {{ item.capacity ?? '-' }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右：作业与提交 -->
      <el-col :xs="24" :md="16" :lg="17">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">作业管理<span v-if="selectedOffering" class="panel-subtitle"> · {{ selectedOffering.courseCode }} {{ selectedOffering.courseTitle }}</span></span>
              <div class="panel-actions">
                <el-button type="primary" plain size="small" icon="Plus" :disabled="offeringId == null" @click="openAssignmentDialog()" v-hasPermi="['edu:assignment:add']">发布作业</el-button>
              </div>
            </div>
          </template>

          <el-alert v-if="offeringId == null && !offeringLoading" title="请先在左侧选择一门授课课程" type="info" show-icon :closable="false" class="mb8" />
          <el-alert v-else-if="assignmentError" :title="assignmentError" type="error" show-icon :closable="false" class="mb8" />
          <el-table
            v-if="offeringId != null"
            v-loading="assignmentLoading"
            :data="assignments"
            border
            stripe
            highlight-current-row
            empty-text="暂无作业，点击「发布作业」创建"
            @current-change="handleAssignmentChange"
          >
            <el-table-column label="作业标题" prop="title" min-width="180" :show-overflow-tooltip="true" />
            <el-table-column label="截止时间" prop="dueAt" width="170" align="center" />
            <el-table-column label="满分" prop="totalScore" width="80" align="center" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag size="small" :type="assignmentStatusTag(scope.row.status)">{{ assignmentStatusLabel(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" align="center">
              <template #default="scope">
                <el-button link type="primary" icon="Edit" @click.stop="openAssignmentDialog(scope.row)" v-hasPermi="['edu:assignment:edit']">修改</el-button>
                <el-button link type="danger" icon="Delete" @click.stop="handleDeleteAssignment(scope.row)" v-hasPermi="['edu:assignment:edit']">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-divider content-position="left">提交与评分</el-divider>
          <el-alert
            v-if="assignmentId == null"
            title="请选择上表中的一次作业，以查看学生提交并评分"
            type="info"
            show-icon
            :closable="false"
          />
          <template v-else>
            <el-alert v-if="submissionError" :title="submissionError" type="error" show-icon :closable="false" class="mb8" />
            <el-table v-loading="submissionLoading" :data="submissions" border stripe empty-text="暂无学生提交">
              <el-table-column label="学号" prop="studentNo" width="130" align="center">
                <template #default="scope">
                  <span>{{ scope.row.studentNo || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="姓名" prop="studentName" width="110" align="center" />
              <el-table-column label="提交时间" prop="submittedAt" width="170" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="scope">
                  <el-tag size="small" :type="submissionStatusTag(scope.row.status)">{{ submissionStatusLabel(scope.row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="得分" width="110" align="center">
                <template #default="scope">
                  <span v-if="scope.row.grade != null">{{ scope.row.grade }} / {{ scope.row.totalScore ?? currentAssignment?.totalScore ?? '-' }}</span>
                  <span v-else class="text-muted">未评分</span>
                </template>
              </el-table-column>
              <el-table-column label="提交内容" min-width="200" :show-overflow-tooltip="true">
                <template #default="scope">
                  <span>{{ scope.row.content || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="110" align="center">
                <template #default="scope">
                  <el-button link type="primary" icon="Edit" @click="openGradeDialog(scope.row)" v-hasPermi="['edu:submission:grade']">评分</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <!-- 作业编辑对话框 -->
    <el-dialog :title="assignmentTitle" v-model="assignmentOpen" width="640px" append-to-body>
      <el-form ref="assignmentFormRef" :model="assignmentForm" :rules="assignmentRules" label-width="90px">
        <el-form-item label="作业标题" prop="title">
          <el-input v-model="assignmentForm.title" placeholder="请输入作业标题" maxlength="200" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="截止时间" prop="dueAt">
              <el-date-picker
                v-model="assignmentForm.dueAt"
                type="datetime"
                placeholder="请选择截止时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="满分" prop="totalScore">
              <el-input-number v-model="assignmentForm.totalScore" :min="0" :max="1000" :precision="1" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-select v-model="assignmentForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option v-for="item in ASSIGNMENT_STATUS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业说明">
          <el-input v-model="assignmentForm.description" type="textarea" :rows="4" placeholder="请输入作业说明" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="assignmentSaving" @click="submitAssignment">确 定</el-button>
          <el-button @click="assignmentOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 评分对话框 -->
    <el-dialog v-model="gradeOpen" title="作业评分" width="640px" append-to-body>
      <el-descriptions v-if="currentSubmission" :column="2" border size="small" class="mb16">
        <el-descriptions-item label="学生">{{ currentSubmission.studentName || currentSubmission.studentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentSubmission.submittedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="作业">{{ currentAssignment?.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="满分">{{ currentSubmission.totalScore ?? currentAssignment?.totalScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交内容" :span="2">{{ currentSubmission.content || '—' }}</el-descriptions-item>
        <el-descriptions-item label="附件" :span="2">
          <el-link v-if="currentSubmission.attachmentUrl" type="primary" :href="currentSubmission.attachmentUrl" target="_blank">查看附件</el-link>
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-form ref="gradeFormRef" :model="gradeForm" :rules="gradeRules" label-width="90px">
        <el-form-item label="得分" prop="grade">
          <el-input-number v-model="gradeForm.grade" :min="0" :max="maxGrade" :precision="1" :step="1" controls-position="right" style="width: 180px" />
          <span class="grade-hint">满分 {{ maxGrade }}</span>
        </el-form-item>
        <el-form-item label="教师评语" prop="feedback">
          <el-input v-model="gradeForm.feedback" type="textarea" :rows="4" placeholder="请输入评语（可选）" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="gradeSaving" @click="submitGrade">确 定</el-button>
          <el-button @click="gradeOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="EduTeaching">
import { listMyOffering } from '@/api/education/enrollment'
import {
  addAssignment,
  delAssignment,
  gradeSubmission,
  listAssignment,
  listSubmission,
  updateAssignment
} from '@/api/education/assignment'
import type { EduAssignment, EduCourseOffering, EduSubmission } from '@/types/education'

interface StatusOption {
  value: string
  label: string
}

const ASSIGNMENT_STATUS: StatusOption[] = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'CLOSED', label: '已关闭' }
]

const SUBMISSION_STATUS: StatusOption[] = [
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'GRADED', label: '已评分' },
  { value: 'RETURNED', label: '已退回' }
]

const proxy = getCurrentInstance()!.proxy as any

/* ------------------------------ 我的授课 ------------------------------ */

const offerings = ref<EduCourseOffering[]>([])
const offeringLoading = ref(false)
const offeringError = ref('')
const offeringId = ref<number | undefined>(undefined)
const selectedOffering = computed(() => offerings.value.find((item) => item.offeringId === offeringId.value) ?? null)

/** 当前教师授课列表（GET /edu/offerings/mine） */
function loadOfferings(): Promise<void> {
  offeringLoading.value = true
  offeringError.value = ''
  return listMyOffering({ pageNum: 1, pageSize: 100 })
    .then((res) => {
      offerings.value = res.rows ?? []
      const exists = offerings.value.some((item) => item.offeringId === offeringId.value)
      if (!exists) {
        offeringId.value = offerings.value[0]?.offeringId
      }
      if (offeringId.value != null) {
        loadAssignments()
      } else {
        assignments.value = []
      }
    })
    .catch(() => {
      offerings.value = []
      offeringId.value = undefined
      offeringError.value = '授课列表加载失败（需要 edu:offering:list 权限）。'
    })
    .finally(() => {
      offeringLoading.value = false
    })
}

function selectOffering(id?: number): void {
  if (id == null || id === offeringId.value) {
    return
  }
  offeringId.value = id
  assignmentId.value = undefined
  currentAssignment.value = null
  submissions.value = []
  loadAssignments()
}

/* ------------------------------ 作业列表 ------------------------------ */

const assignments = ref<EduAssignment[]>([])
const assignmentLoading = ref(false)
const assignmentError = ref('')
const assignmentId = ref<number | undefined>(undefined)
const currentAssignment = ref<EduAssignment | null>(null)

function assignmentStatusLabel(status?: string): string {
  return ASSIGNMENT_STATUS.find((item) => item.value === status)?.label ?? status ?? '未知'
}

function assignmentStatusTag(status?: string): 'success' | 'warning' | 'info' | 'danger' {
  switch ((status || '').toUpperCase()) {
    case 'PUBLISHED':
      return 'success'
    case 'DRAFT':
      return 'warning'
    case 'CLOSED':
      return 'info'
    default:
      return 'info'
  }
}

/** 作业列表（GET /edu/assignments/list?offeringId=） */
function loadAssignments(): Promise<void> {
  const targetOfferingId = offeringId.value
  if (targetOfferingId == null) {
    assignments.value = []
    return Promise.resolve()
  }
  assignmentLoading.value = true
  assignmentError.value = ''
  return listAssignment({ offeringId: targetOfferingId, pageNum: 1, pageSize: 100 })
    .then((res) => {
      assignments.value = res.rows ?? []
      assignmentId.value = undefined
      currentAssignment.value = null
      submissions.value = []
    })
    .catch(() => {
      assignments.value = []
      assignmentError.value = '作业列表加载失败，请稍后重试。'
    })
    .finally(() => {
      assignmentLoading.value = false
    })
}

function handleAssignmentChange(row: EduAssignment | null): void {
  currentAssignment.value = row
  assignmentId.value = row?.assignmentId
  submissions.value = []
  if (row?.assignmentId != null) {
    loadSubmissions()
  }
}

/* ------------------------------ 提交列表 ------------------------------ */

const submissions = ref<EduSubmission[]>([])
const submissionLoading = ref(false)
const submissionError = ref('')

function submissionStatusLabel(status?: string): string {
  return SUBMISSION_STATUS.find((item) => item.value === status)?.label ?? status ?? '未知'
}

function submissionStatusTag(status?: string): 'success' | 'warning' | 'info' | 'danger' {
  switch ((status || '').toUpperCase()) {
    case 'GRADED':
      return 'success'
    case 'SUBMITTED':
      return 'warning'
    case 'RETURNED':
      return 'danger'
    default:
      return 'info'
  }
}

/** 提交列表（GET /edu/submissions/list?assignmentId=） */
function loadSubmissions(): Promise<void> {
  const targetAssignmentId = assignmentId.value
  if (targetAssignmentId == null) {
    submissions.value = []
    return Promise.resolve()
  }
  submissionLoading.value = true
  submissionError.value = ''
  return listSubmission({ assignmentId: targetAssignmentId, pageNum: 1, pageSize: 100 })
    .then((res) => {
      submissions.value = res.rows ?? []
    })
    .catch(() => {
      submissions.value = []
      submissionError.value = '提交列表加载失败，请稍后重试。'
    })
    .finally(() => {
      submissionLoading.value = false
    })
}

/* ---------------------------- 作业新增 / 修改 ---------------------------- */

const assignmentOpen = ref(false)
const assignmentSaving = ref(false)
const assignmentTitle = ref('')
const assignmentForm = ref<EduAssignment>({} as EduAssignment)

const assignmentRules = {
  title: [{ required: true, message: '作业标题不能为空', trigger: 'blur' }],
  totalScore: [{ required: true, message: '满分不能为空', trigger: 'blur' }]
}

function openAssignmentDialog(row?: EduAssignment): void {
  if (offeringId.value == null) {
    proxy.$modal.msgWarning('请先选择授课课程')
    return
  }
  proxy.resetForm('assignmentFormRef')
  if (row) {
    assignmentForm.value = { ...row }
    assignmentTitle.value = '修改作业'
  } else {
    assignmentForm.value = {
      offeringId: offeringId.value,
      title: '',
      description: '',
      dueAt: undefined,
      totalScore: 100,
      status: 'DRAFT'
    }
    assignmentTitle.value = '发布作业'
  }
  assignmentOpen.value = true
}

function submitAssignment(): void {
  proxy.$refs['assignmentFormRef'].validate((valid: boolean) => {
    if (!valid) {
      return
    }
    const isUpdate = assignmentForm.value.assignmentId != null
    const payload: EduAssignment = { ...assignmentForm.value, offeringId: offeringId.value }
    assignmentSaving.value = true
    const request = isUpdate ? updateAssignment(payload) : addAssignment(payload)
    request
      .then(() => {
        proxy.$modal.msgSuccess(isUpdate ? '作业已更新' : '作业已发布')
        assignmentOpen.value = false
        loadAssignments()
      })
      .catch(() => {})
      .finally(() => {
        assignmentSaving.value = false
      })
  })
}

function handleDeleteAssignment(row: EduAssignment): void {
  if (row.assignmentId == null) {
    return
  }
  proxy.$modal
    .confirm(`是否确认删除作业"${row.title || row.assignmentId}"？`)
    .then(() => delAssignment(row.assignmentId as number))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      loadAssignments()
    })
    .catch(() => {})
}

/* ------------------------------ 评分 ------------------------------ */

const gradeOpen = ref(false)
const gradeSaving = ref(false)
const currentSubmission = ref<EduSubmission | null>(null)
const gradeForm = ref<{ grade?: number; feedback: string }>({ grade: undefined, feedback: '' })

const maxGrade = computed(() => currentSubmission.value?.totalScore ?? currentAssignment.value?.totalScore ?? 100)

const gradeRules = {
  grade: [{ required: true, message: '得分不能为空', trigger: 'blur' }]
}

function openGradeDialog(row: EduSubmission): void {
  currentSubmission.value = row
  gradeForm.value = { grade: row.grade, feedback: row.feedback || '' }
  proxy.resetForm('gradeFormRef')
  gradeOpen.value = true
}

/** 评分（PUT /edu/submissions/grade） */
function submitGrade(): void {
  proxy.$refs['gradeFormRef'].validate((valid: boolean) => {
    if (!valid) {
      return
    }
    const submissionId = currentSubmission.value?.submissionId
    if (submissionId == null) {
      return
    }
    if (gradeForm.value.grade == null) {
      proxy.$modal.msgWarning('得分不能为空')
      return
    }
    gradeSaving.value = true
    gradeSubmission({
      submissionId,
      grade: gradeForm.value.grade,
      feedback: gradeForm.value.feedback
    })
      .then(() => {
        proxy.$modal.msgSuccess('评分已保存')
        gradeOpen.value = false
        loadSubmissions()
      })
      .catch(() => {})
      .finally(() => {
        gradeSaving.value = false
      })
  })
}

onMounted(() => {
  loadOfferings()
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
  font-weight: 400;
  font-size: 12px;
  color: #909399;
}

.offering-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fafcff;

  &:hover {
    border-color: #c6e2ff;
  }

  &.active {
    border-color: #409eff;
    box-shadow: 0 0 0 1px #409eff inset;
    background: #f2f8ff;
  }

  .offering-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .code {
    font-family: Consolas, 'SFMono-Regular', monospace;
    font-size: 12px;
    color: #409eff;
  }

  .offering-title {
    margin-top: 4px;
    font-size: 13px;
    font-weight: 600;
    color: #303133;
  }

  .offering-meta {
    margin-top: 4px;
    font-size: 12px;
    color: #909399;
  }

  .offering-stats {
    margin-top: 6px;
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: #606266;
  }
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.grade-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.mb16 {
  margin-bottom: 16px;
}
</style>
