// 만족도 → 잉크 점 크기·농도 (1점: 작고 흐린 점, 5점: 크고 진한 점)
// md: 캘린더 칸·분석 그래프처럼 점이 주인공인 곳 / sm: 목록·범례·버튼처럼 글자 옆에 붙는 곳
const DOT_SIZE = {
  md: { 1: 6, 2: 9, 3: 12, 4: 15, 5: 18 },
  sm: { 1: 4, 2: 6, 3: 8, 4: 10, 5: 12 },
}
// 크기 차이를 크게 두지 않는 대신 진하기 폭을 넓혀서 구분
const DOT_OPACITY = { 1: 0.15, 2: 0.35, 3: 0.55, 4: 0.78, 5: 1 }

export function dotStyle(score, variant = 'md') {
  const sizes = DOT_SIZE[variant] || DOT_SIZE.md
  const size = sizes[score] || sizes[3]
  return { width: `${size}px`, height: `${size}px`, opacity: DOT_OPACITY[score] ?? 0.55 }
}
