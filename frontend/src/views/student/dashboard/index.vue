<template>
  <div class="sp-root">
    <!-- ── Nav ── -->
    <nav class="topnav">
      <router-link class="nav-logo" to="/student">🎓 <span>{{ t('nav.title') }}</span></router-link>
      <span class="nav-sep">›</span>
      <router-link class="nav-link" to="/student">🏠 <span>{{ t('nav.home') }}</span></router-link>
      <router-link class="nav-link" to="/student/guide">📖 <span>{{ t('nav.guide') }}</span></router-link>
      <div class="nav-spacer"></div>
      <div class="lang-toggle">
        <button
          v-for="item in LANGS"
          :key="item.code"
          class="lang-btn"
          :class="{ active: lang === item.code }"
          @click="setLang(item.code)"
        >
          {{ item.label }}
        </button>
      </div>
      <div class="user-pill" style="margin-left: 12px">
        <span>{{ userLabel }}</span>
        <button class="ub" @click="signOut">{{ t('nav.signout') }}</button>
      </div>
    </nav>

    <!-- ── In-page notice strip（接口失败 / 操作结果都走这里，不只弹 toast） ── -->
    <div v-if="notices.length" class="sp-notices">
      <div v-for="item in notices" :key="item.id" class="notice notice-row" :class="'notice-' + item.level">
        <span>{{ item.text }}</span>
        <button class="notice-close" type="button" @click="dismissNotice(item.id)">×</button>
      </div>
    </div>

    <!-- ── 本页仅学生可用：当前账号未绑定学生信息 ── -->
    <div v-if="unbound" class="main sp-unbound">
      <div class="card" style="text-align: center">
        <h2>{{ t('unbound.title') }}</h2>
        <div class="sub" style="margin-bottom: 22px">{{ t('unbound.desc') }}</div>
        <div style="display: flex; gap: 10px; justify-content: center; flex-wrap: wrap">
          <button class="btn btn-primary" @click="switchStudent">{{ t('unbound.switch') }}</button>
          <button class="btn" @click="loadAll">{{ t('msg.retry') }}</button>
        </div>
      </div>
    </div>

    <template v-else>
      <!-- ── School filter ── -->
      <div class="school-bar">
        <div class="filter-row">
          <span class="label">{{ t('filter.schools') }}</span>
          <span class="chip" :class="{ on: categoryFilter === 'ALL' }" @click="selectCategory('ALL')">
            {{ t('filter.all') }}
          </span>
          <span id="initial-chips">
            <span
              v-for="item in optionState.categories"
              :key="item"
              class="chip initial-chip"
              :class="{ on: categoryFilter === item }"
              :title="item"
              @click="selectCategory(item)"
            >
              <span class="dot" :style="{ color: colorOf(item) }"></span>{{ item }}
            </span>
          </span>
        </div>
        <div class="filter-row subject-row">
          <span id="school-chips">
            <span
              v-for="item in optionState.schools"
              :key="item"
              class="chip"
              :class="{ on: schoolFilter === item }"
              :title="item"
              @click="selectSchool(item)"
            >
              <span class="dot" :style="{ color: colorOf(item) }"></span>{{ item }}
            </span>
          </span>
        </div>
        <div class="chip-sec">
          <span>{{ t('filter.term') }}</span>
          <select id="term-filter" v-model="termFilter" @change="onFilterChange">
            <option value="ALL">{{ t('filter.any') }}</option>
            <option value="Fall">{{ t('filter.fall') }}</option>
            <option value="Spring">{{ t('filter.spring') }}</option>
            <option value="Summer">{{ t('filter.summer') }}</option>
          </select>
          <span>{{ t('filter.level') }}</span>
          <select id="level-filter" v-model="levelFilter" @change="onFilterChange">
            <option value="ALL">{{ t('filter.any') }}</option>
            <option v-for="lv in optionState.levels" :key="lv" :value="String(lv)">{{ lv }}</option>
          </select>
        </div>
      </div>

      <div class="main">
        <!-- ══ Left: course browser + study headway ══ -->
        <div class="left-col">
          <div id="course-browser-panel" class="panel">
            <div class="panel-header">
              <h3>📚 Available Courses</h3>
              <span id="course-count-label">{{ courseCountLabel }}</span>
            </div>
            <div class="panel-body" @scroll="onCourseScroll">
              <input
                id="course-search"
                v-model="searchKeyword"
                class="search-box"
                placeholder="Search course code..."
                @input="onSearchInput"
              />
              <div id="course-list">
                <div v-if="courseError" class="sp-error">{{ courseError }}</div>
                <div v-else-if="courseLoading && !courseRows.length" class="sp-empty">{{ t('msg.loading') }}</div>
                <div v-else-if="!visibleCourseRows.length" class="sp-empty">No courses match the filters</div>
                <div
                  v-for="row in visibleCourseRows"
                  :key="row.code"
                  class="course-item"
                  :class="{ selected: isSelected(row.code) }"
                  :style="isSelected(row.code) ? { borderColor: colorOf(row.code), background: colorOf(row.code) + '15' } : undefined"
                  :title="courseTitle(row)"
                  draggable="true"
                  @click="openCourse(row.code)"
                  @dragstart="onCourseDragStart($event, row)"
                  @dragend="onDragEnd"
                >
                  <span class="ci-code" :style="{ color: colorOf(row.code) }" @click.stop="openCourse(row.code)">
                    {{ row.code }}
                  </span>
                  <span class="ci-title">
                    {{ courseTitle(row) }}
                    <br />
                    <small v-if="orgLabel(row)" style="color: var(--text3); font-weight: 600">{{ orgLabel(row) }}</small>
                  </span>
                  <span class="ci-term">
                    <template v-if="row.credits">{{ row.credits }} units</template>
                    <br />
                    <template v-if="firstOfferedTerm(row)">{{ firstOfferedTerm(row) }}</template>
                    <br />
                    {{ isOutlineAvailable(row) ? 'Outline' : 'No outline' }}
                  </span>
                  <span
                    class="ci-check"
                    :style="isSelected(row.code) ? { background: colorOf(row.code), borderColor: colorOf(row.code), color: '#fff' } : undefined"
                    title="Add / remove from portfolio"
                    @click.stop="toggleCourse(row)"
                  >
                    {{ isSelected(row.code) ? '✓' : '' }}
                  </span>
                </div>
                <div v-if="courseLoading && courseRows.length" class="sp-empty">{{ t('msg.loading') }}</div>
              </div>
            </div>
          </div>

          <div id="study-headway" class="headway-card">
            <h2>Your Study Headway</h2>
            <div class="headway-controls">
              <label>
                Admitted in
                <select id="admission-year-select" v-model="selectedEntryYear" @change="loadHeadway">
                  <option v-for="year in entryYearOptions" :key="year" :value="String(year)">{{ year }}</option>
                </select>
              </label>
              <label>
                Major
                <select id="study-major-select" v-model="selectedMajorId" @change="loadHeadway">
                  <option v-for="item in majors" :key="item.majorId" :value="String(item.majorId)">
                    {{ item.schoolAbbr }} - {{ item.majorNameZh || item.majorNameEn || item.majorCode }}
                  </option>
                </select>
              </label>
            </div>
            <div id="headway-content" :style="{ maxHeight: headwayMaxHeight }">
              <div v-if="headwayError" class="sp-error">{{ headwayError }}</div>
              <div v-else-if="headwayLoading && !headwayModel" class="sp-empty">{{ t('msg.loading') }}</div>
              <template v-else-if="headwayModel">
                <!-- 1. Credit progress -->
                <div class="progress-block">
                  <div class="progress-title">
                    <span>Credit Progress</span>
                    <span>{{ headwayModel.credit.done }}/{{ headwayModel.credit.total }}</span>
                  </div>
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: headwayModel.credit.percent + '%' }">
                      {{ headwayModel.credit.percent }}%
                    </div>
                  </div>
                  <div class="headway-details">
                    <div class="headway-muted">
                      All passed transcript units plus manually added exemption/approved units.
                    </div>
                  </div>
                </div>

                <!-- 2. Major progress -->
                <div class="progress-block">
                  <div class="progress-title">
                    <span>Major Progress</span>
                    <span>{{ headwayModel.major.done }}/{{ headwayModel.major.total }}</span>
                  </div>
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: headwayModel.major.percent + '%' }">
                      {{ headwayModel.major.percent }}%
                    </div>
                  </div>
                  <div class="headway-details">
                    <details open>
                      <summary>Courses up to take</summary>
                      <div class="headway-muted">
                        {{ headwayModel.majorIncomplete ? 'Major task incomplete.' : 'Major task completed.' }}
                      </div>
                      <div style="margin-top: 6px; font-weight: 700">Mandatory:</div>
                      <div v-if="headwayModel.requiredRows.length" class="headway-list">
                        <div
                          v-for="row in headwayModel.requiredRows"
                          :key="'req-' + row.code"
                          class="headway-course"
                          :class="{ done: row.done }"
                        >
                          <span class="hc-code">
                            <a href="#" @click.prevent="openCourse(row.code)">{{ compactCode(row.code) }}</a>
                          </span>
                          <span class="hc-status">{{ row.done ? '✓ completed' : 'to take' }}</span>
                        </div>
                      </div>
                      <div v-else class="headway-muted">
                        Mandatory courses appear satisfied under the current rule.
                      </div>
                      <div style="font-weight: 700; margin-top: 8px">Elective:</div>
                      <div class="headway-list">
                        <span class="headway-pill">
                          {{ Math.round(headwayModel.electiveDone) }}/{{ headwayModel.electiveNeed }} elective units
                        </span>
                        <span v-if="headwayModel.electiveShort > 0" class="headway-muted">
                          Still need about {{ Math.round(headwayModel.electiveShort) }} elective units. Open the
                          major course list and choose courses from the study scheme pool.
                        </span>
                        <span v-else class="headway-muted">Elective unit target appears satisfied.</span>
                      </div>
                      <div v-if="headwayModel.categoryRows.length" style="font-weight: 700; margin-top: 8px">
                        Areas / Streams:
                      </div>
                      <details v-for="cat in headwayModel.categoryRows" :key="'cat-' + cat.label">
                        <summary>{{ cat.label }}: {{ cat.earned }}/{{ cat.required }} units</summary>
                        <div class="headway-list">
                          <div
                            v-for="row in cat.rows"
                            :key="'cat-row-' + cat.label + row.code"
                            class="headway-course"
                            :class="{ done: row.done }"
                          >
                            <span class="hc-code">
                              <a href="#" @click.prevent="openCourse(row.code)">{{ compactCode(row.code) }}</a>
                            </span>
                            <span class="hc-status">{{ row.done ? '✓ completed' : 'to take' }}</span>
                          </div>
                        </div>
                      </details>
                      <div class="headway-muted">{{ headwayModel.ruleLine }}</div>
                    </details>
                  </div>
                </div>

                <!-- 3. U-core progress -->
                <div class="progress-block">
                  <div class="progress-title">
                    <span>U-core Progress</span>
                    <span>{{ headwayModel.ucore.done }}/{{ headwayModel.ucore.total }}</span>
                  </div>
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: headwayModel.ucore.percent + '%' }">
                      {{ headwayModel.ucore.percent }}%
                    </div>
                  </div>
                  <div class="headway-details">
                    <details open>
                      <summary>Courses up to take</summary>
                      <div v-if="headwayModel.ucoreRows.length" class="headway-list">
                        <span
                          v-for="row in headwayModel.ucoreRows"
                          :key="'ucore-' + row.label"
                          class="headway-pill"
                          :style="{ borderColor: row.ok ? '#35a766' : 'var(--border)' }"
                        >
                          {{ row.ok ? '✓' : '✕' }} {{ row.label }} · {{ row.earned }} units
                          <template v-if="row.required > 0"> / {{ row.required }} required</template>
                        </span>
                      </div>
                      <div v-else class="headway-muted">U-core requirements appear satisfied.</div>
                      <div class="headway-muted">
                        Rule: {{ headwayModel.ucoreSource }}. GEW courses are not compulsory for U-core; completed
                        GEW may be declared as free elective up to 6 units.
                      </div>
                    </details>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- ══ Center ══ -->
        <div class="center-col">
          <!-- ── Student Portfolio ── -->
          <div id="section-basket" class="card">
            <div class="card-head">
              <h2>{{ t('card.portfolio') }}</h2>
              <div style="display: flex; gap: 8px; flex-wrap: wrap">
                <button class="btn" :disabled="importing" @click="importTranscript">
                  📥 {{ importing ? t('msg.loading') : 'Import Transcript' }}
                </button>
                <button class="btn" @click="exportPortfolio">📤 Export Excel</button>
                <button class="btn" @click="clearPortfolio">{{ t('gpa.clear') }}</button>
              </div>
            </div>
            <div
              id="basket"
              ref="basketRef"
              class="basket"
              :class="{ 'drop-active': dropActive }"
              @dragover.prevent="onBasketDragOver"
              @dragleave="onBasketDragLeave"
              @drop.prevent="onBasketDrop"
            >
              <div v-if="!basket.length" id="basket-empty" class="basket-empty">
                📥 Drag courses here, click any course on the left, or import your transcript.<br />
                <span style="font-size: 11px">Tip: drag a tag out of this area to remove it.</span>
              </div>
              <span
                v-for="item in basket"
                :key="item.code"
                class="basket-tag"
                :style="{ background: colorOf(item.code) }"
                draggable="true"
                title="Click or drag out of the box to remove"
                @click="removeCourse(item.code)"
                @dragstart="onTagDragStart($event, item.code)"
                @dragend="onDragEnd"
              >
                {{ item.code }} ×
              </span>
            </div>
            <div class="profile-summary">
              <div class="ps-card" @click="jumpTo('section-basket')">
                <div id="ps-courses" class="ps-val">{{ basket.length }}</div>
                <div class="ps-lbl">{{ t('ps.courses') }}</div>
              </div>
              <div class="ps-card" @click="jumpTo('section-basket')">
                <div id="ps-credits" class="ps-val">{{ basketUnits }}</div>
                <div class="ps-lbl">{{ t('ps.credits') }}</div>
              </div>
              <div class="ps-card" @click="jumpTo('section-gpa')">
                <div id="ps-avg" class="ps-val">
                  <span style="font-size: 1.55rem">{{ cgpaText }} / {{ mgpaText }}</span>
                </div>
                <div class="ps-lbl">{{ t('ps.avg') }}</div>
              </div>
              <div class="ps-card" @click="jumpTo('section-skill')">
                <div id="ps-top" class="ps-top-list">
                  <div v-if="!topSkills.length" class="ps-top-row" style="color: var(--text3); font-weight: 600">—</div>
                  <div v-for="(item, index) in topSkills" :key="item.name" class="ps-top-row">
                    <span class="rk">{{ index + 1 }}.</span> {{ item.name }}
                    <span style="color: var(--text3); font-weight: 600">({{ item.score }})</span>
                  </div>
                </div>
                <div class="ps-lbl">{{ t('ps.top') }}</div>
              </div>
            </div>
          </div>

          <!-- ── Grade Trajectory ── -->
          <div id="section-gpa" class="card">
            <div class="card-head">
              <div>
                <h2>{{ t('card.gpa') }}</h2>
                <div class="sub">{{ t('gpa.sub') }}</div>
              </div>
              <div style="display: flex; gap: 6px">
                <button id="clear-grades-btn" class="btn" @click="clearPortfolio">{{ t('gpa.clear') }}</button>
              </div>
            </div>
            <div class="gpa-target-bar">
              <span id="gpa-stats" class="stat">
                <b>{{ t('gpa.cgpa') }}</b>:
                <b style="color: var(--accent)">{{ cgpaText }}</b>
                &nbsp;·&nbsp;
                <b>MGPA</b>:
                <b style="color: var(--accent)">{{ mgpaText }}</b>
                &nbsp;·&nbsp;
                <b>{{ t('gpa.lastsem') }}</b>:
                <b style="color: var(--accent)">{{ latestSemLabel }}</b>
              </span>
              <label style="margin-left: auto">
                <span>{{ t('gpa.target') }}</span>:
                <input
                  id="goal-gpa"
                  type="number"
                  min="0"
                  max="4"
                  step="0.01"
                  :value="goalGpa"
                  @input="onGoalInput"
                />
              </label>
            </div>

            <div class="gpa-grid" ref="gpaGridRef">
              <div class="gpa-left-pane">
                <div class="gpa-add-pane">
                  <div style="font-size: 13px; color: var(--text2); margin-bottom: 8px; font-weight: 700">
                    {{ t('gpa.add') }}
                  </div>
                  <div style="font-size: 12px; color: var(--text3); line-height: 1.45; margin: -2px 0 10px">
                    {{ t('gpa.manualNote') }}
                  </div>
                  <div class="gpa-input">
                    <select id="g-sem" v-model="manualSem" title="Semester">
                      <option v-for="item in SEM_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option>
                    </select>
                    <input
                      id="g-code"
                      v-model="manualCode"
                      list="g-code-list"
                      placeholder="Course code (type or pick)"
                      autocomplete="off"
                      @keydown.enter.prevent="addManualGrade"
                    />
                    <datalist id="g-code-list">
                      <option v-for="row in courseRows" :key="'dl-' + row.code" :value="row.code">
                        {{ courseTitle(row).slice(0, 50) }}
                      </option>
                    </datalist>
                    <input
                      id="g-units"
                      type="number"
                      min="0"
                      max="9"
                      step="0.5"
                      :value="manualUnits"
                      title="Units"
                      @input="onUnitsInput"
                    />
                    <select id="g-grade" v-model="manualGrade" title="Letter grade">
                      <option v-for="item in GRADE_ORDER_DESC" :key="item" :value="item">{{ item }}</option>
                      <option value="P">P</option>
                    </select>
                  </div>
                  <button
                    id="add-grade-btn"
                    class="btn btn-primary"
                    style="width: 100%; margin-bottom: 14px; justify-content: center"
                    @click="addManualGrade"
                  >
                    {{ t('gpa.addbtn') }}
                  </button>
                </div>
                <div class="gpa-chart-pane">
                  <div class="canvas-wrap">
                    <div class="gpa-chart-inner">
                      <div id="gpa-chart" ref="gpaChartRef"></div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="gpa-recorded-pane">
                <div style="font-size: 13px; color: var(--text2); margin-bottom: 8px; font-weight: 700">
                  {{ t('gpa.recorded') }}
                </div>
                <div id="gpa-list" class="gpa-list">
                  <div
                    v-if="!gpaListRows.length"
                    style="text-align: center; color: var(--text3); padding: 14px; font-size: 12px"
                  >
                    No grades yet. Import your transcript or add a course above.
                  </div>
                  <div v-for="row in gpaListRows" :key="row.key" class="row">
                    <span><b>{{ row.label }}</b> &nbsp; {{ row.code }}</span>
                    <span>
                      <span class="rg">{{ row.grade }}</span>
                      <span
                        v-if="row.manual"
                        class="rx"
                        role="button"
                        title="Remove"
                        @click="removeManualGrade(row.index)"
                        >×</span
                      >
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- ── School Competency Pool ── -->
          <div id="section-competency-pool" class="card">
            <div class="card-head">
              <div>
                <h2>{{ t('competency.title') }}</h2>
                <div class="sub">{{ t('competency.sub') }}</div>
              </div>
              <button class="btn" @click="jumpTo('section-basket')">Portfolio</button>
            </div>
            <div id="competency-tabs" class="competency-toolbar">
              <button
                v-for="item in skillGroups"
                :key="'tab-' + item.abbr"
                class="competency-tab"
                :class="{ on: activeSchoolAbbr === item.abbr }"
                :title="item.name"
                @click="selectCompetencySchool(item.abbr)"
              >
                {{ item.abbr }}
              </button>
            </div>
            <div class="competency-panel">
              <div id="competency-cloud" class="competency-cloud">
                <div v-if="skillError" class="sp-error">{{ skillError }}</div>
                <div v-else-if="!competencyChips.length" style="color: var(--text3); font-weight: 700">
                  {{ t('pool.empty') }}
                </div>
                <button
                  v-for="(item, index) in competencyChips"
                  :key="'chip-' + item.name"
                  class="competency-chip"
                  :class="{ on: activeChip === item.name }"
                  :title="item.rationale"
                  @click="selectCompetencyChip(item.name)"
                >
                  <span class="competency-rank">{{ pad2(index + 1) }}.</span>{{ item.name }}
                </button>
              </div>
              <div id="competency-detail" class="competency-detail">
                <div class="competency-detail-head">
                  <h3>{{ activeChip || '—' }}</h3>
                  <p>{{ activeChipRationale || t('competency.sub') }}</p>
                  <p style="margin-top: 8px; font-weight: 800; color: var(--accent)">{{ activeSchoolAbbr }}</p>
                </div>
                <div class="competency-course-list">
                  <div v-if="poolError" class="sp-empty">{{ poolError }}</div>
                  <div v-else-if="poolLoading" class="sp-empty">{{ t('msg.loading') }}</div>
                  <div v-else-if="!poolRows.length" class="sp-empty">{{ t('pool.empty') }}</div>
                  <a
                    v-for="(row, index) in poolRows"
                    :key="'pool-' + row.code"
                    class="competency-course"
                    :href="courseHref(row.code)"
                    :title="row.rationale"
                    @click.prevent="openCourse(row.code)"
                  >
                    <div>
                      <div class="competency-course-code">
                        <span class="competency-rank">{{ pad2(index + 1) }}.</span>{{ compactCode(row.code) }}
                        <span v-if="isTaken(row.code)" class="taken-badge">taken</span>
                      </div>
                      <div class="competency-course-title">{{ row.title || t('pool.noTitle') }}</div>
                    </div>
                    <div class="competency-score">{{ row.score.toFixed(1) }}</div>
                  </a>
                </div>
              </div>
            </div>
          </div>

          <!-- ── Internship preparation（数据源为静态抓取文件，新架构无对应接口） ── -->
          <div id="section-intern-prep" class="card intern-card">
            <div class="card-head">
              <div>
                <h2 id="intern-title">Prepare for your intern</h2>
                <div id="intern-source" class="sub">data source: https://www.shixiseng.com, through 2026.7.1</div>
              </div>
            </div>
            <div class="notice">{{ t('intern.unavailable') }}</div>
          </div>

          <!-- ── Skill Ability Profile ── -->
          <div id="section-skill" class="card">
            <div class="card-head">
              <div>
                <h2 class="tip-trigger">
                  <span>{{ t('card.skill') }}</span>
                  <span class="tip-body">
                    <p>
                      <b>🎯 Major Skills</b> — 9 specialized competencies of your selected category. Example:
                      Accounting tracks Financial Reporting, Auditing, Taxation, etc. Shows your
                      <b>depth in your major</b>.
                    </p>
                    <p>
                      <b>🌐 General Skills (9 universal)</b> — <b>PK</b> = average of your major skills (overall
                      depth). <b>QA</b> Quantitative · <b>DI</b> Data · <b>CM</b> Communication · <b>IP</b>
                      Innovation · <b>ER</b> Ethics · <b>TL</b> Teamwork · <b>IC</b> Intercultural · <b>SL</b>
                      Self-directed.
                    </p>
                    <p>
                      <b>How scores are computed</b> — each course's outcome / syllabus is matched against
                      keyword lists for every skill dimension on the server. Red mark on each row = the selected
                      category's average (only counting courses where the skill &gt; 0).
                    </p>
                  </span>
                </h2>
              </div>
              <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
                <span style="font-size: 13px; color: var(--text2); font-weight: 600">{{ t('skill.major.dept') }}</span>
                <select
                  id="major-dept-select"
                  v-model="activeSkillCategory"
                  class="adv-select"
                  style="width: auto; margin: 0; padding: 7px 12px; font-size: 13px"
                  @change="onSkillCategoryChange"
                >
                  <option v-for="item in skillGroups" :key="'sk-' + item.abbr" :value="item.name">
                    {{ item.name }}
                  </option>
                </select>
              </div>
            </div>

            <div class="chart-block">
              <h3>
                <span>
                  🎯 <span>{{ t('skill.major') }}</span>
                  <span id="major-dept-label" style="font-weight: 500; color: var(--text3); font-size: 12px">
                    ({{ activeSchoolAbbr }})
                  </span>
                </span>
                <span class="h3-actions">
                  <span class="sort-toggle">
                    <button
                      :class="{ on: majorSort === 'desc' }"
                      title="High → Low"
                      @click="setSkillSort('major', 'desc')"
                    >
                      ▼
                    </button>
                    <button :class="{ on: majorSort === 'asc' }" title="Low → High" @click="setSkillSort('major', 'asc')">
                      ▲
                    </button>
                  </span>
                </span>
              </h3>
              <div class="csub">{{ t('skill.major.sub') }}</div>
              <div id="major-score-bars">
                <div v-if="skillError" class="sp-error">{{ skillError }}</div>
                <div v-else-if="!majorSkillRows.length" class="sp-score-empty">{{ t('skill.empty') }}</div>
                <div v-for="row in majorSkillRows" :key="'m-' + row.name" class="score-row" :title="scoreTitle(row)">
                  <span class="score-lbl">{{ row.name }}</span>
                  <div class="score-bar-bg">
                    <div class="score-bar-mark" data-l="25" style="left: 25%"></div>
                    <div class="score-bar-mark" data-l="50" style="left: 50%"></div>
                    <div class="score-bar-mark" data-l="75" style="left: 75%"></div>
                    <div
                      v-if="row.avg > 0"
                      class="score-bar-avg"
                      :style="{ left: row.avg + '%' }"
                      :title="'Category avg: ' + row.avg"
                    ></div>
                    <div
                      class="score-bar-dot"
                      :style="{ left: row.score + '%' }"
                      :title="'Your score: ' + row.score"
                    ></div>
                  </div>
                </div>
              </div>
            </div>

            <div class="chart-block">
              <h3>
                <span>🌐 <span>{{ t('skill.gen') }}</span></span>
                <span class="h3-actions">
                  <span class="sort-toggle">
                    <button :class="{ on: genSort === 'desc' }" title="High → Low" @click="setSkillSort('gen', 'desc')">
                      ▼
                    </button>
                    <button :class="{ on: genSort === 'asc' }" title="Low → High" @click="setSkillSort('gen', 'asc')">
                      ▲
                    </button>
                  </span>
                </span>
              </h3>
              <div class="csub">{{ t('skill.gen.sub') }}</div>
              <div id="gen-score-bars">
                <div v-if="!generalSkillRows.length" class="sp-score-empty">{{ t('skill.empty') }}</div>
                <div v-for="row in generalSkillRows" :key="'g-' + row.name" class="score-row" :title="scoreTitle(row)">
                  <span class="score-lbl">{{ row.name }}</span>
                  <div class="score-bar-bg">
                    <div class="score-bar-mark" data-l="25" style="left: 25%"></div>
                    <div class="score-bar-mark" data-l="50" style="left: 50%"></div>
                    <div class="score-bar-mark" data-l="75" style="left: 75%"></div>
                    <div
                      v-if="row.avg > 0"
                      class="score-bar-avg"
                      :style="{ left: row.avg + '%' }"
                      :title="'Category avg: ' + row.avg"
                    ></div>
                    <div
                      class="score-bar-dot"
                      :style="{ left: row.score + '%' }"
                      :title="'Your score: ' + row.score"
                    ></div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="insightText" id="portfolio-insight" class="insight">
              <div id="insight-text" v-html="insightText"></div>
              <div v-if="insightGapText" id="insight-gap" class="gap" v-html="insightGapText"></div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts" name="StudentDashboard">
