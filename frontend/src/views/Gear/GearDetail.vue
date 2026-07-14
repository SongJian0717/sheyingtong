<template>
  <div class="gear-detail-page">
    <div class="detail-header">
      <el-button @click="goBack">← 返回</el-button>
    </div>
    
    <div v-if="gear" class="detail-content">
      <div class="main-section">
        <div class="image-section">
          <div class="main-image">
            <img :src="gear.imageUrl" :alt="gear.model" />
          </div>
          <div class="thumbnail-list">
            <div v-for="(thumb, index) in thumbnails" :key="index" class="thumbnail-item" :class="{ active: index === 0 }">
              <img :src="thumb" :alt="`${gear.model} ${index + 1}`" />
            </div>
          </div>
        </div>
        
        <div class="info-section">
          <div class="gear-brand">{{ gear.brand }}</div>
          <h1 class="gear-model">{{ gear.model }}</h1>
          <div class="gear-price">¥{{ formatPrice(gear.price) }}</div>
          <div class="gear-rating">
            <el-rate :value="gear.rating" disabled size="large" />
            <span class="rating-count">({{ gear.reviewCount }} 条评价)</span>
          </div>
          
          <div class="gear-description">{{ gear.description }}</div>
          
          <div class="specs-brief">
            <div class="spec-item">
              <span class="spec-icon">📷</span>
              <span class="spec-label">像素</span>
              <span class="spec-value">{{ gear.pixel }}</span>
            </div>
            <div class="spec-item">
              <span class="spec-icon">⚡</span>
              <span class="spec-label">连拍</span>
              <span class="spec-value">{{ gear.continuousShooting }}</span>
            </div>
            <div class="spec-item">
              <span class="spec-icon">🎬</span>
              <span class="spec-label">视频</span>
              <span class="spec-value">{{ gear.video }}</span>
            </div>
            <div class="spec-item">
              <span class="spec-icon">🎯</span>
              <span class="spec-label">对焦</span>
              <span class="spec-value">{{ gear.focus }}</span>
            </div>
          </div>
          
          <div class="actions">
            <el-button type="primary" size="large" icon="el-icon-shopping-cart">加入购物车</el-button>
            <el-button size="large" icon="el-icon-share">分享</el-button>
          </div>
        </div>
      </div>
      
      <div class="tabs-section">
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="详细参数" name="specs">
            <div class="specs-table">
              <div v-for="(spec, index) in gear.specifications" :key="index" class="spec-row">
                <div class="spec-name">{{ spec.name }}</div>
                <div class="spec-desc">{{ spec.description }}</div>
              </div>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="AI评测" name="review">
            <div class="ai-review">
              <div class="review-score">
                <div class="score-circle">
                  <span class="score-value">{{ gear.aiScore }}</span>
                  <span class="score-label">AI评分</span>
                </div>
              </div>
              
              <div class="review-analysis">
                <h4>综合评价</h4>
                <p>{{ gear.aiReview }}</p>
              </div>
              
              <div class="review-dimensions">
                <div v-for="(dim, index) in gear.dimensions" :key="index" class="dimension-item">
                  <div class="dimension-header">
                    <span class="dimension-icon">{{ dim.icon }}</span>
                    <span class="dimension-name">{{ dim.name }}</span>
                    <span class="dimension-score">{{ dim.score }}分</span>
                  </div>
                  <el-progress :percentage="dim.score" :color="getProgressColor(dim.score)" />
                  <p class="dimension-comment">{{ dim.comment }}</p>
                </div>
              </div>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="用户评价" name="comments">
            <div class="comments-list">
              <div v-for="(comment, index) in comments" :key="index" class="comment-item">
                <div class="comment-header">
                  <div class="comment-avatar">
                    <img :src="comment.avatar" :alt="comment.username" />
                  </div>
                  <div class="comment-info">
                    <span class="comment-username">{{ comment.username }}</span>
                    <span class="comment-date">{{ comment.date }}</span>
                  </div>
                  <el-rate :value="comment.rating" disabled size="small" />
                </div>
                <div class="comment-content">{{ comment.content }}</div>
                <div class="comment-images" v-if="comment.images.length > 0">
                  <img v-for="(img, i) in comment.images" :key="i" :src="img" :alt="`评价图片${i + 1}`" />
                </div>
                <div class="comment-actions">
                  <span class="action-btn">👍 {{ comment.likes }}</span>
                  <span class="action-btn">💬 回复</span>
                </div>
              </div>
            </div>
            
            <div class="comment-form">
              <el-input v-model="newComment" type="textarea" placeholder="写下你的评价..." :rows="3" />
              <div class="form-actions">
                <el-button type="primary" size="small">提交评价</el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      
      <div class="related-section">
        <h3>📦 相关配件推荐</h3>
        <div class="related-list">
          <div v-for="item in relatedGear" :key="item.id" class="related-item">
            <div class="related-image">
              <img :src="item.imageUrl" :alt="item.model" />
            </div>
            <div class="related-info">
              <div class="related-brand">{{ item.brand }}</div>
              <div class="related-model">{{ item.model }}</div>
              <div class="related-price">¥{{ formatPrice(item.price) }}</div>
            </div>
            <el-button size="small" @click="viewDetail(item.id)">查看</el-button>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else class="loading-state">
      <el-loading text="加载中..." />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getGearDetail } from '../../api/gear'

