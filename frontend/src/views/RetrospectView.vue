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

    <div v-else-if="pageLoading" class="blocked-msg"><DotLoader /></div>

    <!-- 14일 지난 날짜에 회고가 없음: 새로 작성 불가 -->
    <div v-else-if="isReadOnly && !retrospectId" class="blocked-msg">
      작성 기간(14일)이 지난 날짜라 회고를 작성할 수 없습니다.
    </div>

    <div v-else class="retro-body">
      <!-- 오늘의 점: 만족도로 크기가 정해지고, K·P·T를 채울수록 잉크가 차오름 -->
      <div ref="todayCardEl" class="card today-card">
        <div class="today-dot-wrap">
          <TodayDot
            :diameter="dotDiameter"
            :done="doneKeys"
            :complete="dotFill === 1"
            :celebrate="dotCelebrate"
            :pulse="dotPulse"
          />
        </div>

        <div class="today-main">
          <div class="section-label">{{ dotName }}</div>
          <p class="today-caption">{{ dotCaption }}</p>
          <div class="kpt-pips">
            <span
              v-for="t in TYPES"
              :key="t.key"
              class="kpt-pip"
              :class="[`pip-${t.key}`, { done: sectionDone[t.key] }]"
              :title="`${t.badge} ${sectionDone[t.key] ? '작성함' : '아직 비어 있음'}`"
            >{{ t.badge }}</span>
          </div>
        </div>
      </div>

      <!-- 만족도 -->
      <div class="card">
        <div class="section-label">오늘의 만족도</div>
        <div class="score-row">
          <button
            v-for="n in 5"
            :key="n"
            class="score-btn"
            :class="{ active: score === n }"
            :aria-label="`만족도 ${n}점`"
            :disabled="isReadOnly"
            @click="changeScore(n)"
          >
            <span class="ink-dot" :style="dotStyle(n, 'sm')" />
            <span class="score-num">{{ n }}</span>
          </button>
        </div>
      </div>

      <!-- K / P / T -->
      <div v-for="t in TYPES" :key="t.key" class="card kpt-card" :class="`${t.key}-card`">
        <div class="kpt-header">
          <div class="kpt-tag" :class="`${t.key}-tag`">{{ t.badge }}</div>
          <span class="kpt-title">{{ t.label }}</span>
          <span
            v-if="!isReadOnly && aiQuota.REFINE"
            class="refine-count"
            :class="{ empty: refineLeft === 0 }"
            title="오늘 남은 AI 다듬기 횟수 (K·P·T 공통)"
          ><span class="pencil-icon">✎</span> {{ aiQuota.REFINE.remaining }}/{{ aiQuota.REFINE.limit }}</span>
          <button class="btn-guide" @click="openGuide(t.key)">?</button>
        </div>

        <div class="item-list">
          <div
            v-for="(item, idx) in lists[t.key]"
            :key="item._uid"
            class="kpt-item-wrap"
            :draggable="!isReadOnly && !isTemp(item) && refine.uid !== item._uid"
            @dragstart="onDragStart(t.key, idx)"
            @dragover.prevent
            @drop="onDrop(t.key, idx)"
          >
          <div class="kpt-item">
            <span class="item-num">{{ t.badge }}{{ idx + 1 }}</span>
            <textarea
              v-if="!isReadOnly"
              v-autogrow
              rows="1"
              class="item-input"
              :class="{ saving: isTemp(item) && item.content.trim() }"
              :value="item.content"
              :placeholder="t.placeholder"
              :ref="el => focusIfPending(el, item._uid)"
              @input="onItemInput(t.key, item, $event.target.value)"
              @keydown.enter.exact="onItemEnter"
              @blur="onItemBlur(t.key, item)"
            />
            <span v-else class="item-text">{{ item.content }}</span>
            <button
              v-if="!isReadOnly"
              class="item-refine"
              :class="{ active: refine.uid === item._uid }"
              :disabled="!item.content.trim() || isTemp(item) || refine.loading || refineLeft === 0"
              :title="refineLeft === 0 ? '오늘 다듬기를 다 썼어요' : 'AI로 문장 다듬기'"
              :aria-label="`${t.badge}${idx + 1} 문장 다듬기`"
              @click="startRefine(t.key, item)"
            >✎</button>
            <button v-if="!isReadOnly" class="item-del" :disabled="item._creating" @click="removeItem(t.key, item)">×</button>
          </div>

          <!-- 다듬기: 고치기 전·후 비교 -->
          <div v-if="refine.uid === item._uid" class="refine-box" :class="`refine-${t.key}`">
            <div v-if="refine.loading" class="refine-loading"><DotLoader label="문장을 다듬는 중" :size="10" /></div>
            <template v-else>
              <div class="refine-row">
                <span class="refine-label">원래</span>
                <span class="refine-before">{{ refine.original }}</span>
              </div>
              <div class="refine-row">
                <span class="refine-label">다듬은 문장</span>
                <span class="refine-after"><span class="refine-hl">{{ refine.refined }}</span></span>
              </div>
              <div class="refine-actions">
                <span v-if="aiQuota.REFINE" class="refine-quota">오늘 남은 다듬기 {{ aiQuota.REFINE.remaining }}/{{ aiQuota.REFINE.limit }}</span>
                <button class="btn-refine-keep" @click="closeRefine">그대로 두기</button>
                <button class="btn-refine-apply" @click="applyRefine(t.key, item)">적용</button>
              </div>
            </template>
          </div>
          </div>
        </div>

        <button v-if="!isReadOnly" class="btn-add-item" :disabled="lists[t.key].length >= 20" @click="addBlankItem(t.key)">
          + 추가
        </button>

        <!-- AI Try 추천 -->
        <template v-if="t.key === 'tryItems' && !isReadOnly">
          <button class="btn-ai" @click="suggestTry" :disabled="aiLoading || tryQuotaLeft === 0">
            <Icon name="sparkle" :size="15" />
            {{ aiLoading ? '생성 중...' : tryQuotaLeft === 0 ? '오늘 AI 추천을 다 썼어요' : 'AI 추천 받기' }}
            <span v-if="aiQuota.TRY_RECOMMENDATION" class="quota-chip">
              {{ aiQuota.TRY_RECOMMENDATION.remaining }}/{{ aiQuota.TRY_RECOMMENDATION.limit }}
            </span>
          </button>
          <p v-if="tryQuotaLeft === 0" class="quota-note">내일 자정에 다시 채워져요.</p>
          <!-- 추천 히스토리: 다시 받아도, 나갔다 들어와도 남아 있음 (작성 기간 14일이 지나면 자동 삭제) -->
          <div v-if="allSuggestions.length" class="ai-box">
            <div class="ai-box-head">
              <p class="ai-box-label">AI 추천</p>
              <button
                v-if="allSuggestions.some(t => !isAdded(t))"
                class="btn-apply-all"
                @click="addAllAiSuggestions"
                :disabled="saveState === 'saving'"
              >모두 추가</button>
            </div>
            <div v-for="text in allSuggestions" :key="text" class="ai-suggestion" :class="{ added: isAdded(text) }">
              <span class="ai-suggestion-text">{{ text }}</span>
              <span v-if="isAdded(text)" class="ai-added">추가됨</span>
              <button v-else class="btn-apply" @click="addAiSuggestion(text)" :disabled="saveState === 'saving'">+ 추가</button>
            </div>
          </div>
        </template>
      </div>

      <!-- 하단 액션 바 -->
      <div ref="actionRowEl" class="action-row">
        <!-- 14일 지난 회고는 고칠 수 없으므로 저장 버튼·저장 상태를 숨김 -->
        <button v-if="!isReadOnly" class="btn-save" @click="saveAndBack">저장</button>
        <span class="save-status" :class="saveStatusClass">{{ isReadOnly ? '' : saveStatusText }}</span>
        <template v-if="retrospectId">
          <button
            v-if="authStore.user?.githubConnected"
            class="btn-github"
            @click="pushToGithub"
            :disabled="githubLoading || saveState !== 'saved'"
          >
            <Icon v-if="!isGithubSynced" name="upload" :size="15" /> {{ githubLoading ? '업로드 중...' : (isGithubSynced ? 'GitHub 최신' : 'GitHub 업로드') }}
          </button>
          <button v-else class="btn-github" @click="githubNeedSetup = true">GitHub 연동 필요</button>
        </template>
      </div>

      <div v-if="githubNeedSetup" class="github-setup-msg">
        먼저 GitHub를 연동해주세요.
        <button @click="router.push('/settings')">설정으로 이동</button>
      </div>
    </div>

    <!-- 떠 있는 오늘의 점: 밑에서 K·P·T를 쓰는 동안에도 점이 차오르는 게 보이게 -->
    <Transition name="float-dot">
      <button
        v-if="showDotUi && !todayCardVisible"
        class="float-dot"
        :style="{ bottom: `${floatBottom}px` }"
        :class="{ complete: dotFill === 1 }"
        :title="dotCaption"
        @click="scrollToTodayCard"
      >
        <TodayDot
          :diameter="floatDotDiameter"
          :done="doneKeys"
          :complete="dotFill === 1"
          :celebrate="dotCelebrate"
          :pulse="dotPulse"
        />
        <span class="float-dot-text">
          <span class="float-dot-title">{{ dotName }}</span>
          <span class="float-dot-pips">
            <span
              v-for="t in TYPES"
              :key="t.key"
              class="float-pip"
              :class="[`pip-${t.key}`, { done: sectionDone[t.key] }]"
            >{{ t.badge }}</span>
          </span>
        </span>
      </button>
    </Transition>

    <!-- 작성 가이드 모달 -->
    <div v-if="guideKey" class="modal-overlay" @click.self="guideKey = null">
      <div class="modal guide-modal" :class="`guide-${guideKey}`" role="dialog" aria-modal="true">
        <div class="guide-head">
          <span class="guide-badge">{{ currentGuide.badge }}</span>
          <h3 class="guide-title">{{ currentGuide.name }} <span>작성 가이드</span></h3>
          <button class="guide-close" aria-label="닫기" @click="guideKey = null">✕</button>
        </div>

        <div class="guide-body">
          <p class="guide-summary">{{ currentGuide.summary }}</p>

          <div class="guide-section">
            <div class="guide-label">이렇게 물어보세요</div>
            <ul class="guide-questions">
              <li v-for="q in currentGuide.questions" :key="q">{{ q }}</li>
            </ul>
          </div>

          <div class="guide-section">
            <div class="guide-label">이렇게 바꿔 써보세요</div>
            <p class="guide-formula">{{ currentGuide.formula }}</p>
            <div v-for="ex in currentGuide.examples" :key="ex.bad" class="guide-example">
              <div class="ex-bad"><span class="ex-mark">✕</span>{{ ex.bad }}</div>
              <div class="ex-good"><span class="ex-mark">○</span><span class="ex-hl">{{ ex.good }}</span></div>
            </div>
          </div>

          <p class="guide-tip">{{ currentGuide.tip }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { retrospectApi } from '@/api/retrospect'
import { aiApi } from '@/api/ai'
import { githubApi } from '@/api/github'
import { useAuthStore } from '@/stores/auth'
import Icon from '@/components/Icon.vue'
import DotLoader from '@/components/DotLoader.vue'
import TodayDot from '@/components/TodayDot.vue'
import { modal } from '@/composables/useModal'
import { KPT_GUIDE } from '@/utils/kptGuide'
import { dotStyle } from '@/utils/inkDot'
import { aiQuota, refreshAiQuota, updateAiQuota, markQuotaExhausted } from '@/composables/useAiQuota'
import { calendarCache } from '@/utils/calendarCache'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const TYPES = [
  { key: 'keep', apiType: 'KEEP', badge: 'K', label: 'Keep — 잘한 점, 계속할 것', placeholder: '오늘 계속 이어가고 싶은 건 무엇인가요?' },
  { key: 'problem', apiType: 'PROBLEM', badge: 'P', label: 'Problem — 문제점, 개선할 점', placeholder: '오늘 아쉬웠던 점은 무엇인가요?' },
  { key: 'tryItems', apiType: 'TRY', badge: 'T', label: 'Try — 다음에 시도할 것', placeholder: '다음엔 무엇을 시도해볼까요?' },
]


const pageLoading = ref(false)
const retrospectId = ref(null)
const isGithubSynced = ref(false)
const score = ref(3)
const lists = reactive({ keep: [], problem: [], tryItems: [] })

const saveState = ref('saved') // 'saved' | 'unsaved' | 'saving'
let pending = 0

const aiLoading = ref(false)
const aiHistory = ref([]) // [{ batchId, createdAt, suggestions }] 최신순
// 받은 추천을 나누지 않고 한 목록으로 (최신 추천이 위, 같은 문장은 한 번만)
const allSuggestions = computed(() => [...new Set(aiHistory.value.flatMap(b => b.suggestions))])
// 이미 Try에 들어간 추천은 "추가됨"으로 표시
const addedTryTexts = computed(() => new Set(lists.tryItems.map(i => i.content.trim())))
function isAdded(text) { return addedTryTexts.value.has(text.trim()) }

async function loadAiHistory() {
  if (isFuture.value || isReadOnly.value) return
  try {
    aiHistory.value = await aiApi.suggestTryHistory(route.params.date)
  } catch { /* 히스토리를 못 불러와도 작성엔 지장 없음 */ }
}
const githubLoading = ref(false)
const githubNeedSetup = ref(false)
const guideKey = ref(null) // 'keep' | 'problem' | 'tryItems' | null
const currentGuide = computed(() => KPT_GUIDE[guideKey.value] || KPT_GUIDE.keep)
const itemTimers = {}
const pendingFocusUid = ref(null)

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
    modal.error(e.response?.data?.message || '저장에 실패했습니다.', { title: '저장 실패' })
    saveState.value = 'unsaved'
    throw e
  }
}

