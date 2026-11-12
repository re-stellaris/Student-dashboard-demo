<template>
  <div class="sp-root">
    <!-- ── Nav：与 views/student/dashboard 的顶栏一致（logo / 导航 / 语言 / 用户胶囊） ── -->
    <nav class="topnav">
      <router-link class="nav-logo" to="/student">🎓 <span>{{ t('nav.title') }}</span></router-link>
      <span class="nav-sep">›</span>
      <router-link class="nav-link" to="/student">🏠 <span>{{ t('nav.home') }}</span></router-link>
      <router-link class="nav-link" to="/student/guide">📖 <span>{{ t('nav.guide') }}</span></router-link>
      <router-link class="nav-link" to="/student/transcript">📄 <span>{{ t('nav.transcript') }}</span></router-link>
      <div class="nav-spacer"></div>
      <div class="lang-toggle">
        <button
          v-for="item in LANGS"
          :key="item.code"
          class="lang-btn"
          :class="{ active: lang === item.code }"
          type="button"
          :aria-pressed="lang === item.code"
          @click="setLang(item.code)"
        >
          {{ item.label }}
        </button>
      </div>
      <div class="user-pill" style="margin-left: 12px">
        <span>{{ userLabel }}</span>
        <button class="ub" type="button" @click="signOut">{{ t('nav.signout') }}</button>
      </div>
    </nav>

    <div class="page-shell">
      <!-- ═══ TRANSCRIPT WORKFLOW（沿用旧版 section 结构与 class） ═══ -->
      <section id="transcript-workflow" class="section transcript-section">
        <div class="section-eyebrow">{{ t('section.eyebrow') }}</div>
        <h2 class="section-title">{{ t('section.title') }}</h2>
        <p class="section-sub">{{ t('section.sub') }}</p>

        <!-- Drop zone -->
        <div
          id="dropzone"
          class="dropzone"
          :class="{ 'is-dragging': dragging }"
          tabindex="0"
          role="button"
          :aria-label="t('drop.aria')"
          @click="openFilePicker"
          @keydown.enter.prevent="openFilePicker"
          @keydown.space.prevent="openFilePicker"
          @dragover.prevent="onDragOver"
          @dragleave.prevent="onDragLeave"
          @drop.prevent="onDrop"
        >
          <input
            id="file-input"
            ref="fileInput"
            type="file"
            accept=".xlsx,.xls,.csv,.pdf"
            hidden
            @change="onFileChange"
          />
          <div class="dz-icon" aria-hidden="true">↑</div>
          <h3>{{ t('drop.title') }}</h3>
          <p>
            {{ t('drop.or') }}
            <span id="browse-link" class="browse-link" @click.stop="openFilePicker">{{ t('drop.browse') }}</span>
          </p>
          <small>{{ t('drop.supported') }}</small>
          <div class="dz-privacy-pill">
            <span aria-hidden="true">🔒</span>
            <span>{{ t('drop.privacy') }}</span>
          </div>
        </div>

        <!-- 解析失败 / 文件类型不支持 -->
        <div v-if="errorMessage" class="transcript-warning is-error" role="alert">
          <span class="icon" aria-hidden="true">!</span>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- Processing status -->
        <div v-if="processingVisible" id="processing-status" class="processing-status">
          <div
            v-for="step in stepRows"
            :key="step.key"
            class="status-step"
            :class="stepState[step.key]"
            :data-step="step.key"
          >
            <span class="icon">{{ step.icon }}</span>
            <span class="label">{{ step.label }}</span>
            <div class="progress-bar"><span :style="{ width: stepProgress[step.key] + '%' }"></span></div>
          </div>
        </div>

        <!-- Split review panel -->
        <div v-if="parsed" id="review-panel" class="review-panel">
          <div class="review-grid">
            <!-- LEFT：原始文件（本轮简化为文件名 + 大小 + 解析摘要） -->
            <div class="review-pane">
              <div class="review-pane-header">
                <span>{{ t('review.originalTitle') }}</span>
                <span style="font-size: 11px; color: var(--text3); font-weight: 600">
                  {{ t('review.originalBadge') }}
                </span>
              </div>
              <div id="original-preview" class="preview-area">
                <div class="preview-file">
                  <div class="pf-name">{{ parsed.fileName }}</div>
                  <div class="pf-row">
                    <span class="pf-key">{{ t('preview.size') }}</span>
                    <span>{{ formatSize(parsed.fileSize) }}</span>
                  </div>
                  <div class="pf-row">
                    <span class="pf-key">{{ t('preview.courses') }}</span>
                    <span>{{ parsed.rowCount }}</span>
                  </div>
                  <div class="pf-row">
                    <span class="pf-key">{{ t('preview.name') }}</span>
                    <span>{{ parsed.studentName || t('unknown') }}</span>
                  </div>
                  <div class="pf-row">
                    <span class="pf-key">{{ t('preview.id') }}</span>
                    <span>{{ parsed.studentNo || t('unknown') }}</span>
                  </div>
                  <div class="pf-row">
                    <span class="pf-key">{{ t('preview.major') }}</span>
                    <span>{{ parsed.major || t('unknown') }}</span>
                  </div>
                  <div class="pf-row">
                    <span class="pf-key">{{ t('preview.terms') }}</span>
                    <span>{{ parsed.terms.join(' · ') || t('unknown') }}</span>
                  </div>
                  <div class="pf-hint">{{ t('preview.hint') }}</div>
                </div>
              </div>
            </div>

            <!-- RIGHT：解析结果（可编辑） -->
            <div class="review-pane">
              <div class="review-pane-header">
                <span>{{ t('review.parsedTitle') }}</span>
                <span class="confidence-badge ok">{{ t('review.confidence') }}</span>
              </div>
              <div class="parsed-pane-body">
                <div class="student-info-block">
                  <label>
                    {{ t('field.name') }}
                    <input id="parsed-name" v-model="form.studentName" type="text" />
                  </label>
                  <label>
                    {{ t('field.id') }}
                    <input id="parsed-id" v-model="form.studentNo" type="text" />
                  </label>
                  <label style="grid-column: 1 / -1">
                    {{ t('field.major') }}
                    <input
                      id="parsed-major"
                      v-model="form.major"
                      type="text"
                      :placeholder="t('field.majorPlaceholder')"
                    />
                  </label>
                </div>

                <table class="course-edit-table">
                  <thead>
                    <tr>
                      <th></th>
                      <th class="col-code">{{ t('table.code') }}</th>
                      <th>{{ t('table.title') }}</th>
                      <th class="col-credit">{{ t('table.units') }}</th>
                      <th class="col-grade">{{ t('table.grade') }}</th>
                      <th class="col-term">{{ t('table.term') }}</th>
                      <th class="col-action"></th>
                    </tr>
                  </thead>
                  <tbody id="course-rows">
                    <tr v-for="(row, index) in form.rows" :key="index">
                      <td>
                        <span class="row-confidence" :class="{ low: !row.courseCode || !row.termCode }"></span>
                      </td>
                      <td class="col-code"><input v-model="row.courseCode" type="text" /></td>
                      <td><input v-model="row.courseTitle" type="text" /></td>
                      <td class="col-credit">
                        <input v-model.number="row.units" type="number" min="0" step="0.5" />
                      </td>
                      <td class="col-grade"><input v-model="row.grade" type="text" /></td>
                      <td class="col-term"><input v-model="row.termCode" type="text" /></td>
                      <td class="col-action">
                        <button
                          type="button"
                          class="delete-row"
                          :title="t('review.remove')"
                          @click="removeRow(index)"
                        >
                          ✕
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <button id="add-row-btn" type="button" class="add-row-btn" @click="addRow">
                  {{ t('review.addRow') }}
                </button>
              </div>
              <div class="review-actions">
                <span id="parse-meta" class="meta">{{ metaText }}</span>
                <div class="actions">
                  <button id="reupload-btn" type="button" class="btn-ghost" @click="reupload">
                    {{ t('review.reupload') }}
                  </button>
                  <button
                    id="confirm-btn"
                    type="button"
                    class="btn-confirm"
                    :disabled="importing || !form.rows.length"
                    @click="confirmImport"
                  >
                    {{ importing ? t('review.importing') : t('review.confirm') }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 解析提示（警告，不阻塞导入） -->
          <div v-if="notes.length" class="transcript-notes">
            <div v-for="note in notes" :key="note">· {{ note }}</div>
          </div>

          <!-- 必须显著提示：导入的是自填数据 -->
          <div class="transcript-warning">
            <span class="icon" aria-hidden="true">!</span>
            <span>{{ t('warning.selfData') }}</span>
          </div>

          <!-- 导入失败 -->
          <div v-if="importError" class="transcript-warning is-error" role="alert">
            <span class="icon" aria-hidden="true">!</span>
            <span>{{ importError }}</span>
          </div>

          <!-- 导入结果 -->
          <template v-if="importResult">
            <div id="snapshot-banner" class="snapshot-banner">
              <div class="sb-icon" aria-hidden="true">✨</div>
              <div class="sb-text">
                <strong>{{ t('result.title') }}</strong>
                <span>{{ resultSummary }}</span>
              </div>
              <router-link class="sb-link" to="/student">{{ t('result.back') }} →</router-link>
            </div>

            <div class="snapshot-summary-card">
              <div class="snapshot-stat">
                <span class="v">{{ importResult.inserted }}</span>
                <span class="l">{{ t('stat.inserted') }}</span>
              </div>
              <div class="snapshot-stat">
                <span class="v">{{ importResult.updated }}</span>
                <span class="l">{{ t('stat.updated') }}</span>
              </div>
              <div class="snapshot-stat">
                <span class="v">{{ importResult.skipped }}</span>
                <span class="l">{{ t('stat.skipped') }}</span>
              </div>
              <div class="snapshot-stat">
                <span class="v">{{ importResult.rowCount }}</span>
                <span class="l">{{ t('stat.submitted') }}</span>
              </div>
            </div>

            <div v-if="importResult.unknownCourses.length" class="transcript-block">
              <div class="tb-title">{{ t('result.unknownCourses') }}</div>
              <div class="pill-row">
                <span v-for="code in importResult.unknownCourses" :key="code" class="pill">{{ code }}</span>
              </div>
            </div>

            <div v-if="importResult.unknownTerms.length" class="transcript-block">
              <div class="tb-title">{{ t('result.unknownTerms') }}</div>
              <div class="pill-row">
                <span v-for="term in importResult.unknownTerms" :key="term" class="pill">{{ term }}</span>
              </div>
            </div>

            <div v-if="skippedRows.length" class="transcript-block">
              <div class="tb-title">{{ t('result.skippedDetails') }}</div>
              <div class="transcript-notes" style="margin-top: 0">
                <div v-for="row in skippedRows" :key="row.courseCode + row.termCode">
                  · {{ row.courseCode }} · {{ row.termCode }} — {{ row.reason }}
                </div>
              </div>
            </div>

            <div class="transcript-warning">
              <span class="icon" aria-hidden="true">!</span>
              <span>{{ t('warning.selfData') }}</span>
            </div>
          </template>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts" name="StudentTranscript">
import useUserStore from '@/store/modules/user'
import { importTranscript } from '@/api/education/transcript'
import type { TranscriptImportResult, TranscriptSkippedDetail } from '@/api/education/transcript'
import { PDF_NOT_SUPPORTED, parseTranscriptWorkbook } from '@/utils/transcript/parseTranscript'
import type { ParsedTranscript, TranscriptRow } from '@/utils/transcript/parseTranscript'

/* =====================================================================
 * 类型与常量
 * ===================================================================== */
type Lang = 'en' | 'zh-CN' | 'zh-TW'
type StepKey = 'render' | 'ocr' | 'llm' | 'map'
type StepState = '' | 'active' | 'done' | 'error'

interface LangOption {
  code: Lang
  label: string
}

interface StepRow {
  key: StepKey
  icon: string
  label: string
}

/** 左侧「原始文件」面板的摘要信息 */
interface ParsedMeta {
  fileName: string
  fileSize: number
  rowCount: number
  studentName: string
  studentNo: string
  major: string
  terms: string[]
  warnings: string[]
}

const LANGS: LangOption[] = [
  { code: 'en', label: 'EN' },
  { code: 'zh-CN', label: '简中' },
  { code: 'zh-TW', label: '繁中' }
]

const LS_LANG = 'cuhksz_lang'
const MAX_FILE_SIZE = 10 * 1024 * 1024
/** 与 api-contract §10.2 一致：单次导入上限 500 行 */
const MAX_ROWS = 500

/* =====================================================================
 * i18n
 * ===================================================================== */
const I18N: Record<Lang, Record<string, string>> = {
  en: {
    'nav.title': 'Study Dashboard',
    'nav.home': 'Home',
    'nav.guide': 'Guide',
    'nav.transcript': 'Transcript Upload',
    'nav.signout': 'Sign out',
    'section.eyebrow': 'step · transcript intake',
    'section.title': 'Bring Your Transcript',
    'section.sub':
      'Upload your CUHK-Shenzhen transcript as Excel or CSV. We read it in your browser, recognise term headers and course rows, and let you verify every row before importing it into your academic record. PDF transcripts are not supported yet.',
    'drop.aria': 'Upload your transcript file',
    'drop.title': 'Drop your transcript here',
    'drop.or': 'or',
    'drop.browse': 'browse files',
    'drop.supported': 'Supported: XLSX, XLS, CSV · Max 10 MB',
    'drop.privacy': 'Files never leave your browser',
    'step.render': 'Reading the file…',
    'step.ocr': 'Skipped — file already structured',
    'step.llm': 'Parsing terms, courses & summary…',
    'step.map': 'Preparing rows for import…',
    'review.originalTitle': '📄 Original Transcript',
    'review.originalBadge': 'document preview',
    'review.parsedTitle': '✏️ Parsed & Editable',
    'review.confidence': 'structured layout',
    'review.addRow': '+ Add Course Manually',
    'review.remove': 'Remove this row',
    'review.reupload': 'Re-upload',
    'review.confirm': 'Confirm & Import →',
    'review.importing': 'Importing…',
    'field.name': 'Student Name',
    'field.id': 'Student ID',
    'field.major': 'Major / Programme',
    'field.majorPlaceholder': 'e.g. Computer Science and Engineering',
    'table.code': 'Code',
    'table.title': 'Course Name',
    'table.units': 'Credit',
    'table.grade': 'Grade',
    'table.term': 'Term',
    'preview.size': 'SIZE',
    'preview.courses': 'COURSES',
    'preview.name': 'NAME',
    'preview.id': 'ID',
    'preview.major': 'MAJOR',
    'preview.terms': 'TERMS',
    'preview.hint': 'Parsed on your device — nothing has been uploaded yet.',
    'meta.rows': 'rows',
    'meta.terms': 'terms',
    unknown: 'not recognised',
    'warning.selfData':
      'Heads-up: this import contains self-reported data from a file you uploaded yourself. Official transcript records are never overwritten — only your own uploaded rows are added or updated.',
    'result.title': 'Import finished',
    'result.back': 'Back to dashboard',
    'result.total': 'Submitted',
    'result.unknownCourses': 'Course codes not found in the course catalogue (kept as text)',
    'result.unknownTerms': 'Term codes not found in the term table (skipped by the server)',
    'result.skippedDetails': 'Rows skipped by the server',
    'stat.inserted': 'Inserted',
    'stat.updated': 'Updated',
    'stat.skipped': 'Skipped',
    'stat.submitted': 'Submitted',
    'error.tooLarge': 'File is larger than 10 MB. Please compress or split your transcript.',
    'error.type': 'Unsupported file type. Please use XLSX, XLS, or CSV.',
    'error.empty': 'There is no course row to import.',
    'error.incomplete': 'rows are missing a course code or a term. Fill them in or delete those rows first.',
    'error.tooMany': 'Too many rows (the limit is 500). Please split the file and import it in parts.',
    'error.import': 'Import failed:',
    'error.importHint':
      'The endpoint /edu/transcripts/import may not be deployed yet — the frontend follows the API contract, so integration is pending the backend.',
    'settings.saveError': 'The language preference could not be saved in this browser.'
  },
  'zh-CN': {
    'nav.title': '学习仪表盘',
    'nav.home': '首页',
    'nav.guide': '使用指南',
    'nav.transcript': '成绩单上传',
    'nav.signout': '登出',
    'section.eyebrow': 'step · transcript intake',
    'section.title': '上传你的成绩单',
    'section.sub':
      '上传香港中文大学（深圳）的成绩单（Excel 或 CSV）。文件在你本机浏览器里读取，自动识别学期表头与课程行，全部核对无误后再导入到你的学业记录。本轮暂不支持 PDF。',
    'drop.aria': '上传成绩单文件',
    'drop.title': '把成绩单拖到这里',
    'drop.or': '或者',
    'drop.browse': '选择文件',
    'drop.supported': '支持：XLSX、XLS、CSV · 最大 10 MB',
    'drop.privacy': '文件不会离开你的浏览器',
    'step.render': '正在读取文件…',
    'step.ocr': '已跳过 — 文件本身已是结构化数据',
    'step.llm': '正在解析学期、课程与汇总…',
    'step.map': '正在整理待导入的行…',
    'review.originalTitle': '📄 原始成绩单',
    'review.originalBadge': '文件预览',
    'review.parsedTitle': '✏️ 解析结果（可编辑）',
    'review.confidence': '结构化表格',
    'review.addRow': '+ 手动添加课程',
    'review.remove': '删除这一行',
    'review.reupload': '重新选择',
    'review.confirm': '确认导入 →',
    'review.importing': '导入中…',
    'field.name': '学生姓名',
    'field.id': '学号',
    'field.major': '专业 / 主修',
    'field.majorPlaceholder': '例如 Computer Science and Engineering',
    'table.code': '代码',
    'table.title': '课程名称',
    'table.units': '学分',
    'table.grade': '成绩',
    'table.term': '学期',
    'preview.size': '大小',
    'preview.courses': '课程数',
    'preview.name': '姓名',
    'preview.id': '学号',
    'preview.major': '专业',
    'preview.terms': '学期',
    'preview.hint': '解析全部在本机完成 — 还没有上传任何数据。',
    'meta.rows': '行',
    'meta.terms': '个学期',
    unknown: '未识别',
    'warning.selfData':
      '注意：本次导入的是你自己上传的自填数据，不会覆盖官方成绩。导入只会新增或更新你自己上传的行，官方成绩与演示数据保持不变。',
    'result.title': '导入完成',
    'result.back': '返回主控台',
    'result.total': '提交',
    'result.unknownCourses': '课程库未收录的课程代码（已按原文保存）',
    'result.unknownTerms': '学期表未收录的学期代码（后端已跳过）',
    'result.skippedDetails': '后端跳过的行',
    'stat.inserted': '新增',
    'stat.updated': '覆盖',
    'stat.skipped': '跳过',
    'stat.submitted': '提交行数',
    'error.tooLarge': '文件超过 10 MB，请压缩或拆分后再上传。',
    'error.type': '不支持的文件类型，请上传 XLSX、XLS 或 CSV。',
    'error.empty': '没有可导入的课程行。',
    'error.incomplete': '行缺少课程代码或学期，请先补全或删除这些行。',
    'error.tooMany': '行数超过 500 行的上限，请拆分文件后分批导入。',
    'error.import': '导入失败：',
    'error.importHint':
      '（若返回 404，说明后端 /edu/transcripts/import 尚未部署；前端已严格按接口契约实现，联调待后端就绪。）',
    'settings.saveError': '当前浏览器无法保存语言设置。'
  },
  'zh-TW': {
    'nav.title': '學習儀表板',
    'nav.home': '首頁',
    'nav.guide': '使用指南',
    'nav.transcript': '成績單上傳',
    'nav.signout': '登出',
    'section.eyebrow': 'step · transcript intake',
    'section.title': '上傳你的成績單',
    'section.sub':
      '上傳香港中文大學（深圳）的成績單（Excel 或 CSV）。檔案在你本機瀏覽器裡讀取，自動辨識學期表頭與課程列，全部核對無誤後再匯入你的學業紀錄。本輪暫不支援 PDF。',
    'drop.aria': '上傳成績單檔案',
    'drop.title': '把成績單拖到這裡',
    'drop.or': '或者',
    'drop.browse': '選擇檔案',
    'drop.supported': '支援：XLSX、XLS、CSV · 最大 10 MB',
    'drop.privacy': '檔案不會離開你的瀏覽器',
    'step.render': '正在讀取檔案…',
    'step.ocr': '已略過 — 檔案本身已是結構化資料',
    'step.llm': '正在解析學期、課程與彙總…',
    'step.map': '正在整理待匯入的列…',
    'review.originalTitle': '📄 原始成績單',
    'review.originalBadge': '檔案預覽',
    'review.parsedTitle': '✏️ 解析結果（可編輯）',
    'review.confidence': '結構化表格',
    'review.addRow': '+ 手動新增課程',
    'review.remove': '刪除這一列',
    'review.reupload': '重新選擇',
    'review.confirm': '確認匯入 →',
    'review.importing': '匯入中…',
    'field.name': '學生姓名',
    'field.id': '學號',
    'field.major': '主修 / 專業',
    'field.majorPlaceholder': '例如 Computer Science and Engineering',
    'table.code': '代碼',
    'table.title': '課程名稱',
    'table.units': '學分',
    'table.grade': '成績',
    'table.term': '學期',
    'preview.size': '大小',
    'preview.courses': '課程數',
    'preview.name': '姓名',
    'preview.id': '學號',
    'preview.major': '主修',
    'preview.terms': '學期',
    'preview.hint': '解析全部在本機完成 — 還沒有上傳任何資料。',
    'meta.rows': '列',
    'meta.terms': '個學期',
    unknown: '未識別',
    'warning.selfData':
      '注意：本次匯入的是你自己上傳的自填資料，不會覆蓋官方成績。匯入只會新增或更新你自己上傳的列，官方成績與示範資料保持不變。',
    'result.title': '匯入完成',
    'result.back': '返回主控台',
    'result.total': '提交',
    'result.unknownCourses': '課程庫未收錄的課程代碼（已按原文保存）',
    'result.unknownTerms': '學期表未收錄的學期代碼（後端已略過）',
    'result.skippedDetails': '後端略過的列',
    'stat.inserted': '新增',
    'stat.updated': '覆蓋',
    'stat.skipped': '略過',
    'stat.submitted': '提交列數',
    'error.tooLarge': '檔案超過 10 MB，請壓縮或拆分後再上傳。',
    'error.type': '不支援的檔案類型，請上傳 XLSX、XLS 或 CSV。',
    'error.empty': '沒有可匯入的課程列。',
    'error.incomplete': '列缺少課程代碼或學期，請先補全或刪除這些列。',
    'error.tooMany': '列數超過 500 列的上限，請拆分檔案後分批匯入。',
    'error.import': '匯入失敗：',
    'error.importHint':
      '（若回傳 404，代表後端 /edu/transcripts/import 尚未部署；前端已嚴格依介面契約實作，聯調待後端就緒。）',
    'settings.saveError': '目前瀏覽器無法儲存語言設定。'
  }
}

function readLang(): Lang {
  try {
    const saved = localStorage.getItem(LS_LANG)
    if (saved === 'en' || saved === 'zh-CN' || saved === 'zh-TW') return saved
  } catch (error) {
    /* localStorage 不可用时退回英文 */
  }
  return 'en'
}

const lang = ref<Lang>(readLang())

function t(key: string): string {
  return I18N[lang.value][key] ?? I18N.en[key] ?? key
}

function setLang(next: Lang): void {
  lang.value = next
  try {
    localStorage.setItem(LS_LANG, next)
  } catch (error) {
    // localStorage 不可用（隐私模式）时语言切换仍然生效，只是不持久化
  }
  document.documentElement.lang = next
}

/* =====================================================================
 * 页面状态
 * ===================================================================== */
const router = useRouter()
const userStore = useUserStore()

const fileInput = ref<HTMLInputElement | null>(null)
const dragging = ref(false)
const parsing = ref(false)
const importing = ref(false)
const processingVisible = ref(false)
const errorMessage = ref('')
const importError = ref('')
const parsed = ref<ParsedMeta | null>(null)
const importResult = ref<TranscriptImportResult | null>(null)

const stepState = ref<Record<StepKey, StepState>>({ render: '', ocr: '', llm: '', map: '' })
const stepProgress = ref<Record<StepKey, number>>({ render: 0, ocr: 0, llm: 0, map: 0 })

const form = reactive({
  studentName: '',
  studentNo: '',
  major: '',
  rows: [] as TranscriptRow[]
})

/* =====================================================================
 * 计算属性
 * ===================================================================== */
const stepRows = computed<StepRow[]>(() => [
  { key: 'render', icon: '1', label: t('step.render') },
  { key: 'ocr', icon: '2', label: t('step.ocr') },
  { key: 'llm', icon: '3', label: t('step.llm') },
  { key: 'map', icon: '4', label: t('step.map') }
])

const userLabel = computed<string>(() => {
  const nickname = String(userStore.nickName ?? '').trim()
  if (nickname) return nickname
  const name = String(userStore.name ?? '').trim()
  return name || 'Student'
})

const notes = computed<string[]>(() => parsed.value?.warnings ?? [])

const metaText = computed<string>(() => {
  const meta = parsed.value
  if (!meta) return '—'
  return [
    `${form.rows.length} ${t('meta.rows')}`,
    formatSize(meta.fileSize),
    `${meta.terms.length} ${t('meta.terms')}`
  ].join(' · ')
})

const resultSummary = computed<string>(() => {
  const result = importResult.value
  if (!result) return ''
  return [
    `${t('result.total')} ${result.rowCount}`,
    `${t('stat.inserted')} ${result.inserted}`,
    `${t('stat.updated')} ${result.updated}`,
    `${t('stat.skipped')} ${result.skipped}`
  ].join(' · ')
})

const skippedRows = computed<TranscriptSkippedDetail[]>(() =>
  (importResult.value?.skippedDetails ?? []).map((row) => ({
    courseCode: String(row?.courseCode ?? '—'),
    termCode: String(row?.termCode ?? '—'),
    reason: String(row?.reason ?? '')
  }))
)

/* =====================================================================
 * 顶栏动作
 * ===================================================================== */
function signOut(): void {
  if (!window.confirm(t('nav.signout') + '?')) return
  userStore
    .logOut()
    .then(() => router.push('/login'))
    .catch(() => router.push('/login'))
}

/* =====================================================================
 * 工具
 * ===================================================================== */
function formatSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 KB'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function describeError(error: unknown): string {
  if (error instanceof Error && error.message) return error.message
  const text = String(error ?? '').trim()
  return text && text !== '[object Object]' ? text : 'unknown error'
}

