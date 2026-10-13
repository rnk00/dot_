import api from './index'

export const aiApi = {
  // Try 추천 3개 → { suggestions, batch, remaining, limit }
  // date를 넘기면 그 날짜 회고의 추천 히스토리로 저장됨
  suggestTry: (keep, problem, date = null) =>
    api.post('/ai/suggest-try', { keep, problem, date }).then(r => r.data),

  // 회고 날짜별 Try 추천 히스토리 → [{ batchId, createdAt, suggestions }] (최신순)
  suggestTryHistory: (date) =>
    api.get('/ai/suggest-try/history', { params: { date } }).then(r => r.data),

  // K/P/T 한 줄 다듬기 → { refined, remaining, limit }
  refine: (type, content, context) =>
    api.post('/ai/refine', { type, content, context }).then(r => r.data),

  // 여러 날 K/P 패턴 인사이트 → { insight, remaining, limit }
  insight: (keep, problem) =>
    api.post('/ai/insight', { keep, problem }).then(r => r.data),

  // 오늘 남은 횟수 → { TRY_RECOMMENDATION: { remaining, limit }, INSIGHT: {...}, REFINE: {...} }
  quota: () =>
    api.get('/ai/quota').then(r => r.data),
}
