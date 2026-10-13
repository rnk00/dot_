import { reactive } from 'vue'

// 앱 전체에서 하나만 쓰는 공용 모달 상태 (App.vue의 <AppModal />이 이걸 그림)
export const modalState = reactive({
  show: false,
  mode: 'alert',        // 'alert' | 'confirm'
  type: 'info',         // 'info' | 'success' | 'error' | 'danger'
  title: '',
  message: '',
  confirmText: '확인',
  cancelText: '취소',
  resolve: null,
})

function open(options) {
  // 이미 떠 있는 모달이 있으면 취소로 닫고 새로 띄움
  if (modalState.resolve) modalState.resolve(false)
  return new Promise(resolve => {
    Object.assign(modalState, {
      show: true,
      mode: 'alert',
      type: 'info',
      title: '',
      confirmText: '확인',
      cancelText: '취소',
      ...options,
      resolve,
    })
  })
}

export function closeModal(result) {
  const resolve = modalState.resolve
  modalState.show = false
  modalState.resolve = null
  if (resolve) resolve(result)
}

export const modal = {
  /** 확인 버튼 하나. 닫히면 resolve */
  alert(message, options = {}) {
    return open({ ...options, mode: 'alert', message })
  },
  /** 확인/취소. 확인이면 true, 취소·바깥 클릭이면 false */
  confirm(message, options = {}) {
    return open({ ...options, mode: 'confirm', message })
  },
  success(message, options = {}) {
    return open({ title: '완료', ...options, mode: 'alert', type: 'success', message })
  },
  error(message, options = {}) {
    return open({ title: '오류', ...options, mode: 'alert', type: 'error', message })
  },
}

export function useModal() {
  return modal
}
