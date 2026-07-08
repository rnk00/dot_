<template>
  <div class="app-layout">
    <!-- 헤더 -->
    <header class="header">
      <span class="logo">회고 노트</span>
      <div class="header-right">
        <span class="user-nick">{{ authStore.user?.nickname }}</span>
        <button class="btn-logout" @click="logout">로그아웃</button>
      </div>
    </header>

    <!-- 콘텐츠 -->
    <main class="content">
      <RouterView />
    </main>

    <!-- 하단 탭 네비게이션 -->
    <nav class="bottom-nav">
      <RouterLink to="/calendar" class="nav-item" :class="{ active: isActive('/calendar') }">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
        </svg>
        <span>캘린더</span>
      </RouterLink>
      <RouterLink to="/analysis" class="nav-item" :class="{ active: isActive('/analysis') }">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>
        </svg>
        <span>분석</span>
      </RouterLink>
      <RouterLink to="/settings" class="nav-item" :class="{ active: isActive('/settings') }">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>
        </svg>
        <span>설정</span>
      </RouterLink>
    </nav>
  </div>
</template>

<script setup>
import { RouterView, RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

function isActive(path) {
  return route.path.startsWith(path)
}

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f8fafc;
}

.header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.logo {
  font-size: 1.1rem;
  font-weight: 800;
  color: #6366f1;
  letter-spacing: -0.5px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-nick {
  font-size: 0.875rem;
  color: #64748b;
  font-weight: 500;
}

.btn-logout {
  font-size: 0.8rem;
  color: #94a3b8;
  padding: 5px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: none;
  transition: all 0.15s;
}
.btn-logout:hover { background: #f1f5f9; color: #64748b; }

.content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 64px;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 50;
  display: flex;
  background: #fff;
  border-top: 1px solid #e2e8f0;
  height: 60px;
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  text-decoration: none;
  color: #94a3b8;
  font-size: 0.7rem;
  font-weight: 500;
  transition: color 0.15s;
}

.nav-item.active {
  color: #6366f1;
}
</style>
