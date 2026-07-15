<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">📸 摄影通</h1>
        <p class="hero-subtitle">AI驱动的摄影学习平台</p>
        <p class="hero-description">从入门到精通，一站式掌握摄影技巧。智能分析、专业建议、器材推荐，让你的摄影之路更加顺畅。</p>
        <div class="hero-buttons">
          <el-button type="primary" size="large" @click="$router.push('/dictionary')">开始学习</el-button>
          <el-button size="large" @click="$router.push('/photo-score')">上传照片</el-button>
        </div>
      </div>
      <div class="hero-stats">
        <div class="stat-item">
          <span class="stat-number">500+</span>
          <span class="stat-label">摄影术语</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">10000+</span>
          <span class="stat-label">照片分析</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">200+</span>
          <span class="stat-label">器材推荐</span>
        </div>
      </div>
    </section>
    
    <section class="features-section">
      <h2 class="section-title">核心功能</h2>
      <div class="features-grid">
        <div class="feature-card" v-for="feature in features" :key="feature.name" @click="$router.push(feature.path)">
          <div class="feature-icon">{{ feature.icon }}</div>
          <h3 class="feature-name">{{ feature.name }}</h3>
          <p class="feature-description">{{ feature.description }}</p>
          <el-button type="text" class="feature-btn">了解更多 →</el-button>
        </div>
      </div>
    </section>
    
    <section class="quick-start-section">
      <div class="quick-start-content">
        <div class="quick-start-text">
          <h2 class="section-title">快速开始</h2>
          <p class="quick-start-description">注册成为会员，解锁更多功能：保存学习记录、获取个性化推荐、参与摄影社区交流。</p>
          <div class="quick-start-buttons">
            <el-button type="primary" size="large" @click="$router.push('/register')">免费注册</el-button>
            <el-button size="large" @click="$router.push('/login')">登录</el-button>
          </div>
        </div>
        <div class="quick-start-steps">
          <div class="step-item">
            <span class="step-number">1</span>
            <span class="step-text">注册账号</span>
          </div>
          <div class="step-item">
            <span class="step-number">2</span>
            <span class="step-text">学习术语</span>
          </div>
          <div class="step-item">
            <span class="step-number">3</span>
            <span class="step-text">上传照片</span>
          </div>
          <div class="step-item">
            <span class="step-number">4</span>
            <span class="step-text">获得建议</span>
          </div>
        </div>
      </div>
    </section>
    

  </div>
</template>

<script setup>
const features = [
  {
    name: '摄影词典',
    icon: '📚',
    description: '收录500+摄影术语，智能检索，专业解释，助你快速掌握摄影知识。',
    path: '/dictionary'
  },
  {
    name: '照片打分',
    icon: '📷',
    description: '上传照片，AI智能分析构图、光线、色彩，给出专业评分和改进建议。',
    path: '/photo-score'
  },
  {
    name: '器材选购',
    icon: '🎥',
    description: '输入预算和需求，智能推荐最适合你的相机、镜头等摄影器材。',
    path: '/gear'
  },
  {
    name: 'AI问答',
    icon: '💬',
    description: '专业摄影AI助手，解答摄影术语、器材选择、曝光计算等问题。',
    path: '/chat'
  },
  {
    name: '修图建议',
    icon: '🎨',
    description: '上传照片，获得专业修图步骤和参数建议，提升后期处理能力。',
    path: '/retouch'
  }
]
</script>

<style scoped>
.home-page {
  padding-bottom: 30px;
}

.hero-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 80px 30px;
  border-radius: 0 0 30px 30px;
  margin: -30px -30px 30px;
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
}

.hero-title {
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 10px;
}

.hero-subtitle {
  font-size: 24px;
  margin-bottom: 20px;
  opacity: 0.9;
}

.hero-description {
  font-size: 18px;
  line-height: 1.6;
  margin-bottom: 30px;
  opacity: 0.95;
}

.hero-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.hero-stats {
  display: flex;
  justify-content: center;
  gap: 80px;
  margin-top: 50px;
  padding-top: 30px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 32px;
  font-weight: bold;
}

.stat-label {
  font-size: 14px;
  opacity: 0.8;
}

.section-title {
  font-size: 28px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.features-section {
  padding: 30px 0;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 25px;
}

.feature-card {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.feature-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.feature-name {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.feature-description {
  font-size: 14px;
  line-height: 1.6;
  color: #666;
  margin-bottom: 15px;
}

.feature-btn {
  color: #667eea;
  font-weight: 500;
}

.quick-start-section {
  background: #f8f9fa;
  border-radius: 15px;
  padding: 40px;
  margin: 30px 0;
}

.quick-start-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 40px;
}

.quick-start-text {
  flex: 1;
}

.quick-start-description {
  font-size: 16px;
  line-height: 1.6;
  color: #666;
  margin-bottom: 20px;
}

.quick-start-buttons {
  display: flex;
  gap: 15px;
}

.quick-start-steps {
  display: flex;
  gap: 30px;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.step-number {
  width: 40px;
  height: 40px;
  background: #667eea;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-bottom: 10px;
}

.step-text {
  font-size: 14px;
  color: #666;
}

@media (max-width: 768px) {
  .hero-stats {
    gap: 30px;
  }
  
  .quick-start-content {
    flex-direction: column;
  }
  
  .quick-start-steps {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>