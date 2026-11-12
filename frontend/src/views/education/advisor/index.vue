<template>
  <!-- 导师看板：自 legacy/aa_dashboard_v2.html 迁移（视觉 1:1）。
       外壳 LegacyShell 已提供深蓝顶部导航，这里不再重复原页的 .header 品牌区。 -->
  <div class="app-container advisor-page">
    <!-- 页面标题区 -->
    <div class="lg-head">
      <div>
        <h2>导师看板</h2>
        <p>汇总学生的 GPA、学分进度、风险信号与沟通情况，帮助导师快速定位需要支持的学生。</p>
      </div>
      <div class="lg-head-actions">
        <span v-if="loading" class="metric-note">加载中…</span>
        <button class="btn secondary" type="button" @click="reload">刷新数据</button>
      </div>
    </div>

    <div v-if="errorMessage" class="advisor-error">{{ errorMessage }}</div>

    <!-- ================= Overview 卡片组 ================= -->
    <div class="overview-cards">
      <div class="card overview-metric">
        <div class="metric-note">Advisees</div>
        <div class="metric-value">{{ overview.studentCount ?? 0 }}</div>
        <div class="metric-note">当前库中的学生总数</div>
      </div>
      <div class="card overview-metric">
        <div class="metric-note">Average GPA</div>
        <div class="metric-value">{{ fmt(overview.avgGpa, 2) }}</div>
        <div class="metric-note">累计 GPA 均值（4.0 制）</div>
      </div>
      <div class="card overview-metric">
        <div class="metric-note">Average Credits</div>
        <div class="metric-value">{{ fmt(overview.avgCredits, 1) }}</div>
        <div class="metric-note">已修学分均值 / 毕业要求 {{ GRADUATION_CREDITS }} cr</div>
      </div>
      <div class="card overview-metric">
        <div class="metric-note">Risk Snapshot</div>
        <div class="metric-value">{{ riskCounts.high }}</div>
        <div class="metric-note risk-badges">
          <span class="badge red">{{ riskCounts.high }} High</span>
          <span class="badge orange-badge">{{ riskCounts.medium }} Medium</span>
          <span class="badge green-badge">{{ riskCounts.low }} Low</span>
        </div>
      </div>
      <div class="card overview-metric">
        <div class="metric-note">Credit Progress</div>
        <div class="metric-value">{{ overview.progress?.onTrack ?? 0 }}/{{ overview.studentCount ?? 0 }}</div>
        <div class="metric-note">
          on track · {{ overview.progress?.behind ?? 0 }} behind · {{ overview.progress?.unknown ?? 0 }} unknown
        </div>
      </div>
    </div>

    <!-- ================= Filters ================= -->
    <div class="filters advisor-filters">
      <select v-model="filters.entryYear" aria-label="Filter by entry year" @change="reloadStudents">
        <option value="">Year: All</option>
        <option v-for="year in entryYearOptions" :key="year" :value="String(year)">
          {{ year }}
        </option>
      </select>
      <select v-model="filters.majorId" aria-label="Filter by major" @change="reloadStudents">
        <option value="">Major: All</option>
        <option v-for="major in majors" :key="major.majorId" :value="String(major.majorId)">
          {{ majorLabel(major) }}
        </option>
      </select>
      <select v-model="filters.riskLevel" aria-label="Filter by risk level">
        <option value="">Risk Level: All</option>
        <option value="high">High</option>
        <option value="medium">Medium</option>
        <option value="low">Low</option>
      </select>
      <input
        v-model="filters.keyword"
        placeholder="Search student name / ID..."
        aria-label="Search student"
        style="width: 220px"
        @input="onSearch"
      />
      <button class="btn secondary" type="button" @click="clearFilters">✕ Clear filters</button>
      <span class="filter-hint">Filters apply to the advisee list &amp; risk tables.</span>
    </div>

    <!-- ================= GPA Performance + Progress Overview ================= -->
    <div class="overview-row">
      <div class="card">
        <h2>GPA Performance</h2>
        <h3>GPA Distribution</h3>
        <div ref="gpaDistRef" class="echart-box" style="height: 150px"></div>
        <div style="height: 10px"></div>
        <h3>GPA Trend</h3>
        <div ref="gpaTrendRef" class="echart-box" style="height: 120px"></div>
      </div>

      <div class="card progress-overview-card">
        <div class="progress-head">
          <h2 style="margin-bottom: 0">Progress Overview</h2>
          <div class="progress-head-metric">
            <div class="axis-label">Avg Completion Rate</div>
            <div class="progress-head-value">
              <span class="big-blue">{{ avgCompletionPct }}%</span>
              <span class="metric-note">{{ fmt(overview.avgCredits, 1) }} / {{ GRADUATION_CREDITS }} cr</span>
            </div>
            <div class="progress-line" style="height: 7px; margin: 0; width: 140px">
              <div
                class="progress-fill"
                :style="{ width: avgCompletionPct + '%', background: 'linear-gradient(90deg,#1d4ed8,#60a5fa)' }"
              ></div>
            </div>
          </div>
        </div>

        <div class="progress-sub">
          <div class="progress-sub-head">
            <div class="sub-title">Student Distribution by Completion Rate</div>
            <div class="btn-group">
              <button
                class="btn"
                :class="{ secondary: distributionMode !== 'major' }"
                type="button"
                style="padding: 3px 9px; font-size: 11px"
                @click="setDistributionMode('major')"
              >
                By Major
              </button>
              <button
                class="btn"
                :class="{ secondary: distributionMode !== 'year' }"
                type="button"
                style="padding: 3px 9px; font-size: 11px"
                @click="setDistributionMode('year')"
              >
                By Year
              </button>
            </div>
          </div>
          <div class="axis-label" style="margin-bottom: 6px">
            <span style="font-weight: 600; color: #374151">Y-axis: No. of students</span>
            &nbsp;·&nbsp; Bar colour = overall credit completion range
          </div>
          <div ref="distributionRef" class="echart-box" style="height: 190px"></div>
        </div>

        <div class="progress-sub" style="border-bottom: none">
          <div class="sub-title" style="margin-bottom: 12px">Avg. Completion by Credit Category</div>
          <div class="category-head">
            <span>Category</span><span>Progress</span><span style="text-align: right">Credits</span>
          </div>
          <div v-for="row in categoryRows" :key="row.category ?? 'unknown'" class="category-row">
            <span class="category-name">{{ categoryLabel(row.category) }}</span>
            <div class="category-bar">
              <div
                class="category-bar-fill"
                :style="{ width: categoryPercent(row) + '%', background: categoryColor(row.category) }"
              ></div>
              <span class="category-bar-text">{{ categoryPercent(row) }}%</span>
            </div>
            <span class="category-credits">
              {{ fmt(row.avgCredits, 1) }} cr<span class="muted"> avg</span>
            </span>
          </div>
          <div v-if="!categoryRows.length" class="axis-label" style="padding: 8px 0">暂无分类学分数据</div>
          <div class="category-total">
            Total: <strong style="color: #374151">{{ fmt(categoryTotal, 1) }} cr</strong>
            · 培养方案暂无分类要求学分，按平均已修学分展示
          </div>
        </div>
      </div>
    </div>

    <!-- ================= Communication Overview ================= -->
    <div class="card">
      <h2>Communication Overview</h2>
      <div class="comm-grid">
        <div>
          <div class="sub-title" style="margin-bottom: 8px">Communication Coverage</div>
          <div class="comm-big">0%</div>
          <div class="progress-line" style="height: 8px; margin: 10px 0 6px">
            <div class="progress-fill" style="width: 0%; background: #1d4ed8"></div>
          </div>
          <div class="axis-label">0 / {{ overview.studentCount ?? 0 }} students contacted recently</div>
        </div>
        <div>
          <div class="sub-title">Contact Frequency Distribution</div>
          <div class="axis-label" style="margin-top: 3px">
            Based on total number of advising contacts recorded between advisor and student
          </div>
          <div v-for="item in contactFrequency" :key="item.label" class="comm-row">
            <div class="comm-row-head">
              <span style="color: #374151">{{ item.label }}</span>
              <span :style="{ color: item.color, fontWeight: 600 }">
                {{ item.pct }}%&nbsp;
                <span class="axis-label" style="font-size: 12px">{{ item.count }} students</span>
              </span>
            </div>
            <div class="progress-line" style="height: 8px; margin: 0">
              <div class="progress-fill" :style="{ width: item.pct + '%', background: item.color }"></div>
            </div>
          </div>
        </div>
      </div>
      <div class="advisor-note-box">
        当前数据库没有“导师-学生”关系表，也没有联系记录表（advisor contact log），
        因此沟通覆盖与联系频次暂无数据源；补齐联系记录后即可自动填充。
      </div>
    </div>

    <!-- ================= Risk Snapshot ================= -->
    <div class="section-head">
      <h2>Risk Snapshot</h2>
      <p>The early-warning centre helps advisors identify students who may require support and take timely action.</p>
    </div>

    <div class="card high-risk-card">
      <div class="card-head-row">
        <div>
          <h2 style="margin-bottom: 2px">Overall High-Risk Students</h2>
          <p class="subtitle" style="margin: 0">
            Students identified through multiple combined risk signals — immediate attention recommended.
          </p>
        </div>
        <div class="head-actions-inline">
          <span class="badge red" style="font-size: 14px; padding: 6px 14px">
            {{ highRiskRows.length }} Students
          </span>
          <button
            class="btn secondary"
            type="button"
            style="font-size: 12px; padding: 5px 12px"
            @click="configOpen = !configOpen"
          >
            Score Config
          </button>
        </div>
      </div>

      <!-- 风险权重配置（展示层实时重算，不再请求后端） -->
      <div v-if="configOpen" class="config-panel">
        <div class="config-head">
          <div class="sub-title">Risk Score Configuration</div>
          <button class="btn secondary" type="button" style="font-size: 11px; padding: 3px 10px" @click="configOpen = false">
            Close ✕
          </button>
        </div>
        <div class="formula">
          Score = <span id="fw-gpa">{{ weights.gpa }}</span>% × GPA Low + <span id="fw-credit">{{ weights.credit }}</span>%
          × Credit Delay + <span id="fw-contact">{{ weights.contact }}</span>% × No-Contact +
          <span id="fw-final">{{ weights.final }}</span>% × Failed Course
        </div>
        <div class="config-grid">
          <div>
            <div class="sub-title" style="margin-bottom: 10px">Score Weights</div>
            <div class="weight-grid">
              <div v-for="field in weightFields" :key="field.key" class="weight-box">
                <div class="axis-label" style="margin-bottom: 6px">{{ field.label }}</div>
                <div class="weight-input-row">
                  <input
                    :id="'wi-' + field.key"
                    v-model.number="weights[field.key]"
                    type="number"
                    min="0"
                    max="100"
                    style="width: 48px; text-align: center; font-weight: 700; color: #1d4ed8; font-size: 15px"
                  />
                  <span class="axis-label">%</span>
                </div>
              </div>
            </div>
            <div class="weight-total">
              <span class="axis-label">Total:</span>
              <span :style="{ color: weightTotal === 100 ? '#15803d' : '#b91c1c', fontWeight: 700 }">
                {{ weightTotal }}%
              </span>
              <span v-if="weightTotal !== 100" class="warn">Must equal 100%</span>
              <button class="btn secondary" type="button" style="margin-left: auto" @click="resetWeights">Reset</button>
            </div>
          </div>
          <div>
            <div class="sub-title" style="margin-bottom: 10px">High Risk Threshold</div>
            <div class="threshold-box">
              <div class="axis-label" style="line-height: 2.2">
                Flag as High Risk if Score ≥
                <input
                  v-model.number="highRiskThreshold"
                  type="number"
                  min="0"
                  max="100"
                  style="width: 54px; text-align: center; font-weight: 700; color: #b91c1c; font-size: 14px; padding: 2px 5px"
                />
              </div>
              <div class="axis-label" style="margin-top: 4px">Showing students with score ≥ {{ highRiskThreshold }}</div>
              <div style="margin-top: 8px">
                <span class="badge red" style="font-size: 12px; padding: 3px 10px">
                  {{ highRiskRows.length }} student{{ highRiskRows.length === 1 ? '' : 's' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="table-scroll" style="max-height: 200px">
        <table>
          <thead class="sticky-head">
            <tr>
              <th>Student</th>
              <th>Risk Score</th>
              <th>Reason</th>
              <th>Last Contact</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="student in highRiskRows" :key="student.studentId">
              <td><strong>{{ student.name }}</strong></td>
              <td>
                <span :class="['badge', riskBadgeClass(student.displayLevel)]">{{ student.displayScore }}</span>
              </td>
              <td>{{ reasonText(student) }}</td>
              <td class="muted">—</td>
              <td>
                <button class="btn secondary" type="button" @click="openStudent(student)">View Detail</button>
                <button class="btn" type="button" @click="openOutreach(student)">Send Email</button>
              </td>
            </tr>
            <tr v-if="!highRiskRows.length">
              <td colspan="5" class="empty-cell">
                No students at or above the current threshold ({{ highRiskThreshold }}).
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="axis-label" style="text-align: right; margin-top: 6px">
        Showing {{ highRiskRows.length }} of {{ enrichedStudents.length }} students
      </p>
    </div>

    <div class="card">
      <div class="card-head-row">
        <div>
          <h2 style="margin-bottom: 2px">{{ priorityTitle }}</h2>
          <p class="subtitle" style="margin: 0">
            Click an indicator to filter students by risk category. Default sorting by dynamic risk score.
          </p>
        </div>
        <button
          class="btn secondary"
          type="button"
          style="font-size: 12px; padding: 5px 12px"
          @click="alertConfigOpen = !alertConfigOpen"
        >
          Alert Config
        </button>
      </div>

      <!-- 告警阈值配置（与旧版一致：只影响提示文案口径，不改变后端规则） -->
      <div v-if="alertConfigOpen" class="config-panel">
        <div class="config-head">
          <div class="sub-title">Alert Threshold Configuration</div>
          <button class="btn secondary" type="button" style="font-size: 11px; padding: 3px 10px" @click="alertConfigOpen = false">
            Close ✕
          </button>
        </div>
        <div class="alert-grid">
          <div class="alert-box alert-credit">
            <div class="alert-title">Credit Alert</div>
            <div class="alert-line">
              Flag if credits &lt;
              <input v-model.number="alertThresholds.creditPerSem" type="number" min="1" max="30" style="width: 44px" />
              × semesters
            </div>
            <div class="axis-label">e.g. Year 3 (4 sems): &lt; {{ alertThresholds.creditPerSem * 4 }} cr</div>
          </div>
          <div class="alert-box alert-gpa">
            <div class="alert-title">GPA Drop</div>
            <div class="alert-line">
              Flag if GPA fell ≥
              <input
                v-model.number="alertThresholds.gpaDrop"
                type="number"
                min="0"
                max="2"
                step="0.05"
                style="width: 52px"
              />
              pts
            </div>
            <div class="axis-label">e.g. 3.20 → {{ (3.2 - alertThresholds.gpaDrop).toFixed(2) }} triggers alert</div>
          </div>
          <div class="alert-box alert-contact">
            <div class="alert-title">No Contact</div>
            <div class="alert-line">
              Flag if last contact &gt;
              <input v-model.number="alertThresholds.noContact" type="number" min="7" max="365" style="width: 52px" />
              days
            </div>
            <div class="axis-label">Currently: {{ alertThresholds.noContact }}+ days no contact</div>
          </div>
        </div>
      </div>

      <div class="tiles">
        <div
          v-for="tile in riskTiles"
          :key="tile.key"
          class="risk-tile"
          :class="{ active: activeRiskFilter === tile.key }"
          @click="toggleRiskTile(tile.key)"
        >
          <span :class="['badge', tile.badgeClass]">{{ tile.label }}</span>
          <strong>{{ tile.count }} Students</strong>
          <p>{{ tile.desc }}</p>
        </div>
      </div>

      <div class="reason-filter-row">
        <select v-model="reasonFilter" aria-label="Filter by reason">
          <option value="all">Filter by Reason: All</option>
          <option value="gpa">GPA Low</option>
          <option value="credit">Credit Delay</option>
          <option value="contact">No Contact</option>
          <option value="failed">Failed Course</option>
        </select>
        <button
          v-if="activeRiskFilter !== 'all' || reasonFilter !== 'all'"
          class="btn secondary"
          type="button"
          style="font-size: 12px; padding: 6px 12px"
          @click="clearRiskFilters"
        >
          ✕ Clear filter
        </button>
      </div>

      <table>
        <thead>
          <tr>
            <th>Student</th>
            <th>Risk Score</th>
            <th>Reason</th>
            <th>Last Contact</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="student in priorityRows" :key="student.studentId">
            <td>{{ student.name }}</td>
            <td>
              <span :class="['badge', riskBadgeClass(student.displayLevel)]">{{ student.displayScore }}</span>
            </td>
            <td>{{ reasonText(student) }}</td>
            <td class="muted">—</td>
            <td>
              <button class="btn secondary" type="button" @click="openStudent(student)">View Detail</button>
              <button class="btn" type="button" @click="openOutreach(student)">Send Email</button>
            </td>
          </tr>
          <tr v-if="!priorityRows.length">
            <td colspan="5" class="empty-cell">No students match the current risk filters.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 学生画像（Risk Snapshot 详情面板） -->
    <div v-if="profile" class="card profile-panel">
      <div class="card-head-row">
        <div>
          <h2 style="margin-bottom: 4px">Student Profile — {{ profile.name }}</h2>
          <div>
            <span class="badge blue">Class of {{ profile.entryYear ?? '—' }}</span>
            <span class="badge" style="background: #dbeafe; color: #1e40af; margin-left: 6px">
              {{ profile.majorName || '—' }}
            </span>
            <span :class="['badge', riskBadgeClass(profile.riskLevel)]" style="margin-left: 6px">
              {{ riskLevelLabel(profile.riskLevel) }} · {{ profile.riskScore ?? 0 }}
            </span>
          </div>
        </div>
        <button class="btn secondary" type="button" @click="closeProfile">Close ✕</button>
      </div>

      <div class="profile-grid">
        <div>
          <h3 style="margin-bottom: 8px">GPA Trend</h3>
          <div class="mini-chart" style="height: 190px">
            <div ref="profileGpaRef" class="echart-box" style="height: 100%"></div>
          </div>

          <h3 style="margin-bottom: 10px">Credit Completion</h3>
          <div class="credit-row">
            <span style="color: #374151; font-weight: 500">Overall</span>
            <span class="blue-text">
              {{ pct(profile.creditProgressPct) }}%
              <span class="muted">
                ({{ fmt(profile.totalCredits, 1) }}/{{ fmt(profile.graduationCredits, 0) }}cr)
              </span>
            </span>
          </div>
          <div class="progress-line" style="height: 7px; margin: 0">
            <div class="progress-fill" :style="{ width: pct(profile.creditProgressPct) + '%', background: '#1d4ed8' }"></div>
          </div>

          <h3 style="margin: 14px 0 10px">Risk Detail</h3>
          <div class="risk-detail">
            <div v-for="rule in riskRuleCatalog" :key="rule.code" class="risk-rule-row">
              <span :class="['badge', hasReason(rule.code) ? 'red' : 'green-badge']">
                {{ hasReason(rule.code) ? 'HIT' : 'OK' }}
              </span>
              <div>
                <strong style="font-size: 13px">{{ rule.label }}</strong>
                <div class="axis-label">{{ reasonDetail(rule.code) || rule.note }}</div>
              </div>
            </div>
          </div>
        </div>
        <div>
          <h3 style="margin-bottom: 10px">Communication History</h3>
          <div class="comm-history">
            <div v-if="!hasCommunication" class="axis-label">暂无联系记录（数据库无 advisor contact log）。</div>
            <div v-for="(item, index) in communicationHistory" :key="index" class="comm-item">
              <span class="comm-date">{{ item.date }}</span>
              <span class="comm-text">{{ item.text }}</span>
            </div>
          </div>
          <h3 style="margin-bottom: 8px">Advisor Notes</h3>
          <textarea
            v-model="notes"
            class="notes-area"
            style="min-height: 90px"
            placeholder="记录与学生的沟通要点…"
          ></textarea>
          <div style="margin-top: 14px; display: flex; justify-content: flex-end">
            <button class="btn" type="button" style="padding: 10px 22px; font-size: 14px" @click="openOutreach(profile)">
              Send Email
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ================= Student Management ================= -->
    <div class="section-head">
      <h2>Student Management</h2>
      <p>The main working area for advisors to manage student cases efficiently in one place.</p>
    </div>

    <div class="student-layout">
      <!-- Advisee List -->
      <div class="card advisee-card">
        <div class="advisee-head">
          <div class="advisee-head-row">
            <h2 style="margin-bottom: 0; font-size: 17px">Advisee List</h2>
            <button class="btn secondary" type="button" style="font-size: 11px; padding: 4px 10px" @click="clearListFilters">
              ✕ Clear filters
            </button>
          </div>
          <div class="axis-label" style="font-weight: 600; color: #6b7280">Risk Level:</div>
          <div class="legend">
            <span><span class="legend-dot high"></span>High</span>
            <span><span class="legend-dot medium"></span>Medium</span>
            <span><span class="legend-dot low"></span>Low</span>
          </div>
        </div>
        <div style="overflow-x: auto">
          <table>
            <thead>
              <tr class="advisee-thead">
                <th style="padding: 9px 12px; font-size: 12px">Name</th>
                <th style="padding: 9px 8px">
                  <select v-model="listFilters.gpa" aria-label="Filter by GPA" @change="resetAdviseePage">
                    <option value="all">GPA ▾</option>
                    <option value="below3">Below 3.0</option>
                    <option value="above3">3.0+</option>
                  </select>
                </th>
                <th style="padding: 9px 8px">
                  <select v-model="listFilters.credits" aria-label="Filter by credits" @change="resetAdviseePage">
                    <option value="all">Credits ▾</option>
                    <option value="below70">Below 70%</option>
                    <option value="above70">70%+</option>
                  </select>
                </th>
                <th style="padding: 9px 8px">
                  <select v-model="listFilters.contact" aria-label="Filter by contact" disabled title="暂无联系记录数据源">
                    <option value="all">Contact ▾</option>
                    <option value="recent">≤ 30 days</option>
                    <option value="old">90+ days</option>
                  </select>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="student in adviseeRows"
                :key="student.studentId"
                class="sm-row"
                :class="{ selected: student.studentId === selectedStudentId }"
                style="cursor: pointer"
                @click="openStudent(student)"
              >
                <td>
                  <span class="name-cell" :class="'risk-' + student.displayLevel">{{ student.name }}</span>
                </td>
                <td style="font-size: 13px">{{ fmt(student.cumGpa, 2) }}</td>
                <td style="padding: 8px 6px">
                  <div class="mini-progress">
                    <div class="mini-progress-bar">
                      <div :style="{ width: pct(student.creditProgressPct) + '%' }"></div>
                    </div>
                    <span>{{ pct(student.creditProgressPct) }}%</span>
                  </div>
                </td>
                <td class="muted" style="font-size: 13px">—</td>
              </tr>
              <tr v-if="!adviseeRows.length">
                <td colspan="4" class="empty-cell">No students match.</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="advisee-foot">
          <span class="axis-label">{{ adviseeRange }}</span>
          <div class="pager">
            <button class="btn secondary" type="button" :disabled="adviseePage <= 1" @click="adviseePage--">‹ Prev</button>
            <span class="axis-label">Page {{ safeAdviseePage }} / {{ adviseePages }}</span>
            <button class="btn secondary" type="button" :disabled="safeAdviseePage >= adviseePages" @click="adviseePage++">
              Next ›
            </button>
          </div>
        </div>
      </div>

      <!-- Student Profile -->
      <div class="card profile-card">
        <div v-if="!profile" class="profile-empty">
          <div style="font-size: 40px; margin-bottom: 12px">Profile</div>
          <div style="font-size: 14px">Click a student to view their profile</div>
        </div>
        <div v-else>
          <div class="card-head-row" style="margin-bottom: 16px">
            <div>
              <h2 style="margin-bottom: 4px">Student Profile — {{ profile.name }}</h2>
              <div>
                <span class="badge blue">Class of {{ profile.entryYear ?? '—' }}</span>
                <span class="badge" style="background: #dbeafe; color: #1e40af; margin-left: 6px">
                  {{ profile.majorName || '—' }}
                </span>
              </div>
            </div>
          </div>

          <div class="profile-grid">
            <div>
              <h3 style="margin-bottom: 8px">Grade Trajectory</h3>
              <div class="mini-chart" style="height: 420px">
                <div ref="gradeTrajectoryRef" class="echart-box" style="height: 100%"></div>
              </div>

              <h3 style="margin-bottom: 8px">Credit Composition</h3>
              <div class="category-list">
                <div v-for="category in profile.categoryCredits ?? []" :key="category.category ?? 'unknown'" class="sub-progress">
                  <div class="credit-row">
                    <span style="font-size: 13px; color: #374151; font-weight: 500">{{ categoryLabel(category.category) }}</span>
                    <span class="blue-text" style="font-size: 13px">
                      {{ fmt(category.credits, 1) }} cr
                      <span class="muted">({{ category.courseCount ?? 0 }} courses)</span>
                    </span>
                  </div>
                  <div class="progress-line" style="height: 7px; margin: 0">
                    <div
                      class="progress-fill"
                      :style="{ width: categoryShare(category.credits) + '%', background: categoryColor(category.category) }"
                    ></div>
                  </div>
                </div>
                <div v-if="!(profile.categoryCredits ?? []).length" class="axis-label">暂无分类学分数据</div>
              </div>
            </div>
            <div>
              <h3 style="margin-bottom: 10px">Communication History</h3>
              <div class="comm-history">
                <div v-if="!hasCommunication" class="axis-label">暂无联系记录（数据库无 advisor contact log）。</div>
                <div v-for="(item, index) in communicationHistory" :key="index" class="comm-item">
                  <span class="comm-date">{{ item.date }}</span>
                  <span class="comm-text">{{ item.text }}</span>
                </div>
              </div>

              <h3 style="margin-bottom: 8px">Advisor Notes</h3>
              <textarea
                v-model="notes"
                class="notes-area"
                style="min-height: 320px"
                placeholder="记录与学生的沟通要点…"
              ></textarea>
              <div style="margin-top: 12px; display: flex; justify-content: flex-end">
                <button class="btn" type="button" style="padding: 9px 20px; font-size: 13px" @click="openOutreach(profile)">
                  Send Email
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Outreach 草稿弹窗（与原页一致：仅生成草稿，不真正发信） -->
    <div v-if="modalVisible" class="modal-mask" style="display: flex" @click.self="modalVisible = false">
      <div class="modal">
        <h2>{{ modalTitle }}</h2>
        <textarea v-model="modalText"></textarea>
        <button class="btn secondary" type="button" @click="modalVisible = false">Edit Later</button>
        <button class="btn" type="button" @click="modalVisible = false">Confirm &amp; Send</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="EduAdvisor">
import * as echarts from 'echarts'
import { getAdvisorOverview, getAdvisorStudent, getAdvisorStudents } from '@/api/education/advisor'
import { listMajor } from '@/api/education/curriculum'
import type { EduMajor } from '@/types/education'
import type {
  AdvisorCategoryAverage,
  AdvisorGpaTrendPoint,
  AdvisorOverview,
  AdvisorStudent,
  AdvisorStudentDetail,
  AdvisorStudentQuery
} from '@/types/education/advisor'

type ChartInstance = ReturnType<typeof echarts.init>
type WeightKey = 'gpa' | 'credit' | 'contact' | 'final'
type RiskTileKey = 'credit' | 'gpa' | 'contact'

/** 学生行 + 前端按权重实时重算后的展示值 */
interface AdvisorStudentView extends AdvisorStudent {
  displayScore: number
  displayLevel: string
}

/** 权重输入项（fw-gpa / fw-credit / fw-contact / fw-final 对应的四个滑块） */
interface WeightField {
  key: WeightKey
  label: string
  /** 后端规则编码（前端据此把滑块权重映射到风险原因） */
  code: string
}

/** 图表分组（按专业 / 按入学年） */
interface DistributionGroup {
  label: string
  counts: number[]
}

interface ContactFrequencyRow {
  label: string
  pct: number
  count: number
  color: string
}

interface RiskTile {
  key: RiskTileKey
  label: string
  badgeClass: string
  desc: string
  code: string
  count: number
}

interface CommunicationItem {
  date: string
  text: string
}

/* ------------------------------------------------------------------
 * 常量
 * ------------------------------------------------------------------ */

/** 毕业要求学分（后端 AdvisorRiskRules.DEFAULT_GRADUATION_CREDITS 同口径；培养方案 total_credits 暂为 0） */
const GRADUATION_CREDITS = 120

/** 学分完成度分箱（与 GPA 分布同样的 5 档） */
const CREDIT_BINS = ['0–20%', '20–40%', '40–60%', '60–80%', '80–100%']
const CREDIT_BIN_COLORS = ['#bfdbfe', '#93c5fd', '#3b82f6', '#1d4ed8', '#1e3a8a']

/** 后端规则编码 → 前端滑块 key（NO_RECENT_TERM 对应旧版 No-Contact） */
const RULE_WEIGHT_KEY: Record<string, WeightKey> = {
  GPA_LOW: 'gpa',
  CREDIT_BEHIND: 'credit',
  NO_RECENT_TERM: 'contact',
  FAILED_COURSE: 'final'
}

/** 风险原因下拉 → 规则编码 */
const REASON_CODE: Record<string, string> = {
  gpa: 'GPA_LOW',
  credit: 'CREDIT_BEHIND',
  contact: 'NO_RECENT_TERM',
  failed: 'FAILED_COURSE'
}

const CATEGORY_LABELS: Record<string, string> = {
  Major: 'Major Courses',
  GenEd: 'GenEd / U-core',
  English: 'English',
  Chinese: 'Chinese',
  IT: 'IT',
  PE: 'PE'
}

const CATEGORY_COLORS: Record<string, string> = {
  Major: '#1e3a8a',
  GenEd: '#1d4ed8',
  English: '#3b82f6',
  Chinese: '#60a5fa',
  IT: '#93c5fd',
  PE: '#bfdbfe'
}

/** 画像面板里的规则清单（未命中时也展示，保证规则透明） */
const riskRuleCatalog = [
  { code: 'GPA_LOW', label: 'GPA 偏低', note: 'cum_gpa < 2.5（< 2.0 为高危）' },
  { code: 'CREDIT_BEHIND', label: '学分进度滞后', note: '已修学分 < 应修学分 × 80%' },
  { code: 'FAILED_COURSE', label: '存在不及格课程', note: 'passed=0 或等级 F' },
  { code: 'NO_RECENT_TERM', label: '近期无成绩记录', note: '最近两个有数据的学期均无记录' }
]

const weightFields: WeightField[] = [
  // 说明：第四项在旧版是 Final-Year Urgency；本轮规则集没有 final-year 规则，
  // 因此映射到 FAILED_COURSE（保留旧版 fw-final / wi-final 的 id 以便对照）。
  { key: 'gpa', label: 'GPA Low', code: 'GPA_LOW' },
  { key: 'credit', label: 'Credit Delay', code: 'CREDIT_BEHIND' },
  { key: 'contact', label: 'No-Contact', code: 'NO_RECENT_TERM' },
  { key: 'final', label: 'Failed Course', code: 'FAILED_COURSE' }
]

/* ------------------------------------------------------------------
 * 状态
 * ------------------------------------------------------------------ */

const loading = ref<boolean>(false)
const errorMessage = ref<string>('')

const overview = ref<AdvisorOverview>({})
const students = ref<AdvisorStudent[]>([])
const studentTotal = ref<number>(0)
const majors = ref<EduMajor[]>([])

const filters = reactive({
  entryYear: '',
  majorId: '',
  riskLevel: '',
  keyword: ''
})

const listFilters = reactive({
  gpa: 'all',
  credits: 'all',
  contact: 'all'
})

/** 四个权重输入（默认 40/30/20/10，与旧版一致） */
const weights = reactive<Record<WeightKey, number>>({
  gpa: 40,
  credit: 30,
  contact: 20,
  final: 10
})

/** 高风险阈值（旧版默认 85） */
const highRiskThreshold = ref<number>(85)

/** Alert 阈值（旧版仅用于展示口径，不改变后端规则） */
const alertThresholds = reactive({
  creditPerSem: 15,
  gpaDrop: 0.3,
  noContact: 90
})

const configOpen = ref<boolean>(false)
const alertConfigOpen = ref<boolean>(false)
const distributionMode = ref<'major' | 'year'>('major')
const activeRiskFilter = ref<'all' | RiskTileKey>('all')
const reasonFilter = ref<string>('all')

const selectedStudentId = ref<number | null>(null)
const profile = ref<AdvisorStudentDetail | null>(null)
const notes = ref<string>('')

const modalVisible = ref<boolean>(false)
const modalTitle = ref<string>('')
const modalText = ref<string>('')

const adviseePage = ref<number>(1)
const ADVISE_PAGE_SIZE = 6

const entryYearOptions = ref<number[]>([])

/* 图表容器 */
const gpaDistRef = ref<HTMLDivElement | null>(null)
const gpaTrendRef = ref<HTMLDivElement | null>(null)
const distributionRef = ref<HTMLDivElement | null>(null)
const profileGpaRef = ref<HTMLDivElement | null>(null)
const gradeTrajectoryRef = ref<HTMLDivElement | null>(null)

let gpaDistChart: ChartInstance | null = null
let gpaTrendChart: ChartInstance | null = null
let distributionChart: ChartInstance | null = null
let profileGpaChart: ChartInstance | null = null
let gradeTrajectoryChart: ChartInstance | null = null
let searchTimer: ReturnType<typeof setTimeout> | null = null

/* ------------------------------------------------------------------
 * 计算属性
 * ------------------------------------------------------------------ */

const weightTotal = computed<number>(
  () => Number(weights.gpa || 0) + Number(weights.credit || 0) + Number(weights.contact || 0) + Number(weights.final || 0)
)

/** 按前端权重实时重算风险分与等级（不再请求后端） */
function scoreFor(student: AdvisorStudent): number {
  const total = weightTotal.value
  if (total <= 0) return 0
  let hit = 0
  for (const reason of student.riskReasons ?? []) {
    const key = reason.code ? RULE_WEIGHT_KEY[reason.code] : undefined
    if (key) {
      // 后端返回的基础权重（默认 1.0）参与展示层加权，便于后端调参后仍然一致
      hit += weights[key] * (reason.weight ?? 1)
    }
  }
  return Math.round((hit * 100) / total)
}

function levelFor(score: number): string {
  if (score >= 60) return 'high'
  if (score >= 30) return 'medium'
  return 'low'
}

const enrichedStudents = computed<AdvisorStudentView[]>(() =>
  students.value.map((student) => {
    const displayScore = scoreFor(student)
    return { ...student, displayScore, displayLevel: levelFor(displayScore) }
  })
)

/** 风险等级分布：优先用当前列表实时重算，未加载时回退后端总览 */
const riskCounts = computed<{ high: number; medium: number; low: number }>(() => {
  if (enrichedStudents.value.length > 0) {
    const counts = { high: 0, medium: 0, low: 0 }
    for (const student of enrichedStudents.value) {
      if (student.displayLevel === 'high') counts.high++
      else if (student.displayLevel === 'medium') counts.medium++
      else counts.low++
    }
    return counts
  }
  return {
    high: overview.value.risk?.high ?? 0,
    medium: overview.value.risk?.medium ?? 0,
    low: overview.value.risk?.low ?? 0
  }
})

const avgCompletionPct = computed<number>(() => {
  if (!enrichedStudents.value.length) return 0
  const sum = enrichedStudents.value.reduce((acc, student) => acc + (student.creditProgressPct ?? 0), 0)
  return Math.round(sum / enrichedStudents.value.length)
})

const categoryRows = computed<AdvisorCategoryAverage[]>(() => overview.value.categoryBreakdown ?? [])

const maxCategoryCredits = computed<number>(() => {
  let max = 0
  for (const row of categoryRows.value) {
    max = Math.max(max, row.avgCredits ?? 0)
  }
  return max > 0 ? max : 1
})

const categoryTotal = computed<number>(() =>
  categoryRows.value.reduce((acc, row) => acc + (row.avgCredits ?? 0), 0)
)

const contactFrequency = computed<ContactFrequencyRow[]>(() => [
  // 暂无联系记录表，三项均为空态；接入后按真实记录聚合
  { label: 'No contact', pct: 0, count: 0, color: '#ef4444' },
  { label: '1–2 contacts', pct: 0, count: 0, color: '#3b82f6' },
  { label: '3+ contacts', pct: 0, count: 0, color: '#1d4ed8' }
])

/** 风险指标磁贴：按规则编码统计命中人数 */
const riskTiles = computed<RiskTile[]>(() => {
  const countByCode = (code: string): number =>
    enrichedStudents.value.filter((student) => (student.riskReasons ?? []).some((r) => r.code === code)).length
  return [
    {
      key: 'credit',
      label: 'Credit Alert',
      badgeClass: 'orange-badge',
      desc: 'Falling behind expected credit progress.',
      code: 'CREDIT_BEHIND',
      count: countByCode('CREDIT_BEHIND')
    },
    {
      key: 'gpa',
      label: 'GPA Drop',
      badgeClass: 'yellow',
      desc: 'Significant GPA decline this semester.',
      code: 'GPA_LOW',
      count: countByCode('GPA_LOW')
    },
    {
      key: 'contact',
      label: 'No Contact',
      badgeClass: 'blue',
      desc: 'Have not met their advisor recently.',
      code: 'NO_RECENT_TERM',
      count: countByCode('NO_RECENT_TERM')
    }
  ]
})

const priorityTitle = computed<string>(() => {
  const map: Record<RiskTileKey, string> = {
    credit: 'Credit Alert',
    gpa: 'GPA Drop',
    contact: 'No Contact'
  }
  return activeRiskFilter.value === 'all'
    ? 'Risk Indicators & Priority Students'
    : 'Priority Students — ' + map[activeRiskFilter.value]
})

/** 按风险磁贴 / 原因下拉过滤后的优先学生（按动态风险分降序） */
const priorityRows = computed<AdvisorStudentView[]>(() => {
  let list = enrichedStudents.value.slice()
  if (activeRiskFilter.value !== 'all') {
    const tile = riskTiles.value.find((item) => item.key === activeRiskFilter.value)
    const code = tile?.code
    if (code) list = list.filter((student) => (student.riskReasons ?? []).some((r) => r.code === code))
  }
  if (reasonFilter.value !== 'all') {
    const code = REASON_CODE[reasonFilter.value]
    if (code) list = list.filter((student) => (student.riskReasons ?? []).some((r) => r.code === code))
  }
  return list.sort((a, b) => b.displayScore - a.displayScore)
})

/** 高风险名单：展示层风险分 ≥ 阈值 */
const highRiskRows = computed<AdvisorStudentView[]>(() => {
  const threshold = Number(highRiskThreshold.value || 0)
  return enrichedStudents.value
    .filter((student) => student.displayScore >= threshold)
    .sort((a, b) => b.displayScore - a.displayScore)
})

/** 建议名单（GPA / 学分 / 风险等级过滤） */
const filteredStudents = computed<AdvisorStudentView[]>(() => {
  let list = enrichedStudents.value
  if (filters.riskLevel) list = list.filter((student) => student.displayLevel === filters.riskLevel)
  if (listFilters.gpa === 'below3') list = list.filter((student) => (student.cumGpa ?? 0) < 3)
  else if (listFilters.gpa === 'above3') list = list.filter((student) => (student.cumGpa ?? 0) >= 3)
  if (listFilters.credits === 'below70') list = list.filter((student) => (student.creditProgressPct ?? 0) < 70)
  else if (listFilters.credits === 'above70') list = list.filter((student) => (student.creditProgressPct ?? 0) >= 70)
  return list
})

const adviseePages = computed<number>(() => Math.max(1, Math.ceil(filteredStudents.value.length / ADVISE_PAGE_SIZE)))
const safeAdviseePage = computed<number>(() => Math.min(Math.max(adviseePage.value, 1), adviseePages.value))
const adviseeRows = computed<AdvisorStudentView[]>(() => {
  const start = (safeAdviseePage.value - 1) * ADVISE_PAGE_SIZE
  return filteredStudents.value.slice(start, start + ADVISE_PAGE_SIZE)
})
const adviseeRange = computed<string>(() => {
  const total = filteredStudents.value.length
  if (!total) return '0 students'
  const from = (safeAdviseePage.value - 1) * ADVISE_PAGE_SIZE + 1
  const to = Math.min(from + ADVISE_PAGE_SIZE - 1, total)
  return `${from}-${to} / ${total} students (total ${studentTotal.value})`
})

const hasCommunication = computed<boolean>(() => false)
const communicationHistory = computed<CommunicationItem[]>(() => [])

/* ------------------------------------------------------------------
 * 展示辅助
 * ------------------------------------------------------------------ */

function fmt(value: number | null | undefined, digits = 1): string {
  if (value === null || value === undefined || !Number.isFinite(value)) return '—'
  return value.toFixed(digits)
}

function pct(value: number | null | undefined): number {
  if (value === null || value === undefined || !Number.isFinite(value)) return 0
  return Math.round(value)
}

function termLabel(termCode?: string): string {
  if (!termCode) return ''
  const match = /^(\d{4})-(\d{2})-(\d+)$/.exec(termCode)
  if (!match) return termCode
  return match[1].slice(-2) + '-' + match[2] + ' T' + match[3]
}

function majorLabel(major: EduMajor): string {
  return major.majorNameZh || major.majorNameEn || major.majorCode || String(major.majorId ?? '')
}

function majorShort(name?: string): string {
  if (!name) return 'Unknown'
  const found = majors.value.find((major) => major.majorNameZh === name || major.majorNameEn === name)
  if (found?.majorCode) return found.majorCode
  return name.length > 12 ? name.slice(0, 11) + '…' : name
}

function categoryLabel(category?: string): string {
  if (!category) return 'Other'
  return CATEGORY_LABELS[category] ?? category
}

function categoryColor(category?: string): string {
  return (category && CATEGORY_COLORS[category]) || '#93c5fd'
}

function categoryPercent(row: AdvisorCategoryAverage): number {
  const value = row.avgCredits ?? 0
  return Math.round((value / maxCategoryCredits.value) * 100)
}

function categoryShare(credits?: number): number {
  const value = credits ?? 0
  const max = Math.max(1, ...(profile.value?.categoryCredits ?? []).map((item) => item.credits ?? 0))
  return Math.round((value / max) * 100)
}

function riskBadgeClass(level?: string): string {
  if (level === 'high') return 'red'
  if (level === 'medium') return 'orange-badge'
  return 'green-badge'
}

function riskLevelLabel(level?: string): string {
  if (level === 'high') return 'High'
  if (level === 'medium') return 'Medium'
  return 'Low'
}

function reasonText(student: AdvisorStudentView | AdvisorStudentDetail): string {
  const reasons = student.riskReasons ?? []
  if (!reasons.length) return 'No risk signals'
  return reasons.map((reason) => reason.label || reason.code || '').filter(Boolean).join(' + ')
}

function hasReason(code: string): boolean {
  return (profile.value?.riskReasons ?? []).some((reason) => reason.code === code)
}

function reasonDetail(code: string): string {
  return (profile.value?.riskReasons ?? []).find((reason) => reason.code === code)?.detail ?? ''
}

/* ------------------------------------------------------------------
 * 数据加载
 * ------------------------------------------------------------------ */

function describeError(error: unknown): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'string') return error
  return ''
}

