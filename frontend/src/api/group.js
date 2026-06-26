import request from '@/utils/request'

export function getList() {
  return request.get('/groups')
}

export function getTree(projectId, startDate, endDate) {
  const params = {}
  if (projectId) params.projectId = projectId
  if (startDate) params.startDate = startDate
  if (endDate) params.endDate = endDate
  return request.get('/groups/tree', { params })
}

export function createGroup(data) {
  return request.post('/groups', data)
}

export function updateGroup(id, data) {
  return request.put('/groups/' + id, data)
}

export function deleteGroup(id) {
  return request.delete('/groups/' + id)
}
