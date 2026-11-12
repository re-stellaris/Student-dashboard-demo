<template>
  <div class="sp-root">
    <!-- 原页面 .topnav：logo + 返回 + 三区切换 + 语言切换 + 用户胶囊 -->
    <nav class="topnav">
      <span class="logo">Study Dashboard</span>
      <router-link to="/student">← {{ t('back') }}</router-link>
      <router-link class="nav-link" to="/student/transcript">📄 <span>成绩单上传</span></router-link>
      <div class="lang-toggle area-toggle" role="tablist" :aria-label="t('areaSwitch')">
        <button
          v-for="item in areaOptions"
          :key="item.key"
          class="lang-btn"
          :class="{ active: areaKey === item.key }"
          type="button"
          role="tab"
          :aria-selected="areaKey === item.key"
          :title="item.title"
          @click="goArea(item.key)"
        >
          {{ item.label }}
        </button>
      </div>
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

    <main class="wrap">
      <section class="hero">
        <h1>{{ areaMeta.title }}</h1>
        <p>{{ areaMeta.subtitle }}</p>
      </section>

      <div class="toolbar">
        <input v-model="keyword" type="search" :placeholder="t('search')" />
        <div class="count">{{ visibleCourses.length }} {{ t('countSuffix') }}</div>
      </div>

      <!-- 页内可读状态：载入中 / 接口错误 / 空 -->
      <div v-if="loading" class="empty">{{ t('loading') }}</div>
      <div v-else-if="errorMessage" class="empty">
        {{ errorMessage }}
        <button class="empty-retry" type="button" @click="loadCourses">{{ t('retry') }}</button>
      </div>
      <div v-else-if="!visibleCourses.length" class="empty">{{ t('noCourses') }}</div>

      <!-- 与原页面一致：按「已有大纲 / 暂无大纲」两段分组 -->
      <template v-else>
        <section
          v-for="group in availabilityGroups"
          :key="group.available ? 'available' : 'unavailable'"
          class="availability-section"
        >
          <div class="section-head">
            <span>{{ availabilityHeading(group.available, group.rows.length) }}</span>
          </div>
          <div class="grid">
            <article v-for="item in group.rows" :key="item.code" class="course">
              <router-link class="code" :to="courseLink(item)">{{ compact(item.code) }}</router-link>
              <div class="title">{{ titleOf(item) }}</div>
              <div class="meta">
                <span class="pill">{{ unitsOf(item) }} {{ t('units') }}</span>
                <span class="pill" :class="{ ok: hasOutline(item) }">
                  {{ availabilityLabel(hasOutline(item)) }}
                </span>
              </div>
            </article>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts" name="StudentGeArea">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listCourse } from '@/api/education/course'
import useUserStore from '@/store/modules/user'
import type { EduCourse } from '@/types/education'

/* =====================================================================
 * 类型
 * ===================================================================== */

type Lang = 'en' | 'zh-CN' | 'zh-TW'
type Dict = Record<string, string>
type AreaKey = 'b' | 'c' | 'd'
type AreaCode = 'GEB' | 'GEC' | 'GED'

interface AreaOption {
  key: AreaKey
  label: string
  title: string
}

interface CourseGroup {
  available: boolean
  rows: EduCourse[]
}

interface AreaMeta {
  code: AreaCode
  title: string
  subtitle: string
}

/* =====================================================================
 * i18n（字典取自 legacy/ge_area_b.html）
 * ===================================================================== */

const LANG_KEY = 'cuhksz_lang'

const I18N: Record<Lang, Dict> = {
  en: {
    back: 'Back',
    search: 'Search course code or title...',
    noCourses: 'No courses found.',
    available: 'Syllabus available',
    unavailable: 'No syllabus yet',
    units: 'units',
    countSuffix: 'courses',
    loading: 'Loading courses...',
    loadError: 'Failed to load courses.',
    retry: 'Reload',
    areaSwitch: 'Switch GE area',
    signOut: 'Sign out',
    signOutConfirm: 'Sign out? Your portfolio is preserved on this device.'
  },
  'zh-CN': {
    back: '返回',
    search: '搜索课程代码或标题...',
    noCourses: '未找到课程。',
    available: '可查看 syllabus',
    unavailable: '暂无 syllabus',
    units: '学分',
    countSuffix: '门课程',
    loading: '正在载入课程...',
    loadError: '无法载入课程列表。',
    retry: '重新载入',
    areaSwitch: '切换 GE 范围',
    signOut: '退出登录',
    signOutConfirm: '确认退出登录？本机的作品集数据会保留。'
  },
  'zh-TW': {
    back: '返回',
    search: '搜尋課程代碼或標題...',
    noCourses: '未找到課程。',
    available: '可查看 syllabus',
    unavailable: '暫無 syllabus',
    units: '學分',
    countSuffix: '門課程',
    loading: '正在載入課程...',
    loadError: '無法載入課程列表。',
    retry: '重新載入',
    areaSwitch: '切換 GE 範圍',
    signOut: '登出',
    signOutConfirm: '確認登出？本機的作品集資料會保留。'
  }
}