/** 让浏览器有时间渲染「处理中」状态 */
function tick(): Promise<void> {
  return new Promise((resolve) => {
    window.setTimeout(resolve, 16)
  })
}

function resetSteps(): void {
  stepState.value = { render: '', ocr: '', llm: '', map: '' }
  stepProgress.value = { render: 0, ocr: 0, llm: 0, map: 0 }
}

function setStep(key: StepKey, state: StepState, progress: number): void {
  stepState.value[key] = state
  stepProgress.value[key] = progress
}

function markFailure(): void {
  ;(Object.keys(stepState.value) as StepKey[]).forEach((key) => {
    if (stepState.value[key] === 'active') stepState.value[key] = 'error'
  })
}

/* =====================================================================
 * 文件选择 / 拖拽
 * ===================================================================== */
function openFilePicker(): void {
  if (parsing.value || importing.value) return
  fileInput.value?.click()
}

function onFileChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) void handleFile(file)
  // 允许重复选择同一个文件
  input.value = ''
}

function onDragOver(): void {
  if (parsing.value || importing.value) return
  dragging.value = true
}

function onDragLeave(): void {
  dragging.value = false
}

function onDrop(event: DragEvent): void {
  dragging.value = false
  if (parsing.value || importing.value) return
  const file = event.dataTransfer?.files?.[0]
  if (file) void handleFile(file)
}

