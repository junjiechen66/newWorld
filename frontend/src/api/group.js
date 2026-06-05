import request from '@/utils/request'

export function getList() {
  return request.get('/groups')
}

export function getTree(projectId) {
  return request.get('/groups/tree', { params: { projectId } })
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