/** 三区标题 / 副标题，逐字取自原型 AREA_META */
const AREA_TEXTS: Record<Lang, Record<AreaCode, [string, string]>> = {
  en: {
    GEB: [
      'GE Area B Courses',
      'Nature, Science and Technology. Choose any eligible course in this area to satisfy the GE Area B requirement.'
    ],
    GEC: [
      'GE Area C Courses',
      'Society and Culture. Choose any eligible course in this area to satisfy the GE Area C requirement.'
    ],
    GED: [
      'GE Area D Courses',
      'Self and Humanity. Choose any eligible course in this area to satisfy the GE Area D requirement.'
    ]
  },
  'zh-CN': {
    GEB: ['GE B 范围课程', '自然、科学与科技。修读该范围任意符合要求课程即可满足 GE Area B 要求。'],
    GEC: ['GE C 范围课程', '社会与文化。修读该范围任意符合要求课程即可满足 GE Area C 要求。'],
    GED: ['GE D 范围课程', '自我与人文。修读该范围任意符合要求课程即可满足 GE Area D 要求。']
  },
  'zh-TW': {
    GEB: ['GE B 範圍課程', '自然、科學與科技。修讀該範圍任意符合要求課程即可滿足 GE Area B 要求。'],
    GEC: ['GE C 範圍課程', '社會與文化。修讀該範圍任意符合要求課程即可滿足 GE Area C 要求。'],
    GED: ['GE D 範圍課程', '自我與人文。修讀該範圍任意符合要求課程即可滿足 GE Area D 要求。']
  }
}

const AREA_CODES: Record<AreaKey, AreaCode> = { b: 'GEB', c: 'GEC', d: 'GED' }

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

function applyLang(value: Lang): void {
  lang.value = value
  try {
    localStorage.setItem(LANG_KEY, value)
  } catch (error) {
    // 忽略写入失败，语言切换仍然生效
  }
}

/* =====================================================================
 * 路由参数 → 区
 * ===================================================================== */

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 路由参数 area ∈ b|c|d；非法值回落到 B（原型只有 B/C/D 三页） */
const areaKey = computed<AreaKey>(() => {
  const raw = route.params.area
  const value = String(Array.isArray(raw) ? raw[0] : (raw ?? '')).trim().toLowerCase()
  return value === 'c' || value === 'd' ? value : 'b'
})

const areaCode = computed<AreaCode>(() => AREA_CODES[areaKey.value])

const areaMeta = computed<AreaMeta>(() => {
  const texts = AREA_TEXTS[lang.value][areaCode.value] ?? AREA_TEXTS.en[areaCode.value]
  return { code: areaCode.value, title: texts[0], subtitle: texts[1] }
})

const areaOptions = computed<AreaOption[]>(() =>
  (['b', 'c', 'd'] as AreaKey[]).map((key) => {
    const code = AREA_CODES[key]
    return {
      key,
      label: code,
      title: (AREA_TEXTS[lang.value][code] ?? AREA_TEXTS.en[code])[0]
    }
  })
)

function goArea(key: AreaKey): void {
  if (key === areaKey.value) return
  void router.push('/student/ge/' + key)
}

/* =====================================================================
 * 课程数据
 * ===================================================================== */

const courses = ref<EduCourse[]>([])
const keyword = ref('')
const loading = ref(true)
const errorMessage = ref('')

function compact(value: string | undefined): string {
  return String(value ?? '').replace(/\s+/g, '').toUpperCase()
}

function hasOutline(item: EduCourse): boolean {
  return item.outlineStatus === 'saved_outline'
}

function unitsOf(item: EduCourse): number {
  return typeof item.credits === 'number' ? item.credits : 3
}

/** 标题回退顺序与原型 title() 一致（新库无繁体字段，繁体回落到简体） */
function titleOf(item: EduCourse): string {
  const code = compact(item.code)
  if (lang.value === 'zh-CN' || lang.value === 'zh-TW') {
    return item.titleZh || item.titleEn || code
  }
  return item.titleEn || code
}