// 서버 id와 별개로 DOM :key에 쓸 안정적인 로컬 id.
// item.id는 확정 전엔 null(임시 항목)이었다가 서버 응답 후 실제 값으로 바뀌는데,
// _uid는 그 과정 내내 그대로 유지돼서 Vue가 같은 엘리먼트로 인식 -> 포커스가 안 끊김
let uidSeq = 0
function nextUid() { return ++uidSeq }

function isTemp(item) {
  return item.id == null
}

// 서버에서 받은 목록을 그대로 대입하지 않고, 이미 알고 있는 항목(id로 매칭)은
// 같은 객체(=같은 _uid)를 재사용해서 병합. 새 항목만 _uid를 새로 발급.
function mergeItems(typeKey, serverItems) {
  const oldById = new Map(lists[typeKey].filter(i => i.id != null).map(i => [i.id, i]))
  return (serverItems || []).map(si => {
    const existing = oldById.get(si.id)
    if (existing) {
      existing.content = si.content
      return existing
    }
    return { ...si, _uid: nextUid() }
  })
}

function monthKeyOf(dateStr) {
  const [y, m] = dateStr.split('-')
  return `${parseInt(y)}-${parseInt(m)}`
}

function applyResponse(data, { mutated = false } = {}) {
  retrospectId.value = data.id
  isGithubSynced.value = !!data.isGithubSynced
  score.value = data.score ?? 3
  lists.keep = mergeItems('keep', data.keep)
  lists.problem = mergeItems('problem', data.problem)
  lists.tryItems = mergeItems('tryItems', data.tryItems)
  // 캘린더 화면의 점수/작성여부 표시가 이 회고에 달려있으므로, 실제로 뭔가 바뀐 경우에만
  // 그 달의 캐시만 지워서(전체 캐시는 유지) 캘린더로 돌아갔을 때 최신 상태로 다시 불러오게 함
  if (mutated) calendarCache.delete(monthKeyOf(data.date || route.params.date))
}