/* =====================================================================
 * 解析
 * ===================================================================== */
function resetForNewFile(): void {
  parsed.value = null
  importResult.value = null
  importError.value = ''
  errorMessage.value = ''
  form.studentName = ''
  form.studentNo = ''
  form.major = ''
  form.rows = []
}

async function handleFile(file: File): Promise<void> {
  if (parsing.value || importing.value) return
  resetForNewFile()
  processingVisible.value = false
  resetSteps()

  if (file.size > MAX_FILE_SIZE) {
    errorMessage.value = t('error.tooLarge')
    return
  }
  if (/\.pdf$/i.test(file.name)) {
    errorMessage.value = PDF_NOT_SUPPORTED
    return
  }
  if (!/\.(xlsx|xls|csv)$/i.test(file.name)) {
    errorMessage.value = t('error.type')
    return
  }

  parsing.value = true
  processingVisible.value = true
  setStep('render', 'active', 35)
  await tick()

  try {
    const payload: ArrayBuffer | string = /\.csv$/i.test(file.name)
      ? await file.text()
      : await file.arrayBuffer()
    setStep('render', 'done', 100)
    setStep('ocr', 'done', 100)
    setStep('llm', 'active', 45)
    await tick()

    const result: ParsedTranscript = parseTranscriptWorkbook(payload, file.name)
    setStep('llm', 'done', 100)
    setStep('map', 'active', 70)
    await tick()

    applyParsed(result, file)
    setStep('map', 'done', 100)
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    errorMessage.value = message
    markFailure()
  } finally {
    parsing.value = false
  }
}

