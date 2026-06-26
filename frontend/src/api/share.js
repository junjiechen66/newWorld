import request from '@/utils/request'

// ========== 共享成员 ==========

export function getShareMembers(resourceType, resourceId) {
  return request.get('/shares', { params: { resourceType, resourceId } })
}

export function addShareMember(data) {
  return request.post('/shares', data)
}

export function updateShareMember(id, permission) {
  return request.put('/shares/' + id, { permission })
}

export function removeShareMember(id) {
  return request.delete('/shares/' + id)
}

// ========== 分享链接 ==========

export function getShareLinks(resourceType, resourceId) {
  return request.get('/shares/links', { params: { resourceType, resourceId } })
}

export function createShareLink(data) {
  return request.post('/shares/links', data)
}

export function updateShareLink(id, data) {
  return request.put('/shares/links/' + id, data)
}

export function disableShareLink(id) {
  return request.delete('/shares/links/' + id)
}

// ========== 辅助 ==========

export function searchShareUsers(keyword) {
  return request.get('/shares/search-users', { params: { keyword } })
}

export function getSharedWithMe() {
  return request.get('/shares/shared-with-me')
}

// ========== 公开分享（不需要登录） ==========

export function getPublicSharedResource(shareCode) {
  return request.get('/public/share/' + shareCode)
}

export function updatePublicSharedResource(shareCode, data) {
  return request.put('/public/share/' + shareCode, data)
}