const route = useRoute()
const activeTab = ref('specs')
const gear = ref(null)
const newComment = ref('')

const thumbnails = computed(() => {
  if (!gear.value?.imageUrl) return []
  const baseUrl = gear.value.imageUrl.split('?')[0] || gear.value.imageUrl
  return [
    gear.value.imageUrl,
    baseUrl.replace('product', 'front'),
    baseUrl.replace('product', 'back')
  ]
})

const comments = [
  {
    username: '摄影爱好者小王',
    avatar: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=young%20asian%20male%20photographer%20portrait&image_size=square',
    date: '2024-01-15',
    rating: 5,
    content: '用了三个月，非常满意！对焦真的很快，特别是拍人像的时候，眼部对焦几乎不会失误。4K视频画质也很棒，推荐给想入门全画幅的朋友。',
    images: [],
    likes: 128
  },
  {
    username: '专业摄影师老李',
    avatar: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=middle%20aged%20asian%20male%20photographer%20portrait&image_size=square',
    date: '2024-01-10',
    rating: 5,
    content: '从A7M3升级过来的，提升非常明显。像素更高了，连拍更快了，视频能力也更强了。唯一觉得不足的是价格有点贵，但一分钱一分货，还是值得的。',
    images: [
      'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=photography%20work%20shot%20with%20sony%20a7m4&image_size=square'
    ],
    likes: 256
  },
  {
    username: '视频创作者小美',
    avatar: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=young%20asian%20female%20content%20creator%20portrait&image_size=square',
    date: '2024-01-05',
    rating: 4,
    content: '主要用来拍视频，4K 60p内录太棒了！S-Cinetone色彩模式拍出来的画面很有电影感。唯一的缺点是散热一般，长时间录制会发热降频。',
    images: [],
    likes: 89
  }
]

const relatedGear = [
  {
    id: 5,
    brand: '索尼',
    model: '85mm f/1.4 GM',
    price: 11999,
    imageUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=sony%2085mm%20f1.4%20gm%20lens%20product&image_size=square'
  },
  {
    id: 7,
    brand: '尼康',
    model: '24-70mm f/2.8 S',
    price: 14999,
    imageUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=nikon%2024-70mm%20f2.8%20lens%20product&image_size=square'
  },
  {
    id: 6,
    brand: '佳能',
    model: '50mm f/1.8 STM',
    price: 699,
    imageUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=canon%2050mm%20f1.8%20lens%20product&image_size=square'
  }
]

const formatPrice = (price) => {
  if (typeof price === 'number') {
    return Math.round(price).toLocaleString()
  }
  return price
}

