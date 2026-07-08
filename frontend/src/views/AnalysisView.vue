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

    <!-- 개요 탭 -->
    <div v-if="activeTab === 'overview'" class="tab-content">
      <!-- 통계 카드 -->
      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-icon">📝</div>
          <div class="stat-val">{{ overview.count }}</div>
          <div class="stat-label">총 작성 수</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">⭐</div>
          <div class="stat-val">{{ overview.avgScore }}</div>
          <div class="stat-label">평균 점수</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🔥</div>
          <div class="stat-val">{{ overview.streak }}</div>
          <div class="stat-label">연속 일수</div>
        </div>
      </div>

      <!-- 7일 만족도 그래프 -->
      <div class="card">
        <div class="card-title">최근 7일 만족도</div>
        <div class="chart">
          <div v-for="item in weekChart" :key="item.date" class="chart-bar-wrap">
            <div class="chart-bar-track">
              <div class="chart-bar" :style="{ height: (item.score / 5 * 100) + '%', background: scoreColors[item.score] || '#e2e8f0' }" />
            </div>
            <div class="chart-label">{{ item.label }}</div>
            <div class="chart-score" v-if="item.score">{{ item.score }}</div>
          </div>
        </div>
      </div>

      <!-- AI 인사이트 -->
      <div class="card insight-card">
        <div class="card-title">✨ AI 인사이트</div>
        <p v-if="!aiInsight && !aiLoading && allRetros.length < 3" class="empty-text">회고를 3개 이상 작성하면 AI 인사이트를 받을 수 있어요.</p>
        <p v-if="aiLoading" class="empty-text">분석 중...</p>
        <p v-if="aiInsight" class="insight-text">{{ aiInsight }}</p>
        <button v-if="allRetros.length >= 3 && !aiInsight && !aiLoading" class="btn-primary" @click="loadInsight">인사이트 생성</button>
      </div>

      <!-- 상위 Keep / Problem -->
      <div class="two-col">
        <div class="card">
          <div class="card-title keep-title">✅ 상위 Keep</div>
          <div v-if="topKeeps.length === 0" class="empty-text">데이터 없음</div>
          <div v-for="(k, i) in topKeeps" :key="i" class="keyword-item">
            <span class="keyword-rank">{{ i+1 }}</span>
            <span class="keyword-text">{{ k }}</span>
          </div>
        </div>
        <div class="card">
          <div class="card-title problem-title">⚠️ 상위 Problem</div>
          <div v-if="topProblems.length === 0" class="empty-text">데이터 없음</div>
          <div v-for="(p, i) in topProblems" :key="i" class="keyword-item">
            <span class="keyword-rank problem-rank">{{ i+1 }}</span>
            <span class="keyword-text">{{ p }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 패턴 탭 -->
    <div v-if="activeTab === 'pattern'" class="tab-content">
      <div class="card">
        <div class="card-title">🔁 반복 키워드</div>
        <p class="card-desc">Problem에서 자주 언급된 단어예요.</p>
        <div v-if="keywords.length === 0" class="empty-text">회고를 더 작성하면 패턴이 나타나요.</div>
        <div v-for="kw in keywords" :key="kw.word" class="kw-row">
          <span class="kw-word">{{ kw.word }}</span>
          <span class="kw-badge">{{ kw.count }}회</span>
        </div>
      </div>

      <div v-if="hasRepeatProblem" class="card warn-card">
        <div class="warn-icon">🚨</div>
        <div class="warn-title">반복 문제 감지</div>
        <p class="warn-desc">같은 문제가 반복되고 있어요. Try를 통해 적극적으로 해결해보세요!</p>
      </div>
    </div>

    <!-- 추천 탭 -->
    <div v-if="activeTab === 'recommend'" class="tab-content">
      <div class="card">
        <div class="card-title">🎯 AI 추천 다음 단계</div>
        <p v-if="!recommendations && !recLoading && allRetros.length < 3" class="empty-text">회고를 3개 이상 작성하면 추천을 받을 수 있어요.</p>
        <p v-if="recLoading" class="empty-text">추천 생성 중...</p>
        <div v-if="recommendations" class="rec-text">{{ recommendations }}</div>
        <button v-if="allRetros.length >= 3 && !recommendations && !recLoading" class="btn-primary" @click="loadRecommendations">추천 생성</button>
      </div>

      <div class="card tip-card">
        <div class="card-title">💡 성장 팁</div>
        <div v-for="tip in growthTips" :key="tip" class="tip-item">• {{ tip }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { retrospectApi } from '@/api/retrospect'
import { aiApi } from '@/api/ai'

const activeTab = ref('overview')
const tabs = [
  { key: 'overview', label: '개요' },
  { key: 'pattern', label: '패턴' },
  { key: 'recommend', label: '추천' }
]

const allRetros = ref([])
const aiInsight = ref('')
const aiLoading = ref(false)
const recommendations = ref('')
const recLoading = ref(false)

const scoreColors = { 1: '#fca5a5', 2: '#fdba74', 3: '#fde68a', 4: '#86efac', 5: '#34d399' }

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
    const dateStr = d.toISOString().split('T')[0]
    const retro = allRetros.value.find(r => r.date === dateStr)
    result.push({ date: dateStr, label: dayLabels[d.getDay()], score: retro?.score || 0 })
  }
  return result
})

const topKeeps = computed(() => {
  const texts = allRetros.value.map(r => r.keep).filter(Boolean)
  return texts.slice(-3).reverse()
})

