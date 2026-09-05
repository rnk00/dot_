<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-head">
        <h3>{{ label }} 메모 불러오기</h3>
        <button @click="$emit('close')">✕</button>
      </div>
      <div class="modal-body">
        <p v-if="list.length === 0" class="empty-msg">저장된 메모가 없어요. 캘린더 화면의 KPT 메모에서 먼저 추가해보세요.</p>
        <p v-else class="remaining-msg">남은 자리 {{ remaining }}개까지 선택할 수 있어요.</p>
        <label
          v-for="m in list" :key="m.id"
          class="memo-row"
          :class="{ disabled: isDisabled(m.id) }"
        >
          <input type="checkbox" v-model="selected" :value="m.id" :disabled="isDisabled(m.id)" />
          <span>{{ m.content }}</span>
        </label>
      </div>
      <div class="modal-foot">
        <button class="btn-confirm" :disabled="selected.length === 0" @click="confirm">
          {{ selected.length > 0 ? `${selected.length}개 추가` : '추가할 메모 선택' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useKptMemoStore } from '@/stores/kptMemo'

const props = defineProps({ type: String, remaining: { type: Number, default: 20 } })
const emit = defineEmits(['close', 'confirm'])

const memoStore = useKptMemoStore()
const selected = ref([])

const label = computed(() => ({ KEEP: 'Keep', PROBLEM: 'Problem', TRY: 'Try' }[props.type]))
const list = computed(() => memoStore.memos[props.type])

onMounted(() => memoStore.loadAll())

function isDisabled(id) {
  return !selected.value.includes(id) && selected.value.length >= props.remaining
}

function confirm() {
  emit('confirm', selected.value)
}
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
  z-index: 200; padding: 16px;
}
.modal {
  background: #fff; border-radius: 16px; width: 100%; max-width: 420px;
  max-height: 70vh; display: flex; flex-direction: column;
}
.modal-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 18px; border-bottom: 1px solid #f1f5f9;
}
.modal-head h3 { font-size: 1rem; font-weight: 700; color: #1e293b; }
.modal-head button { background: none; border: none; font-size: 1.1rem; color: #94a3b8; cursor: pointer; }
.modal-body { padding: 12px 18px; overflow-y: auto; flex: 1; }
.remaining-msg { font-size: 0.75rem; color: #6366f1; margin-bottom: 8px; }
.memo-row { display: flex; align-items: flex-start; gap: 8px; padding: 8px 0; font-size: 0.875rem; color: #374151; cursor: pointer; line-height: 1.5; }
.memo-row.disabled { opacity: 0.4; cursor: not-allowed; }
.memo-row input { margin-top: 3px; }
.empty-msg { font-size: 0.85rem; color: #94a3b8; padding: 12px 0; line-height: 1.6; }
.modal-foot { padding: 14px 18px; border-top: 1px solid #f1f5f9; }
.btn-confirm { width: 100%; padding: 12px; background: #6366f1; color: #fff; border: none; border-radius: 10px; font-weight: 700; cursor: pointer; }
.btn-confirm:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
