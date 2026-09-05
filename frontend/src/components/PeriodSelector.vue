<template>
  <div class="period-selector">
    <button
      v-for="opt in options" :key="opt.value"
      class="opt" :class="{ active: modelValue === opt.value }"
      @click="$emit('update:modelValue', opt.value)"
    >{{ opt.label }}</button>
  </div>
</template>

<script setup>
defineProps({ modelValue: String })
defineEmits(['update:modelValue'])

const options = [
  { value: '7d', label: '7일' },
  { value: '30d', label: '30일' },
  { value: '1y', label: '1년' },
  { value: 'all', label: '전체' }
]
</script>

<style scoped>
.period-selector { display: flex; gap: 3px; background: #f1f5f9; border-radius: 8px; padding: 3px; flex-shrink: 0; }
.opt { padding: 5px 10px; border: none; background: none; border-radius: 6px; font-size: 0.75rem; font-weight: 600; color: #64748b; cursor: pointer; white-space: nowrap; }
.opt.active { background: #fff; color: #6366f1; box-shadow: 0 1px 2px rgba(0,0,0,0.08); }
</style>
