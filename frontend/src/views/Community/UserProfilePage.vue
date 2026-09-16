<template>
  <div class="user-profile-page" v-loading="loading">
    <template v-if="profile">
      <!-- 用户头卡 -->
      <div class="profile-hero">
        <img :src="avatarUrl" class="hero-avatar" alt="头像" />
        <div class="hero-info">
          <div class="hero-name-row">
            <h2 class="hero-name">{{ displayName }}</h2>
            <span v-if="isMe" class="me-tag">我</span>
          </div>
          <p class="hero-username">@{{ profile.username }}</p>
          <p class="hero-joined">加入时间：{{ profile.joinedDate }}</p>
        </div>

        <div class="hero-actions">
          <el-button v-if="isMe" type="primary" round @click="$router.push('/profile')">
            编辑资料
          </el-button>
          <el-button
            v-else
            :type="followedByMe ? 'default' : 'primary'"
            :plain="followedByMe"
            round
            :loading="following"
            @click="toggleFollow"
          >
            {{ followedByMe ? '已关注' : '＋ 关注' }}
          </el-button>
        </div>
      </div>

      <!-- 数据条 -->
      <div class="profile-stats">
        <div class="stat-block" :class="{ active: activeTab === 'works' }" @click="switchTab('works')">
          <div class="stat-num">{{ profile.worksCount }}</div>
          <div class="stat-label">作品</div>
        </div>
        <div class="stat-block" :class="{ active: activeTab === 'followers' }" @click="switchTab('followers')">
          <div class="stat-num">{{ profile.followersCount }}</div>
          <div class="stat-label">粉丝</div>
        </div>
        <div class="stat-block" :class="{ active: activeTab === 'following' }" @click="switchTab('following')">
          <div class="stat-num">{{ profile.followingCount }}</div>
          <div class="stat-label">关注</div>
        </div>
      </div>

      <!-- Tab 内容 -->
      <div class="profile-tabs">
        <!-- 作品墙 -->
        <div v-if="activeTab === 'works'">
          <WorkGrid
            :works="works"
            :has-more="worksHasMore"
            :loading="worksLoading"
            empty-text="Ta 还没有发布公开作品"
            @load-more="loadMoreWorks"
          />
        </div>

        <!-- 粉丝 / 关注列表 -->
        <div v-else-if="activeTab === 'followers' || activeTab === 'following'">
          <div v-if="relationList.length" class="relation-list">
            <div
              v-for="person in relationList"
              :key="person.id"
              class="relation-item"
              @click="$router.push(`/user/${person.username}`)"
            >
              <img :src="relationAvatar(person)" class="relation-avatar" alt="头像" />
              <div class="relation-info">
                <span class="relation-name">{{ person.nickname || person.username }}</span>
                <span class="relation-username">@{{ person.username }}</span>
              </div>
              <el-icon class="relation-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
          <div v-else-if="!relationLoading">
            <el-empty
              :description="activeTab === 'followers' ? '还没有粉丝' : '还没有关注任何人'"
              :image-size="90"
            />
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="用户不存在或已被删除" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getUserProfileByUsername,
  getUserWorks,
  getUserFollowers,
  getUserFollowing,
  followUser,
  unfollowUser
} from '../../api/user'
import { useUserStore } from '../../stores/user'
import { requireLogin } from '../../utils/requireLogin'
import WorkGrid from '../../components/Community/WorkGrid.vue'

const route = useRoute()
const userStore = useUserStore()

const PAGE_SIZE = 12

const loading = ref(true)
const profile = ref(null)
const followedByMe = ref(false)
const following = ref(false)

const activeTab = ref('works')

// 作品墙
const works = ref([])
const worksCursor = ref(null)
const worksHasMore = ref(false)
const worksLoading = ref(false)

// 粉丝 / 关注
const relationList = ref([])
const relationLoading = ref(false)

