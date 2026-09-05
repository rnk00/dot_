import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { retrospectApi } from '@/api/retrospect'

const TYPES = ['KEEP', 'PROBLEM', 'TRY']
const DEBOUNCE_MS = 1000
const MAX_ITEMS = 20

function emptyItems() {
  return { KEEP: [], PROBLEM: [], TRY: [] }
}

// 각 타입 목록 끝에 항상 입력용 draft row(id 없음)를 하나 유지
function ensureDraftRows(items) {
  TYPES.forEach(type => {
    const list = items[type]
    const hasDraft = list.length > 0 && list[list.length - 1].id == null
    if (!hasDraft && list.length < MAX_ITEMS) {
      list.push({ id: null, tempKey: `draft-${type}-${Date.now()}-${Math.random()}`, type, content: '' })
    }
  })
  return items
}

export const useRetrospectStore = defineStore('retrospect', () => {
  const date = ref(null)
  const retrospectId = ref(null)
  const exists = ref(false)
  const editable = ref(true)
  const score = ref(3)
  const githubSynced = ref(false)
  const items = ref(emptyItems())

  const pristine = ref(true)
  const dirtyCount = ref(0)
  const pendingCount = ref(0)

  const saveStatus = computed(() => {
    if (dirtyCount.value > 0) return 'before'
    if (pendingCount.value > 0) return 'saving'
    return 'saved'
  })
  const saveFaded = computed(() => pristine.value && saveStatus.value === 'saved')

  const debounceTimers = {}

  function applyResponse(data) {
    retrospectId.value = data.id
    exists.value = data.exists
    editable.value = data.editable
    score.value = data.score
    githubSynced.value = data.is_github_synced
    const next = { KEEP: data.keep || [], PROBLEM: data.problem || [], TRY: data.try || [] }
    items.value = ensureDraftRows(next)
  }

  async function load(d) {
    date.value = d
    pristine.value = true
    dirtyCount.value = 0
    pendingCount.value = 0
    Object.values(debounceTimers).forEach(t => clearTimeout(t))
    for (const key of Object.keys(debounceTimers)) delete debounceTimers[key]

    const data = await retrospectApi.getByDate(d)
    applyResponse(data)
  }

  async function runMutation(fn) {
    pendingCount.value++
    try {
      const data = await fn()
      applyResponse(data)
      return data
    } catch (e) {
      const msg = e.response?.data?.message || '저장에 실패했습니다.'
      alert(msg)
      throw e
    } finally {
      pendingCount.value--
    }
  }

  async function changeScore(newScore) {
    if (!editable.value) return
    pristine.value = false
    await runMutation(() => retrospectApi.updateScore(date.value, newScore)).catch(() => {})
  }

  function itemCount(type) {
    return items.value[type].filter(i => i.id != null).length
  }

  function scheduleItemSave(item, type) {
    if (!editable.value) return
    pristine.value = false
    const key = item.tempKey || `id-${item.id}`

    clearTimeout(debounceTimers[key])
    dirtyCount.value++

    debounceTimers[key] = setTimeout(async () => {
      dirtyCount.value--
      const isBlank = !item.content || !item.content.trim()

      try {
        if (item.id == null) {
          if (isBlank) return
          if (itemCount(type) >= MAX_ITEMS) {
            alert('최대 20개까지 작성 가능합니다.')
            item.content = ''
            return
          }
          await runMutation(() => retrospectApi.addItem(date.value, type, item.content))
        } else {
          if (isBlank) {
            await runMutation(() => retrospectApi.deleteItem(date.value, item.id))
          } else {
            await runMutation(() => retrospectApi.updateItem(date.value, item.id, item.content))
          }
        }
      } catch {
        // 실패 알림은 runMutation에서 처리, 상태는 저장 전으로 복귀
      }
    }, DEBOUNCE_MS)
  }

  async function deleteItem(item, type) {
    if (item.id == null) {
      items.value[type] = items.value[type].filter(i => i !== item)
      ensureDraftRows(items.value)
      return
    }
    await runMutation(() => retrospectApi.deleteItem(date.value, item.id))
  }

  async function reorderItems(type, orderedItems) {
    if (!editable.value || !retrospectId.value) return
    const orderedIds = orderedItems.filter(i => i.id != null).map(i => i.id)
    await runMutation(() => retrospectApi.reorderItems(date.value, type, orderedIds))
  }

  async function addItemsFromMemo(type, kptNoteIds) {
    if (!editable.value) return
    if (itemCount(type) + kptNoteIds.length > MAX_ITEMS) {
      alert('최대 20개까지 작성 가능합니다.')
      return
    }
    await runMutation(() => retrospectApi.addItemsFromMemo(date.value, type, kptNoteIds))
  }

  function markGithubSynced() {
    githubSynced.value = true
  }

  async function deleteRetrospect() {
    if (!exists.value) return
    await retrospectApi.delete(date.value)
  }

  return {
    date, retrospectId, exists, editable, score, githubSynced, items,
    saveStatus, saveFaded,
    load, changeScore, scheduleItemSave, deleteItem, reorderItems,
    addItemsFromMemo, deleteRetrospect, itemCount, markGithubSynced
  }
})
