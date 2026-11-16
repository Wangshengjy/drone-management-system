<template>
  <div class="home-page">
    <div class="bg-grid"></div>
    <div class="bg-particles">
      <div v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></div>
    </div>

    <header class="home-header">
      <div class="header-inner">
        <div class="logo-area" @click="router.push('/home')">
          <svg viewBox="0 0 48 48" width="32" height="32" fill="none" stroke="#40e0d0" stroke-width="1.5">
            <path d="M24 4L4 18v16l20 10 20-10V18L24 4z"/>
            <circle cx="24" cy="24" r="6" fill="rgba(64,224,208,0.2)" stroke="#40e0d0"/>
          </svg>
          <span class="logo-text">无人机协作调度平台</span>
        </div>
        <nav class="nav-links">
          <a href="#features">平台功能</a>
          <a href="#query">查询中心</a>
          <a href="#stats">数据概览</a>
        </nav>
        <div class="header-actions">
          <el-button v-if="isAdmin" size="small" @click="router.push('/admin')" class="btn-admin-back">管理员</el-button>
          <el-button v-if="!isLoggedIn" size="small" @click="router.push('/register')" class="btn-register">飞手报名</el-button>
          <el-button v-if="!isLoggedIn" size="small" @click="router.push('/register')" class="btn-register-org">单位报名</el-button>
          <el-button v-if="!isLoggedIn" size="small" @click="router.push('/login')" class="btn-login">登录</el-button>
          <el-button v-if="isLoggedIn && !isAdmin" size="small" @click="handleLogout" class="btn-login">退出</el-button>
        </div>
      </div>
    </header>

    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="title-line">智能调度</span>
          <span class="title-line accent">精准响应</span>
        </h1>
        <p class="hero-desc">基于人工智能的多维度匹配算法，实现应急任务的智能派发与实时调度，构建全方位无人机协作网络</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/register')" class="btn-start">立即报名</el-button>
          <el-button size="large" @click="router.push('/login')" class="btn-learn">登录系统</el-button>
        </div>
      </div>
      <div class="hero-visual">
        <div ref="heroChartRef" class="hero-chart"></div>
      </div>
    </section>

    <section id="features" class="features-section">
      <h2 class="section-title">核心功能</h2>
      <div class="features-grid">
        <div class="feature-card" v-for="f in features" :key="f.title" @click="openFeatureDetail(f)">
          <div class="feature-icon" v-html="f.icon"></div>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
          <div class="feature-more">查看详情 →</div>
        </div>
      </div>
    </section>

    <section id="query" class="query-section">
      <h2 class="section-title">查询中心</h2>
      <template v-if="canQuery">
        <el-tabs v-model="queryTab" class="query-tabs">
          <el-tab-pane label="飞手查询" name="pilots">
            <div class="query-bar">
              <el-input v-model="pilotQuery.keyword" placeholder="搜索飞手姓名/备案号" clearable style="width:240px" @keyup.enter="searchPilots" />
              <el-input v-model="pilotQuery.region" placeholder="地区筛选" clearable style="width:160px" @keyup.enter="searchPilots" />
              <el-button type="primary" @click="searchPilots">搜索</el-button>
            </div>
            <el-table :data="pilots" v-loading="pilotLoading" stripe border style="margin-top:12px">
              <el-table-column prop="fullName" label="姓名" width="100"><template #default="{ row }">{{ row.fullName || '-' }}</template></el-table-column>
              <el-table-column prop="region" label="地区" width="120"><template #default="{ row }">{{ row.region || '-' }}</template></el-table-column>
              <el-table-column prop="certCount" label="资质数" width="80"><template #default="{ row }">{{ row.certCount ?? 0 }}</template></el-table-column>
              <el-table-column prop="status" label="认证状态" width="90">
                <template #default>
                  <span class="verify-badge verified">已认证</span>
                </template>
              </el-table-column>
              <el-table-column prop="filingNo" label="备案号" min-width="140" show-overflow-tooltip><template #default="{ row }">{{ row.filingNo || '-' }}</template></el-table-column>
            </el-table>
            <el-pagination v-if="pilotTotal > pilotQuery.size" :current-page="pilotQuery.page" :page-size="pilotQuery.size" :total="pilotTotal" layout="prev, pager, next" @current-change="(p: number) => { pilotQuery.page = p; searchPilots() }" style="margin-top:12px;justify-content:center" />
          </el-tab-pane>

          <el-tab-pane label="协作单位查询" name="orgs">
            <div class="query-bar">
              <el-input v-model="orgQuery.keyword" placeholder="搜索单位名称" clearable style="width:240px" @keyup.enter="searchOrgs" />
              <el-select v-model="orgQuery.orgType" placeholder="单位类型" clearable style="width:160px" @change="searchOrgs">
                <el-option label="企业" value="ENTERPRISE" />
                <el-option label="志愿者机构" value="VOLUNTEER" />
                <el-option label="其他" value="OTHER" />
              </el-select>
              <el-button type="primary" @click="searchOrgs">搜索</el-button>
            </div>
            <el-table :data="orgs" v-loading="orgLoading" stripe border style="margin-top:12px">
              <el-table-column prop="name" label="单位名称" min-width="160" show-overflow-tooltip><template #default="{ row }">{{ row.name || '-' }}</template></el-table-column>
              <el-table-column prop="orgType" label="类型" width="110">
                <template #default="{ row }">{{ orgTypeLabel(row.orgType) || '-' }}</template>
              </el-table-column>
              <el-table-column prop="contactPerson" label="联系人" width="100"><template #default="{ row }">{{ row.contactPerson || '-' }}</template></el-table-column>
              <el-table-column prop="status" label="认证状态" width="90">
                <template #default>
                  <span class="verify-badge verified">已认证</span>
                </template>
              </el-table-column>
              <el-table-column prop="creditCode" label="信用代码" min-width="160" show-overflow-tooltip><template #default="{ row }">{{ row.creditCode || '-' }}</template></el-table-column>
            </el-table>
            <el-pagination v-if="orgTotal > orgQuery.size" :current-page="orgQuery.page" :page-size="orgQuery.size" :total="orgTotal" layout="prev, pager, next" @current-change="(p: number) => { orgQuery.page = p; searchOrgs() }" style="margin-top:12px;justify-content:center" />
          </el-tab-pane>
        </el-tabs>
      </template>
      <div v-else class="login-prompt">
        <el-alert title="请登录后查看飞手与协作单位信息" type="warning" :closable="false" show-icon style="margin-bottom: 16px" />
        <div class="login-prompt-actions">
          <el-button type="primary" @click="router.push('/login')">登录</el-button>
          <el-button @click="router.push('/register')">注册</el-button>
        </div>
      </div>
    </section>

    <section id="stats" class="stats-section">
      <h2 class="section-title">平台数据</h2>
      <template v-if="canQuery">
        <div class="stats-grid">
          <div class="stat-card" v-for="s in statItems" :key="s.label">
            <div class="stat-value">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
        <div ref="statsChartRef" class="stats-chart"></div>
      </template>
      <div v-else class="login-prompt">
        <el-alert title="请登录后查看平台数据" type="warning" :closable="false" show-icon style="margin-bottom: 16px" />
        <div class="login-prompt-actions">
          <el-button type="primary" @click="router.push('/login')">登录</el-button>
          <el-button @click="router.push('/register')">注册</el-button>
        </div>
      </div>
    </section>

    <section class="register-section">
      <h2 class="section-title">加入我们</h2>
      <div class="register-cards">
        <div class="reg-card pilot-card" @click="router.push('/register')">
          <div class="reg-icon">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="#40e0d0" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
          <h3>飞手报名</h3>
          <p>注册成为无人机飞手，接收应急任务派发，参与救援行动</p>
          <el-button type="primary" class="reg-btn">立即报名</el-button>
        </div>
        <div class="reg-card org-card" @click="router.push('/register')">
          <div class="reg-icon">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="#60b0ff" stroke-width="1.5"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          </div>
          <h3>协作单位报名</h3>
          <p>注册成为协作单位，参与应急调度，提供专业救援支持</p>
          <el-button type="primary" class="reg-btn">立即报名</el-button>
        </div>
        <div class="reg-card guest-card" @click="router.push('/login')">
          <div class="reg-icon">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="#8caad2" stroke-width="1.5"><path d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
          </div>
          <h3>登录系统</h3>
          <p>登录后查看飞手与协作单位信息，使用完整平台功能</p>
          <el-button class="reg-btn">立即登录</el-button>
        </div>
      </div>
    </section>

    <footer class="home-footer">
      <span>无人机协作调度平台 v1.0 · UAV Collaborative Dispatch Platform</span>
    </footer>

    <el-dialog v-model="featureDetailVisible" :title="featureDetail?.title" width="600px" destroy-on-close>
      <div class="feature-detail" v-if="featureDetail">
        <div class="fd-icon" v-html="featureDetail.icon"></div>
        <h3>{{ featureDetail.title }}</h3>
        <p class="fd-desc">{{ featureDetail.fullDesc }}</p>
        <div class="fd-highlights" v-if="featureDetail.highlights?.length">
          <div class="fd-hl-item" v-for="h in featureDetail.highlights" :key="h">
            <div class="fd-hl-dot"></div>
            <span>{{ h }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import * as echarts from 'echarts'
import http from '@/api/http'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const isLoggedIn = computed(() => !!auth.token)
const canQuery = computed(() => isLoggedIn.value)
const isAdmin = computed(() => auth.role === 'ADMIN')
const heroChartRef = ref<HTMLElement>()
const statsChartRef = ref<HTMLElement>()
let heroChart: echarts.ECharts | null = null
let statsChart: echarts.ECharts | null = null

const queryTab = ref('pilots')
const pilots = ref<any[]>([])
const pilotLoading = ref(false)
const pilotTotal = ref(0)
const pilotQuery = ref({ keyword: '', region: '', page: 1, size: 10 })

const orgs = ref<any[]>([])
const orgLoading = ref(false)
const orgTotal = ref(0)
const orgQuery = ref({ keyword: '', orgType: '', page: 1, size: 10 })

const featureDetailVisible = ref(false)
const featureDetail = ref<any>(null)

const features = [
  { title: '智能派单', desc: '多维度评分算法，自动匹配最优飞手与协作单位', icon: '<svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#40e0d0" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>', fullDesc: '智能派单系统基于多维度评分算法，综合考虑距离、能力、状态和负载均衡等因素，为每个应急任务自动匹配最优的飞手和协作单位。系统支持红色/黄色/蓝色三级紧急程度，不同级别设定不同的响应时限。', highlights: ['Haversine距离计算，优先就近派发', '设备能力与资质自动匹配校验', '实时状态感知，优先空闲飞手', '负载均衡算法，避免疲劳作业', '超时熔断机制，自动顺位派发'] },
  { title: '实时监控', desc: '应急资源热力图实时展示飞手分布与任务态势', icon: '<svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#40e0d0" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>', fullDesc: '调度大屏基于ECharts和中国地图GeoJSON，实现应急资源态势的实时可视化。热力图展示飞手分布密度，涟漪效果标记任务位置，菱形标记协作单位覆盖范围。', highlights: ['中国地图底图，支持缩放拖拽', '飞手分布热力图叠加', '任务涟漪效果标记（按紧急程度着色）', '协作单位菱形标记与覆盖范围', '实时数据刷新，态势感知'] },
  { title: '应急响应', desc: '强提醒与超时熔断策略，确保黄金时间响应', icon: '<svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#40e0d0" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>', fullDesc: '任务派发后，系统自动向所有相关飞手和协作单位发送通知提醒。飞手/单位需在规定时间内确认接单，超时未确认则系统自动顺位派给下一个候选。红色紧急5分钟、黄色15分钟、蓝色30分钟。', highlights: ['实时通知推送，铃铛图标提醒', '分级响应时限（5/15/30分钟）', '超时自动熔断，顺位派发', '接单确认二次弹窗防误操作', '现场反馈与照片上传'] },
  { title: '流程追踪', desc: '从发单到完成的全流程可视化追踪', icon: '<svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#40e0d0" stroke-width="1.5"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>', fullDesc: '任务全流程可视化追踪系统，从管理员发布需求、系统智能派单、飞手/单位确认接单、出发作业到最终完成，每个环节都有详细日志记录。调度大屏可查看每个任务的完成流程与内容详情。', highlights: ['五步流程可视化（发布→派单→接单→作业→完成）', '操作日志时间线记录', '调度大屏流程追踪面板', '任务详情弹窗含完整流程与日志', '状态变更实时更新'] }
]

const statItems = [
  { value: '6+', label: '注册飞手' },
  { value: '6+', label: '协作单位' },
  { value: '10+', label: '应急任务' },
  { value: '24/7', label: '全天候响应' }
]

function statusLabel(s: string) { const m: Record<string, string> = { DRAFT: '草稿', PENDING_REVIEW: '审核中', APPROVED: '已通过', REJECTED: '已驳回', FROZEN: '已冻结' }; return m[s] || '未知' }
function orgTypeLabel(t: string) { const m: Record<string, string> = { ENTERPRISE: '企业', VOLUNTEER: '志愿者机构', OTHER: '其他' }; return m[t] || '未知' }

function particleStyle(i: number) {
  const size = Math.random() * 3 + 1
  return { width: size + 'px', height: size + 'px', left: Math.random() * 100 + '%', top: Math.random() * 100 + '%', animationDelay: Math.random() * 20 + 's', animationDuration: (Math.random() * 20 + 15) + 's' }
}

function openFeatureDetail(f: any) {
  featureDetail.value = f
  featureDetailVisible.value = true
}

async function searchPilots() {
  pilotLoading.value = true
  try {
    const params: any = { page: pilotQuery.value.page, size: pilotQuery.value.size }
    if (pilotQuery.value.keyword) params.keyword = pilotQuery.value.keyword
    if (pilotQuery.value.region) params.region = pilotQuery.value.region
    const res = await http.get('/api/public/pilots', { params })
    pilots.value = res?.records || []
    pilotTotal.value = res?.total || 0
  } catch { pilots.value = [] } finally { pilotLoading.value = false }
}

async function searchOrgs() {
  orgLoading.value = true
  try {
    const params: any = { page: orgQuery.value.page, size: orgQuery.value.size }
    if (orgQuery.value.keyword) params.keyword = orgQuery.value.keyword
    if (orgQuery.value.orgType) params.orgType = orgQuery.value.orgType
    const res = await http.get('/api/public/organizations', { params })
    orgs.value = res?.records || []
    orgTotal.value = res?.total || 0
  } catch { orgs.value = [] } finally { orgLoading.value = false }
}

function handleLogout() {
  auth.logout()
  ElMessage.success('已退出登录')
}

function initHeroChart() {
  if (!heroChartRef.value) return
  heroChart = echarts.init(heroChartRef.value)
  const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
  heroChart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 20, right: 20, bottom: 30, left: 40 },
    xAxis: { type: 'category', data: hours, axisLine: { lineStyle: { color: '#1a3a5c' } }, axisLabel: { color: '#5a7a9a', fontSize: 9 } },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#0f1f33' } }, axisLabel: { color: '#5a7a9a', fontSize: 9 } },
    series: [
      { type: 'line', data: [3, 2, 1, 0, 1, 2, 5, 8, 12, 15, 10, 8, 6, 9, 14, 18, 12, 8, 5, 3, 2, 1, 2, 4], smooth: true, symbol: 'none', lineStyle: { color: '#40e0d0', width: 2 }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64,224,208,0.3)' }, { offset: 1, color: 'rgba(64,224,208,0)' }]) } },
      { type: 'line', data: [1, 0, 0, 0, 0, 1, 3, 5, 8, 10, 7, 5, 4, 6, 9, 12, 8, 5, 3, 2, 1, 0, 1, 2], smooth: true, symbol: 'none', lineStyle: { color: '#60b0ff', width: 2 }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(96,176,255,0.2)' }, { offset: 1, color: 'rgba(96,176,255,0)' }]) } }
    ]
  })
}

