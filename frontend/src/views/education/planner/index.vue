<template>
  <div class="app-container">
    <!-- 顶部：学期 / 规划选择 -->
    <el-card shadow="never" class="mb16">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="toolbar-label">学期</span>
          <el-select v-model="termId" placeholder="请选择学期" style="width: 210px" :loading="termLoading" @change="handleTermChange">
            <el-option v-for="item in terms" :key="item.termId" :label="termLabel(item)" :value="item.termId" />
          </el-select>
          <span class="toolbar-label">规划</span>
          <el-select v-model="planId" placeholder="暂存新计划" style="width: 240px" clearable :loading="planLoading" @change="handlePlanChange">
            <el-option v-for="item in plans" :key="item.planId" :label="planLabel(item)" :value="item.planId" />
          </el-select>
          <el-tag v-if="selectedPlan" size="small" :type="planStatusTag(selectedPlan.status)">{{ planStatusLabel(selectedPlan.status) }}</el-tag>
          <el-tag v-else size="small" type="warning">未保存</el-tag>
          <el-button type="primary" plain icon="Plus" @click="openCreatePlan" v-hasPermi="['edu:plan:add']">新建规划</el-button>
        </div>
        <div class="toolbar-right">
          <el-button icon="Refresh" @click="refreshAll">刷新</el-button>
        </div>
      </div>
      <el-alert v-if="termError || planError" :title="termError || planError" type="error" show-icon :closable="false" class="mt8" />
    </el-card>

    <el-row :gutter="16">
      <!-- 左：计划工作台 -->
      <el-col :xs="24" :lg="15">
        <el-card shadow="never" class="mb16">
          <template #header>
            <div class="panel-header">
              <div class="panel-title">
                <span>选课计划</span>
                <span v-if="validationStale" class="stale-tip">（有未校验的修改）</span>
              </div>
              <div class="panel-actions">
                <el-button
                  type="primary"
                  plain
                  size="small"
                  icon="Check"
                  :loading="validating"
                  @click="handleValidate"
                  v-hasPermi="['edu:plan:validate']"
                >校验</el-button>
                <el-button
                  type="success"
                  plain
                  size="small"
                  icon="Document"
                  :loading="saving"
                  @click="handleSave"
                  v-hasPermi="['edu:plan:add']"
                >保存</el-button>
                <el-button
                  type="warning"
                  plain
                  size="small"
                  icon="Promotion"
                  :disabled="planId == null || !workItems.length"
                  :loading="submitting"
                  @click="handleSubmit"
                  v-hasPermi="['edu:plan:submit']"
                >提交</el-button>
                <el-button
                  type="danger"
                  plain
                  size="small"
                  icon="Delete"
                  :disabled="planId == null"
                  @click="handleDeletePlan"
                  v-hasPermi="['edu:plan:add']"
                >删除</el-button>
              </div>
            </div>
          </template>

          <!-- 学分进度 -->
          <div class="units-box">
            <div class="units-head">
              <span class="units-title">学分进度</span>
              <span class="units-value" :class="{ 'units-over': totalUnits > unitLimit }">{{ totalUnits }} / {{ unitLimit }} 学分</span>
            </div>
            <el-progress :percentage="unitsPercent" :status="unitsProgressStatus" :stroke-width="14" :text-inside="false" />
            <div class="units-hint">
              学分上限由后端按学期配置下发；超出上限时校验状态为 WARN/FAIL。
            </div>
          </div>

          <!-- 总体状态与提示 -->
          <el-alert
            v-if="validationResult"
            :title="stateAlertTitle"
            :type="stateAlertType"
            show-icon
            :closable="false"
            class="mb8"
          >
            <div v-if="validationMessages.length" class="state-messages">
              <div v-for="(message, index) in validationMessages" :key="index">· {{ message }}</div>
            </div>
          </el-alert>
          <el-alert v-else-if="validationStale && workItems.length" title="计划已修改，请点击「校验」重新计算结果" type="info" show-icon :closable="false" class="mb8" />
          <el-alert v-if="validateError" :title="validateError" type="error" show-icon :closable="false" class="mb8" />

          <el-table v-loading="detailLoading" :data="workItems" border stripe empty-text="暂无课程，请从右侧搜索或规则推荐中添加">
            <el-table-column label="课程代码" prop="code" width="120" align="center">
              <template #default="scope">
                <span>{{ scope.row.code || (scope.row.courseId != null ? `#${scope.row.courseId}` : '-') }}</span>
              </template>
            </el-table-column>
            <el-table-column label="课程名称" prop="title" min-width="150" :show-overflow-tooltip="true" />
            <el-table-column label="学分" prop="units" width="80" align="center" />
            <el-table-column label="校验状态" width="100" align="center">
              <template #default="scope">
                <el-tag size="small" :type="statusMeta(scope.row.validationStatus).tagType">{{ statusMeta(scope.row.validationStatus).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="可解释说明" min-width="260">
              <template #default="scope">
                <div class="reason-text">{{ scope.row.reason || '—' }}</div>
                <div v-if="scope.row.validationMessage" class="reason-message">{{ scope.row.validationMessage }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="scope">
                <el-button link type="danger" icon="Delete" @click="removeCourse(scope.$index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右：课程搜索 + 规则推荐 -->
      <el-col :xs="24" :lg="9">
        <el-card shadow="never" class="mb16">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">课程检索</span>
              <span class="panel-subtitle">按代码 / 名称</span>
            </div>
          </template>
          <div class="search-row">
            <el-input v-model="searchKeyword" placeholder="如 CSC1001 或 数据结构" clearable @keyup.enter="handleSearchCourse" />
            <el-button type="primary" icon="Search" :loading="searchLoading" @click="handleSearchCourse">搜索</el-button>
          </div>
          <el-alert v-if="searchError" :title="searchError" type="error" show-icon :closable="false" class="mt8" />
          <el-empty v-else-if="searchDone && !searchResults.length" description="未检索到课程" :image-size="60" />
          <el-scrollbar v-else max-height="240px" class="result-scroll">
            <div v-for="course in searchResults" :key="course.courseId" class="result-row">
              <div class="result-main">
                <div class="result-title">
                  <span class="code">{{ course.code || '—' }}</span>
                  <span class="title">{{ course.titleZh || course.titleEn || '未命名课程' }}</span>
                </div>
                <div class="result-meta">{{ course.credits ?? '-' }} 学分 · {{ course.category || '未分类' }} · {{ course.school || '未知学院' }}</div>
              </div>
              <el-button
                size="small"
                type="primary"
                plain
                :disabled="isInPlan(course.courseId)"
                @click="addFromCourse(course)"
              >{{ isInPlan(course.courseId) ? '已加入' : '加入' }}</el-button>
            </div>
          </el-scrollbar>
        </el-card>

        <el-card shadow="never" class="mb16">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">规则推荐</span>
              <el-button link type="primary" icon="Refresh" :loading="recommendLoading" @click="loadRecommendations">刷新</el-button>
            </div>
          </template>
          <el-alert
            title="推荐由确定性规则引擎生成：先修依赖判定 + 学分上限约束 + 培养方案未修课程筛选，并为每门课给出可解释理由。不涉及任何 AI 模型或随机采样。"
            type="info"
            show-icon
            :closable="false"
            class="mb8"
          />
          <el-alert v-if="recommendError" :title="recommendError" type="error" show-icon :closable="false" class="mb8" />
          <div v-else-if="recommendLoading" v-loading="recommendLoading" class="recommend-loading" />
          <el-empty v-else-if="!recommendations.length" description="暂无推荐课程（可能已修完或学期未配置）" :image-size="60" />
          <el-scrollbar v-else max-height="360px">
            <div v-for="item in recommendations" :key="item.courseId" class="recommend-card">
              <div class="recommend-head">
                <span class="code">{{ item.code }}</span>
                <span class="title">{{ item.title }}</span>
                <span class="units">{{ item.units }} 学分</span>
              </div>
              <div class="recommend-reason">{{ item.reason }}</div>
              <div v-if="item.missingPrereqs && item.missingPrereqs.length" class="recommend-missing">
                <span class="missing-label">缺失先修：</span>
                <el-tag v-for="prereq in item.missingPrereqs" :key="prereq.courseId ?? prereq.code" size="small" type="danger" effect="plain">
                  {{ prereq.code || prereq.title || '未知课程' }}
                </el-tag>
              </div>
              <div class="recommend-actions">
                <el-button size="small" type="primary" plain :disabled="isInPlan(item.courseId)" @click="addFromRecommendation(item)">
                  {{ isInPlan(item.courseId) ? '已加入' : '加入计划' }}
                </el-button>
              </div>
            </div>
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建规划对话框 -->
    <el-dialog v-model="createOpen" title="新建选课规划（草稿）" width="520px" append-to-body>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="目标学期">
          <el-input :model-value="currentTermLabel" disabled />
        </el-form-item>
        <el-form-item label="规划标题" prop="title">
          <el-input v-model="createForm.title" placeholder="如 2026-27 秋季选课计划" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="createSaving" @click="submitCreatePlan">确 定</el-button>
          <el-button @click="createOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="EduPlanner">
import { listCourse } from '@/api/education/course'
import { listTerm } from '@/api/education/enrollment'
import {
  addPlan,
  delPlan,
  getPlan,
  listPlan,
  recommendPlan,
  submitPlan,
  updatePlanItems,
  validatePlan,
  validateSavedPlan
} from '@/api/education/plan'
import type {
  CourseQuery,
  EduCourse,
  EduPlanItemInput,
  EduStudyPlan,
  EduStudyPlanItem,
  EduTerm,
  PlanItemValidation,
  PlanQuery,
  PlanValidationResult,
  RecommendedCourse
} from '@/types/education'

/** 课程检索参数（分页必填） */
type CourseListQuery = CourseQuery & { pageNum: number; pageSize: number }

const proxy = getCurrentInstance()!.proxy as any

/* ------------------------------ 学期与规划 ------------------------------ */

const terms = ref<EduTerm[]>([])
const termLoading = ref(false)
const termError = ref('')
const termId = ref<number | undefined>(undefined)

const plans = ref<EduStudyPlan[]>([])
const planLoading = ref(false)
const planError = ref('')
const planId = ref<number | undefined>(undefined)
const selectedPlan = ref<EduStudyPlan | null>(null)

function termLabel(term: EduTerm): string {
  const code = term.termCode || ''
  const name = term.termName || ''
  const label = [code, name].filter((part) => !!part).join(' ')
  return label || (term.termId != null ? `学期 #${term.termId}` : '未知学期')
}

function currentTermLabel(): string {
  const term = terms.value.find((item) => item.termId === termId.value)
  return term ? termLabel(term) : '未选择学期'
}

function planLabel(plan: EduStudyPlan): string {
  return `${plan.title || `规划 #${plan.planId}`}（${planStatusLabel(plan.status)}）`
}

function planStatusLabel(status?: string): string {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT':
      return '草稿'
    case 'VALIDATED':
      return '已校验'
    case 'SUBMITTED':
      return '已提交'
    case 'ARCHIVED':
      return '已归档'
    default:
      return status || '未知状态'
  }
}

function planStatusTag(status?: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT':
      return 'info'
    case 'VALIDATED':
      return 'primary'
    case 'SUBMITTED':
      return 'success'
    case 'ARCHIVED':
      return 'warning'
    default:
      return 'info'
  }
}

/** 学期列表（GET /edu/terms/list），默认选中当前学期 */
function loadTerms(): Promise<void> {
  termLoading.value = true
  termError.value = ''
  return listTerm({ pageNum: 1, pageSize: 100 })
    .then((res) => {
      terms.value = res.rows ?? []
      const current = terms.value.find((item) => item.isCurrent === '1')
      if (termId.value == null) {
        termId.value = current?.termId ?? terms.value[0]?.termId
      }
    })
    .catch(() => {
      terms.value = []
      termError.value = '学期列表加载失败，请稍后重试。'
    })
    .finally(() => {
      termLoading.value = false
    })
}

/** 规划列表（GET /edu/plans/list，学生仅本人） */
function loadPlans(): Promise<void> {
  planLoading.value = true
  planError.value = ''
  const query: PlanQuery = { pageNum: 1, pageSize: 100, termId: termId.value }
  return listPlan(query)
    .then((res) => {
      plans.value = res.rows ?? []
    })
    .catch(() => {
      plans.value = []
      planError.value = '规划列表加载失败或当前账号无规划权限。'
    })
    .finally(() => {
      planLoading.value = false
    })
}

async function handleTermChange(): Promise<void> {
  planId.value = undefined
  selectedPlan.value = null
  workItems.value = []
  validationResult.value = null
  validationStale.value = false
  await loadPlans()
  loadRecommendations()
}

function handlePlanChange(): void {
  const target = planId.value
  if (target == null) {
    selectedPlan.value = null
    workItems.value = []
    validationResult.value = null
    return
  }
  loadPlanDetail(target)
}

async function refreshAll(): Promise<void> {
  if (!terms.value.length) {
    await loadTerms()
  }
  await loadPlans()
  if (planId.value != null) {
    await loadPlanDetail(planId.value)
  }
  loadRecommendations()
}

/* ------------------------------ 计划明细 ------------------------------ */

const workItems = ref<EduStudyPlanItem[]>([])
const detailLoading = ref(false)

/** 规划详情（GET /edu/plans/{planId}） */
function loadPlanDetail(targetPlanId: number): Promise<void> {
  detailLoading.value = true
  return getPlan(targetPlanId)
    .then((res) => {
      selectedPlan.value = res.data?.plan ?? null
      workItems.value = (res.data?.items ?? []).map((item) => ({ ...item }))
      validationResult.value = null
      validationStale.value = false
      validateError.value = ''
    })
    .catch(() => {
      selectedPlan.value = null
      workItems.value = []
    })
    .finally(() => {
      detailLoading.value = false
    })
}

function isInPlan(courseId?: number): boolean {
  return courseId != null && workItems.value.some((item) => item.courseId === courseId)
}

function removeCourse(index: number): void {
  workItems.value.splice(index, 1)
  validationStale.value = true
}

function addFromCourse(course: EduCourse): void {
  pushWorkItem({
    courseId: course.courseId,
    code: course.code,
    title: course.titleZh || course.titleEn,
    units: course.credits
  })
}

function addFromRecommendation(item: RecommendedCourse): void {
  pushWorkItem({
    courseId: item.courseId,
    code: item.code,
    title: item.title,
    units: item.units,
    reason: item.reason
  })
}

function pushWorkItem(item: EduStudyPlanItem): void {
  if (item.courseId == null) {
    return
  }
  if (isInPlan(item.courseId)) {
    proxy.$modal.msgWarning('该课程已在计划中')
    return
  }
  workItems.value.push({
    ...item,
    validationStatus: item.validationStatus || 'UNKNOWN',
    validationMessage: item.validationMessage || '',
    sortOrder: workItems.value.length + 1
  })
  validationStale.value = true
}

function buildItemPayload(): EduPlanItemInput[] {
  return workItems.value
    .filter((item): item is EduStudyPlanItem & { courseId: number } => item.courseId != null)
    .map((item) => ({ courseId: item.courseId }))
}

/* ------------------------------ 校验与状态 ------------------------------ */

const validating = ref(false)
const validateError = ref('')
const validationResult = ref<PlanValidationResult | null>(null)
const validationStale = ref(false)

const validationMessages = computed(() => validationResult.value?.messages ?? [])

const totalUnits = computed(() => {
  const sum = workItems.value.reduce((acc, item) => acc + (item.units ?? 0), 0)
  return Math.round(sum * 10) / 10
})

const unitLimit = computed(() => validationResult.value?.unitLimit ?? selectedPlan.value?.unitLimit ?? 21)

const unitsPercent = computed(() => {
  const limit = unitLimit.value
  if (!limit || limit <= 0) {
    return 0
  }
  return Math.min(100, Math.round((totalUnits.value / limit) * 100))
})

const unitsProgressStatus = computed<'success' | 'warning' | 'exception'>(() => {
  if (totalUnits.value > unitLimit.value) {
    return 'exception'
  }
  return unitsPercent.value >= 90 ? 'warning' : 'success'
})

const stateAlertType = computed<'success' | 'warning' | 'error' | 'info'>(() => {
  switch ((validationResult.value?.state || '').toUpperCase()) {
    case 'PASS':
      return 'success'
    case 'WARN':
      return 'warning'
    case 'FAIL':
      return 'error'
    default:
      return 'info'
  }
})

const stateAlertTitle = computed(() => {
  const state = (validationResult.value?.state || '').toUpperCase()
  const label = state === 'PASS' ? '校验通过' : state === 'WARN' ? '校验有风险' : state === 'FAIL' ? '校验未通过' : '校验结果'
  return `${label}：${totalUnits.value} / ${unitLimit.value} 学分`
})

interface StatusMeta {
  label: string
  tagType: 'success' | 'warning' | 'danger' | 'info'
}

/** 契约 4.1：VALID 绿 / WARN 黄 / INVALID 红 */
function statusMeta(status?: string): StatusMeta {
  switch ((status || '').toUpperCase()) {
    case 'VALID':
      return { label: '通过', tagType: 'success' }
    case 'WARN':
      return { label: '警告', tagType: 'warning' }
    case 'INVALID':
      return { label: '不通过', tagType: 'danger' }
    default:
      return { label: '未校验', tagType: 'info' }
  }
}

/** 把校验结果回填到课程行，保留可解释说明 */
function applyValidation(result: PlanValidationResult): void {
  validationResult.value = result
  validationStale.value = false
  const byCourse = new Map<number, PlanItemValidation>()
  const validatedItems = result.items ?? []
  validatedItems.forEach((item) => byCourse.set(item.courseId, item))
  workItems.value = workItems.value.map((item) => {
    const validated = item.courseId != null ? byCourse.get(item.courseId) : undefined
    if (!validated) {
      return item
    }
    return {
      ...item,
      code: validated.code || item.code,
      title: validated.title || item.title,
      units: validated.units ?? item.units,
      reason: validated.reason || item.reason,
      validationStatus: validated.validationStatus,
      validationMessage: validated.validationMessage
    }
  })
}

/**
 * 校验：有未保存修改或无规划时用不落库的试算（POST /edu/plans/validate），
 * 计划已保存且无修改时用持久化校验（POST /edu/plans/{planId}/validate）。
 */
function handleValidate(): void {
  const targetTermId = termId.value
  if (targetTermId == null) {
    proxy.$modal.msgWarning('请先选择学期')
    return
  }
  if (!workItems.value.length) {
    proxy.$modal.msgWarning('请先添加课程')
    return
  }
  const courseIds = workItems.value.map((item) => item.courseId).filter((id): id is number => id != null)
  if (!courseIds.length) {
    proxy.$modal.msgWarning('请先添加课程')
    return
  }
  const savedPlanId = planId.value
  const useSavedValidate = savedPlanId != null && !validationStale.value
  validating.value = true
  validateError.value = ''
  const request = useSavedValidate && savedPlanId != null ? validateSavedPlan(savedPlanId) : validatePlan({ termId: targetTermId, courseIds })
  request
    .then((res) => {
      if (res.data) {
        applyValidation(res.data)
      }
    })
    .catch(() => {
      validateError.value = '校验失败，请稍后重试。'
    })
    .finally(() => {
      validating.value = false
    })
}

/* ------------------------------ 保存 / 提交 / 删除 ------------------------------ */

const saving = ref(false)
const submitting = ref(false)

async function handleSave(): Promise<void> {
  const term = termId.value
  if (term == null) {
    proxy.$modal.msgWarning('请先选择学期')
    return
  }
  if (!workItems.value.length) {
    proxy.$modal.msgWarning('请先添加课程')
    return
  }
  saving.value = true
  try {
    let targetPlanId = planId.value
    if (targetPlanId == null) {
      const created = await addPlan({ termId: term, title: defaultPlanTitle() })
      targetPlanId = created.data?.planId ?? undefined
      if (targetPlanId == null) {
        await loadPlans()
        targetPlanId = plans.value.find((item) => item.termId === term)?.planId
      }
    }
    if (targetPlanId == null) {
      proxy.$modal.msgError('保存失败：未能创建规划')
      return
    }
    await updatePlanItems(targetPlanId, buildItemPayload())
    proxy.$modal.msgSuccess('规划已保存，后端已重新校验并写入理由')
    planId.value = targetPlanId
    await loadPlans()
    await loadPlanDetail(targetPlanId)
  } catch {
    // 请求拦截器已提示错误，这里仅保证 loading 归位
  } finally {
    saving.value = false
  }
}

function handleSubmit(): void {
  const targetPlanId = planId.value
  if (targetPlanId == null) {
    proxy.$modal.msgWarning('请先保存规划')
    return
  }
  proxy.$modal
    .confirm('提交后将进入 SUBMITTED 状态（提交前后端会强制校验），是否继续？')
    .then(() => {
      submitting.value = true
      return submitPlan(targetPlanId)
    })
    .then(() => {
      proxy.$modal.msgSuccess('规划已提交')
      return loadPlans().then(() => loadPlanDetail(targetPlanId))
    })
    .catch(() => {})
    .finally(() => {
      submitting.value = false
    })
}

function handleDeletePlan(): void {
  const targetPlanId = planId.value
  if (targetPlanId == null) {
    return
  }
  proxy.$modal
    .confirm(`是否确认删除规划"${selectedPlan.value?.title || targetPlanId}"？`)
    .then(() => delPlan(targetPlanId))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      planId.value = undefined
      selectedPlan.value = null
      workItems.value = []
      validationResult.value = null
      return loadPlans()
    })
    .catch(() => {})
}

