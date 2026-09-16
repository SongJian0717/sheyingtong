/**
 * Vue Router 路由配置（SPA 路由入口）
 * ----------------------------------------------------------------
 * 模式：createWebHistory()（HTML5 History API，URL 无 #）
 * 路由 meta 字段：
 *  - requiresAuth: true  →  进入前必须有 token（chat/photo-score/retouch/profile 等）
 *  - layout: 'auth'      →  用 AuthLayout（去掉侧边栏，全屏登录/注册页）
 *  - 无 meta             →  公开页面（首页/词典/教程/作品赏析）
 *
 * 鉴权流程：
 *  beforeEach → 检测 to.meta.requiresAuth → 有 token next() → 没 token 弹登录对话框
 *              对话框点击"去登录"→ router.push /login → 登录成功回跳原目标
 */
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessageBox } from 'element-plus'

let isShowingLoginDialog = false

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/Home/HomePage.vue')
  },
  {
    path: '/dictionary',
    name: 'dictionary',
    component: () => import('../views/Dictionary/DictionaryPage.vue')
  },
  {
    path: '/dictionary/term/:term',
    name: 'term-detail',
    component: () => import('../views/Dictionary/TermDetail.vue')
  },
  {
    path: '/photo-score',
    name: 'photo-score',
    component: () => import('../views/PhotoScore/PhotoScorePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/gear',
    name: 'gear',
    component: () => import('../views/Gear/GearRecommendPage.vue')
  },
  {
    path: '/gear/:id',
    name: 'gear-detail',
    component: () => import('../views/Gear/GearDetail.vue')
  },

  {
    path: '/chat',
    name: 'chat',
    component: () => import('../views/Chat/ChatPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/retouch',
    name: 'retouch',
    component: () => import('../views/Retouch/RetouchPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Auth/LoginPage.vue'),
    meta: { layout: 'auth' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/Auth/RegisterPage.vue'),
    meta: { layout: 'auth' }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/Profile/ProfilePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/tutorials',
    name: 'tutorials',
    component: () => import('../views/Learning/TutorialsPage.vue')
  },
  {
    path: '/gallery',
    name: 'gallery',
    component: () => import('../views/Learning/GalleryPage.vue')
  },
  {
    path: '/community',
    name: 'community',
    component: () => import('../views/Community/CommunityPage.vue')
  },
  {
    path: '/community/work/:id',
    name: 'work-detail',
    component: () => import('../views/Community/WorkDetailPage.vue')
  },
  {
    path: '/user/:username',
    name: 'user-profile',
    component: () => import('../views/Community/UserProfilePage.vue')
  },
  {
    path: '/feed',
    name: 'feed',
    component: () => import('../views/Community/FeedPage.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 路由前置守卫：实现基于 token 的鉴权控制
 * ----------------------------------------------------------------
 * 三条分支：
 *  1) to.meta.requiresAuth && !token → next(false) 阻止跳转 + 弹 ElMessageBox 提示登录
 *     → 点"登录"按钮 → router.push('/login')
 *     → 点"取消" → 留在当前页面
 *  2) to 是 /login 或 /register 但已登录 → 已登录用户没必要再去登录页 → next('/') 跳首页
 *  3) 其他情况 → next() 正常放行
 *
 * isShowingLoginDialog 防止同时弹多个对话框（比如用户连续快速点多个 requiresAuth 链接）
 */
router.beforeEach((to, from, next) => {
  const isLoggedIn = !!localStorage.getItem('token')
  
  if (to.meta.requiresAuth) {
    if (isLoggedIn) {
      next()
    } else {
      next(false)
      if (!isShowingLoginDialog) {
        isShowingLoginDialog = true
        ElMessageBox.confirm(
          '该功能需要登录后才能使用',
          '提示',
          {
            confirmButtonText: '登录',
            cancelButtonText: '取消',
            type: 'info',
            confirmButtonClass: 'login-confirm-btn'
          }
        ).then(() => {
          isShowingLoginDialog = false
          router.push('/login')
        }).catch(() => {
          isShowingLoginDialog = false
        })
      }
    }
  } else if (to.path === '/login' || to.path === '/register') {
    if (isLoggedIn) {
      next('/')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
