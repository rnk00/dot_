// 회고/KPT 항목 작성·수정 가능 기간 규칙 (백엔드 RetrospectDatePolicy와 동일 규칙)
export const EDIT_WINDOW_DAYS = 14

function toDateOnly(d) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x
}

export function isFutureDate(dateStr) {
  const today = toDateOnly(new Date())
  const target = toDateOnly(dateStr)
  return target > today
}

export function isWithinEditWindow(dateStr) {
  const today = toDateOnly(new Date())
  const target = toDateOnly(dateStr)
  const earliest = new Date(today)
  earliest.setDate(earliest.getDate() - EDIT_WINDOW_DAYS)
  return target >= earliest && target <= today
}
