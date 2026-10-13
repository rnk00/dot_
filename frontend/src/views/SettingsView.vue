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
          <span class="connected-label"><Icon name="check-circle" :size="15" /> 연결됨 — {{ github.repo }}</span>
          <button class="btn-disconnect" @click="disconnectGithub">해제</button>
        </div>

        <template v-else>
          <!-- 연동 방법 안내 -->
          <button class="guide-toggle" @click="showGuide = !showGuide">
            <Icon name="document" :size="14" />
            연동 방법 {{ showGuide ? '접기' : '보기' }}
            <span class="guide-caret" :class="{ open: showGuide }">▾</span>
          </button>

          <div v-if="showGuide" class="guide-box">
            <div class="guide-step-title">1. 토큰(PAT) 발급</div>
            <ol class="guide-steps">
              <li>GitHub 우측 상단 프로필 사진 → <b>Settings</b></li>
              <li>왼쪽 메뉴 맨 아래 <b>Developer settings</b></li>
              <li>
                <b>Personal access tokens</b> → <b>Fine-grained tokens</b> → <b>Generate new token</b>
                <a class="guide-link" href="https://github.com/settings/personal-access-tokens/new" target="_blank" rel="noopener">바로 가기 →</a>
              </li>
              <li>
                설정값
                <ul class="guide-sub">
                  <li><b>Token name</b> — 아무거나 (예: <code>dot-retro</code>)</li>
                  <li><b>Expiration</b> — 원하는 기간. 만료되면 다시 발급해서 등록해야 해요.</li>
                  <li class="guide-important">
                    <b>Repository access</b> — <b>Only select repositories</b> → 회고 올릴 저장소 <u>하나만</u> 선택
                  </li>
                  <li><b>Permissions</b> → Repository permissions → <b>Contents: Read and write</b></li>
                </ul>
              </li>
              <li><b>Generate token</b>을 누르고 <code>github_pat_...</code> 값을 바로 복사하세요. 페이지를 벗어나면 다시 볼 수 없어요.</li>
            </ol>

            <div class="guide-step-title">2. 아래에 입력</div>
            <ul class="guide-steps guide-plain">
              <li>복사한 토큰과 저장소를 입력하고 저장하면 끝이에요.</li>
              <li>저장소는 <code>owner/repo</code>, <code>repo</code>, <code>https://github.com/owner/repo</code> 모두 가능해요.</li>
            </ul>
          </div>

          <!-- 보안 주의 -->
          <div class="security-callout">
            <div class="security-title"><Icon name="alert-triangle" :size="15" /> 토큰은 회고 저장소 하나에만 권한을 주세요</div>
            <p class="security-desc">
              발급할 때 <b>Repository access</b>를 꼭 <b>Only select repositories</b>로 하고 회고용 저장소만 고르세요.
              <i>All repositories</i>나 Classic 토큰의 <i>repo</i> 권한으로 발급하면, 만에 하나 서버가 해킹돼 토큰이 유출됐을 때
              <b>내 GitHub의 모든 저장소</b>를 누군가 수정하거나 지울 수 있게 돼요.
            </p>
          </div>

          <div class="field-group">
            <label class="field-label">Personal Access Token</label>
            <input
              v-model="github.token"
              type="password"
              class="field-input"
              placeholder="github_pat_xxxxxxxxxxxxxxxxxxxx"
            />
            <div class="field-hint">
              <a href="https://github.com/settings/personal-access-tokens/new" target="_blank" rel="noopener">
                Fine-grained 토큰 발급 →
              </a>
              (회고 저장소 하나만 · Contents 읽기/쓰기)
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
            <div class="field-hint">예: john/daily-retrospects — 토큰 발급 때 선택한 그 저장소여야 해요.</div>
          </div>
          <button class="btn-connect" @click="saveGithub" :disabled="github.saving">
            {{ github.saving ? '저장 중...' : '저장하기' }}
          </button>
        </template>
      </div>

      <!-- 계정 -->
      <div class="section-title">계정</div>
      <div class="card danger-card">
        <div class="danger-title"><Icon name="alert-triangle" :size="15" /> 계정 삭제</div>
        <p class="danger-desc">계정을 삭제하면 모든 회고 데이터가 영구적으로 삭제됩니다. 이 작업은 되돌릴 수 없습니다.</p>
        <button class="btn-danger" @click="deleteAccount">계정 삭제</button>
      </div>

      <!-- 로그아웃 -->
      <button class="btn-logout" @click="logout">로그아웃</button>

      <div class="version-text">dot day v1.0.0</div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { githubApi } from '@/api/github'
