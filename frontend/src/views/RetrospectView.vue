<template>
  <div class="retro-page">
    <div class="retro-header">
      <button class="btn-back" @click="router.push('/calendar')">← 뒤로</button>
      <h2 class="retro-date">{{ formatDate(route.params.date) }}</h2>
      <button v-if="retrospectId" class="btn-delete" @click="deleteRetro">삭제</button>
      <div v-else style="width:40px" />
    </div>

    <!-- 미래 날짜: 접근 불가 -->
    <div v-if="isFuture" class="blocked-msg">접근할 수 없는 페이지입니다.</div>

    <div v-else class="retro-body">
      <!-- 만족도 -->
      <div class="card">
        <div class="section-label">오늘의 만족도</div>
        <div class="score-row">
          <button
            v-for="n in 5"
            :key="n"
            class="score-btn"
            :class="{ active: score === n }"
            :style="score === n ? { background: scoreColors[n], borderColor: scoreColors[n] } : {}"
            :disabled="isReadOnly"
            @click="changeScore(n)"
          >
            {{ n }}
          </button>
          <span class="score-emoji">{{ scoreEmoji[score] }}</span>
        </div>
      </div>

      <!-- K / P / T -->
      <div v-for="t in TYPES" :key="t.key" class="card kpt-card" :class="`${t.key}-card`">
        <div class="kpt-header">
          <div class="kpt-tag" :class="`${t.key}-tag`">{{ t.badge }}</div>
          <span class="kpt-title">{{ t.label }}</span>
          <button class="btn-guide" @click="openGuide(t.key)">?</button>
        </div>

        <div class="item-list">
          <div
            v-for="(item, idx) in lists[t.key]"
            :key="item.id"
            class="kpt-item"
            :draggable="!isReadOnly"
            @dragstart="onDragStart(t.key, idx)"
            @dragover.prevent
            @drop="onDrop(t.key, idx)"
          >
            <span class="item-num">{{ t.badge }}{{ idx + 1 }}</span>
            <input
              v-if="!isReadOnly"
              class="item-input"
              :value="item.content"
              @input="onItemInput(t.key, item, $event.target.value)"
            />
            <span v-else class="item-text">{{ item.content }}</span>
            <button v-if="!isReadOnly" class="item-del" @click="removeItem(t.key, item)">×</button>
          </div>

          <div v-if="draft[t.key] !== null" class="kpt-item">
            <span class="item-num">{{ t.badge }}{{ lists[t.key].length + 1 }}</span>
            <input
              ref="draftInputs"
              class="item-input"
              :value="draft[t.key]"
              :placeholder="t.placeholder"
              @input="onDraftInput(t.key, $event.target.value)"
              @blur="onDraftBlur(t.key)"
            />
          </div>
        </div>

        <button v-if="!isReadOnly" class="btn-add-item" :disabled="lists[t.key].length >= 20" @click="openDraft(t.key)">
          + 추가
        </button>

        <!-- AI Try 추천 -->
        <template v-if="t.key === 'tryItems' && !isReadOnly">
          <button class="btn-ai" @click="suggestTry" :disabled="aiLoading">
            {{ aiLoading ? '생성 중...' : '✨ AI 추천 받기' }}
          </button>
          <div v-if="aiSuggestion" class="ai-box">
            <p class="ai-box-label">AI 추천</p>
            <p class="ai-box-text">{{ aiSuggestion }}</p>
            <button class="btn-apply" @click="addAiSuggestion" :disabled="lists.tryItems.length >= 20">추가</button>
          </div>
        </template>
      </div>

      <!-- 하단 액션 바 -->
      <div class="action-row">
        <button class="btn-save" @click="router.push('/calendar')">저장</button>
        <span class="save-status" :class="saveStatusClass">{{ saveStatusText }}</span>
        <template v-if="retrospectId">
          <button
            v-if="authStore.user?.githubConnected"
            class="btn-github"
            @click="pushToGithub"
            :disabled="githubLoading || saveState !== 'saved'"
          >
            {{ githubLoading ? '업로드 중...' : (isGithubSynced ? 'GitHub 최신' : '🐙 GitHub 업로드') }}
          </button>
          <button v-else class="btn-github" @click="githubNeedSetup = true">GitHub 연동 필요</button>
        </template>
      </div>

      <div v-if="githubNeedSetup" class="github-setup-msg">
        먼저 GitHub를 연동해주세요.
        <button @click="router.push('/settings')">설정으로 이동</button>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { retrospectApi } from '@/api/retrospect'
import { aiApi } from '@/api/ai'
import { githubApi } from '@/api/github'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const TYPES = [
  { key: 'keep', apiType: 'KEEP', badge: 'K', label: 'Keep — 잘한 점, 계속할 것', placeholder: '오늘 계속 이어가고 싶은 건 무엇인가요?' },
  { key: 'problem', apiType: 'PROBLEM', badge: 'P', label: 'Problem — 문제점, 개선할 점', placeholder: '오늘 아쉬웠던 점은 무엇인가요?' },
  { key: 'tryItems', apiType: 'TRY', badge: 'T', label: 'Try — 다음에 시도할 것', placeholder: '다음엔 무엇을 시도해볼까요?' },
]