function buildStudentQuery(): AdvisorStudentQuery {
  const query: AdvisorStudentQuery = { pageNum: 1, pageSize: 200 }
  if (filters.entryYear) query.entryYear = Number(filters.entryYear)
  if (filters.majorId) query.majorId = Number(filters.majorId)
  if (filters.keyword.trim()) query.keyword = filters.keyword.trim()
  return query
}

async function loadOverview(): Promise<void> {
  const response = await getAdvisorOverview()
  overview.value = response.data ?? {}
  await nextTick()
  renderGpaDistribution()
  renderGpaTrend()
}

async function loadStudents(): Promise<void> {
  const response = await getAdvisorStudents(buildStudentQuery())
  students.value = response.rows ?? []
  studentTotal.value = response.total ?? students.value.length
  adviseePage.value = 1
  if (!entryYearOptions.value.length) {
    entryYearOptions.value = Array.from(
      new Set(students.value.map((student) => student.entryYear).filter((year): year is number => !!year))
    ).sort((a, b) => a - b)
  }
  await nextTick()
  renderDistribution()
  // 初次进入时默认选中第一名学生，填充画像面板（旧版需手动点击）
  if (!selectedStudentId.value && students.value.length && students.value[0].studentId) {
    await openStudent(students.value[0])
  }
}

