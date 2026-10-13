<template>
  <!-- 오늘의 점: 채운 K·P·T가 형광펜 색 층으로 쌓이고, 셋 다 채우면 세 색이 빙글 돌며 섞인 점으로 완성됨 -->
  <span class="td-wrap" :style="{ width: `${diameter}px`, height: `${diameter}px` }" aria-hidden="true">
    <span class="td" :class="{ complete, celebrate, pulse }">
      <!-- 채운 섹션만, 먼저 채운 것이 아래부터 -->
      <span class="td-bands" :style="{ height: `${(done.length / 3) * 100}%` }">
        <span v-for="k in done" :key="k" class="td-band" :class="`band-${k}`" />
      </span>
      <!-- 완성: 세 색이 소용돌이처럼 섞인 점 -->
      <span class="td-swirl" :style="{ background: swirlGradient }" />
    </span>

    <!-- 완성 순간: 세 색 잉크 방울이 사방으로 튐 -->
    <template v-if="celebrate">
      <span
        v-for="(color, i) in dropColors"
        :key="i"
        class="td-drop"
        :style="{ background: color, '--a': `${i * 60 + 15}deg`, '--d': `${diameter / 2 + 12 + (i % 2) * 6}px` }"
      />
    </template>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  diameter: { type: Number, required: true },
  done: { type: Array, default: () => [] },     // 채운 섹션 키, 먼저 채운 순서대로 (예: ['problem', 'keep'])
  complete: { type: Boolean, default: false },
  celebrate: { type: Boolean, default: false }, // 방금 완성됐을 때만 true → 빙글 섞이는 연출
  pulse: { type: Boolean, default: false },     // 섹션 하나 채울 때 톡
})

const HL = { keep: 'var(--hl-keep)', problem: 'var(--hl-problem)', tryItems: 'var(--hl-try)' }
const order = computed(() => {
  const [a = 'keep', b = 'problem', c = 'tryItems'] = props.done
  return [a, b, c]
})
// 섞인 점: 채운 순서대로 세 색이 한 바퀴 이어지는 소용돌이
const swirlGradient = computed(() => {
  const [a, b, c] = order.value.map(k => HL[k])
  return `conic-gradient(from 200deg, ${a}, ${b}, ${c}, ${a})`
})
// 방울 6개: 세 색이 번갈아
const dropColors = computed(() => [...order.value, ...order.value].map(k => HL[k]))
</script>

<style scoped>
.td-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: width 0.35s ease, height 0.35s ease;
}

.td {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 1.5px dashed var(--ink-faint);
  overflow: hidden;
  transition: border-color 0.4s;
}
.td.complete { border-style: solid; border-color: var(--ink); }
.td.pulse { animation: td-pulse 0.45s cubic-bezier(0.3, 1.6, 0.5, 1); }
/* 섞이는 동안 가만히 있다가, 다 섞이면 한 번 튀고 자리 잡음 */
.td.celebrate { animation: td-pop 0.6s cubic-bezier(0.3, 1.6, 0.5, 1) 3.1s both; }

/* 색 층 */
.td-bands {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column-reverse; /* done 배열의 앞(먼저 채운 것)이 맨 아래 */
  transition: height 0.5s cubic-bezier(0.3, 1.2, 0.5, 1);
}
.td-band { flex: 1; }
.band-keep { background: var(--hl-keep); }
.band-problem { background: var(--hl-problem); }
.band-tryItems { background: var(--hl-try); }

/* 섞인 점 (완성 후 그대로 남음) */
.td-swirl {
  position: absolute;
  inset: -20%;
  filter: blur(3px);
  opacity: 0;
  transition: opacity 0.4s;
}
.td.complete .td-swirl { opacity: 1; }
/* 방금 완성됐으면: 층 위로 소용돌이가 번지며 빙글 돌다가 멈춤 */
.td.celebrate .td-swirl { animation: td-swirl 3.2s cubic-bezier(0.45, 0, 0.25, 1) both; } /* 아주 천천히 섞임 */

/* 잉크 방울 */
.td-drop {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 6px;
  height: 6px;
  margin: -3px 0 0 -3px;
  border-radius: 50%;
  opacity: 0;
  animation: td-drop 0.75s cubic-bezier(0.2, 0.8, 0.3, 1) 3.15s;
  pointer-events: none;
}

@keyframes td-pulse {
  0% { transform: scale(1); }
  45% { transform: scale(1.15); }
  100% { transform: scale(1); }
}
@keyframes td-swirl {
  0% { opacity: 0; transform: rotate(0deg) scale(0.6); }
  25% { opacity: 1; }
  100% { opacity: 1; transform: rotate(360deg) scale(1); }
}
@keyframes td-pop {
  0% { transform: scale(1); }
  40% { transform: scale(1.18); }
  100% { transform: scale(1); }
}
@keyframes td-drop {
  0% { transform: rotate(var(--a)) translateY(0) scale(0.6); opacity: 0; }
  20% { opacity: 1; }
  100% { transform: rotate(var(--a)) translateY(calc(var(--d) * -1)) scale(1); opacity: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .td, .td-bands, .td-swirl { transition: none; animation: none !important; }
  .td-drop { display: none; }
}
</style>