function initStatsChart() {
  if (!statsChartRef.value) return
  statsChart = echarts.init(statsChartRef.value)
  statsChart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 20, right: 20, bottom: 30, left: 50 },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'], axisLine: { lineStyle: { color: '#1a3a5c' } }, axisLabel: { color: '#5a7a9a' } },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#0f1f33' } }, axisLabel: { color: '#5a7a9a' } },
    series: [
      { type: 'bar', data: [12, 18, 25, 20, 32, 28], itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#40e0d0' }, { offset: 1, color: '#0e4a50' }]), borderRadius: [4, 4, 0, 0] }, barWidth: '40%' },
      { type: 'bar', data: [8, 12, 15, 14, 22, 18], itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#60b0ff' }, { offset: 1, color: '#1a3a5c' }]), borderRadius: [4, 4, 0, 0] }, barWidth: '40%' }
    ]
  })
}

function handleResize() { heroChart?.resize(); statsChart?.resize() }

onMounted(() => {
  initHeroChart()
  if (isLoggedIn.value) {
    initStatsChart()
    searchPilots()
    searchOrgs()
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  heroChart?.dispose()
  statsChart?.dispose()
})
</script>

<style scoped>
.home-page { background: #050a18; min-height: 100vh; position: relative; overflow-x: hidden; color: #c0cde0; }
.bg-grid { position: fixed; inset: 0; background-image: linear-gradient(rgba(64,224,208,0.02) 1px, transparent 1px), linear-gradient(90deg, rgba(64,224,208,0.02) 1px, transparent 1px); background-size: 60px 60px; pointer-events: none; z-index: 0; }
.bg-particles { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.particle { position: absolute; background: rgba(64, 224, 208, 0.3); border-radius: 50%; animation: float-up linear infinite; }
@keyframes float-up { 0% { transform: translateY(0); opacity: 0; } 10% { opacity: 1; } 90% { opacity: 1; } 100% { transform: translateY(-100vh); opacity: 0; } }

.home-header { position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: rgba(5, 10, 24, 0.85); backdrop-filter: blur(16px); border-bottom: 1px solid rgba(64, 224, 208, 0.08); }
.header-inner { max-width: 1200px; margin: 0 auto; padding: 0 24px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.logo-area { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.logo-text { font-size: 16px; font-weight: 700; background: linear-gradient(90deg, #40e0d0, #60b0ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.nav-links { display: flex; gap: 24px; }
.nav-links a { color: rgba(140, 170, 210, 0.7); text-decoration: none; font-size: 14px; transition: color 0.2s; }
.nav-links a:hover { color: #40e0d0; }
.header-actions { display: flex; gap: 8px; }
.btn-register { background: rgba(64, 224, 208, 0.1) !important; border: 1px solid rgba(64, 224, 208, 0.3) !important; color: #40e0d0 !important; border-radius: 6px !important; }
.btn-register:hover { background: rgba(64, 224, 208, 0.2) !important; }
.btn-register-org { background: rgba(96, 176, 255, 0.1) !important; border: 1px solid rgba(96, 176, 255, 0.3) !important; color: #60b0ff !important; border-radius: 6px !important; }
.btn-register-org:hover { background: rgba(96, 176, 255, 0.2) !important; }
.btn-login { background: transparent; border: 1px solid rgba(64, 224, 208, 0.3); color: #40e0d0; border-radius: 6px; }
.btn-login:hover { background: rgba(64, 224, 208, 0.1); border-color: rgba(64, 224, 208, 0.5); }
.btn-admin-back { background: linear-gradient(135deg, #40e0d0, #20b2aa); border: none; color: #0a1628; font-weight: 600; border-radius: 6px; }
.btn-admin-back:hover { background: linear-gradient(135deg, #5ef5e5, #40e0d0); }

.hero { max-width: 1200px; margin: 0 auto; padding: 120px 24px 60px; display: flex; align-items: center; gap: 40px; position: relative; z-index: 1; }
.hero-content { flex: 1; }
.hero-title { font-size: 48px; font-weight: 800; line-height: 1.2; margin: 0 0 20px 0; }
.title-line { display: block; color: #e0e6f0; }
.title-line.accent { background: linear-gradient(90deg, #40e0d0, #60b0ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.hero-desc { font-size: 16px; color: rgba(140, 170, 210, 0.6); line-height: 1.8; margin: 0 0 32px 0; max-width: 480px; }
.hero-actions { display: flex; gap: 16px; }
.btn-start { background: linear-gradient(135deg, #1a6b6a, #0e4a50) !important; border: 1px solid rgba(64, 224, 208, 0.3) !important; border-radius: 10px !important; font-weight: 600; letter-spacing: 2px; }
.btn-start:hover { box-shadow: 0 0 20px rgba(64, 224, 208, 0.2) !important; }
.btn-learn { background: transparent !important; border: 1px solid rgba(64, 224, 208, 0.2) !important; color: #8caad2 !important; border-radius: 10px !important; }
.hero-visual { flex: 1; }
.hero-chart { height: 300px; }

.section-title { font-size: 28px; font-weight: 700; text-align: center; margin: 0 0 40px 0; background: linear-gradient(90deg, #40e0d0, #60b0ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }

.features-section { max-width: 1200px; margin: 0 auto; padding: 60px 24px; position: relative; z-index: 1; }
.features-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.feature-card { background: rgba(10, 20, 40, 0.6); border: 1px solid rgba(64, 224, 208, 0.1); border-radius: 16px; padding: 28px 20px; text-align: center; transition: all 0.3s; cursor: pointer; }
.feature-card:hover { border-color: rgba(64, 224, 208, 0.3); transform: translateY(-4px); box-shadow: 0 8px 32px rgba(64, 224, 208, 0.08); }
.feature-icon { margin-bottom: 16px; }
.feature-card h3 { font-size: 16px; font-weight: 600; color: #e0e6f0; margin: 0 0 10px 0; }
.feature-card p { font-size: 13px; color: rgba(140, 170, 210, 0.6); line-height: 1.6; margin: 0; }
.feature-more { font-size: 12px; color: #40e0d0; margin-top: 12px; opacity: 0; transition: opacity 0.3s; }
.feature-card:hover .feature-more { opacity: 1; }

.query-section { max-width: 1200px; margin: 0 auto; padding: 60px 24px; position: relative; z-index: 1; }
.query-tabs :deep(.el-tabs__item) { color: #8caad2; }
.query-tabs :deep(.el-tabs__item.is-active) { color: #40e0d0; }
.query-tabs :deep(.el-tabs__nav-wrap::after) { background-color: #1a3a5c; }
.query-bar { display: flex; gap: 12px; align-items: center; }
.query-section :deep(.el-table) { background-color: rgba(10, 20, 40, 0.6); --el-table-border-color: #1a3a5c; --el-table-header-bg-color: #0f1f33; --el-table-row-hover-bg-color: rgba(64, 224, 208, 0.05); --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-text-color: #8caad2; --el-table-text-color: #c0cde0; }
.query-section :deep(.el-table th.el-table__cell) { background-color: #0f1f33 !important; color: #8caad2; border-bottom: 1px solid #1a3a5c; }
.query-section :deep(.el-table td.el-table__cell) { border-bottom: 1px solid #1a2a40; }
.query-section :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) { background: rgba(15, 31, 51, 0.5); }
.query-section :deep(.el-input__wrapper) { background: rgba(15, 25, 50, 0.8); border: 1px solid rgba(64, 224, 208, 0.15); box-shadow: none; }
.query-section :deep(.el-input__wrapper:hover), .query-section :deep(.el-input__wrapper.is-focus) { border-color: rgba(64, 224, 208, 0.4); }
.query-section :deep(.el-input__inner) { color: #e0e6f0; }
.query-section :deep(.el-input__inner::placeholder) { color: rgba(140, 170, 210, 0.4); }
.query-section :deep(.el-select .el-input__wrapper) { background: rgba(15, 25, 50, 0.8); }
.query-section :deep(.el-pagination) { --el-pagination-bg-color: transparent; --el-pagination-text-color: #8caad2; --el-pagination-button-bg-color: rgba(15, 25, 50, 0.8); --el-pagination-hover-color: #40e0d0; }
.query-section :deep(.el-pager li) { background: rgba(15, 25, 50, 0.8) !important; color: #8caad2; }
.query-section :deep(.el-pager li.is-active) { background: rgba(64, 224, 208, 0.2) !important; color: #40e0d0; }

.stats-section { max-width: 1200px; margin: 0 auto; padding: 60px 24px; position: relative; z-index: 1; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 32px; }
.stat-card { background: rgba(10, 20, 40, 0.6); border: 1px solid rgba(64, 224, 208, 0.1); border-radius: 12px; padding: 24px; text-align: center; }
.stat-value { font-size: 36px; font-weight: 800; background: linear-gradient(90deg, #40e0d0, #60b0ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.stat-label { font-size: 13px; color: rgba(140, 170, 210, 0.5); margin-top: 4px; }
.stats-chart { height: 280px; }

.register-section { max-width: 1200px; margin: 0 auto; padding: 60px 24px; position: relative; z-index: 1; }
.register-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
.reg-card { background: rgba(10, 20, 40, 0.6); border: 1px solid rgba(64, 224, 208, 0.1); border-radius: 16px; padding: 36px 24px; text-align: center; cursor: pointer; transition: all 0.3s; }
.reg-card:hover { border-color: rgba(64, 224, 208, 0.3); transform: translateY(-4px); box-shadow: 0 8px 32px rgba(64, 224, 208, 0.08); }
.pilot-card:hover { border-color: rgba(64, 224, 208, 0.4); }
.org-card:hover { border-color: rgba(96, 176, 255, 0.4); }
.guest-card:hover { border-color: rgba(140, 170, 210, 0.4); }
.reg-icon { margin-bottom: 16px; }
.reg-card h3 { font-size: 18px; font-weight: 600; color: #e0e6f0; margin: 0 0 10px 0; }
.reg-card p { font-size: 13px; color: rgba(140, 170, 210, 0.6); line-height: 1.6; margin: 0 0 20px 0; }
.reg-btn { border-radius: 8px !important; }
.pilot-card .reg-btn { background: linear-gradient(135deg, #1a6b6a, #0e4a50) !important; border: 1px solid rgba(64, 224, 208, 0.3) !important; }
.org-card .reg-btn { background: linear-gradient(135deg, #1a3a6b, #0e2a50) !important; border: 1px solid rgba(96, 176, 255, 0.3) !important; }
.guest-card .reg-btn { background: transparent !important; border: 1px solid rgba(140, 170, 210, 0.3) !important; color: #8caad2 !important; }

.feature-detail { text-align: center; }
.fd-icon { margin-bottom: 16px; }
.fd-desc { font-size: 14px; color: #606266; line-height: 1.8; text-align: left; margin-bottom: 20px; }
.fd-highlights { text-align: left; }
.fd-hl-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f5f7fa; }
.fd-hl-item:last-child { border-bottom: none; }
.fd-hl-dot { width: 6px; height: 6px; border-radius: 50%; background: #40e0d0; flex-shrink: 0; }

.home-footer { text-align: center; padding: 32px 24px; font-size: 12px; color: rgba(140, 170, 210, 0.3); border-top: 1px solid rgba(64, 224, 208, 0.05); position: relative; z-index: 1; }

.verify-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
}
.verify-badge.verified {
  color: #40e0d0;
  background: rgba(64, 224, 208, 0.1);
  border: 1px solid rgba(64, 224, 208, 0.25);
}

.login-prompt {
  background: rgba(10, 20, 40, 0.6);
  border: 1px solid rgba(64, 224, 208, 0.1);
  border-radius: 12px;
  padding: 40px 24px;
  text-align: center;
}
.login-prompt-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 8px;
}
.login-prompt :deep(.el-alert) {
  background: rgba(230, 162, 60, 0.1);
  border: 1px solid rgba(230, 162, 60, 0.25);
}
.login-prompt :deep(.el-alert__title) {
  color: #e6a23c;
}
</style>