async function loadMajors(): Promise<void> {
  const response = await listMajor({ pageNum: 1, pageSize: 100 })
  majors.value = response.rows ?? []
}

async function reload(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    await Promise.all([loadOverview(), loadStudents()])
  } catch (error) {
    errorMessage.value = describeError(error) || '导师看板数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function loadProfile(studentId: number): Promise<void> {
  const response = await getAdvisorStudent(studentId)
  profile.value = response.data ?? null
  notes.value = ''
  await nextTick()
  renderProfileGpaTrend()
  renderGradeTrajectory()
}

/* ------------------------------------------------------------------
 * 交互
 * ------------------------------------------------------------------ */

function openStudent(student: { studentId?: number }): void {
  if (!student.studentId) return
  selectedStudentId.value = student.studentId
  loadProfile(student.studentId).catch((error) => {
    errorMessage.value = describeError(error) || '学生画像加载失败'
  })
}

function closeProfile(): void {
  selectedStudentId.value = null
  profile.value = null
}

function onSearch(): void {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    loadStudents().catch((error) => {
      errorMessage.value = describeError(error) || '学生列表加载失败'
    })
  }, 350)
}

/** 下拉筛选变更时的统一入口（吞掉 Promise 异常，避免 unhandled rejection） */
function reloadStudents(): void {
  loadStudents().catch((error) => {
    errorMessage.value = describeError(error) || '学生列表加载失败'
  })
}