/* ------------------------------ 新建规划 ------------------------------ */

const createOpen = ref(false)
const createSaving = ref(false)
const createForm = ref<{ title: string }>({ title: '' })
const createRules = {
  title: [{ required: true, message: '规划标题不能为空', trigger: 'blur' }]
}

function defaultPlanTitle(): string {
  const term = terms.value.find((item) => item.termId === termId.value)
  const name = term?.termName || term?.termCode || ''
  return name ? `${name} 选课计划` : '选课计划'
}

function openCreatePlan(): void {
  if (termId.value == null) {
    proxy.$modal.msgWarning('请先选择学期')
    return
  }
  createForm.value = { title: defaultPlanTitle() }
  createOpen.value = true
}

function submitCreatePlan(): void {
  const term = termId.value
  if (term == null) {
    return
  }
  proxy.$refs['createFormRef'].validate((valid: boolean) => {
    if (!valid) {
      return
    }
    createSaving.value = true
    addPlan({ termId: term, title: createForm.value.title.trim() })
      .then(async (res) => {
        proxy.$modal.msgSuccess('规划已创建（草稿）')
        createOpen.value = false
        const createdId = res.data?.planId
        await loadPlans()
        const targetPlanId = createdId ?? plans.value.find((item) => item.termId === term)?.planId
        if (targetPlanId != null) {
          planId.value = targetPlanId
          await loadPlanDetail(targetPlanId)
        }
      })
      .catch(() => {})
      .finally(() => {
        createSaving.value = false
      })
  })
}

