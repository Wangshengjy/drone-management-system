<template>
  <div class="login-page">
    <div class="bg-particles">
      <div v-for="i in 30" :key="i" class="particle" :style="particleStyle(i)"></div>
    </div>
    <div class="bg-grid"></div>

    <div class="login-container">
      <div class="login-left">
        <div class="brand-area">
          <div class="brand-icon">
            <svg viewBox="0 0 64 64" width="64" height="64" fill="none" stroke="#40e0d0" stroke-width="1.5">
              <path d="M32 4L8 20v24l24 16 24-16V20L32 4z"/>
              <path d="M32 4v56M8 20l24 12 24-12M8 44l24-12 24 12"/>
              <circle cx="32" cy="32" r="8" fill="rgba(64,224,208,0.2)" stroke="#40e0d0"/>
            </svg>
          </div>
          <h1 class="brand-title">无人机协作调度平台</h1>
          <p class="brand-subtitle">UAV Collaborative Dispatch Platform</p>
          <div class="brand-features">
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>智能派单 · 精准调度</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>实时监控 · 应急响应</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>多维评估 · 资源优化</span>
            </div>
          </div>
        </div>
        <div class="hex-decoration">
          <svg v-for="h in 6" :key="h" class="hex" :style="hexStyle(h)" viewBox="0 0 100 100" fill="none" stroke="rgba(64,224,208,0.15)" stroke-width="1">
            <polygon points="50,3 95,25 95,75 50,97 5,75 5,25"/>
          </svg>
        </div>
      </div>

      <div class="login-right">
        <div class="login-card">
          <div class="card-glow"></div>
          <h2 class="login-title">系统登录</h2>
          <p class="login-desc">欢迎回来，请输入您的账号信息</p>

          <div class="role-tabs">
            <div
              v-for="r in roles"
              :key="r.key"
              :class="['role-tab', { active: selectedRole === r.key }]"
              @click="selectRole(r.key)"
            >
              <span class="role-icon" v-html="r.icon"></span>
              <span class="role-label">{{ r.label }}</span>
            </div>
          </div>

          <el-form ref="formRef" :model="form" :rules="formRules" @submit.prevent="handleLogin" size="large">
            <el-form-item prop="username">
              <el-input v-model="form.username" :placeholder="usernamePlaceholder" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleLogin" class="login-btn">
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="login-footer">
            <el-button text class="public-home-btn" @click="router.push('/home')">进入公开首页 →</el-button>
            <span class="footer-version">无人机协作调度平台 v1.0</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import http from '@/api/http'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const formRef = ref()
const selectedRole = ref('PILOT')

