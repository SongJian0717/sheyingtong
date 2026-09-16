<template>
  <div class="work-detail-page" v-loading="loading">
    <div v-if="detail" class="detail-content">
      <a class="back-link" @click="goBack">← 返回</a>

      <!-- 作品主体 -->
      <div class="detail-card">
        <div class="photo-stage">
          <img :src="imageUrl" :alt="detail.title" />
        </div>

        <div class="detail-body">
          <div class="detail-head">
            <div class="detail-title-row">
              <h2 class="detail-title">{{ detail.title }}</h2>
              <el-button
                v-if="isMyWork"
                type="danger"
                plain
                size="small"
                @click="handleDeleteWork"
              >
                删除作品
              </el-button>
            </div>

            <p v-if="detail.content" class="detail-content">{{ detail.content }}</p>

            <div class="detail-meta">
              <div class="author" @click="goAuthor">
                <img :src="authorAvatar" class="author-avatar" alt="头像" />
                <div class="author-text">
                  <span class="author-name">{{ authorName }}</span>
                  <span class="publish-time">{{ formatDate(detail.createdAt) }} 发布</span>
                </div>
              </div>

              <div class="action-buttons">
                <button class="action-btn" :class="{ liked: liked }" @click="toggleLike">
                  <span class="action-icon">{{ liked ? '❤️' : '🤍' }}</span>
                  <span>{{ liked ? '已点赞' : '点赞' }}</span>
                  <span class="action-count">{{ likesCount }}</span>
                </button>
                <span class="action-static">
                  <span class="action-icon">💬</span>
                  <span>{{ commentsCount }} 条评论</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comments-card" id="comments">
        <div class="comments-header">
          <h3>评论 <span class="comments-count">({{ commentsCount }})</span></h3>
        </div>

        <div class="comment-editor">
          <textarea
            v-model="commentText"
            rows="3"
            maxlength="500"
            placeholder="友善评论，理性发言～"
            class="comment-textarea"
          ></textarea>
          <div v-if="replyTarget" class="reply-target">
            回复 <b>@{{ replyTargetName }}</b>
            <span class="reply-cancel" @click="cancelReply">取消</span>
          </div>
          <div class="editor-actions">
            <el-button type="primary" :disabled="!commentText.trim()" @click="submitComment">
              发表评论
            </el-button>
          </div>
        </div>

        <div v-if="comments.length" class="comment-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <img :src="commentAvatar(comment.author)" class="comment-avatar" alt="头像" />
            <div class="comment-main">
              <div class="comment-head">
                <span class="comment-name">{{ authorNameOf(comment.author) }}</span>
                <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
              </div>
              <p class="comment-content">
                <span v-if="comment.replyToName" class="reply-to">@{{ comment.replyToName }}</span>
                {{ comment.content }}
              </p>
              <div class="comment-actions">
                <button class="comment-action" @click="startReply(comment)">回复</button>
                <button
                  v-if="canDeleteComment(comment)"
                  class="comment-action danger"
                  @click="handleDeleteComment(comment)"
                >
                  删除
                </button>
              </div>
            </div>
          </div>

          <div v-if="commentsHasMore" class="comments-load-more">
            <el-button text @click="loadMoreComments">加载更多评论</el-button>
          </div>
        </div>
        <el-empty v-else description="还没有评论，来抢沙发～" :image-size="80" />
      </div>
    </div>

    <el-empty v-else-if="!loading" description="作品不存在或已被删除" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getWorkDetail,
  likeWork,
  unlikeWork,
  getComments,
  addComment,
  deleteComment,
  deleteWork,
  workImageUrl
} from '../../api/work'
import { useUserStore } from '../../stores/user'
import { requireLogin } from '../../utils/requireLogin'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const workId = route.params.id

const loading = ref(true)
const detail = ref(null)
const liked = ref(false)
const likesCount = ref(0)
const commentsCount = ref(0)

// ---------- 评论 ----------
const comments = ref([])
const commentText = ref('')
const replyTarget = ref(null)
const commentsCursor = ref(null)
const commentsHasMore = ref(false)
const loadingComments = ref(false)
const COMMENT_SIZE = 20

