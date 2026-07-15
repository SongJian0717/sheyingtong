<template>
  <div class="gear-recommend-page">
    <div class="page-header">
      <h2>器材选购</h2>
      <p class="page-subtitle">输入预算和需求，智能推荐最适合你的相机、镜头等摄影器材</p>
    </div>
    
    <div class="form-section">
      <el-card class="form-card">
        <h3 class="form-title">🎯 告诉我你的需求</h3>
        
        <el-form :model="requirementForm" label-width="100px" class="requirement-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="预算范围">
                <div class="budget-input">
                  <el-input-number v-model="requirementForm.minBudget" :min="0" :max="1000000" placeholder="最低预算" style="width: 140px" />
                  <span class="budget-separator">~</span>
                  <el-input-number v-model="requirementForm.maxBudget" :min="0" :max="1000000" placeholder="最高预算" style="width: 140px" />
                  <span class="budget-unit">元</span>
                </div>
              </el-form-item>
            </el-col>
            
            <el-col :span="12">
              <el-form-item label="拍摄题材">
                <el-select v-model="requirementForm.subject" placeholder="请选择拍摄题材" style="width: 100%">
                  <el-option label="人像摄影" value="人像" />
                  <el-option label="风光摄影" value="风光" />
                  <el-option label="运动摄影" value="运动" />
                  <el-option label="微距摄影" value="微距" />
                  <el-option label="纪实摄影" value="纪实" />
                  <el-option label="商业摄影" value="商业" />
                  <el-option label="视频拍摄" value="视频" />
                  <el-option label="综合用途" value="综合" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="用户水平">
                <el-select v-model="requirementForm.level" placeholder="请选择用户水平" style="width: 100%">
                  <el-option label="入门新手" value="入门" />
                  <el-option label="进阶爱好者" value="进阶" />
                  <el-option label="专业摄影师" value="专业" />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :span="12">
              <el-form-item label="偏好品牌">
                <el-select v-model="requirementForm.brand" placeholder="请选择品牌（可选）" style="width: 100%" clearable filterable>
                  <el-option label="索尼 Sony" value="Sony" />
                  <el-option label="佳能 Canon" value="Canon" />
                  <el-option label="尼康 Nikon" value="Nikon" />
                  <el-option label="富士 Fujifilm" value="Fujifilm" />
                  <el-option label="适马 Sigma" value="Sigma" />
                  <el-option label="腾龙 Tamron" value="Tamron" />
                  <el-option label="森养 Samyang" value="Samyang" />
                  <el-option label="捷信 Gitzo" value="Gitzo" />
                  <el-option label="曼富图 Manfrotto" value="Manfrotto" />
                  <el-option label="巅峰设计 Peak Design" value="Peak Design" />
                  <el-option label="大疆 DJI" value="DJI" />
                  <el-option label="百诺 Benro" value="Benro" />
                  <el-option label="神牛 Godox" value="Godox" />
                  <el-option label="永诺 Yongnuo" value="Yongnuo" />
                  <el-option label="斯莫格 SmallRig" value="SmallRig" />
                  <el-option label="耐司 NiSi" value="NiSi" />
                  <el-option label="K&F Concept" value="K&F Concept" />
                  <el-option label="徕卡 Leica" value="Leica" />
                  <el-option label="松下 Panasonic" value="Panasonic" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="器材类型">
                <el-select v-model="requirementForm.type" placeholder="请选择器材类型" style="width: 100%">
                  <el-option label="相机机身" value="相机" />
                  <el-option label="镜头" value="镜头" />
                  <el-option label="相机+镜头套装" value="套装" />
                  <el-option label="配件" value="配件" />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :span="12">
              <el-form-item label="额外需求">
                <el-input v-model="requirementForm.extra" placeholder="其他特殊需求（可选）" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        
        <div class="form-actions">
          <el-button type="primary" size="large" :loading="isLoading" @click="getRecommendations">
            {{ isLoading ? '推荐中...' : '🎬 智能推荐' }}
          </el-button>
          <el-button size="large" @click="resetForm">重置</el-button>
        </div>
      </el-card>
    </div>
    
    <div v-if="recommendations.length > 0" class="result-section">
      <el-card class="result-card">
        <div class="result-header">
          <h3>💡 推荐方案</h3>
          <span class="result-count">共 {{ recommendations.length }} 款推荐</span>
        </div>
        
        <div class="summary-section">
          <div class="summary-card">
            <h4>综合建议</h4>
            <p>{{ recommendationSummary }}</p>
          </div>
        </div>
        
        <div class="recommendations-grid">
          <div v-for="item in recommendations" :key="item.rank" class="recommendation-card">
            <div class="card-rank">
              <span class="rank-number" :class="getRankClass(item.rank)">{{ item.rank }}</span>
              <span class="rank-label">推荐</span>
            </div>
            
            <div class="card-image">
              <img :src="item.gear.imageUrl" :alt="item.gear.model" />
            </div>
            
            <div class="card-info">
              <div class="gear-brand">{{ item.gear.brand }}</div>
              <div class="gear-model">{{ item.gear.model }}</div>
              <div class="gear-price">¥{{ formatPrice(item.gear.price) }}</div>
              <div class="gear-type">{{ item.gear.category?.name || '其他' }}</div>
            </div>
            
            <div class="card-reason">
              <h4>推荐理由</h4>
              <p>{{ item.reason }}</p>
            </div>
            
            <div class="card-highlights">
              <span v-for="(highlight, index) in item.highlights" :key="index" class="highlight-tag">
                {{ highlight }}
              </span>
            </div>
            
            <div class="card-actions">
              <el-button type="primary" size="small" @click="viewDetail(item.gear.id)">查看详情</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
    
    <div class="hot-gear-section">
      <div class="section-header">
        <h3>🔥 热门器材</h3>
      </div>
      
      <div class="hot-gear-list">
        <div v-for="gear in hotGearList" :key="gear.id" class="hot-gear-card" @click="viewDetail(gear.id)">
          <div class="hot-gear-image">
            <img :src="gear.imageUrl" :alt="gear.model" />
          </div>
          <div class="hot-gear-info">
            <div class="hot-gear-brand">{{ gear.brand }}</div>
            <div class="hot-gear-model">{{ gear.model }}</div>
            <div class="hot-gear-price">¥{{ formatPrice(gear.price) }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="all-gear-section">
      <div class="section-header all-gear-header">
        <h3>📦 全部器材库（共 {{ allGearTotal }} 款）</h3>
        <div class="search-box">
          <el-input
            v-model="gearKeyword"
            placeholder="搜索器材型号或品牌（如：24-70、Sony、三脚架）"
            clearable
            style="width: 340px"
            @keyup.enter="handleSearch"
            @clear="handleSearch">
            <template #append>
              <el-button @click="handleSearch">🔍 搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <div class="category-tabs">
        <div
          class="tab-item"
          :class="{ active: activeCategoryId === null }"
          @click="handleCategoryChange(null)">
          全部（{{ allGearTotal }}）
        </div>
        <div
          v-for="cat in gearCategories"
          :key="cat.id"
          class="tab-item"
          :class="{ active: activeCategoryId === cat.id }"
          @click="handleCategoryChange(cat.id)">
          <span v-if="cat.icon">{{ cat.icon }}</span> {{ cat.name }}（{{ categoryCounts[cat.id] || 0 }}）
        </div>
      </div>

      <div v-loading="allGearLoading" class="hot-gear-list all-gear-list">
        <div
          v-for="gear in allGearList"
          :key="gear.id"
          class="hot-gear-card all-gear-card"
          @click="viewDetail(gear.id)">
          <div class="hot-gear-image">
            <img :src="gear.imageUrl" :alt="gear.model" />
          </div>
          <div class="hot-gear-info">
            <div class="hot-gear-brand">{{ gear.brand }} · {{ gear.category?.name || '其他' }}</div>
            <div class="hot-gear-model">{{ gear.model }}</div>
            <div class="hot-gear-price">¥{{ formatPrice(gear.price) }}</div>
          </div>
        </div>
        <div v-if="allGearList.length === 0 && !allGearLoading" class="empty-tip">
          😅 没有找到符合条件的器材，试试换个关键词或分类
        </div>
      </div>

      <div v-if="allGearTotal > allGearSize" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next, total, jumper"
          :total="allGearTotal"
          :page-size="allGearSize"
          :current-page="allGearPage"
          @current-change="handlePageChange" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { getRecommendations as fetchRecommendationsApi, getPopularGear, searchGear, getCategories } from '../../api/gear'

const requirementForm = ref({
  minBudget: null,
  maxBudget: null,
  subject: '',
  level: '',
  brand: '',
  type: '',
  extra: ''
})

const isLoading = ref(false)
const recommendations = ref([])
const recommendationSummary = ref('')
const hotGearList = ref([])

const gearCategories = ref([])
const activeCategoryId = ref(null)
const gearKeyword = ref('')
const allGearList = ref([])
const allGearTotal = ref(0)
const allGearPage = ref(1)
const allGearSize = ref(15)
const allGearLoading = ref(false)
const categoryCounts = ref({})

const formatPrice = (price) => {
  if (typeof price === 'number') {
    return Math.round(price).toLocaleString()
  }
  return price
}

const getRankClass = (rank) => {
  if (rank === 1) return 'rank-first'
  if (rank === 2) return 'rank-second'
  if (rank === 3) return 'rank-third'
  return 'rank-other'
}

const getRecommendations = async () => {
  isLoading.value = true
  
  try {
    const response = await fetchRecommendationsApi({
      minBudget: requirementForm.value.minBudget,
      maxBudget: requirementForm.value.maxBudget,
      subject: requirementForm.value.subject,
      level: requirementForm.value.level,
      brand: requirementForm.value.brand,
      type: requirementForm.value.type,
      extra: requirementForm.value.extra
    })
    
    recommendations.value = (response.recommendations || []).map((gear, index) => ({
      rank: index + 1,
      gear: gear,
      reason: gear.description || '暂无详细描述',
      highlights: generateHighlights(gear)
    }))
    recommendationSummary.value = response.summary || ''
  } catch (error) {
    console.error('获取推荐失败:', error)
    recommendations.value = []
    recommendationSummary.value = '获取推荐失败，请重试'
  } finally {
    isLoading.value = false
  }
}

const generateHighlights = (gear) => {
  const highlights = []
  if (gear.pixel) highlights.push(gear.pixel)
  if (gear.continuousShooting) highlights.push(gear.continuousShooting)
  if (gear.video) highlights.push(gear.video)
  if (gear.focus) highlights.push(gear.focus)
  if (gear.rating) highlights.push(`评分 ${gear.rating}/5.0`)
  if (highlights.length === 0 && gear.description) {
    highlights.push(gear.description.substring(0, 20) + '...')
  }
  return highlights.slice(0, 4)
}

const resetForm = () => {
  requirementForm.value = {
    minBudget: null,
    maxBudget: null,
    subject: '',
    level: '',
    brand: '',
    type: '',
    extra: ''
  }
  recommendations.value = []
  recommendationSummary.value = ''
}

const viewDetail = (id) => {
  window.location.href = `/gear/${id}`
}

const loadHotGear = async () => {
  try {
    hotGearList.value = await getPopularGear()
  } catch (error) {
    console.error('加载热门器材失败:', error)
    hotGearList.value = []
  }
}

const loadCategories = async () => {
  try {
    gearCategories.value = await getCategories()
  } catch (error) {
    console.error('加载器材分类失败:', error)
    gearCategories.value = []
  }
}

const loadCategoryCounts = async () => {
  try {
    const resp = await searchGear({ page: 0, size: 9999 })
    const allGears = resp.content || []
    const counts = {}
    allGears.forEach(g => {
      if (g.category && g.category.id) {
        counts[g.category.id] = (counts[g.category.id] || 0) + 1
      }
    })
    categoryCounts.value = counts
  } catch (error) {
    console.error('加载分类数量失败:', error)
    categoryCounts.value = {}
  }
}

const loadAllGear = async () => {
  allGearLoading.value = true
  try {
    const pageParams = {
      page: allGearPage.value - 1,
      size: allGearSize.value
    }
    if (activeCategoryId.value) pageParams.categoryId = activeCategoryId.value
    if (gearKeyword.value && gearKeyword.value.trim()) pageParams.keyword = gearKeyword.value.trim()
    const resp = await searchGear(pageParams)
    allGearList.value = resp.content || []
    allGearTotal.value = resp.totalElements || 0
  } catch (error) {
    console.error('加载全部器材失败:', error)
    allGearList.value = []
    allGearTotal.value = 0
  } finally {
    allGearLoading.value = false
  }
}

const handleCategoryChange = (catId) => {
  activeCategoryId.value = activeCategoryId.value === catId ? null : catId
  allGearPage.value = 1
  loadAllGear()
}

const handleSearch = () => {
  allGearPage.value = 1
  loadAllGear()
}

const handlePageChange = (p) => {
  allGearPage.value = p
  loadAllGear()
}

watch([activeCategoryId, gearKeyword, allGearPage, allGearSize], () => {
})

onMounted(() => {
  loadHotGear()
  loadCategories()
  loadAllGear()
  loadCategoryCounts()
})
</script>

<style scoped>
.gear-recommend-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.page-subtitle {
  font-size: 16px;
  color: #666;
}

.form-section {
  margin-bottom: 30px;
}

.form-card {
  border-radius: 12px;
}

.form-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.requirement-form {
  margin-bottom: 20px;
}

.budget-input {
  display: flex;
  align-items: center;
  gap: 5px;
}

.budget-separator {
  font-size: 16px;
  color: #999;
  margin: 0 5px;
}

.budget-unit {
  margin-left: 5px;
  font-size: 14px;
  color: #666;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.result-section {
  margin-bottom: 30px;
}

.result-card {
  border-radius: 12px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.result-header h3 {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.result-count {
  font-size: 14px;
  color: #999;
}

.summary-section {
  margin-bottom: 20px;
}

.summary-card {
  background: #f5f7ff;
  border-radius: 10px;
  padding: 20px;
}

.summary-card h4 {
  font-size: 16px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 10px;
}

.summary-card p {
  font-size: 14px;
  line-height: 1.6;
  color: #666;
  margin: 0;
}

.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 25px;
}

.recommendation-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.recommendation-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
}

.card-rank {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 15px;
  background: #f8f9fa;
}

.rank-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  color: white;
}

