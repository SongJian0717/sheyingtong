<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <div class="logo">📸</div>
          <h2>摄影通</h2>
          <p>AI驱动的摄影学习平台</p>
        </div>
        
        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="login-form">
          <el-form-item prop="username">
            <el-input 
              v-model="loginForm.username" 
              placeholder="用户名" 
              prefix-icon="User"
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="密码" 
              prefix-icon="Lock"
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          
          <el-form-item>
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <span class="forgot-password" @click="showForgotDialog = true">忘记密码？</span>
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              class="login-btn"
              :loading="isLoading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="login-footer">
          <p>还没有账号？<span @click="$router.push('/register')" class="register-link">立即注册</span></p>
        </div>
      </div>
    </div>
    
    <el-dialog v-model="showForgotDialog" title="找回密码" width="400px">
      <el-form :model="forgotForm" :rules="forgotRules" ref="forgotFormRef">
        <el-form-item prop="email">
          <el-input 
            v-model="forgotForm.email" 
            placeholder="请输入注册邮箱" 
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForgotDialog = false">取消</el-button>
        <el-button type="primary" @click="handleForgot">发送验证码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../../api/auth'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const forgotFormRef = ref(null)
const isLoading = ref(false)
const showForgotDialog = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rememberMe = ref(false)

const forgotForm = reactive({
  email: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度为6-30个字符', trigger: 'blur' }
  ]
}

const forgotRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  const valid = await loginFormRef.value.validate()
  if (!valid) return
  
  isLoading.value = true
  
  try {
    const response = await login(loginForm.username, loginForm.password)
    
    if (response.token) {
      userStore.login(response.token, response.user)
      
      if (rememberMe.value) {
        localStorage.setItem('rememberMe', 'true')
        localStorage.setItem('username', loginForm.username)
      } else {
        localStorage.removeItem('rememberMe')
        localStorage.removeItem('username')
      }
      
      ElMessage.success('登录成功')
      setTimeout(() => {
        router.push('/')
      }, 1000)
    } else {
      ElMessage.error('登录失败，请检查用户名和密码')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    isLoading.value = false
  }
}

const handleForgot = async () => {
  if (!forgotFormRef.value) return
  
  const valid = await forgotFormRef.value.validate()
  if (!valid) return
  
  ElMessage.success('验证码已发送到您的邮箱，请查收')
  showForgotDialog.value = false
}

onMounted(() => {
  const savedUsername = localStorage.getItem('username')
  const savedRememberMe = localStorage.getItem('rememberMe')
  
  if (savedRememberMe === 'true' && savedUsername) {
    loginForm.username = savedUsername
    rememberMe.value = true
  }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 420px;
}

.login-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  font-size: 48px;
  margin-bottom: 10px;
}

.login-header h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 5px;
}

.login-header p {
  color: #999;
  font-size: 14px;
}

.login-form {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

.forgot-password {
  float: right;
  color: #667eea;
  cursor: pointer;
  font-size: 13px;
}

.forgot-password:hover {
  text-decoration: underline;
}

.login-footer {
  text-align: center;
}

.login-footer p {
  color: #666;
  font-size: 14px;
}

.register-link {
  color: #667eea;
  cursor: pointer;
  font-weight: 500;
}

.register-link:hover {
  text-decoration: underline;
}
</style>