// ---------- 作者 ----------
const imageUrl = computed(() => workImageUrl(detail.value.photoAnalysisId))
const authorName = computed(() => {
  const author = detail.value.author
  return author ? author.nickname || author.username : '匿名'
})
const authorAvatar = computed(() => {
  const author = detail.value.author
  if (author && author.avatar) return author.avatar
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=${authorName.value}`
})
const isMyWork = computed(() => {
  return (
    userStore.isLoggedIn &&
    detail.value &&
    detail.value.author &&
    detail.value.author.id === userStore.userInfo.id
  )
})

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/community')
  }
}

const goAuthor = () => {
  router.push(`/user/${detail.value.author.username}`)
}

const pad = (n) => String(n).padStart(2, '0')

/**
 * 后端 LocalDateTime 经 Jackson 序列化成数组 [年,月,日,时,分,秒]
 * （见字典/评论等接口响应 createdAt），同时兼容 ISO 字符串与时间戳。
 */
const formatDate = (date) => {
  if (date === null || date === undefined || date === '') return ''

  if (Array.isArray(date)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = date
    if (!y) return ''
    return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(s)}`
  }

  if (typeof date === 'number') {
    const dt = new Date(date)
    if (Number.isNaN(dt.getTime())) return ''
    return formatDate([dt.getFullYear(), dt.getMonth() + 1, dt.getDate(),
      dt.getHours(), dt.getMinutes(), dt.getSeconds()])
  }

  const str = String(date).substring(0, 19).replace('T', ' ')
  return str
}

// ---------- 点赞 ----------
const toggleLike = async () => {
  if (!(await requireLogin())) return
  try {
    const result = liked.value
      ? await unlikeWork(detail.value.id)
      : await likeWork(detail.value.id)
    liked.value = result.liked
    likesCount.value = result.likesCount
  } catch (error) {
    // 失败提示已由请求拦截器统一弹出
  }
}

// ---------- 评论 ----------
const authorNameOf = (author) => (author ? author.nickname || author.username : '匿名')
const commentAvatar = (author) =>
  author && author.avatar
    ? author.avatar
    : `https://api.dicebear.com/7.x/avataaars/svg?seed=${authorNameOf(author)}`

const loadComments = async () => {
  loadingComments.value = true
  try {
    const page = await getComments(detail.value.id, commentsCursor.value, COMMENT_SIZE)
    comments.value = commentsCursor.value
      ? comments.value.concat(page.items || [])
      : page.items || []
    commentsCursor.value = page.nextCursor
    commentsHasMore.value = !!page.hasMore
  } catch (error) {
    // 评论加载失败不阻塞页面
  } finally {
    loadingComments.value = false
  }
}

const loadMoreComments = () => {
  loadComments()
}

const replyTargetName = computed(() => {
  if (!replyTarget.value) return ''
  const target = comments.value.find((c) => c.id === replyTarget.value)
  return target ? authorNameOf(target.author) : ''
})

const startReply = (comment) => {
  replyTarget.value = comment.id
  commentText.value = ''
}

const cancelReply = () => {
  replyTarget.value = null
}

const submitComment = async () => {
  if (!(await requireLogin())) return
  const content = commentText.value.trim()
  if (!content) return

  try {
    const created = await addComment(detail.value.id, {
      content,
      parentId: replyTarget.value
    })
    comments.value.unshift(created)
    commentsCount.value += 1
    commentText.value = ''
    replyTarget.value = null
    ElMessage.success('评论成功')
  } catch (error) {
    // 失败提示已由请求拦截器统一弹出
  }
}

const canDeleteComment = (comment) => {
  if (!userStore.isLoggedIn || !detail.value || !comment.author) return false
  const myId = userStore.userInfo.id
  return comment.author.id === myId || detail.value.author.id === myId
}

const handleDeleteComment = async (comment) => {
  if (!(await requireLogin())) return
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await deleteComment(comment.id)
    const before = comments.value.length
    comments.value = comments.value.filter((c) => {
      if (c.id === comment.id) return false
      // 删除顶楼评论时，其子回复也会被后端连带删除
      if (comment.parentId == null && c.parentId === comment.id) return false
      return true
    })
    // 本地列表里删掉几条就同步扣几条（与后端删除行数一致）
    commentsCount.value = Math.max(0, commentsCount.value - (before - comments.value.length))
    ElMessage.success('评论已删除')
  } catch (error) {
    // 失败提示已由请求拦截器统一弹出
  }
}

