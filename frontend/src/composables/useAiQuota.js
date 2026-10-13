import { reactive } from 'vue'
import { aiApi } from '@/api/ai'

// AI 기능별 오늘 남은 횟수 (앱 전체에서 공유). 서버 응답이 오기 전엔 null
export const aiQuota = reactive({
  TRY_RECOMMENDATION: null, // { remaining, limit }
  INSIGHT: null,
  REFINE: null,
})

export async function refreshAiQuota() {
  try {
    Object.assign(aiQuota, await aiApi.quota())
  } catch { /* 표시만 못 할 뿐 기능엔 영향 없음 */ }
}

// AI 호출 응답에 담겨 온 남은 횟수로 갱신
export function updateAiQuota(feature, res) {
  if (res && typeof res.remaining === 'number') {
    aiQuota[feature] = { remaining: res.remaining, limit: res.limit }
  }
}

// 429(오늘 다 씀) 응답이면 0으로 맞춤
export function markQuotaExhausted(feature, error) {
  if (error?.response?.status === 429 && aiQuota[feature]) {
    aiQuota[feature] = { ...aiQuota[feature], remaining: 0 }
  }
}

// 회고 목록에서 K 또는 P 내용만 줄바꿈으로 이어붙임 (항목은 { content } 배열)
export function joinItems(retros, key) {
  return retros
    .flatMap(r => (r[key] || []).map(i => i?.content?.trim()))
    .filter(Boolean)
    .join('\n')
}