function applyParsed(result: ParsedTranscript, file: File): void {
  form.studentName = result.studentName ?? ''
  form.studentNo = result.studentNo ?? ''
  form.major = result.major ?? ''
  form.rows = result.rows.map((row) => ({ ...row }))

  const terms: string[] = []
  result.rows.forEach((row) => {
    if (row.termCode && !terms.includes(row.termCode)) terms.push(row.termCode)
  })
  terms.sort()

  parsed.value = {
    fileName: file.name,
    fileSize: file.size,
    rowCount: result.rows.length,
    studentName: result.studentName ?? '',
    studentNo: result.studentNo ?? '',
    major: result.major ?? '',
    terms,
    warnings: result.warnings
  }
}

function reupload(): void {
  if (parsing.value || importing.value) return
  openFilePicker()
}

/* =====================================================================
 * 课程行编辑
 * ===================================================================== */
function addRow(): void {
  form.rows.push({
    courseCode: '',
    courseTitle: '',
    termCode: '',
    units: 0,
    grade: '',
    score: null,
    category: '',
    passed: true,
    status: 'Completed'
  })
}

function removeRow(index: number): void {
  if (index < 0 || index >= form.rows.length) return
  form.rows.splice(index, 1)
}

function normalizeRow(row: TranscriptRow): TranscriptRow {
  const units = Number(row.units)
  const scoreValue = row.score === null || row.score === undefined ? NaN : Number(row.score)
  return {
    courseCode: String(row.courseCode ?? '').replace(/\s+/g, '').trim().toUpperCase(),
    courseTitle: String(row.courseTitle ?? '').trim(),
    termCode: String(row.termCode ?? '').trim(),
    units: Number.isFinite(units) ? units : 0,
    grade: String(row.grade ?? '').trim().toUpperCase(),
    score: Number.isFinite(scoreValue) ? scoreValue : null,
    category: String(row.category ?? '').trim(),
    passed: Boolean(row.passed),
    status: String(row.status ?? '').trim() || (row.passed ? 'Completed' : 'In Progress')
  }
}

