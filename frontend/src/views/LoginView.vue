<template>
  <div class="login-page">
    <ConnectingDots avoid-selector=".login-layout" />
    <div class="login-layout">
      <!-- 소개 -->
      <section class="intro">
        <div class="intro-brand">
          <LogoMark :size="10" class="brand-mark" />
          <span class="intro-brand-name">dot day</span>
        </div>

        <h1 class="hero-title">어제랑 같은 실수,<br />오늘도 했나요?</h1>
        <p class="hero-sub">
          머릿속으로 한 반성은 하룻밤이면 사라져요.<br />
          적어야 바뀝니다. 하루 5분, <mark class="hl">KPT 회고</mark>로.
        </p>

        <div class="kpt-steps">
          <div class="kpt-step">
            <span class="kpt-badge k">K</span>
            <div><b>Keep</b><span>잘한 점 — 계속할 것</span></div>
          </div>
          <div class="kpt-step">
            <span class="kpt-badge p">P</span>
            <div><b>Problem</b><span>아쉬운 점 — 고칠 것</span></div>
          </div>
          <div class="kpt-step">
            <span class="kpt-badge t">T</span>
            <div><b>Try</b><span>다음 행동 — 내일 해볼 것</span></div>
          </div>
        </div>

        <ul class="why-list">
          <li>
            <b>'다음엔 잘하자'는 계획이 아니에요.</b>
            <span>Problem을 Try로 바꾸는 순간, 반성이 내일의 할 일이 돼요.</span>
          </li>
          <li>
            <b>이유를 모르는 성공은 그냥 운이에요.</b>
            <span>잘된 걸 Keep으로 적어둬야 다음에 다시 꺼내 쓸 수 있어요.</span>
          </li>
          <li>
            <b>기록하지 않은 성장은 증명할 수 없어요.</b>
            <span>매일의 회고가 캘린더와 분석, 내 GitHub에 쌓여요.</span>
          </li>
        </ul>
      </section>

      <!-- 로그인 -->
      <div class="login-card">
        <h2 class="login-title">지금 시작하기</h2>
        <p class="login-desc">오늘 하루부터 돌아봐요.</p>

        <div class="social-buttons">
          <a :href="`${backendUrl}/oauth2/authorization/kakao`" class="btn btn-kakao">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 3C6.477 3 2 6.477 2 10.8c0 2.736 1.668 5.14 4.186 6.587L5.07 21l4.868-2.56c.667.093 1.35.143 2.062.143 5.523 0 10-3.477 10-7.783C22 6.477 17.523 3 12 3z"/>
            </svg>
            카카오로 시작하기
          </a>
          <a :href="`${backendUrl}/oauth2/authorization/github`" class="btn btn-github">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C6.477 2 2 6.477 2 12c0 4.418 2.865 8.166 6.839 9.489.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12c0-5.523-4.477-10-10-10z"/>
            </svg>
            GitHub으로 시작하기
          </a>
        </div>

        <p v-if="isError" class="error-msg">로그인에 실패했습니다. 다시 시도해주세요.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import LogoMark from '@/components/LogoMark.vue'
import ConnectingDots from '@/components/ConnectingDots.vue'

const route = useRoute()
const isError = computed(() => route.query.error === 'true')
// 소셜 로그인은 페이지 이동(<a href>)이라 CORS 걱정 없이 백엔드로 직접 보냄 —
// Vercel 프록시를 거치면 리다이렉트 중 Set-Cookie가 유실돼서 OAuth 진행 상태가 사라짐
const backendUrl = import.meta.env.VITE_BACKEND_URL || ''
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--dot-grid), var(--paper);
  padding: 32px 16px;
  color: var(--ink);
  position: relative;
  overflow: hidden;
}

.login-layout {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 560px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}
/* 태블릿·작은 노트북부터 소개 | 로그인 2단 */
@media (min-width: 720px) {
  .login-layout {
    max-width: 1040px;
    flex-direction: row;
    align-items: center;
    gap: 40px;
  }
  .intro { flex: 1; min-width: 0; }
  .login-card { width: 340px; flex-shrink: 0; }
}
@media (min-width: 1024px) {
  .login-layout { gap: 72px; }
  .login-card { width: 380px; padding: 40px 32px; }
}

/* 소개 */
.intro-brand { display: flex; align-items: center; gap: 8px; margin-bottom: 22px; }
.brand-mark { color: var(--ink); }
.intro-brand-name { font-family: var(--font-serif); font-size: 1.05rem; font-weight: 700; color: var(--ink); }

.hero-title {
  font-family: var(--font-serif);
  font-size: 1.9rem;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.4;
  letter-spacing: -0.5px;
  word-break: keep-all;
}
@media (min-width: 1024px) { .hero-title { font-size: 2.6rem; } }
.hero-sub {
  margin-top: 14px;
  font-size: 0.95rem;
  color: var(--ink-soft);
  line-height: 1.75;
  word-break: keep-all;
}
/* 형광펜으로 그은 듯한 강조 */
.hl {
  background: linear-gradient(transparent 55%, var(--hl-try) 55%, var(--hl-try) 92%, transparent 92%);
  color: var(--ink);
  font-weight: 700;
  padding: 0 2px;
}

.kpt-steps {
  margin-top: 26px;
  background: var(--sheet);
  border: 1px solid var(--rule);
  border-radius: 6px;
}
.kpt-step {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  font-size: 0.875rem;
  color: var(--ink-soft);
}
.kpt-step + .kpt-step { border-top: 1px dashed var(--rule); }
.kpt-step b { color: var(--ink); margin-right: 8px; }
.kpt-badge {
  width: 26px;
  height: 26px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--ink);
  flex-shrink: 0;
}
.kpt-badge.k { background: var(--hl-keep); }
.kpt-badge.p { background: var(--hl-problem); }
.kpt-badge.t { background: var(--hl-try); }

.why-list {
  list-style: none;
  margin-top: 26px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.why-list li { padding-left: 18px; position: relative; word-break: keep-all; }
.why-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--ink);
}
.why-list b { display: block; font-size: 0.95rem; color: var(--ink); }
.why-list span { display: block; margin-top: 2px; font-size: 0.85rem; color: var(--ink-soft); line-height: 1.6; }

/* 로그인 — 종이 한 장 */
.login-card {
  background: var(--sheet);
  border: 1px solid var(--rule);
  border-radius: 6px;
  padding: 32px 28px;
  box-shadow: 0 1px 0 var(--rule), 0 12px 32px rgba(31, 42, 68, 0.07);
}
.login-title { font-family: var(--font-serif); font-size: 1.35rem; font-weight: 700; color: var(--ink); }
.login-desc { margin-top: 4px; margin-bottom: 24px; font-size: 0.875rem; color: var(--ink-soft); }

.social-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 13px;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 600;
  text-decoration: none;
  transition: transform 0.15s, opacity 0.15s;
}
.btn:hover { opacity: 0.92; transform: translateY(-1px); }

.btn-kakao { background: #FEE500; color: #3A1D1D; }
.btn-github { background: var(--ink); color: #fff; }

.error-msg {
  margin-top: 16px;
  text-align: center;
  color: var(--red-pen);
  font-size: 0.875rem;
}
</style>