function resetState() {
  retrospectId.value = null
  isGithubSynced.value = false
  score.value = 3
  lists.keep = []
  lists.problem = []
  lists.tryItems = []
  aiHistory.value = []
  closeRefine()
  githubNeedSetup.value = false
}

async function loadRetro() {
  resetState()
  if (isFuture.value) return
  pageLoading.value = true
  try {
    const data = await retrospectApi.getByDate(route.params.date)
    if (data) applyResponse(data)
    initFillOrder()
  } catch (e) {
    if (e.response?.status !== 404) console.error(e)
  } finally {
    suppressPulse = true
    pageLoading.value = false
    nextTick(() => { suppressPulse = false })
  }
}

async function changeScore(n) {
  if (isReadOnly.value) return
  score.value = n
  try {
    const data = await trackSave(retrospectApi.upsertScore(route.params.date, n))
    applyResponse(data, { mutated: true })
  } catch { /* handled in trackSave */ }
}

function focusIfPending(el, uid) {
  if (el && pendingFocusUid.value === uid) {
    pendingFocusUid.value = null
    el.focus()
  }
}

function onItemInput(typeKey, item, value) {
  item.content = value
  saveState.value = pending > 0 ? 'saving' : 'unsaved'
  clearTimeout(itemTimers[item._uid])
  itemTimers[item._uid] = setTimeout(() => commitItem(typeKey, item), 1000)
}

function onItemBlur(typeKey, item) {
  if (item.id != null) return // 이미 확정된 항목은 디바운스가 알아서 처리
  clearTimeout(itemTimers[item._uid])
  if (!item.content.trim()) {
    // 아무것도 안 쓰고 벗어난 빈 새 항목은 저장 없이 제거
    lists[typeKey] = lists[typeKey].filter(i => i !== item)
  } else {
    commitItem(typeKey, item)
  }
}

