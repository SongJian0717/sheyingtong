<template>
  <footer class="app-footer">
    <div class="footer-content">
      <div class="footer-brand">
        <h4 class="footer-logo">📸 摄影通</h4>
        <p>AI 智能工具与摄影学习社区一体化平台，助你从按下快门到分享作品的每一步。</p>
      </div>

      <div class="footer-section">
        <h4>AI 智能工具</h4>
        <ul>
          <li><router-link to="/photo-score">照片打分</router-link></li>
          <li><router-link to="/retouch">修图建议</router-link></li>
          <li><router-link to="/chat">AI 问答</router-link></li>
          <li><router-link to="/gear">器材选购</router-link></li>
        </ul>
      </div>

      <div class="footer-section">
        <h4>学习资源</h4>
        <ul>
          <li><router-link to="/dictionary">摄影词典</router-link></li>
          <li><router-link to="/tutorials">入门教程</router-link></li>
          <li><router-link to="/gallery">作品赏析</router-link></li>
        </ul>
      </div>

      <div class="footer-section">
        <h4>摄影社区</h4>
        <ul>
          <li><router-link to="/community">作品广场</router-link></li>
          <li><router-link to="/feed">关注动态</router-link></li>
        </ul>
      </div>
    </div>
    <div class="footer-bottom">
      <p>© 2026 摄影通 · AI + 社区一体化摄影学习平台</p>
    </div>
  </footer>
</template>

<script setup>
</script>

<style scoped>
.app-footer {
  background: #2d3748;
  color: #a0aec0;
  padding: 40px 30px 20px;
  margin-top: auto;
}

.footer-content {
  display: flex;
  justify-content: space-around;
  gap: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 30px;
  border-bottom: 1px solid #4a5568;
}

.footer-brand {
  flex: 1.4;
  max-width: 340px;
}

.footer-section {
  flex: 1;
}

.footer-logo {
  font-size: 20px;
}

.footer-section h4,
.footer-logo {
  color: white;
  margin-bottom: 15px;
}

.footer-brand p {
  font-size: 14px;
  line-height: 1.8;
}

.footer-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-section ul li {
  margin-bottom: 8px;
}

.footer-section ul li a {
  color: #a0aec0;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s ease;
}

.footer-section ul li a:hover {
  color: white;
}

.footer-bottom {
  text-align: center;
  padding-top: 20px;
  font-size: 13px;
}

@media (max-width: 768px) {
  .footer-content {
    flex-direction: column;
  }
}
</style>
