<template>
  <div class="settings-page">
    <div class="page-header">
      <h2 class="page-title">설정</h2>
    </div>

    <div class="settings-body">
      <!-- 프로필 -->
      <div class="section-title">프로필</div>
      <div class="card profile-card">
        <div class="profile-avatar">{{ authStore.user?.nickname?.[0] || '?' }}</div>
        <div class="profile-info">
          <div class="profile-name">{{ authStore.user?.nickname }}</div>
          <div class="profile-email">{{ authStore.user?.email }}</div>
          <div class="provider-badge" :class="authStore.user?.provider?.toLowerCase()">
            {{ authStore.user?.provider === 'KAKAO' ? '카카오' : 'GitHub' }} 계정
          </div>
        </div>
      </div>

      <!-- 알림 -->
      <div class="section-title">알림</div>
      <div class="card">
        <div class="toggle-item">
          <div class="toggle-info">
            <div class="toggle-label">일일 리마인더</div>
            <div class="toggle-desc">매일 오후 8시에 회고 작성 알림</div>
          </div>
          <label class="toggle-switch">
            <input type="checkbox" v-model="settings.dailyReminder" />
            <span class="toggle-slider" />
          </label>
        </div>
        <div class="toggle-item">
          <div class="toggle-info">
            <div class="toggle-label">주간 요약</div>
            <div class="toggle-desc">매주 월요일 지난 주 회고 요약 받기</div>
          </div>
          <label class="toggle-switch">
            <input type="checkbox" v-model="settings.weeklySummary" />
            <span class="toggle-slider" />
          </label>
        </div>
        <div class="toggle-item">
          <div class="toggle-info">
            <div class="toggle-label">AI 인사이트</div>
            <div class="toggle-desc">새로운 AI 분석 결과 알림</div>
          </div>
          <label class="toggle-switch">
            <input type="checkbox" v-model="settings.aiInsight" />
            <span class="toggle-slider" />
          </label>
        </div>
      </div>

      <!-- GitHub 연동 -->
      <div class="section-title">GitHub 연동</div>
      <div class="card">
        <div class="toggle-label" style="margin-bottom:4px">회고 → GitHub 업로드</div>
        <div class="toggle-desc" style="margin-bottom:16px">
          회고를 GitHub 저장소의 <code>retrospects/</code> 폴더에 마크다운으로 업로드합니다.
        </div>

        <div v-if="github.connected" class="connected-banner">
          ✅ 연결됨 — {{ github.repo }}
          <button class="btn-disconnect" @click="disconnectGithub">해제</button>
        </div>

        <template v-else>
          <div class="field-group">
            <label class="field-label">Personal Access Token</label>
            <input
              v-model="github.token"
              type="password"
              class="field-input"
              placeholder="ghp_xxxxxxxxxxxxxxxxxxxx"
            />
            <div class="field-hint">
              <a href="https://github.com/settings/tokens/new?scopes=repo&description=DOT+회고노트" target="_blank">
                토큰 발급 →
              </a>
              (repo 권한 필요)
            </div>
          </div>
          <div class="field-group">
            <label class="field-label">저장소 이름</label>
            <input
              v-model="github.repo"
              type="text"
              class="field-input"
              placeholder="username/my-retrospects"
            />
            <div class="field-hint">예: john/daily-retrospects</div>
          </div>
          <button class="btn-connect" @click="saveGithub" :disabled="github.saving">
            {{ github.saving ? '저장 중...' : '저장하기' }}
          </button>
        </template>
      </div>

      <!-- 계정 -->
      <div class="section-title">계정</div>
      <div class="card danger-card">
        <div class="danger-title">⚠️ 계정 삭제</div>
        <p class="danger-desc">계정을 삭제하면 모든 회고 데이터가 영구적으로 삭제됩니다. 이 작업은 되돌릴 수 없습니다.</p>
        <button class="btn-danger" @click="deleteAccount">계정 삭제</button>
      </div>

      <!-- 로그아웃 -->
      <button class="btn-logout" @click="logout">로그아웃</button>

      <div class="version-text">회고 노트 v1.0.0</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { githubApi } from '@/api/github'

const router = useRouter()
const authStore = useAuthStore()

const settings = reactive({
  dailyReminder: false,
  weeklySummary: false,
  aiInsight: true,
})

const github = reactive({
  token: '',
  repo: '',
  connected: false,
  saving: false,
})

onMounted(() => {
  const user = authStore.user
  if (user?.githubConnected) {
    github.connected = true
    github.repo = user.githubRepo || ''
  }
})