/* ------------------------------ 课程检索 ------------------------------ */

const searchKeyword = ref('')
const searchResults = ref<EduCourse[]>([])
const searchLoading = ref(false)
const searchError = ref('')
const searchDone = ref(false)

function handleSearchCourse(): void {
  searchLoading.value = true
  searchError.value = ''
  const keyword = searchKeyword.value.trim()
  const query: CourseListQuery = {
    pageNum: 1,
    pageSize: 20,
    code: keyword || undefined,
    title: keyword || undefined
  }
  listCourse(query)
    .then((res) => {
      searchResults.value = res.rows ?? []
      searchDone.value = true
    })
    .catch(() => {
      searchResults.value = []
      searchError.value = '课程检索失败，请稍后重试。'
    })
    .finally(() => {
      searchLoading.value = false
    })
}

/* ------------------------------ 规则推荐 ------------------------------ */

const recommendations = ref<RecommendedCourse[]>([])
const recommendLoading = ref(false)
const recommendError = ref('')

function loadRecommendations(): void {
  if (termId.value == null) {
    recommendations.value = []
    return
  }
  recommendLoading.value = true
  recommendError.value = ''
  recommendPlan({ termId: termId.value, limit: 10 })
    .then((res) => {
      recommendations.value = res.data ?? []
    })
    .catch(() => {
      recommendations.value = []
      recommendError.value = '规则推荐加载失败（需要规划查看权限）。'
    })
    .finally(() => {
      recommendLoading.value = false
    })
}