function setDistributionMode(mode: 'major' | 'year'): void {
  if (distributionMode.value === mode) return
  distributionMode.value = mode
  nextTick(() => renderDistribution())
}

function toggleRiskTile(key: RiskTileKey): void {
  activeRiskFilter.value = activeRiskFilter.value === key ? 'all' : key
}

function clearRiskFilters(): void {
  activeRiskFilter.value = 'all'
  reasonFilter.value = 'all'
}

function resetAdviseePage(): void {
  adviseePage.value = 1
}

function clearFilters(): void {
  filters.entryYear = ''
  filters.majorId = ''
  filters.riskLevel = ''
  filters.keyword = ''
  loadStudents().catch((error) => {
    errorMessage.value = describeError(error) || '学生列表加载失败'
  })
}

function clearListFilters(): void {
  listFilters.gpa = 'all'
  listFilters.credits = 'all'
  listFilters.contact = 'all'
  adviseePage.value = 1
}

function resetWeights(): void {
  weights.gpa = 40
  weights.credit = 30
  weights.contact = 20
  weights.final = 10
}

function openOutreach(target: AdvisorStudent | AdvisorStudentDetail | null): void {
  if (!target) return
  const name = target.name || 'Student'
  modalTitle.value = 'Outreach Draft to ' + name
  const reason = reasonText(target)
  const topic = reason === 'No risk signals' ? 'your recent academic record' : reason
  modalText.value =
    'Dear ' + name.split(' ')[0] + ',\n\n' +
    'I noticed some recent academic changes related to ' + topic + '. ' +
    'I would like to schedule a short meeting with you this week to better understand your situation and discuss how I may support your academic planning.\n\n' +
    'Please let me know your availability.\n\n' +
    'Best regards,\n' +
    'Academic Advisor'
  modalVisible.value = true
}