// 새 항목(id 없음)이든 기존 항목이든 같은 <input> 요소를 그대로 쓰면서 저장만 처리 —
// 화면 요소가 안 바뀌니 커서/포커스가 저장 전후로 끊기지 않음
async function commitItem(typeKey, item) {
  const trimmed = item.content.trim()
  if (!trimmed) return // 빈 값은 저장하지 않음
  try {
    let data;
    if (item.id == null) {
      if (item._creating) return // 같은 항목을 두 번 만들지 않음 (blur + 디바운스 겹칠 때)
      item._creating = true
      const type = TYPES.find(t => t.key === typeKey).apiType
      try {
        data = await trackSave(retrospectApi.addItem(route.params.date, type, trimmed))
      } finally {
        item._creating = false
      }
      const serverItem = (data[typeKey] || []).at(-1) // 방금 추가되어 맨 뒤에 붙은 항목
      if (serverItem) {
        item.id = serverItem.id
        if (item.content.trim() === trimmed) item.content = serverItem.content
      }
    } else {
      data = await trackSave(retrospectApi.updateItem(route.params.date, item.id, trimmed))
    }
    applyResponse(data, { mutated: true })
  } catch { /* handled */ }
}

async function removeItem(typeKey, item) {
  // 아직 서버에 없는 임시 항목(빈 칸, 저장 대기 중)은 화면에서만 지움
  if (item.id == null) {
    if (item._creating) return // 서버에 만드는 중이면 끝날 때까지 대기
    clearTimeout(itemTimers[item._uid])
    lists[typeKey] = lists[typeKey].filter(i => i !== item)
    return
  }
  // 낙관적 업데이트: 응답 기다리지 않고 바로 화면에서 제거, 실패하면 원래 위치로 복구
  const idx = lists[typeKey].indexOf(item)
  lists[typeKey] = lists[typeKey].filter(i => i !== item)
  try {
    const data = await trackSave(retrospectApi.deleteItem(route.params.date, item.id))
    applyResponse(data, { mutated: true })
  } catch {
    const arr = [...lists[typeKey]]
    arr.splice(idx, 0, item)
    lists[typeKey] = arr
  }
}

function addBlankItem(typeKey) {
  if (lists[typeKey].length >= 20) {
    modal.alert('최대 20개까지 작성 가능합니다.')
    return
  }
  const item = { id: null, content: '', _uid: nextUid() }
  lists[typeKey] = [...lists[typeKey], item]
  pendingFocusUid.value = item._uid
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
    const orderedIds = arr.filter(i => i.id != null).map(i => i.id)
    const data = await trackSave(retrospectApi.reorderItems(route.params.date, type, orderedIds))
    applyResponse(data, { mutated: true })
  } catch { /* handled */ }
}

// 저장(자동 저장됨) 후 캘린더로 — 회고가 있으면 캘린더에서 그 날짜에 점이 찍히는 효과를 보여줌
function saveAndBack() {
  router.push(retrospectId.value ? { path: '/calendar', query: { stamp: route.params.date } } : '/calendar')
}

async function deleteRetro() {
  const ok = await modal.confirm('작성한 Keep · Problem · Try가 모두 삭제됩니다.', {
    title: '회고를 삭제할까요?',
    type: 'danger',
    confirmText: '삭제',
  })
  if (!ok) return
  try {
    await retrospectApi.delete(retrospectId.value)
    router.push('/calendar')
  } catch (e) {
    modal.error(e.response?.data?.message || '회고 삭제에 실패했습니다.')
  }
}

async function suggestTry() {
  const keepText = lists.keep.map(i => i.content.trim()).filter(Boolean).join('\n')
  const problemText = lists.problem.map(i => i.content.trim()).filter(Boolean).join('\n')
  if (!keepText && !problemText) return modal.alert('Keep 또는 Problem을 먼저 작성해주세요.')
  aiLoading.value = true
  try {
    const res = await aiApi.suggestTry(keepText, problemText, route.params.date)
    updateAiQuota('TRY_RECOMMENDATION', res)
    if (!res?.suggestions?.length) throw new Error()
    // 새 추천을 맨 위에 쌓음 (이전 추천은 히스토리로 남음)
    const batch = res.batch || { batchId: `local-${Date.now()}`, createdAt: new Date().toISOString(), suggestions: res.suggestions }
    aiHistory.value = [batch, ...aiHistory.value]
  } catch (e) {
    markQuotaExhausted('TRY_RECOMMENDATION', e)
    modal.error(e.response?.data?.message || 'AI 서비스 오류가 발생했습니다.', { title: 'AI 추천 실패' })
  }
  finally { aiLoading.value = false }
}

// 추천 하나를 Try 항목으로 추가 (히스토리엔 "추가됨"으로 남음)
async function addAiSuggestion(text) {
  if (isAdded(text)) return
  if (lists.tryItems.length >= 20) return modal.alert('최대 20개까지 작성 가능합니다.')
  try {
    const data = await trackSave(retrospectApi.addItem(route.params.date, 'TRY', text))
    applyResponse(data, { mutated: true })
  } catch { /* handled */ }
}

async function addAllAiSuggestions() {
  for (const text of allSuggestions.value.filter(t => !isAdded(t))) {
    if (lists.tryItems.length >= 20) return modal.alert('최대 20개까지 작성 가능합니다.')
    await addAiSuggestion(text)
  }
}

async function pushToGithub() {
  if (!retrospectId.value) return
  githubLoading.value = true
  try {
    await githubApi.push(retrospectId.value)
    isGithubSynced.value = true
    modal.success('GitHub에 업로드되었습니다.', { title: '업로드 완료' })
  } catch (e) {
    modal.error(e.response?.data?.message || 'GitHub 업로드에 실패했습니다.', { title: '업로드 실패' })
  } finally {
    githubLoading.value = false
  }
}

function openGuide(field) {
  guideKey.value = field
}

// ── 오늘의 점 미리보기 ──
// 섹션마다 내용 있는 항목이 하나라도 있으면 채운 것으로 봄
const sectionDone = computed(() => Object.fromEntries(
  TYPES.map(t => [t.key, lists[t.key].some(i => i.content.trim())])))
