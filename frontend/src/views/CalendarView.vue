<template>
  <div class="calendar-page">
    <div class="calendar-layout">
      <!-- 좌측: 캘린더 -->
      <section class="calendar-section" :class="{ loading: calendarLoading }">
        <div v-if="calendarLoading" class="loading-overlay"><DotLoader /></div>
        <!-- 월 네비게이션 -->
        <div class="month-nav">
          <button class="nav-btn" @click="prevMonth">‹</button>
          <h2 class="month-title">
            <span class="month-num">{{ currentMonth }}월</span>
            <span class="month-year">{{ currentYear }}</span>
          </h2>
          <button class="nav-btn" @click="nextMonth">›</button>
        </div>

        <!-- 요일 헤더 -->
        <div class="weekday-row">
          <div v-for="d in weekDays" :key="d" class="weekday">{{ d }}</div>
        </div>

        <!-- 날짜 그리드 -->
        <div ref="gridEl" class="day-grid">
          <!-- 연속으로 회고를 쓴 날의 점끼리 잇는 선 (주가 바뀌면 줄바꿈하듯 오른쪽 끝 → 다음 줄 왼쪽에서 이어짐) -->
          <svg class="dot-lines" :width="gridSize.w" :height="gridSize.h" aria-hidden="true">
            <path
              v-for="(seg, i) in lineSegments"
              :key="seg.key"
              class="dot-line"
              :d="seg.d"
              pathLength="1"
              :style="{ animationDelay: `${i * 50}ms` }"
            />
          </svg>
          <div v-for="n in firstDayOffset" :key="`e${n}`" class="day-cell empty" />
          <div
            v-for="day in daysInMonth"
            :key="day"
            class="day-cell"
            :class="{ today: isToday(day), 'has-data': getDayData(day), future: isFutureDay(day), locked: isLockedDay(day) }"
            :title="getDayData(day) ? `만족도 ${getDayData(day).score}점` : ''"
            @click="goToDate(day)"
          >
            <span class="day-num">{{ day }}</span>
            <!-- 회고를 쓴 날엔 잉크 점: 만족도가 높을수록 크고 진하게 -->
            <span
              v-if="getDayData(day)"
              :ref="el => setDotRef(day, el)"
              class="ink-dot"
              :class="{ stamp: stampDay === day }"
              :style="dotStyle(getDayData(day).score)"
            />
          </div>
        </div>

        <p v-if="!calendarLoading && calendarData.length === 0 && !isFutureMonth" class="empty-hint">
          {{ isCurrentMonth ? '이번 달 첫 번째 점을 찍어보세요.' : '이 달엔 찍힌 점이 없어요.' }}
        </p>

        <!-- 점수 범례 -->
        <div class="legend">
          <span class="legend-label">만족도</span>
          <div class="legend-items">
            <span class="legend-end">1</span>
            <span v-for="n in 5" :key="n" class="ink-dot" :style="dotStyle(n, 'sm')" />
            <span class="legend-end">5</span>
          </div>
        </div>

        <!-- 오늘 회고 작성 버튼 -->
        <button class="btn-today" @click="goToToday">
          {{ todayWritten ? '오늘 회고 보기' : '오늘의 점 찍기' }}
        </button>
      </section>

      <!-- 우측: 사이드바 -->
      <aside class="sidebar">
        <!-- 월 통계 -->
        <div class="sidebar-card">
          <h3 class="card-title"><Icon name="bar-chart" /> 이번 달 통계</h3>
          <div class="stats-row">
            <div class="stat">
              <div class="stat-value">{{ stats.count }}</div>
              <div class="stat-label">작성 수</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ stats.avgScore }}</div>
              <div class="stat-label">평균 점수</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ stats.rate }}%</div>
              <div class="stat-label">작성률</div>
            </div>
          </div>
        </div>

        <!-- 최근 회고 -->
        <div class="sidebar-card">
          <h3 class="card-title"><Icon name="document" /> 최근 회고</h3>
          <div v-if="recentRetros.length === 0" class="empty-msg">아직 찍힌 점이 없어요. 첫 번째 점을 찍어보세요.</div>
          <div v-for="r in recentRetros" :key="r.id" class="recent-item" @click="$router.push(`/retrospect/${r.date}`)">
            <div class="recent-date">{{ formatDate(r.date) }}</div>
            <div class="recent-score">
              <span class="ink-dot" :style="dotStyle(r.score, 'sm')" />
              {{ r.score }}점
            </div>
          </div>
        </div>

        <!-- AI 인사이트 -->
        <div class="sidebar-card ai-card">
          <h3 class="card-title"><Icon name="sparkle" /> AI 인사이트</h3>
          <p v-if="!aiInsight && !aiLoading && stats.count < 3" class="empty-msg">이 달에 회고를 3개 이상 쓰면 AI 인사이트를 받을 수 있어요</p>
          <div v-if="aiLoading" class="ai-loading"><DotLoader label="기록을 돌아보는 중" :size="11" /></div>
          <p v-if="aiInsight" class="ai-text">{{ aiInsight }}</p>
          <button
            v-if="stats.count >= 3 && !aiInsight"
            class="btn-ai"
            @click="getInsight"
            :disabled="aiLoading || insightLeft === 0"
          >
            {{ insightLeft === 0 ? '오늘 인사이트를 다 썼어요' : '인사이트 받기' }}
          </button>
          <p v-if="aiQuota.INSIGHT" class="quota-text">
            오늘 남은 횟수 {{ aiQuota.INSIGHT.remaining }}/{{ aiQuota.INSIGHT.limit }}
            <template v-if="insightLeft === 0"> · 내일 자정에 다시 채워져요</template>
          </p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { retrospectApi } from '@/api/retrospect'