const displayName = computed(() => profile.value.nickname || profile.value.username)
const avatarUrl = computed(() => {
  if (profile.value.avatar) return profile.value.avatar
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=${profile.value.username}`
})
const isMe = computed(() => {
  return userStore.isLoggedIn && profile.value.id === userStore.userInfo.id
})

const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'followers' && !relationLoaded('followers')) {
    loadRelations('followers')
  } else if (tab === 'following' && !relationLoaded('following')) {
    loadRelations('following')
  }
}

const relationLoadedMap = {
  followers: false,
  following: false
}

const relationLoaded = (key) => relationLoadedMap[key]

const loadRelations = async (key) => {
  if (relationLoading.value) return
  relationLoading.value = true
  try {
    if (key === 'followers') {
      relationList.value = await getUserFollowers(profile.value.id)
    } else {
      relationList.value = await getUserFollowing(profile.value.id)
    }
    relationLoadedMap[key] = true
  } catch (error) {
    relationList.value = []
    // 失败提示已由请求拦截器统一弹出
  } finally {
    relationLoading.value = false
  }
}

const relationAvatar = (person) =>
  person.avatar
    ? person.avatar
    : `https://api.dicebear.com/7.x/avataaars/svg?seed=${person.username}`

// ---------- 作品墙 ----------
const loadFirstWorks = async () => {
  worksLoading.value = true
  try {
    const page = await getUserWorks(profile.value.id, null, PAGE_SIZE)
    works.value = page.items || []
    worksCursor.value = page.nextCursor
    worksHasMore.value = !!page.hasMore
  } catch (error) {
    works.value = []
    worksHasMore.value = false
  } finally {
    worksLoading.value = false
  }
}

const loadMoreWorks = async () => {
  if (worksLoading.value || !worksHasMore.value) return
  worksLoading.value = true
  try {
    const page = await getUserWorks(profile.value.id, worksCursor.value, PAGE_SIZE)
    works.value = works.value.concat(page.items || [])
    worksCursor.value = page.nextCursor
    worksHasMore.value = !!page.hasMore
  } catch (error) {
    // 失败提示已由请求拦截器统一弹出
  } finally {
    worksLoading.value = false
  }
}

// ---------- 关注 ----------
const toggleFollow = async () => {
  if (!(await requireLogin())) return
  if (isMe.value) return

  following.value = true
  try {
    if (followedByMe.value) {
      const result = await unfollowUser(profile.value.id)
      followedByMe.value = result.following
      profile.value.followersCount = result.followersCount
    } else {
      const result = await followUser(profile.value.id)
      followedByMe.value = result.following
      profile.value.followersCount = result.followersCount
    }
    ElMessage.success(followedByMe.value ? '关注成功' : '已取消关注')
  } catch (error) {
    // 失败提示已由请求拦截器统一弹出
  } finally {
    following.value = false
  }
}

// ---------- 加载 ----------
const load = async () => {
  loading.value = true
  profile.value = null
  activeTab.value = 'works'
  works.value = []
  relationList.value = []
  relationLoadedMap.followers = false
  relationLoadedMap.following = false

  try {
    profile.value = await getUserProfileByUsername(route.params.username)
    followedByMe.value = !!profile.value.followedByMe
    loadFirstWorks()
  } catch (error) {
    profile.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.username,
  () => {
    load()
  }
)

onMounted(load)
</script>

<style scoped>
.user-profile-page {
  max-width: 1000px;
  margin: 0 auto;
}

.profile-hero {
  display: flex;
  align-items: center;
  gap: 24px;
  background: #fff;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.hero-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #667eea;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  flex-shrink: 0;
  background: #e8ecf3;
}

.hero-info {
  flex: 1;
  min-width: 0;
}

.hero-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-name {
  font-size: 24px;
  font-weight: bold;
  color: #222;
  margin: 0;
}

.me-tag {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.hero-username {
  font-size: 14px;
  color: #888;
  margin: 6px 0 4px;
}

.hero-joined {
  font-size: 13px;
  color: #aaa;
  margin: 0;
}

.hero-actions {
  flex-shrink: 0;
}

.profile-stats {
  display: flex;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  margin-bottom: 20px;
}

.stat-block {
  flex: 1;
  text-align: center;
  padding: 18px 0;
  cursor: pointer;
  transition: background 0.2s ease;
  border-bottom: 3px solid transparent;
}

.stat-block:hover {
  background: #f7f8fd;
}

.stat-block.active {
  border-bottom-color: #667eea;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #667eea;
}

.stat-label {
  font-size: 13px;
  color: #888;
  margin-top: 4px;
}

.profile-tabs {
  min-height: 200px;
}

.relation-list {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.relation-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.relation-item:hover {
  background: #f7f8fd;
}

.relation-item + .relation-item {
  border-top: 1px solid #f4f4f6;
}

.relation-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  background: #e8ecf3;
  flex-shrink: 0;
}

.relation-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.relation-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.relation-username {
  font-size: 12px;
  color: #aaa;
}

.relation-arrow {
  color: #ccc;
}

@media (max-width: 640px) {
  .profile-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
