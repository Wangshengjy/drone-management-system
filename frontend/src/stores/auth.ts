import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const username = ref<string | null>(localStorage.getItem('username'))
  const role = ref<string | null>(localStorage.getItem('role'))
  const pilotId = ref<string | null>(localStorage.getItem('pilotId'))
  const orgId = ref<string | null>(localStorage.getItem('orgId'))

  const isLoggedIn = computed(() => !!token.value)

  function setSession(data: {
    token: string
    username: string
    role: string
    pilotId?: number | null
    orgId?: number | null
  }) {
    token.value = data.token
    username.value = data.username
    role.value = data.role
    pilotId.value = data.pilotId != null ? String(data.pilotId) : null
    orgId.value = data.orgId != null ? String(data.orgId) : null
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
    if (data.pilotId != null) localStorage.setItem('pilotId', String(data.pilotId))
    else localStorage.removeItem('pilotId')
    if (data.orgId != null) localStorage.setItem('orgId', String(data.orgId))
    else localStorage.removeItem('orgId')
  }

  function logout() {
    token.value = null
    username.value = null
    role.value = null
    pilotId.value = null
    orgId.value = null
    localStorage.clear()
  }

  return { token, username, role, pilotId, orgId, isLoggedIn, setSession, logout }
})
