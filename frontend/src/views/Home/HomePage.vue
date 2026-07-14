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
    
    <section class="articles-section">
      <div class="section-header">
        <h2 class="section-title">摄影技巧</h2>
        <el-button type="text" class="view-all-btn">查看全部 →</el-button>
      </div>
      <div class="articles-grid">
        <div class="article-card" v-for="article in articles" :key="article.id">
          <div class="article-image">
            <img :src="article.image" :alt="article.title" />
          </div>
          <div class="article-content">
            <span class="article-category">{{ article.category }}</span>
            <h3 class="article-title">{{ article.title }}</h3>
            <p class="article-summary">{{ article.summary }}</p>
            <div class="article-meta">
              <span class="article-date">{{ article.date }}</span>
              <span class="article-read">阅读 {{ article.readCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
    
    <section class="testimonials-section">
      <h2 class="section-title">用户评价</h2>
      <div class="testimonials-carousel">
        <div class="testimonial-card" v-for="testimonial in testimonials" :key="testimonial.id">
          <div class="testimonial-content">
            <p class="testimonial-text">"{{ testimonial.text }}"</p>
            <div class="testimonial-author">
              <span class="author-name">{{ testimonial.author }}</span>
              <span class="author-title">{{ testimonial.title }}</span>
            </div>
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

const articles = [
  {
    id: 1,
    title: '新手必学：光圈、快门、ISO的关系',
    summary: '曝光三要素是摄影的基础，掌握它们之间的关系，让你拍出更精彩的照片。',
    category: '基础入门',
    date: '2026-07-01',
    readCount: 3200,
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=photography%20camera%20settings%20aperture%20shutter%20iso%20diagram&image_size=landscape_4_3'
  },
  {
    id: 2,
    title: '人像摄影构图技巧大全',
    summary: '从三分法到引导线，掌握这些构图技巧，让你的人像作品更加出色。',
    category: '人像摄影',
    date: '2026-06-28',
    readCount: 2800,
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=portrait%20photography%20composition%20rules%20beautiful%20woman&image_size=landscape_4_3'
  },
  {
    id: 3,
    title: '风光摄影必备技巧',
    summary: '拍摄壮丽风光的秘诀，包括器材选择、光线把握和构图原则。',
    category: '风光摄影',
    date: '2026-06-25',
    readCount: 2500,
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=landscape%20photography%20beautiful%20mountain%20sunset%20professional&image_size=landscape_4_3'
  },
  {
    id: 4,
    title: '如何选择适合自己的镜头',
    summary: '从定焦到变焦，从广角到长焦，帮你理清镜头选择的思路。',
    category: '器材指南',
    date: '2026-06-22',
    readCount: 1900,
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=photography%20lenses%20collection%20camera%20equipment%20professional&image_size=landscape_4_3'
  }
]

const testimonials = [
  {
    id: 1,
    text: '摄影通帮我从零开始学习摄影，照片打分功能让我知道自己哪里需要改进，进步很快！',
    author: '小明同学',
    title: '摄影新手'
  },
  {
    id: 2,
    text: '器材推荐功能太实用了，帮我在预算范围内选到了最适合的相机，省去了很多研究时间。',
    author: '阿杰',
    title: '业余爱好者'
  },
  {
    id: 3,
    text: '作为摄影专业学生，摄影词典是我学习的好帮手，术语解释准确又易懂。',
    author: '张同学',
    title: '摄影专业学生'
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

.articles-section {
  padding: 30px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.view-all-btn {
  color: #667eea;
  font-weight: 500;
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 25px;
}

.article-card {
  background: white;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
}

.article-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.article-image {
  height: 200px;
  overflow: hidden;
}

.article-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.article-card:hover .article-image img {
  transform: scale(1.05);
}

.article-content {
  padding: 20px;
}

.article-category {
  display: inline-block;
  padding: 4px 12px;
  background: #e8f0fe;
  color: #667eea;
  border-radius: 15px;
  font-size: 12px;
  margin-bottom: 10px;
}

.article-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
  line-height: 1.4;
}

.article-summary {
  font-size: 14px;
  line-height: 1.6;
  color: #666;
  margin-bottom: 15px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #999;
}

.testimonials-section {
  padding: 30px 0;
}

.testimonials-carousel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 25px;
}

.testimonial-card {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.testimonial-text {
  font-size: 16px;
  line-height: 1.6;
  color: #666;
  margin-bottom: 20px;
  font-style: italic;
}

.testimonial-author {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-weight: bold;
  color: #333;
}

.author-title {
  font-size: 13px;
  color: #999;
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