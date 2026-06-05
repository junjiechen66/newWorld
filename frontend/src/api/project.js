import request from '@/utils/request'

export function getProjectList(groupId) {
  return request.get('/projects', { params: { groupId } })
}

export function createProject(data) {
  return request.post('/projects', data)
}

export function updateProject(id, data) {
  return request.put('/projects/' + id, data)
}

export function deleteProject(id) {
  return request.delete('/projects/' + id)
}
