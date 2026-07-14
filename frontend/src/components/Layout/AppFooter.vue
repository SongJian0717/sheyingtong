<template>
  <footer class="app-footer">
    <div class="footer-content">
      <div class="footer-section">
        <h4>摄影通</h4>
        <p>AI驱动的摄影学习平台，帮助你快速掌握摄影技巧</p>
      </div>
      <div class="footer-section">
        <h4>功能模块</h4>
        <ul>
          <li><router-link to="/dictionary">摄影词典</router-link></li>
          <li><router-link to="/photo-score">照片打分</router-link></li>
          <li><router-link to="/gear">器材选购</router-link></li>
          <li><router-link to="/chat">AI问答</router-link></li>
        </ul>
      </div>
      <div class="footer-section">
        <h4>关于我们</h4>
        <ul>
          <li><a href="#">联系我们</a></li>
          <li><a href="#">使用条款</a></li>
          <li><a href="#">隐私政策</a></li>
        </ul>
      </div>
    </div>
    <div class="footer-bottom">
      <p>&copy; 2026 摄影通. All rights reserved.</p>
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
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 30px;
  border-bottom: 1px solid #4a5568;
}

.footer-section {
  flex: 1;
}

.footer-section h4 {
  color: white;
  margin-bottom: 15px;
  font-size: 16px;
}

.footer-section p {
  font-size: 14px;
  line-height: 1.6;
}

.footer-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-section ul li {
  margin-bottom: 8px;
}

.footer-section ul li a,
.footer-section ul li router-link {
  color: #a0aec0;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s ease;
}

.footer-section ul li a:hover,
.footer-section ul li router-link:hover {
  color: white;
}

.footer-bottom {
  text-align: center;
  padding-top: 20px;
  font-size: 13px;
}
</style>