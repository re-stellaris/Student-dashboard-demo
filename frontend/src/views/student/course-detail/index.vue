<template>
  <div class="sp-root">
    <!-- 原页面 .topnav：logo + 返回 + 语言切换 + 用户胶囊（Sign out） -->
    <nav class="topnav">
      <span class="logo">📖 {{ t('pageTitle') }}</span>
      <router-link to="/student">{{ t('back') }}</router-link>
      <div class="nav-spacer"></div>
      <div class="lang-toggle">
        <button
          v-for="item in langOptions"
          :key="item.value"
          class="lang-btn"
          :class="{ active: lang === item.value }"
          type="button"
          :aria-pressed="lang === item.value"
          @click="applyLang(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <div v-if="currentUserName" class="user-pill">
        <span>{{ currentUserName }}</span>
        <button class="ub" type="button" @click="handleSignOut">{{ t('signOut') }}</button>
      </div>
    </nav>

    <div class="container">
      <div class="card">
        <!-- 载入中 -->
        <div v-if="loading" class="not-found">
          <div class="icon">...</div>
          <h2>{{ t('loading') }}</h2>
        </div>

        <!-- 接口错误 -->
        <div v-else-if="errorMessage" class="not-found">
          <div class="icon">!</div>
          <h2>{{ t('loadError') }}</h2>
          <p class="state-note">{{ errorMessage }}</p>
          <button class="back-link link-btn" type="button" @click="loadCourse">{{ t('retry') }}</button>
        </div>

        <!-- 课程不存在 -->
        <div v-else-if="!course" class="not-found">
          <div class="icon">!</div>
          <h2>{{ t('notFound') }}</h2>
          <p class="state-note">{{ t('missing') }}</p>
          <router-link class="back-link" to="/student">{{ t('viewCourseList') }}</router-link>
        </div>

        <!-- 课程详情 -->
        <template v-else>
          <div class="course-header">
            <div class="course-code">{{ courseCode }}</div>
            <div class="course-title">{{ courseTitleText }}</div>
            <div class="meta-row">
              <span v-for="tag in metaTags" :key="tag.label" class="meta-tag">
                <strong>{{ tag.label }}</strong> {{ tag.value }}
              </span>
            </div>
          </div>

          <div class="section">
            <h2>{{ t('info') }}</h2>
            <div class="detail-grid">
              <div v-for="cell in infoCells" :key="cell.label">
                <span>{{ cell.label }}</span>
                <strong>{{ cell.value }}</strong>
              </div>
            </div>
          </div>

          <div v-if="descriptionText" class="section">
            <h2>{{ t('description') }}</h2>
            <div class="text-block">{{ descriptionText }}</div>
          </div>

          <!-- 先修要求：保留原 SIS 原文，并按 groupNo 分组展示（组内 AND、组间 OR） -->
          <div v-if="prereqRawText || prereqGroups.length" class="section">
            <h2>{{ t('prerequisites') }}</h2>
            <div v-if="prereqRawText" class="text-block">{{ prereqRawText }}</div>
            <template v-if="prereqGroups.length">
              <div class="prereq-hint">{{ t('prereqHint') }}</div>
              <div v-for="(group, index) in prereqGroups" :key="group.groupNo">
                <div v-if="index > 0" class="prereq-or">{{ t('orLabel') }}</div>
                <div class="prereq-group">
                  <div class="prereq-group-head">{{ groupLabel(index) }} · {{ t('groupAnd') }}</div>
                  <span
                    v-for="row in group.items"
                    :key="prereqRowKey(row)"
                    class="prereq-pill"
                    :class="{ missed: row.satisfied === false }"
                  >
                    <span class="pc">{{ prereqCodeOf(row) }}</span>
                    <span>{{ prereqTitleOf(row) }}</span>
                  </span>
                </div>
              </div>
            </template>

            <!-- 学生本人先修自检（管理员/未绑定学籍时静默隐藏） -->
            <div
              v-if="prereqCheck"
              class="prereq-check"
              :class="prereqCheck.satisfied ? 'ok' : 'warn'"
            >
              <span class="prereq-check-icon">{{ prereqCheck.satisfied ? '✓' : '!' }}</span>
              <span>
                <strong>{{ t('prereqCheckTitle') }}</strong>
                <span class="prereq-check-reason">{{ prereqCheckReason }}</span>
              </span>
            </div>
          </div>

          <div v-if="coreqRawText" class="section">
            <h2>{{ t('coReq') }}</h2>
            <div class="text-block">{{ coreqRawText }}</div>
          </div>

          <div v-if="componentText" class="section">
            <h2>{{ t('components') }}</h2>
            <div class="text-block">{{ componentText }}</div>
          </div>

          <!-- 开课学期时间轴（与原页面同算法：2014 起，按 Term 1 / Term 2 / Summer 铺开） -->
          <div v-if="termNodes.length" class="section">
            <h2>{{ t('terms') }}</h2>
            <div class="term-layout">
              <div>
                <div class="term-timeline" :aria-label="t('timeline')">
                  <div class="term-track">
                    <div
                      v-for="node in termNodes"
                      :key="node.key"
                      class="term-node"
                      :class="{ on: node.on }"
                    >
                      <span class="year">{{ node.year }}</span>
                      <span class="term">{{ termLabel(node.term) }}</span>
                    </div>
                  </div>
                </div>
                <div class="muted-note">{{ t('note') }}</div>
              </div>
              <div class="term-list">
                <div class="term-list-title">{{ t('offeredList') }}</div>
                <div v-for="term in offeredTerms" :key="term.key + term.text" class="term-list-item">
                  {{ term.text }}
                </div>
              </div>
            </div>
          </div>

          <!-- 课程能力匹配 -->
          <div v-if="competencyGroups.length" class="section">
            <h2>{{ t('courseCompetencies') }}</h2>
            <div class="course-competency-card">
              <div class="course-competency-intro">
                <h3>{{ t('courseCompetencies') }}</h3>
                <p>{{ t('competencyIntro') }}</p>
              </div>
              <div
                v-for="group in competencyGroups"
                :key="group.key"
                class="competency-relevance-block"
              >
                <div class="competency-relevance-title">{{ t(group.titleKey) }}</div>
                <div
                  v-for="row in group.rows"
                  :key="competencyRowKey(row)"
                  class="course-competency-row"
                  :title="t(row.titleKey)"
                >
                  <span class="course-competency-name">{{ localizeCompetencyText(skillNameOf(row)) }}</span>
                  <span class="course-competency-rationale">{{ localizeRationale(row.rationale) }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="card-footer">
            <router-link class="back-link" to="/student">{{ t('back') }}</router-link>
          </div>
        </template>
      </div>

      <!-- 讨论区：沿用原页面 localStorage（不新增后端接口） -->
      <div class="comments-card">
        <h2>💬 {{ t('discussion') }}</h2>
        <div v-if="!comments.length" class="comments-empty">{{ t('commentsEmpty') }}</div>
        <div v-for="(comment, index) in comments" :key="index" class="comment-item">
          <div class="comment-avatar">{{ initialsOf(comment.name) }}</div>
          <div class="comment-body">
            <div class="comment-meta">
              <span class="comment-author">{{ comment.name }}</span>
              <span class="comment-time">{{ comment.time }}</span>
            </div>
            <div class="comment-text">{{ comment.text }}</div>
          </div>
          <button
            v-if="currentUserName && comment.name === currentUserName"
            class="comment-del"
            type="button"
            :title="t('deleteComment')"
            @click="removeComment(index)"
          >
            ✕
          </button>
        </div>
        <div class="comment-form">
          <input
            v-model="commentInput"
            type="text"
            :placeholder="t('commentPlaceholder')"
            maxlength="500"
            @keydown.enter="addComment"
          />
          <button class="btn-send" type="button" @click="addComment">{{ t('send') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="StudentCourseDetail">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { checkPrereq, getCourseByCode, listPrereq } from '@/api/education/course'
import { getCourseCompetency } from '@/api/education/analytics'
import { getMyStudent } from '@/api/education/enrollment'
import useUserStore from '@/store/modules/user'
import type { CourseCompetency, EduCourse, EduCoursePrereq, PrereqCheckResult } from '@/types/education'

/* =====================================================================
 * 类型
 * ===================================================================== */

type Lang = 'en' | 'zh-CN' | 'zh-TW'
type Dict = Record<string, string>
type TitleKey = 'titleEn' | 'titleZh'
type CompetencyLevel = 'High' | 'Moderate' | 'Low'

/** 先修关系：后端联表字段为 prereqCode / prereqTitle，并带当前学生的 satisfied 标记 */
interface PrereqRow extends EduCoursePrereq {
  prereqCode?: string
  prereqTitle?: string
  satisfied?: boolean
}

/** 课程能力关联：后端返回 skillName / skillCategory */
interface CompetencyRow extends CourseCompetency {
  skillName?: string
  skillCategory?: string
  matchedKeywords?: string
}

/** 分组后的能力行（带上所属分组的标题 key） */
interface CompetencyItem extends CompetencyRow {
  titleKey: string
}

interface CompetencyGroup {
  key: CompetencyLevel
  titleKey: string
  rows: CompetencyItem[]
}

interface PrereqGroup {
  groupNo: number
  items: PrereqRow[]
}

interface MetaTag {
  label: string
  value: string
}

interface InfoCell {
  label: string
  value: string
}

interface OfferedTerm {
  text: string
  year?: string
  term?: string
  key: string
}

interface TermNode {
  year: string
  term: string
  key: string
  on: boolean
}

/** 讨论区留言（只存 localStorage，键名沿用原型） */
interface CourseComment {
  name: string
  text: string
  time: string
}

/* =====================================================================
 * i18n（字典取自 legacy/course_detail.html）
 * ===================================================================== */

const LANG_KEY = 'cuhksz_lang'

const I18N: Record<Lang, Dict> = {
  en: {
    pageTitle: 'Course Detail',
    back: 'Back to Course Browser',
    loading: 'Loading course...',
    notFound: 'Course Not Found',
    missing: 'No course matched this code.',
    loadError: 'Could not load this course from the API.',
    retry: 'Try again',
    subject: 'Subject',
    units: 'Units',
    level: 'Level',
    outline: 'Outline',
    outlineOk: 'SIS outline available',
    info: 'Course Information',
    academicOrg: 'Academic Org',
    gradeType: 'Grade Type',
    description: 'Description',
    prerequisites: 'Prerequisites',
    coReq: 'Co-requisites',
    components: 'Course Components',
    terms: 'Offered Terms',
    timeline: 'Timeline',
    offeredList: 'All Offered Terms',
    note: 'Highlighted nodes show terms when this course was offered.',
    term1: 'Term 1',
    term2: 'Term 2',
    summer: 'Summer',
    courseCompetencies: 'Course competencies',
    competencyIntro: 'All positive competency matches for this course.',
    highRel: 'High relevance',
    moderateRel: 'Moderate relevance',
    lowRel: 'Low relevance',
    prereqHint: 'Courses in the same group are all required (AND); any single group satisfies the requirement (OR).',
    groupAnd: 'all required',
    orLabel: 'OR',
    missingPrereq: 'Missing prerequisites',
    prereqCheckTitle: 'Do you meet the prerequisites?',
    viewCourseList: 'Back to course list',
    discussion: 'Discussion',
    commentsEmpty: 'No comments yet. Be the first to share your thoughts!',
    commentPlaceholder: 'Share your thoughts about this course...',
    send: 'Send',
    deleteComment: 'Delete my comment',
    deleteConfirm: 'Delete this comment? This action cannot be undone.',
    signOut: 'Sign out',
    signOutConfirm: 'Sign out? Your portfolio is preserved on this device.'
  },
  'zh-CN': {
    pageTitle: '课程详细信息',
    back: '返回 浏览课程目录',
    loading: '正在载入课程...',
    notFound: '未找到课程',
    missing: '没有匹配该代码的课程。',
    loadError: '无法从 API 载入课程。',
    retry: '重新载入',
    subject: '科目',
    units: '学分',
    level: '级别',
    outline: '大纲',
    outlineOk: '已有 SIS 课程大纲',
    info: '课程信息',
    academicOrg: 'Academic Org',
    gradeType: '成绩类型',
    description: '课程描述',
    prerequisites: '先修要求',
    coReq: '并修要求',
    components: '课程组件',
    terms: '开课学期',
    timeline: '时间轴',
    offeredList: '全部开课记录',
    note: '高亮节点表示该课程曾在对应学期开设。',
    term1: 'Term 1',
    term2: 'Term 2',
    summer: 'Summer',
    courseCompetencies: '课程能力匹配',
    competencyIntro: '本课程所有正向能力匹配。',
    highRel: '高相关',
    moderateRel: '中等相关',
    lowRel: '低相关',
    prereqHint: '同一组内的课程需全部满足（AND）；任一组满足即可（OR）。',
    groupAnd: '组内全部满足',
    orLabel: '或（OR）',
    missingPrereq: '缺少先修',
    prereqCheckTitle: '你是否满足先修要求？',
    viewCourseList: '返回课程列表',
    discussion: '讨论区',
    commentsEmpty: '还没有留言，来写下第一条想法吧！',
    commentPlaceholder: '写下你对这门课的想法...',
    send: '发送',
    deleteComment: '删除我的留言',
    deleteConfirm: '删除这条留言？该操作不可撤销。',
    signOut: '退出登录',
    signOutConfirm: '确认退出登录？本机的作品集数据会保留。'
  },
  'zh-TW': {
    pageTitle: '課程詳細資訊',
    back: '返回 瀏覽課程目錄',
    loading: '正在載入課程...',
    notFound: '未找到課程',
    missing: '沒有匹配該代碼的課程。',
    loadError: '無法從 API 載入課程。',
    retry: '重新載入',
    subject: '科目',
    units: '學分',
    level: '級別',
    outline: '大綱',
    outlineOk: '已有 SIS 課程大綱',
    info: '課程資訊',
    academicOrg: 'Academic Org',
    gradeType: '成績類型',
    description: '課程描述',
    prerequisites: '先修要求',
    coReq: '並修要求',
    components: '課程組件',
    terms: '開課學期',
    timeline: '時間軸',
    offeredList: '全部開課記錄',
    note: '高亮節點表示該課程曾在對應學期開設。',
    term1: 'Term 1',
    term2: 'Term 2',
    summer: 'Summer',
    courseCompetencies: '課程能力匹配',
    competencyIntro: '本課程所有正向能力匹配。',
    highRel: '高相關',
    moderateRel: '中等相關',
    lowRel: '低相關',
    prereqHint: '同一組內的課程需全部滿足（AND）；任一組滿足即可（OR）。',
    groupAnd: '組內全部滿足',
    orLabel: '或（OR）',
    missingPrereq: '缺少先修',
    prereqCheckTitle: '你是否滿足先修要求？',
    viewCourseList: '返回課程列表',
    discussion: '討論區',
    commentsEmpty: '還沒有留言，來寫下第一條想法吧！',
    commentPlaceholder: '寫下你對這門課的想法...',
    send: '送出',
    deleteComment: '刪除我的留言',
    deleteConfirm: '刪除這條留言？該操作不可撤銷。',
    signOut: '登出',
    signOutConfirm: '確認登出？本機的作品集資料會保留。'
  }
}

const langOptions: Array<{ value: Lang; label: string }> = [
  { value: 'en', label: 'EN' },
  { value: 'zh-CN', label: '简中' },
  { value: 'zh-TW', label: '繁中' }
]

function readLang(): Lang {
  try {
    const stored = localStorage.getItem(LANG_KEY)
    if (stored === 'en' || stored === 'zh-CN' || stored === 'zh-TW') return stored
  } catch (error) {
    // localStorage 不可用（隐私模式）时退回英文
  }
  return 'en'
}

const lang = ref<Lang>(readLang())

function t(key: string): string {
  return I18N[lang.value][key] ?? I18N.en[key] ?? key
}

/** 先修分组标题：EN `Group N`，简中 `第 N 组`，繁中 `第 N 組` */
function groupLabel(index: number): string {
  const order = String(index + 1)
  if (lang.value === 'zh-CN') return `第 ${order} 组`
  if (lang.value === 'zh-TW') return `第 ${order} 組`
  return `Group ${order}`
}

function applyLang(value: Lang): void {
  lang.value = value
  try {
    localStorage.setItem(LANG_KEY, value)
  } catch (error) {
    // 忽略写入失败，语言切换仍然生效
  }
}

/* =====================================================================
 * 页面状态
 * ===================================================================== */

const route = useRoute()
const userStore = useUserStore()

const course = ref<EduCourse | null>(null)
const prereqRows = ref<PrereqRow[]>([])
const prereqCheck = ref<PrereqCheckResult | null>(null)
const competencies = ref<CompetencyRow[]>([])
const studentName = ref('')
const loading = ref(true)
const errorMessage = ref('')

const codeParam = computed<string>(() => {
  const raw = route.params.code
  const value = Array.isArray(raw) ? raw[0] : raw
  return String(value ?? '').trim()
})

const storageKey = computed<string>(() => 'course_comments_' + codeParam.value.replace(/\s+/g, '_'))

/* =====================================================================
 * 课程展示字段
 * ===================================================================== */

function compactCode(value: string | undefined): string {
  return String(value ?? '').replace(/\s+/g, '').toUpperCase()
}

/** 与原页面 titleCandidate 一致：跳过与课程代码相同的候选值 */
function titleCandidate(target: EduCourse, keys: TitleKey[]): string {
  const code = compactCode(target.code)
  for (const key of keys) {
    const value = String(target[key] ?? '').trim()
    if (value && compactCode(value) !== code) return value
  }
  return ''
}

function courseTitleOf(target: EduCourse): string {
  if (lang.value === 'zh-CN' || lang.value === 'zh-TW') {
    return titleCandidate(target, ['titleZh', 'titleEn']) || target.code || ''
  }
  return titleCandidate(target, ['titleEn', 'titleZh']) || target.code || ''
}

/** 原页面 meta 里的 Subject 是短学科码（CSC / GEB），新库用 code 前缀还原同一观感 */
function subjectShort(target: EduCourse): string {
  const fromCode = String(target.code ?? '').replace(/[^A-Za-z]/g, '').toUpperCase()
  if (fromCode) return fromCode
  const subject = String(target.subject ?? '').trim()
  return subject.split(/\s+/)[0] ?? ''
}

function valueOrDash(value: string | number | undefined | null): string {
  if (value === null || value === undefined) return '-'
  const text = String(value).trim()
  return text === '' ? '-' : text
}

const courseCode = computed<string>(() => course.value?.code ?? '')

const courseTitleText = computed<string>(() => (course.value ? courseTitleOf(course.value) : ''))

const hasOutline = computed<boolean>(() => course.value?.outlineStatus === 'saved_outline')

const metaTags = computed<MetaTag[]>(() => {
  const target = course.value
  if (!target) return []
  const tags: MetaTag[] = []
  const push = (labelKey: string, value: string): void => {
    if (!value) return
    tags.push({ label: t(labelKey), value })
  }
  push('subject', subjectShort(target))
  push('units', target.credits === null || target.credits === undefined ? '' : String(target.credits))
  push('level', target.level === null || target.level === undefined ? '' : String(target.level))
  if (hasOutline.value) push('outline', t('outlineOk'))
  return tags
})

const infoCells = computed<InfoCell[]>(() => {
  const target = course.value
  if (!target) return []
  return [
    { label: t('academicOrg'), value: valueOrDash(target.academicOrg || target.school) },
    { label: t('gradeType'), value: valueOrDash(target.gradingBasis) }
  ]
})

const descriptionText = computed<string>(() => {
  const target = course.value
  if (!target) return ''
  const english = target.descriptionEn ?? ''
  const chinese = target.descriptionZh ?? ''
  const text = lang.value === 'en' ? english || chinese : chinese || english
  return String(text).trim()
})

const prereqRawText = computed<string>(() => String(course.value?.prereqText ?? '').trim())
const coreqRawText = computed<string>(() => String(course.value?.coreqText ?? '').trim())
const componentText = computed<string>(() => String(course.value?.component ?? '').trim())

/* =====================================================================
 * 先修关系
 * ===================================================================== */

function prereqCodeOf(row: PrereqRow): string {
  return row.prereqCode || row.prereqCourseCode || row.code || ''
}

function prereqTitleOf(row: PrereqRow): string {
  return row.prereqTitle || row.prereqCourseTitle || row.title || ''
}

function prereqRowKey(row: PrereqRow): string {
  return String(row.id ?? row.prereqCourseId ?? prereqCodeOf(row))
}

const prereqGroups = computed<PrereqGroup[]>(() => {
  const groups = new Map<number, PrereqRow[]>()
  for (const row of prereqRows.value) {
    const groupNo = typeof row.groupNo === 'number' ? row.groupNo : 1
    const items = groups.get(groupNo)
    if (items) items.push(row)
    else groups.set(groupNo, [row])
  }
  return [...groups.entries()]
    .sort((left, right) => left[0] - right[0])
    .map(([groupNo, items]) => ({
      groupNo,
      items: items.slice().sort((left, right) => prereqCodeOf(left).localeCompare(prereqCodeOf(right)))
    }))
})

const prereqCheckReason = computed<string>(() => {
  const result = prereqCheck.value
  if (!result) return ''
  const reason = String(result.reason ?? '').trim()
  if (result.satisfied) return reason
  const missing = (result.missing ?? [])
    .map((item) => [item.code, item.title].filter(Boolean).join(' '))
    .filter(Boolean)
  if (!missing.length) return reason
  return [reason, `${t('missingPrereq')}: ${missing.join(', ')}`].filter(Boolean).join(' · ')
})

/* =====================================================================
 * 开课学期
 * ===================================================================== */

const TERM_PATTERN = /(\d{4})-(\d{2})\s+(Term\s+1|Term\s+2|Summer)/i
const TERM_NAMES = ['Term 1', 'Term 2', 'Summer']

function parseOfferedTerms(value: string | undefined): OfferedTerm[] {
  return String(value ?? '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((text) => {
      const matched = text.match(TERM_PATTERN)
      if (!matched) return { text, key: text }
      const year = `${matched[1]}-${matched[2]}`
      const term = matched[3].replace(/\s+/g, ' ')
      return { text, year, term, key: `${year} ${term}` }
    })
}

const offeredTerms = computed<OfferedTerm[]>(() => parseOfferedTerms(course.value?.offeredTerms))

const termNodes = computed<TermNode[]>(() => {
  const offered = offeredTerms.value
  const years = offered.map((item) => item.year).filter((year): year is string => Boolean(year))
  let lastStart = 2025
  years.forEach((year) => {
    const value = Number(year.slice(0, 4))
    if (Number.isFinite(value) && value > lastStart) lastStart = value
  })
  const offeredSet = new Set(offered.map((item) => item.key))
  const nodes: TermNode[] = []
  for (let start = 2014; start <= lastStart; start += 1) {
    const year = `${start}-${String((start + 1) % 100).padStart(2, '0')}`
    for (const term of TERM_NAMES) {
      const key = `${year} ${term}`
      nodes.push({ year, term, key, on: offeredSet.has(key) })
    }
  }
  return nodes
})

function termLabel(term: string): string {
  if (term === 'Term 1') return t('term1')
  if (term === 'Term 2') return t('term2')
  return t('summer')
}

/* =====================================================================
 * 课程能力匹配
 * ===================================================================== */

const COMPETENCY_ZH_PHRASES: ReadonlyArray<readonly [string, string]> = [
  ['AI ethics, governance and regulation', 'AI伦理、治理与监管'],
  ['AI programming and software implementation', 'AI编程与软件实现'],
  ['AI systems, algorithms and data-driven problem solving', 'AI系统、算法与数据驱动问题解决'],
  ['Embodied, robotic and domain AI applications', '具身智能、机器人与领域AI应用'],
  ['Machine learning, deep learning and large language models', '机器学习、深度学习与大语言模型'],
  ['Mathematical and statistical foundations for AI', 'AI的数学与统计基础'],
  ['Project-based AI exploration, practice and capstone execution', '项目式AI探索、实践与毕业项目执行'],
  ['Big data analytics and data-driven modelling', '大数据分析与数据驱动建模'],
  ['Data structures, databases and computing infrastructure', '数据结构、数据库与计算基础设施'],
  ['Domain applications in business, finance, life science and engineering', '商业、金融、生命科学与工程领域应用'],
  ['Machine learning, AI and data mining', '机器学习、AI与数据挖掘'],
  ['Programming, algorithms, software and computing systems', '编程、算法、软件与计算系统'],
  ['Quantitative modelling and mathematical foundations', '定量建模与数学基础'],
  ['Research, capstone and project execution', '研究、毕业项目与项目执行'],
  ['Robotics, control and embodied intelligent systems', '机器人、控制与具身智能系统'],
  ['Statistical reasoning, probability and inference', '统计推理、概率与推断'],
  ['course outline matches', '课程大纲匹配'],
  ['prefix aligns with this competency area', '课程前缀与该能力领域相关'],
  ['Ability to', '能够'],
  ['understand and apply', '理解并应用'],
  ['understand', '理解'],
  ['apply', '应用'],
  ['analyze', '分析'],
  ['reason about', '推理'],
  ['use', '使用'],
  ['complete', '完成'],
  ['carry out', '开展'],
  ['implement', '实现'],
  ['design', '设计'],
  ['large-scale data', '大规模数据'],
  ['data-driven models', '数据驱动模型'],
  ['decisions', '决策'],
  ['programming', '编程'],
  ['software', '软件'],
  ['algorithms', '算法'],
  ['algorithm', '算法'],
  ['data structures', '数据结构'],
  ['databases', '数据库'],
  ['database', '数据库'],
  ['infrastructure', '基础设施'],
  ['mathematics', '数学'],
  ['statistics', '统计'],
  ['statistical', '统计'],
  ['probability', '概率'],
  ['inference', '推断'],
  ['optimization', '优化'],
  ['optimisation', '优化'],
  ['modelling', '建模'],
  ['modeling', '建模'],
  ['model', '模型'],
  ['data mining', '数据挖掘'],
  ['visualization', '可视化'],
  ['analysis', '分析'],
  ['data', '数据'],
  ['research', '研究'],
  ['project', '项目'],
  ['capstone', '毕业项目'],
  ['team', '团队'],
  ['practice', '实践'],
  ['presentation', '展示'],
  ['business', '商业'],
  ['finance', '金融'],
  ['life science', '生命科学'],
  ['engineering', '工程'],
  ['robotics', '机器人'],
  ['robot', '机器人'],
  ['control', '控制'],
  ['sensors', '传感器'],
  ['sensor', '传感器']
]

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function localizeCompetencyText(text: string | undefined): string {
  let output = String(text ?? '')
  if (lang.value === 'en') return output
  COMPETENCY_ZH_PHRASES.forEach(([english, chinese]) => {
    output = output.replace(new RegExp(escapeRegExp(english), 'gi'), chinese)
  })
  return output
}

function localizeRationale(text: string | undefined): string {
  return localizeCompetencyText(String(text ?? '').replace(/\s*\(\+\d+(?:\.\d+)?\)/g, ''))
}

function skillNameOf(row: CompetencyRow): string {
  return row.skillName || row.name || row.skillCode || ''
}

function competencyRowKey(row: CompetencyRow): string {
  return String(row.id ?? row.skillTagId ?? skillNameOf(row))
}

const competencyGroups = computed<CompetencyGroup[]>(() => {
  const rows = competencies.value.filter((row) => Number(row.score ?? 0) > 0)
  if (!rows.length) return []
  const groups: CompetencyGroup[] = [
    { key: 'High', titleKey: 'highRel', rows: [] },
    { key: 'Moderate', titleKey: 'moderateRel', rows: [] },
    { key: 'Low', titleKey: 'lowRel', rows: [] }
  ]
  rows
    .slice()
    .sort((left, right) => Number(right.score ?? 0) - Number(left.score ?? 0))
    .forEach((row) => {
      const score = Number(row.score ?? 0)
      const rawLevel = String(row.level ?? '')
      const level: CompetencyLevel =
        rawLevel === 'High' || rawLevel === 'Moderate' || rawLevel === 'Low'
          ? rawLevel
          : score >= 6.5
            ? 'High'
            : score >= 4
              ? 'Moderate'
              : 'Low'
      const group = groups.find((item) => item.key === level) ?? groups[2]
      group.rows.push({ ...row, titleKey: group.titleKey })
    })
  return groups.filter((group) => group.rows.length > 0)
})

/* =====================================================================
 * 当前用户
 * ===================================================================== */

const isStudent = computed<boolean>(() => userStore.roles.includes('STUDENT'))

const currentUserName = computed<string>(() => {
  const stored = studentName.value.trim()
  if (stored) return stored
  const nickname = String(userStore.nickName ?? '').trim()
  if (nickname) return nickname
  return String(userStore.name ?? '').trim()
})

async function loadStudentName(): Promise<void> {
  if (!isStudent.value) return
  try {
    const result = await getMyStudent()
    studentName.value = String(result.data?.name ?? '').trim()
  } catch (error) {
    studentName.value = ''
  }
}

function handleSignOut(): void {
  if (!window.confirm(t('signOutConfirm'))) return
  userStore
    .logOut()
    .catch(() => undefined)
    .then(() => {
      window.location.href = '/index'
    })
}

/* =====================================================================
 * 数据加载
 * ===================================================================== */

function errorText(error: unknown): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'string') return error
  return t('loadError')
}

