<template>
  <div class="work-card" @click="goDetail">
    <div class="card-image">
      <img :src="imageUrl" :alt="work.title" loading="lazy" />
      <div class="card-overlay">
        <p class="overlay-title">{{ work.title }}</p>
        <p v-if="work.content" class="overlay-content">{{ work.content }}</p>
      </div>
    </div>

    <div class="card-footer">
      <div class="author" @click.stop="goAuthor">
        <img :src="authorAvatar" class="author-avatar" alt="头像" />
        <span class="author-name">{{ authorName }}</span>
      </div>
      <div class="card-stats">
        <span class="stat" title="点赞">
          <span class="stat-icon">👍</span>{{ work.likesCount ?? 0 }}
        </span>
        <span class="stat" title="评论">
          <span class="stat-icon">💬</span>{{ work.commentsCount ?? 0 }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { workImageUrl } from '../../api/work'

const props = defineProps({
  work: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const imageUrl = computed(() => workImageUrl(props.work.photoAnalysisId))

const authorName = computed(() => {
  const author = props.work.author
  return author ? author.nickname || author.username : '匿名'
})

const authorAvatar = computed(() => {
  const author = props.work.author
  if (author && author.avatar) return author.avatar
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=${props.work.author ? props.work.author.username : 'default'}`
})

const goDetail = () => {
  router.push(`/community/work/${props.work.id}`)
}

const goAuthor = () => {
  if (props.work.author) {
    router.push(`/user/${props.work.author.username}`)
  }
}
</script>

<style scoped>
.work-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  display: flex;
  flex-direction: column;
}

.work-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.18);
}

.card-image {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #f0f2f5;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.3s ease;
}

.work-card:hover .card-image img {
  transform: scale(1.05);
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 40%, rgba(0, 0, 0, 0.72) 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 14px;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.work-card:hover .card-overlay {
  opacity: 1;
}

.overlay-title {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overlay-content {
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  gap: 8px;
}

.author {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.author-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  background: #e8ecf3;
  flex-shrink: 0;
}

.author-name {
  font-size: 13px;
  color: #555;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author:hover .author-name {
  color: #667eea;
}

.card-stats {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.stat {
  font-size: 13px;
  color: #888;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.stat-icon {
  font-size: 13px;
}
</style>