const roles = [
  { key: 'PILOT', label: '飞手', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>' },
  { key: 'ORG', label: '协作单位', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>' },
  { key: 'ADMIN', label: '管理员', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>' }
]

const usernamePlaceholder = computed(() => {
  const map: Record<string, string> = { PILOT: '请输入飞手用户名', ORG: '请输入单位用户名', ADMIN: '请输入管理员用户名' }
  return map[selectedRole.value] || '请输入用户名'
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function selectRole(key: string) {
  selectedRole.value = key
}

const form = reactive({ username: '', password: '' })

function particleStyle(i: number) {
  const size = Math.random() * 4 + 1
  return {
    width: size + 'px',
    height: size + 'px',
    left: Math.random() * 100 + '%',
    top: Math.random() * 100 + '%',
    animationDelay: Math.random() * 20 + 's',
    animationDuration: (Math.random() * 20 + 15) + 's'
  }
}

function hexStyle(i: number) {
  return {
    left: (10 + Math.random() * 80) + '%',
    top: (10 + Math.random() * 80) + '%',
    width: (30 + Math.random() * 40) + 'px',
    opacity: 0.3 + Math.random() * 0.4,
    animationDelay: Math.random() * 10 + 's'
  }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res: any = await http.post('/api/auth/login', { username: form.username, password: form.password })
    auth.setSession(res)
    ElMessage.success('登录成功')
    const role = auth.role
    if (role === 'ADMIN') router.push('/admin')
    else if (role === 'PILOT') router.push('/pilot')
    else if (role === 'ORG') router.push('/org')
    else router.push('/home')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #050a18;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(64,224,208,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(64,224,208,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
}

.bg-particles {
  position: absolute;
  inset: 0;
}

.particle {
  position: absolute;
  background: rgba(64, 224, 208, 0.4);
  border-radius: 50%;
  animation: float-up linear infinite;
}

@keyframes float-up {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translateY(-100vh) translateX(30px); opacity: 0; }
}

.login-container {
  display: flex;
  width: 960px;
  min-height: 540px;
  border-radius: 20px;
  overflow: hidden;
  background: rgba(10, 20, 40, 0.8);
  border: 1px solid rgba(64, 224, 208, 0.15);
  box-shadow: 0 0 60px rgba(64, 224, 208, 0.08), 0 20px 60px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(20px);
  position: relative;
  z-index: 1;
}

.login-left {
  flex: 1;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  background: linear-gradient(135deg, rgba(10, 30, 60, 0.9), rgba(5, 15, 35, 0.9));
  border-right: 1px solid rgba(64, 224, 208, 0.1);
}

.brand-icon {
  margin-bottom: 20px;
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(90deg, #40e0d0, #60b0ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 8px 0;
  letter-spacing: 2px;
}

.brand-subtitle {
  font-size: 13px;
  color: rgba(140, 170, 210, 0.6);
  letter-spacing: 3px;
  margin: 0 0 36px 0;
  text-transform: uppercase;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(140, 170, 210, 0.7);
  font-size: 14px;
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #40e0d0;
  box-shadow: 0 0 8px rgba(64, 224, 208, 0.5);
}

.hex-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hex {
  position: absolute;
  animation: hex-float 12s ease-in-out infinite alternate;
}

@keyframes hex-float {
  0% { transform: translateY(0) rotate(0deg); }
  100% { transform: translateY(-15px) rotate(10deg); }
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  position: relative;
}

.login-card {
  width: 100%;
  max-width: 360px;
  position: relative;
}

.card-glow {
  position: absolute;
  top: -40px;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(64, 224, 208, 0.08), transparent 70%);
  pointer-events: none;
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  color: #e0e6f0;
  margin: 0 0 8px 0;
}

.login-desc {
  font-size: 13px;
  color: rgba(140, 170, 210, 0.5);
  margin: 0 0 20px 0;
}

.role-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.role-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 4px;
  border-radius: 10px;
  border: 1px solid rgba(64, 224, 208, 0.12);
  background: rgba(15, 25, 50, 0.5);
  cursor: pointer;
  transition: all 0.25s;
  color: rgba(140, 170, 210, 0.6);
}

.role-tab:hover {
  border-color: rgba(64, 224, 208, 0.3);
  background: rgba(15, 25, 50, 0.8);
  color: rgba(200, 220, 240, 0.8);
}

.role-tab.active {
  border-color: rgba(64, 224, 208, 0.5);
  background: rgba(64, 224, 208, 0.08);
  color: #40e0d0;
  box-shadow: 0 0 12px rgba(64, 224, 208, 0.1);
}

.role-tab .role-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.role-tab .role-label {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.login-card :deep(.el-input__wrapper) {
  background: rgba(15, 25, 50, 0.8);
  border: 1px solid rgba(64, 224, 208, 0.15);
  box-shadow: none;
  border-radius: 10px;
}

.login-card :deep(.el-input__wrapper:hover),
.login-card :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(64, 224, 208, 0.4);
  box-shadow: 0 0 12px rgba(64, 224, 208, 0.1);
}

.login-card :deep(.el-input__inner) {
  color: #e0e6f0;
}

.login-card :deep(.el-input__inner::placeholder) {
  color: rgba(140, 170, 210, 0.4);
}

.login-card :deep(.el-input__prefix .el-icon) {
  color: rgba(64, 224, 208, 0.5);
}

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #1a6b6a, #0e4a50);
  border: 1px solid rgba(64, 224, 208, 0.3);
  transition: all 0.3s;
}

.login-btn:hover {
  background: linear-gradient(135deg, #0e4a50, #1a6b4a);
  box-shadow: 0 0 20px rgba(64, 224, 208, 0.2);
  border-color: rgba(64, 224, 208, 0.5);
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.public-home-btn {
  color: rgba(64, 224, 208, 0.7) !important;
  font-size: 13px;
  padding: 4px 0;
}

.public-home-btn:hover {
  color: #40e0d0 !important;
}

.footer-version {
  font-size: 12px;
  color: rgba(140, 170, 210, 0.3);
}
</style>