import * as echarts from 'echarts'
import useUserStore from '@/store/modules/user'
import { getCourseOptions, listCourse } from '@/api/education/course'
import { getCurriculum, getCurriculumByMajor, listCurriculum, listMajor } from '@/api/education/curriculum'
import { getMyProgress, getMyStudent, listMyRecord } from '@/api/education/enrollment'
import { getCompetencyPool, getDashboard, getSkillProfile } from '@/api/education/analytics'
import type {
  CourseCompetency,
  CreditCategoryRow,
  CreditProgress,
  DashboardStat,
  EduCourse,
  EduCourseOption,
  EduCourseRecord,
  EduCurriculum,
  EduCurriculumItem,
  EduMajor,
  EduStudent,
  GpaTrendPoint,
  SkillProfileItem
} from '@/types/education'

/* =====================================================================
 * 类型
 * ===================================================================== */
type Lang = 'en' | 'zh-CN' | 'zh-TW'
type SortDir = 'asc' | 'desc'
type NoticeLevel = 'error' | 'info' | 'success'

interface LangOption {
  code: Lang
  label: string
}

interface NoticeItem {
  id: number
  level: NoticeLevel
  text: string
}

interface BasketEntry {
  code: string
  title: string
  units: number
  school: string
  category: string
  offered: string
  level: number
  outline: string
}

