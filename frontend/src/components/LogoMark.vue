<template>
  <!-- ●—●—● : 점점 진해지며 이어지는 세 점 (매일의 점이 이어진다는 뜻) -->
  <svg class="logo-mark" :width="size * 3" :height="size" :viewBox="`0 0 30 10`" aria-hidden="true">
    <line x1="5" y1="5" x2="25" y2="5" stroke="currentColor" stroke-width="1.4" stroke-opacity="0.5" />
    <circle cx="5" cy="5" r="2.6" fill="currentColor" fill-opacity="0.35" />
    <circle cx="15" cy="5" r="3.2" fill="currentColor" fill-opacity="0.65" />
    <circle cx="25" cy="5" r="3.8" fill="currentColor" />
  </svg>
</template>

<script setup>
defineProps({ size: { type: Number, default: 10 } })
</script>

<style scoped>
.logo-mark { display: block; flex-shrink: 0; }
</style>