const scoreColors = { 1: '#FFADAD', 2: '#FFD6A5', 3: '#FDFFB6', 4: '#CAFFBF', 5: '#8CD98C' }
const scoreEmoji = { 1: '😞', 2: '😕', 3: '😐', 4: '🙂', 5: '😄' }

const retrospectId = ref(null)
const isGithubSynced = ref(false)
const score = ref(3)
const lists = reactive({ keep: [], problem: [], tryItems: [] })
const draft = reactive({ keep: null, problem: null, tryItems: null })

const saveState = ref('saved') // 'saved' | 'unsaved' | 'saving'
let pending = 0

const aiLoading = ref(false)
const aiSuggestion = ref('')
const githubLoading = ref(false)
const githubNeedSetup = ref(false)
const guide = reactive({ show: false, title: '', content: '', loading: false })
const itemTimers = {}
const draftTimers = {}

const today = new Date()
today.setHours(0, 0, 0, 0)

function toIso(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const isFuture = computed(() => route.params.date > toIso(today))
const isReadOnly = computed(() => {
  const cutoff = new Date(today)
  cutoff.setDate(cutoff.getDate() - 14)
  return route.params.date < toIso(cutoff)
})

const saveStatusText = computed(() => ({
  saved: '저장 완료',
  unsaved: '저장 전',
  saving: '저장 중...',
}[saveState.value]))

const saveStatusClass = computed(() => `status-${saveState.value}`)

function formatDate(dateStr) {
  if (!dateStr) return ''
  const [y, m, d] = dateStr.split('-')
  return `${y}년 ${parseInt(m)}월 ${parseInt(d)}일`
}

async function trackSave(promise) {
  pending++
  saveState.value = 'saving'
  try {
    const res = await promise
    pending = Math.max(0, pending - 1)
    if (pending === 0) saveState.value = 'saved'
    return res
  } catch (e) {
    pending = Math.max(0, pending - 1)
    alert(e.response?.data?.message || '저장에 실패했습니다.')
    saveState.value = 'unsaved'
    throw e
  }
}

function applyResponse(data) {
  retrospectId.value = data.id
  isGithubSynced.value = data.isGithubSynced
  score.value = data.score
  lists.keep = data.keep
  lists.problem = data.problem
  lists.tryItems = data.tryItems
}

async function loadRetro() {
  if (isFuture.value) return
  try {
    const data = await retrospectApi.getByDate(route.params.date)
    if (data) applyResponse(data)
  } catch (e) {
    if (e.response?.status !== 404) console.error(e)
  }
}

async function changeScore(n) {
  if (isReadOnly.value) return
  score.value = n
  try {
    const data = await trackSave(retrospectApi.upsertScore(route.params.date, n))
    applyResponse(data)
  } catch { /* handled in trackSave */ }
}

function onItemInput(typeKey, item, value) {
  item.content = value
  saveState.value = pending > 0 ? 'saving' : 'unsaved'
  clearTimeout(itemTimers[item.id])
  itemTimers[item.id] = setTimeout(async () => {
    const trimmed = item.content.trim()
    if (!trimmed) return
    try {
      const data = await trackSave(retrospectApi.updateItem(route.params.date, item.id, trimmed))
      applyResponse(data)
    } catch { /* handled */ }
  }, 1000)
}

async function removeItem(typeKey, item) {
  try {
    const data = await trackSave(retrospectApi.deleteItem(route.params.date, item.id))
    applyResponse(data)
  } catch { /* handled */ }
}

function openDraft(typeKey) {
  if (lists[typeKey].length >= 20) {
    alert('최대 20개까지 작성 가능합니다.')
    return
  }
  if (draft[typeKey] === null) draft[typeKey] = ''
}

function onDraftInput(typeKey, value) {
  draft[typeKey] = value
  saveState.value = pending > 0 ? 'saving' : 'unsaved'
  clearTimeout(draftTimers[typeKey])
  draftTimers[typeKey] = setTimeout(() => commitDraft(typeKey), 1000)
}

function onDraftBlur(typeKey) {
  clearTimeout(draftTimers[typeKey])
  commitDraft(typeKey)
}

async function commitDraft(typeKey) {
  const content = (draft[typeKey] || '').trim()
  if (!content) return
  if (lists[typeKey].length >= 20) {
    alert('최대 20개까지 작성 가능합니다.')
    draft[typeKey] = null
    return
  }
  const type = TYPES.find(t => t.key === typeKey).apiType
  try {
    const data = await trackSave(retrospectApi.addItem(route.params.date, type, content))
    applyResponse(data)
    draft[typeKey] = null
  } catch { /* handled */ }
}

let dragState = null
function onDragStart(typeKey, index) {
  dragState = { typeKey, index }
}

async function onDrop(typeKey, targetIndex) {
  if (!dragState || dragState.typeKey !== typeKey) return
  const arr = [...lists[typeKey]]
  const [moved] = arr.splice(dragState.index, 1)
  arr.splice(targetIndex, 0, moved)
  lists[typeKey] = arr
  dragState = null

  const type = TYPES.find(t => t.key === typeKey).apiType
  try {
    const data = await trackSave(retrospectApi.reorderItems(route.params.date, type, arr.map(i => i.id)))
    applyResponse(data)
  } catch { /* handled */ }
}

async function deleteRetro() {
  if (!confirm('회고를 삭제할까요?')) return
  await retrospectApi.delete(retrospectId.value)
  router.push('/calendar')
}

async function suggestTry() {
  const keepText = lists.keep.map(i => i.content).join('\n')
  const problemText = lists.problem.map(i => i.content).join('\n')
  if (!keepText && !problemText) return alert('Keep 또는 Problem을 먼저 작성해주세요.')
  aiLoading.value = true
  aiSuggestion.value = ''
  try {
    const res = await aiApi.suggestTry(keepText, problemText)
    aiSuggestion.value = res.suggestion
  } catch { alert('AI 서비스 오류가 발생했습니다.') }
  finally { aiLoading.value = false }
}

async function addAiSuggestion() {
  if (!aiSuggestion.value || lists.tryItems.length >= 20) return
  try {
    const data = await trackSave(retrospectApi.addItem(route.params.date, 'TRY', aiSuggestion.value))
    applyResponse(data)
    aiSuggestion.value = ''
  } catch { /* handled */ }
}

async function pushToGithub() {
  if (!retrospectId.value) return
  githubLoading.value = true
  try {
    await githubApi.push(retrospectId.value)
    isGithubSynced.value = true
    alert('GitHub에 성공적으로 업로드되었습니다! 🎉')
  } catch (e) {
    const msg = e.response?.data?.message || 'GitHub 업로드에 실패했습니다.'
    alert(msg)
  } finally {
    githubLoading.value = false
  }
}

async function openGuide(field) {
  const titles = { keep: 'Keep', problem: 'Problem', tryItems: 'Try' }
  guide.title = titles[field]
  guide.show = true
  guide.loading = true
  guide.content = ''
  try {
    const res = await aiApi.getGuide(field === 'tryItems' ? 'try' : field)
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

.blocked-msg {
  max-width: 680px;
  margin: 60px auto;
  text-align: center;
  color: #94a3b8;
  font-size: 1rem;
}

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
.score-btn:disabled { cursor: not-allowed; opacity: 0.7; }
.score-emoji { font-size: 1.5rem; margin-left: 4px; }

.kpt-card { border-left: 4px solid transparent; }
.keep-card { border-left-color: #10b981; }
.problem-card { border-left-color: #f59e0b; }
.tryItems-card { border-left-color: #6366f1; }

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
.tryItems-tag { background: #6366f1; }

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

.item-list { display: flex; flex-direction: column; gap: 8px; }

.kpt-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-num {
  font-size: 0.75rem;
  font-weight: 700;
  color: #94a3b8;
  width: 28px;
  flex-shrink: 0;
}

.item-input, .item-text {
  flex: 1;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  padding: 9px 10px;
  font-size: 0.875rem;
  color: #374151;
  font-family: inherit;
}
.item-input:focus { outline: none; border-color: #6366f1; }
.item-text { border-color: transparent; background: #f8fafc; }

.item-del {
  background: none;
  border: none;
  color: #cbd5e1;
  font-size: 1.1rem;
  cursor: pointer;
  flex-shrink: 0;
  width: 22px;
}
.item-del:hover { color: #ef4444; }

.btn-add-item {
  margin-top: 10px;
  background: none;
  border: 1.5px dashed #cbd5e1;
  border-radius: 8px;
  padding: 8px;
  width: 100%;
  color: #64748b;
  font-size: 0.825rem;
  cursor: pointer;
}
.btn-add-item:hover { border-color: #6366f1; color: #6366f1; }
.btn-add-item:disabled { opacity: 0.5; cursor: not-allowed; }

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
.btn-apply:disabled { opacity: 0.6; cursor: not-allowed; }

.action-row { display: flex; align-items: center; gap: 10px; }

.btn-save {
  padding: 15px 22px;
  background: #6366f1;
  color: #fff;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
}

.save-status {
  flex: 1;
  font-size: 0.8rem;
  font-weight: 600;
}
.status-saved { color: #cbd5e1; }
.status-unsaved { color: #f59e0b; }
.status-saving { color: #6366f1; }

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

.github-setup-msg {
  text-align: right;
  font-size: 0.8rem;
  color: #94a3b8;
}
.github-setup-msg button {
  margin-left: 8px;
  background: none;
  border: none;
  color: #6366f1;
  font-weight: 600;
  cursor: pointer;
}

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
