// 모듈 스코프 캐시 — CalendarView가 언마운트/재마운트돼도(캘린더 -> 회고 작성 -> 캘린더 이동 등)
// 페이지를 새로고침하지 않는 한 유지되어, 이미 본 적 있는 달은 다시 안 기다리게 함
export const calendarCache = new Map()