import { aiApi } from '@/api/ai'
import Icon from '@/components/Icon.vue'
import DotLoader from '@/components/DotLoader.vue'
import { calendarCache } from '@/utils/calendarCache'
import { dotStyle } from '@/utils/inkDot'
import { modal } from '@/composables/useModal'
import { aiQuota, refreshAiQuota, updateAiQuota, markQuotaExhausted, joinItems } from '@/composables/useAiQuota'

const router = useRouter()
const route = useRoute()
const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1)

// 회고 페이지에서 "저장"하고 넘어오면(?stamp=YYYY-MM-DD) 그 날짜의 달을 열고 점이 "톡" 찍히는 효과를 보여줌
const stampDay = ref(null)
const stampDate = typeof route.query.stamp === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(route.query.stamp)
  ? route.query.stamp
  : null
if (stampDate) {
  const [y, m, d] = stampDate.split('-').map(Number)
  currentYear.value = y
  currentMonth.value = m
  stampDay.value = d
  router.replace({ query: {} })
}
const calendarData = ref([])
const aiInsight = ref('')
const aiLoading = ref(false)

const weekDays = ['일', '월', '화', '수', '목', '금', '토']


const daysInMonth = computed(() =>
  new Date(currentYear.value, currentMonth.value, 0).getDate()
)

const firstDayOffset = computed(() =>
  new Date(currentYear.value, currentMonth.value - 1, 1).getDay()
)

const dayMap = computed(() => {
  const map = {}
  calendarData.value.forEach(item => {
    const day = parseInt(item.date.split('-')[2])
    map[day] = item
  })
  return map
})

const stats = computed(() => {
  const count = calendarData.value.length
  const avg = count > 0
    ? (calendarData.value.reduce((s, r) => s + r.score, 0) / count).toFixed(1)
    : '-'
  const rate = Math.round((count / daysInMonth.value) * 100)
  return { count, avgScore: avg, rate }
})

const recentRetros = computed(() =>
  [...calendarData.value].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 3)
)

function getDayData(day) { return dayMap.value[day] || null }

function isToday(day) {
  return day === today.getDate() &&
    currentMonth.value === today.getMonth() + 1 &&
    currentYear.value === today.getFullYear()
}

function isFutureDay(day) {
  const cellDate = new Date(currentYear.value, currentMonth.value - 1, day)
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  return cellDate > todayStart
}

// 14일보다 지난 날짜는 새로 작성할 수 없으므로, 회고가 없는 날은 들어가지 못하게 막음
// (이미 작성된 회고는 읽기 전용으로 볼 수 있음)
const WRITABLE_DAYS = 14

function isLockedDay(day) {
  if (getDayData(day)) return false
  const cellDate = new Date(currentYear.value, currentMonth.value - 1, day)
  const cutoff = new Date(today.getFullYear(), today.getMonth(), today.getDate() - WRITABLE_DAYS)
  return cellDate < cutoff
}

