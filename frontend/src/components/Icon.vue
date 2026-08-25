<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
    class="icon"
  >
    <template v-if="name === 'document'">
      <path d="M14 3H6a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8z" />
      <path d="M14 3v5h5" />
      <path d="M9 13h6M9 17h6" />
    </template>
    <template v-else-if="name === 'bar-chart'">
      <path d="M4 20V10M12 20V4M20 20v-7" />
    </template>
    <template v-else-if="name === 'flame'">
      <path d="M12 2c1 3-2 4-2 7a3 3 0 1 0 6 0c0-1-1-2-1-2 1 3 2.5 4 2.5 7A6.5 6.5 0 0 1 6 14c0-5 4-6 4-9 0-1 .5-2 2-3z" />
    </template>
    <template v-else-if="name === 'star'">
      <path d="M12 3l2.6 5.6 6.1.6-4.6 4.1 1.3 6-5.4-3.2-5.4 3.2 1.3-6-4.6-4.1 6.1-.6z" />
    </template>
    <template v-else-if="name === 'sparkle'">
      <path d="M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5L18 18M18 6l-2.5 2.5M8.5 15.5L6 18" />
    </template>
    <template v-else-if="name === 'check-circle'">
      <circle cx="12" cy="12" r="9" />
      <path d="M8.5 12.5l2.5 2.5 4.5-5" />
    </template>
    <template v-else-if="name === 'alert-triangle'">
      <path d="M12 3.5l9.5 16.5H2.5z" />
      <path d="M12 10v4M12 17.5v.1" />
    </template>
    <template v-else-if="name === 'repeat'">
      <path d="M4 9a5 5 0 0 1 5-5h9M18 4v4h-4M20 15a5 5 0 0 1-5 5H6M6 20v-4h4" />
    </template>
    <template v-else-if="name === 'target'">
      <circle cx="12" cy="12" r="8.5" />
      <circle cx="12" cy="12" r="4.5" />
      <circle cx="12" cy="12" r="0.5" fill="currentColor" />
    </template>
    <template v-else-if="name === 'lightbulb'">
      <path d="M9 18h6M10 21h4" />
      <path d="M12 3a6 6 0 0 0-3.5 10.9c.5.4.8 1 .8 1.6h5.4c0-.6.3-1.2.8-1.6A6 6 0 0 0 12 3z" />
    </template>
    <template v-else-if="name === 'upload'">
      <path d="M12 16V4M7 9l5-5 5 5" />
      <path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
    </template>
  </svg>
</template>

<script setup>
defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 16 },
})
</script>

<style scoped>
.icon { flex-shrink: 0; vertical-align: -3px; }
</style>
