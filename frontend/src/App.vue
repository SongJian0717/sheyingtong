<template>
  <div class="app-container">
    <template v-if="isAuthLayout">
      <router-view class="auth-content" />
    </template>
    <template v-else>
      <AppHeader />
      <div class="main-content">
        <AppSidebar />
        <router-view class="page-content" />
      </div>
      <AppFooter />
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from './components/Layout/AppHeader.vue'
import AppSidebar from './components/Layout/AppSidebar.vue'
import AppFooter from './components/Layout/AppFooter.vue'
import { useUserStore } from './stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isAuthLayout = computed(() => {
  return (
    route.meta.layout === 'auth' || 
    route.name === 'login' || 
    route.name === 'register'
  )
})

// 打开项目时向后端校验本地 Token 的真实有效性
// 后端重启/密钥变化后，localStorage 里残留的旧 Token 已失效，若仍按"有 Token 即已登录"
// 显示，会出现"顶栏显示已登录，但调用需要登录的功能全部无权限"的错乱状态。
// 校验不通过 → 清空登录态，顶栏回到"登录/注册"的未登录展示。
onMounted(async () => {
  if (!userStore.token) return
  const valid = await userStore.validateSession()
  if (!valid) {
    userStore.logout()
    if (route.meta.requiresAuth) {
      router.push('/')
    }
  }
})
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  display: flex;
  flex: 1;
  padding-top: 60px;
}

.page-content {
  flex: 1;
  margin-left: 240px;
  padding: 30px;
  min-height: calc(100vh - 60px);
  background: #fff;
}

.auth-content {
  min-height: 100vh;
}
</style>