interface ManualGrade {
  sem: string
  code: string
  grade: string
  credit: number
}

interface GpaListRow {
  key: string
  label: string
  code: string
  grade: string
  manual: boolean
  index: number
}

interface SemesterEntry {
  key: string
  label: string
  sort: number
  gpa: number | null
}

interface CourseDot {
  x: number
  y: number
  label: string
}

interface GpaSeries {
  sems: SemesterEntry[]
  dots: CourseDot[]
  termPoints: Array<[number, number]>
}

interface SkillGroup {
  abbr: string
  name: string
}

interface SkillRow {
  name: string
  score: number
  avg: number
  courseCount: number
  rationale: string
  category: string
}

interface PoolRow {
  code: string
  title: string
  score: number
  level: string
  rationale: string
}

interface HeadwayRequirement {
  code: string
  title: string
  done: boolean
}

interface HeadwayCategory {
  label: string
  earned: number
  required: number
  rows: HeadwayRequirement[]
}

interface HeadwayProgress {
  done: number
  total: number
  percent: number
}

interface HeadwayUcoreRow {
  label: string
  earned: number
  required: number
  ok: boolean
}

interface HeadwayModel {
  credit: HeadwayProgress
  major: HeadwayProgress
  ucore: HeadwayProgress
  requiredRows: HeadwayRequirement[]
  categoryRows: HeadwayCategory[]
  ucoreRows: HeadwayUcoreRow[]
  ucoreSource: string
  electiveDone: number
  electiveNeed: number
  electiveShort: number
  majorIncomplete: boolean
  ruleLine: string
}

/* =====================================================================
 * 常量（与原页面一致）
 * ===================================================================== */
const LANGS: LangOption[] = [
  { code: 'en', label: 'EN' },
  { code: 'zh-CN', label: '简中' },
  { code: 'zh-TW', label: '繁中' }
]

