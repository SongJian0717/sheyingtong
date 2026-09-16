import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { validateStoredToken } from '../utils/request'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  const login = (newToken, info) => {
    token.value = newToken
    userInfo.value = info
    localStorage.setItem('token', newToken)
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('user')   // 兼容历史版本遗留的旧键名
  }

  const updateUser = (info) => {
    userInfo.value = { ...userInfo.value, ...info }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  /**
   * 打开项目时静默校验本地 Token 是否真实有效
   * 后端重启导致 JWT 密钥变化后旧 Token 会失效；有效 → 用最新用户信息刷新 store；
   * 无效 → 返回 false，由调用方（App.vue）清空登录态，顶栏显示"未登录"。
   */
  const validateSession = async () => {
    const user = await validateStoredToken()
    if (user) {
      userInfo.value = user
      localStorage.setItem('userInfo', JSON.stringify(user))
      return true
    }
    return false
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    logout,
    updateUser,
    validateSession
  }
})