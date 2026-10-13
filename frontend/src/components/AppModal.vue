<template>
  <Transition name="app-modal">
    <div v-if="modalState.show" class="app-modal-overlay" @click.self="closeModal(false)">
      <div class="app-modal" role="dialog" aria-modal="true">
        <div class="app-modal-body">
          <div v-if="modalState.title" class="app-modal-title" :class="`is-${modalState.type}`">
            <Icon v-if="icon" :name="icon" :size="18" />
            {{ modalState.title }}
          </div>
          <p class="app-modal-text">{{ modalState.message }}</p>
        </div>
        <div class="app-modal-actions">
          <button
            v-if="modalState.mode === 'confirm'"
            class="app-modal-btn btn-cancel"
            @click="closeModal(false)"
          >{{ modalState.cancelText }}</button>
          <button
            ref="confirmBtn"
            class="app-modal-btn btn-confirm"
            :class="{ danger: modalState.type === 'danger' }"
            @click="closeModal(true)"
          >{{ modalState.confirmText }}</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed, nextTick, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { modalState, closeModal } from '@/composables/useModal'
import Icon from '@/components/Icon.vue'

const confirmBtn = ref(null)

const icon = computed(() => ({
  success: 'check-circle',
  error: 'alert-triangle',
  danger: 'alert-triangle',
}[modalState.type] || null))

// 열리면 확인 버튼에 포커스 → Enter로 바로 닫기 가능
watch(() => modalState.show, async show => {
  if (show) {
    await nextTick()
    confirmBtn.value?.focus()
  }
})

function onKeydown(e) {
  if (modalState.show && e.key === 'Escape') closeModal(false)
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.app-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
}
.app-modal {
  background: #fff;
  border-radius: 12px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}
.app-modal-body { padding: 20px 20px 18px; }
.app-modal-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 1.05rem;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 8px;
}
.app-modal-title.is-success { color: #059669; }
.app-modal-title.is-error,
.app-modal-title.is-danger { color: #ef4444; }
.app-modal-text {
  font-size: 0.9rem;
  line-height: 1.6;
  color: #475569;
  white-space: pre-line;
  word-break: keep-all;
}
.app-modal-actions {
  display: flex;
  gap: 8px;
  padding: 0 20px 20px;
}
.app-modal-btn {
  flex: 1;
  padding: 11px;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-cancel { background: #f1f5f9; color: #475569; }
.btn-cancel:hover { background: #e2e8f0; }
.btn-confirm { background: #6366f1; color: #fff; }
.btn-confirm:hover { background: #4f46e5; }
.btn-confirm.danger { background: #ef4444; }
.btn-confirm.danger:hover { background: #dc2626; }
.app-modal-btn:focus-visible { outline: 2px solid #a5b4fc; outline-offset: 2px; }

.app-modal-enter-active, .app-modal-leave-active { transition: opacity 0.15s; }
.app-modal-enter-active .app-modal, .app-modal-leave-active .app-modal { transition: transform 0.15s; }
.app-modal-enter-from, .app-modal-leave-to { opacity: 0; }
.app-modal-enter-from .app-modal, .app-modal-leave-to .app-modal { transform: scale(0.96); }
</style>