const dotFill = computed(() => Object.values(sectionDone.value).filter(Boolean).length / TYPES.length)
// 만족도가 높을수록 크고 진한 점 (캘린더의 잉크 점과 같은 규칙을 크게)
const dotDiameter = computed(() => 40 + score.value * 8)
// 채운 섹션만 K→P→T 순서로 아래부터 색 층이 쌓임
// 먼저 채운 섹션이 점의 맨 아래 층이 되도록, 채운 순서를 기억함
const fillOrder = ref([])
const doneKeys = computed(() => fillOrder.value.filter(k => sectionDone.value[k]))
watch(sectionDone, now => {
  const order = fillOrder.value.filter(k => now[k]) // 비워진 섹션은 빠짐
  for (const t of TYPES) if (now[t.key] && !order.includes(t.key)) order.push(t.key) // 새로 채운 섹션은 맨 위로
  fillOrder.value = order
})
// 불러온 회고는 섹션마다 가장 먼저 쓴 항목(id가 작을수록 먼저)의 순서로 층을 쌓음
function initFillOrder() {
  const firstId = k => Math.min(...lists[k].filter(i => i.content.trim() && i.id != null).map(i => i.id))
  fillOrder.value = TYPES.map(t => t.key).filter(k => sectionDone.value[k]).sort((a, b) => firstId(a) - firstId(b))
}


// 떠 있는 배지 높이: 평소엔 하단 탭 바로 위(76px),
// 맨 아래 저장·GitHub 버튼 줄이 화면에 올라오면 그 줄 바로 위로 비켜 뜸
const FLOAT_BASE_BOTTOM = 76
const actionRowEl = ref(null)
const floatBottom = ref(FLOAT_BASE_BOTTOM)
let floatRaf = 0
function updateFloatBottom() {
  floatRaf = 0
  const el = actionRowEl.value
  if (!el) { floatBottom.value = FLOAT_BASE_BOTTOM; return }
  const rowTop = el.getBoundingClientRect().top
  floatBottom.value = Math.max(FLOAT_BASE_BOTTOM, window.innerHeight - rowTop + 12)
}
function scheduleFloatUpdate() {
  if (!floatRaf) floatRaf = requestAnimationFrame(updateFloatBottom)
}
onMounted(() => {
  window.addEventListener('scroll', scheduleFloatUpdate, { passive: true })
  window.addEventListener('resize', scheduleFloatUpdate)
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', scheduleFloatUpdate)
  window.removeEventListener('resize', scheduleFloatUpdate)
  cancelAnimationFrame(floatRaf)
})

// 떠 있는 배지 속 점: 큰 점과 같은 규칙, 크기만 작게
const floatDotDiameter = computed(() => 20 + score.value * 3)
function scrollToTodayCard() {
  todayCardEl.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}
const showDotUi = computed(() => !isFuture.value && !pageLoading.value && !(isReadOnly.value && !retrospectId.value))

// 큰 점 카드가 화면에 보이는지 — 스크롤해서 안 보이면 헤더에 미니 점을 띄움
const todayCardEl = ref(null)
const todayCardVisible = ref(true)
let todayCardObserver = null
watch(todayCardEl, (el, prev) => {
  if (prev) todayCardObserver?.unobserve(prev)
  if (!el) { todayCardVisible.value = true; return }
  todayCardObserver ??= new IntersectionObserver(([entry]) => {
    todayCardVisible.value = entry.isIntersecting
  }, { rootMargin: '-120px 0px 0px 0px' }) // 상단 고정 헤더(앱 헤더 + 날짜 헤더)에 가려진 부분은 안 보이는 것으로
  todayCardObserver.observe(el)
})
onBeforeUnmount(() => todayCardObserver?.disconnect())
// 배지가 나타나는 순간 위치를 바로 맞춤
watch(todayCardVisible, visible => { if (!visible) nextTick(updateFloatBottom) })

// 섹션을 하나 채울 때마다 점이 "톡" 반응
const dotPulse = ref(false)
let pulseTimer = null
let suppressPulse = false // 회고를 불러오는 순간엔 반응하지 않음

// 방금 셋 다 채웠을 때만: 색이 섞여 잉크 점으로 완성되는 연출
const dotCelebrate = ref(false)
let celebrateTimer = null
watch(dotFill, (now, before) => {
  if (now === 1 && before < 1 && !pageLoading.value && !suppressPulse) {
    dotCelebrate.value = true
    clearTimeout(celebrateTimer)
    celebrateTimer = setTimeout(() => { dotCelebrate.value = false }, 4200) // 섞임 3.2초 + 톡·방울
  } else if (now < 1) {
    dotCelebrate.value = false
  }
})

watch(dotFill, (now, before) => {
  if (now <= before || now === 1 || pageLoading.value || suppressPulse) return
  dotPulse.value = false
  clearTimeout(pulseTimer)
  requestAnimationFrame(() => {
    dotPulse.value = true
    pulseTimer = setTimeout(() => { dotPulse.value = false }, 500)
  })
})

// 점 이름: 오늘의 점 / 어제의 점 / 10월 6일의 점
const dotName = computed(() => {
  const date = route.params.date
  if (date === toIso(today)) return '오늘의 점'
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date === toIso(yesterday)) return '어제의 점'
  const [, m, d] = date.split('-').map(Number)
  return `${m}월 ${d}일의 점`
})

const dotCaption = computed(() => {
  if (isReadOnly.value) return `${dotName.value}이에요.`
  if (dotFill.value === 1) return `${dotName.value}이 완성됐어요.`
  if (dotFill.value === 0) return 'K·P·T를 하나씩 채울 때마다 점에 잉크가 차올라요.'
  const left = TYPES.filter(t => !sectionDone.value[t.key]).map(t => t.badge).join('·')
  return `${left}만 채우면 ${dotName.value}이 완성돼요.`
})

