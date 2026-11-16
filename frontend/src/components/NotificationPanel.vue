<template>
  <div class="notification-wrapper" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
    <div class="bell-icon" @click="toggleDropdown">
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
      </svg>
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </div>

    <transition name="dropdown">
      <div v-if="showDropdown" class="notification-dropdown">
        <div class="dropdown-header">
          <span class="dropdown-title">通知中心</span>
          <span class="dropdown-count" v-if="unreadCount > 0">{{ unreadCount }} 条未读</span>
          <el-button v-if="unreadCount > 0" size="small" text type="primary" @click="markAllRead">全部已读</el-button>
        </div>

        <div class="dropdown-body" v-if="!notifLoading && notifications.length > 0">
          <div
            v-for="n in notifications.slice(0, 10)"
            :key="n.id"
            class="notif-item"
            :class="{ unread: !n.readFlag }"
            @click="handleNotification(n)"
          >
            <div class="notif-dot" :class="getNotifClass(n)"></div>
            <div class="notif-body">
              <div class="notif-title">{{ n.title }}</div>
              <div class="notif-desc" v-if="n.content">{{ n.content }}</div>
              <div class="notif-meta">
                <span class="notif-time">{{ formatTime(n.createdAt) }}</span>
                <span v-if="n.targetType === 'MISSION'" class="notif-link">查看详情 ›</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="notifLoading" class="dropdown-loading">
          <el-icon class="is-loading" :size="20"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <div v-if="!notifLoading && notifications.length === 0" class="dropdown-empty">
          <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="#c0c4cc" stroke-width="1.5">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
          <span>暂无通知</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import http from '@/api/http'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const router = useRouter()
const showDropdown = ref(false)
const unreadCount = ref(0)
const notifications = ref<any[]>([])
const notifLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

function handleMouseEnter() {
  showDropdown.value = true
  if (notifications.value.length === 0) {
    loadNotifications()
  }
}

function handleMouseLeave() {
  showDropdown.value = false
}

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) loadNotifications()
}

function getNotifClass(n: any) {
  if (n.type === 'MISSION') return 'dot-mission'
  if (n.type === 'ALERT') return 'dot-alert'
  return 'dot-info'
}

async function loadNotifications() {
  notifLoading.value = true
  try {
    const res = await http.get('/api/notifications')
    notifications.value = res || []
    updateUnreadCount()
  } catch { /* ignore */ } finally {
    notifLoading.value = false
  }
}

async function loadUnreadCount() {
  try {
    const res = await http.get('/api/notifications/unread-count')
    unreadCount.value = res || 0
  } catch { /* ignore */ }
}

function updateUnreadCount() {
  unreadCount.value = notifications.value.filter(n => !n.readFlag).length
}

async function markAsRead(id: number) {
  try {
    await http.put(`/api/notifications/${id}/read`)
    const n = notifications.value.find(item => item.id === id)
    if (n) n.readFlag = true
    updateUnreadCount()
  } catch { /* ignore */ }
}

async function markAllRead() {
  try {
    await http.put('/api/notifications/read-all')
    notifications.value.forEach(n => n.readFlag = true)
    updateUnreadCount()
    ElMessage.success('已全部标记为已读')
  } catch { /* ignore */ }
}

function handleNotification(n: any) {
  if (!n.readFlag) markAsRead(n.id)
  showDropdown.value = false
  if (n.targetType === 'MISSION' && n.targetId) {
    if (window.location.pathname.includes('/pilot')) {
      router.push('/pilot/missions')
    } else if (window.location.pathname.includes('/org')) {
      router.push('/org/missions')
    } else if (window.location.pathname.includes('/admin')) {
      router.push('/admin/missions')
    }
  }
}

function formatTime(dt: string) {
  if (!dt) return '-'
  const date = new Date(dt)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return dt.substring(5, 16).replace('T', ' ')
}

onMounted(() => {
  loadUnreadCount()
  refreshTimer = setInterval(loadUnreadCount, 15000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})

defineExpose({ refresh: loadUnreadCount })
</script>

<style scoped>
.notification-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.bell-icon {
  position: relative;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: background-color 0.2s;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bell-icon:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.badge {
  position: absolute;
  top: 0;
  right: 0;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: #f56c6c;
  border-radius: 8px;
  text-align: center;
  transform: translate(30%, -20%);
  animation: badge-pulse 2s infinite;
}

@keyframes badge-pulse {
  0%, 100% { transform: translate(30%, -20%) scale(1); }
  50% { transform: translate(30%, -20%) scale(1.1); }
}

.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  width: 380px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #ebeef5;
  z-index: 2000;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f2f5;
  background: #fafbfc;
}

.dropdown-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.dropdown-count {
  margin-left: 8px;
  font-size: 12px;
  color: #f56c6c;
  background: #fef0f0;
  padding: 2px 8px;
  border-radius: 10px;
}

.dropdown-header .el-button {
  margin-left: auto;
}

.dropdown-body {
  max-height: 400px;
  overflow-y: auto;
}

.dropdown-body::-webkit-scrollbar {
  width: 4px;
}

.dropdown-body::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  padding: 14px 16px;
  border-bottom: 1px solid #f5f7fa;
  cursor: pointer;
  transition: background-color 0.15s;
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-item:hover {
  background-color: #f5f9ff;
}

.notif-item.unread {
  background-color: #f0f7ff;
}

.notif-item.unread:hover {
  background-color: #e8f2ff;
}

.notif-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  margin-right: 12px;
  flex-shrink: 0;
}

.dot-mission { background: #409eff; box-shadow: 0 0 6px rgba(64, 158, 255, 0.4); }
.dot-alert { background: #f56c6c; box-shadow: 0 0 6px rgba(245, 108, 108, 0.4); }
.dot-info { background: #67c23a; box-shadow: 0 0 6px rgba(103, 194, 58, 0.4); }

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
}

.notif-desc {
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.notif-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notif-time {
  font-size: 11px;
  color: #909399;
}

.notif-link {
  font-size: 11px;
  color: #409eff;
  cursor: pointer;
}

.dropdown-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px;
  color: #909399;
  font-size: 13px;
}

.dropdown-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px;
  color: #909399;
  font-size: 13px;
}

.dropdown-enter-active {
  transition: all 0.2s ease-out;
}

.dropdown-leave-active {
  transition: all 0.15s ease-in;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
