<template>
  <div class="profile-container">
    <div class="profile-header">
      <div class="avatar-section">
        <div class="avatar-wrapper">
          <img :src="userStore.userInfo.avatar || defaultAvatar" alt="头像" class="avatar" />
          <div class="avatar-upload" @click="triggerAvatarUpload">
            <span>📷</span>
          </div>
          <input type="file" ref="avatarInput" class="avatar-input" accept="image/*" @change="handleAvatarChange" />
        </div>
        <h2 class="username">{{ userStore.userInfo.nickname || userStore.userInfo.username }}</h2>
        <p class="user-id">ID: {{ userStore.userInfo.id }}</p>
      </div>
    </div>

    <div class="profile-content">
      <div class="stats-card">
        <div class="card-header">
          <h3 class="card-title">📊 我的数据</h3>
          <el-button v-if="userStore.isLoggedIn" type="text" @click="goPublicProfile">
            🌐 我的公开主页 →
          </el-button>
        </div>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalPhotos || 0 }}</div>
            <div class="stat-label">上传照片</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalScoreRecords || 0 }}</div>
            <div class="stat-label">照片打分</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalRetouchRecords || 0 }}</div>
            <div class="stat-label">修图建议</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalChatSessions || 0 }}</div>
            <div class="stat-label">聊天会话</div>
          </div>
        </div>
        <div class="community-stats-block">
          <div class="community-stats-label">🌐 社区数据</div>
          <div class="community-stats-grid">
            <div class="stat-item community-stat-item" @click="openMyWorks">
              <div class="stat-value">{{ statistics.worksCount || 0 }}</div>
              <div class="stat-label">📤 我的作品</div>
            </div>
            <div class="stat-item community-stat-item" @click="openFollowers">
              <div class="stat-value">{{ statistics.followersCount || 0 }}</div>
              <div class="stat-label">👥 粉丝</div>
            </div>
            <div class="stat-item community-stat-item" @click="openFollowing">
              <div class="stat-value">{{ statistics.followingCount || 0 }}</div>
              <div class="stat-label">🤝 关注</div>
            </div>
          </div>
        </div>
        <div class="join-date">
          <span>加入时间：{{ statistics.joinDate || '未知' }}</span>
        </div>
      </div>

      <div id="my-works" class="my-works-card" v-loading="loadingMyWorks">
        <div class="card-header">
          <h3 class="card-title">📤 我的作品</h3>
          <span class="my-works-total">共 {{ myWorks.length }} 件</span>
        </div>

        <div v-if="myWorks.length" class="my-works-grid">
          <div v-for="work in myWorks" :key="work.id" class="my-work-item">
            <div class="my-work-thumb" @click="openMyWork(work)">
              <img :src="workImageUrl(work.photoAnalysisId)" :alt="work.title" loading="lazy" />
              <div class="my-work-overlay">
                <span class="thumb-action">查看作品 →</span>
              </div>
            </div>
            <div class="my-work-info">
              <div class="my-work-title" :title="work.title">{{ work.title }}</div>
              <div class="my-work-meta">
                <span>👍 {{ work.likesCount || 0 }}</span>
                <span>💬 {{ work.commentsCount || 0 }}</span>
                <el-popconfirm
                  title="删除后作品及其点赞、评论将一并清除，确定删除吗？"
                  width="260"
                  @confirm="handleDeleteMyWork(work)"
                >
                  <template #reference>
                    <span class="my-work-delete">删除</span>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else-if="!loadingMyWorks" description="还没有发布作品" :image-size="80">
          <el-button type="primary" round @click="$router.push('/photo-score')">
            去「照片打分」发布第一件 →
          </el-button>
        </el-empty>
      </div>

      <div class="info-card">
        <div class="card-header">
          <h3 class="card-title">👤 个人信息</h3>
          <el-button type="text" @click="toggleEditMode">{{ isEditing ? '取消' : '编辑' }}</el-button>
        </div>
        
        <el-form :model="formData" :rules="rules" ref="infoForm" label-width="100px" class="info-form">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="formData.username" disabled />
          </el-form-item>
          
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="formData.nickname" :disabled="!isEditing" />
          </el-form-item>
          
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" :disabled="!isEditing" />
          </el-form-item>
          
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="formData.phone" :disabled="!isEditing" />
          </el-form-item>
          
          <el-form-item v-if="isEditing">
            <el-button type="primary" @click="saveUserInfo" :loading="isSaving">保存</el-button>
            <el-button @click="toggleEditMode">取消</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="password-card">
        <h3 class="card-title">🔐 修改密码</h3>
        <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="120px" class="password-form">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input type="password" v-model="passwordForm.oldPassword" show-password />
          </el-form-item>
          
          <el-form-item label="新密码" prop="newPassword">
            <el-input type="password" v-model="passwordForm.newPassword" show-password />
          </el-form-item>
          
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input type="password" v-model="passwordForm.confirmPassword" show-password />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="savePassword" :loading="isPasswordSaving">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'
import { updateUserInfo, changePassword, getUserStatistics } from '../../api/auth'
import { getMyWorks, deleteWork, workImageUrl } from '../../api/work'

