<template>
  <div class="dashboard-screen">
    <div class="screen-header">
      <h2>应急调度指挥中心</h2>
      <div class="header-stats">
        <div class="h-stat"><span class="h-num">{{ stats.totalMissions ?? '-' }}</span><span class="h-lbl">总任务</span></div>
        <div class="h-stat"><span class="h-num warn">{{ stats.activeMissions ?? '-' }}</span><span class="h-lbl">进行中</span></div>
        <div class="h-stat"><span class="h-num ok">{{ stats.completedMissions ?? '-' }}</span><span class="h-lbl">已完成</span></div>
        <div class="h-stat"><span class="h-num blue">{{ stats.idlePilots ?? '-' }}</span><span class="h-lbl">空闲飞手</span></div>
        <div class="h-stat"><span class="h-num blue">{{ stats.idleOrgs ?? '-' }}</span><span class="h-lbl">空闲单位</span></div>
      </div>
    </div>

    <el-row :gutter="12" class="screen-body">
      <el-col :span="17">
        <el-card shadow="never" class="chart-card map-card">
          <template #header>
            <div class="map-header">
              <span>资源分布态势图</span>
              <div class="map-legend">
                <span class="legend-item"><i class="legend-dot" style="background:#67c23a"></i>空闲飞手</span>
                <span class="legend-item"><i class="legend-dot" style="background:#e6a23c"></i>任务中飞手</span>
                <span class="legend-item"><i class="legend-dot" style="background:#409eff"></i>空闲单位</span>
                <span class="legend-item"><i class="legend-dot" style="background:#f56c6c"></i>任务/紧急</span>
              </div>
            </div>
          </template>
          <div ref="mapChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="7">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-card shadow="never" class="chart-card small">
              <template #header><span>任务类型分布</span></template>
              <div ref="typeChartRef" class="chart-container-sm"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" class="chart-card small">
              <template #header><span>紧急程度分布</span></template>
              <div ref="urgencyChartRef" class="chart-container-sm"></div>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="never" class="chart-card" style="margin-top: 12px">
          <template #header><span>飞手/单位状态分布</span></template>
          <div ref="statusChartRef" class="chart-container-sm"></div>
        </el-card>
        <el-card shadow="never" class="chart-card" style="margin-top: 12px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>任务流程追踪</span>
              <el-button size="small" text type="primary" @click="loadData">刷新</el-button>
            </div>
          </template>
          <div class="recent-missions">
            <div v-for="m in recentMissions" :key="m.id" class="rm-item" @click="viewMissionFlow(m)">
              <div class="rm-dot" :class="'rm-' + m.urgency?.toLowerCase()"></div>
              <div class="rm-info">
                <div class="rm-title">{{ m.title }}</div>
                <div class="rm-meta">
                  <el-tag size="small" :type="missionStatusTag(m.status)">{{ missionStatusLabel(m.status) }}</el-tag>
                  <span class="rm-loc">{{ m.locationName }}</span>
                </div>
                <div class="rm-flow-bar">
                  <div class="flow-step" :class="{ active: flowStep(m.status) >= 0 }">发布</div>
                  <div class="flow-line" :class="{ active: flowStep(m.status) >= 1 }"></div>
                  <div class="flow-step" :class="{ active: flowStep(m.status) >= 1 }">派单</div>
                  <div class="flow-line" :class="{ active: flowStep(m.status) >= 2 }"></div>
                  <div class="flow-step" :class="{ active: flowStep(m.status) >= 2 }">接单</div>
                  <div class="flow-line" :class="{ active: flowStep(m.status) >= 3 }"></div>
                  <div class="flow-step" :class="{ active: flowStep(m.status) >= 3 }">作业</div>
                  <div class="flow-line" :class="{ active: flowStep(m.status) >= 4 }"></div>
                  <div class="flow-step" :class="{ active: flowStep(m.status) >= 4 }">完成</div>
                </div>
              </div>
              <div class="rm-time">{{ m.createdAt?.replace('T', ' ').substring(5, 16) }}</div>
            </div>
            <el-empty v-if="recentMissions.length === 0" description="暂无任务" :image-size="40" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="flowDialogVisible" :title="'任务流程详情 #' + flowMission?.id" width="780px" destroy-on-close>
      <template v-if="flowMission">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="任务标题" :span="2">{{ flowMission.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ missionTypeLabel(flowMission.missionType) }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            <el-tag :type="urgencyTagType(flowMission.urgency)" effect="dark" size="small">{{ urgencyLabel(flowMission.urgency) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="地点" :span="2">{{ flowMission.locationName }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ flowMission.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">完成流程</el-divider>
        <el-steps :active="flowStep(flowMission.status)" align-center>
          <el-step title="发布" :description="flowMission.createdAt?.replace('T', ' ').substring(0, 16)" />
          <el-step title="派单" />
          <el-step title="接单" />
          <el-step title="作业" />
          <el-step title="完成" :description="flowMission.completedAt?.replace('T', ' ').substring(0, 16)" />
        </el-steps>

        <el-divider content-position="left">操作日志</el-divider>
        <el-timeline v-if="flowLogs.length > 0">
          <el-timeline-item v-for="log in flowLogs" :key="log.id" :timestamp="log.createdAt?.replace('T', ' ').substring(0, 16)">
            <el-tag size="small" :type="logActionTag(log.action)" style="margin-right: 8px">{{ log.action }}</el-tag>
            {{ log.commentText }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无日志" :image-size="60" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import http from '@/api/http'
import * as echarts from 'echarts'

const stats = ref<any>({})
const recentMissions = ref<any[]>([])
const pilotStatuses = ref<any[]>([])
const orgStatuses = ref<any[]>([])

const mapChartRef = ref<HTMLElement>()
const typeChartRef = ref<HTMLElement>()
const urgencyChartRef = ref<HTMLElement>()
const statusChartRef = ref<HTMLElement>()

let mapChart: echarts.ECharts | null = null
let typeChart: echarts.ECharts | null = null
let urgencyChart: echarts.ECharts | null = null
let statusChart: echarts.ECharts | null = null
let chinaGeoLoaded = false

const flowDialogVisible = ref(false)
const flowMission = ref<any>(null)
const flowLogs = ref<any[]>([])

function flowStep(s: string) { const m: Record<string, number> = { PUBLISHED: 0, DISPATCHED: 1, ACCEPTED: 2, IN_PROGRESS: 3, COMPLETED: 4, CANCELLED: -1 }; return m[s] ?? 0 }
function missionStatusLabel(s: string) { const m: Record<string, string> = { PUBLISHED: '已发布', DISPATCHED: '已派单', ACCEPTED: '已接单', IN_PROGRESS: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }; return m[s] || s }
function missionStatusTag(s: string) { const m: Record<string, string> = { PUBLISHED: 'info', DISPATCHED: 'warning', ACCEPTED: '', IN_PROGRESS: 'success', COMPLETED: 'success', CANCELLED: 'info' }; return m[s] || '' }
function missionTypeLabel(t: string) { const m: Record<string, string> = { FIRE_FIGHTING: '森林灭火', FLOOD_RESCUE: '洪涝搜救', ACCIDENT_SURVEY: '事故勘察', OTHER: '其他' }; return m[t] || t }
function urgencyLabel(u: string) { const m: Record<string, string> = { RED: '红色', YELLOW: '黄色', BLUE: '蓝色' }; return m[u] || u }
function urgencyTagType(u: string) { const m: Record<string, string> = { RED: 'danger', YELLOW: 'warning', BLUE: '' }; return m[u] || '' }
function logActionTag(a: string) { const m: Record<string, string> = { CREATE: 'info', DISPATCH: 'warning', ACCEPT: 'success', STATUS_CHANGE: '', REJECT: 'danger' }; return m[a] || '' }

async function viewMissionFlow(m: any) {
  flowMission.value = m
  try {
    const res = await http.get(`/api/missions/${m.id}/logs`)
    flowLogs.value = res || []
  } catch { flowLogs.value = [] }
  flowDialogVisible.value = true
}

async function loadChinaGeo() {
  if (chinaGeoLoaded) return true
  try {
    const resp = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json')
    const geoJson = await resp.json()
    echarts.registerMap('china', geoJson)
    chinaGeoLoaded = true
    return true
  } catch {
    try {
      const resp = await fetch('https://unpkg.com/echarts@4.9.0/map/json/china.json')
      const geoJson = await resp.json()
      echarts.registerMap('china', geoJson)
      chinaGeoLoaded = true
      return true
    } catch { return false }
  }
}

async function loadData() {
  try {
    const res = await http.get('/api/missions/dashboard')
    stats.value = res || {}
    recentMissions.value = res?.recentMissions || []
    pilotStatuses.value = res?.pilotStatuses || []
    orgStatuses.value = res?.orgStatuses || []
    await loadChinaGeo()
    renderCharts()
  } catch { /* ignore */ }
}

function renderCharts() {
  renderMapChart()
  renderTypeChart()
  renderUrgencyChart()
  renderStatusChart()
}

function renderMapChart() {
  if (!mapChartRef.value) return
  if (!mapChart) mapChart = echarts.init(mapChartRef.value)

  const pilotHeatData: number[][] = []
  const pilotScatterData: any[] = []
  pilotStatuses.value.forEach((p: any) => {
    if (p.currentLatitude && p.currentLongitude) {
      pilotHeatData.push([p.currentLongitude, p.currentLatitude, p.availability === 'IDLE' ? 80 : 30])
      pilotScatterData.push({
        name: p.pilotName || ('飞手#' + p.pilotId),
        value: [p.currentLongitude, p.currentLatitude, p.availability === 'IDLE' ? 1 : 0.5],
        itemStyle: { color: p.availability === 'IDLE' ? '#67c23a' : '#e6a23c' },
        _type: 'pilot', _availability: p.availability, _droneType: p.droneType || '-', _missionCount: p.missionCount ?? 0
      })
    }
  })

  const orgScatterData: any[] = []
  orgStatuses.value.forEach((o: any) => {
    if (o.latitude && o.longitude) {
      orgScatterData.push({
        name: o.orgName || ('单位#' + o.orgId),
        value: [o.longitude, o.latitude, o.availability === 'IDLE' ? 1 : 0.5],
        itemStyle: { color: o.availability === 'IDLE' ? '#409eff' : '#f56c6c' },
        _type: 'org', _availability: o.availability, _missionCount: o.missionCount ?? 0
      })
    }
  })

  const missionEffectData: any[] = []
  recentMissions.value.forEach((m: any) => {
    if (m.latitude && m.longitude) {
      const urgencyColor = m.urgency === 'RED' ? '#f56c6c' : m.urgency === 'YELLOW' ? '#e6a23c' : '#409eff'
      missionEffectData.push({
        name: m.title, value: [m.longitude, m.latitude, 1],
        itemStyle: { color: urgencyColor, shadowBlur: 10, shadowColor: urgencyColor },
        _type: 'mission', _urgency: m.urgency, _status: m.status, _location: m.locationName
      })
    }
  })

  const option: any = {
    tooltip: {
      trigger: 'item', backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#e4e7ed',
      textStyle: { color: '#303133', fontSize: 12 },
      formatter: (params: any) => {
        const d = params.data
        if (!d) return params.name
        if (d._type === 'pilot') return `<b>${d.name}</b><br/>状态: ${d._availability === 'IDLE' ? '空闲' : d._availability === 'ON_MISSION' ? '任务中' : d._availability}<br/>无人机: ${d._droneType}<br/>完成任务: ${d._missionCount}`
        if (d._type === 'org') return `<b>${d.name}</b><br/>状态: ${d._availability === 'IDLE' ? '空闲' : d._availability === 'ON_MISSION' ? '任务中' : d._availability}<br/>完成任务: ${d._missionCount}`
        if (d._type === 'mission') {
          const urgMap: Record<string, string> = { RED: '红色', YELLOW: '黄色', BLUE: '蓝色' }
          const stMap: Record<string, string> = { PUBLISHED: '已发布', DISPATCHED: '已派单', ACCEPTED: '已接单', IN_PROGRESS: '进行中', COMPLETED: '已完成' }
          return `<b>${d.name}</b><br/>紧急程度: ${urgMap[d._urgency] || d._urgency}<br/>状态: ${stMap[d._status] || d._status}<br/>地点: ${d._location || '-'}`
        }
        return d.name
      }
    },
    geo: {
      map: 'china', roam: true, zoom: 1.2, center: [104.5, 35.5], scaleLimit: { min: 0.8, max: 8 },
      label: { show: true, color: 'rgba(96,98,102,0.5)', fontSize: 9 },
      itemStyle: { areaColor: '#e8ecf1', borderColor: '#b8c4d0', borderWidth: 0.8 },
      emphasis: { itemStyle: { areaColor: '#d0dce8' }, label: { color: '#303133', fontSize: 11 } },
      select: { disabled: true }
    },
    visualMap: {
      show: true, min: 0, max: 80, right: 10, bottom: 20, text: ['高密度', '低密度'],
      textStyle: { color: '#606266', fontSize: 10 },
      inRange: { color: ['#e8ecf1', '#b8d8e8', '#7ec8c8', '#5ab878', '#3da35d', '#67c23a'] },
      calculable: true, orient: 'vertical', itemWidth: 12, itemHeight: 80, seriesIndex: 0
    },
    series: [
      { type: 'heatmap', coordinateSystem: 'geo', data: pilotHeatData, pointSize: 18, blurSize: 28, z: 5 },
      { type: 'scatter', coordinateSystem: 'geo', symbol: 'circle', symbolSize: (val: number[]) => Math.max(10, val[2] * 14), data: pilotScatterData, label: { show: true, formatter: '{b}', fontSize: 9, color: '#3a7a3a', position: 'bottom', distance: 4 }, z: 10 },
      { type: 'scatter', coordinateSystem: 'geo', symbol: 'diamond', symbolSize: (val: number[]) => Math.max(14, val[2] * 18), data: orgScatterData, label: { show: true, formatter: '{b}', fontSize: 9, color: '#2a6ab0', position: 'bottom', distance: 4 }, z: 10 },
      { type: 'effectScatter', coordinateSystem: 'geo', symbolSize: 14, data: missionEffectData, rippleEffect: { brushType: 'stroke', scale: 4, period: 3 }, label: { show: true, formatter: '{b}', fontSize: 10, fontWeight: 'bold', color: '#c07020', position: 'top', distance: 6 }, z: 20 }
    ]
  }

  if (!chinaGeoLoaded) {
    option.xAxis = { type: 'value', min: 73, max: 136, show: false }
    option.yAxis = { type: 'value', min: 18, max: 54, show: false }
    option.grid = { left: 10, right: 10, top: 10, bottom: 10 }
    option.series.forEach((s: any) => { s.coordinateSystem = 'cartesian2d' })
    delete option.geo; delete option.visualMap
  }

  mapChart.setOption(option, true)
}

function renderTypeChart() {
  if (!typeChartRef.value) return
  if (!typeChart) typeChart = echarts.init(typeChartRef.value)
  const typeMap: Record<string, string> = { FIRE_FIGHTING: '森林灭火', FLOOD_RESCUE: '洪涝搜救', ACCIDENT_SURVEY: '事故勘察', OTHER: '其他' }
  const counts: Record<string, number> = {}
  recentMissions.value.forEach((m: any) => { const label = typeMap[m.missionType] || m.missionType; counts[label] = (counts[label] || 0) + 1 })
  typeChart.setOption({ tooltip: { trigger: 'item' }, series: [{ type: 'pie', radius: ['35%', '65%'], data: Object.entries(counts).map(([name, value]) => ({ name, value })), label: { fontSize: 11, color: '#606266' }, emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.15)' } } }] })
}

function renderUrgencyChart() {
  if (!urgencyChartRef.value) return
  if (!urgencyChart) urgencyChart = echarts.init(urgencyChartRef.value)
  const urgencyMap: Record<string, string> = { RED: '红色', YELLOW: '黄色', BLUE: '蓝色' }
  const counts: Record<string, number> = {}
  recentMissions.value.forEach((m: any) => { const label = urgencyMap[m.urgency] || m.urgency; counts[label] = (counts[label] || 0) + 1 })
  urgencyChart.setOption({ tooltip: { trigger: 'item' }, color: ['#f56c6c', '#e6a23c', '#409eff'], series: [{ type: 'pie', radius: ['35%', '65%'], data: Object.entries(counts).map(([name, value]) => ({ name, value })), label: { fontSize: 11, color: '#606266' }, emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.15)' } } }] })
}

function renderStatusChart() {
  if (!statusChartRef.value) return
  if (!statusChart) statusChart = echarts.init(statusChartRef.value)
  const allData: Record<string, number> = {}
  pilotStatuses.value.forEach((p: any) => { const label = p.availability === 'IDLE' ? '空闲飞手' : p.availability === 'ON_MISSION' ? '任务中飞手' : '其他飞手'; allData[label] = (allData[label] || 0) + 1 })
  orgStatuses.value.forEach((o: any) => { const label = o.availability === 'IDLE' ? '空闲单位' : o.availability === 'ON_MISSION' ? '任务中单位' : '其他单位'; allData[label] = (allData[label] || 0) + 1 })
  statusChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: Object.keys(allData), axisLabel: { fontSize: 10, rotate: 15, color: '#606266' } },
    yAxis: { type: 'value', axisLabel: { color: '#606266' } },
    series: [{ type: 'bar', data: Object.entries(allData).map(([name, value]) => ({ name, value, itemStyle: { color: name.includes('空闲') ? '#67c23a' : name.includes('任务') ? '#e6a23c' : '#909399' } })), barWidth: '40%', label: { show: true, position: 'top', color: '#606266' } }]
  })
}

function handleResize() { mapChart?.resize(); typeChart?.resize(); urgencyChart?.resize(); statusChart?.resize() }

onMounted(() => { loadData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); mapChart?.dispose(); typeChart?.dispose(); urgencyChart?.dispose(); statusChart?.dispose() })
</script>

<style scoped>
.dashboard-screen { background: #f5f7fa; min-height: calc(100vh - 100px); padding: 12px; }
.screen-header { background: linear-gradient(135deg, #1d3a5c 0%, #2c5f8a 50%, #1a4a6e 100%); color: #fff; padding: 16px 24px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; border: 1px solid #d0d7e0; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); }
.screen-header h2 { margin: 0; font-size: 22px; letter-spacing: 2px; color: #fff; }
.header-stats { display: flex; gap: 24px; }
.h-stat { text-align: center; }
.h-num { display: block; font-size: 28px; font-weight: 700; color: #fff; }
.h-num.warn { color: #f5c542; } .h-num.ok { color: #67c23a; } .h-num.blue { color: #79bbff; }
.h-lbl { font-size: 12px; color: rgba(255,255,255,0.7); }
.chart-card { border-radius: 8px; background: #fff; border: 1px solid #e4e7ed; color: #303133; }
.chart-card :deep(.el-card__header) { background: #fafafa; border-bottom: 1px solid #e4e7ed; color: #303133; padding: 10px 16px; }
.chart-card :deep(.el-card__body) { background: #fff; }
.map-card { height: 100%; }
.map-header { display: flex; justify-content: space-between; align-items: center; }
.map-legend { display: flex; gap: 14px; font-size: 11px; color: #606266; }
.legend-item { display: flex; align-items: center; gap: 4px; }
.legend-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; }
.chart-container { height: 560px; }
.chart-container-sm { height: 200px; }
.recent-missions { max-height: 300px; overflow-y: auto; }
.rm-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background 0.15s; }
.rm-item:hover { background: #f5f7fa; border-radius: 6px; }
.rm-item:last-child { border-bottom: none; }
.rm-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.rm-red { background: #f56c6c; box-shadow: 0 0 6px #f56c6c; }
.rm-yellow { background: #e6a23c; box-shadow: 0 0 6px #e6a23c; }
.rm-blue { background: #409eff; box-shadow: 0 0 6px #409eff; }
.rm-info { flex: 1; min-width: 0; }
.rm-title { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #303133; }
.rm-meta { display: flex; align-items: center; gap: 6px; margin-top: 2px; }
.rm-loc { font-size: 11px; color: #909399; }
.rm-time { font-size: 11px; color: #909399; white-space: nowrap; }
.rm-flow-bar { display: flex; align-items: center; gap: 0; margin-top: 6px; }
.flow-step { font-size: 9px; color: #909399; padding: 1px 4px; border-radius: 3px; background: #f5f7fa; }
.flow-step.active { color: #409eff; background: #ecf5ff; }
.flow-line { width: 12px; height: 2px; background: #e4e7ed; }
.flow-line.active { background: #409eff; }
.recent-missions::-webkit-scrollbar { width: 4px; }
.recent-missions::-webkit-scrollbar-track { background: #fff; }
.recent-missions::-webkit-scrollbar-thumb { background: #c0c4cc; border-radius: 2px; }
</style>