const isCurrentMonth = computed(() =>
  currentYear.value === today.getFullYear() && currentMonth.value === today.getMonth() + 1)
const isFutureMonth = computed(() =>
  currentYear.value * 12 + currentMonth.value > today.getFullYear() * 12 + today.getMonth() + 1)
const todayWritten = computed(() => isCurrentMonth.value && !!getDayData(today.getDate()))

function goToToday() {
  const iso = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  router.push(`/retrospect/${iso}`)
}

// ── 점 잇는 선 ──
const gridEl = ref(null)
const gridSize = reactive({ w: 0, h: 0 })
const lineSegments = ref([])
const dotEls = {}

function setDotRef(day, el) {
  if (el) dotEls[day] = el
  else delete dotEls[day]
}

function computeLines() {
  const grid = gridEl.value
  if (!grid) return
  const g = grid.getBoundingClientRect()
  gridSize.w = g.width
  gridSize.h = g.height
  const center = day => {
    const el = dotEls[day]
    if (!el) return null
    const r = el.getBoundingClientRect()
    return { x: r.left - g.left + r.width / 2, y: r.top - g.top + r.height / 2 }
  }
  const segs = []
  for (let d = 1; d < daysInMonth.value; d++) {
    if (!getDayData(d) || !getDayData(d + 1)) continue
    const a = center(d)
    const b = center(d + 1)
    if (!a || !b) continue
    const path = Math.abs(a.y - b.y) < 1
      ? `M${a.x} ${a.y} L${b.x} ${b.y}`
      : `M${a.x} ${a.y} L${g.width} ${a.y} M0 ${b.y} L${b.x} ${b.y}` // 토 → 다음 주 일
    segs.push({ key: `${currentYear.value}-${currentMonth.value}-${d}`, d: path })
  }
  lineSegments.value = segs
}

watch(calendarData, () => nextTick(computeLines))

let gridObserver = null
onMounted(() => {
  gridObserver = new ResizeObserver(() => computeLines())
  if (gridEl.value) gridObserver.observe(gridEl.value)
})
onBeforeUnmount(() => gridObserver?.disconnect())

function goToDate(day) {
  if (isFutureDay(day) || isLockedDay(day)) return
  const date = `${currentYear.value}-${String(currentMonth.value).padStart(2,'0')}-${String(day).padStart(2,'0')}`
  router.push(`/retrospect/${date}`)
}

function prevMonth() {
  if (currentMonth.value === 1) { currentMonth.value = 12; currentYear.value-- }
  else currentMonth.value--
}

function nextMonth() {
  if (currentMonth.value === 12) { currentMonth.value = 1; currentYear.value++ }
  else currentMonth.value++
}

function formatDate(dateStr) {
  const [, m, d] = dateStr.split('-')
  return `${parseInt(m)}월 ${parseInt(d)}일`
}

async function getInsight() {
  if (calendarData.value.length < 3) return
  aiLoading.value = true
  try {
    const res = await aiApi.insight(joinItems(calendarData.value, 'keep'), joinItems(calendarData.value, 'problem'))
    updateAiQuota('INSIGHT', res)
    aiInsight.value = res.insight
  } catch (e) {
    markQuotaExhausted('INSIGHT', e)
    modal.error(e.response?.data?.message || '인사이트를 불러오지 못했습니다.', { title: 'AI 인사이트 실패' })
  }
  finally { aiLoading.value = false }
}

const calendarLoading = ref(false)

async function loadCalendar() {
  const key = `${currentYear.value}-${currentMonth.value}`
  const cached = calendarCache.get(key)
  if (cached) {
    // 이미 본 적 있는 달이면 캐시로 즉시 표시하고, 최신 데이터로 조용히 갱신
    calendarData.value = cached
  } else {
    calendarLoading.value = true
  }
  try {
    const data = await retrospectApi.getCalendar(currentYear.value, currentMonth.value)
    calendarData.value = data
    calendarCache.set(key, data)
  } catch (e) { console.error(e) }
  finally { calendarLoading.value = false }
}

watch([currentYear, currentMonth], () => { aiInsight.value = ''; stampDay.value = null; loadCalendar() })
const insightLeft = computed(() => aiQuota.INSIGHT?.remaining ?? null)