import Icon from '@/components/Icon.vue'
import { modal } from '@/composables/useModal'

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
const showGuide = ref(false)

onMounted(() => {
  const user = authStore.user
  if (user?.githubConnected) {
    github.connected = true
    github.repo = user.githubRepo || ''
  }
})

async function saveGithub() {
  if (!github.token.trim() || !github.repo.trim()) return modal.alert('토큰과 저장소 이름을 모두 입력해주세요.')
  github.saving = true
  try {
    // 서버가 실제로 GitHub에 접속해서 토큰·저장소·쓰기 권한을 확인한 뒤에만 저장됨
    await githubApi.updateSettings(github.token, github.repo)
    // 유저 정보 갱신 (서버에 owner/repo 형태로 정리돼 저장된 값 반영)
    await authStore.init()
    github.connected = true
    github.token = ''
    github.repo = authStore.user?.githubRepo || github.repo
    modal.success(`${github.repo} 저장소와 연동되었습니다.`, { title: 'GitHub 연동 완료' })
  } catch (e) {
    modal.error(e.response?.data?.message || '연동에 실패했습니다. 토큰과 저장소 이름을 확인해주세요.', { title: 'GitHub 연동 실패' })
  } finally {
    github.saving = false
  }
}

async function disconnectGithub() {
  const ok = await modal.confirm('회고를 더 이상 GitHub에 업로드할 수 없게 됩니다.\n이미 올라간 파일은 그대로 남아요.', {
    title: 'GitHub 연동을 해제할까요?',
    confirmText: '해제',
  })
  if (!ok) return
  try {
    await githubApi.updateSettings('', '')
    github.connected = false
    github.token = ''
    github.repo = ''
    await authStore.init()
  } catch (e) {
    modal.error(e.response?.data?.message || '연동 해제에 실패했습니다.')
  }
}

function logout() {
  authStore.logout()
  router.push('/login')
}

async function deleteAccount() {
  const ok = await modal.confirm('모든 회고 데이터가 사라지고 되돌릴 수 없습니다.\n(GitHub에 올라간 내용은 그대로 유지됩니다.)', {
    title: '정말 계정을 삭제할까요?',
    type: 'danger',
    confirmText: '삭제 확정',
  })
  if (!ok) return
  try {
    await authApi.deleteMe()
    authStore.logout()
    router.push('/login')
  } catch {
    modal.error('계정 삭제에 실패했습니다. 잠시 후 다시 시도해주세요.')
  }
}
</script>

<style scoped>
.settings-page { min-height: 100%; color: var(--ink); }

.page-header { padding: 22px 20px 0; }
.page-title { font-family: var(--font-serif); font-size: 1.4rem; font-weight: 700; color: var(--ink); }

.settings-body {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-family: var(--font-serif);
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ink-soft);
  margin-top: 14px;
  margin-bottom: 4px;
}

.card {
  background: var(--sheet);
  border: 1px solid var(--rule);
  border-radius: 6px;
  padding: 20px;
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
  background: var(--ink);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  flex-shrink: 0;
}
.profile-name { font-size: 1rem; font-weight: 700; color: var(--ink); }
.profile-email { font-size: 0.8rem; color: var(--ink-soft); margin-top: 2px; }
.provider-badge {
  display: inline-block;
  margin-top: 6px;
  font-size: 0.7rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}
