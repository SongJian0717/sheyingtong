<template>
  <aside class="app-sidebar">
    <!-- 顶部：返回首页 -->
    <router-link to="/" class="nav-link home-link" :class="{ active: $route.name === 'home' }">
      <span class="nav-icon">🏠</span>
      <span class="nav-text">首页</span>
    </router-link>

    <!-- 按板块分组渲染 -->
    <div v-for="group in navGroups" :key="group.title" class="sidebar-section">
      <h3 class="section-title">{{ group.title }}</h3>
      <ul class="nav-list">
        <li v-for="item in group.items" :key="item.to">
          <router-link :to="item.to" class="nav-link" :class="{ active: isActive(item) }">
            <span class="nav-icon">{{ item.icon }}</span>
            <span class="nav-text">{{ item.label }}</span>
          </router-link>
        </li>
      </ul>
    </div>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

// names：该入口需要点亮高亮的全部路由名（含二级详情页）
const navGroups = [
  {
    title: 'AI 智能工具',
    items: [
      { to: '/photo-score', label: '照片打分', icon: '📷', names: ['photo-score'] },
      { to: '/retouch', label: '修图建议', icon: '🎨', names: ['retouch'] },
      { to: '/chat', label: 'AI 问答', icon: '💬', names: ['chat'] },
      { to: '/gear', label: '器材选购', icon: '🎥', names: ['gear', 'gear-detail'] }
    ]
  },
  {
    title: '学习资源',
    items: [
      { to: '/dictionary', label: '摄影词典', icon: '📚', names: ['dictionary', 'term-detail'] },
      { to: '/tutorials', label: '入门教程', icon: '📖', names: ['tutorials'] },
      { to: '/gallery', label: '作品赏析', icon: '🏆', names: ['gallery'] }
    ]
  },
  {
    title: '摄影社区',
    items: [
      {
        to: '/community',
        label: '作品广场',
        icon: '🖼️',
        names: ['community', 'work-detail', 'user-profile']
      },
      { to: '/feed', label: '关注动态', icon: '🔔', names: ['feed'] }
    ]
  }
]

const isActive = (item) => route.name && item.names.includes(route.name)
</script>

<style scoped>
.app-sidebar {
  width: 240px;
  background: #f8f9fa;
  padding: 20px;
  height: calc(100vh - 60px);
  position: fixed;
  left: 0;
  top: 60px;
  overflow-y: auto;
}

.sidebar-section {
  margin-bottom: 26px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #888;
  margin-bottom: 12px;
  padding-left: 10px;
  border-left: 3px solid #667eea;
  letter-spacing: 1px;
}

.nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-list li {
  margin-bottom: 5px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 15px;
  border-radius: 8px;
  color: #333;
  text-decoration: none;
  transition: all 0.3s ease;
}

.nav-link:hover {
  background: #e8f0fe;
  color: #667eea;
}

.nav-link.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 10px rgba(102, 126, 234, 0.3);
}

.nav-icon {
  font-size: 18px;
  width: 22px;
  text-align: center;
}

.nav-text {
  font-size: 14px;
}

.home-link {
  font-weight: 600;
  background: #fff;
  border: 1px solid #eef0f6;
  margin-bottom: 20px;
}

.home-link.active {
  border-color: transparent;
}
</style>
