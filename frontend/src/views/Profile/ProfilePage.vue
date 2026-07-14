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
        <h3 class="card-title">📊 我的数据</h3>
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
        <div class="join-date">
          <span>加入时间：{{ statistics.joinDate || '未知' }}</span>
        </div>
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
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'
import { updateUserInfo, changePassword, getUserStatistics } from '../../api/auth'

const userStore = useUserStore()
const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed='

const isEditing = ref(false)
const isSaving = ref(false)
const isPasswordSaving = ref(false)

const statistics = ref({})

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