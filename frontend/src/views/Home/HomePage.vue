<template>
  <div class="home-page">
    <!-- Hero -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">📸 摄影通</h1>
        <p class="hero-subtitle">AI 智能工具 + 摄影学习社区，一站式成长</p>
        <p class="hero-description">
          用 AI 给你的照片打分、给器材出主意、陪你聊摄影，再把满意的作品分享到社区，
          在同好的点赞与评论中持续精进。
        </p>
        <div class="hero-buttons">
          <el-button type="primary" size="large" @click="$router.push('/dictionary')">开始学习</el-button>
          <el-button plain size="large" @click="$router.push('/photo-score')">上传照片打分</el-button>
          <el-button plain size="large" @click="$router.push('/community')">逛逛作品广场</el-button>
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

    <!-- AI 智能工具 -->
    <section class="features-section">
      <h2 class="section-title">AI 智能工具</h2>
      <p class="section-subtitle">把 AI 请进你的创作流程</p>
      <div class="features-grid">
        <div class="feature-card" v-for="feature in aiTools" :key="feature.name" @click="$router.push(feature.path)">
          <div class="feature-icon">{{ feature.icon }}</div>
          <h3 class="feature-name">{{ feature.name }}</h3>
          <p class="feature-description">{{ feature.description }}</p>
          <el-button type="text" class="feature-btn">了解更多 →</el-button>
        </div>
      </div>
    </section>

    <!-- 学习资源 -->
    <section class="features-section learning-section">
      <h2 class="section-title">学习资源</h2>
      <p class="section-subtitle">从术语到实战，系统补齐摄影知识</p>
      <div class="features-grid">
        <div
          class="feature-card learning-card"
          v-for="feature in learningResources"
          :key="feature.name"
          @click="$router.push(feature.path)"
        >
          <div class="feature-icon">{{ feature.icon }}</div>
          <h3 class="feature-name">{{ feature.name }}</h3>
          <p class="feature-description">{{ feature.description }}</p>
          <el-button type="text" class="feature-btn">去看看 →</el-button>
        </div>
      </div>
    </section>

    <!-- 摄影社区 CTA -->
    <section class="community-section">
      <div class="community-band">
        <div class="community-text">
          <h2 class="community-title">🖼️ 摄影社区 · 作品广场</h2>
          <p class="community-desc">
            发布你的打分作品，收获 AI 评语之外的真实反馈；点赞、评论、关注，
            让每一次快门都有人共鸣。
          </p>
          <ul class="community-points">
            <li>✅ 一键发布：照片打分后可直接分享到广场</li>
            <li>✅ 互动交流：点赞、评论楼与回复、关注作者</li>
            <li>✅ 关注动态：不错过喜欢摄影师的每一次上新</li>
          </ul>
          <div class="community-buttons">
            <el-button type="primary" size="large" @click="$router.push('/community')">进入作品广场</el-button>
            <el-button size="large" plain @click="$router.push('/feed')">查看关注动态</el-button>
          </div>
        </div>
        <div class="community-emoji" aria-hidden="true">
          <span class="ce-1">🖼️</span>
          <span class="ce-2">💬</span>
          <span class="ce-3">❤️</span>
          <span class="ce-4">👥</span>
        </div>
      </div>
    </section>

    <!-- 快速开始 -->
    <section class="quick-start-section">
      <div class="quick-start-content">
        <div class="quick-start-text">
          <h2 class="section-title">快速开始</h2>
          <p class="quick-start-description">
            注册成为会员，体验完整的 AI 分析与社区之旅：从上传第一张照片，
            到收获第一份 AI 点评、发布第一件作品。
          </p>
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
            <span class="step-text">照片 AI 打分</span>
          </div>
          <div class="step-item">
            <span class="step-number">3</span>
            <span class="step-text">获取优化建议</span>
          </div>
          <div class="step-item">
            <span class="step-number">4</span>
            <span class="step-text">发布作品进社区</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
const aiTools = [
  {
    name: '照片打分',
    icon: '📷',
    description: '上传照片，AI 从构图、光线、色彩、对焦、内容五个维度专业评分并给出改进建议。',
    path: '/photo-score'
  },
  {
    name: '修图建议',
    icon: '🎨',
    description: '把后期中的照片交给 AI，获得一步步可执行的调色与修图参数建议。',
    path: '/retouch'
  },
  {
    name: 'AI 问答',
    icon: '💬',
    description: '随问随答的摄影助理，覆盖曝光、用光、参数设置与创作思路。',
    path: '/chat'
  },
  {
    name: '器材选购',
    icon: '🎥',
    description: '输入预算与拍摄需求，智能匹配适合你的相机与镜头方案。',
    path: '/gear'
  }
]

