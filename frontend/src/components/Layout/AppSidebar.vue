<template>
  <aside class="app-sidebar">
    <div class="sidebar-section">
      <h3 class="section-title">快速导航</h3>
      <ul class="nav-list">
        <li>
          <router-link to="/" class="nav-link" :class="{ active: $route.name === 'home' }">
            <span class="nav-icon">🏠</span>
            <span class="nav-text">首页</span>
          </router-link>
        </li>
        <li>
          <router-link to="/dictionary" class="nav-link" :class="{ active: $route.name === 'dictionary' }">
            <span class="nav-icon">📚</span>
            <span class="nav-text">摄影词典</span>
          </router-link>
        </li>
        <li>
          <router-link to="/photo-score" class="nav-link" :class="{ active: $route.name === 'photo-score' }">
            <span class="nav-icon">📷</span>
            <span class="nav-text">照片打分</span>
          </router-link>
        </li>
        <li>
          <router-link to="/gear" class="nav-link" :class="{ active: $route.name === 'gear' }">
            <span class="nav-icon">🎥</span>
            <span class="nav-text">器材选购</span>
          </router-link>
        </li>
        <li>
          <router-link to="/chat" class="nav-link" :class="{ active: $route.name === 'chat' }">
            <span class="nav-icon">💬</span>
            <span class="nav-text">AI问答</span>
          </router-link>
        </li>
        <li>
          <router-link to="/retouch" class="nav-link" :class="{ active: $route.name === 'retouch' }">
            <span class="nav-icon">🎨</span>
            <span class="nav-text">修图建议</span>
          </router-link>
        </li>
      </ul>
    </div>
    
    <div class="sidebar-section">
      <h3 class="section-title">学习资源</h3>
      <ul class="nav-list">
        <li>
          <router-link to="/tutorials" class="nav-link" :class="{ active: $route.name === 'tutorials' }">
            <span class="nav-icon">📖</span>
            <span class="nav-text">入门教程</span>
          </router-link>
        </li>
        <li>
          <router-link to="/tips" class="nav-link" :class="{ active: $route.name === 'tips' }">
            <span class="nav-icon">📝</span>
            <span class="nav-text">摄影技巧</span>
          </router-link>
        </li>
        <li>
          <router-link to="/gallery" class="nav-link" :class="{ active: $route.name === 'gallery' }">
            <span class="nav-icon">🏆</span>
            <span class="nav-text">作品赏析</span>
          </router-link>
        </li>
      </ul>
    </div>
    
  </aside>
</template>

<script setup>
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
  margin-bottom: 30px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #666;
  margin-bottom: 15px;
  padding-left: 10px;
  border-left: 3px solid #667eea;
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
  background: #667eea;
  color: white;
}

.nav-icon {
  font-size: 18px;
}

.nav-text {
  font-size: 14px;
}

</style>