import request from '@/utils/request'

export function exportTasks(params) {
  return request.get('/export/tasks', { params, responseType: 'blob' })
}
