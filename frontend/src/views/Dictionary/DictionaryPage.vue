<template>
  <div class="dictionary-page">
    <div class="page-header">
      <h2>摄影词典</h2>
      <p class="page-subtitle">收录500+摄影术语，智能检索，专业解释</p>
    </div>
    
    <div class="search-section">
      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="输入摄影术语进行搜索，如：光圈、快门、ISO..."
          prefix-icon="Search"
          size="large"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
      </div>
    </div>
    
    <div class="category-section">
      <div class="category-tabs">
        <el-tag
          v-for="cat in categories"
          :key="cat.id"
          :type="selectedCategory === cat.id ? 'primary' : 'info'"
          :closable="false"
          @click="selectCategory(cat.id)"
          class="category-tag"
        >
          {{ cat.name }}
        </el-tag>
      </div>
    </div>
    
    <div class="main-content">
      <div class="term-list">
        <div class="list-header">
          <span class="list-title">术语列表</span>
          <span class="list-count">共 {{ totalElements }} 条</span>
        </div>
        
        <div v-if="terms.length > 0" class="terms-grid">
          <div
            v-for="term in terms"
            :key="term.id"
            class="term-card"
            @click="showTermDetail(term)"
          >
            <div class="term-header">
              <span class="term-name">{{ term.term }}</span>
              <span class="term-category">{{ term.category }}</span>
            </div>
            <p class="term-definition">{{ term.definition }}</p>
            <div class="term-tags">
              <span v-for="related in term.relatedTermsArray" :key="related" class="related-tag" @click.stop="handleQuickSearch(related)">
                {{ related }}
              </span>
            </div>
          </div>
        </div>
        
        <div v-else class="empty-state">
          <el-empty description="暂无术语数据" />
        </div>
        
        <div v-if="totalElements > 0" class="pagination-section">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="totalElements"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
      
      <div class="detail-panel" v-if="selectedTerm">
        <div class="panel-header">
          <h3>{{ selectedTerm.term }}</h3>
          <el-button type="text" @click="selectedTerm = null">✕</el-button>
        </div>
        
        <div class="panel-content">
          <div class="detail-section">
            <span class="detail-label">分类</span>
            <span class="detail-value">{{ selectedTerm.category }}</span>
          </div>
          
          <div class="detail-section">
            <span class="detail-label">定义</span>
            <p class="detail-content">{{ selectedTerm.definition }}</p>
          </div>
          
          <div class="detail-section" v-if="selectedTerm.explanation">
            <span class="detail-label">详细解释</span>
            <p class="detail-content">{{ selectedTerm.explanation }}</p>
          </div>
          
          <div class="detail-section" v-if="selectedTerm.examplesArray && selectedTerm.examplesArray.length > 0">
            <span class="detail-label">使用场景</span>
            <ul class="detail-list">
              <li v-for="(example, index) in selectedTerm.examplesArray" :key="index">{{ example }}</li>
            </ul>
          </div>
          
          <div class="detail-section" v-if="selectedTerm.relatedTermsArray && selectedTerm.relatedTermsArray.length > 0">
            <span class="detail-label">相关术语</span>
            <div class="related-terms">
              <span
                v-for="related in selectedTerm.relatedTermsArray"
                :key="related"
                class="related-term-btn"
                @click="handleQuickSearch(related)"
              >
                {{ related }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { searchTerms, getTerms, getCategories } from '../../api/dictionary'

const searchQuery = ref('')
const selectedCategory = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const terms = ref([])
const totalElements = ref(0)
const selectedTerm = ref(null)
const categories = ref([{ id: '', name: '全部' }])

const parseRelatedTerms = (relatedTerms) => {
  if (!relatedTerms) return []
  return relatedTerms.split(',').map(t => t.trim()).filter(t => t)
}

const parseExamples = (examples) => {
  if (!examples) return []
  return examples.split('\n').map(e => e.trim()).filter(e => e)
}

const loadTerms = async () => {
  try {
    const page = currentPage.value > 0 ? currentPage.value - 1 : 0
    const response = await getTerms(page, pageSize.value, selectedCategory.value)
    terms.value = response.content || []
    totalElements.value = response.totalElements || 0
    terms.value.forEach(term => {
      term.relatedTermsArray = parseRelatedTerms(term.relatedTerms)
      term.examplesArray = parseExamples(term.examples)
    })
  } catch (error) {
    console.error('加载术语失败:', error)
  }
}

const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    loadTerms()
    return
  }
  try {
    const response = await searchTerms(searchQuery.value.trim(), pageSize.value)
    terms.value = response.content || []
    totalElements.value = response.totalElements || 0
    terms.value.forEach(term => {
      term.relatedTermsArray = parseRelatedTerms(term.relatedTerms)
      term.examplesArray = parseExamples(term.examples)
    })
    
    if (terms.value.length > 0) {
      selectedTerm.value = terms.value[0]
    }
  } catch (error) {
    console.error('搜索失败:', error)
  }
}

const handleQuickSearch = (term) => {
  searchQuery.value = term
  handleSearch()
}

const selectCategory = (category) => {
  selectedCategory.value = category
  currentPage.value = 0
  loadTerms()
}

const showTermDetail = (term) => {
  selectedTerm.value = term
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 0
  loadTerms()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadTerms()
}

const loadCategories = async () => {
  try {
    const catList = await getCategories()
    categories.value = [{ id: '', name: '全部' }, ...catList.map(c => ({ id: c, name: c }))]
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

onMounted(() => {
  loadCategories()
  loadTerms()
})
</script>

<style scoped>
.dictionary-page {
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

.search-section {
  margin-bottom: 20px;
}

.search-box {
  display: flex;
  gap: 10px;
  max-width: 600px;
}

.search-box :deep(.el-input) {
  flex: 1;
}

.category-section {
  margin-bottom: 20px;
}

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-tag {
  cursor: pointer;
  padding: 8px 16px;
  font-size: 14px;
}

.main-content {
  display: flex;
  gap: 30px;
}

.term-list {
  flex: 2;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.list-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.list-count {
  font-size: 14px;
  color: #999;
}

.terms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.term-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s ease;
}

.term-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.term-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.term-name {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.term-category {
  padding: 4px 10px;
  background: #e8f0fe;
  color: #667eea;
  border-radius: 10px;
  font-size: 12px;
}

.term-definition {
  font-size: 14px;
  line-height: 1.6;
  color: #666;
  margin-bottom: 15px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.term-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.related-tag {
  padding: 4px 10px;
  background: #f5f5f5;
  color: #666;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.related-tag:hover {
  background: #667eea;
  color: white;
}

.empty-state {
  padding: 60px 0;
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

.detail-panel {
  flex: 1;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 20px;
  position: sticky;
  top: 80px;
  height: fit-content;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.panel-header h3 {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.panel-content {
  padding: 10px 0;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #999;
  margin-bottom: 8px;
}

.detail-value {
  font-size: 16px;
  color: #333;
}

.detail-content {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
}

.detail-list {
  list-style: disc;
  padding-left: 20px;
}

.detail-list li {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.related-terms {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.related-term-btn {
  padding: 6px 12px;
  background: #e8f0fe;
  color: #667eea;
  border-radius: 15px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.related-term-btn:hover {
  background: #667eea;
  color: white;
}

@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }
  
  .detail-panel {
    position: static;
  }
}
</style>