// 항목 입력칸: 내용이 길어지면 줄이 늘어나서 항상 전부 보이게 (Enter는 줄바꿈 대신 입력 끝내기)
function autoGrow(el) {
  el.style.height = 'auto'
  el.style.height = `${el.scrollHeight}px`
}
// 한글 입력 중(조합 중) Enter는 글자 확정용이라 건드리지 않음
function onItemEnter(e) {
  if (e.isComposing || e.keyCode === 229) return
  e.preventDefault()
  e.target.blur()
}
const vAutogrow = {
  mounted(el) {
    autoGrow(el)
    // 화면 폭이 바뀌면 줄바꿈 위치도 바뀌므로 다시 맞춤 (높이 변화엔 반응하지 않게 폭만 비교)
    let lastWidth = el.clientWidth
    el._autogrowObserver = new ResizeObserver(() => {
      if (el.clientWidth !== lastWidth) { lastWidth = el.clientWidth; autoGrow(el) }
    })
    el._autogrowObserver.observe(el)
  },
  updated: autoGrow, // 다듬기 적용 등으로 내용이 코드에서 바뀔 때
  unmounted(el) { el._autogrowObserver?.disconnect() },
}

// ── 다듬기 ──
const refine = reactive({ uid: null, loading: false, original: '', refined: '' })
const refineLeft = computed(() => aiQuota.REFINE?.remaining ?? null)
const API_TYPE = { keep: 'KEEP', problem: 'PROBLEM', tryItems: 'TRY' }

async function startRefine(typeKey, item) {
  const original = item.content.trim()
  if (!original) return
  Object.assign(refine, { uid: item._uid, loading: true, original, refined: '' })
  // 같은 날 쓴 다른 K/P를 참고로 넘겨서, AI가 원인을 지어내지 않고 기록 안에서 찾게 함
  const others = typeKey === 'keep' ? ['problem'] : typeKey === 'problem' ? ['keep'] : ['keep', 'problem']
  const context = [...others, typeKey]
    .flatMap(k => lists[k].filter(i => i !== item).map(i => `${TYPES.find(t => t.key === k).badge}: ${i.content.trim()}`))
    .filter(line => !line.endsWith(': '))
    .join('\n')
  try {
    const res = await aiApi.refine(API_TYPE[typeKey], original, context)
    updateAiQuota('REFINE', res)
    if (refine.uid !== item._uid) return // 그 사이 다른 항목을 눌렀으면 무시
    refine.refined = res.refined
    refine.loading = false
  } catch (e) {
    markQuotaExhausted('REFINE', e)
    closeRefine()
    modal.error(e.response?.data?.message || '문장을 다듬지 못했습니다.', { title: '다듬기 실패' })
  }
}

function closeRefine() {
  Object.assign(refine, { uid: null, loading: false, original: '', refined: '' })
}

// 다듬은 문장으로 바꾸고 바로 저장 (입력하다 멈춘 것과 같은 흐름)
function applyRefine(typeKey, item) {
  const text = refine.refined
  closeRefine()
  if (!text) return
  item.content = text
  clearTimeout(itemTimers[item._uid])
  commitItem(typeKey, item)
}

const tryQuotaLeft = computed(() => aiQuota.TRY_RECOMMENDATION?.remaining ?? null)

onMounted(() => { loadRetro(); loadAiHistory(); refreshAiQuota() })
watch(() => route.params.date, () => { loadRetro(); loadAiHistory() })
</script>

<style scoped>
.retro-page { min-height: 100%; color: var(--ink); }

.retro-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: var(--paper);
  border-bottom: 1px solid var(--rule);
  position: sticky;
  top: 56px;
  z-index: 10;
}

.btn-back { background: none; color: var(--ink); font-weight: 600; font-size: 0.9rem; border: none; cursor: pointer; }
.retro-date {
  font-family: var(--font-serif);
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--ink);
}

/* 떠 있는 오늘의 점 배지 — 본문(680px) 오른쪽 끝, 하단 탭 바로 위 */
.float-dot {
  position: fixed;
  right: max(16px, calc((100vw - 680px) / 2 + 16px));
  bottom: 76px; /* 실제 값은 floatBottom(스크립트)으로 덮어씀 */
  z-index: 40;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px 8px 10px;
  background: var(--sheet);
  border: 1px solid var(--rule);
  border-radius: 999px;
  box-shadow: 0 6px 20px rgba(31, 42, 68, 0.14);
  cursor: pointer;
  color: var(--ink);
}
.float-dot.complete { border-color: var(--ink); }
.float-dot-text { display: flex; flex-direction: column; align-items: flex-start; gap: 3px; }
.float-dot-title { font-family: var(--font-serif); font-size: 0.82rem; font-weight: 700; line-height: 1; }
.float-dot-pips { display: flex; gap: 3px; }
.float-pip {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px dashed var(--ink-faint);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ink-faint);
  transition: background 0.25s, color 0.25s;
}
.float-pip.done { border: 1px solid transparent; color: var(--ink); }
.float-pip.pip-keep.done { background: var(--hl-keep); }
.float-pip.pip-problem.done { background: var(--hl-problem); }
.float-pip.pip-tryItems.done { background: var(--hl-try); }

