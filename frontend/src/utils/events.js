// Centralized event names for cross-component communication
export const EVENTS = {
  REFRESH_DATA: 'refresh-data',
  OPEN_TASK_DETAIL: 'open-task-detail'
}

export function emitRefresh() {
  window.dispatchEvent(new CustomEvent(EVENTS.REFRESH_DATA))
}

export function emitOpenTaskDetail(taskId) {
  window.dispatchEvent(new CustomEvent(EVENTS.OPEN_TASK_DETAIL, { detail: { taskId } }))
}

export function onRefreshData(handler) {
  window.addEventListener(EVENTS.REFRESH_DATA, handler)
  return () => window.removeEventListener(EVENTS.REFRESH_DATA, handler)
}