/* =====================================================================
 * 确认导入
 * ===================================================================== */
/** 后端并发实现中，对返回结构做一次兜底（缺字段/空数组都不影响渲染） */
function normalizeResult(data: TranscriptImportResult): TranscriptImportResult {
  return {
    rowCount: Number(data.rowCount ?? 0) || 0,
    inserted: Number(data.inserted ?? 0) || 0,
    updated: Number(data.updated ?? 0) || 0,
    skipped: Number(data.skipped ?? 0) || 0,
    unknownCourses: Array.isArray(data.unknownCourses) ? data.unknownCourses.map(String) : [],
    unknownTerms: Array.isArray(data.unknownTerms) ? data.unknownTerms.map(String) : [],
    skippedDetails: Array.isArray(data.skippedDetails) ? data.skippedDetails : []
  }
}

async function confirmImport(): Promise<void> {
  const meta = parsed.value
  if (!meta || importing.value) return
  importError.value = ''
  importResult.value = null

  const rows = form.rows.map(normalizeRow)
  if (!rows.length) {
    importError.value = t('error.empty')
    return
  }
  if (rows.length > MAX_ROWS) {
    importError.value = t('error.tooMany')
    return
  }
  const incomplete = rows.filter((row) => !row.courseCode || !row.termCode).length
  if (incomplete > 0) {
    importError.value = `${incomplete} ${t('error.incomplete')}`
    return
  }

  importing.value = true
  try {
    const response = await importTranscript({ fileName: meta.fileName, rows })
    const data = response?.data
    if (!data) {
      importError.value = t('error.import')
      return
    }
    importResult.value = normalizeResult(data)
  } catch (error) {
    const detail = describeError(error)
    const hint = /404/.test(detail) ? ' ' + t('error.importHint') : ''
    importError.value = `${t('error.import')} ${detail}${hint}`.trim()
  } finally {
    importing.value = false
  }
}

