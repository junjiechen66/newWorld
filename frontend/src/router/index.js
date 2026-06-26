import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/s/:shareCode',
    name: 'PublicShare',
    component: () => import('@/views/PublicShareView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/calendar',
    children: [
      {
        path: 'calendar',
        name: 'Calendar',
        component: () => import('@/views/CalendarView.vue'),
        meta: { title: '日历视图' }
      },
      {
        path: 'today',
        name: 'Today',
        component: () => import('@/views/TodayView.vue'),
        meta: { title: '今日任务' }
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('@/views/TaskListView.vue'),
        meta: { title: '全部任务' }
      },
      {
        path: 'quadrant',
        name: 'Quadrant',
        component: () => import('@/views/QuadrantView.vue'),
        meta: { title: '四象限' }
      },
      {
        path: 'notes',
        name: 'Notes',
        component: () => import('@/views/NotesView.vue'),
        meta: { title: '笔记' }
      },
      {
        path: 'projects',
        name: 'Projects',
        component: () => import('@/views/ProjectView.vue'),
        meta: { title: '项目总览' }
      },
      {
        path: 'shared-with-me',
        name: 'SharedWithMe',
        component: () => import('@/views/SharedWithMeView.vue'),
        meta: { title: '共享给我' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Token是否已过期
function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp && payload.exp * 1000 < Date.now()
  } catch (e) {
    return true
  }
}

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && (!token || isTokenExpired(token))) {
    localStorage.removeItem('token')
    next('/login')
  } else if (to.path === '/login' && token && !isTokenExpired(token)) {
    next('/')
  } else {
    next()
  }
})

export default router
