<template>
  <div class="retro-page">
    <div class="retro-header">
      <button class="btn-back" @click="router.push('/calendar')">← 뒤로</button>
      <h2 class="retro-date">{{ formatDate(route.params.date) }}</h2>
      <button v-if="retrospect?.id" class="btn-delete" @click="deleteRetro">삭제</button>
      <div v-else style="width:40px" />
    </div>

    <div class="retro-body">
      <!-- 만족도 -->
      <div class="card">
        <div class="section-label">만족도</div>
        <div class="score-row">
          <button
            v-for="n in 5"
            :key="n"
            class="score-btn"
            :class="{ active: form.score === n }"
            :style="form.score === n ? { background: scoreColors[n], borderColor: scoreColors[n] } : {}"
            @click="form.score = n"
          >
            {{ n }}
          </button>
          <span class="score-emoji">{{ scoreEmoji[form.score] }}</span>
        </div>
      </div>

      <!-- K -->
      <div class="card kpt-card keep-card">
        <div class="kpt-header">
          <div class="kpt-tag keep-tag">K</div>
          <span class="kpt-title">Keep — 잘한 점, 계속할 것</span>
          <button class="btn-guide" @click="openGuide('keep')">?</button>
        </div>
        <textarea v-model="form.keep" placeholder="오늘 잘한 점이나 유지하고 싶은 것을 적어보세요." rows="4" />
      </div>

      <!-- P -->
      <div class="card kpt-card problem-card">
        <div class="kpt-header">
          <div class="kpt-tag problem-tag">P</div>
          <span class="kpt-title">Problem — 문제점, 개선할 점</span>
          <button class="btn-guide" @click="openGuide('problem')">?</button>
        </div>
        <textarea v-model="form.problem" placeholder="아쉬웠던 점이나 개선이 필요한 것을 적어보세요." rows="4" />
      </div>

      <!-- T -->
      <div class="card kpt-card try-card">
        <div class="kpt-header">
          <div class="kpt-tag try-tag">T</div>
          <span class="kpt-title">Try — 다음에 시도할 것</span>
          <button class="btn-guide" @click="openGuide('try')">?</button>
        </div>
        <textarea v-model="form.tryContent" placeholder="다음에 시도해볼 구체적인 행동을 적어보세요." rows="4" />

        <!-- AI 추천 -->
        <button class="btn-ai" @click="suggestTry" :disabled="aiLoading">
          {{ aiLoading ? '분석 중...' : '✨ AI 추천 받기' }}
        </button>
        <div v-if="aiSuggestion" class="ai-box">
          <p class="ai-box-label">AI 추천</p>
          <p class="ai-box-text">{{ aiSuggestion }}</p>
          <button class="btn-apply" @click="applyAI">적용하기</button>
        </div>
      </div>

      <!-- 하단 버튼 -->
      <div class="action-row">
        <button class="btn-save" @click="save" :disabled="saving">
          {{ saving ? '저장 중...' : (retrospect?.id ? '수정하기' : '저장하기') }}
        </button>
        <button
          v-if="retrospect?.id && authStore.user?.githubConnected"
          class="btn-github"
          @click="pushToGithub"
          :disabled="githubLoading"
        >
          {{ githubLoading ? '업로드 중...' : '🐙 GitHub에 올리기' }}
        </button>
      </div>
    </div>

    <!-- 가이드 모달 -->
    <div v-if="guide.show" class="modal-overlay" @click.self="guide.show = false">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ guide.title }} 작성 가이드</h3>
          <button @click="guide.show = false">✕</button>
        </div>
        <div class="modal-body">
          <p v-if="guide.loading" class="guide-loading">가이드 불러오는 중...</p>
          <p v-else class="guide-text">{{ guide.content }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { retrospectApi } from '@/api/retrospect'
import { aiApi } from '@/api/ai'
import { githubApi } from '@/api/github'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const retrospect = ref(null)
const saving = ref(false)
const aiLoading = ref(false)
const aiSuggestion = ref('')
const githubLoading = ref(false)

const form = reactive({ score: 3, keep: '', problem: '', tryContent: '' })

const scoreColors = { 1: '#fca5a5', 2: '#fdba74', 3: '#fde68a', 4: '#86efac', 5: '#34d399' }
const scoreEmoji = { 1: '😞', 2: '😕', 3: '😐', 4: '🙂', 5: '😄' }

const guide = reactive({ show: false, title: '', content: '', loading: false })

function formatDate(dateStr) {
  if (!dateStr) return ''
  const [y, m, d] = dateStr.split('-')
  return `${y}년 ${parseInt(m)}월 ${parseInt(d)}일`
}

async function loadRetro() {
  try {
    const data = await retrospectApi.getByDate(route.params.date)
    if (data) {
      retrospect.value = data
      form.score = data.score
      form.keep = data.keep || ''
      form.problem = data.problem || ''
      form.tryContent = data.tryContent || ''
    }
  } catch (e) {
    if (e.response?.status !== 404) console.error(e)
  }
}

async function save() {
  saving.value = true
  try {
    const payload = { date: route.params.date, keep: form.keep, problem: form.problem, tryContent: form.tryContent, score: form.score }
    if (retrospect.value?.id) await retrospectApi.update(retrospect.value.id, payload)
    else await retrospectApi.create(payload)
    router.push('/calendar')
  } catch { alert('저장에 실패했습니다.') }
  finally { saving.value = false }
}

async function deleteRetro() {
  if (!confirm('회고를 삭제할까요?')) return
  await retrospectApi.delete(retrospect.value.id)
  router.push('/calendar')
}

async function suggestTry() {
  if (!form.keep && !form.problem) return alert('Keep 또는 Problem을 먼저 작성해주세요.')
  aiLoading.value = true
  aiSuggestion.value = ''
  try {
    const res = await aiApi.suggestTry(form.keep, form.problem)
    aiSuggestion.value = res.suggestion
  } catch { alert('AI 서비스 오류가 발생했습니다.') }
  finally { aiLoading.value = false }
}

function applyAI() { form.tryContent = aiSuggestion.value; aiSuggestion.value = '' }

async function pushToGithub() {
  if (!retrospect.value?.id) return
  githubLoading.value = true
  try {
    await githubApi.push(retrospect.value.id)
    alert('GitHub에 성공적으로 업로드되었습니다! 🎉')
  } catch (e) {
    const msg = e.response?.data?.message || 'GitHub 업로드에 실패했습니다.'
    alert(msg)
  } finally {
    githubLoading.value = false
  }
}

async function openGuide(field) {
  const titles = { keep: 'Keep', problem: 'Problem', try: 'Try' }
  guide.title = titles[field]
  guide.show = true
  guide.loading = true
  guide.content = ''
  try {
    const res = await aiApi.getGuide(field)
    guide.content = res.guide
  } catch { guide.content = '가이드를 불러오지 못했습니다.' }
  finally { guide.loading = false }
}

onMounted(loadRetro)
</script>

<style scoped>
.retro-page { background: #f8fafc; min-height: 100%; }

.retro-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 56px;
  z-index: 10;
}

.btn-back { background: none; color: #6366f1; font-weight: 600; font-size: 0.9rem; border: none; cursor: pointer; }
.retro-date { font-size: 1rem; font-weight: 700; color: #1e293b; }
.btn-delete { background: none; color: #ef4444; font-size: 0.8rem; padding: 5px 10px; border: 1px solid #fecaca; border-radius: 6px; cursor: pointer; }

.retro-body {
  max-width: 680px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.section-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #475569;
  margin-bottom: 12px;
}

.score-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.score-btn {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  border: 2px solid #e2e8f0;
  background: #f8fafc;
  font-weight: 700;
  font-size: 1rem;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}
.score-btn:hover { border-color: #6366f1; }
.score-btn.active { color: #1e293b; transform: scale(1.1); }
.score-emoji { font-size: 1.5rem; margin-left: 4px; }

.kpt-card { border-left: 4px solid transparent; }
.keep-card { border-left-color: #10b981; }
.problem-card { border-left-color: #f59e0b; }
.try-card { border-left-color: #6366f1; }

.kpt-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.kpt-tag {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 800;
  color: #fff;
  flex-shrink: 0;
}
.keep-tag { background: #10b981; }
.problem-tag { background: #f59e0b; }
.try-tag { background: #6366f1; }

.kpt-title { font-size: 0.875rem; font-weight: 600; color: #374151; flex: 1; }

.btn-guide {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #f1f5f9;
  border: none;
  color: #94a3b8;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  flex-shrink: 0;
}

textarea {
  width: 100%;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  font-size: 0.9rem;
  line-height: 1.6;
  resize: vertical;
  min-height: 100px;
  color: #374151;
  font-family: inherit;
  transition: border-color 0.15s;
}
textarea:focus { outline: none; border-color: #6366f1; }

.btn-ai {
  margin-top: 12px;
  width: 100%;
  padding: 11px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-ai:disabled { opacity: 0.6; cursor: not-allowed; }

.ai-box {
  margin-top: 12px;
  background: #f5f3ff;
  border: 1px solid #ddd6fe;
  border-radius: 10px;
  padding: 14px;
}
.ai-box-label { font-size: 0.75rem; font-weight: 700; color: #7c3aed; margin-bottom: 6px; }
.ai-box-text { font-size: 0.875rem; line-height: 1.7; white-space: pre-wrap; color: #374151; }
.btn-apply {
  margin-top: 10px;
  padding: 7px 14px;
  background: #7c3aed;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}

.action-row { display: flex; gap: 10px; }

.btn-save {
  flex: 1;
  padding: 15px;
  background: #6366f1;
  color: #fff;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-save:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-github {
  padding: 15px 18px;
  background: #24292e;
  color: #fff;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.875rem;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.15s;
}
.btn-github:disabled { opacity: 0.6; cursor: not-allowed; }

/* 모달 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 100;
  padding: 16px;
}
@media (min-width: 600px) {
  .modal-overlay { align-items: center; }
}

.modal {
  background: #fff;
  border-radius: 20px;
  width: 100%;
  max-width: 520px;
  max-height: 75vh;
  overflow-y: auto;
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid #f1f5f9;
}
.modal-head h3 { font-size: 1rem; font-weight: 700; color: #1e293b; }
.modal-head button { background: none; border: none; font-size: 1.1rem; color: #94a3b8; cursor: pointer; }
.modal-body { padding: 20px; }
.guide-text { font-size: 0.875rem; line-height: 1.8; white-space: pre-wrap; color: #374151; }
.guide-loading { font-size: 0.875rem; color: #94a3b8; }
</style>