const userStore = useUserStore()
const router = useRouter()
const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed='

const isEditing = ref(false)
const isSaving = ref(false)
const isPasswordSaving = ref(false)

const statistics = ref({})
const myWorks = ref([])
const loadingMyWorks = ref(false)

const formData = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 50, message: '昵称长度为1-50个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { max: 20, message: '手机号长度不能超过20个字符', trigger: 'blur' }
  ]
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 30, message: '新密码长度为6-30个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const infoForm = ref()
const passwordFormRef = ref()
const avatarInput = ref()

const initForm = () => {
  formData.username = userStore.userInfo.username || ''
  formData.nickname = userStore.userInfo.nickname || ''
  formData.email = userStore.userInfo.email || ''
  formData.phone = userStore.userInfo.phone || ''
}

const toggleEditMode = () => {
  isEditing.value = !isEditing.value
  if (!isEditing.value) {
    initForm()
  }
}

const saveUserInfo = async () => {
  if (!infoForm.value) return
  
  await infoForm.value.validate(async (valid) => {
    if (valid) {
      isSaving.value = true
      try {
        const { nickname, email, phone } = formData
        const result = await updateUserInfo({ nickname, email, phone })
        userStore.updateUser(result)
        ElMessage.success('信息更新成功')
        isEditing.value = false
      } catch (error) {
        ElMessage.error(error.message || '更新失败')
      } finally {
        isSaving.value = false
      }
    }
  })
}

const savePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      isPasswordSaving.value = true
      try {
        await changePassword(passwordForm)
        ElMessage.success('密码修改成功')
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
      } catch (error) {
        ElMessage.error(error.message || '密码修改失败')
      } finally {
        isPasswordSaving.value = false
      }
    }
  })
}

const fetchStatistics = async () => {
  try {
    statistics.value = await getUserStatistics()
  } catch (error) {
    console.error('获取统计信息失败:', error)
  }
}

// ---------- 我的作品（社区）----------
const loadMyWorks = async () => {
  loadingMyWorks.value = true
  try {
    myWorks.value = await getMyWorks()
  } catch (error) {
    myWorks.value = []
  } finally {
    loadingMyWorks.value = false
  }
}

const openMyWork = (work) => {
  router.push(`/community/work/${work.id}`)
}

const openMyWorks = () => {
  document.getElementById('my-works')?.scrollIntoView({ behavior: 'smooth' })
}

const goPublicProfile = () => {
  router.push(`/user/${userStore.userInfo.username}`)
}

const openFollowers = () => {
  // 粉丝/关注明细见他人主页的 Tab；自己的粉丝墙可后续在主页扩展
  goPublicProfile()
}

const openFollowing = () => {
  goPublicProfile()
}

const handleDeleteMyWork = async (work) => {
  try {
    await deleteWork(work.id)
    ElMessage.success('作品已删除')
    loadMyWorks()
    fetchStatistics()
  } catch (error) {
    console.error('删除作品失败:', error)
  }
}

const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

