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

export function getTaskStatistics() {
  return request.get('/tasks/statistics')
}

export function getTodayTasks() {
  return request.get('/tasks/today')
}

export function noteArchiveTask(id) {
  return request.put('/tasks/' + id + '/note-archive')
}

export function noteUnarchiveTask(id) {
  return request.put('/tasks/' + id + '/note-unarchive')
}
