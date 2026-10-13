<template>
  <div class="analysis-page">
    <div class="page-header">
      <h2 class="page-title">AI 분석</h2>
    </div>

    <!-- 탭 -->
    <div class="tabs">
      <button v-for="tab in tabs" :key="tab.key" class="tab-btn" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
        {{ tab.label }}
      </button>
    </div>

    <div v-if="pageLoading" class="page-loading"><DotLoader /></div>

    <!-- 개요 탭 -->
    <div v-else-if="activeTab === 'overview'" class="tab-content">
      <!-- 통계 카드 -->
      <div class="stat-cards">
        <div class="stat-card">
          <Icon name="document" class="stat-icon" :size="22" />
          <div class="stat-val">{{ overview.count }}</div>
          <div class="stat-label">총 작성 수</div>
        </div>
        <div class="stat-card">
          <Icon name="star" class="stat-icon" :size="22" />
          <div class="stat-val">{{ overview.avgScore }}</div>
          <div class="stat-label">평균 점수</div>
        </div>
        <div class="stat-card">
          <Icon name="flame" class="stat-icon" :size="22" />
          <div class="stat-val">{{ overview.streak }}</div>
          <div class="stat-label">연속 일수</div>
        </div>
      </div>

      <!-- 7일 만족도 그래프 -->
      <div class="card">
        <div class="card-title">최근 7일 만족도</div>
        <div class="chart">
          <!-- 회고를 연달아 쓴 날끼리 점을 이음 (빠진 날에서 선이 끊김) -->
          <svg class="chart-lines" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
            <polyline v-for="(pts, i) in chartLines" :key="i" :points="pts" vector-effect="non-scaling-stroke" />
          </svg>
          <div v-for="item in weekChart" :key="item.date" class="chart-col">
            <div class="chart-track">
              <span
                v-if="item.score"
                class="ink-dot chart-dot"
                :style="{ ...dotStyle(item.score), bottom: `${item.score / 5 * 100}%` }"
                :title="`${item.score}점`"
              />
            </div>
            <div class="chart-label">{{ item.label }}</div>
          </div>
        </div>
      </div>

      <!-- AI 인사이트 -->
      <div class="card insight-card">
        <div class="card-title"><Icon name="sparkle" /> AI 인사이트</div>
        <p v-if="!aiInsight && !aiLoading && allRetros.length < 3" class="empty-text">회고를 3개 이상 작성하면 AI 인사이트를 받을 수 있어요.</p>
        <div v-if="aiLoading" class="ai-loading"><DotLoader label="기록을 돌아보는 중" :size="11" /></div>
        <p v-if="aiInsight" class="insight-text">{{ aiInsight }}</p>
        <button
          v-if="allRetros.length >= 3 && !aiInsight && !aiLoading"
          class="btn-primary"
          :disabled="insightLeft === 0"
          @click="loadInsight"
        >{{ insightLeft === 0 ? '오늘 인사이트를 다 썼어요' : '인사이트 생성' }}</button>
        <p v-if="aiQuota.INSIGHT" class="quota-text">
          오늘 남은 횟수 {{ aiQuota.INSIGHT.remaining }}/{{ aiQuota.INSIGHT.limit }}
          <template v-if="insightLeft === 0"> · 내일 자정에 다시 채워져요</template>
        </p>
      </div>

      <!-- 반복 Keep / Problem: 최근 3개월 동안 두 번 이상 쓴 내용 -->
      <div class="two-col">
        <div class="card">
          <div class="card-title keep-title"><Icon name="check-circle" /> 반복 Keep</div>
          <div v-if="repeatKeeps.length === 0" class="empty-text">아직 반복된 Keep이 없어요. 같은 내용을 두 번 이상 쓰면 여기에 모여요.</div>
          <div v-for="(k, i) in repeatKeeps" :key="k.text" class="keyword-item">
            <span class="keyword-rank">{{ i + 1 }}</span>
            <span class="keyword-text">{{ k.text }}</span>
            <span class="repeat-count">{{ k.count }}회</span>
          </div>
        </div>
        <div class="card">
          <div class="card-title problem-title"><Icon name="alert-triangle" /> 반복 Problem</div>
          <div v-if="repeatProblems.length === 0" class="empty-text">아직 반복된 Problem이 없어요. 같은 문제가 두 번 이상 나오면 여기에 모여요.</div>
          <div v-for="(p, i) in repeatProblems" :key="p.text" class="keyword-item">
            <span class="keyword-rank problem-rank">{{ i + 1 }}</span>
            <span class="keyword-text">{{ p.text }}</span>
            <span class="repeat-count">{{ p.count }}회</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 패턴 탭 -->
    <div v-if="activeTab === 'pattern'" class="tab-content">
      <div class="card">
        <div class="card-title"><Icon name="repeat" /> 반복 키워드</div>
        <p class="card-desc">Problem에서 자주 언급된 단어예요.</p>
        <div v-if="keywords.length === 0" class="empty-text">회고를 더 작성하면 패턴이 나타나요.</div>
        <div v-for="kw in keywords" :key="kw.word" class="kw-row">
          <span class="kw-word">{{ kw.word }}</span>
          <span class="kw-badge">{{ kw.count }}회</span>
        </div>
      </div>

      <div v-if="hasRepeatProblem" class="card warn-card">
        <div class="warn-title"><Icon name="alert-triangle" /> 반복 문제 감지</div>
        <p class="warn-desc">같은 문제가 반복되고 있어요. Try를 통해 적극적으로 해결해보세요!</p>
      </div>
    </div>

    <!-- 추천 탭 -->
    <div v-if="activeTab === 'recommend'" class="tab-content">
      <div class="card">
        <div class="card-title"><Icon name="target" /> AI 추천 다음 단계</div>
        <p v-if="!recommendations.length && !recLoading && allRetros.length < 3" class="empty-text">회고를 3개 이상 작성하면 추천을 받을 수 있어요.</p>
        <div v-if="recLoading" class="ai-loading"><DotLoader label="다음 점을 고르는 중" :size="11" /></div>
        <ul v-if="recommendations.length" class="rec-list">
          <li v-for="r in recommendations" :key="r">{{ r }}</li>
        </ul>
        <button
          v-if="allRetros.length >= 3 && !recommendations.length && !recLoading"
          class="btn-primary"
          :disabled="tryLeft === 0"
          @click="loadRecommendations"
        >{{ tryLeft === 0 ? '오늘 AI 추천을 다 썼어요' : '추천 생성' }}</button>
        <p v-if="aiQuota.TRY_RECOMMENDATION" class="quota-text">
          오늘 남은 횟수 {{ aiQuota.TRY_RECOMMENDATION.remaining }}/{{ aiQuota.TRY_RECOMMENDATION.limit }}
          · 회고 작성 화면의 AI Try 추천과 함께 써요
        </p>
      </div>

      <div class="card tip-card">
        <div class="card-title"><Icon name="lightbulb" /> 성장 팁</div>
        <div v-for="tip in growthTips" :key="tip" class="tip-item">• {{ tip }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { retrospectApi } from '@/api/retrospect'
import { aiApi } from '@/api/ai'
import Icon from '@/components/Icon.vue'
import DotLoader from '@/components/DotLoader.vue'
import { calendarCache } from '@/utils/calendarCache'
import { dotStyle } from '@/utils/inkDot'
import { modal } from '@/composables/useModal'
import { aiQuota, refreshAiQuota, updateAiQuota, markQuotaExhausted, joinItems } from '@/composables/useAiQuota'

const activeTab = ref('overview')
const tabs = [
  { key: 'overview', label: '개요' },
  { key: 'pattern', label: '패턴' },
  { key: 'recommend', label: '추천' }
]

const pageLoading = ref(false)
const allRetros = ref([])
const aiInsight = ref('')
const aiLoading = ref(false)
const recommendations = ref([])
const recLoading = ref(false)


const growthTips = [
  'Try는 측정 가능하고 구체적으로 작성할수록 실천률이 높아져요.',
  'Problem의 원인을 파악하면 더 나은 Try를 만들 수 있어요.',
  '매일 꾸준히 작성하는 것이 가장 중요해요.',
  'Keep을 통해 자신의 강점을 인식하세요.',
]

const overview = computed(() => {
  const count = allRetros.value.length
  const avg = count > 0
    ? (allRetros.value.reduce((s, r) => s + r.score, 0) / count).toFixed(1)
    : '-'
  // 연속 일수 계산
  const sorted = [...allRetros.value].sort((a, b) => b.date.localeCompare(a.date))
  let streak = 0
  const today = new Date()
  for (let i = 0; i < sorted.length; i++) {
    const d = new Date(sorted[i].date)
    const diff = Math.floor((today - d) / 86400000)
    if (diff === i || diff === i + 1) streak++
    else break
  }
  return { count, avgScore: avg, streak }
})

const weekChart = computed(() => {
  const result = []
  const today = new Date()
  const dayLabels = ['일', '월', '화', '수', '목', '금', '토']
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    const retro = allRetros.value.find(r => r.date === dateStr)
    result.push({ date: dateStr, label: dayLabels[d.getDay()], score: retro?.score || 0 })
  }
  return result
})