/* ------------------------------------------------------------------
 * 图表（ECharts；配色对齐原 Chart.js 观感）
 * ------------------------------------------------------------------ */

function gpaRange(values: number[]): { min: number; max: number } {
  if (!values.length) return { min: 0, max: 4 }
  let min = Math.max(0, Math.floor((Math.min(...values) - 0.3) * 2) / 2)
  let max = Math.min(4.1, Math.ceil((Math.max(...values) + 0.3) * 2) / 2)
  if (max - min < 0.6) {
    const mid = (min + max) / 2
    min = mid - 0.5
    max = mid + 0.5
  }
  return { min, max }
}

const AXIS_LABEL_STYLE = { color: '#6b7280', fontSize: 11 }
const SPLIT_LINE_STYLE = { lineStyle: { color: '#f3f4f6' } }

function renderGpaDistribution(): void {
  const container = gpaDistRef.value
  if (!container) return
  gpaDistChart?.dispose()
  gpaDistChart = echarts.init(container)
  const buckets = overview.value.gpaBuckets ?? []
  gpaDistChart.setOption({
    animationDuration: 300,
    grid: { left: 4, right: 8, top: 20, bottom: 2, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const list = params as Array<{ name: string; value: number }>
        return list.length ? list[0].name + '：' + list[0].value + ' students' : ''
      }
    },
    xAxis: {
      type: 'category',
      data: buckets.map((bucket) => bucket.label ?? ''),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#d1d5db' } },
      axisLabel: { ...AXIS_LABEL_STYLE, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: SPLIT_LINE_STYLE,
      axisLabel: { ...AXIS_LABEL_STYLE }
    },
    series: [
      {
        type: 'bar',
        data: buckets.map((bucket) => bucket.count ?? 0),
        barWidth: 26,
        itemStyle: { color: '#3b82f6', borderRadius: [6, 6, 0, 0] },
        label: { show: true, position: 'top', color: '#1e3a8a', fontSize: 10, fontWeight: 'bold' }
      }
    ]
  })
}

