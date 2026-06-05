import request from '@/utils/request'

export function getTagList() {
  return request.get('/tags')
}

export function createTag(data) {
  return request.post('/tags', data)
}

export function deleteTag(id) {
  return request.delete('/tags/' + id)
}
