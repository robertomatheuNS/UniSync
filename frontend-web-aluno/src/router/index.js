import { createRouter, createWebHistory } from 'vue-router'
import { authUrl } from '@/utils/authUrl'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
      },
      {
        path: 'cadastro',
        name: 'register',
        component: () => import('@/views/RegisterView.vue'),
      },
    ],
  },
  {
    path: '/app',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
      { path: 'professor', name: 'teacher', component: () => import('@/views/TeacherView.vue') },
      { path: 'disciplinas', name: 'subjects', component: () => import('@/views/SubjectsView.vue') },
      { path: 'kanban', name: 'kanban', component: () => import('@/views/KanbanView.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})
router.beforeEach((to) => {
  if (to.path.startsWith('/app') && !window.UNISYNC_SHELL?.accessToken) {
    window.location.replace(authUrl(to.fullPath))
    return false
  }
})
export default router
