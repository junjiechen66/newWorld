import request from '@/utils/request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function getUserInfo() {
  return request.get('/auth/user-info')
}

export function updateUserInfo(data) {
  return request.put('/auth/user-info', data)
}

export function changePassword(data) {
  return request.put('/auth/password', data)
}