async function loadPrereqs(courseId: number): Promise<void> {
  prereqRows.value = []
  try {
    const result = await listPrereq(courseId)
    prereqRows.value = Array.isArray(result.data) ? result.data : []
  } catch (error) {
    prereqRows.value = []
  }
}

async function loadPrereqCheckResult(courseId: number): Promise<void> {
  prereqCheck.value = null
  // 只有学生才有「已修课程」可比对；管理员/教师后端返回「未绑定学籍」，直接静默隐藏
  if (!isStudent.value) return
  try {
    const result = await checkPrereq(courseId)
    prereqCheck.value = result.data ?? null
  } catch (error) {
    prereqCheck.value = null
  }
}

async function loadCompetencies(courseId: number): Promise<void> {
  competencies.value = []
  try {
    const result = await getCourseCompetency(courseId)
    competencies.value = Array.isArray(result.data) ? result.data : []
  } catch (error) {
    competencies.value = []
  }
}

function updateDocumentTitle(): void {
  if (!course.value) {
    document.title = t('pageTitle')
    return
  }
  document.title = `${courseCode.value} - ${courseTitleText.value || t('pageTitle')}`
}

async function loadCourse(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  course.value = null
  prereqRows.value = []
  prereqCheck.value = null
  competencies.value = []

  const code = codeParam.value
  if (!code) {
    loading.value = false
    updateDocumentTitle()
    return
  }

  try {
    const result = await getCourseByCode(code)
    const detail = result?.data ?? null
    if (!detail) {
      course.value = null
      return
    }
    course.value = detail
    const courseId = detail.courseId
    if (typeof courseId === 'number') {
      void loadPrereqs(courseId)
      void loadPrereqCheckResult(courseId)
      void loadCompetencies(courseId)
    }
  } catch (error) {
    errorMessage.value = errorText(error)
  } finally {
    loading.value = false
    updateDocumentTitle()
  }
}

