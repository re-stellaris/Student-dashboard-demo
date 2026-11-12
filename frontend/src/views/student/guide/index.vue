<template>
  <div class="sp-root">
    <!-- 原页面 .topnav：brand + Home / Dashboard + 语言切换（+ 用户胶囊，新架构补登录态） -->
    <nav class="topnav">
      <router-link to="/student" class="nav-logo">
        🎓 <span>{{ t('nav.title') }}</span>
      </router-link>
      <router-link to="/student" class="nav-link">🏠 <span>{{ t('nav.home') }}</span></router-link>
      <router-link to="/student" class="nav-link">📊 <span>{{ t('nav.dashboard') }}</span></router-link>
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

    <div class="wrap">
      <h1>{{ t('guide.h1') }}</h1>
      <p class="subtitle">{{ t('guide.sub') }}</p>

      <div class="step">
        <div class="step-num">1</div>
        <div>
          <h2>🏫 Pick your school(s)</h2>
          <p>
            Use the chips at the top of the dashboard. Click <b>All</b> for every department, or click
            specific ones (ACT, MAT, CSC, etc.) to filter. Add a <b>Term</b> filter to see only courses
            offered in Fall or Spring.
          </p>
        </div>
      </div>

      <div class="step">
        <div class="step-num">2</div>
        <div>
          <h2>🎒 Build your portfolio</h2>
          <p>
            Click any course on the left to add it to your portfolio. Or click
            <b>📥 Import Transcript</b> to load an Excel/CSV file (columns:
            <code>Code | Title | Grade | Semester</code>). Selections persist on this device — no signup
            needed.
          </p>
        </div>
      </div>

      <div class="step">
        <div class="step-num">3</div>
        <div>
          <h2>📊 Read your skill profile</h2>
          <p>
            Two bar charts show your <b>Major Skills</b> (specialized to your dept) and
            <b>General Skills</b> (cross-disciplinary). The solid blue bar = your portfolio. The red
            marker = department average.
          </p>
        </div>
      </div>

      <div class="card">
        <h2>📐 Skill Maturity Scale</h2>
        <p class="card-lead">
          Every score (0–100) maps to one of four maturity levels. The bar background uses a single-hue
          blue gradient (color-blind safe) with vertical markers at <b>25 / 55 / 75</b> showing the
          boundaries.
        </p>
        <div class="scale">
          <div
            v-for="cell in scaleCells"
            :key="cell.level"
            class="scale-cell"
            :style="{ background: cell.background, color: cell.color }"
          >
            <div class="r">{{ cell.range }}</div>
            <div class="l">{{ cell.level }}</div>
            <div class="d">{{ cell.description }}</div>
          </div>
        </div>
      </div>

      <div class="card">
        <h2>🧮 How scores are computed</h2>
        <p class="card-lead">
          For every course we read its <b>outcome</b>, <b>syllabus</b>, and <b>description</b>, then match
          the text against keyword lists for each skill dimension.
        </p>
        <div class="formula">
          <b>score</b> = coverage × (0.65 + depth × 0.20 + specificity × 0.15) × 100
        </div>
        <ul>
          <li>
            <b>Coverage (65%)</b> — how many distinct keywords for the skill appear in the course text
          </li>
          <li><b>Depth (20%)</b> — how often those keywords recur (frequency / repetition)</li>
          <li>
            <b>Specificity (15%)</b> — whether the skill keyword appears in the course title or first
            sentence
          </li>
        </ul>
        <div class="tip">
          💡 <b>Department Average</b> (red marker on each bar) = mean of all courses in that department
          where the skill score is <b>greater than 0</b>. Courses that don't cover the skill at all are
          excluded, so the average reflects <i>real depth</i> instead of being dragged down by unrelated
          courses.
        </div>
      </div>

      <div class="card">
        <h2>🎯 Major vs General Skills</h2>
        <p class="card-lead">
          <b>Major Skills</b> are 9 specialized competencies <i>per department</i> (e.g., Financial
          Reporting for ACT, Calculus &amp; Analysis for MAT). They show your depth inside your major.
        </p>
        <p class="card-lead card-lead--tight">
          <b>General Skills</b> are 9 universal competencies that apply to every student:
        </p>
        <ul>
          <li>
            <b>PK</b> Professional Knowledge — <b>auto-computed</b> as the average of your major-skill
            scores (your overall major depth in one number)
          </li>
          <li><b>QA</b> Quantitative Analysis — math, stats, modeling, data-driven decisions</li>
          <li><b>DI</b> Data &amp; Information Literacy — collection, cleaning, analysis, visualization</li>
          <li>
            <b>CM</b> Communication · <b>IP</b> Innovation &amp; Problem Solving · <b>ER</b> Ethics &amp;
            Social Responsibility
          </li>
          <li>
            <b>TL</b> Teamwork &amp; Leadership · <b>IC</b> Intercultural Competency ·
            <b>SL</b> Self-directed Learning
          </li>
        </ul>
      </div>

      <div class="card">
        <h2>📈 Grade Trajectory</h2>
        <p class="card-lead">
          The Grade Trajectory chart plots <b>course letter grades</b> (blue dots),
          <b>semester GPA</b> (solid line), <b>current CGPA</b> (horizontal solid line), and your
          <b>goal GPA</b> (red dashed line). Add grades manually or via
          <b>Import Transcript</b>.
        </p>
      </div>

      <div class="cta">
        <router-link to="/student" class="btn btn-primary">🚀 Open the Dashboard</router-link>
        <router-link to="/student" class="btn btn-outline">← Back home</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="StudentGuide">