function courseLink(item: EduCourse): string {
  return '/student/course/' + encodeURIComponent(String(item.code ?? ''))
}

/** 原型 matchesArea：课程代码前缀命中该区，或学科字段包含该区代码 */
function matchesArea(item: EduCourse): boolean {
  const code = compact(item.code)
  if (code.startsWith(areaCode.value)) return true
  const subject = compact(item.subject)
  return subject.startsWith(areaCode.value) || subject.includes(areaCode.value)
}

function availabilityLabel(available: boolean): string {
  return available ? t('available') : t('unavailable')
}

function availabilityHeading(available: boolean, count: number): string {
  return `${availabilityLabel(available)} · ${count} ${t('countSuffix')}`
}

const visibleCourses = computed<EduCourse[]>(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return courses.value
  return courses.value.filter(
    (item) => compact(item.code).toLowerCase().includes(query) || titleOf(item).toLowerCase().includes(query)
  )
})

const availabilityGroups = computed<CourseGroup[]>(() =>
  [
    { available: true, rows: visibleCourses.value.filter((item) => hasOutline(item)) },
    { available: false, rows: visibleCourses.value.filter((item) => !hasOutline(item)) }
  ].filter((group) => group.rows.length > 0)
)

function errorText(error: unknown): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'string') return error
  return t('loadError')
}

/**
 * 服务端筛选：category=GenEd + code 前缀（GEB/GEC/GED），分页拉全后再按原型规则二次校验。
 * 单页 200 条即可覆盖一个区（实际 48/65/55 门），循环上限 5 页避免异常分页死循环。
 */
async function loadCourses(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  courses.value = []
  try {
    const prefix = areaCode.value
    const collected: EduCourse[] = []
    let pageNum = 1
    while (pageNum <= 5) {
      const result = await listCourse({ category: 'GenEd', code: prefix, pageNum, pageSize: 200 })
      const rows = Array.isArray(result?.rows) ? result.rows : []
      collected.push(...rows)
      const total = typeof result?.total === 'number' ? result.total : collected.length
      if (!rows.length || collected.length >= total) break
      pageNum += 1
    }
    courses.value = collected
      .filter((item) => matchesArea(item))
      .sort((left, right) => compact(left.code).localeCompare(compact(right.code)))
  } catch (error) {
    errorMessage.value = errorText(error)
    courses.value = []
  } finally {
    loading.value = false
    updateDocumentTitle()
  }
}

function updateDocumentTitle(): void {
  document.title = areaMeta.value.title
}

/* =====================================================================
 * 当前用户
 * ===================================================================== */

const currentUserName = computed<string>(() => {
  const nickname = String(userStore.nickName ?? '').trim()
  if (nickname) return nickname
  return String(userStore.name ?? '').trim()
})

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
 * 生命周期
 * ===================================================================== */

onMounted(() => {
  document.documentElement.lang = lang.value
  void loadCourses()
})

// 同一个组件在 B/C/D 之间切换：只改路由参数，重新拉该区课程
watch(areaKey, () => {
  void loadCourses()
})

watch(lang, (value) => {
  document.documentElement.lang = value
  updateDocumentTitle()
})
</script>

<style scoped src="../../../assets/student/ge.css"></style>

<style scoped>
/* ---------------------------------------------------------------------
 * 原 ge_area_*.html 的 .topnav 里没有 .user-pill（原型该页无登录态），
 * 这里补上与 course-detail 完全一致的胶囊样式；
 * .area-toggle 复用 .lang-toggle 的外观，作为 B/C/D 三区切换入口。
 * ------------------------------------------------------------------- */
.area-toggle{margin-right:4px}
.user-pill{display:flex;align-items:center;gap:8px;color:#fff;font-size:12px;background:rgba(255,255,255,.12);padding:5px 10px;border-radius:20px}
.user-pill span{font-size:12px;font-weight:700;color:#fff}
.user-pill .ub{cursor:pointer;background:rgba(255,255,255,.2);border:none;color:#fff;border-radius:20px;padding:2px 8px;font-size:11px;font-weight:700}
.user-pill .ub:hover{background:rgba(255,255,255,.35)}
.empty-retry{margin-top:10px;background:none;border:none;color:var(--accent);font-size:13px;font-weight:800;cursor:pointer;text-decoration:underline;font-family:inherit}
</style>