/* =====================================================================
 * 讨论区（localStorage，键名沿用原型 course_comments_<code>）
 * ===================================================================== */

const comments = ref<CourseComment[]>([])
const commentInput = ref('')

function loadComments(): void {
  try {
    const raw = localStorage.getItem(storageKey.value)
    if (!raw) {
      comments.value = []
      return
    }
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      comments.value = []
      return
    }
    const list = parsed as unknown[]
    comments.value = list
      .filter((item): item is Record<string, unknown> => typeof item === 'object' && item !== null)
      .map((item) => ({
        name: String(item.name ?? 'Anonymous'),
        text: String(item.text ?? ''),
        time: String(item.time ?? '')
      }))
  } catch (error) {
    comments.value = []
  }
}

function saveComments(): void {
  try {
    localStorage.setItem(storageKey.value, JSON.stringify(comments.value))
  } catch (error) {
    // 存储不可用时保持页面内数据，不打断用户
  }
}

function initialsOf(name: string): string {
  return name
    .split(/\s+/)
    .map((part) => part.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

function formatNow(): string {
  const now = new Date()
  const pad = (value: number): string => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`
}

function addComment(): void {
  const text = commentInput.value.trim()
  if (!text) return
  comments.value.push({
    name: currentUserName.value || 'Anonymous',
    text,
    time: formatNow()
  })
  commentInput.value = ''
  saveComments()
}

function removeComment(index: number): void {
  const target = comments.value[index]
  if (!target) return
  if (!currentUserName.value || target.name !== currentUserName.value) return
  if (!window.confirm(t('deleteConfirm'))) return
  comments.value.splice(index, 1)
  saveComments()
}

/* =====================================================================
 * 生命周期
 * ===================================================================== */

onMounted(() => {
  document.documentElement.lang = lang.value
  loadComments()
  void loadCourse()
  void loadStudentName()
})

watch(codeParam, () => {
  loadComments()
  void loadCourse()
})

watch(lang, (value) => {
  document.documentElement.lang = value
  updateDocumentTitle()
})
</script>

<style scoped src="../../../assets/student/course-detail.css"></style>

<style scoped>
/* ---------------------------------------------------------------------
 * 仅补齐原型缺失/新增部分的样式：
 * 1) .lang-toggle 在原 course_detail.html 里没有（该页原无语言切换），
 *    这里沿用 ge / guide 页同一套视觉规则；
 * 2) 先修分组、先修自检、空态提示是新架构新增的内容块，
 *    全部复用原页面的 CSS 变量与圆角/边框语言；
 * 3) .link-btn 让按钮复用 .back-link 的观感。
 * ------------------------------------------------------------------- */
.lang-toggle{display:inline-flex;background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.18);border-radius:20px;overflow:hidden}
.lang-btn{padding:5px 12px;border:0;background:transparent;color:rgba(255,255,255,.72);font-size:11.5px;font-weight:800;cursor:pointer}
.lang-btn:hover{background:rgba(255,255,255,.1);color:#fff}
.lang-btn.active{background:rgba(255,255,255,.24);color:#fff}
.state-note{margin-top:8px;color:var(--text2)}
.link-btn{cursor:pointer;background:none;border:none;font-family:inherit}
.card-footer{margin-top:32px;text-align:center}
.prereq-hint{color:var(--text3);font-size:12px;margin-top:10px}
.prereq-group{border:1px solid var(--border);background:var(--bg);border-radius:10px;padding:10px 12px;margin-top:10px}
.prereq-group-head{font-size:11px;font-weight:900;text-transform:uppercase;letter-spacing:.06em;color:var(--text3);margin-bottom:8px}
.prereq-pill{display:inline-flex;align-items:center;gap:6px;background:#fff;border:1px solid var(--border);border-radius:999px;padding:4px 10px;margin:0 6px 6px 0;font-size:12px;font-weight:700;color:var(--text)}
.prereq-pill .pc{color:var(--accent);font-weight:900}
.prereq-pill.missed{border-color:#fcd9a4;background:#fffbeb}
.prereq-or{font-size:11px;font-weight:900;color:var(--text3);letter-spacing:.08em;margin:8px 0 0}
.prereq-check{display:flex;gap:10px;align-items:flex-start;border-radius:10px;padding:10px 12px;margin-top:12px;font-size:12.5px;font-weight:700;line-height:1.55}
.prereq-check .prereq-check-icon{font-weight:900}
.prereq-check .prereq-check-reason{margin-left:6px;font-weight:600}
.prereq-check.ok{background:#ecfdf5;border:1px solid #99f6e4;color:#0f766e}
.prereq-check.warn{background:#fffbeb;border:1px solid #fde68a;color:#b45309}
</style>