onMounted(async () => {
  refreshAiQuota()
  await loadCalendar()
  // 찍히는 애니메이션이 끝난 뒤 표시 해제
  if (stampDay.value) setTimeout(() => { stampDay.value = null }, 1500)
})
</script>

<style scoped>
.calendar-page {
  padding: 24px 20px;
  max-width: 1100px;
  margin: 0 auto;
  color: var(--ink);
}

.calendar-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 24px;
}

@media (max-width: 768px) {
  .calendar-layout { grid-template-columns: 1fr; }
  .sidebar { order: -1; }
}

/* 공통: 종이 한 장 */
.calendar-section,
.sidebar-card {
  position: relative;
  background: var(--sheet);
  border: 1px solid var(--rule);
  border-radius: 6px;
  box-shadow: 0 1px 0 var(--rule);
}

/* 캘린더 섹션 */
.calendar-section { padding: 26px 24px 22px; }

.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ink-soft);
  z-index: 5;
}

.month-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--rule);
}

.month-title { display: flex; align-items: baseline; gap: 8px; }
.month-num { font-family: var(--font-serif); font-size: 1.6rem; font-weight: 700; color: var(--ink); }
.month-year { font-size: 0.85rem; font-weight: 600; color: var(--ink-faint); letter-spacing: 0.04em; }

.nav-btn {
  background: none;
  border: 1px solid var(--rule);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 1.1rem;
  color: var(--ink-soft);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.nav-btn:hover { background: var(--paper-deep); }

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 6px;
}

.weekday {
  text-align: center;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ink-faint);
  padding: 4px 0;
}
.weekday:first-child { color: var(--red-pen); }

.day-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.day-cell {
  aspect-ratio: 1;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  z-index: 1;
  transition: background 0.15s;
}
/* hover: 칸을 칠하지 않고, 점이 찍힐 자리에 연필로 그린 동그라미를 띄움 (점 잇는 선도 가리지 않음) */
.day-cell::before {
  content: '';
  position: absolute;
  top: calc(50% + 3px);
  left: 50%;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1.5px dashed var(--ink-faint);
  transform: translate(-50%, -50%) scale(0.7);
  opacity: 0;
  transition: opacity 0.15s, transform 0.2s cubic-bezier(0.3, 1.6, 0.5, 1);
  pointer-events: none;
}
.day-cell.has-data::before { width: 30px; height: 30px; border-style: solid; border-color: var(--ink-soft); }
.day-cell:not(.empty):not(.future):not(.locked):hover::before { opacity: 1; transform: translate(-50%, -50%) scale(1); }
.day-cell:not(.empty):not(.future):not(.locked):not(.today):hover .day-num { color: var(--ink); font-weight: 700; }
.day-cell.empty { cursor: default; }
.day-cell.future, .day-cell.locked { cursor: not-allowed; opacity: 0.35; }

/* 날짜 숫자는 칸 왼쪽 위에 작게 — 노트 귀퉁이에 쓴 날짜처럼 */
.day-num {
  position: absolute;
  top: 5px;
  left: 7px;
  font-size: 0.72rem;
  font-weight: 500;
  color: var(--ink-soft);
  line-height: 1;
}

/* 오늘: 빨간 펜으로 대충 동그라미 친 표시 */
.day-cell.today .day-num {
  color: var(--red-pen);
  font-weight: 700;
}
.day-cell.today .day-num::after {
  content: '';
  position: absolute;
  inset: -5px -7px;
  border: 1.5px solid var(--red-pen);
  border-radius: 52% 48% 55% 45% / 50% 55% 45% 50%;
  transform: rotate(-8deg);
}

.ink-dot {
  display: inline-block;
  border-radius: 50%;
  background: var(--ink);
  flex-shrink: 0;
}
.day-cell .ink-dot { margin-top: 6px; position: relative; z-index: 1; }

/* 점 잇는 선: 칸 뒤에 깔리고, 처음 나타날 때 슥 그어짐 */
.dot-lines {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: visible;
}
.dot-line {
  fill: none;
  stroke: var(--ink);
  stroke-width: 1.5;
  stroke-opacity: 0.3;
  stroke-linecap: round;
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  animation: draw-line 0.5s ease-out forwards;
}
@keyframes draw-line { to { stroke-dashoffset: 0; } }