// 점수가 있는 날이 연속된 구간마다 선 하나 (x: 칸 가운데, y: 점수 높이)
const chartLines = computed(() => {
  const n = weekChart.value.length
  const lines = []
  let cur = []
  weekChart.value.forEach((item, i) => {
    if (!item.score) { if (cur.length > 1) lines.push(cur.join(' ')); cur = []; return }
    cur.push(`${((i + 0.5) / n) * 100},${100 - (item.score / 5) * 100}`)
  })
  if (cur.length > 1) lines.push(cur.join(' '))
  return lines
})

// 회고 항목은 { content } 배열 — 최신 회고부터 내용만 뽑음
function contentsOf(key) {
  return allRetros.value.flatMap(r => (r[key] || []).map(i => i?.content?.trim())).filter(Boolean)
}

// 두 번 이상 쓴 내용만 많이 쓴 순으로 3개 (띄어쓰기·문장부호·대소문자 차이는 같은 내용으로 봄)
function repeatedOf(key) {
  const groups = new Map()
  for (const text of contentsOf(key)) { // 최신순이라 처음 나온 문장을 대표 문장으로 씀
    const norm = text.toLowerCase().replace(/[\s.,!?~·…'"()\-]/g, '')
    if (!norm) continue
    const g = groups.get(norm)
    if (g) g.count++
    else groups.set(norm, { text, count: 1 })
  }
  return [...groups.values()].filter(g => g.count >= 2).sort((a, b) => b.count - a.count).slice(0, 3)
}

const repeatKeeps = computed(() => repeatedOf('keep'))
const repeatProblems = computed(() => repeatedOf('problem'))

const keywords = computed(() => {
  const wordCount = {}
  contentsOf('problem').forEach(text => {
    text.split(/[\s,\.]+/).forEach(w => {
      if (w.length >= 2) wordCount[w] = (wordCount[w] || 0) + 1
    })
  })
  return Object.entries(wordCount)
    .filter(([, c]) => c >= 2)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 8)
    .map(([word, count]) => ({ word, count }))
})

const hasRepeatProblem = computed(() => keywords.value.some(k => k.count >= 3))

const insightLeft = computed(() => aiQuota.INSIGHT?.remaining ?? null)
const tryLeft = computed(() => aiQuota.TRY_RECOMMENDATION?.remaining ?? null)

async function loadInsight() {
  aiLoading.value = true
  try {
    const res = await aiApi.insight(joinItems(allRetros.value, 'keep'), joinItems(allRetros.value, 'problem'))
    updateAiQuota('INSIGHT', res)
    aiInsight.value = res.insight
  } catch (e) {
    markQuotaExhausted('INSIGHT', e)
    modal.error(e.response?.data?.message || '인사이트를 불러오지 못했습니다.', { title: 'AI 인사이트 실패' })
  }
  finally { aiLoading.value = false }
}

async function loadRecommendations() {
  recLoading.value = true
  try {
    const res = await aiApi.suggestTry(joinItems(allRetros.value, 'keep'), joinItems(allRetros.value, 'problem'))
    updateAiQuota('TRY_RECOMMENDATION', res)
    recommendations.value = res.suggestions || []
  } catch (e) {
    markQuotaExhausted('TRY_RECOMMENDATION', e)
    modal.error(e.response?.data?.message || '추천을 불러오지 못했습니다.', { title: 'AI 추천 실패' })
  }
  finally { recLoading.value = false }
}

async function loadAll() {
  pageLoading.value = true
  try {
    // 최근 3개월치 로드 (캘린더 화면에서 이미 본 달이면 캐시 재사용)
    const now = new Date()
    const months = [0, 1, 2].map(back => {
      const d = new Date(now.getFullYear(), now.getMonth() - back, 1)
      return [d.getFullYear(), d.getMonth() + 1]
    })
    const results = await Promise.all(months.map(([y, m]) => {
      const key = `${y}-${m}`
      const cached = calendarCache.get(key)
      if (cached) return cached
      return retrospectApi.getCalendar(y, m).then(data => {
        calendarCache.set(key, data)
        return data
      })
    }))
    allRetros.value = results.flat().sort((a, b) => b.date.localeCompare(a.date))
  } catch (e) { console.error(e) }
  finally { pageLoading.value = false }
}

onMounted(() => { loadAll(); refreshAiQuota() })
</script>

<style scoped>
.analysis-page { min-height: 100%; color: var(--ink); }

.page-header {
  padding: 22px 20px 0;
}
.page-title { font-family: var(--font-serif); font-size: 1.4rem; font-weight: 700; color: var(--ink); }

/* 탭: 고른 탭에 형광펜을 그은 듯이 */
.tabs {
  display: flex;
  gap: 4px;
  padding: 14px 20px 10px;
  border-bottom: 1px solid var(--rule);
  background: var(--paper);
  position: sticky;
  top: 56px;
  z-index: 10;
}
.tab-btn {
  padding: 6px 12px;
  background: none;
  border: none;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ink-faint);
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.tab-btn:hover { color: var(--ink-soft); }
.tab-btn.active { color: var(--ink); background: var(--hl-try); }

.tab-content {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 800px;
  margin: 0 auto;
}

/* 종이 한 장 */
.stat-card,
.card {
  background: var(--sheet);
  border: 1px solid var(--rule);
  border-radius: 6px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.stat-card { padding: 16px; text-align: center; }
.stat-icon { color: var(--ink-soft); margin-bottom: 6px; }
.stat-val { font-family: var(--font-serif); font-size: 1.6rem; font-weight: 700; color: var(--ink); }
.stat-label { font-size: 0.7rem; color: var(--ink-faint); margin-top: 2px; }

.card { padding: 20px; }
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
.card-desc { font-size: 0.8rem; color: var(--ink-faint); margin-bottom: 12px; margin-top: -10px; }

/* 7일 그래프: 점선 세로줄 위에 잉크 점, 연속된 날은 선으로 이음 */
.chart {
  position: relative;
  display: flex;
  height: 140px;
  padding: 10px 0 24px;
}
.chart-lines {
  position: absolute;
  top: 10px;
  left: 0;
  right: 0;
  bottom: 24px;
  width: 100%;
  height: calc(100% - 34px);
  overflow: visible;
  pointer-events: none;
}
.chart-lines polyline {
  fill: none;
  stroke: var(--ink);
  stroke-width: 1.5;
  stroke-opacity: 0.35;
  stroke-linejoin: round;
  stroke-linecap: round;
}
.chart-col {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.chart-track {
  position: relative;
  flex: 1;
  width: 100%;
  background: linear-gradient(var(--rule), var(--rule)) center / 1px 100% no-repeat;
}
.chart-dot {
  position: absolute;
  left: 50%;
  transform: translate(-50%, 50%);
  border-radius: 50%;
  background: var(--ink);
  z-index: 1;
}
.chart-label {
  position: absolute;
  bottom: -20px;
  font-size: 0.72rem;
  color: var(--ink-faint);
  font-weight: 500;
}

.insight-card { border-color: var(--hl-try); }
.insight-text { font-size: 0.875rem; line-height: 1.8; color: var(--ink); white-space: pre-wrap; }

.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.keyword-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--rule);
}
.keyword-item:last-child { border-bottom: none; }
.keyword-rank {
  width: 22px;
  height: 22px;
  background: var(--hl-keep);
  color: var(--ink);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-size: 0.75rem;
  font-weight: 700;
  flex-shrink: 0;
}
.problem-rank { background: var(--hl-problem); }
.keyword-text { flex: 1; font-size: 0.85rem; color: var(--ink); }
.repeat-count { flex-shrink: 0; font-size: 0.72rem; font-weight: 700; color: var(--ink-faint); }

.kw-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed var(--rule);
}
.kw-row:last-child { border-bottom: none; }
.kw-word { font-size: 0.875rem; font-weight: 600; color: var(--ink); }
.kw-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 8px;
  background: var(--hl-problem);
  color: var(--ink);
  border-radius: 4px;
}

