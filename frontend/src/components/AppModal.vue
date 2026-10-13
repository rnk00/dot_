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
  background: rgba(31, 42, 68, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
}
.app-modal {
  background: var(--sheet);
  border: 1px solid var(--rule);
  border-radius: 8px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 12px 32px rgba(31, 42, 68, 0.12);
}
.app-modal-body { padding: 20px 20px 18px; }
.app-modal-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-serif);
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 8px;
}
.app-modal-title.is-success { color: var(--ok-ink); }
.app-modal-title.is-error,
.app-modal-title.is-danger { color: var(--red-pen); }
.app-modal-text {
  font-size: 0.9rem;
  line-height: 1.6;
  color: var(--ink-soft);
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
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-cancel { background: var(--sheet); color: var(--ink-soft); border: 1px solid var(--rule); }
.btn-cancel:hover { background: var(--paper-deep); }
.btn-confirm { background: var(--ink); color: #fff; }
.btn-confirm:hover { opacity: 0.9; }
.btn-confirm.danger { background: var(--red-pen); }
.btn-confirm.danger:hover { background: #b04532; }
.app-modal-btn:focus-visible { outline: 2px solid var(--hl-try); outline-offset: 2px; }

.app-modal-enter-active, .app-modal-leave-active { transition: opacity 0.15s; }
.app-modal-enter-active .app-modal, .app-modal-leave-active .app-modal { transition: transform 0.15s; }
.app-modal-enter-from, .app-modal-leave-to { opacity: 0; }
.app-modal-enter-from .app-modal, .app-modal-leave-to .app-modal { transform: scale(0.96); }
</style>