onMounted(async () => {
  await loadTerms()
  await loadPlans()
  loadRecommendations()
})
</script>

<style scoped lang="scss">
.mb16 {
  margin-bottom: 16px;
}

.mt8 {
  margin-top: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  .toolbar-label {
    font-size: 13px;
    color: #606266;
  }
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

.panel-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.stale-tip {
  font-size: 12px;
  color: #e6a23c;
  font-weight: 400;
}

.units-box {
  margin-bottom: 14px;

  .units-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
  }

  .units-title {
    font-size: 13px;
    color: #606266;
  }

  .units-value {
    font-size: 13px;
    font-weight: 600;
    color: #303133;

    &.units-over {
      color: #f56c6c;
    }
  }

  .units-hint {
    margin-top: 6px;
    font-size: 12px;
    color: #909399;
  }
}

.state-messages {
  font-size: 12px;
  line-height: 1.7;
}

.reason-text {
  color: #303133;
  font-size: 13px;
  line-height: 1.6;
}

.reason-message {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1.6;
}

.search-row {
  display: flex;
  gap: 8px;
}

.result-scroll {
  margin-top: 8px;
}

.result-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;

  &:last-child {
    border-bottom: none;
  }

  .result-main {
    min-width: 0;
  }

  .result-title {
    display: flex;
    gap: 8px;
    align-items: baseline;

    .code {
      font-family: Consolas, 'SFMono-Regular', monospace;
      font-size: 12px;
      color: #409eff;
    }

    .title {
      font-size: 13px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .result-meta {
    font-size: 12px;
    color: #909399;
  }
}

.recommend-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 10px;
  background: #fafcff;

  .recommend-head {
    display: flex;
    align-items: baseline;
    gap: 8px;
    flex-wrap: wrap;

    .code {
      font-family: Consolas, 'SFMono-Regular', monospace;
      font-size: 12px;
      color: #409eff;
    }

    .title {
      font-size: 13px;
      font-weight: 600;
      color: #303133;
    }

    .units {
      font-size: 12px;
      color: #909399;
    }
  }

  .recommend-reason {
    margin-top: 6px;
    font-size: 12px;
    color: #606266;
    line-height: 1.6;
  }

  .recommend-missing {
    margin-top: 6px;
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;

    .missing-label {
      font-size: 12px;
      color: #f56c6c;
    }
  }

  .recommend-actions {
    margin-top: 8px;
    text-align: right;
  }
}

.recommend-loading {
  height: 120px;
}
</style>