.provider-badge.kakao { background: #FEE500; color: #3A1D1D; }
.provider-badge.github { background: var(--ink); color: #fff; }

/* 토글 */
.toggle-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px dashed var(--rule);
}
.toggle-item:last-child { border-bottom: none; padding-bottom: 0; }
.toggle-item:first-child { padding-top: 0; }
.toggle-label { font-size: 0.875rem; font-weight: 600; color: var(--ink); }
.toggle-desc { font-size: 0.775rem; color: var(--ink-faint); margin-top: 2px; }

.toggle-switch { position: relative; display: inline-block; width: 44px; height: 24px; flex-shrink: 0; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider {
  position: absolute;
  inset: 0;
  background: var(--rule);
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
  background: var(--sheet);
  border-radius: 50%;
  transition: 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
input:checked + .toggle-slider { background: var(--ink); }
input:checked + .toggle-slider::before { transform: translateX(20px); }

/* GitHub */
.field-group { margin-bottom: 14px; }
.field-label { display: block; font-size: 0.8rem; font-weight: 600; color: var(--ink); margin-bottom: 6px; }
.field-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--rule);
  border-radius: 6px;
  font-size: 0.875rem;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;
}
.field-input:focus { border-color: var(--ink); }
.field-hint { font-size: 0.75rem; color: var(--ink-faint); margin-top: 4px; }
.field-hint a { color: var(--ink); font-weight: 600; text-decoration: underline; text-underline-offset: 2px; }
.btn-connect {
  padding: 10px 18px;
  background: var(--ink);
  color: #fff;
  border: none;
  border-radius: 6px;
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
  background: var(--sheet);
  border: 1.5px solid var(--hl-keep);
  border-radius: 6px;
  padding: 10px 14px;
  font-size: 0.875rem;
  color: var(--ok-ink);
  font-weight: 600;
}
.btn-disconnect {
  background: none;
  border: 1px solid var(--rule);
  color: var(--ink-faint);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  cursor: pointer;
}
.guide-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  padding: 0;
  margin-bottom: 12px;
  color: var(--ink);
  font-size: 0.825rem;
  font-weight: 600;
  cursor: pointer;
}
.guide-caret { transition: transform 0.15s; }
.guide-caret.open { transform: rotate(180deg); }

.guide-box {
  background: var(--paper-deep);
  border: 1px solid var(--rule);
  border-radius: 6px;
  padding: 14px 16px;
  margin-bottom: 14px;
  font-size: 0.825rem;
  color: var(--ink-soft);
  line-height: 1.6;
}
.guide-step-title { font-weight: 700; color: var(--ink); margin-bottom: 6px; }
.guide-step-title:not(:first-child) { margin-top: 14px; }
.guide-steps { padding-left: 20px; margin: 0; display: flex; flex-direction: column; gap: 4px; }
.guide-plain { list-style: disc; }
.guide-sub { padding-left: 18px; margin: 4px 0 0; list-style: disc; display: flex; flex-direction: column; gap: 3px; }
.guide-link { margin-left: 6px; color: var(--ink); font-weight: 700; text-decoration: underline; text-underline-offset: 2px; white-space: nowrap; }
.guide-important {
  list-style: none;
  margin-left: -18px;
  padding: 6px 10px;
  background: #fdf4f1;
  border: 1.5px solid var(--red-pen);
  border-radius: 6px;
  color: var(--red-pen);
}
.guide-important b { color: var(--red-pen); }

.security-callout {
  border: 1.5px solid var(--hl-problem);
  background: #fff6f4;
  border-radius: 6px;
  padding: 12px 14px;
  margin-bottom: 16px;
}
.security-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--red-pen);
  margin-bottom: 6px;
}
.security-desc { font-size: 0.8rem; color: var(--ink-soft); line-height: 1.6; margin: 0; }
.security-desc b { color: var(--ink); }

code { background: var(--paper-deep); padding: 1px 5px; border-radius: 4px; font-size: 0.8rem; }

/* 위험 */
.danger-card { border-color: #f1d3cb; background: #fdf4f1; }
.danger-title { display: flex; align-items: center; gap: 6px; font-size: 0.9rem; font-weight: 700; color: var(--red-pen); margin-bottom: 8px; }
.connected-label { display: flex; align-items: center; gap: 6px; }
.danger-desc { font-size: 0.825rem; color: var(--ink-soft); line-height: 1.5; margin-bottom: 14px; }
.btn-danger {
  padding: 9px 18px;
  background: var(--red-pen);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-logout {
  width: 100%;
  padding: 14px;
  background: var(--sheet);
  color: var(--ink-soft);
  border: 1px solid var(--rule);
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.15s;
}
.btn-logout:hover { background: var(--paper-deep); }

.version-text { text-align: center; font-size: 0.75rem; color: var(--ink-faint); margin-top: 8px; }

</style>
