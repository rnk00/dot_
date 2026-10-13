<template>
  <div class="callback-page">
    <DotLoader label="로그인하는 중" />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import DotLoader from '@/components/DotLoader.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

onMounted(async () => {
  const token = route.query.token
  if (token) {
    authStore.setToken(token)
    await authStore.init()
    router.push('/calendar')
  } else {
    router.push('/login')
  }
})
</script>

<style scoped>
.callback-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--dot-grid), var(--paper);
  font-family: var(--font-serif);
  color: var(--ink-soft);
}
</style>
