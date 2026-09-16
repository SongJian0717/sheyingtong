<template>
  <div class="work-grid-area">
    <div v-if="works.length > 0" class="work-grid">
      <WorkCard v-for="work in works" :key="work.id" :work="work" />
    </div>

    <div v-else-if="!loading" class="work-grid-empty">
      <el-empty :description="emptyText" />
    </div>

    <div v-if="hasMore" class="load-more">
      <el-button :loading="loading" @click="$emit('load-more')">
        {{ loading ? '加载中...' : '加载更多' }}
      </el-button>
    </div>
    <div v-else-if="works.length > 0" class="no-more">
      <span>— 已经到底啦 —</span>
    </div>
  </div>
</template>

<script setup>
import WorkCard from './WorkCard.vue'

defineProps({
  works: {
    type: Array,
    default: () => []
  },
  hasMore: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  emptyText: {
    type: String,
    default: '这里还没有内容'
  }
})

defineEmits(['load-more'])
</script>

<style scoped>
.work-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.work-grid-empty {
  padding: 60px 0;
}

.load-more {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

.no-more {
  text-align: center;
  color: #bbb;
  font-size: 13px;
  margin-top: 26px;
}
</style>