function renderGpaTrend(): void {
  const container = gpaTrendRef.value
  if (!container) return
  gpaTrendChart?.dispose()
  gpaTrendChart = echarts.init(container)
  const points = overview.value.gpaTrend ?? []
  const values = points.map((point) => point.avgGpa ?? 0)
  const range = gpaRange(values)
  gpaTrendChart.setOption({
    animationDuration: 300,
    grid: { left: 2, right: 10, top: 12, bottom: 2, containLabel: true },
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value: unknown) => (typeof value === 'number' ? value.toFixed(2) : String(value ?? ''))
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((point) => termLabel(point.termCode)),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#d1d5db' } },
      axisLabel: { ...AXIS_LABEL_STYLE, fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      min: range.min,
      max: range.max,
      interval: 0.2,
      splitLine: SPLIT_LINE_STYLE,
      axisLabel: { ...AXIS_LABEL_STYLE, formatter: (value: number | string) => Number(value).toFixed(1) }
    },
    series: [
      {
        name: 'Avg GPA',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: values,
        itemStyle: { color: '#3b82f6' },
        lineStyle: { width: 3, color: '#3b82f6' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59,130,246,0.22)' },
            { offset: 1, color: 'rgba(59,130,246,0.02)' }
          ])
        }
      }
    ]
  })
}

