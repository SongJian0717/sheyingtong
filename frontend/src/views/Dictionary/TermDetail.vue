<template>
  <div class="term-detail-page">
    <div v-if="loading" class="loading-state">
      <el-loading text="加载中..." />
    </div>
    
    <div v-else-if="term" class="detail-content">
      <div class="detail-header">
        <div class="term-info">
          <h1>{{ term.term }}</h1>
          <el-tag type="primary">{{ term.category }}</el-tag>
        </div>
        <el-button type="text" @click="$router.back()">返回列表</el-button>
      </div>
      
      <div class="detail-body">
        <div class="section-card">
          <h3 class="section-title">📖 定义</h3>
          <p class="definition-text">{{ term.definition }}</p>
        </div>
        
        <div class="section-card" v-if="term.explanation">
          <h3 class="section-title">🔍 详细解释</h3>
          <p class="explanation-text">{{ term.explanation }}</p>
        </div>
        
        <div class="section-card" v-if="examples.length > 0">
          <h3 class="section-title">💡 使用场景</h3>
          <ul class="examples-list">
            <li v-for="(example, index) in examples" :key="index">
              <span class="example-number">{{ index + 1 }}</span>
              <span class="example-text">{{ example }}</span>
            </li>
          </ul>
        </div>
        
        <div class="section-card" v-if="relatedTerms.length > 0">
          <h3 class="section-title">🔗 相关术语</h3>
          <div class="related-terms">
            <el-tag
              v-for="related in relatedTerms"
              :key="related"
              class="related-tag"
              @click="navigateToTerm(related)"
            >
              {{ related }}
            </el-tag>
          </div>
        </div>
        
        <div class="section-card">
          <h3 class="section-title">📚 学习建议</h3>
          <div class="study-tips">
            <div class="tip-item">
              <span class="tip-icon">🎯</span>
              <span class="tip-text">理解这个术语在实际拍摄中的应用场景</span>
            </div>
            <div class="tip-item">
              <span class="tip-icon">📷</span>
              <span class="tip-text">尝试在拍摄时有意识地运用这个概念</span>
            </div>
            <div class="tip-item">
              <span class="tip-icon">🔗</span>
              <span class="tip-text">点击相关术语，深入了解关联知识</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else class="not-found">
      <el-empty description="未找到该术语" />
      <el-button type="primary" @click="$router.push('/dictionary')">返回词典首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTermDetail } from '../../api/dictionary'

const route = useRoute()
const term = ref(null)
const loading = ref(true)
const examples = ref([])
const relatedTerms = ref([])

const parseRelatedTerms = (relatedTermsStr) => {
  if (!relatedTermsStr) return []
  return relatedTermsStr.split(',').map(t => t.trim()).filter(t => t)
}

const parseExamples = (examplesStr) => {
  if (!examplesStr) return []
  return examplesStr.split('\n').map(e => e.trim()).filter(e => e)
}

const navigateToTerm = (termName) => {
  window.location.href = `/dictionary/term/${encodeURIComponent(termName)}`
}

onMounted(async () => {
  const termName = decodeURIComponent(route.params.term)
  try {
    const response = await getTermDetail(termName)
    term.value = response
    examples.value = parseExamples(response.examples)
    relatedTerms.value = parseRelatedTerms(response.relatedTerms)
  } catch (error) {
    console.error('获取术语详情失败:', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.term-detail-page {
  padding: 20px;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

.detail-content {
  max-width: 800px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.term-info h1 {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.section-card {
  background: white;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 15px;
}

.definition-text {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
}

.explanation-text {
  font-size: 15px;
  line-height: 1.8;
  color: #666;
}

.examples-list {
  list-style: none;
  padding: 0;
}

.examples-list li {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.example-number {
  width: 28px;
  height: 28px;
  background: #667eea;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: bold;
  flex-shrink: 0;
}

.example-text {
  font-size: 15px;
  line-height: 1.6;
  color: #666;
  padding-top: 3px;
}

.related-terms {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.related-tag {
  cursor: pointer;
  padding: 8px 16px;
  font-size: 14px;
  background: #e8f0fe;
  color: #667eea;
}

.related-tag:hover {
  background: #667eea;
  color: white;
}

.study-tips {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.tip-icon {
  font-size: 18px;
}

.tip-text {
  font-size: 14px;
  color: #666;
}

.not-found {
  text-align: center;
  padding: 60px 0;
}

.not-found .el-button {
  margin-top: 20px;
}

@media (max-width: 768px) {
  .detail-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
  
  .term-info h1 {
    font-size: 24px;
  }
}
</style>