/* =====================================================================
 * 生命周期
 * ===================================================================== */
onMounted(() => {
  document.documentElement.lang = lang.value
})
</script>

<style scoped src="../../../assets/student/transcript.css"></style>

<style scoped>
/* ──────────────────────────────────────────────────────────────
 * 顶栏：与 views/student/dashboard 完全一致。
 * transcript.css 只抽出了旧版成绩单区块的样式，不含 .topnav 系列，
 * 因此这里按 dashboard.css 的规则原样补齐（不改 assets/student/*）。
 * ────────────────────────────────────────────────────────────── */
.sp-root {
  --purple: #0070e0;
  min-height: 100vh;
}

.topnav {
  background: var(--purple);
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 100;
}
.nav-logo {
  color: #fff;
  font-weight: 800;
  font-size: 14px;
  margin-right: 16px;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 8px;
}
.nav-link {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 6px;
}
.nav-link:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}
.nav-sep {
  color: rgba(255, 255, 255, 0.3);
}
.nav-spacer {
  flex: 1;
}
/* transcript.css 里 .user-pill 是旧版浮动的白色胶囊，这里限定在顶栏内还原 dashboard 样式 */
.topnav .user-pill {
  position: static;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.12);
  border: none;
  border-radius: 20px;
  padding: 5px 10px;
  box-shadow: none;
}
.topnav .user-pill .ub {
  cursor: pointer;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #fff;
  border-radius: 20px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
}
.topnav .lang-toggle {
  display: inline-flex;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 20px;
  overflow: hidden;
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.15);
}
.topnav .lang-btn {
  padding: 5px 12px;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.15s;
  letter-spacing: 0.02em;
}
.topnav .lang-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}
.topnav .lang-btn.active {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

/* ──────────────────────────────────────────────────────────────
 * 左侧「原始文件」预览（本轮简化：文件名 + 大小 + 解析摘要）
 * ────────────────────────────────────────────────────────────── */
.preview-file {
  width: 100%;
  padding: 18px;
  color: #e5e7f0;
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 12px;
  line-height: 1.9;
}
.preview-file .pf-name {
  font-size: 13.5px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 10px;
  word-break: break-all;
}
.preview-file .pf-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 4px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.12);
}
.preview-file .pf-row > span:last-child {
  text-align: right;
  word-break: break-all;
}
.preview-file .pf-key {
  color: rgba(255, 255, 255, 0.55);
  flex-shrink: 0;
}
.preview-file .pf-hint {
  margin-top: 14px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  line-height: 1.6;
}

/* ──────────────────────────────────────────────────────────────
 * 提示 / 导入结果（复用旧版色板，不引入新的视觉语言）
 * ────────────────────────────────────────────────────────────── */
.transcript-warning {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 18px;
  padding: 12px 16px;
  border-radius: 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.6;
}
.transcript-warning .icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  margin-top: 1px;
  border-radius: 50%;
  background: #f59e0b;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  display: grid;
  place-items: center;
}
.transcript-warning.is-error {
  background: #fef2f2;
  border-color: #fecaca;
  color: #b91c1c;
}
.transcript-warning.is-error .icon {
  background: #dc2626;
}
.transcript-notes {
  margin-top: 14px;
  font-size: 12px;
  color: var(--text2);
  line-height: 1.8;
}
.transcript-block {
  margin-top: 16px;
}
.transcript-block .tb-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 8px;
}
</style>