/** A 4.0 · A- 3.7 · B+ 3.3 · B 3.0 · B- 2.7 · C+ 2.4 · C 2.0 · C- 1.7 · D+ 1.3 · D 1.0 · F 0 */
const GRADE_VAL: Record<string, number> = {
  A: 4.0,
  'A-': 3.7,
  'B+': 3.3,
  B: 3.0,
  'B-': 2.7,
  'C+': 2.4,
  C: 2.0,
  'C-': 1.7,
  'D+': 1.3,
  D: 1.0,
  F: 0.0
}
const GRADE_ORDER_DESC: string[] = ['A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 'D+', 'D', 'F']

/** 原页面的手动录入学期限定列表（F23 … S27） */
const SEM_OPTIONS: Array<{ value: string; label: string }> = [
  { value: 'F23', label: '23-24 Term 1' },
  { value: 'S24', label: '23-24 Term 2' },
  { value: 'SUM24', label: 'SUM 24' },
  { value: 'F24', label: '24-25 Term 1' },
  { value: 'S25', label: '24-25 Term 2' },
  { value: 'SUM25', label: 'SUM 25' },
  { value: 'F25', label: '25-26 Term 1' },
  { value: 'S26', label: '25-26 Term 2' },
  { value: 'SUM26', label: 'SUM 26' },
  { value: 'F26', label: '26-27 Term 1' },
  { value: 'S27', label: '26-27 Term 2' }
]

/** 学院全称 → 缩写（用于能力池 schoolAbbr 与 tab 文案） */
const SCHOOL_ABBR: Record<string, string> = {
  'School of Data Science': 'SDS',
  'School of Management and Economics': 'SME',
  'School of Science and Engineering': 'SSE',
  'School of Humanities and Social Science': 'HSS',
  'School of Medicine': 'MED',
  'School of Music': 'MUS',
  'School of Artificial Intelligence': 'SAI'
}

const CHIP_PALETTE: string[] = [
  '#2563eb',
  '#0891b2',
  '#16a34a',
  '#dc2626',
  '#9333ea',
  '#ea580c',
  '#1e40af',
  '#475569',
  '#be185d',
  '#0f766e'
]

const LS_LANG = 'cuhksz_lang'
const LS_PORTFOLIO = 'cuhksz_portfolio'

/** i18n 字典：逐字沿用 legacy dashboard.html 的 I18N，另补少量新架构文案 */
const I18N: Record<Lang, Record<string, string>> = {
  en: {
    'nav.title': 'Study Dashboard',
    'nav.home': 'Home',
    'nav.guide': 'Guide',
    'nav.signout': 'Sign out',
    'filter.schools': '🗂️ Course Category',
    'filter.all': 'All',
    'filter.term': 'Term:',
    'filter.level': 'Level:',
    'filter.any': 'Any',
    'filter.fall': 'Fall',
    'filter.spring': 'Spring',
    'filter.summer': 'Summer',
    'card.portfolio': '🎒 Student Portfolio',
    'card.skill': '📊 Skill Ability Profile',
    'card.gpa': '📈 Grade Trajectory',
    'card.advisor': '🎯 Skill Driven Recommendation',
    'card.matrix': '🔍 Skill Matrix',
    'card.matrix.sub': 'All courses by all skills — click any column header to sort',
    'ps.courses': '📚 Courses',
    'ps.credits': '⭐ Credits',
    'ps.avg': '🎓 CGPA / MGPA',
    'ps.top': '🏆 Top 3 Strengths',
    'gpa.clear': 'Clear',
    'gpa.sub': 'Track your letter grades, semester GPA, current CGPA, and goal GPA over time',
    'gpa.add': 'Add a grade',
    'gpa.manualNote':
      'Enter a course manually with its semester, units, and grade. Letter grades update the GPA graph; P records a passed course in the portfolio only.',
    'gpa.recorded': 'Recorded grades (term order)',
    'gpa.addbtn': '＋ Add Grade',
    'gpa.target': '🎯 Target CGPA',
    'gpa.cgpa': 'CGPA',
    'gpa.lastsem': 'Latest sem',
    'competency.title': 'School Competency Pool',
    'competency.sub': 'Click a competency to see highly related courses ranked by relevance.',
    'skill.major': 'Major Skills',
    'skill.gen': 'General Skills',
    'skill.major.dept': 'Major dept:',
    'skill.major.sub':
      '9 specialized dimensions for the selected category · gray dot = your score · red mark = category average',
    'skill.gen.sub': '9 cross-disciplinary competencies · PK is auto-computed as the average of your major skills',
    'advisor.sub':
      'Pick a target skill — we’ll rank courses that build it most efficiently (filtered by your category + term selections)',
    'msg.loading': 'Loading…',
    'msg.retry': 'Retry',
    'msg.loadError': 'Data could not be loaded. Please retry later.',
    'unbound.title': 'This page is for students',
    'unbound.desc':
      'Your account is not linked to a student record, so the study dashboard has nothing to show. Sign in with a student account (for example stu_chen) to see courses, credit progress and skill profile.',
    'unbound.switch': 'Switch to a student account',
    'courses.error': 'Courses could not be loaded from the API.',
    'headway.error': 'Study headway could not be loaded for this major / entry year.',
    'skill.empty': 'Add courses to your portfolio to see your skill profile.',
    'skill.error': 'Skill profile could not be loaded.',
    'pool.empty': 'No competency pool data found.',
    'pool.forbidden': 'The competency pool is not available for your account (missing permission).',
    'pool.noTitle': 'Course title unavailable',
    'basket.imported': 'Imported courses from your transcript:',
    'basket.importError': 'Transcript import failed.',
    'basket.emptyList': 'Add courses to your portfolio first.',
    'intern.unavailable':
      'This board was fed by a static scraped dataset (shixiseng, snapshot 2026.7.1) in the prototype. The new Java API does not serve internship postings yet, so the panel is shown as a placeholder instead of fabricating data.',
    'settings.saveError': 'Settings could not be saved to this browser.'
  },
  'zh-CN': {
    'nav.title': '学习仪表盘',
    'nav.home': '首页',
    'nav.guide': '指南',
    'nav.signout': '登出',
    'filter.schools': '🗂️ 课程类别',
    'filter.all': '全部',
    'filter.term': '学期:',
    'filter.level': '年级:',
    'filter.any': '任意',
    'filter.fall': '秋季',
    'filter.spring': '春季',
    'filter.summer': '夏季',
    'card.portfolio': '🎒 学生档案',
    'card.skill': '📊 技能能力剖面',
    'card.gpa': '📈 成绩轨迹',
    'card.advisor': '🎯 技能驱动推荐',
    'card.matrix': '🔍 技能矩阵',
    'card.matrix.sub': '所有课程 × 所有技能 — 点击任意列标题排序',
    'ps.courses': '📚 课程数',
    'ps.credits': '⭐ 学分',
    'ps.avg': '🎓 CGPA / MGPA',
    'ps.top': '🏆 三大优势',
    'gpa.clear': '清空',
    'gpa.sub': '追踪字母成绩、学期 GPA、当前 CGPA 和目标 GPA',
    'gpa.add': '添加成绩',
    'gpa.manualNote':
      '手动输入课程、学期、学分和成绩。字母成绩会更新 GPA 图；P 只记录已通过课程到 portfolio。',
    'gpa.recorded': '已记录成绩（学期顺序）',
    'gpa.addbtn': '＋ 添加',
    'gpa.target': '🎯 目标 CGPA',
    'gpa.cgpa': 'CGPA',
    'gpa.lastsem': '最近学期',
    'competency.title': '学院能力池',
    'competency.sub': '点击一项能力，查看按相关性排序的课程。',
    'skill.major': '专业技能',
    'skill.gen': '通用技能',
    'skill.major.dept': '专业方向:',
    'skill.major.sub': '9 个选定类别的专业维度 · 灰点 = 你的得分 · 红线 = 类别均值',
    'skill.gen.sub': '9 项跨学科能力 · PK 自动等于你专业技能的平均值',
    'advisor.sub': '选择目标技能 — 系统按构建效率排序课程（受类别 + 学期筛选影响）',
    'msg.loading': '加载中…',
    'msg.retry': '重试',
    'msg.loadError': '数据加载失败，请稍后重试。',
    'unbound.title': '本页面仅限学生使用',
    'unbound.desc':
      '当前账号未绑定学生信息，学习主控台没有可展示的数据。请使用学生账号（例如 stu_chen）登录后查看课程、学分进度与技能画像。',
    'unbound.switch': '换学生账号登录',
    'courses.error': '课程列表加载失败。',
    'headway.error': '该专业 / 入学年份的学习进度加载失败。',
    'skill.empty': '先把课程加入 portfolio，才能看到技能画像。',
    'skill.error': '技能画像加载失败。',
    'pool.empty': '没有找到能力池数据。',
    'pool.forbidden': '当前账号没有查看学院能力池的权限。',
    'pool.noTitle': '课程标题待补全',
    'basket.imported': '已从成绩单导入课程：',
    'basket.importError': '成绩单导入失败。',
    'basket.emptyList': '请先把课程加入 portfolio。',
    'intern.unavailable':
      '原型里这个板块由静态抓取数据（实习僧，快照 2026.7.1）驱动。新架构的 Java API 暂未提供实习岗位数据，所以这里只保留占位说明，不编造内容。',
    'settings.saveError': '当前浏览器无法保存设置。'
  },
  'zh-TW': {
    'nav.title': '學習儀表板',
    'nav.home': '首頁',
    'nav.guide': '指南',
    'nav.signout': '登出',
    'filter.schools': '🗂️ 課程類別',
    'filter.all': '全部',
    'filter.term': '學期:',
    'filter.level': '年級:',
    'filter.any': '任意',
    'filter.fall': '秋季',
    'filter.spring': '春季',
    'filter.summer': '夏季',
    'card.portfolio': '🎒 學生檔案',
    'card.skill': '📊 技能能力剖面',
    'card.gpa': '📈 成績軌跡',
    'card.advisor': '🎯 技能驅動推薦',
    'card.matrix': '🔍 技能矩陣',
    'card.matrix.sub': '所有課程 × 所有技能 — 點擊任意列標題排序',
    'ps.courses': '📚 課程數',
    'ps.credits': '⭐ 學分',
    'ps.avg': '🎓 CGPA / MGPA',
    'ps.top': '🏆 三大優勢',
    'gpa.clear': '清空',
    'gpa.sub': '追蹤字母成績、學期 GPA、當前 CGPA 和目標 GPA',
    'gpa.add': '新增成績',
    'gpa.manualNote':
      '手動輸入課程、學期、學分和成績。字母成績會更新 GPA 圖；P 只記錄已通過課程到 portfolio。',
    'gpa.recorded': '已記錄成績（學期順序）',
    'gpa.addbtn': '＋ 新增',
    'gpa.target': '🎯 目標 CGPA',
    'gpa.cgpa': 'CGPA',
    'gpa.lastsem': '最近學期',
    'competency.title': '學院能力池',
    'competency.sub': '點擊一項能力，查看按相關性排序的課程。',
    'skill.major': '專業技能',
    'skill.gen': '通用技能',
    'skill.major.dept': '專業方向:',
    'skill.major.sub': '9 個所選類別的專業維度 · 灰點 = 你的得分 · 紅線 = 類別均值',
    'skill.gen.sub': '9 項跨學科能力 · PK 自動等於你專業技能的平均值',
    'advisor.sub': '選擇目標技能 — 系統按建構效率排序課程（受類別 + 學期篩選影響）',
    'msg.loading': '載入中…',
    'msg.retry': '重試',
    'msg.loadError': '資料載入失敗，請稍後重試。',
    'unbound.title': '本頁面僅限學生使用',
    'unbound.desc':
      '目前帳號未綁定學生資訊，學習主控台沒有可顯示的資料。請使用學生帳號（例如 stu_chen）登入後查看課程、學分進度與技能畫像。',
    'unbound.switch': '切換學生帳號登入',
    'courses.error': '課程列表載入失敗。',
    'headway.error': '該專業 / 入學年份的學習進度載入失敗。',
    'skill.empty': '先把課程加入 portfolio，才能看到技能畫像。',
    'skill.error': '技能畫像載入失敗。',
    'pool.empty': '沒有找到能力池資料。',
    'pool.forbidden': '目前帳號沒有查看學院能力池的權限。',
    'pool.noTitle': '課程標題待補全',
    'basket.imported': '已從成績單匯入課程：',
    'basket.importError': '成績單匯入失敗。',
    'basket.emptyList': '請先把課程加入 portfolio。',
    'intern.unavailable':
      '原型裡這個板塊由靜態抓取資料（實習僧，快照 2026.7.1）驅動。新架構的 Java API 暫未提供實習職缺資料，所以這裡只保留佔位說明，不編造內容。',
    'settings.saveError': '目前瀏覽器無法儲存設定。'
  }
}

/* =====================================================================
 * 语言
 * ===================================================================== */
function readLang(): Lang {
  try {
    const saved = localStorage.getItem(LS_LANG)
    if (saved === 'en' || saved === 'zh-CN' || saved === 'zh-TW') return saved
  } catch {
    /* ignore */
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
  } catch {
    pushNotice('info', t('settings.saveError'))
  }
  document.documentElement.lang = next
}

/* =====================================================================
 * 全局状态
 * ===================================================================== */
const router = useRouter()
const userStore = useUserStore()

const loading = ref<boolean>(false)
const unbound = ref<boolean>(false)
const notices = ref<NoticeItem[]>([])
let noticeSeq = 0

const student = ref<EduStudent | null>(null)
const progress = ref<CreditProgress | null>(null)
const dashboard = ref<DashboardStat | null>(null)
const records = ref<EduCourseRecord[]>([])
const majors = ref<EduMajor[]>([])
const curricula = ref<EduCurriculum[]>([])
const optionState = ref<EduCourseOption>({ schools: [], categories: [], levels: [] })
const skillProfile = ref<SkillProfileItem[]>([])

/* 课程浏览 */
const courseRows = ref<EduCourse[]>([])
const courseTotal = ref<number>(0)
const coursePage = ref<number>(1)
const courseLoading = ref<boolean>(false)
const courseError = ref<string>('')
const searchKeyword = ref<string>('')
const categoryFilter = ref<string>('ALL')
const schoolFilter = ref<string>('ALL')
const termFilter = ref<string>('ALL')
const levelFilter = ref<string>('ALL')
const COURSE_PAGE_SIZE = 60
let searchTimer: ReturnType<typeof setTimeout> | null = null

/* Study Headway */
const selectedMajorId = ref<string>('')
const selectedEntryYear = ref<string>(String(new Date().getFullYear()))
const headwayModel = ref<HeadwayModel | null>(null)
const resolvedCurriculum = ref<EduCurriculum | null>(null)
const resolvedEntryYear = ref<number>(0)
const headwayLoading = ref<boolean>(false)
const headwayError = ref<string>('')
const headwayMaxHeight = ref<string>('')

/* Portfolio / basket */
const basket = ref<BasketEntry[]>([])
const manualGrades = ref<ManualGrade[]>([])
const basketRef = ref<HTMLDivElement | null>(null)
const dropActive = ref<boolean>(false)
const importing = ref<boolean>(false)
let dragCode: string | null = null
let dragSource: 'list' | 'basket' | null = null

/* GPA */
const gpaChartRef = ref<HTMLDivElement | null>(null)
const gpaGridRef = ref<HTMLDivElement | null>(null)
let gpaChart: ReturnType<typeof echarts.init> | null = null
const goalGpa = ref<number>(3.5)
const manualSem = ref<string>('S25')
const manualCode = ref<string>('')
const manualUnits = ref<number>(3)
const manualGrade = ref<string>('A')

/* 能力池 / 技能 */
const activeSkillCategory = ref<string>('')
const activeSchoolAbbr = ref<string>('')
const activeChip = ref<string>('')
const poolRows = ref<PoolRow[]>([])
const poolLoading = ref<boolean>(false)
const poolError = ref<string>('')
const skillError = ref<string>('')
const majorSort = ref<SortDir>('desc')
const genSort = ref<SortDir>('desc')

/* =====================================================================
 * 工具函数
 * ===================================================================== */
function pushNotice(level: NoticeLevel, text: string): void {
  if (!text) return
  noticeSeq += 1
  notices.value = [...notices.value, { id: noticeSeq, level, text }]
}

function dismissNotice(id: number): void {
  notices.value = notices.value.filter((item) => item.id !== id)
}

/** 后端拒绝值可能是 Error，也可能只是字符串（403 → 'error'） */
function describeError(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) return error.message
  if (typeof error === 'string' && error && error !== 'error') return error
  return fallback
}

function normalizeCode(code?: string | null): string {
  return String(code ?? '')
    .trim()
    .toUpperCase()
    .replace(/\s+/g, ' ')
    .replace(/^([A-Z]+)\s*(\d)/, '$1 $2')
}

function compactCode(code?: string | null): string {
  return normalizeCode(code).replace(/\s+/g, '')
}

function pad2(value: number): string {
  return value < 10 ? '0' + value : String(value)
}

function colorOf(key?: string | null): string {
  let hash = 0
  const text = String(key ?? '')
  for (let i = 0; i < text.length; i += 1) hash = (hash * 31 + text.charCodeAt(i)) % 9973
  return CHIP_PALETTE[hash % CHIP_PALETTE.length]
}