// ---------- 删除作品 ----------
const handleDeleteWork = async () => {
  try {
    await ElMessageBox.confirm(
      '删除后该作品及其点赞、评论将一并清除，确定删除吗？',
      '删除作品',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    await deleteWork(detail.value.id)
    ElMessage.success('作品已删除')
    router.push('/profile')
  } catch (error) {
    // 失败提示已由请求拦截器统一弹出
  }
}

onMounted(async () => {
  try {
    detail.value = await getWorkDetail(workId)
    liked.value = !!detail.value.liked
    likesCount.value = detail.value.likesCount || 0
    commentsCount.value = detail.value.commentsCount || 0
    loadComments()
  } catch (error) {
    detail.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.work-detail-page {
  max-width: 900px;
  margin: 0 auto;
}

.back-link {
  display: inline-block;
  color: #667eea;
  font-size: 14px;
  cursor: pointer;
  margin-bottom: 16px;
}

.back-link:hover {
  text-decoration: underline;
}

.detail-card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  margin-bottom: 24px;
}

.photo-stage {
  background: #101018;
  display: flex;
  justify-content: center;
}

.photo-stage img {
  max-width: 100%;
  max-height: 640px;
  object-fit: contain;
}

.detail-body {
  padding: 24px 28px 28px;
}

.detail-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-title {
  font-size: 24px;
  font-weight: bold;
  color: #222;
  margin: 0 0 10px;
  word-break: break-word;
}

.detail-content {
  font-size: 15px;
  line-height: 1.9;
  color: #444;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0 0 20px;
}

.detail-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  padding-top: 18px;
  border-top: 1px solid #f0f0f0;
}

.author {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  background: #e8ecf3;
  border: 2px solid #667eea;
}

.author-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.author:hover .author-name {
  color: #667eea;
}

.publish-time {
  font-size: 12px;
  color: #999;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 22px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: #f3f4f8;
  color: #555;
  padding: 9px 18px;
  border-radius: 24px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.action-btn:hover {
  background: #e8ecf7;
}

.action-btn.liked {
  background: #ffeef0;
  color: #e5484d;
}

.action-static {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #777;
  font-size: 14px;
}

.action-icon {
  font-size: 16px;
}

.action-count {
  font-weight: 600;
}

/* 评论区 */
.comments-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px 28px 28px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.comments-header h3 {
  margin: 0 0 18px;
  font-size: 18px;
  color: #222;
}

.comments-count {
  color: #999;
  font-weight: 400;
}

.comment-editor {
  margin-bottom: 22px;
}

.comment-textarea {
  width: 100%;
  border: 1px solid #dcdce4;
  border-radius: 10px;
  padding: 12px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.25s ease;
}

.comment-textarea:focus {
  border-color: #667eea;
}

.reply-target {
  font-size: 13px;
  color: #667eea;
  margin: 6px 2px 0;
}

.reply-cancel {
  color: #999;
  cursor: pointer;
  margin-left: 8px;
}

.reply-cancel:hover {
  color: #f56c6c;
}

.editor-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-top: 1px solid #f4f4f6;
}

.comment-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  background: #e8ecf3;
  flex-shrink: 0;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.comment-name {
  font-size: 14px;
  font-weight: 600;
  color: #444;
}

.comment-time {
  font-size: 12px;
  color: #aaa;
  flex-shrink: 0;
}

.comment-content {
  font-size: 15px;
  line-height: 1.7;
  color: #333;
  margin: 6px 0 8px;
  word-break: break-word;
}

.reply-to {
  color: #667eea;
  font-weight: 600;
  margin-right: 2px;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.comment-action {
  border: none;
  background: none;
  color: #999;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

.comment-action:hover {
  color: #667eea;
}

.comment-action.danger:hover {
  color: #f56c6c;
}

.comments-load-more {
  text-align: center;
  padding: 8px 0 4px;
}

@media (max-width: 640px) {
  .detail-body,
  .comments-card {
    padding: 18px;
  }

  .detail-title {
    font-size: 20px;
  }
}
</style>