.float-dot-enter-active, .float-dot-leave-active { transition: opacity 0.2s, transform 0.25s cubic-bezier(0.3, 1.4, 0.5, 1); }
.float-dot-enter-from, .float-dot-leave-to { opacity: 0; transform: translateY(12px) scale(0.9); }
.btn-delete { background: none; color: var(--red-pen); font-size: 0.8rem; padding: 5px 10px; border: 1px solid #f1d3cb; border-radius: 6px; cursor: pointer; }

.blocked-msg {
  max-width: 680px;
  margin: 60px auto;
  text-align: center;
  color: var(--ink-faint);
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
  background: var(--sheet);
  border: 1px solid var(--rule);
  border-radius: 6px;
  padding: 20px;
}

.section-label {
  font-family: var(--font-serif);
  font-size: 1rem;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 14px;
}

/* 오늘의 점 */
.today-card { display: flex; align-items: center; gap: 20px; }
.today-dot-wrap {
  width: 88px;
  height: 88px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.today-main { flex: 1; min-width: 0; }
.today-main .section-label { margin-bottom: 4px; }
.today-caption { font-size: 0.85rem; color: var(--ink-soft); line-height: 1.5; word-break: keep-all; }
.kpt-pips { display: flex; gap: 6px; margin-top: 10px; }
.kpt-pip {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px dashed var(--ink-faint);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ink-faint);
  transition: background 0.25s, border-color 0.25s, color 0.25s;
}
.kpt-pip.done { border: 1px solid transparent; color: var(--ink); }
.kpt-pip.pip-keep.done { background: var(--hl-keep); }
.kpt-pip.pip-problem.done { background: var(--hl-problem); }
.kpt-pip.pip-tryItems.done { background: var(--hl-try); }

@media (prefers-reduced-motion: reduce) {
  .float-dot-enter-active, .float-dot-leave-active { transition: opacity 0.2s; }
}

.score-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

/* 만족도: 점이 클수록·진할수록 높은 점수. 고른 점수엔 동그라미 */
.score-btn {
  width: 52px;
  height: 58px;
  border-radius: 6px;
  border: 1.5px solid transparent;
  background: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.score-btn .ink-dot { display: inline-block; border-radius: 50%; background: var(--ink); }
.score-num { font-size: 0.75rem; font-weight: 600; color: var(--ink-faint); line-height: 1; }
.score-btn:hover:not(:disabled) { background: var(--paper-deep); }
.score-btn.active { border-color: var(--ink); }
.score-btn.active .score-num { color: var(--ink); font-weight: 700; }
.score-btn:disabled { cursor: not-allowed; }
.score-btn:disabled:not(.active) { opacity: 0.5; }
.score-emoji { font-size: 1.5rem; margin-left: 4px; }

.kpt-card { border: 1.5px solid transparent; }
.keep-card { border-color: var(--hl-keep); }
.problem-card { border-color: var(--hl-problem); }
.tryItems-card { border-color: var(--hl-try); }

.kpt-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.kpt-tag {
  width: 26px;
  height: 26px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--ink);
  flex-shrink: 0;
}
.keep-tag { background: var(--hl-keep); }
.problem-tag { background: var(--hl-problem); }
.tryItems-tag { background: var(--hl-try); }

.kpt-title { font-family: var(--font-serif); font-size: 0.95rem; font-weight: 700; color: var(--ink); flex: 1; }

.btn-guide {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--paper-deep);
  border: none;
  color: var(--ink-faint);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  flex-shrink: 0;
}

.item-list { display: flex; flex-direction: column; gap: 8px; }

.kpt-item {
  display: flex;
  align-items: flex-start; /* 여러 줄이 돼도 번호·버튼은 첫 줄에 */
  gap: 8px;
}

.item-num {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ink-faint);
  width: 28px;
  flex-shrink: 0;
  padding-top: 11px;
}

.item-input, .item-text {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--rule);
  border-radius: 6px;
  padding: 9px 10px;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--ink);
  font-family: inherit;
  word-break: keep-all;
  overflow-wrap: anywhere;
}
textarea.item-input {
  display: block;
  resize: none;
  overflow: hidden;
  min-height: 0;
}
.item-input:focus { outline: none; border-color: var(--ink); }
.item-input.saving { opacity: 0.6; }
/* 읽기 전용(14일 지난 회고): 흰 바탕에 얇은 회색 테두리 */
.item-text { background: var(--sheet); border-color: var(--rule); }

/* ✎ 아이콘은 좌우반전해서 씀 */
.pencil-icon { display: inline-block; transform: scaleX(-1); }
.refine-count { font-size: 0.72rem; font-weight: 600; color: var(--ink-faint); white-space: nowrap; }
.refine-count.empty { color: var(--red-pen); }

.kpt-item-wrap { display: flex; flex-direction: column; gap: 6px; }

.item-refine {
  background: none;
  border: none;
  color: var(--ink-faint);
  font-size: 0.95rem;
  cursor: pointer;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  transform: scaleX(-1); /* ✎ 좌우반전 */
  transition: color 0.15s, background 0.15s;
  margin-top: 8px; /* 입력칸 첫 줄 높이에 맞춤 */
}
.item-refine:hover:not(:disabled), .item-refine.active { color: var(--ink); background: var(--paper-deep); }
.item-refine:disabled { opacity: 0.35; cursor: not-allowed; }

/* 다듬기 비교 박스 — 항목 종류별 형광펜 색 */
.refine-box { --hl: var(--hl-keep); margin-left: 36px; border: 1px solid var(--hl); border-radius: 6px; padding: 10px 12px; }
.refine-box.refine-problem { --hl: var(--hl-problem); }
.refine-box.refine-tryItems { --hl: var(--hl-try); }
.refine-loading { display: flex; justify-content: center; padding: 4px 0; }
.refine-row { display: flex; gap: 10px; font-size: 0.85rem; line-height: 1.55; }
.refine-row + .refine-row { margin-top: 6px; }
.refine-label { flex-shrink: 0; width: 64px; font-size: 0.72rem; font-weight: 700; color: var(--ink-faint); padding-top: 2px; }
.refine-before { color: var(--ink-faint); }
.refine-after { color: var(--ink); font-weight: 600; word-break: keep-all; }
.refine-hl { background: linear-gradient(transparent 58%, var(--hl) 58%); }
.refine-actions { display: flex; align-items: center; justify-content: flex-end; gap: 6px; margin-top: 10px; }
.refine-quota { margin-right: auto; font-size: 0.72rem; color: var(--ink-faint); }
.btn-refine-keep, .btn-refine-apply {
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}
.btn-refine-keep { background: var(--sheet); color: var(--ink-soft); border: 1px solid var(--rule); }
.btn-refine-apply { background: var(--ink); color: #fff; border: none; }

.item-del {
  background: none;
  border: none;
  color: var(--ink-faint);
  font-size: 1.1rem;
  cursor: pointer;
  flex-shrink: 0;
  width: 22px;
  height: 24px;
  margin-top: 8px; /* 입력칸 첫 줄 높이에 맞춤 */
}
.item-del:hover { color: var(--red-pen); }

.btn-add-item {
  margin-top: 10px;
  background: none;
  border: 1px dashed var(--ink-faint);
  border-radius: 6px;
  padding: 8px;
  width: 100%;
  color: var(--ink-soft);
  font-size: 0.825rem;
  cursor: pointer;
}
.btn-add-item:hover { border-color: var(--ink); color: var(--ink); }
.btn-add-item:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-ai {
  margin-top: 12px;
  width: 100%;
  padding: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: var(--hl-try);
  color: var(--ink);
  border: none;
  border-radius: 6px;
  font-weight: 700;
  font-size: 0.875rem;
  cursor: pointer;
  transition: filter 0.15s;
}
.btn-ai:hover:not(:disabled) { filter: brightness(0.96); }
/* 남은 횟수 표시 */
.quota-chip {
  margin-left: 4px;
  padding: 1px 7px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ink-soft);
}
.quota-note { margin-top: 6px; text-align: center; font-size: 0.78rem; color: var(--ink-faint); }
.btn-ai:disabled { opacity: 0.6; cursor: not-allowed; }

