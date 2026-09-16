<template>
  <div class="feed-page">
    <div class="page-header">
      <h2>🔔 关注动态</h2>
      <p class="page-subtitle">你关注的人发布的最新作品会实时出现在这里</p>
    </div>

    <WorkGrid
      :works="works"
      :has-more="hasMore"
      :loading="loading"
      empty-text="你还没有关注任何摄影师，或关注的人暂无新作品"
      @load-more="loadMore"
    />

    <div v-if="!loading && works.length === 0" class="feed-empty-action">
      <el-button type="primary" round @click="$router.push('/community')">
        去作品广场逛逛 →
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFeed } from '../../api/feed'
import WorkGrid from '../../components/Community/WorkGrid.vue'

const PAGE_SIZE = 12

const works = ref([])
const nextCursor = ref(null)
const hasMore = ref(false)
const loading = ref(false)

const loadFirstPage = async () => {
  loading.value = true
  try {
    const page = await getFeed(null, PAGE_SIZE)
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
    const page = await getFeed(nextCursor.value, PAGE_SIZE)
    works.value = works.value.concat(page.items || [])
    nextCursor.value = page.nextCursor
    hasMore.value = !!page.hasMore
  } catch (error) {
    // 失败提示已由请求拦截器统一弹出
  } finally {
    loading.value = false
  }
}

onMounted(loadFirstPage)
</script>

<style scoped>
.feed-page {
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

.feed-empty-action {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}
</style>