function escapeHtml(value: unknown): string {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/** F23 / S24 / SUM24 / 2023-24-1 → 统一的学期键 */
function semKeyFromTermCode(termCode?: string | null): string {
  const raw = String(termCode ?? '').trim().toUpperCase()
  if (!raw) return ''
  const legacy = raw.match(/^(F|S|SUM)\s*(\d{2})$/)
  if (legacy) return legacy[1] + legacy[2]
  const modern = raw.match(/^(\d{4})-(\d{2})-(\d+)$/)
  if (modern) {
    const nextYear = Number(modern[2])
    if (modern[3] === '3') return 'SUM' + nextYear
    if (modern[3] === '2') return 'S' + nextYear
    return 'F' + modern[1].slice(2)
  }
  const named = raw.match(/^(\d{4})-(\d{2})\s*(?:TERM|T)\s*(\d)$/)
  if (named) {
    const nextYear = Number(named[2])
    return named[3] === '2' ? 'S' + nextYear : 'F' + named[1].slice(2)
  }
  return raw
}

function semSortValue(sem: string, fallback = 0): number {
  const raw = String(sem ?? '').trim().toUpperCase()
  let match = raw.match(/^F\s*(\d{2})$/)
  if (match) return Number(match[1]) * 3
  match = raw.match(/^S\s*(\d{2})$/)
  if (match) return (Number(match[1]) - 1) * 3 + 1
  match = raw.match(/^SUM\s*(\d{2})$/)
  if (match) return (Number(match[1]) - 1) * 3 + 2
  return 100000 + fallback
}

function semDisplayLabel(sem: string): string {
  const raw = String(sem ?? '').trim().toUpperCase()
  let match = raw.match(/^F(\d{2})$/)
  if (match) return match[1] + '-' + pad2((Number(match[1]) + 1) % 100) + ' Term 1'
  match = raw.match(/^S(\d{2})$/)
  if (match) return pad2((Number(match[1]) + 99) % 100) + '-' + match[1] + ' Term 2'
  match = raw.match(/^SUM\s*(\d{2})$/)
  if (match) return 'SUM ' + match[1]
  return raw
}

function courseTitle(course: EduCourse): string {
  const zh = (course.titleZh ?? '').trim()
  const en = (course.titleEn ?? '').trim()
  if (lang.value === 'en') return en || zh || (course.code ?? '')
  return zh || en || (course.code ?? '')
}

function orgLabel(course: EduCourse): string {
  return (course.academicOrg ?? '').trim() || (course.school ?? '').trim()
}

function firstOfferedTerm(course: EduCourse): string {
  const lines = String(course.offeredTerms ?? '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
  return lines.length ? lines[lines.length - 1] : ''
}

function isOutlineAvailable(course: EduCourse): boolean {
  return (course.outlineStatus ?? '') === 'saved_outline'
}

function termMatches(course: EduCourse): boolean {
  if (termFilter.value === 'ALL') return true
  const lines = String(course.offeredTerms ?? '')
    .split(/\r?\n/)
    .filter(Boolean)
  if (!lines.length) return true
  const pattern = termFilter.value === 'Fall' ? /Term\s*1/i : termFilter.value === 'Spring' ? /Term\s*2/i : /Summer/i
  return lines.some((line) => pattern.test(line))
}

function courseHref(code: string): string {
  return '/student/course/' + encodeURIComponent(normalizeCode(code))
}

function openCourse(code?: string | null): void {
  const normalized = normalizeCode(code)
  if (!normalized) return
  router.push(courseHref(normalized)).catch(() => undefined)
}

function jumpTo(id: string): void {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/* =====================================================================
 * 计算属性
 * ===================================================================== */
const userLabel = computed<string>(() => {
  const info = student.value
  if (!info) return loading.value ? '…' : 'Student'
  const number = (info.studentNo ?? '').trim()
  const name = (info.name ?? '').trim()
  if (number && name) return number + ' · ' + name
  return number || name || 'Student'
})

const visibleCourseRows = computed<EduCourse[]>(() =>
  termFilter.value === 'ALL' ? courseRows.value : courseRows.value.filter(termMatches)
)

const courseCountLabel = computed<string>(() => {
  const count = termFilter.value === 'ALL' ? courseTotal.value : visibleCourseRows.value.length
  return count + ' courses'
})

const basketUnits = computed<string>(() => {
  const total = basket.value.reduce((sum, item) => sum + (Number(item.units) || 0), 0)
  return String(Math.round(total * 10) / 10)
})

const cgpaValue = computed<number | null>(() => {
  const fromDashboard = dashboard.value?.gpa
  if (typeof fromDashboard === 'number' && Number.isFinite(fromDashboard)) return fromDashboard
  const fromStudent = student.value?.cumGpa
  if (typeof fromStudent === 'number' && Number.isFinite(fromStudent)) return fromStudent
  return null
})

const cgpaText = computed<string>(() => (cgpaValue.value == null ? '—' : cgpaValue.value.toFixed(2)))

/** MGPA = 专业类课程按学分加权的绩点（无数据时显示 —） */
const mgpaValue = computed<number | null>(() => {
  let points = 0
  let units = 0
  records.value.forEach((record) => {
    const grade = GRADE_VAL[String(record.grade ?? '').toUpperCase()]
    if (grade == null) return
    if ((record.category ?? '') !== 'Major') return
    const unit = Number(record.units ?? 0) || 0
    if (unit <= 0) return
    points += grade * unit
    units += unit
  })
  return units > 0 ? points / units : null
})

const mgpaText = computed<string>(() => (mgpaValue.value == null ? '—' : mgpaValue.value.toFixed(2)))

const gpaTrend = computed<GpaTrendPoint[]>(() => dashboard.value?.gpaTrend ?? [])

const entryYearOptions = computed<number[]>(() => {
  const years = new Set<number>([2022, 2023, 2024, 2025, 2026])
  const own = Number(student.value?.entryYear ?? 0)
  if (own > 0) years.add(own)
  return Array.from(years).sort((a, b) => a - b)
})

/* ── GPA 序列（学期 / 课程点 / 学期 GPA 折线） ── */
const gpaSeries = computed<GpaSeries>(() => {
  const keys: string[] = []
  const trendGpa = new Map<string, number>()
  const computedGpa = new Map<string, { points: number; units: number; count: number }>()

  const touch = (key: string): string => {
    if (!keys.includes(key)) keys.push(key)
    return key
  }

  gpaTrend.value.forEach((point, index) => {
    const key = semKeyFromTermCode(point.termCode) || 'T' + index
    touch(key)
    if (typeof point.gpa === 'number' && Number.isFinite(point.gpa)) trendGpa.set(key, point.gpa)
  })

  records.value.forEach((record, index) => {
    const grade = GRADE_VAL[String(record.grade ?? '').toUpperCase()]
    if (grade == null) return
    const key = semKeyFromTermCode(record.termCode) || 'T' + index
    touch(key)
    const unit = Number(record.units ?? 0) || 1
    const bucket = computedGpa.get(key) ?? { points: 0, units: 0, count: 0 }
    bucket.points += grade * unit
    bucket.units += unit
    bucket.count += 1
    computedGpa.set(key, bucket)
  })

  manualGrades.value.forEach((record) => {
    const grade = GRADE_VAL[String(record.grade ?? '').toUpperCase()]
    if (grade == null) return
    const key = touch(semKeyFromTermCode(record.sem) || record.sem)
    const unit = Number(record.credit ?? 0) || 1
    const bucket = computedGpa.get(key) ?? { points: 0, units: 0, count: 0 }
    bucket.points += grade * unit
    bucket.units += unit
    bucket.count += 1
    computedGpa.set(key, bucket)
  })

  const sems: SemesterEntry[] = keys
    .map((key, index) => {
      const computedEntry = computedGpa.get(key)
      const term = computedEntry && computedEntry.units > 0 ? computedEntry.points / computedEntry.units : null
      return {
        key,
        label: semDisplayLabel(key),
        sort: semSortValue(key, index),
        gpa: trendGpa.get(key) ?? term
      }
    })
    .sort((a, b) => a.sort - b.sort)

  const indexOf = new Map<string, number>()
  sems.forEach((sem, index) => indexOf.set(sem.key, index))

  const dots: CourseDot[] = []
  records.value.forEach((record, index) => {
    const grade = GRADE_VAL[String(record.grade ?? '').toUpperCase()]
    if (grade == null) return
    const key = semKeyFromTermCode(record.termCode) || 'T' + index
    const x = indexOf.get(key)
    if (x == null) return
    dots.push({
      x,
      y: grade,
      label: compactCode(record.courseCode) + ': ' + String(record.grade ?? '') + ' (' + grade.toFixed(1) + ')'
    })
  })
  manualGrades.value.forEach((record) => {
    const grade = GRADE_VAL[String(record.grade ?? '').toUpperCase()]
    if (grade == null) return
    const x = indexOf.get(semKeyFromTermCode(record.sem) || record.sem)
    if (x == null) return
    dots.push({
      x,
      y: grade,
      label: compactCode(record.code) + ': ' + String(record.grade ?? '') + ' (' + grade.toFixed(1) + ')'
    })
  })

  const termPoints: Array<[number, number]> = []
  sems.forEach((sem, index) => {
    if (typeof sem.gpa === 'number' && Number.isFinite(sem.gpa)) termPoints.push([index, sem.gpa])
  })

  return { sems, dots, termPoints }
})

const latestSemLabel = computed<string>(() => {
  const sems = gpaSeries.value.sems
  if (sems.length) return sems[sems.length - 1].label
  return '—'
})

/* ── 已记录成绩列表（服务端成绩单 + 本地手输） ── */
const gpaListRows = computed<GpaListRow[]>(() => {
  const rows: Array<GpaListRow & { sort: number }> = []
  records.value.forEach((record, index) => {
    const grade = String(record.grade ?? '').toUpperCase()
    if (!(grade in GRADE_VAL) && grade !== 'P') return
    const key = semKeyFromTermCode(record.termCode)
    rows.push({
      key: 'rec-' + index,
      label: key ? semDisplayLabel(key) : '—',
      code: compactCode(record.courseCode),
      grade,
      manual: false,
      index,
      sort: semSortValue(key, index)
    })
  })
  manualGrades.value.forEach((record, index) => {
    const key = semKeyFromTermCode(record.sem)
    rows.push({
      key: 'man-' + index,
      label: semDisplayLabel(key),
      code: compactCode(record.code),
      grade: record.grade,
      manual: true,
      index,
      sort: semSortValue(key, index)
    })
  })
  return rows.sort((a, b) => a.sort - b.sort || a.code.localeCompare(b.code))
})

/* ── 技能画像 ── */
function abbrOf(category: string): string {
  if (SCHOOL_ABBR[category]) return SCHOOL_ABBR[category]
  const matched = Object.keys(SCHOOL_ABBR).find((name) => category.includes(name) || name.includes(category))
  if (matched) return SCHOOL_ABBR[matched]
  const trimmed = String(category ?? '').trim()
  return trimmed ? trimmed.slice(0, 3).toUpperCase() : 'ALL'
}

const skillGroups = computed<SkillGroup[]>(() => {
  const seen = new Map<string, string>()
  skillProfile.value.forEach((item) => {
    const category = (item.category ?? '').trim()
    if (!category) return
    const abbr = abbrOf(category)
    if (!seen.has(abbr)) seen.set(abbr, category)
  })
  const groups = Array.from(seen.entries()).map(([abbr, name]) => ({ abbr, name }))
  const ownAbbr = abbrOf(activeSkillCategory.value)
  return groups.sort((a, b) => {
    if (a.abbr === ownAbbr) return -1
    if (b.abbr === ownAbbr) return 1
    return a.abbr.localeCompare(b.abbr)
  })
})

function scoreOf(item: SkillProfileItem): number {
  const raw = Number(item.score ?? 0)
  if (!Number.isFinite(raw)) return 0
  const percent = raw <= 10 ? raw * 10 : raw
  return Math.max(0, Math.min(100, Math.round(percent)))
}

function skillRowsFor(predicate: (item: SkillProfileItem) => boolean, dir: SortDir): SkillRow[] {
  const items = skillProfile.value.filter(predicate)
  if (!items.length) return []
  const avg = Math.round(items.reduce((sum, item) => sum + scoreOf(item), 0) / items.length)
  return items
    .map((item) => ({
      name: (item.name ?? '').trim() || t('pool.noTitle'),
      score: scoreOf(item),
      avg,
      courseCount: Number(item.courseCount ?? 0),
      rationale: item.rationale ?? '',
      category: item.category ?? ''
    }))
    .sort((a, b) => (dir === 'desc' ? b.score - a.score : a.score - b.score))
}

const majorSkillRows = computed<SkillRow[]>(() =>
  skillRowsFor((item) => (item.category ?? '').trim() === activeSkillCategory.value, majorSort.value)
)

const generalSkillRows = computed<SkillRow[]>(() =>
  skillRowsFor((item) => (item.category ?? '').trim() !== activeSkillCategory.value, genSort.value)
)

const topSkills = computed<Array<{ name: string; score: number }>>(() =>
  skillProfile.value
    .map((item) => ({ name: (item.name ?? '').trim() || t('pool.noTitle'), score: scoreOf(item) }))
    .sort((a, b) => b.score - a.score)
    .slice(0, 3)
)

function maturityLabel(score: number): string {
  if (score >= 75) return 'Mastery'
  if (score >= 50) return 'Proficiency'
  if (score >= 25) return 'Foundation'
  return 'Exposure'
}

function scoreTitle(row: SkillRow): string {
  return row.name + ': ' + row.score + (row.avg > 0 ? ' · category avg ' + row.avg : '')
}

const insightText = computed<string>(() => {
  const rows = [...majorSkillRows.value, ...generalSkillRows.value]
  if (!rows.length) return ''
  const top = rows.reduce((best, row) => (row.score > best.score ? row : best), rows[0])
  const bottom = rows.reduce((worst, row) => (row.score < worst.score ? row : worst), rows[0])
  const aboveAvg = majorSkillRows.value.filter((row) => row.avg > 0 && row.score > row.avg).length
  let text =
    'Your strongest skill is <strong>' +
    escapeHtml(top.name) +
    '</strong> (' +
    top.score +
    ' — ' +
    maturityLabel(top.score) +
    ').'
  if (aboveAvg > 0) {
    text +=
      ' You outperform the category average in <strong>' +
      aboveAvg +
      '</strong> major dimension' +
      (aboveAvg > 1 ? 's' : '') +
      '.'
  }
  if (bottom.score < 25) {
    text +=
      '<br>Your biggest growth area is <strong>' +
      escapeHtml(bottom.name) +
      '</strong> (' +
      bottom.score +
      ' — Exposure).'
  }
  return text
})

const insightGapText = computed<string>(() => {
  const rows = majorSkillRows.value.filter((row) => row.score < 30)
  if (!rows.length) return ''
  const lowest = rows.reduce((worst, row) => (row.score < worst.score ? row : worst), rows[0])
  const candidates = generalSkillRows.value
    .filter((row) => row.score >= 30)
    .sort((a, b) => b.score - a.score)
    .slice(0, 3)
  if (!candidates.length) return ''
  const names = candidates.map((row) => '<strong>' + escapeHtml(row.name) + '</strong> (' + row.score + ')').join(' · ')
  return '📈 To build <strong>' + escapeHtml(lowest.name) + '</strong>, consider your stronger areas: ' + names
})

/* ── 能力池 chips / 明细 ── */
const competencyChips = computed<SkillRow[]>(() =>
  skillRowsFor((item) => abbrOf((item.category ?? '').trim()) === activeSchoolAbbr.value, 'desc')
)

const activeChipRationale = computed<string>(() => {
  const found = skillProfile.value.find((item) => (item.name ?? '').trim() === activeChip.value)
  return found?.rationale ?? ''
})

function isTaken(code: string): boolean {
  const target = compactCode(code)
  return records.value.some((record) => compactCode(record.courseCode) === target)
}

/* =====================================================================
 * localStorage（沿用原页面键名 cuhksz_portfolio）
 * ===================================================================== */
function savePortfolio(): void {
  try {
    localStorage.setItem(LS_PORTFOLIO, JSON.stringify({ selected: basket.value, gpa: manualGrades.value }))
  } catch {
    pushNotice('info', t('settings.saveError'))
  }
}

function readPortfolio(): void {
  try {
    const raw = localStorage.getItem(LS_PORTFOLIO)
    if (!raw) return
    const parsed = JSON.parse(raw) as { selected?: BasketEntry[]; gpa?: ManualGrade[] }
    if (Array.isArray(parsed.selected)) {
      basket.value = parsed.selected
        .filter((item) => item && item.code)
        .map((item) => ({
          code: String(item.code),
          title: String(item.title ?? ''),
          units: Number(item.units ?? 0) || 0,
          school: String(item.school ?? ''),
          category: String(item.category ?? ''),
          offered: String(item.offered ?? ''),
          level: Number(item.level ?? 0) || 0,
          outline: String(item.outline ?? '')
        }))
    }
    if (Array.isArray(parsed.gpa)) {
      manualGrades.value = parsed.gpa
        .filter((item) => item && item.code && item.grade)
        .map((item) => ({
          sem: String(item.sem ?? 'F24'),
          code: normalizeCode(item.code),
          grade: String(item.grade ?? ''),
          credit: Number(item.credit ?? 3) || 3
        }))
    }
  } catch {
    /* 忽略损坏的本地状态 */
  }
}

/* =====================================================================
 * 课程浏览：选项 + 分页列表
 * ===================================================================== */
function isSelected(code?: string | null): boolean {
  return basket.value.some((item) => item.code === normalizeCode(code))
}

function entryFromCourse(course: EduCourse): BasketEntry {
  return {
    code: normalizeCode(course.code),
    title: courseTitle(course),
    units: Number(course.credits ?? 0) || 0,
    school: (course.school ?? '').trim(),
    category: (course.category ?? '').trim(),
    offered: firstOfferedTerm(course),
    level: Number(course.level ?? 0) || 0,
    outline: isOutlineAvailable(course) ? 'Yes' : 'No'
  }
}

function toggleCourse(course: EduCourse): void {
  const code = normalizeCode(course.code)
  if (!code) return
  if (isSelected(code)) {
    basket.value = basket.value.filter((item) => item.code !== code)
  } else {
    basket.value = [...basket.value, entryFromCourse(course)]
  }
  savePortfolio()
}

function removeCourse(code: string): void {
  basket.value = basket.value.filter((item) => item.code !== normalizeCode(code))
  savePortfolio()
}

async function loadCourseOptions(): Promise<void> {
  try {
    const res = await getCourseOptions()
    const data = res.data
    optionState.value = {
      schools: data?.schools ?? [],
      categories: data?.categories ?? [],
      levels: data?.levels ?? []
    }
  } catch (error) {
    pushNotice('error', describeError(error, t('courses.error')))
  }
}

async function loadCourses(reset: boolean): Promise<void> {
  if (courseLoading.value) return
  courseLoading.value = true
  courseError.value = ''
  if (reset) {
    coursePage.value = 1
    courseRows.value = []
  }
  try {
    const res = await listCourse({
      pageNum: coursePage.value,
      pageSize: COURSE_PAGE_SIZE,
      code: searchKeyword.value.trim() || undefined,
      school: schoolFilter.value === 'ALL' ? undefined : schoolFilter.value,
      category: categoryFilter.value === 'ALL' ? undefined : categoryFilter.value,
      level: levelFilter.value === 'ALL' ? undefined : Number(levelFilter.value)
    })
    const rows = res.rows ?? []
    courseTotal.value = Number(res.total ?? rows.length)
    courseRows.value = reset ? rows : [...courseRows.value, ...rows]
    coursePage.value += 1
  } catch (error) {
    courseError.value = describeError(error, t('courses.error'))
  } finally {
    courseLoading.value = false
  }
}

function onFilterChange(): void {
  loadCourses(true)
}

function selectCategory(value: string): void {
  categoryFilter.value = categoryFilter.value === value ? 'ALL' : value
  loadCourses(true)
}

function selectSchool(value: string): void {
  schoolFilter.value = schoolFilter.value === value ? 'ALL' : value
  loadCourses(true)
}

function onSearchInput(): void {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    searchTimer = null
    loadCourses(true)
  }, 350)
}

function onCourseScroll(event: Event): void {
  const el = event.target as HTMLElement | null
  if (!el) return
  if (courseLoading.value) return
  if (courseRows.value.length >= courseTotal.value) return
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 120) {
    loadCourses(false)
  }
}