async function saveGithub() {
  if (!github.token || !github.repo) return alert('토큰과 저장소 이름을 모두 입력해주세요.')
  github.saving = true
  try {
    await githubApi.updateSettings(github.token, github.repo)
    github.connected = true
    // 유저 정보 갱신
    await authStore.init()
    alert('GitHub 연동이 완료되었습니다!')
  } catch {
    alert('저장에 실패했습니다. 토큰과 저장소 이름을 확인해주세요.')
  } finally {
    github.saving = false
  }
}

async function disconnectGithub() {
  if (!confirm('GitHub 연동을 해제할까요?')) return
  await githubApi.updateSettings('', '')
  github.connected = false
  github.token = ''
  github.repo = ''
  await authStore.init()
}

function logout() {
  authStore.logout()
  router.push('/login')
}

function deleteAccount() {
  if (confirm('정말로 계정을 삭제하시겠습니까? 모든 데이터가 사라집니다.')) {
    alert('계정 삭제 기능은 준비 중입니다.')
  }
}
</script>

<style scoped>
.settings-page { background: #f8fafc; min-height: 100%; }

.page-header { padding: 20px 20px 0; }
.page-title { font-size: 1.2rem; font-weight: 800; color: #1e293b; }

.settings-body {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-size: 0.75rem;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-top: 12px;
  margin-bottom: 4px;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

/* 프로필 */
.profile-card {
  display: flex;
  align-items: center;
  gap: 16px;
}
.profile-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  flex-shrink: 0;
}
.profile-name { font-size: 1rem; font-weight: 700; color: #1e293b; }
.profile-email { font-size: 0.8rem; color: #64748b; margin-top: 2px; }
.provider-badge {
  display: inline-block;
  margin-top: 6px;
  font-size: 0.7rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
}
.provider-badge.kakao { background: #FEE500; color: #3A1D1D; }
.provider-badge.github { background: #24292e; color: #fff; }

/* 토글 */
.toggle-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}
.toggle-item:last-child { border-bottom: none; padding-bottom: 0; }
.toggle-item:first-child { padding-top: 0; }
.toggle-label { font-size: 0.875rem; font-weight: 600; color: #374151; }
.toggle-desc { font-size: 0.775rem; color: #94a3b8; margin-top: 2px; }

.toggle-switch { position: relative; display: inline-block; width: 44px; height: 24px; flex-shrink: 0; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider {
  position: absolute;
  inset: 0;
  background: #e2e8f0;
  border-radius: 24px;
  cursor: pointer;
  transition: 0.2s;
}
.toggle-slider::before {
  content: '';
  position: absolute;
  width: 18px;
  height: 18px;
  left: 3px;
  top: 3px;
  background: #fff;
  border-radius: 50%;
  transition: 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
input:checked + .toggle-slider { background: #6366f1; }
input:checked + .toggle-slider::before { transform: translateX(20px); }

/* GitHub */
.field-group { margin-bottom: 14px; }
.field-label { display: block; font-size: 0.8rem; font-weight: 600; color: #374151; margin-bottom: 6px; }
.field-input {
  width: 100%;
  padding: 10px 12px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 0.875rem;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;
}
.field-input:focus { border-color: #6366f1; }
.field-hint { font-size: 0.75rem; color: #94a3b8; margin-top: 4px; }
.field-hint a { color: #6366f1; text-decoration: none; }
.btn-connect {
  padding: 10px 18px;
  background: #24292e;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  width: 100%;
}
.btn-connect:disabled { opacity: 0.6; cursor: not-allowed; }
.connected-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f0fdf4;
  border: 1.5px solid #bbf7d0;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 0.875rem;
  color: #059669;
  font-weight: 600;
}
.btn-disconnect {
  background: none;
  border: 1px solid #e2e8f0;
  color: #94a3b8;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  cursor: pointer;
}
code { background: #f1f5f9; padding: 1px 5px; border-radius: 4px; font-size: 0.8rem; }

/* 위험 */
.danger-card { border: 1.5px solid #fecaca; background: #fff5f5; }
.danger-title { font-size: 0.9rem; font-weight: 700; color: #ef4444; margin-bottom: 8px; }
.danger-desc { font-size: 0.825rem; color: #64748b; line-height: 1.5; margin-bottom: 14px; }
.btn-danger {
  padding: 9px 18px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-logout {
  width: 100%;
  padding: 14px;
  background: #f1f5f9;
  color: #64748b;
  border: none;
  border-radius: 12px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.15s;
}
.btn-logout:hover { background: #e2e8f0; }

.version-text { text-align: center; font-size: 0.75rem; color: #cbd5e1; margin-top: 8px; }
</style>