const topProblems = computed(() => {
  const texts = allRetros.value.map(r => r.problem).filter(Boolean)
  return texts.slice(-3).reverse()
})

const keywords = computed(() => {
  const wordCount = {}
  allRetros.value.forEach(r => {
    if (!r.problem) return
    r.problem.split(/[\s,\.]+/).forEach(w => {
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

async function loadInsight() {
  aiLoading.value = true
  try {
    const keeps = allRetros.value.map(r => r.keep).filter(Boolean).join('\n')
    const problems = allRetros.value.map(r => r.problem).filter(Boolean).join('\n')
    const res = await aiApi.suggestTry(keeps, problems)
    aiInsight.value = res.suggestion
  } catch { aiInsight.value = '인사이트를 불러오지 못했습니다.' }
  finally { aiLoading.value = false }
}

async function loadRecommendations() {
  recLoading.value = true
  try {
    const keeps = allRetros.value.map(r => r.keep).filter(Boolean).join('\n')
    const problems = allRetros.value.map(r => r.problem).filter(Boolean).join('\n')
    const res = await aiApi.suggestTry(keeps, problems)
    recommendations.value = res.suggestion
  } catch { recommendations.value = '추천을 불러오지 못했습니다.' }
  finally { recLoading.value = false }
}

async function loadAll() {
  try {
    // 최근 3개월치 로드
    const now = new Date()
    const results = await Promise.all([
      retrospectApi.getCalendar(now.getFullYear(), now.getMonth() + 1),
      retrospectApi.getCalendar(now.getFullYear(), now.getMonth() || 12),
      retrospectApi.getCalendar(now.getFullYear(), now.getMonth() - 1 || 11),
    ])
    allRetros.value = results.flat().sort((a, b) => b.date.localeCompare(a.date))
  } catch (e) { console.error(e) }
}

onMounted(loadAll)
</script>

<style scoped>
.analysis-page { background: #f8fafc; min-height: 100%; }

.page-header {
  padding: 20px 20px 0;
}
.page-title { font-size: 1.2rem; font-weight: 800; color: #1e293b; }

.tabs {
  display: flex;
  gap: 0;
  padding: 16px 20px 0;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  position: sticky;
  top: 56px;
  z-index: 10;
}
.tab-btn {
  padding: 10px 20px;
  background: none;
  border: none;
  font-size: 0.875rem;
  font-weight: 600;
  color: #94a3b8;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.15s;
}
.tab-btn.active { color: #6366f1; border-bottom-color: #6366f1; }

.tab-content {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 800px;
  margin: 0 auto;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.stat-icon { font-size: 1.5rem; margin-bottom: 6px; }
.stat-val { font-size: 1.5rem; font-weight: 800; color: #6366f1; }
.stat-label { font-size: 0.7rem; color: #94a3b8; margin-top: 2px; }

.card {
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
.card-desc { font-size: 0.8rem; color: #94a3b8; margin-bottom: 12px; margin-top: -10px; }

/* 차트 */
.chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 120px;
  padding-bottom: 24px;
  position: relative;
}
.chart-bar-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  position: relative;
}
.chart-bar-track {
  flex: 1;
  width: 100%;
  background: #f1f5f9;
  border-radius: 6px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}
.chart-bar {
  width: 100%;
  border-radius: 6px;
  transition: height 0.3s;
  min-height: 4px;
}
.chart-label {
  position: absolute;
  bottom: -20px;
  font-size: 0.7rem;
  color: #94a3b8;
  font-weight: 500;
}
.chart-score {
  position: absolute;
  top: -18px;
  font-size: 0.7rem;
  font-weight: 700;
  color: #6366f1;
}

.insight-card { border: 1.5px solid #e0e7ff; background: #fafafe; }
.insight-text { font-size: 0.875rem; line-height: 1.8; color: #374151; white-space: pre-wrap; }

.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.keep-title { color: #059669; }
.problem-title { color: #d97706; }

.keyword-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
}
.keyword-item:last-child { border-bottom: none; }
.keyword-rank {
  width: 20px;
  height: 20px;
  background: #e0e7ff;
  color: #6366f1;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 700;
  flex-shrink: 0;
}
.problem-rank { background: #fef3c7; color: #d97706; }
.keyword-text { font-size: 0.85rem; color: #374151; }

.kw-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}
.kw-row:last-child { border-bottom: none; }
.kw-word { font-size: 0.875rem; font-weight: 600; color: #374151; }
.kw-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 3px 10px;
  background: #fef3c7;
  color: #d97706;
  border-radius: 20px;
}

.warn-card {
  border: 1.5px solid #fecaca;
  background: #fff5f5;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 6px;
}
.warn-icon { font-size: 2rem; }
.warn-title { font-size: 1rem; font-weight: 700; color: #ef4444; }
.warn-desc { font-size: 0.85rem; color: #64748b; }

.rec-text { font-size: 0.875rem; line-height: 1.8; color: #374151; white-space: pre-wrap; }

.tip-card { background: #f0fdf4; border: 1.5px solid #bbf7d0; }
.tip-item { font-size: 0.85rem; color: #374151; padding: 5px 0; line-height: 1.5; }

.btn-primary {
  margin-top: 12px;
  padding: 10px 20px;
  background: #6366f1;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
}

.empty-text { font-size: 0.85rem; color: #94a3b8; line-height: 1.6; }
</style>