/* =====================================================================
 * 拖拽 / 加课
 * ===================================================================== */
function onCourseDragStart(event: DragEvent, course: EduCourse): void {
  dragCode = normalizeCode(course.code)
  dragSource = 'list'
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'copy'
    try {
      event.dataTransfer.setData('text/plain', dragCode)
    } catch {
      /* ignore */
    }
  }
}

function onTagDragStart(event: DragEvent, code: string): void {
  dragCode = normalizeCode(code)
  dragSource = 'basket'
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    try {
      event.dataTransfer.setData('text/plain', dragCode)
    } catch {
      /* ignore */
    }
  }
}

function onDragEnd(event: DragEvent): void {
  if (dragSource === 'basket' && dragCode && basketRef.value) {
    const rect = basketRef.value.getBoundingClientRect()
    const inside =
      event.clientX >= rect.left &&
      event.clientX <= rect.right &&
      event.clientY >= rect.top &&
      event.clientY <= rect.bottom
    if (!inside) removeCourse(dragCode)
  }
  dragCode = null
  dragSource = null
  dropActive.value = false
}

function onBasketDragOver(event: DragEvent): void {
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'copy'
  dropActive.value = true
}

function onBasketDragLeave(event: DragEvent): void {
  if (event.target === basketRef.value) dropActive.value = false
}

function onBasketDrop(event: DragEvent): void {
  dropActive.value = false
  let code = ''
  try {
    code = event.dataTransfer?.getData('text/plain') ?? ''
  } catch {
    code = ''
  }
  code = normalizeCode(code || dragCode || '')
  dragCode = null
  dragSource = null
  if (!code || isSelected(code)) return
  const known = courseRows.value.find((row) => normalizeCode(row.code) === code)
  if (known) {
    basket.value = [...basket.value, entryFromCourse(known)]
  } else {
    basket.value = [
      ...basket.value,
      { code, title: '', units: 0, school: '', category: '', offered: '', level: 0, outline: '' }
    ]
  }
  savePortfolio()
}

/* =====================================================================
 * Portfolio：导入成绩单 / 导出 CSV / 清空
 * ===================================================================== */
async function loadRecords(): Promise<void> {
  try {
    const res = await listMyRecord({ pageNum: 1, pageSize: 500 })
    records.value = res.rows ?? []
  } catch (error) {
    pushNotice('error', describeError(error, t('msg.loadError')))
  }
}

async function importTranscript(): Promise<void> {
  importing.value = true
  try {
    const res = await listMyRecord({ pageNum: 1, pageSize: 500 })
    const rows = res.rows ?? []
    if (rows.length) records.value = rows
    let added = 0
    rows.forEach((record) => {
      const grade = String(record.grade ?? '').toUpperCase()
      const passed = String(record.passed ?? '') === '1' ? true : grade in GRADE_VAL && grade !== 'F'
      if (!passed || !record.courseCode) return
      const code = normalizeCode(record.courseCode)
      if (basket.value.some((item) => item.code === code)) return
      basket.value = [
        ...basket.value,
        {
          code,
          title: record.courseTitle ?? '',
          units: Number(record.units ?? 0) || 0,
          school: '',
          category: record.category ?? '',
          offered: semDisplayLabel(semKeyFromTermCode(record.termCode)),
          level: 0,
          outline: ''
        }
      ]
      added += 1
    })
    savePortfolio()
    pushNotice('success', t('basket.imported') + ' ' + added)
  } catch (error) {
    pushNotice('error', describeError(error, t('basket.importError')))
  } finally {
    importing.value = false
  }
}

