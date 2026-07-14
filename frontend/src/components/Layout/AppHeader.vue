<template>
  <header class="app-header">
    <div class="header-left">
      <div class="logo" @click="$router.push('/')">
        <span class="logo-icon">📸</span>
        <span class="logo-text">摄影通</span>
      </div>
    </div>
    
    <nav class="header-nav">
      <router-link to="/" class="nav-item" :class="{ active: $route.name === 'home' }">首页</router-link>
      <router-link to="/dictionary" class="nav-item" :class="{ active: $route.name === 'dictionary' }">摄影词典</router-link>
      <router-link to="/photo-score" class="nav-item" :class="{ active: $route.name === 'photo-score' }">照片打分</router-link>
      <router-link to="/gear" class="nav-item" :class="{ active: $route.name === 'gear' }">器材选购</router-link>
      <router-link to="/chat" class="nav-item" :class="{ active: $route.name === 'chat' }">AI问答</router-link>
      <router-link to="/retouch" class="nav-item" :class="{ active: $route.name === 'retouch' }">修图建议</router-link>
    </nav>
    
    <div class="header-right">
      <div v-if="isLoggedIn" class="user-info">
        <router-link to="/profile" class="nav-item profile-link">
          <span class="avatar-icon">👤</span>
          <span>{{ userStore.userInfo.nickname || userStore.userInfo.username }}</span>
        </router-link>
        <el-button type="text" @click="handleLogout">退出</el-button>
      </div>
      <div v-else class="auth-buttons">
        <el-button type="text" @click="$router.push('/login')">登录</el-button>
        <el-button type="primary" @click="$router.push('/register')">注册</el-button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)

const handleLogout = () => {
  userStore.logout()
  window.location.reload()
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.header-left {
  flex: 1;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: 22px;
  font-weight: bold;
}

.header-nav {
  flex: 2;
  display: flex;
  justify-content: center;
  gap: 30px;
}

.nav-item {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  font-size: 16px;
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.nav-item:hover {
  color: white;
  background: rgba(255, 255, 255, 0.1);
}

.nav-item.active {
  color: white;
  background: rgba(255, 255, 255, 0.2);
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.username {
  font-size: 16px;
  font-weight: 500;
}

.auth-buttons {
  display: flex;
  gap: 10px;
}

.auth-buttons :deep(.el-button) {
  color: white;
}

.auth-buttons :deep(.el-button--primary) {
  background: white;
  color: #667eea;
  border: none;
}
</style>