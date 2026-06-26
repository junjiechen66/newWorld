// Centralized event names for cross-component communication
export const EVENTS = {
  REFRESH_DATA: 'refresh-data',
  OPEN_TASK_DETAIL: 'open-task-detail',
  CALENDAR_DATE_CHANGE: 'calendar-date-change'
}

export function emitRefresh() {
  window.dispatchEvent(new CustomEvent(EVENTS.REFRESH_DATA))
}

export function emitOpenTaskDetail(taskId) {
  window.dispatchEvent(new CustomEvent(EVENTS.OPEN_TASK_DETAIL, { detail: { taskId } }))
}

export function emitCalendarDateChange(startDate, endDate) {
  window.dispatchEvent(new CustomEvent(EVENTS.CALENDAR_DATE_CHANGE, { detail: { startDate, endDate } }))
}

export function onRefreshData(handler) {
  window.addEventListener(EVENTS.REFRESH_DATA, handler)
  return () => window.removeEventListener(EVENTS.REFRESH_DATA, handler)
}

export function onCalendarDateChange(handler) {
  const listener = (e) => handler(e.detail.startDate, e.detail.endDate)
  window.addEventListener(EVENTS.CALENDAR_DATE_CHANGE, listener)
  return () => window.removeEventListener(EVENTS.CALENDAR_DATE_CHANGE, listener)
}
