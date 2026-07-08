<template>
  <div class="calendar-page">
    <div class="calendar-layout">
      <!-- 좌측: 캘린더 -->
      <section class="calendar-section">
        <!-- 월 네비게이션 -->
        <div class="month-nav">
          <button class="nav-btn" @click="prevMonth">‹</button>
          <h2 class="month-title">{{ currentYear }}년 {{ currentMonth }}월</h2>
          <button class="nav-btn" @click="nextMonth">›</button>
        </div>

        <!-- 요일 헤더 -->
        <div class="weekday-row">
          <div v-for="d in weekDays" :key="d" class="weekday">{{ d }}</div>
        </div>

        <!-- 날짜 그리드 -->
        <div class="day-grid">
          <div v-for="n in firstDayOffset" :key="`e${n}`" class="day-cell empty" />
          <div
            v-for="day in daysInMonth"
            :key="day"
            class="day-cell"
            :class="{ today: isToday(day), 'has-data': getDayData(day) }"
            :style="getCellStyle(day)"
            @click="goToDate(day)"
          >
            <span class="day-num">{{ day }}</span>
            <span v-if="getDayData(day)" class="day-score">{{ getDayData(day).score }}</span>
          </div>
        </div>

        <!-- 점수 범례 -->
        <div class="legend">
          <span class="legend-label">만족도</span>
          <div class="legend-items">
            <div v-for="n in 5" :key="n" class="legend-item" :style="{ background: scoreColors[n] }">{{ n }}</div>
          </div>
        </div>

        <!-- 오늘 회고 작성 버튼 -->
        <button class="btn-today" @click="goToDate(today.getDate())">
          ✏️ 오늘 회고 작성
        </button>
      </section>

      <!-- 우측: 사이드바 -->
      <aside class="sidebar">
        <!-- 월 통계 -->
        <div class="sidebar-card">
          <h3 class="card-title">📊 이번 달 통계</h3>
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
          <h3 class="card-title">📝 최근 회고</h3>
          <div v-if="recentRetros.length === 0" class="empty-msg">아직 작성된 회고가 없어요</div>
          <div v-for="r in recentRetros" :key="r.id" class="recent-item" @click="$router.push(`/retrospect/${r.date}`)">
            <div class="recent-date">{{ formatDate(r.date) }}</div>
            <div class="recent-score-badge" :style="{ background: scoreColors[r.score] }">{{ r.score }}점</div>
          </div>
        </div>

        <!-- AI 인사이트 -->
        <div class="sidebar-card ai-card">
          <h3 class="card-title">✨ AI 인사이트</h3>
          <p v-if="!aiInsight && !aiLoading" class="empty-msg">회고를 3개 이상 작성하면 AI 인사이트를 받을 수 있어요</p>
          <p v-if="aiLoading" class="empty-msg">분석 중...</p>
          <p v-if="aiInsight" class="ai-text">{{ aiInsight }}</p>
          <button v-if="stats.count >= 3 && !aiInsight" class="btn-ai" @click="getInsight" :disabled="aiLoading">
            인사이트 받기
          </button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { retrospectApi } from '@/api/retrospect'
import { aiApi } from '@/api/ai'

const router = useRouter()
const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1)
const calendarData = ref([])
const aiInsight = ref('')
const aiLoading = ref(false)

const weekDays = ['일', '월', '화', '수', '목', '금', '토']

const scoreColors = {
  1: '#fca5a5',
  2: '#fdba74',
  3: '#fde68a',
  4: '#86efac',
  5: '#34d399'
}

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

function getCellStyle(day) {
  const data = getDayData(day)
  if (!data) return {}
  return {
    background: scoreColors[data.score] || '#e2e8f0',
    color: data.score >= 4 ? '#065f46' : '#1e293b'
  }
}

function isToday(day) {
  return day === today.getDate() &&
    currentMonth.value === today.getMonth() + 1 &&
    currentYear.value === today.getFullYear()
}

function goToDate(day) {
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
    const keeps = calendarData.value.map(r => r.keep).filter(Boolean).join('\n')
    const problems = calendarData.value.map(r => r.problem).filter(Boolean).join('\n')
    const res = await aiApi.suggestTry(keeps, problems)
    aiInsight.value = res.suggestion
  } catch { aiInsight.value = '인사이트를 불러오지 못했습니다.' }
  finally { aiLoading.value = false }
}

async function loadCalendar() {
  try {
    calendarData.value = await retrospectApi.getCalendar(currentYear.value, currentMonth.value)
  } catch (e) { console.error(e) }
}

watch([currentYear, currentMonth], () => { aiInsight.value = ''; loadCalendar() })
onMounted(loadCalendar)
</script>

<style scoped>
.calendar-page {
  padding: 20px;
  max-width: 1100px;
  margin: 0 auto;
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

/* 캘린더 섹션 */
.calendar-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.month-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.month-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: #1e293b;
}

.nav-btn {
  background: #f1f5f9;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  font-size: 1.2rem;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-btn:hover { background: #e2e8f0; }

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: #94a3b8;
  padding: 4px 0;
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.day-cell {
  aspect-ratio: 1;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #f8fafc;
  border: 1.5px solid transparent;
  transition: transform 0.1s, box-shadow 0.1s;
  position: relative;
}

.day-cell:not(.empty):hover {
  transform: scale(1.06);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  border-color: #6366f1;
}

.day-cell.empty { background: transparent; cursor: default; }

.day-cell.today {
  border-color: #6366f1;
}
.day-cell.today .day-num { color: #6366f1; font-weight: 800; }

.day-num { font-size: 0.8rem; font-weight: 500; color: #374151; }
.day-score { font-size: 0.65rem; font-weight: 700; margin-top: 1px; }

.legend {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}
.legend-label { font-size: 0.75rem; color: #94a3b8; }
.legend-items { display: flex; gap: 4px; }
.legend-item {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 700;
  color: #374151;
}

.btn-today {
  margin-top: 16px;
  width: 100%;
  padding: 12px;
  background: #6366f1;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-today:hover { opacity: 0.9; }

/* 사이드바 */
.sidebar { display: flex; flex-direction: column; gap: 16px; }

.sidebar-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.card-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 14px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.stat {
  text-align: center;
  padding: 10px 4px;
  background: #f8fafc;
  border-radius: 10px;
}
.stat-value { font-size: 1.3rem; font-weight: 800; color: #6366f1; }
.stat-label { font-size: 0.7rem; color: #94a3b8; margin-top: 2px; }

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: opacity 0.15s;
}
.recent-item:last-child { border-bottom: none; }
.recent-item:hover { opacity: 0.7; }
.recent-date { font-size: 0.875rem; color: #374151; font-weight: 500; }
.recent-score-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 20px;
  color: #374151;
}

.empty-msg { font-size: 0.825rem; color: #94a3b8; line-height: 1.5; }

.ai-card { border: 1.5px solid #e0e7ff; background: #fafafe; }

.ai-text {
  font-size: 0.85rem;
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
}

.btn-ai {
  margin-top: 12px;
  width: 100%;
  padding: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
}
.btn-ai:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
