import request from '@/utils/request'

export function getTaskList(params) {
  return request.get('/tasks', { params })
}

export function getTaskById(id) {
  return request.get('/tasks/' + id)
}

export function createTask(data) {
  return request.post('/tasks', data)
}

export function updateTask(id, data) {
  return request.put('/tasks/' + id, data)
}

export function deleteTask(id) {
  return request.delete('/tasks/' + id)
}

export function updateTaskStatus(id, status) {
  return request.put('/tasks/' + id + '/status', { status })
}

export function updateTaskPriority(id, priority) {
  return request.put('/tasks/' + id + '/priority', { priority })
}

export function duplicateTask(id) {
  return request.put('/tasks/' + id + '/duplicate')
}

export function archiveTask(id) {
  return request.put('/tasks/' + id + '/archive')
}

export function convertToNote(id) {
  return request.put('/tasks/' + id + '/convert-note')
}

export function getShareLink(id) {
  return request.get('/tasks/' + id + '/share-link')
}

export function searchTasks(keyword) {
  return request.get('/tasks/search', { params: { keyword } })
}

export function getTaskStatistics() {
  return request.get('/tasks/statistics')
}
