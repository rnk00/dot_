<template>
  <div class="memo-panel">
    <div class="memo-head">
      <h3>KPT 메모</h3>
      <p class="memo-sub">날짜와 무관하게 자유롭게 메모하세요</p>
    </div>

    <div class="memo-tabs">
      <button
        v-for="t in types" :key="t.value"
        class="tab" :class="{ active: activeType === t.value }"
        @click="activeType = t.value"
      >{{ t.short }} {{ memoStore.memos[t.value].length }}</button>
    </div>

    <div v-if="list.length === 0 && !adding" class="empty-msg">아직 작성한 {{ shortLabel }}가 없어요</div>

    <draggable v-model="list" item-key="id" handle=".drag-handle" animation="150" class="memo-list" @end="onReorder">
      <template #item="{ element }">
        <div v-if="editingId !== element.id" class="memo-block" @click="startEdit(element)">
          <span class="drag-handle" @click.stop>⠿</span>
          <span class="memo-text">{{ element.content }}</span>
          <button class="btn-x" @click.stop="onDelete(element)">✕</button>
        </div>
        <div v-else class="memo-block editing">
          <input
            :ref="setEditInputRef"
            v-model="editingContent"
            @keydown.enter="confirmEdit(element)"
            @blur="confirmEdit(element)"
            @keydown.esc="editingId = null"
          />
        </div>
      </template>
    </draggable>

    <div v-if="adding" class="memo-block editing">
      <input
        ref="addInputRef"
        v-model="newContent"
        :placeholder="placeholder"
        @keydown.enter="confirmAdd"
        @blur="confirmAdd"
        @keydown.esc="cancelAdd"
      />
    </div>
    <button v-else class="btn-add" :disabled="list.length >= 20" @click="startAdd">+</button>

    <p class="memo-count">{{ list.length }} / 20</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import draggable from 'vuedraggable'
import { useKptMemoStore } from '@/stores/kptMemo'

const memoStore = useKptMemoStore()
const types = [
  { value: 'KEEP', short: 'K' },
  { value: 'PROBLEM', short: 'P' },
  { value: 'TRY', short: 'T' }
]
const activeType = ref('KEEP')
const adding = ref(false)
const newContent = ref('')
const editingId = ref(null)
const editingContent = ref('')
const addInputRef = ref(null)

const placeholderMap = { KEEP: '유지하고 싶은 것', PROBLEM: '반복되는 문제', TRY: '하고 싶은 것' }
const shortLabelMap = { KEEP: 'Keep', PROBLEM: 'Problem', TRY: 'Try' }
const placeholder = computed(() => placeholderMap[activeType.value])
const shortLabel = computed(() => shortLabelMap[activeType.value])

const list = computed({
  get: () => memoStore.memos[activeType.value],
  set: (v) => { memoStore.memos[activeType.value] = v }
})

onMounted(() => memoStore.loadAll())

function startAdd() {
  adding.value = true
  newContent.value = ''
  nextTick(() => addInputRef.value?.focus())
}

function cancelAdd() {
  adding.value = false
  newContent.value = ''
}

async function confirmAdd() {
  if (!adding.value) return
  const content = newContent.value.trim()
  adding.value = false
  if (!content) return
  await memoStore.create(activeType.value, content)
}

function startEdit(memo) {
  editingId.value = memo.id
  editingContent.value = memo.content
}

function setEditInputRef(el) {
  if (el) nextTick(() => el.focus())
}

async function confirmEdit(memo) {
  if (editingId.value !== memo.id) return
  const content = editingContent.value.trim()
  editingId.value = null
  if (content === memo.content) return
  await memoStore.update(activeType.value, memo.id, content)
}

async function onDelete(memo) {
  await memoStore.remove(activeType.value, memo.id)
}

function onReorder() {
  memoStore.reorder(activeType.value, list.value)
}
</script>

<style scoped>
.memo-panel {
  background: #fff; border-radius: 16px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.memo-head h3 { font-size: 0.9rem; font-weight: 700; color: #1e293b; }
.memo-sub { font-size: 0.7rem; color: #94a3b8; margin-top: 2px; margin-bottom: 12px; }

.memo-tabs { display: flex; gap: 4px; margin-bottom: 12px; }
.tab {
  flex: 1; padding: 6px 4px; border: none; border-radius: 8px;
  background: #f1f5f9; color: #64748b; font-weight: 700; font-size: 0.75rem; cursor: pointer;
}
.tab.active { background: #6366f1; color: #fff; }

.memo-list { display: flex; flex-direction: column; gap: 6px; }
.memo-block {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 10px; background: #f8fafc; border-radius: 8px;
  cursor: pointer; font-size: 0.8rem; color: #374151;
}
.memo-block.editing { background: #fff; border: 1.5px solid #6366f1; padding: 4px 8px; }
.memo-block.editing input {
  width: 100%; border: none; outline: none; font-size: 0.8rem;
  font-family: inherit; padding: 4px 2px; color: #374151;
}
.drag-handle { cursor: grab; color: #cbd5e1; user-select: none; flex-shrink: 0; }
.memo-text { flex: 1; word-break: break-word; }
.btn-x {
  flex-shrink: 0; background: none; border: none; color: #cbd5e1;
  font-size: 0.7rem; cursor: pointer;
}
.btn-x:hover { color: #ef4444; }

.empty-msg { font-size: 0.8rem; color: #94a3b8; padding: 10px 0; }

.btn-add {
  margin-top: 8px; width: 100%; padding: 8px; border: 1.5px dashed #cbd5e1;
  border-radius: 8px; background: none; color: #94a3b8; font-weight: 700;
  cursor: pointer; transition: border-color 0.15s, color 0.15s;
}
.btn-add:hover:not(:disabled) { border-color: #6366f1; color: #6366f1; }
.btn-add:disabled { opacity: 0.4; cursor: not-allowed; }

.memo-count { font-size: 0.7rem; color: #94a3b8; margin-top: 8px; text-align: right; }
</style>
