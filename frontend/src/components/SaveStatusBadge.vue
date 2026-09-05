<template>
  <span class="save-badge" :class="[status, { faded }]">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ status: String, faded: Boolean })

const label = computed(() => ({
  before: '저장 전',
  saving: '저장 중...',
  saved: '저장 완료'
}[props.status] || '저장 전'))
</script>

<style scoped>
.save-badge {
  display: inline-flex;
  align-items: center;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #64748b;
  transition: opacity 0.15s;
}
.save-badge.saved { background: #dcfce7; color: #16a34a; }
.save-badge.saving { background: #e0e7ff; color: #6366f1; }
.save-badge.before { background: #fef3c7; color: #b45309; }
.save-badge.faded { opacity: 0.5; }
</style>
