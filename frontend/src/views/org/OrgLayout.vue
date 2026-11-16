<template>
  <el-container class="layout">
    <el-aside width="180px" class="aside">
      <div class="logo">协作单位工作台</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/org">单位资料</el-menu-item>
        <el-menu-item index="/org/missions">应急任务</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <NotificationPanel ref="notificationRef" />
        <el-button link @click="router.push('/home')">首页</el-button>
        <el-button link type="danger" @click="logout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NotificationPanel from '@/components/NotificationPanel.vue'

const router = useRouter()
const auth = useAuthStore()
const notificationRef = ref<InstanceType<typeof NotificationPanel>>()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.aside {
  background: #fff;
  border-right: 1px solid #ebeef5;
}
.logo {
  padding: 16px;
  font-weight: 700;
}
.header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}
</style>
