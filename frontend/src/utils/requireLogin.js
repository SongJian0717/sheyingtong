import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

let showingLoginDialog = false

/**
 * 公开页面内交互（点赞/评论/关注/发布）前的登录拦截
 * ----------------------------------------------------------------
 * 社区广场/详情/他人主页都是公开路由（router guard 不会拦），但点赞等写操作需要登录。
 * 已登录 → 返回 true 直接放行；未登录 → 弹引导框去 /login，返回 false 中断操作。
 * showingLoginDialog 防止多次点击时弹出一串对话框。
 */
export async function requireLogin() {
  const userStore = useUserStore()
  const router = useRouter()

  if (userStore.isLoggedIn) return true
  if (showingLoginDialog) return false

  showingLoginDialog = true
  try {
    await ElMessageBox.confirm(
      '该操作需要登录后才能使用',
      '提示',
      {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'info',
        confirmButtonClass: 'login-confirm-btn'
      }
    )
    router.push('/login')
    return false
  } catch {
    return false
  } finally {
    showingLoginDialog = false
  }
}