function csvCell(value: unknown): string {
  const text = String(value ?? '')
  return /[",\r\n]/.test(text) ? '"' + text.replace(/"/g, '""') + '"' : text
}

function exportPortfolio(): void {
  if (!basket.value.length) {
    pushNotice('info', t('basket.emptyList'))
    return
  }
  const header = ['Code', 'Title', 'Units', 'Level', 'School', 'Category', 'Offered', 'Outline', 'Grade']
  const gradeOf = (code: string): string => {
    const found = records.value.find((record) => compactCode(record.courseCode) === compactCode(code))
    return found ? String(found.grade ?? '') : ''
  }
  const rows = basket.value.map((item) => [
    item.code,
    item.title,
    item.units,
    item.level,
    item.school,
    item.category,
    item.offered,
    item.outline,
    gradeOf(item.code)
  ])
  const csv = [header, ...rows].map((line) => line.map(csvCell).join(',')).join('\r\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'portfolio.csv'
  anchor.click()
  URL.revokeObjectURL(url)
}

function clearPortfolio(): void {
  if (!window.confirm('Clear all portfolio and grade records?')) return
  basket.value = []
  manualGrades.value = []
  savePortfolio()
  renderGpaChart()
}

/* =====================================================================
 * 手动成绩（仅本浏览器，服务端成绩单只读）
 * ===================================================================== */
function unitForCode(code: string): number {
  const known = courseRows.value.find((row) => normalizeCode(row.code) === normalizeCode(code))
  if (known?.credits) return Number(known.credits)
  const inBasket = basket.value.find((item) => item.code === normalizeCode(code))
  if (inBasket?.units) return Number(inBasket.units)
  return 3
}

function addManualGrade(): void {
  const code = normalizeCode(manualCode.value)
  if (!code) {
    pushNotice('info', 'Please type or pick a course code first (e.g. "MAT 1001").')
    return
  }
  manualGrades.value = [
    ...manualGrades.value,
    {
      sem: manualSem.value,
      code,
      grade: manualGrade.value,
      credit: Number(manualUnits.value) || unitForCode(code)
    }
  ]
  manualCode.value = ''
  savePortfolio()
  renderGpaChart()
}

function removeManualGrade(index: number): void {
  const next = [...manualGrades.value]
  next.splice(index, 1)
  manualGrades.value = next
  savePortfolio()
  renderGpaChart()
}

function onUnitsInput(event: Event): void {
  const value = Number((event.target as HTMLInputElement).value)
  manualUnits.value = Number.isFinite(value) ? value : 0
}

function onGoalInput(event: Event): void {
  const value = Number((event.target as HTMLInputElement).value)
  goalGpa.value = Number.isFinite(value) && value > 0 ? Math.min(4, value) : 3.5
  renderGpaChart()
}

/* =====================================================================
 * GPA 趋势图（ECharts，观感对齐原 Chart.js）
 * ===================================================================== */
function tooltipContent(params: unknown): string {
  const single = (Array.isArray(params) ? params[0] : params) as
    | { seriesName?: string; value?: unknown }
    | undefined
  if (!single) return ''
  const value = single.value
  const series = single.seriesName ?? ''
  if (Array.isArray(value) && value.length >= 2) {
    const x = Number(value[0])
    const y = Number(value[1])
    if (series === 'Course grades') {
      const dot = gpaSeries.value.dots.find((item) => item.x === x && item.y === y)
      return dot ? escapeHtml(dot.label) : ''
    }
    if (series === 'Semester GPA' || series === 'Term GPA') {
      return 'Term GPA: ' + (Number.isFinite(y) ? y.toFixed(2) : '—')
    }
    if (series.startsWith('CGPA') || series.startsWith('Goal CGPA')) return escapeHtml(series)
  }
  return escapeHtml(series)
}

/** 与原页面 syncGPAGridHeights 一致：按"添加成绩"面板实际高度撑开第一行 */
function syncGpaGridHeight(): void {
  const grid = gpaGridRef.value
  if (!grid) return
  if (window.innerWidth <= 1100) {
    grid.style.removeProperty('--gpa-add-h')
    return
  }
  const addPane = grid.querySelector<HTMLElement>('.gpa-add-pane')
  if (!addPane) return
  const height = Math.max(120, Math.ceil(addPane.getBoundingClientRect().height))
  grid.style.setProperty('--gpa-add-h', height + 'px')
}

function renderGpaChart(): void {
  const container = gpaChartRef.value
  if (!container) return
  if (!gpaChart) gpaChart = echarts.init(container)
  const series = gpaSeries.value
  const sems = series.sems
  const cgpa = cgpaValue.value
  const goal = goalGpa.value
  const xMin = -1
  const xMax = Math.max(1, sems.length)
  const values: number[] = series.dots.map((dot) => dot.y)
  series.termPoints.forEach((point) => values.push(point[1]))
  if (cgpa != null) values.push(cgpa)
  values.push(goal)
  const finite = values.filter((value) => Number.isFinite(value))
  const minValue = finite.length ? Math.min(...finite) : 2
  const maxValue = finite.length ? Math.max(...finite) : 4
  // 取 0.5 的整数倍，保证 Y 轴刻度落在整齐的 GPA 数值上（对齐原 Chart.js 观感）
  const minY = Math.max(0, Math.floor((minValue - 0.35) * 2) / 2)
  const maxY = Math.min(4, Math.max(3, Math.ceil((maxValue + 0.35) * 2) / 2))
  const dense = sems.length > 10
  const medium = sems.length >= 7 && sems.length <= 10
  const rotate = dense ? 40 : medium ? 35 : 0
  const axisFontSize = dense ? 10 : medium ? 11 : 12

  const lineBase = {
    type: 'line' as const,
    smooth: 0.25,
    symbolSize: 9,
    showSymbol: true
  }

  gpaChart.setOption(
    {
      animationDuration: 320,
      grid: { left: 10, right: 18, top: 24, bottom: 42, containLabel: true },
      tooltip: { trigger: 'item', formatter: tooltipContent },
      legend: {
        bottom: 0,
        icon: 'roundRect',
        itemWidth: 26,
        itemHeight: 3,
        itemGap: 16,
        textStyle: { fontSize: 12, fontWeight: 500, color: '#1a1d2e' },
        data: [
          'Course grades',
          'Semester GPA',
          'CGPA (' + (cgpa == null ? '—' : cgpa.toFixed(2)) + ')',
          'Goal CGPA (' + goal.toFixed(2) + ')'
        ]
      },
      xAxis: {
        type: 'value',
        min: xMin,
        max: xMax,
        interval: 1,
        axisLine: { lineStyle: { color: '#e2e5ed' } },
        axisTick: { show: false },
        axisLabel: {
          formatter: (value: number) => {
            const index = Math.round(value)
            if (index < 0 || index >= sems.length) return ''
            return sems[index].label
          },
          fontSize: axisFontSize,
          fontWeight: 500,
          color: '#1a1d2e',
          rotate,
          hideOverlap: true,
          margin: 10
        },
        splitLine: { show: true, lineStyle: { color: 'rgba(33,113,181,.10)' } },
        name: 'Semester',
        nameLocation: 'middle',
        nameGap: 30,
        nameTextStyle: { fontSize: 12, fontWeight: 500, color: '#5a6076' }
      },
      yAxis: {
        type: 'value',
        min: minY,
        max: maxY,
        interval: 0.5,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          formatter: (value: number) => (Number(value) > 4 ? '' : Number(value).toFixed(1)),
          fontSize: 12,
          fontWeight: 500,
          color: '#1a1d2e',
          margin: 8
        },
        splitLine: { show: true, lineStyle: { color: 'rgba(33,113,181,.10)' } },
        name: 'GPA',
        nameTextStyle: { fontSize: 12, fontWeight: 500, color: '#5a6076', padding: [0, 0, 6, 0] }
      },
      series: [
        {
          name: 'Course grades',
          type: 'scatter',
          data: series.dots.map((dot) => [dot.x, dot.y]),
          symbolSize: 9,
          itemStyle: { color: '#6baed6', borderColor: '#2171b5', borderWidth: 1 },
          emphasis: { scale: 1.3 }
        },
        {
          ...lineBase,
          name: 'Semester GPA',
          data: series.termPoints,
          lineStyle: { width: 3, color: '#2171b5' },
          itemStyle: { color: '#2171b5' }
        },
        {
          ...lineBase,
          name: 'CGPA (' + (cgpa == null ? '—' : cgpa.toFixed(2)) + ')',
          symbolSize: 1,
          data: cgpa == null ? [] : sems.map((_, index) => [index, cgpa]),
          lineStyle: { width: 2.5, color: '#08306b' },
          itemStyle: { color: '#08306b' }
        },
        {
          ...lineBase,
          name: 'Goal CGPA (' + goal.toFixed(2) + ')',
          symbolSize: 1,
          data: sems.map((_, index) => [index, goal]),
          lineStyle: { width: 2.5, color: '#dc2626', type: 'dashed' },
          itemStyle: { color: '#dc2626' }
        }
      ]
    },
    true
  )
  syncGpaGridHeight()
}

/* =====================================================================
 * Study Headway：专业 / 入学年 / 学分进度 / 分类明细
 * ===================================================================== */
async function loadMajors(): Promise<void> {
  try {
    const res = await listMajor({ pageNum: 1, pageSize: 200 })
    majors.value = res.rows ?? []
  } catch (error) {
    pushNotice('error', describeError(error, t('msg.loadError')))
  }
}

async function loadCurricula(): Promise<void> {
  try {
    const res = await listCurriculum({ pageNum: 1, pageSize: 200 })
    curricula.value = res.rows ?? []
  } catch (error) {
    pushNotice('error', describeError(error, t('headway.error')))
  }
}

function pickCurriculum(rows: EduCurriculum[], majorId: number, entryYear: number): EduCurriculum | null {
  const own = rows.filter((row) => Number(row.majorId) === majorId)
  if (!own.length) return null
  const exact = own.find((row) => Number(row.versionYear) === entryYear)
  if (exact) return exact
  const ranged = own.find(
    (row) =>
      Number(row.effectiveFrom ?? 0) <= entryYear &&
      (row.effectiveTo == null || entryYear <= Number(row.effectiveTo))
  )
  if (ranged) return ranged
  const older = own
    .filter((row) => Number(row.effectiveFrom ?? 0) <= entryYear)
    .sort((a, b) => Number(b.effectiveFrom ?? 0) - Number(a.effectiveFrom ?? 0))
  if (older.length) return older[0]
  // 入学年早于所有版本时取最早的版本（比拿最新版更贴近原页面行为）
  const sorted = [...own].sort((a, b) => Number(a.effectiveFrom ?? 0) - Number(b.effectiveFrom ?? 0))
  return sorted[0] ?? null
}

function unitOfItem(item: EduCurriculumItem): number {
  return Number(item.minUnits ?? 0) || 0
}

function completedCodeSet(): Set<string> {
  const set = new Set<string>()
  records.value.forEach((record) => {
    const grade = String(record.grade ?? '').toUpperCase()
    const passed = String(record.passed ?? '') === '1' && grade !== 'F'
    if (passed && record.courseCode) set.add(compactCode(record.courseCode))
  })
  basket.value.forEach((item) => set.add(compactCode(item.code)))
  return set
}

function percentOf(done: number, total: number): number {
  if (!total) return 0
  return Math.max(0, Math.min(100, Math.round((done / total) * 100)))
}

function buildHeadway(curriculum: EduCurriculum | null, entryYear: number): HeadwayModel {
  const earned = Number(progress.value?.earned ?? 0)
  const curriculumTotal = Number(curriculum?.totalCredits ?? 0)
  const creditTotal = curriculumTotal > 0 ? curriculumTotal : 120

  const categories: CreditCategoryRow[] = progress.value?.categoryBreakdown ?? []
  const majorCategory = categories.find((row) => (row.category ?? '') === 'Major')
  const majorTotal =
    Number(curriculum?.majorCredits ?? 0) ||
    Number(progress.value?.majorRequired ?? 0) ||
    Number(majorCategory?.required ?? 0) ||
    70
  const majorEarned = Number(majorCategory?.earned ?? 0)
  const majorDone = Math.min(majorEarned, majorTotal)

  const items = curriculum?.items ?? []
  const completed = completedCodeSet()
  const requiredItems = items.filter((item) => (item.requirementType ?? '').toUpperCase() === 'REQUIRED')
  const electiveItems = items.filter((item) => (item.requirementType ?? '').toUpperCase() !== 'REQUIRED')

  const requiredRows: HeadwayRequirement[] = requiredItems.map((item) => ({
    code: normalizeCode(item.courseCode),
    title: item.courseTitle ?? '',
    done: completed.has(compactCode(item.courseCode))
  }))

  const requiredUnits = requiredItems.reduce((sum, item) => sum + unitOfItem(item), 0)
  const electiveNeed = Math.max(0, Math.round(majorTotal - requiredUnits))
  const electiveDone = Math.round(
    electiveItems
      .filter((item) => completed.has(compactCode(item.courseCode)))
      .reduce((sum, item) => sum + unitOfItem(item), 0)
  )
  const electiveShort = Math.max(0, electiveNeed - electiveDone)

  const sections = new Map<string, HeadwayRequirement[]>()
  electiveItems.forEach((item) => {
    const label = (item.section ?? '').trim() || 'Elective'
    const rows = sections.get(label) ?? []
    rows.push({
      code: normalizeCode(item.courseCode),
      title: item.courseTitle ?? '',
      done: completed.has(compactCode(item.courseCode))
    })
    sections.set(label, rows)
  })

  const categoryRows: HeadwayCategory[] = Array.from(sections.entries())
    .map(([label, rows]) => {
      const sameSection = electiveItems.filter((item) => ((item.section ?? '').trim() || 'Elective') === label)
      const doneUnits = sameSection
        .filter((item) => completed.has(compactCode(item.courseCode)))
        .reduce((sum, item) => sum + unitOfItem(item), 0)
      const allUnits = sameSection.reduce((sum, item) => sum + unitOfItem(item), 0)
      return { label, earned: Math.round(doneUnits), required: Math.round(allUnits), rows }
    })
    .sort((a, b) => a.label.localeCompare(b.label))

  const ucoreRows: HeadwayUcoreRow[] = categories
    .filter((row) => (row.category ?? '') !== 'Major')
    .map((row) => {
      const earnedUnits = Number(row.earned ?? 0)
      const requiredUnitsRow = Number(row.required ?? 0)
      return {
        label: row.category ?? 'Other',
        earned: Math.round(earnedUnits),
        required: Math.round(requiredUnitsRow),
        ok: earnedUnits > 0 && (requiredUnitsRow <= 0 || earnedUnits >= requiredUnitsRow)
      }
    })
    .sort((a, b) => a.label.localeCompare(b.label))

  const ucoreEarned = categories
    .filter((row) => (row.category ?? '') !== 'Major')
    .reduce((sum, row) => sum + Number(row.earned ?? 0), 0)
  const ucoreTotal = 36
  const ucoreDone = Math.min(ucoreTotal, Math.round(ucoreEarned))

  const ruleLine =
    'Rule: ' +
    (curriculum?.versionLabel ?? 'no matched curriculum version') +
    ' (' +
    entryYear +
    ' entry), major denominator ' +
    majorTotal +
    ' units.' +
    (curriculum?.sourceDoc ? ' Source: ' + curriculum.sourceDoc : '')

  return {
    credit: { done: Math.round(earned), total: creditTotal, percent: percentOf(earned, creditTotal) },
    major: { done: Math.round(majorDone), total: majorTotal, percent: percentOf(majorDone, majorTotal) },
    ucore: { done: ucoreDone, total: ucoreTotal, percent: percentOf(ucoreDone, ucoreTotal) },
    requiredRows,
    categoryRows,
    ucoreRows,
    ucoreSource: curriculum?.versionLabel ? 'Academic Curriculum ' + curriculum.versionLabel : 'Academic Curriculum',
    electiveDone,
    electiveNeed,
    electiveShort,
    majorIncomplete: requiredRows.some((row) => !row.done) || electiveShort > 0 || majorDone < majorTotal,
    ruleLine
  }
}

async function loadHeadway(): Promise<void> {
  headwayLoading.value = true
  headwayError.value = ''
  try {
    const majorId = Number(selectedMajorId.value)
    const entryYear = Number(selectedEntryYear.value)
    if (!majorId) {
      headwayModel.value = null
      headwayError.value = t('headway.error')
      return
    }
    let curriculum: EduCurriculum | null = null
    try {
      const byMajor = await getCurriculumByMajor({ majorId, entryYear })
      curriculum = byMajor.data ?? null
    } catch {
      curriculum = null
    }
    if (!curriculum) {
      if (!curricula.value.length) await loadCurricula()
      curriculum = pickCurriculum(curricula.value, majorId, entryYear)
    }
    if (curriculum?.curriculumId) {
      try {
        const detail = await getCurriculum(curriculum.curriculumId)
        curriculum = detail.data ?? curriculum
      } catch {
        /* 详情失败时用列表行（无 items）继续渲染进度条 */
      }
    }
    headwayModel.value = buildHeadway(curriculum, entryYear)
    resolvedCurriculum.value = curriculum
    resolvedEntryYear.value = entryYear
  } catch (error) {
    headwayError.value = describeError(error, t('headway.error'))
    headwayModel.value = null
  } finally {
    headwayLoading.value = false
    await nextTick()
    updateHeadwayMaxHeight()
  }
}

/** 篮子 / 成绩单变化后（例如导入成绩单）重建 headway，无需重新请求接口 */
function refreshHeadwayModel(): void {
  if (!headwayModel.value && !resolvedCurriculum.value) return
  headwayModel.value = buildHeadway(resolvedCurriculum.value, resolvedEntryYear.value)
  nextTick(() => updateHeadwayMaxHeight())
}

function updateHeadwayMaxHeight(): void {
  const el = document.getElementById('headway-content')
  if (!el) return
  const rect = el.getBoundingClientRect()
  const available = window.innerHeight - rect.top - 24
  headwayMaxHeight.value = Math.max(480, Math.floor(available), Math.floor(window.innerHeight * 0.7)) + 'px'
}

/* =====================================================================
 * 技能画像 + 能力池
 * ===================================================================== */
async function loadSkillProfile(): Promise<void> {
  skillError.value = ''
  try {
    const res = await getSkillProfile()
    skillProfile.value = res.data ?? []
    if (!activeSkillCategory.value && skillGroups.value.length) {
      activeSkillCategory.value = skillGroups.value[0].name
    }
    activeSchoolAbbr.value = activeSkillCategory.value ? abbrOf(activeSkillCategory.value) : ''
    const chips = competencyChips.value
    if (!activeChip.value && chips.length) {
      activeChip.value = chips[0].name
      await loadCompetencyPool(chips[0].name)
    }
  } catch (error) {
    skillError.value = describeError(error, t('skill.error'))
  }
}

async function loadCompetencyPool(keyword: string): Promise<void> {
  poolLoading.value = true
  poolError.value = ''
  poolRows.value = []
  try {
    const res = await getCompetencyPool({
      pageNum: 1,
      pageSize: 24,
      schoolAbbr: activeSchoolAbbr.value || undefined,
      keyword: keyword || undefined
    })
    poolRows.value = (res.rows ?? []).map((row: CourseCompetency) => ({
      code: normalizeCode(row.courseCode),
      title: row.courseTitle ?? '',
      score: Number(row.score ?? 0) || 0,
      level: row.level ?? '',
      rationale: row.rationale ?? ''
    }))
    if (!poolRows.value.length) poolError.value = t('pool.empty')
  } catch (error) {
    const message = describeError(error, t('pool.forbidden'))
    poolError.value = /权限|403|forbidden|error/i.test(message) ? t('pool.forbidden') : message
  } finally {
    poolLoading.value = false
  }
}

function defaultSkillCategoryForSchool(): string {
  const ownMajor = majors.value.find((item) => String(item.majorId) === String(student.value?.majorId ?? ''))
  const abbr = ownMajor?.schoolAbbr ?? ''
  const matched = skillGroups.value.find((group) => group.abbr === abbr)
  return matched?.name ?? ''
}

function onSkillCategoryChange(): void {
  activeSchoolAbbr.value = abbrOf(activeSkillCategory.value)
  const chips = competencyChips.value
  activeChip.value = chips.length ? chips[0].name : ''
  if (activeChip.value) loadCompetencyPool(activeChip.value)
  else poolRows.value = []
}

function selectCompetencySchool(abbr: string): void {
  const group = skillGroups.value.find((item) => item.abbr === abbr)
  if (group) activeSkillCategory.value = group.name
  activeSchoolAbbr.value = abbr
  const chips = competencyChips.value
  activeChip.value = chips.length ? chips[0].name : ''
  if (activeChip.value) loadCompetencyPool(activeChip.value)
  else {
    poolRows.value = []
    poolError.value = t('pool.empty')
  }
}

function selectCompetencyChip(name: string): void {
  activeChip.value = name
  loadCompetencyPool(name)
}

function setSkillSort(target: 'major' | 'gen', dir: SortDir): void {
  if (target === 'major') majorSort.value = dir
  else genSort.value = dir
}

/* =====================================================================
 * 顶层加载
 * ===================================================================== */
async function loadAnalytics(): Promise<void> {
  const results = await Promise.allSettled([getDashboard(), getMyProgress()])
  const dashboardResult = results[0]
  const progressResult = results[1]
  if (dashboardResult.status === 'fulfilled') {
    dashboard.value = dashboardResult.value.data ?? null
  } else {
    pushNotice('error', describeError(dashboardResult.reason, t('msg.loadError')))
  }
  if (progressResult.status === 'fulfilled') {
    progress.value = progressResult.value.data ?? null
  } else {
    pushNotice('error', describeError(progressResult.reason, t('msg.loadError')))
  }
}

async function loadAll(): Promise<void> {
  loading.value = true
  unbound.value = false
  notices.value = []
  try {
    const res = await getMyStudent()
    student.value = res.data ?? null
  } catch (error) {
    const message = describeError(error, t('msg.loadError'))
    if (/未绑定/.test(message) || /not bound/i.test(message)) {
      unbound.value = true
      loading.value = false
      return
    }
    pushNotice('error', message)
    loading.value = false
    return
  }

  await Promise.all([loadCourseOptions(), loadRecords(), loadAnalytics(), loadMajors(), loadSkillProfile()])

  const info = student.value
  const ownMajor = majors.value.find(
    (item) => String(item.majorId) === String(info?.majorId ?? '') || item.majorCode === info?.majorCode
  )
  if (ownMajor?.majorId != null) selectedMajorId.value = String(ownMajor.majorId)
  else if (majors.value.length && majors.value[0].majorId != null) selectedMajorId.value = String(majors.value[0].majorId)
  selectedEntryYear.value = String(info?.entryYear ?? new Date().getFullYear())

  const ownCategory = defaultSkillCategoryForSchool()
  if (ownCategory && activeSkillCategory.value !== ownCategory) {
    activeSkillCategory.value = ownCategory
    activeSchoolAbbr.value = abbrOf(ownCategory)
    const chips = competencyChips.value
    activeChip.value = chips.length ? chips[0].name : ''
    if (activeChip.value) await loadCompetencyPool(activeChip.value)
  }

  await Promise.all([loadCourses(true), loadHeadway()])
  await nextTick()
  renderGpaChart()
  loading.value = false
}

/* =====================================================================
 * 顶栏动作
 * ===================================================================== */
function signOut(): void {
  if (!window.confirm('Sign out? Your portfolio is preserved on this device.')) return
  userStore
    .logOut()
    .then(() => router.push('/login'))
    .catch(() => router.push('/login'))
}

function switchStudent(): void {
  userStore
    .logOut()
    .then(() => router.push('/login'))
    .catch(() => router.push('/login'))
}

/* =====================================================================
 * 生命周期
 * ===================================================================== */
function onWindowResize(): void {
  updateHeadwayMaxHeight()
  syncGpaGridHeight()
  gpaChart?.resize()
}

onMounted(() => {
  document.documentElement.lang = lang.value
  readPortfolio()
  window.addEventListener('resize', onWindowResize)
  loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onWindowResize)
  if (searchTimer) clearTimeout(searchTimer)
  gpaChart?.dispose()
  gpaChart = null
})