const compressImage = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        
        const maxWidth = 500
        const maxHeight = 500
        let width = img.width
        let height = img.height
        
        if (width > height) {
          if (width > maxWidth) {
            height = (height * maxWidth) / width
            width = maxWidth
          }
        } else {
          if (height > maxHeight) {
            width = (width * maxHeight) / height
            height = maxHeight
          }
        }
        
        canvas.width = width
        canvas.height = height
        ctx.drawImage(img, 0, 0, width, height)
        
        const mimeType = file.type || 'image/jpeg'
        canvas.toBlob((blob) => {
          if (blob) {
            resolve(new File([blob], file.name, { type: mimeType }))
          } else {
            reject(new Error('图片压缩失败'))
          }
        }, mimeType, 0.8)
      }
      img.onerror = reject
      img.src = e.target?.result
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const handleAvatarChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }
  
  try {
    const compressedFile = await compressImage(file)
    
    const formData = new FormData()
    formData.append('file', compressedFile)
    
    const uploadResult = await request.post('/api/upload/avatar', formData, {
      headers: { 'Content-Type': undefined }
    })
    
    const avatarUrl = uploadResult.data
    
    const result = await updateUserInfo({ avatar: avatarUrl })
    userStore.updateUser(result)
    ElMessage.success('头像更新成功')
  } catch (error) {
    ElMessage.error(error.message || '头像更新失败')
  }
}

watch(() => userStore.userInfo, () => {
  initForm()
}, { immediate: true, deep: true })

onMounted(() => {
  initForm()
  fetchStatistics()
  loadMyWorks()
})
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
  padding-top: 100px;
}

.profile-header {
  text-align: center;
  margin-bottom: 40px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.avatar-wrapper {
  position: relative;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid #667eea;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.3s ease;
  font-size: 20px;
}

.avatar-wrapper:hover .avatar-upload {
  opacity: 1;
}

.avatar-input {
  display: none;
}

.username {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.user-id {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.stats-card,
.info-card,
.password-card {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.card-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0;
  margin-bottom: 25px;
}

.card-header .card-title {
  margin-bottom: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 25px;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 12px;
}

.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.join-date {
  text-align: center;
  padding-top: 15px;
  border-top: 1px solid #eee;
  font-size: 14px;
  color: #999;
}

.info-form,
.password-form {
  max-width: 500px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  padding: 8px 30px;
}

:deep(.el-button--primary):hover {
  background: linear-gradient(135deg, #5a6fd6 0%, #6a418a 100%);
}

.profile-link {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar-icon {
  font-size: 18px;
}

/* ---------- 社区数据 ---------- */
.community-stats-block {
  margin-top: 10px;
  padding-top: 18px;
  border-top: 1px solid #eee;
}

.community-stats-label {
  font-size: 14px;
  font-weight: 600;
  color: #666;
  margin-bottom: 14px;
}

.community-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-item.community-stat-item {
  cursor: pointer;
  background: linear-gradient(135deg, #eef1ff 0%, #f7efff 100%);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-item.community-stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.2);
}

/* ---------- 我的作品 ---------- */
.my-works-card {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.my-works-total {
  font-size: 13px;
  color: #999;
}

.my-works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 16px;
}

.my-work-item {
  background: #fafbfe;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f0f4;
  transition: box-shadow 0.25s ease;
}

.my-work-item:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
}

.my-work-thumb {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  cursor: pointer;
  background: #e8ecf3;
}

.my-work-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.my-work-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.my-work-thumb:hover .my-work-overlay {
  opacity: 1;
}

.thumb-action {
  color: #fff;
  font-size: 14px;
  padding: 6px 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 20px;
}

.my-work-info {
  padding: 10px 12px;
}

.my-work-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.my-work-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

.my-work-delete {
  margin-left: auto;
  color: #f56c6c;
  cursor: pointer;
}

.my-work-delete:hover {
  text-decoration: underline;
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .username {
    font-size: 22px;
  }
  
  .profile-container {
    padding: 20px;
    padding-top: 100px;
  }
  
  .stats-card,
  .info-card,
  .password-card {
    padding: 20px;
  }
}
</style>