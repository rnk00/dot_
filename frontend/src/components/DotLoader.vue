<template>
  <!-- 로고(•–•–●)처럼 점 세 개가 하나씩 찍히고 선으로 이어지는 로딩 -->
  <div class="dot-loader" role="status" :aria-label="label || '불러오는 중'">
    <svg :width="size * 3" :height="size" viewBox="0 0 30 10" aria-hidden="true">
      <line class="dl-line" x1="5" y1="5" x2="25" y2="5" pathLength="1" />
      <circle class="dl-dot d1" cx="5" cy="5" r="2.6" />
      <circle class="dl-dot d2" cx="15" cy="5" r="3.2" />
      <circle class="dl-dot d3" cx="25" cy="5" r="3.8" />
    </svg>
    <span v-if="label" class="dl-label">{{ label }}</span>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  size: { type: Number, default: 14 },
})
</script>

<style scoped>
.dot-loader {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--ink);
}
svg { display: block; overflow: visible; }

.dl-line {
  stroke: currentColor;
  stroke-width: 1.4;
  stroke-opacity: 0.45;
  stroke-linecap: round;
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  animation: dl-draw 1.6s ease-in-out infinite;
}
.dl-dot {
  fill: currentColor;
  transform-box: fill-box;
  transform-origin: center;
  animation: dl-dot 1.6s ease-out infinite;
}
.d1 { fill-opacity: 0.35; animation-delay: 0s; }
.d2 { fill-opacity: 0.65; animation-delay: 0.25s; }
.d3 { animation-delay: 0.5s; }

/* 점: 톡 찍히고 → 잠깐 머물렀다 → 같이 사라짐 */
@keyframes dl-dot {
  0% { transform: scale(0); opacity: 0; }
  12% { transform: scale(1.25); opacity: 1; }
  20%, 70% { transform: scale(1); opacity: 1; }
  90%, 100% { transform: scale(1); opacity: 0; }
}
/* 선: 점이 찍히는 동안 왼쪽부터 그어짐 */
@keyframes dl-draw {
  0%, 10% { stroke-dashoffset: 1; opacity: 1; }
  45%, 70% { stroke-dashoffset: 0; opacity: 1; }
  90%, 100% { stroke-dashoffset: 0; opacity: 0; }
}

.dl-label { font-size: 0.8rem; color: var(--ink-faint); }

@media (prefers-reduced-motion: reduce) {
  .dl-line, .dl-dot { animation: none; stroke-dashoffset: 0; opacity: 1; transform: none; }
}
</style>
