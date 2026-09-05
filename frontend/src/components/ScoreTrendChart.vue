<template>
  <svg :viewBox="`0 0 ${width} ${height}`" class="line-chart">
    <line v-for="s in 5" :key="s" :x1="padding" :x2="width - padding" :y1="yFor(s)" :y2="yFor(s)" class="grid-line" />
    <text v-for="s in 5" :key="`t${s}`" :x="padding - 6" :y="yFor(s) + 4" class="axis-label">{{ s }}</text>

    <polyline :points="points" class="line-path" />
    <circle
      v-for="(d, i) in data" :key="d.date"
      :cx="xFor(i)" :cy="yFor(d.score)" r="4"
      class="point"
      @click="$emit('point-click', d.date)"
    >
      <title>{{ d.date }} · {{ d.score }}점</title>
    </circle>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ data: { type: Array, default: () => [] } })
defineEmits(['point-click'])

const width = 600
const height = 200
const padding = 28

function xFor(i) {
  if (props.data.length <= 1) return width / 2
  return padding + (i / (props.data.length - 1)) * (width - padding * 2)
}
function yFor(score) {
  return height - padding - ((score - 1) / 4) * (height - padding * 2)
}

const points = computed(() => props.data.map((d, i) => `${xFor(i)},${yFor(d.score)}`).join(' '))
</script>

<style scoped>
.line-chart { width: 100%; height: 200px; }
.grid-line { stroke: #f1f5f9; stroke-width: 1; }
.axis-label { font-size: 9px; fill: #94a3b8; text-anchor: end; }
.line-path { fill: none; stroke: #6366f1; stroke-width: 2; }
.point { fill: #6366f1; cursor: pointer; stroke: #fff; stroke-width: 1.5; }
.point:hover { fill: #4338ca; }
</style>
