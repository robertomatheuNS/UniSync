import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
      { path: 'cadastro', name: 'register', component: () => import('@/views/RegisterView.vue') },
    ],
  },
  {
    path: '/app',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
      { path: 'professor', name: 'teacher', component: () => import('@/views/TeacherView.vue') },
      { path: 'disciplinas', name: 'subjects', component: () => import('@/views/SubjectsView.vue') },
      { path: 'mural', name: 'board', component: () => import('@/views/BoardView.vue') },
      { path: 'kanban', name: 'kanban', component: () => import('@/views/KanbanView.vue') },
    ],
  },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})