/* 저장 직후: 잉크 점이 "톡" 찍히고 번지는 효과 */
.ink-dot.stamp { animation: ink-stamp 0.55s cubic-bezier(0.3, 1.6, 0.5, 1) both; }
.ink-dot.stamp::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1.5px solid var(--ink);
  opacity: 0;
  animation: ink-ring 0.8s ease-out 0.2s;
}
@keyframes ink-stamp {
  0% { transform: scale(0); opacity: 0; }
  60% { transform: scale(1.3); }
  100% { transform: scale(1); }
}
@keyframes ink-ring {
  0% { transform: scale(0.6); opacity: 0.6; }
  100% { transform: scale(2.2); opacity: 0; }
}

.empty-hint {
  margin-top: 14px;
  text-align: center;
  font-family: var(--font-serif);
  font-size: 0.95rem;
  color: var(--ink-soft);
}

@media (prefers-reduced-motion: reduce) {
  .dot-line { animation: none; stroke-dashoffset: 0; }
  .ink-dot.stamp, .ink-dot.stamp::after { animation: none; }
}

.legend {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed var(--rule);
}
.legend-label { font-size: 0.75rem; color: var(--ink-faint); }
.legend-items { display: flex; align-items: center; gap: 8px; }
.legend-end { font-size: 0.7rem; color: var(--ink-faint); }

.btn-today {
  margin-top: 16px;
  width: 100%;
  padding: 12px;
  background: var(--ink);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-today:hover { opacity: 0.9; }

/* 사이드바 */
.sidebar { display: flex; flex-direction: column; gap: 16px; }

.sidebar-card { padding: 20px; }

.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-serif);
  font-size: 1rem;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 14px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
}

.stat { text-align: center; padding: 4px; }
.stat + .stat { border-left: 1px dashed var(--rule); }
.stat-value { font-family: var(--font-serif); font-size: 1.5rem; font-weight: 700; color: var(--ink); }
.stat-label { font-size: 0.7rem; color: var(--ink-faint); margin-top: 2px; }

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed var(--rule);
  cursor: pointer;
  transition: opacity 0.15s;
}
.recent-item:last-child { border-bottom: none; }
.recent-item:hover { opacity: 0.65; }
.recent-date { font-size: 0.875rem; color: var(--ink); font-weight: 500; }
.recent-score {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ink-soft);
}

.empty-msg { font-size: 0.825rem; color: var(--ink-faint); line-height: 1.6; }

/* AI 카드는 하늘색(Try 형광펜) 테두리 포인트 */
.ai-card { border: 1.5px solid var(--hl-try); }

.ai-text {
  font-size: 0.85rem;
  color: var(--ink);
  line-height: 1.7;
  white-space: pre-wrap;
}

.btn-ai {
  margin-top: 12px;
  width: 100%;
  padding: 10px;
  background: var(--hl-try);
  color: var(--ink);
  border: none;
  border-radius: 6px;
  font-weight: 700;
  font-size: 0.875rem;
  cursor: pointer;
  transition: filter 0.15s;
}
.btn-ai:hover { filter: brightness(0.96); }
.btn-ai:disabled { opacity: 0.6; cursor: not-allowed; }
.ai-loading { display: flex; justify-content: center; padding: 8px 0; }
.quota-text { margin-top: 8px; text-align: center; font-size: 0.75rem; color: var(--ink-faint); }

/* 좁은 화면: 통계(왼쪽)와 최근 회고(오른쪽)를 한 줄에, AI 인사이트는 아래 전체 폭 */
@media (max-width: 768px) {
  .sidebar {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 12px;
  }
  .ai-card { grid-column: 1 / -1; }
  .sidebar-card { padding: 14px 16px; }
  .card-title { font-size: 0.92rem; margin-bottom: 8px; }

  /* 통계는 세로로: 이름 왼쪽, 숫자 오른쪽 */
  .stats-row { grid-template-columns: 1fr; }
  .stat {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    padding: 6px 0;
    text-align: left;
  }
  .stat + .stat { border-left: none; border-top: 1px dashed var(--rule); }
  .stat-value { order: 2; font-size: 1.1rem; }
  .stat-label { order: 1; margin-top: 0; font-size: 0.75rem; }

  .recent-item { padding: 6px 0; }
  .recent-date { font-size: 0.82rem; }
  .recent-score { gap: 6px; font-size: 0.75rem; }
}
</style>