watch(
  gpaSeries,
  () => {
    nextTick(() => renderGpaChart())
  },
  { deep: false }
)

watch([records, basket], () => refreshHeadwayModel(), { deep: false })
</script>

<style scoped src="../../../assets/student/dashboard.css"></style>

<style scoped>
/* 本页新增：页内提示条 + 空/错态。其余视觉一律来自 assets/student/dashboard.css。 */
.sp-root {
  min-height: 100vh;
}

.sp-notices {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 36px 0;
  max-width: 1500px;
  margin: 0 auto;
  width: 100%;
}

.notice-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.notice-error {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #991b1b;
}

.notice-success {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #065f46;
}

.notice-close {
  margin-left: auto;
  border: none;
  background: transparent;
  color: inherit;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  padding: 0 2px;
}

.sp-unbound {
  grid-template-columns: 1fr;
  max-width: 760px;
}

.sp-empty {
  text-align: center;
  color: var(--text3);
  padding: 20px;
  font-size: 12px;
}

.sp-score-empty {
  font-size: 13px;
  color: var(--text3);
  text-align: center;
  padding: 18px;
}

.sp-error {
  text-align: center;
  color: #991b1b;
  background: #fee2e2;
  border-radius: 8px;
  padding: 14px;
  font-size: 12px;
  line-height: 1.5;
}

/* ECharts 挂载点需要显式高度（位于 .gpa-chart-inner 内） */
#gpa-chart {
  width: 100%;
  height: 100%;
  min-height: 520px;
}
</style>