const learningResources = [
  {
    name: '摄影词典',
    icon: '📚',
    description: '收录 500+ 摄影术语，智能检索、专业解释，随时查阅。',
    path: '/dictionary'
  },
  {
    name: '入门教程',
    icon: '📖',
    description: '从相机操作到构图用光，循序渐进的系统入门课程。',
    path: '/tutorials'
  },
  {
    name: '作品赏析',
    icon: '🏆',
    description: '赏析经典照片的构图与技法，把好照片拆开看明白。',
    path: '/gallery'
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
  padding: 70px 30px 40px;
  border-radius: 0 0 30px 30px;
  margin: -30px -30px 30px;
}

.hero-content {
  max-width: 860px;
  margin: 0 auto;
  text-align: center;
}

.hero-title {
  font-size: 44px;
  font-weight: bold;
  margin-bottom: 10px;
}

.hero-subtitle {
  font-size: 22px;
  margin-bottom: 18px;
  opacity: 0.95;
}

.hero-description {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 30px;
  opacity: 0.92;
}

.hero-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
  flex-wrap: wrap;
}

.hero-buttons :deep(.el-button) {
  height: 46px;
  padding: 0 30px;
  border: none;
  border-radius: 24px;
  background: #fff;
  color: #667eea;
  font-size: 16px;
  box-shadow: 0 6px 18px rgba(20, 26, 70, 0.18);
  transition: all 0.25s ease;
}

.hero-buttons :deep(.el-button--primary) {
  font-weight: 700;
}

.hero-buttons :deep(.el-button:hover) {
  background: #f3f5ff;
  color: #5569d8;
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(20, 26, 70, 0.24);
}

.hero-stats {
  display: flex;
  justify-content: center;
  gap: 80px;
  margin-top: 44px;
  padding-top: 26px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 30px;
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
  margin: 0 0 8px;
  color: #333;
}

.section-subtitle {
  text-align: center;
  color: #999;
  font-size: 14px;
  margin: 0 0 30px;
}

.features-section {
  padding: 20px 0 34px;
}

.learning-section {
  padding-top: 10px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 22px;
}

.feature-card {
  background: white;
  border-radius: 15px;
  padding: 28px 26px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid transparent;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px rgba(102, 126, 234, 0.16);
  border-color: #e0e5ff;
}

.learning-card {
  background: linear-gradient(180deg, #fafbff 0%, #fff 100%);
}

.feature-icon {
  font-size: 40px;
  margin-bottom: 14px;
}

.feature-name {
  font-size: 19px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.feature-description {
  font-size: 14px;
  line-height: 1.7;
  color: #666;
  margin-bottom: 14px;
}

.feature-btn {
  color: #667eea;
  font-weight: 500;
}

/* 摄影社区横幅 */
.community-section {
  padding: 10px 0 30px;
}

.community-band {
  background: linear-gradient(120deg, #1e2a4a 0%, #3b3f8f 55%, #764ba2 100%);
  border-radius: 18px;
  padding: 40px 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  color: white;
  box-shadow: 0 10px 34px rgba(59, 63, 143, 0.28);
  overflow: hidden;
  position: relative;
}

.community-text {
  flex: 1;
  min-width: 0;
}

.community-title {
  font-size: 26px;
  margin: 0 0 14px;
}

.community-desc {
  font-size: 15px;
  line-height: 1.8;
  opacity: 0.92;
  margin: 0 0 18px;
}

.community-points {
  list-style: none;
  padding: 0;
  margin: 0 0 24px;
}

.community-points li {
  font-size: 14px;
  line-height: 2;
  opacity: 0.95;
}

.community-buttons :deep(.el-button) {
  height: 44px;
  padding: 0 26px;
  border: none;
  border-radius: 24px;
  background: #fff;
  color: #4a4f9e;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.25s ease;
}

.community-buttons :deep(.el-button:hover) {
  background: #eef1ff;
  transform: translateY(-2px);
}

.community-emoji {
  position: relative;
  width: 220px;
  height: 180px;
  flex-shrink: 0;
}

.community-emoji span {
  position: absolute;
  font-size: 56px;
  filter: drop-shadow(0 6px 12px rgba(0, 0, 0, 0.2));
}

.community-emoji .ce-1 {
  top: 8px;
  left: 10px;
  font-size: 68px;
}

.community-emoji .ce-2 {
  top: 90px;
  right: 6px;
  font-size: 44px;
}

.community-emoji .ce-3 {
  top: 30px;
  right: 40px;
  font-size: 36px;
}

.community-emoji .ce-4 {
  bottom: 4px;
  left: 60px;
  font-size: 42px;
}

/* 快速开始 */
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
  line-height: 1.8;
  color: #666;
  margin-bottom: 20px;
}

.quick-start-buttons {
  display: flex;
  gap: 15px;
}

.quick-start-steps {
  display: flex;
  gap: 22px;
  flex-wrap: wrap;
  justify-content: center;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 84px;
}

.step-number {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-bottom: 10px;
}

.step-text {
  font-size: 13px;
  color: #555;
  text-align: center;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .hero-stats {
    gap: 26px;
  }

  .hero-title {
    font-size: 32px;
  }

  .community-band {
    flex-direction: column;
    padding: 30px 24px;
    text-align: center;
  }

  .community-emoji {
    display: none;
  }

  .quick-start-content {
    flex-direction: column;
  }
}
</style>
