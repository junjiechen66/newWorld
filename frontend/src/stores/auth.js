import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, register, getUserInfo } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(null)

  const isLoggedIn = () => !!token.value

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const doLogin = async (loginData) => {
    const res = await login(loginData)
    setToken(res.data.token)
    await fetchUserInfo()
    return res
  }

  const doRegister = async (registerData) => {
    return await register(registerData)
  }

  const fetchUserInfo = async () => {
    const res = await getUserInfo()
    user.value = res.data
    return res
  }

  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
  }

  return { token, user, isLoggedIn, doLogin, doRegister, fetchUserInfo, logout, setToken }
})