function binIndex(value?: number): number {
  const pctValue = value ?? 0
  if (pctValue < 20) return 0
  if (pctValue < 40) return 1
  if (pctValue < 60) return 2
  if (pctValue < 80) return 3
  return 4
}

function distributionGroups(mode: 'major' | 'year'): DistributionGroup[] {
  const map = new Map<string, number[]>()
  for (const student of students.value) {
    const label = mode === 'major' ? majorShort(student.majorName) : String(student.entryYear ?? 'Unknown')
    let counts = map.get(label)
    if (!counts) {
      counts = [0, 0, 0, 0, 0]
      map.set(label, counts)
    }
    counts[binIndex(student.creditProgressPct)]++
  }
  return Array.from(map.entries())
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([label, counts]) => ({ label, counts }))
}

function renderDistribution(): void {
  const container = distributionRef.value
  if (!container) return
  distributionChart?.dispose()
  distributionChart = echarts.init(container)
  const groups = distributionGroups(distributionMode.value)
  distributionChart.setOption({
    animationDuration: 300,
    grid: { left: 2, right: 12, top: 18, bottom: 34, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: {
      bottom: 0,
      itemWidth: 9,
      itemHeight: 8,
      textStyle: { fontSize: 10, color: '#6b7280' }
    },
    xAxis: {
      type: 'category',
      data: groups.map((group) => group.label),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#d1d5db', width: 1.5 } },
      axisLabel: { color: '#374151', fontSize: 10, fontWeight: 'bold' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f4ff' } },
      axisLabel: { color: '#9ca3af', fontSize: 9 }
    },
    series: CREDIT_BINS.map((label, index) => ({
      name: label,
      type: 'bar' as const,
      data: groups.map((group) => group.counts[index]),
      barGap: '10%',
      itemStyle: { color: CREDIT_BIN_COLORS[index], borderRadius: [2, 2, 0, 0] },
      label: {
        show: true,
        position: 'top' as const,
        fontSize: 8,
        fontWeight: 'bold' as const,
        color: '#1e3a8a',
        formatter: (params: unknown) => {
          const item = params as { value: number }
          return item.value > 0 ? String(item.value) : ''
        }
      }
    }))
  })
}

function renderStudentLine(
  container: HTMLDivElement | null,
  chart: ChartInstance | null,
  points: AdvisorGpaTrendPoint[],
  cgpa: number | null
): ChartInstance | null {
  if (!container) return chart
  chart?.dispose()
  const instance = echarts.init(container)
  const values = points.map((point) => point.avgGpa ?? 0)
  const range = gpaRange(values)
  const series: any[] = [
    {
      name: 'Term GPA',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      data: values,
      itemStyle: { color: '#f97316' },
      lineStyle: { width: 3, color: '#f97316' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(249,115,22,0.16)' },
          { offset: 1, color: 'rgba(249,115,22,0.02)' }
        ])
      }
    }
  ]
  if (cgpa !== null) {
    series.push({
      name: 'CGPA (' + cgpa.toFixed(2) + ')',
      type: 'line',
      symbol: 'none',
      data: points.map(() => cgpa),
      lineStyle: { width: 2, type: 'dashed', color: '#08306b' },
      itemStyle: { color: '#08306b' }
    })
  }
  instance.setOption({
    animationDuration: 300,
    grid: { left: 2, right: 10, top: 14, bottom: 26, containLabel: true },
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value: unknown) => (typeof value === 'number' ? value.toFixed(2) : String(value ?? ''))
    },
    legend: {
      bottom: 0,
      itemWidth: 16,
      itemHeight: 2,
      textStyle: { fontSize: 10, color: '#374151' }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((point) => termLabel(point.termCode)),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#d1d5db' } },
      axisLabel: { ...AXIS_LABEL_STYLE, fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      min: range.min,
      max: range.max,
      interval: 0.2,
      splitLine: SPLIT_LINE_STYLE,
      axisLabel: { ...AXIS_LABEL_STYLE, formatter: (value: number | string) => Number(value).toFixed(1) }
    },
    series
  })
  return instance
}

function renderProfileGpaTrend(): void {
  if (!profile.value) return
  profileGpaChart = renderStudentLine(
    profileGpaRef.value,
    profileGpaChart,
    profile.value.gpaTrend ?? [],
    profile.value.cumGpa ?? null
  )
}

function renderGradeTrajectory(): void {
  if (!profile.value) return
  gradeTrajectoryChart = renderStudentLine(
    gradeTrajectoryRef.value,
    gradeTrajectoryChart,
    profile.value.gpaTrend ?? [],
    profile.value.cumGpa ?? null
  )
}

function resizeCharts(): void {
  gpaDistChart?.resize()
  gpaTrendChart?.resize()
  distributionChart?.resize()
  profileGpaChart?.resize()
  gradeTrajectoryChart?.resize()
}

/* ------------------------------------------------------------------
 * 生命周期
 * ------------------------------------------------------------------ */

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  loadMajors().catch(() => undefined)
  reload()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  if (searchTimer) clearTimeout(searchTimer)
  gpaDistChart?.dispose()
  gpaTrendChart?.dispose()
  distributionChart?.dispose()
  profileGpaChart?.dispose()
  gradeTrajectoryChart?.dispose()
  gpaDistChart = null
  gpaTrendChart = null
  distributionChart = null
  profileGpaChart = null
  gradeTrajectoryChart = null
})
</script>

<style scoped src="../../../assets/student/advisor.css"></style>