.rank-first {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
}

.rank-second {
  background: linear-gradient(135deg, #ffa502 0%, #ff9f43 100%);
}

.rank-third {
  background: linear-gradient(135deg, #ffd32a 0%, #ff9f43 100%);
}

.rank-other {
  background: #999;
}

.rank-label {
  font-size: 13px;
  color: #999;
}

.card-image {
  height: 200px;
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-info {
  padding: 15px;
}

.gear-brand {
  font-size: 13px;
  color: #667eea;
  margin-bottom: 5px;
}

.gear-model {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.gear-price {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 5px;
}

.gear-type {
  font-size: 13px;
  color: #999;
}

.card-reason {
  padding: 0 15px 15px;
}

.card-reason h4 {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.card-reason p {
  font-size: 13px;
  line-height: 1.6;
  color: #666;
  margin: 0;
}

.card-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 15px 15px;
}

.highlight-tag {
  padding: 4px 10px;
  background: #e8f0fe;
  color: #667eea;
  border-radius: 8px;
  font-size: 12px;
}

.card-actions {
  padding: 15px;
  border-top: 1px solid #eee;
}

.hot-gear-section {
  margin-top: 30px;
}

.section-header {
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.hot-gear-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.hot-gear-card {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s ease;
}

.hot-gear-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.hot-gear-image {
  height: 150px;
  overflow: hidden;
}

.hot-gear-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hot-gear-info {
  padding: 12px;
}

.hot-gear-brand {
  font-size: 12px;
  color: #667eea;
  margin-bottom: 4px;
}

.hot-gear-model {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.hot-gear-price {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
}

.all-gear-section {
  margin-top: 40px;
  padding: 20px;
  background: #fafbfc;
  border-radius: 12px;
}

.all-gear-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.all-gear-header h3 {
  margin: 0;
  font-size: 20px;
}

.search-box {
  display: flex;
  align-items: center;
}

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.tab-item {
  padding: 8px 18px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 20px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.25s ease;
  user-select: none;
}

.tab-item:hover {
  color: #667eea;
  border-color: #667eea;
  background: #f0f3ff;
}

.tab-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
}

.all-gear-list {
  min-height: 300px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

.all-gear-card {
  transition: all 0.3s ease;
}

.all-gear-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.12);
}

.empty-tip {
  grid-column: 1 / -1;
  padding: 60px 20px;
  text-align: center;
  color: #909399;
  font-size: 15px;
  background: #fff;
  border-radius: 10px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

@media (max-width: 768px) {
  .recommendations-grid {
    grid-template-columns: 1fr;
  }

  .all-gear-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-box,
  .search-box .el-input {
    width: 100% !important;
  }

  .all-gear-list {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 12px;
  }
}
</style>