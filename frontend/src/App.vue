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
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/Layout/AppHeader.vue'
import AppSidebar from './components/Layout/AppSidebar.vue'
import AppFooter from './components/Layout/AppFooter.vue'

const route = useRoute()

const isAuthLayout = computed(() => {
  return (
    route.meta.layout === 'auth' || 
    route.name === 'login' || 
    route.name === 'register'
  )
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