.warn-card {
  border-color: #f1d3cb;
  background: #fdf4f1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 6px;
}
.warn-icon { font-size: 2rem; }
.warn-title { display: flex; align-items: center; gap: 6px; font-family: var(--font-serif); font-size: 1rem; font-weight: 700; color: var(--red-pen); }
.warn-desc { font-size: 0.85rem; color: var(--ink-soft); }

.rec-list { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.rec-list li {
  position: relative;
  padding: 10px 12px 10px 28px;
  border: 1px solid var(--hl-try);
  border-radius: 6px;
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--ink);
}
.rec-list li::before {
  content: '';
  position: absolute;
  left: 12px;
  top: 18px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--ink);
}
.quota-text { margin-top: 10px; font-size: 0.75rem; color: var(--ink-faint); }

.tip-card { border-color: var(--hl-keep); }
.tip-item { font-size: 0.85rem; color: var(--ink); padding: 5px 0; line-height: 1.5; }

.btn-primary {
  margin-top: 12px;
  padding: 10px 20px;
  background: var(--ink);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
}
.btn-primary:disabled { opacity: 0.55; cursor: not-allowed; }

.page-loading { display: flex; justify-content: center; padding: 56px 0; }
.ai-loading { display: flex; justify-content: center; padding: 8px 0; }

.empty-text { font-size: 0.85rem; color: var(--ink-faint); line-height: 1.6; }
</style>