const getProgressColor = (score) => {
  if (score >= 90) return '#67c23a'
  if (score >= 70) return '#409eff'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

const goBack = () => {
  window.history.back()
}

const viewDetail = (id) => {
  window.location.href = `/gear/${id}`
}

const loadGearDetail = async () => {
  const id = route.params.id
  try {
    const response = await getGearDetail(id)
    gear.value = response
  } catch (error) {
    console.error('加载器材详情失败:', error)
  }
}

onMounted(() => {
  loadGearDetail()
})
</script>

<style scoped>
.gear-detail-page {
  padding: 20px;
}

.detail-header {
  margin-bottom: 20px;
}

.detail-content {
  max-width: 1200px;
  margin: 0 auto;
}

.main-section {
  display: flex;
  gap: 40px;
  margin-bottom: 30px;
}

.image-section {
  flex: 1;
}

.main-image {
  height: 450px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  margin-bottom: 15px;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-list {
  display: flex;
  gap: 10px;
}

.thumbnail-item {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.thumbnail-item.active {
  border-color: #667eea;
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-section {
  flex: 1;
}

.gear-brand {
  font-size: 14px;
  color: #667eea;
  margin-bottom: 10px;
}

.gear-model {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 15px;
}

.gear-price {
  font-size: 32px;
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 15px;
}

.gear-rating {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.rating-count {
  font-size: 14px;
  color: #999;
}

.gear-description {
  font-size: 15px;
  line-height: 1.7;
  color: #666;
  margin-bottom: 30px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 10px;
}

.specs-brief {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 30px;
}

.spec-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.spec-icon {
  font-size: 20px;
}

.spec-label {
  font-size: 13px;
  color: #999;
  width: 40px;
}

.spec-value {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.actions {
  display: flex;
  gap: 15px;
}

.tabs-section {
  margin-bottom: 30px;
}

.specs-table {
  padding: 20px;
}

.spec-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.spec-row:last-child {
  border-bottom: none;
}

.spec-name {
  width: 200px;
  font-size: 14px;
  color: #999;
}

.spec-desc {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.ai-review {
  padding: 20px;
}

.review-score {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
}

.score-value {
  font-size: 40px;
  font-weight: bold;
}

.score-label {
  font-size: 12px;
}

.review-analysis {
  margin-bottom: 30px;
}

.review-analysis h4 {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.review-analysis p {
  font-size: 14px;
  line-height: 1.7;
  color: #666;
}

.review-dimensions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.dimension-item {
  padding: 15px;
  background: #f8f9fa;
  border-radius: 10px;
}

.dimension-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.dimension-icon {
  font-size: 18px;
}

.dimension-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.dimension-score {
  margin-left: auto;
  font-size: 14px;
  font-weight: bold;
  color: #667eea;
}

.dimension-comment {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.comments-list {
  padding: 20px;
}

.comment-item {
  padding: 20px 0;
  border-bottom: 1px solid #eee;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.comment-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-info {
  flex: 1;
}

.comment-username {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  display: block;
}

.comment-date {
  font-size: 12px;
  color: #999;
}

.comment-content {
  font-size: 14px;
  line-height: 1.6;
  color: #666;
  margin-bottom: 15px;
}

.comment-images {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.comment-images img {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
}

.comment-actions {
  display: flex;
  gap: 20px;
}

.action-btn {
  font-size: 13px;
  color: #999;
  cursor: pointer;
}

.comment-form {
  padding: 20px;
  border-top: 1px solid #eee;
}

.comment-form .form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.related-section {
  margin-top: 30px;
}

.related-section h3 {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.related-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 15px;
}

.related-item {
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.related-image {
  height: 150px;
  overflow: hidden;
}

.related-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.related-info {
  padding: 12px;
}

.related-brand {
  font-size: 12px;
  color: #667eea;
  margin-bottom: 4px;
}

.related-model {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.related-price {
  font-size: 15px;
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 10px;
}

.related-item .el-button {
  margin: 0 12px 12px;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

@media (max-width: 768px) {
  .main-section {
    flex-direction: column;
  }
  
  .review-dimensions {
    grid-template-columns: 1fr;
  }
}
</style>