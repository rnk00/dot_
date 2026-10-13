<template>
  <div class="app-layout">
    <!-- 헤더 -->
    <header class="header">
      <span class="logo"><LogoMark :size="9" />dot day</span>
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
        <span class="nav-dot" />
        <span>캘린더</span>
      </RouterLink>
      <RouterLink to="/analysis" class="nav-item" :class="{ active: isActive('/analysis') }">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>
        </svg>
        <span class="nav-dot" />
        <span>분석</span>
      </RouterLink>
      <RouterLink to="/settings" class="nav-item" :class="{ active: isActive('/settings') }">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>
        </svg>
        <span class="nav-dot" />
        <span>설정</span>
      </RouterLink>
    </nav>
  </div>
</template>

<script setup>
import { RouterView, RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LogoMark from '@/components/LogoMark.vue'

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
  background: var(--dot-grid), var(--paper);
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
  background: var(--paper);
  border-bottom: 1px solid var(--rule);
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-serif);
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--ink);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-nick {
  font-size: 0.875rem;
  color: var(--ink-soft);
  font-weight: 500;
}

.btn-logout {
  font-size: 0.8rem;
  color: var(--ink-faint);
  padding: 5px 10px;
  border-radius: 6px;
  border: 1px solid var(--rule);
  background: none;
  transition: all 0.15s;
}
.btn-logout:hover { background: var(--paper-deep); color: var(--ink-soft); }

.content {
  flex: 1;
  padding-bottom: 64px;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 50;
  display: flex;
  background: var(--paper);
  border-top: 1px solid var(--rule);
  height: 60px;
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  text-decoration: none;
  color: var(--ink-faint);
  font-size: 0.7rem;
  font-weight: 500;
  transition: color 0.15s;
}

.nav-item.active {
  color: var(--ink);
  font-weight: 700;
}
/* 선택된 탭엔 아이콘 밑에 잉크 점이 톡 찍힘 */
.nav-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--ink);
  transform: scale(0);
  transition: transform 0.2s cubic-bezier(0.3, 1.6, 0.5, 1);
}
.nav-item.active .nav-dot { transform: scale(1); }
</style>
