import { defineStore } from 'pinia'
import { ref } from 'vue'
import { kptMemoApi } from '@/api/kptMemo'

const MAX_MEMOS = 20

export const useKptMemoStore = defineStore('kptMemo', () => {
  const memos = ref({ KEEP: [], PROBLEM: [], TRY: [] })
  const loaded = ref(false)

  async function loadAll(force = false) {
    if (loaded.value && !force) return
    const data = await kptMemoApi.list()
    memos.value = { KEEP: data.keep, PROBLEM: data.problem, TRY: data.try }
    loaded.value = true
  }

  async function create(type, content) {
    if (memos.value[type].length >= MAX_MEMOS) {
      alert('최대 20개까지 작성 가능합니다.')
      return
    }
    await kptMemoApi.create(type, content)
    await loadAll(true)
  }

  async function update(type, id, content) {
    if (!content || !content.trim()) {
      await remove(type, id)
      return
    }
    await kptMemoApi.update(id, content)
    const memo = memos.value[type].find(m => m.id === id)
    if (memo) memo.content = content
  }

  async function remove(type, id) {
    await kptMemoApi.delete(id)
    memos.value[type] = memos.value[type].filter(m => m.id !== id)
  }

  async function reorder(type, orderedList) {
    memos.value[type] = orderedList
    await kptMemoApi.reorder(type, orderedList.map(m => m.id))
  }

  return { memos, loadAll, create, update, remove, reorder }
})