import { computed, onMounted, ref, watch } from 'vue'
import useUserStore from '@/store/modules/user'

/* =====================================================================
 * 类型
 * ===================================================================== */

type Lang = 'en' | 'zh-CN' | 'zh-TW'
type Dict = Record<string, string>

interface ScaleCell {
  range: string
  level: string
  description: string
  background: string
  color: string
}

/* =====================================================================
 * i18n（字典逐字取自 legacy/guide.html 的 I18N_GUIDE）
 *
 * 说明：原型里只有 nav.title / nav.home / nav.dashboard / guide.h1 /
 * guide.sub 这 5 个键参与翻译，正文（步骤、卡片、公式、提示、CTA）在
 * 原文件里就是硬编码英文 —— 这里的实现范围与原型完全一致。
 * ===================================================================== */

const LANG_KEY = 'cuhksz_lang'

const I18N_GUIDE: Record<Lang, Dict> = {
  en: {
    'nav.title': 'Study Dashboard',
    'nav.home': 'Home',
    'nav.dashboard': 'Dashboard',
    'guide.h1': '📖 Quick Start Guide',
    'guide.sub': 'Three steps to read your skill profile · 1 minute',
    signOut: 'Sign out',
    signOutConfirm: 'Sign out? Your portfolio is preserved on this device.'
  },
  'zh-CN': {
    'nav.title': '学习仪表盘',
    'nav.home': '首页',
    'nav.dashboard': '仪表盘',
    'guide.h1': '📖 快速开始指南',
    'guide.sub': '三步读懂你的技能档案 · 一分钟',
    signOut: '退出登录',
    signOutConfirm: '确认退出登录？本机的作品集数据会保留。'
  },
  'zh-TW': {
    'nav.title': '學習儀表板',
    'nav.home': '首頁',
    'nav.dashboard': '儀表板',
    'guide.h1': '📖 快速開始指南',
    'guide.sub': '三步讀懂你的技能檔案 · 一分鐘',
    signOut: '登出',
    signOutConfirm: '確認登出？本機的作品集資料會保留。'
  }
}

const langOptions: Array<{ value: Lang; label: string }> = [
  { value: 'en', label: 'EN' },
  { value: 'zh-CN', label: '简中' },
  { value: 'zh-TW', label: '繁中' }
]

/** 四档成熟度（原页面 4 个 .scale-cell 的内联配色，原样保留） */
const scaleCells: ScaleCell[] = [
  { range: '0 – 25', level: 'Exposure', description: 'Skill mentioned, not the focus', background: '#eff3ff', color: '#1e3a8a' },
  { range: '25 – 55', level: 'Foundation', description: 'Secondary agenda', background: '#bdd7e7', color: '#1e40af' },
  { range: '55 – 75', level: 'Proficiency', description: 'Core skill, structured training', background: '#6baed6', color: '#0c4a6e' },
  { range: '75 – 100', level: 'Mastery', description: 'Primary purpose', background: '#2171b5', color: '#ffffff' }
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
  return I18N_GUIDE[lang.value][key] ?? I18N_GUIDE.en[key] ?? key
}

function applyLang(value: Lang): void {
  lang.value = value
  try {
    localStorage.setItem(LANG_KEY, value)
  } catch (error) {
    // 忽略写入失败，语言切换仍然生效
  }
  document.documentElement.lang = value
}

/* =====================================================================
 * 当前用户（原 guide.html 没有登录态，新架构补 .user-pill + Sign out）
 * ===================================================================== */

const userStore = useUserStore()

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
})

watch(lang, (value) => {
  document.documentElement.lang = value
})
</script>

<style scoped src="../../../assets/student/guide.css"></style>

<style scoped>
/* ---------------------------------------------------------------------
 * 原 guide.html 的 .topnav 没有 .user-pill（原型该页无登录态），
 * 这里补上与 course-detail / ge 完全一致的胶囊样式；
 * .card-lead 只是把原页面 card 里内联的
 * style="color:var(--text2);font-size:13.5px" 提成语义类，渲染结果一致。
 * ------------------------------------------------------------------- */
.user-pill{display:flex;align-items:center;gap:8px;color:#fff;font-size:12px;background:rgba(255,255,255,.12);padding:5px 10px;border-radius:20px}
.user-pill span{font-size:12px;font-weight:700;color:#fff}
.user-pill .ub{cursor:pointer;background:rgba(255,255,255,.2);border:none;color:#fff;border-radius:20px;padding:2px 8px;font-size:11px;font-weight:700}
.user-pill .ub:hover{background:rgba(255,255,255,.35)}
.card-lead{color:var(--text2);font-size:13.5px}
.card-lead--tight{margin-top:6px}
</style>