<style scoped>
/* 少量补充样式（原页大量内联样式在模板里保留） */
.advisor-page {
  padding-bottom: 24px;
}

/* .lg-head 的主题样式在 education.css 里（外壳全局引入）；这里做一份兜底，避免样式加载顺序变化 */
.advisor-page .lg-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin: 4px 0 16px;
  flex-wrap: wrap;
}

.advisor-page .lg-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #1a1d2e;
  letter-spacing: -0.01em;
}

.advisor-page .lg-head p {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: #5a6076;
}

.advisor-page .lg-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.advisor-error {
  background: #fee2e2;
  border: 1px solid #fca5a5;
  color: #b91c1c;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 13px;
  margin-bottom: 16px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.overview-metric {
  margin-bottom: 0;
  padding: 16px 18px;
}

.overview-metric .metric-value {
  font-size: 28px;
  line-height: 1.2;
}

.risk-badges .badge {
  margin-right: 4px;
}

.advisor-filters {
  align-items: center;
}

.advisor-filters .btn {
  margin-right: 0;
}

.filter-hint {
  font-size: 13px;
  color: #9ca3af;
  align-self: center;
}

.overview-row {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 22px;
  margin-bottom: 22px;
}

.overview-row > .card {
  margin-bottom: 0;
}

.progress-overview-card {
  padding: 0;
  overflow: hidden;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px 14px;
  border-bottom: 1px solid #f0f4ff;
}

.progress-head-metric {
  text-align: right;
}

.progress-head-value {
  display: flex;
  align-items: baseline;
  gap: 8px;
  justify-content: flex-end;
}

.big-blue {
  font-size: 32px;
  font-weight: 800;
  color: #1d4ed8;
  line-height: 1;
}

.progress-sub {
  padding: 14px 20px;
  border-bottom: 1px solid #f0f4ff;
}

.progress-sub-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.sub-title {
  font-size: 12px;
  font-weight: 700;
  color: #1e3a8a;
}

.btn-group {
  display: flex;
  gap: 5px;
}

.category-head,
.category-row {
  display: grid;
  grid-template-columns: 130px 1fr 80px;
  gap: 8px;
  align-items: center;
}

.category-head {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 5px;
}

.category-row {
  margin: 7px 0;
}

.category-name {
  font-size: 12px;
  color: #374151;
  font-weight: 500;
}

.category-bar {
  position: relative;
  height: 15px;
  background: #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.category-bar-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  border-radius: 8px;
}

.category-bar-text {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 10px;
  font-weight: 700;
  color: #fff;
}

.category-credits {
  font-size: 11px;
  color: #6b7280;
  text-align: right;
}

.category-total {
  text-align: right;
  font-size: 11px;
  color: #9ca3af;
  margin-top: 6px;
}

.comm-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  align-items: start;
}

.comm-big {
  font-size: 40px;
  font-weight: 700;
  color: #1d4ed8;
  line-height: 1.1;
}

.comm-row {
  margin-top: 12px;
}

.comm-row-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 4px;
}

.advisor-note-box {
  margin-top: 14px;
  background: #f8faff;
  border: 1px dashed #c7d7fb;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.6;
}

.section-head {
  margin: 4px 0 14px;
}

.section-head h2 {
  font-size: 20px;
  margin-bottom: 4px;
}

.section-head p {
  color: #6b7280;
  font-size: 13.5px;
  margin: 0;
}

.high-risk-card {
  border-left: 4px solid #b91c1c;
}

.card-head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.head-actions-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.config-panel {
  background: #f8faff;
  border: 1px solid #c7d7fb;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.config-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.formula {
  font-size: 12px;
  font-family: monospace;
  color: #1e3a8a;
  background: #eff6ff;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 14px;
  line-height: 2;
}

.config-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.weight-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.weight-box {
  background: #fff;
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  padding: 10px;
}

.weight-input-row {
  display: flex;
  align-items: center;
  gap: 3px;
}

.weight-total {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.warn {
  font-size: 11px;
  color: #b91c1c;
}

.threshold-box {
  background: #fff1f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  padding: 12px;
}

.table-scroll {
  overflow-y: auto;
  border: 1px solid #fecaca;
  border-radius: 8px;
}

.sticky-head {
  position: sticky;
  top: 0;
  background: #fff5f5;
  z-index: 1;
}

.tiles {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}

.risk-tile {
  cursor: pointer;
  border-radius: 12px;
  padding: 14px;
  border: 2px solid #e5e7eb;
  background: #fff;
  transition: background 0.15s, border-color 0.15s;
}

.risk-tile strong {
  display: block;
  font-size: 18px;
  margin: 8px 0 4px;
}

.risk-tile p {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

.risk-tile.active {
  background: #eff6ff;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px #3b82f655;
}

.reason-filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.alert-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.alert-box {
  border-radius: 10px;
  padding: 12px;
  border: 1px solid #fed7aa;
  background: #fff7ed;
}

.alert-box.alert-gpa {
  border-color: #fde68a;
  background: #fefce8;
}

.alert-box.alert-contact {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.alert-title {
  font-size: 12px;
  font-weight: 600;
  color: #92400e;
  margin-bottom: 8px;
}

.alert-line {
  font-size: 12px;
  color: #374151;
  line-height: 1.9;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;
}

.mini-chart {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f9fafb;
  padding: 10px;
  margin-bottom: 16px;
}

.profile-panel {
  scroll-margin-top: 64px;
}

.credit-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 4px;
}

.risk-detail {
  font-size: 13px;
}

.risk-rule-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 8px 0;
  border-bottom: 1px dashed #e5e7eb;
}

.risk-rule-row .axis-label {
  line-height: 1.5;
}

.comm-history {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 16px;
  background: #fafafa;
  min-height: 70px;
}

.comm-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 8px;
}

.comm-date {
  background: #dbeafe;
  color: #1e40af;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
  margin-top: 1px;
}

.comm-text {
  font-size: 13px;
  color: #374151;
}

.notes-area {
  width: 100%;
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px;
  resize: vertical;
}

.student-layout {
  display: grid;
  grid-template-columns: 35% 65%;
  gap: 20px;
  align-items: start;
}

.advisee-card {
  padding: 0;
  overflow: hidden;
}

.advisee-head {
  padding: 14px 16px 8px;
}

.advisee-head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.legend {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 4px;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
  margin-right: 4px;
}

.legend-dot.high {
  background: #fee2e2;
  border: 1px solid #fca5a5;
}

.legend-dot.medium {
  background: #ffedd5;
  border: 1px solid #fdba74;
}

.legend-dot.low {
  background: #dcfce7;
  border: 1px solid #86efac;
}

.advisee-thead {
  background: #f8faff;
  border-bottom: 1px solid #e5e7eb;
}

.advisee-thead th {
  padding: 9px 8px;
}

.advisee-thead select {
  font-size: 11px;
  padding: 2px 5px;
  border-radius: 6px;
  width: 100%;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.name-cell {
  display: inline-block;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 6px;
}

.risk-high {
  background: #fee2e2;
  color: #991b1b;
}

.risk-medium {
  background: #ffedd5;
  color: #9a3412;
}

.risk-low {
  background: #dcfce7;
  color: #166534;
}

.mini-progress {
  display: flex;
  align-items: center;
  gap: 5px;
}

.mini-progress-bar {
  flex: 1;
  height: 6px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.mini-progress-bar > div {
  height: 100%;
  background: #1d4ed8;
  border-radius: 4px;
}

.sm-row:hover {
  background: #f0f9ff !important;
}

.sm-row.selected {
  background: #eff6ff !important;
}

.advisee-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-top: 1px solid #e5e7eb;
}

.pager {
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #9ca3af;
}

.empty-cell {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 18px 10px;
}

.muted {
  color: #9ca3af;
  font-weight: 400;
}

.blue-text {
  color: #1d4ed8;
  font-weight: 600;
}

.sub-progress {
  margin-bottom: 12px;
}

.category-list {
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .overview-row,
  .config-grid,
  .alert-grid,
  .profile-grid,
  .student-layout,
  .tiles,
  .comm-grid {
    grid-template-columns: 1fr;
  }

  .category-head,
  .category-row {
    grid-template-columns: 110px 1fr 70px;
  }
}
</style>
