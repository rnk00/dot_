import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import MainLayout from '@/components/MainLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/oauth2/callback',
    name: 'OAuth2Callback',
    component: () => import('@/views/OAuth2CallbackView.vue')
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/calendar' },
      { path: 'calendar', name: 'Calendar', component: () => import('@/views/CalendarView.vue') },
      { path: 'retrospect/:date', name: 'Retrospect', component: () => import('@/views/RetrospectView.vue') },
      { path: 'analysis', name: 'Analysis', component: () => import('@/views/AnalysisView.vue') },
      { path: 'settings', name: 'Settings', component: () => import('@/views/SettingsView.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  if (!authStore.initialized) await authStore.init()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) next('/login')
  else if (to.meta.requiresGuest && authStore.isLoggedIn) next('/calendar')
  else next()
})

export default router
