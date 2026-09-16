<template>
  <div class="community-page">
    <div class="page-header">
      <h2>📷 作品广场</h2>
      <p class="page-subtitle">摄影师们把 AI 打分的满意之作分享到这里，欢迎围观、点赞与评论</p>
    </div>

    <div class="sort-tabs">
      <button
        v-for="tab in sortTabs"
        :key="tab.value"
        class="sort-tab"
        :class="{ active: sort === tab.value }"
        @click="switchSort(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <WorkGrid
      :works="works"
      :has-more="hasMore"
      :loading="loading"
      empty-text="还没有作品，快去「照片打分」发布第一张吧"
      @load-more="loadMore"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getWorks } from '../../api/work'
import WorkGrid from '../../components/Community/WorkGrid.vue'

const PAGE_SIZE = 12
const sortTabs = [
  { value: 'latest', label: '最新发布' },
  { value: 'hot', label: '热门作品' }
]

const sort = ref('latest')
const works = ref([])
const nextCursor = ref(null)
const hasMore = ref(false)
const loading = ref(false)

const fetchFirstPage = async () => {
  loading.value = true
  try {
    const page = await getWorks(sort.value, null, PAGE_SIZE)
    works.value = page.items || []
    nextCursor.value = page.nextCursor
    hasMore.value = !!page.hasMore
  } catch (error) {
    works.value = []
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  loading.value = true
  try {
    const page = await getWorks(sort.value, nextCursor.value, PAGE_SIZE)
    works.value = works.value.concat(page.items || [])
    nextCursor.value = page.nextCursor
    hasMore.value = !!page.hasMore
  } catch (error) {
    // 失败提示已由请求拦截器统一弹出
  } finally {
    loading.value = false
  }
}

const switchSort = (value) => {
  if (sort.value === value) return
  sort.value = value
  works.value = []
  nextCursor.value = null
  fetchFirstPage()
}

onMounted(fetchFirstPage)
</script>

<style scoped>
.community-page {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 26px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px;
}

.page-subtitle {
  font-size: 15px;
  color: #888;
  margin: 0;
}

.sort-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 22px;
}

.sort-tab {
  border: none;
  background: #f2f4f8;
  color: #555;
  padding: 8px 22px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.sort-tab:hover {
  background: #e8ecf7;
  color: #667eea;
}

.sort-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
}
</style>