.ai-box {
  margin-top: 12px;
  background: #f3f8ff;
  border: 1px solid var(--hl-try);
  border-radius: 6px;
  padding: 14px;
}
.ai-box-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.ai-box-label { font-size: 0.75rem; font-weight: 700; color: var(--ink); }
.ai-suggestion.added { background: transparent; }
.ai-suggestion.added .ai-suggestion-text { color: var(--ink-faint); }
.ai-added { flex-shrink: 0; font-size: 0.72rem; font-weight: 700; color: var(--ink-faint); }
.ai-suggestion {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  background: var(--sheet);
  border: 1px solid var(--hl-try);
  border-radius: 6px;
}
.ai-suggestion + .ai-suggestion { margin-top: 6px; }
.ai-suggestion-text { flex: 1; font-size: 0.875rem; line-height: 1.5; color: var(--ink); }
.btn-apply, .btn-apply-all {
  flex-shrink: 0;
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}
.btn-apply { background: var(--ink); color: #fff; border: none; }
.btn-apply-all { background: none; color: var(--ink); border: 1px solid var(--hl-try); }
.btn-apply:disabled, .btn-apply-all:disabled { opacity: 0.6; cursor: not-allowed; }

.action-row { display: flex; align-items: center; gap: 10px; }

.btn-save {
  padding: 15px 22px;
  background: var(--ink);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
}

.save-status {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  font-weight: 600;
}
.status-saved { color: var(--ink-faint); }
.status-unsaved { color: var(--warn-ink); }
.status-saving { color: var(--ink); }

.btn-github {
  padding: 15px 18px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--sheet);
  color: var(--ink);
  border: 1px solid var(--ink);
  border-radius: 6px;
  font-weight: 700;
  font-size: 0.875rem;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}
.btn-github:hover:not(:disabled) { background: var(--paper-deep); }
.btn-github:disabled { opacity: 0.6; cursor: not-allowed; }

.github-setup-msg {
  text-align: right;
  font-size: 0.8rem;
  color: var(--ink-faint);
}
.github-setup-msg button {
  margin-left: 8px;
  background: none;
  border: none;
  color: var(--ink);
  font-weight: 700;
  text-decoration: underline;
  cursor: pointer;
}

/* 모달 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(31, 42, 68, 0.35);
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
  background: var(--sheet);
  border: 1px solid var(--rule);
  border-radius: 8px;
  width: 100%;
  max-width: 520px;
  max-height: 80vh;
  overflow-y: auto;
}

/* 작성 가이드 — 노트 한 장 느낌. 타입별 형광펜 색은 --hl 하나로 */
.guide-modal { --hl: var(--hl-keep); color: var(--ink); }
.guide-modal.guide-problem { --hl: var(--hl-problem); }
.guide-modal.guide-tryItems { --hl: var(--hl-try); }

.guide-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 22px 14px;
  border-bottom: 1px solid var(--rule);
}
.guide-badge {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: var(--hl);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 1rem;
  flex-shrink: 0;
}
.guide-title { flex: 1; font-family: var(--font-serif); font-size: 1.2rem; font-weight: 700; }
.guide-title span { font-family: inherit; font-weight: 400; color: var(--ink-soft); font-size: 1rem; }
.guide-close { background: none; border: none; font-size: 1.05rem; color: var(--ink-faint); cursor: pointer; padding: 4px; }

.guide-body { padding: 18px 22px 22px; display: flex; flex-direction: column; gap: 20px; }
.guide-summary { font-size: 0.95rem; font-weight: 600; line-height: 1.6; word-break: keep-all; }

.guide-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ink-faint);
  letter-spacing: 0.03em;
  margin-bottom: 8px;
}

.guide-questions { list-style: none; display: flex; flex-direction: column; gap: 6px; }
.guide-questions li {
  position: relative;
  padding-left: 16px;
  font-size: 0.9rem;
  color: var(--ink);
  line-height: 1.5;
}
.guide-questions li::before {
  content: '';
  position: absolute;
  left: 2px;
  top: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ink);
}

.guide-formula {
  display: inline-block;
  margin-bottom: 10px;
  padding: 3px 8px;
  border-radius: 4px;
  background: var(--hl);
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ink);
}

.guide-example {
  border: 1px solid var(--rule);
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 0.875rem;
  line-height: 1.5;
}
.guide-example + .guide-example { margin-top: 8px; }
.ex-bad { color: var(--ink-faint); text-decoration: line-through; text-decoration-color: rgba(31, 42, 68, 0.3); }
.ex-good { color: var(--ink); font-weight: 600; word-break: keep-all; }
.ex-mark { display: inline-block; width: 18px; text-decoration: none; font-weight: 700; }
.ex-bad .ex-mark { color: var(--ink-faint); }
.ex-good .ex-mark { color: var(--ink); }
.ex-hl { background: linear-gradient(transparent 58%, var(--hl) 58%); }

.guide-tip {
  background: var(--paper-deep);
  border-radius: 6px;
  padding: 12px 14px;
  font-size: 0.85rem;
  color: var(--ink-soft);
  line-height: 1.6;
  word-break: keep-all;
}
</style>
