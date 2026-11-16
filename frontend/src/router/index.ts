import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/home', name: 'home', component: () => import('@/views/PublicHome.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/Register.vue') },
    {
      path: '/pilot',
      component: () => import('@/views/pilot/PilotLayout.vue'),
      meta: { roles: ['PILOT'] },
      children: [
        { path: '', name: 'pilot-home', component: () => import('@/views/pilot/PilotHome.vue') },
        { path: 'missions', name: 'pilot-missions', component: () => import('@/views/mission/MissionResponse.vue'), props: { role: 'PILOT' } },
      ],
    },
    {
      path: '/org',
      component: () => import('@/views/org/OrgLayout.vue'),
      meta: { roles: ['ORG'] },
      children: [{ path: '', name: 'org-home', component: () => import('@/views/org/OrgHome.vue') },
        { path: 'missions', name: 'org-missions', component: () => import('@/views/mission/MissionResponse.vue'), props: { role: 'ORG' } }],
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { roles: ['ADMIN'] },
      children: [
        { path: '', name: 'admin-dash', component: () => import('@/views/admin/AdminDashboard.vue') },
        { path: 'review', name: 'admin-review', component: () => import('@/views/admin/AdminReview.vue') },
        { path: 'audit', name: 'admin-audit', component: () => import('@/views/admin/AdminAudit.vue') },
        { path: 'config', name: 'admin-config', component: () => import('@/views/admin/AdminConfig.vue') },
        { path: 'missions', name: 'admin-missions', component: () => import('@/views/admin/MissionManage.vue') },
        { path: 'mission-dashboard', name: 'admin-mission-dashboard', component: () => import('@/views/admin/MissionDashboard.vue') },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const need = to.matched.find((r) => r.meta.roles)
  if (!need?.meta?.roles) return true
  const roles = need.meta.roles as string[]
  if (!auth.isLoggedIn) return { name: 'login', query: { redirect: to.fullPath } }
  if (!roles.includes(auth.role || '')) return { name: 'home' }
  return true
})

export default router
