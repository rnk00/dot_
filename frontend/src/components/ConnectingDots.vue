<template>
  <!-- 배경 도트 그리드 위에서 몇 개의 점이 천천히 찍히고 선으로 이어지는 장식.
       본문(avoid 요소)과 겹치지 않는 빈 여백에만 그리고, 여백이 모자라면(모바일 등) 그리지 않음 -->
  <div ref="root" class="connecting-dots" aria-hidden="true">
    <svg v-if="size.w && constellations.length" :width="size.w" :height="size.h">
      <g v-for="(c, ci) in constellations" :key="c.key" :style="{ '--offset': `${ci * 6}s` }">
        <path class="cd-line" :d="c.path" pathLength="1" />
        <circle
          v-for="(p, pi) in c.points"
          :key="pi"
          class="cd-dot"
          :cx="p.x"
          :cy="p.y"
          r="3.5"
          :style="{ animationDelay: `calc(var(--offset) + ${pi * 0.8}s)` }"
        />
      </g>
    </svg>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  // 이 요소의 영역(+여유)과는 겹치지 않게 그림
  avoidSelector: { type: String, default: '' },
})

// 배경의 --dot-grid와 같은 간격(22px, 점 중심은 칸의 11px 지점)에 맞춰야 점이 그리드 위에 정확히 올라감
const GRID = 22
const HALF = GRID / 2
const PAD = 16 // 본문과 띄울 최소 간격(px)

// [열, 행] 칸 좌표. 좌우 여백엔 세로로 긴 모양, 위아래 여백엔 가로로 긴 모양
const SHAPES = {
  vertical: [[1, 0], [3, 2], [0, 4], [2, 6], [3, 8]],
  horizontal: [[0, 1], [2, 0], [4, 2], [6, 0], [8, 1]],
}
const spanOf = shape => ({
  cols: Math.max(...shape.map(([c]) => c)),
  rows: Math.max(...shape.map(([, r]) => r)),
})

const root = ref(null)
const size = reactive({ w: 0, h: 0 })
const avoid = reactive({ left: 0, top: 0, right: 0, bottom: 0, ready: false })

// 영역(px) 안에 모양이 들어가면 그리드에 맞춘 점 좌표를, 안 들어가면 null
function place(region, shapeName) {
  const shape = SHAPES[shapeName]
  const span = spanOf(shape)
  // 영역 안에 점 중심이 들어가는 칸 범위
  const c0 = Math.max(0, Math.ceil((region.x0 - HALF) / GRID))
  const c1 = Math.floor((region.x1 - HALF) / GRID)
  const r0 = Math.max(0, Math.ceil((region.y0 - HALF) / GRID))
  const r1 = Math.floor((region.y1 - HALF) / GRID)
  if (c1 - c0 < span.cols || r1 - r0 < span.rows) return null
  const oc = c0 + Math.floor((c1 - c0 - span.cols) / 2)
  const or = r0 + Math.floor((r1 - r0 - span.rows) / 2)
  return shape.map(([c, r]) => ({ x: HALF + GRID * (oc + c), y: HALF + GRID * (or + r) }))
}

const constellations = computed(() => {
  if (!size.w) return []
  const W = size.w
  const H = size.h
  const a = avoid.ready ? avoid : { left: W, top: H, right: 0, bottom: 0 } // 피할 요소가 없으면 전체가 빈 공간
  const candidates = [
    { key: 'left', region: { x0: 0, y0: 0, x1: a.left - PAD, y1: H }, shape: 'vertical' },
    { key: 'right', region: { x0: a.right + PAD, y0: 0, x1: W, y1: H }, shape: 'vertical' },
    { key: 'top', region: { x0: 0, y0: 0, x1: W, y1: a.top - PAD }, shape: 'horizontal' },
    { key: 'bottom', region: { x0: 0, y0: a.bottom + PAD, x1: W, y1: H }, shape: 'horizontal' },
  ]
  const result = []
  for (const cand of candidates) {
    if (result.length === 2) break
    const points = place(cand.region, cand.shape)
    if (!points) continue
    result.push({
      key: cand.key,
      points,
      path: points.map((p, i) => `${i ? 'L' : 'M'}${p.x} ${p.y}`).join(' '),
    })
  }
  return result
})

function measure() {
  const el = root.value
  if (!el) return
  const r = el.getBoundingClientRect()
  size.w = r.width
  size.h = r.height
  const target = props.avoidSelector && el.parentElement?.querySelector(props.avoidSelector)
  if (target) {
    const t = target.getBoundingClientRect()
    Object.assign(avoid, {
      left: t.left - r.left, top: t.top - r.top, right: t.right - r.left, bottom: t.bottom - r.top, ready: true,
    })
  } else {
    avoid.ready = false
  }
}

let observer = null
onMounted(() => {
  observer = new ResizeObserver(measure)
  observer.observe(root.value)
  const target = props.avoidSelector && root.value.parentElement?.querySelector(props.avoidSelector)
  if (target) observer.observe(target)
  measure()
})
onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.connecting-dots {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
svg { display: block; }

.cd-line {
  fill: none;
  stroke: var(--ink);
  stroke-width: 1.2;
  stroke-opacity: 0.3;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  animation: cd-draw 12s ease-in-out infinite;
  animation-delay: var(--offset);
}
.cd-dot {
  fill: var(--ink);
  opacity: 0;
  animation: cd-dot 12s ease-out infinite;
}

/* 선: 천천히 그어졌다가 잠시 머문 뒤 사라짐 */
@keyframes cd-draw {
  0% { stroke-dashoffset: 1; opacity: 1; }
  40% { stroke-dashoffset: 0; opacity: 1; }
  80% { stroke-dashoffset: 0; opacity: 1; }
  95%, 100% { stroke-dashoffset: 0; opacity: 0; }
}
/* 점: 하나씩 찍혔다가 순서대로 사라짐 */
@keyframes cd-dot {
  0% { opacity: 0; }
  4% { opacity: 0.55; }
  75% { opacity: 0.55; }
  90%, 100% { opacity: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .cd-line { animation: none; stroke-dashoffset: 0; }
  .cd-dot { animation: none; opacity: 0.55; }
}
</style>
