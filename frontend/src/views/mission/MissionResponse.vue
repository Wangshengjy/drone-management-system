<template>
  <div class="mission-response">
    <div v-if="pendingMissions.length > 0" class="pending-alert">
      <div class="alert-header">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#f56c6c" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <span>您有 <strong>{{ pendingMissions.length }}</strong> 个应急任务待响应！</span>
      </div>
      <div class="pending-list">
        <div v-for="m in pendingMissions" :key="m.id" class="pending-card">
          <div class="pending-urgency" :class="'urg-' + m.urgency?.toLowerCase()"></div>
          <div class="pending-info">
            <div class="pending-title">{{ m.title }}</div>
            <div class="pending-meta">
              <el-tag :type="urgencyTagType(m.urgency)" size="small" effect="dark">{{ urgencyLabel(m.urgency) }}级</el-tag>
              <span class="pending-loc">{{ m.locationName }}</span>
              <span class="pending-deadline">截止: {{ m.confirmDeadline?.replace('T', ' ').substring(0, 16) || '-' }}</span>
            </div>
          </div>
          <div class="pending-actions">
            <el-button type="success" size="small" @click="acceptMission(m)">确认接单</el-button>
            <el-button type="danger" size="small" plain @click="rejectMission(m)">拒绝</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的任务</span>
              <div class="filter-tags">
                <el-tag :type="statusFilter === '' ? '' : 'info'" style="cursor:pointer" @click="statusFilter = ''; loadMyMissions()">全部</el-tag>
                <el-tag :type="statusFilter === 'DISPATCHED' ? 'warning' : 'info'" style="cursor:pointer;margin-left:4px" @click="statusFilter = 'DISPATCHED'; loadMyMissions()">待响应</el-tag>
                <el-tag :type="statusFilter === 'ACCEPTED' ? '' : 'info'" style="cursor:pointer;margin-left:4px" @click="statusFilter = 'ACCEPTED'; loadMyMissions()">已接单</el-tag>
                <el-tag :type="statusFilter === 'IN_PROGRESS' ? 'success' : 'info'" style="cursor:pointer;margin-left:4px" @click="statusFilter = 'IN_PROGRESS'; loadMyMissions()">进行中</el-tag>
                <el-tag :type="statusFilter === 'COMPLETED' ? 'success' : 'info'" style="cursor:pointer;margin-left:4px" @click="statusFilter = 'COMPLETED'; loadMyMissions()">已完成</el-tag>
              </div>
            </div>
          </template>
          <el-table :data="filteredMissions" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="55" />
            <el-table-column prop="title" label="任务标题" min-width="150" show-overflow-tooltip />
            <el-table-column prop="missionType" label="类型" width="95">
              <template #default="{ row }">
                <el-tag :type="missionTypeTag(row.missionType)" size="small">{{ missionTypeLabel(row.missionType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="urgency" label="紧急" width="75">
              <template #default="{ row }">
                <el-tag :type="urgencyTagType(row.urgency)" size="small" effect="dark">{{ urgencyLabel(row.urgency) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="85">
              <template #default="{ row }">
                <el-tag :type="missionStatusTag(row.status)" size="small">{{ missionStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="locationName" label="地点" min-width="110" show-overflow-tooltip />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="viewDetail(row)">详情</el-button>
                <el-button size="small" type="success" v-if="row.status === 'DISPATCHED'" @click="acceptMission(row)">接单</el-button>
                <el-button size="small" type="primary" v-if="row.status === 'ACCEPTED'" @click="startWork(row)">出发</el-button>
                <el-button size="small" type="warning" v-if="row.status === 'IN_PROGRESS'" @click="reportProgress(row)">反馈</el-button>
                <el-button size="small" type="success" v-if="row.status === 'IN_PROGRESS'" @click="completeMission(row)">完成</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && filteredMissions.length === 0" description="暂无任务" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card header="我的状态">
          <el-form label-width="80px" size="small">
            <el-form-item label="当前状态">
              <el-select v-model="myStatus.availability" @change="updateMyStatus">
                <el-option label="空闲" value="IDLE" />
                <el-option label="任务中" value="ON_MISSION" />
                <el-option label="休假" value="LEAVE" />
                <el-option label="离线" value="OFFLINE" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="props.role === 'PILOT'" label="无人机类型">
              <el-input v-model="myStatus.droneType" placeholder="如：多旋翼-载重" @change="updateMyStatus" />
            </el-form-item>
            <el-form-item label="累计任务">
              <span style="font-size: 24px; font-weight: 700; color: #409eff">{{ myStatus.missionCount ?? 0 }}</span>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card header="任务流程指引" style="margin-top: 12px">
          <el-steps direction="vertical" :active="flowActiveStep" :space="56">
            <el-step title="已发布" description="管理员发布任务需求" />
            <el-step title="已派单" description="系统智能匹配并派发" />
            <el-step title="已接单" description="飞手/单位确认接单" />
            <el-step title="进行中" description="出发前往现场作业" />
            <el-step title="已完成" description="任务完成并归档" />
          </el-steps>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="detailVisible" :title="'任务详情 #' + detailMission?.id" width="800px" destroy-on-close>
      <template v-if="detailMission">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务标题" :span="2">{{ detailMission.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="missionTypeTag(detailMission.missionType)">{{ missionTypeLabel(detailMission.missionType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            <el-tag :type="urgencyTagType(detailMission.urgency)" effect="dark">{{ urgencyLabel(detailMission.urgency) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="地点" :span="2">{{ detailMission.locationName }}</el-descriptions-item>
          <el-descriptions-item label="坐标" :span="2">{{ detailMission.latitude }}, {{ detailMission.longitude }}</el-descriptions-item>
          <el-descriptions-item label="气象" :span="2">{{ detailMission.weatherInfo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需要资质">{{ detailMission.requiredCertTypes || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需要机型">{{ detailMission.requiredDroneType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ detailMission.confirmDeadline?.replace('T', ' ').substring(0, 16) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detailMission.description }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">任务流程</el-divider>
        <el-steps :active="detailFlowStep" align-center>
          <el-step title="发布" />
          <el-step title="派单" />
          <el-step title="接单" />
          <el-step title="进行中" />
          <el-step title="完成" />
        </el-steps>

        <el-divider v-if="props.role === 'ORG' && detailMission?.status === 'ACCEPTED'" content-position="left">指派飞手</el-divider>
        <div v-if="props.role === 'ORG' && detailMission?.status === 'ACCEPTED'" class="dispatch-section">
          <div class="dispatch-mode">
            <h4 style="margin: 0 0 12px 0; font-size: 14px; color: #606266">手动指派</h4>
            <el-table :data="availablePilots" v-loading="availablePilotsLoading" border stripe size="small">
              <el-table-column prop="pilotName" label="飞手姓名" min-width="100" />
              <el-table-column prop="droneType" label="机型" min-width="100" />
              <el-table-column prop="phone" label="联系电话" width="120" />
              <el-table-column prop="availability" label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="pilotAvailTag(row.availability)" size="small">{{ pilotAvailLabel(row.availability) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="dispatchPilot(row)">指派</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!availablePilotsLoading && availablePilots.length === 0" description="暂无可用飞手" :image-size="40" />
          </div>
          <el-divider direction="vertical" style="height: auto; margin: 0 20px" />
          <div class="dispatch-mode" style="display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 160px">
            <h4 style="margin: 0 0 16px 0; font-size: 14px; color: #606266">自动指派</h4>
            <el-button type="warning" @click="autoDispatch">系统自动分配</el-button>
            <p style="color: #909399; font-size: 12px; margin-top: 8px">系统将根据飞手资质和状态自动匹配</p>
          </div>
        </div>

        <el-divider content-position="left">操作日志</el-divider>
        <el-timeline v-if="detailLogs.length > 0">
          <el-timeline-item v-for="log in detailLogs" :key="log.id" :timestamp="log.createdAt?.replace('T', ' ').substring(0, 16)">
            <el-tag size="small" style="margin-right: 8px" :type="logActionTag(log.action)">{{ log.action }}</el-tag>
            {{ log.commentText }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无日志" :image-size="60" />
      </template>
    </el-dialog>

    <el-dialog v-model="reportVisible" title="现场反馈" width="500px" destroy-on-close>
      <el-form :model="reportForm" label-width="80px">
        <el-form-item label="任务状态">
          <el-radio-group v-model="reportForm.status">
            <el-radio-button value="IN_PROGRESS">作业中</el-radio-button>
            <el-radio-button value="COMPLETED">已完成</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="反馈说明">
          <el-input v-model="reportForm.comment" type="textarea" :rows="3" placeholder="如：已到达现场，开始作业" />
        </el-form-item>
        <el-form-item label="现场照片">
          <el-input v-model="reportForm.attachmentUrl" placeholder="照片URL（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReport">提交反馈</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import http from '@/api/http'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps<{ role: 'PILOT' | 'ORG' }>()

const loading = ref(false)
const myMissions = ref<any[]>([])
const statusFilter = ref('')
const myStatus = ref<any>({ availability: 'IDLE', droneType: '', missionCount: 0 })

const detailVisible = ref(false)
const detailMission = ref<any>(null)
const detailLogs = ref<any[]>([])

const reportVisible = ref(false)
const reportForm = ref<any>({ missionId: null, status: 'IN_PROGRESS', comment: '', attachmentUrl: '' })

const availablePilotsLoading = ref(false)
const availablePilots = ref<any[]>([])

const pendingMissions = computed(() => myMissions.value.filter(m => m.status === 'DISPATCHED'))
const filteredMissions = computed(() => {
  if (!statusFilter.value) return myMissions.value
  if (statusFilter.value === 'DISPATCHED') return myMissions.value.filter(m => m.status === 'DISPATCHED')
  return myMissions.value.filter(m => m.status === statusFilter.value)
})
const flowActiveStep = computed(() => {
  if (myMissions.value.length === 0) return 0
  const s = myMissions.value[0]?.status
  const m: Record<string, number> = { PUBLISHED: 0, DISPATCHED: 1, ACCEPTED: 2, IN_PROGRESS: 3, COMPLETED: 4 }
  return m[s] ?? 0
})
const detailFlowStep = computed(() => {
  const s = detailMission.value?.status
  const m: Record<string, number> = { PUBLISHED: 0, DISPATCHED: 1, ACCEPTED: 2, IN_PROGRESS: 3, COMPLETED: 4 }
  return m[s] ?? 0
})

function missionTypeLabel(t: string) { const m: Record<string, string> = { FIRE_FIGHTING: '森林灭火', FLOOD_RESCUE: '洪涝搜救', ACCIDENT_SURVEY: '事故勘察', OTHER: '其他' }; return m[t] || t }
function missionTypeTag(t: string) { const m: Record<string, string> = { FIRE_FIGHTING: 'danger', FLOOD_RESCUE: 'warning', ACCIDENT_SURVEY: 'info', OTHER: '' }; return m[t] || '' }
function urgencyLabel(u: string) { const m: Record<string, string> = { RED: '红色', YELLOW: '黄色', BLUE: '蓝色' }; return m[u] || u }
function urgencyTagType(u: string) { const m: Record<string, string> = { RED: 'danger', YELLOW: 'warning', BLUE: '' }; return m[u] || '' }
function missionStatusLabel(s: string) { const m: Record<string, string> = { PUBLISHED: '已发布', DISPATCHED: '已派单', ACCEPTED: '已接单', IN_PROGRESS: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }; return m[s] || s }
function missionStatusTag(s: string) { const m: Record<string, string> = { PUBLISHED: 'info', DISPATCHED: 'warning', ACCEPTED: '', IN_PROGRESS: 'success', COMPLETED: 'success', CANCELLED: 'info' }; return m[s] || '' }
function logActionTag(a: string) { const m: Record<string, string> = { CREATE: 'info', DISPATCH: 'warning', ACCEPT: 'success', STATUS_CHANGE: '', REJECT: 'danger' }; return m[a] || '' }

async function loadMyMissions() {
  loading.value = true
  try {
    const res = await http.get('/api/missions/my')
    myMissions.value = res || []
  } finally {
    loading.value = false
  }
}

async function loadMyStatus() {
  try {
    const url = props.role === 'PILOT' ? '/api/missions/pilot-status/me' : '/api/missions/org-status/me'
    const res = await http.get(url)
    myStatus.value = res || { availability: 'IDLE', missionCount: 0 }
  } catch { /* ignore */ }
}

async function updateMyStatus() {
  try {
    const url = props.role === 'PILOT' ? '/api/missions/pilot-status' : '/api/missions/org-status'
    const body: any = { availability: myStatus.value.availability }
    if (props.role === 'PILOT') body.droneType = myStatus.value.droneType
    await http.put(url, body)
    ElMessage.success('状态已更新')
  } catch { /* ignore */ }
}

async function acceptMission(mission: any) {
  try {
    await ElMessageBox.confirm(`确认接受任务"${mission.title}"？`, '接单确认', { type: 'warning' })
    await http.post(`/api/missions/${mission.id}/accept`)
    ElMessage.success('接单成功！')
    await loadMyMissions()
    await loadMyStatus()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function rejectMission(mission: any) {
  try {
    await ElMessageBox.confirm(`确认拒绝任务"${mission.title}"？系统将派给下一个候选。`, '拒绝确认', { type: 'warning' })
    await http.post(`/api/missions/${mission.id}/reject`)
    ElMessage.info('已拒绝')
    await loadMyMissions()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function startWork(mission: any) {
  try {
    await http.post(`/api/missions/${mission.id}/status`, { status: 'IN_PROGRESS', comment: '已出发前往现场' })
    ElMessage.success('状态已更新为进行中')
    await loadMyMissions()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function completeMission(mission: any) {
  try {
    await ElMessageBox.confirm('确认标记任务为已完成？', '完成确认', { type: 'success' })
    await http.post(`/api/missions/${mission.id}/status`, { status: 'COMPLETED', comment: '任务已完成' })
    ElMessage.success('任务已完成！')
    await loadMyMissions()
    await loadMyStatus()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

function reportProgress(mission: any) {
  reportForm.value = { missionId: mission.id, status: 'IN_PROGRESS', comment: '', attachmentUrl: '' }
  reportVisible.value = true
}

async function submitReport() {
  try {
    await http.post(`/api/missions/${reportForm.value.missionId}/status`, {
      status: reportForm.value.status,
      comment: reportForm.value.comment,
      attachmentUrl: reportForm.value.attachmentUrl
    })
    ElMessage.success('反馈已提交')
    reportVisible.value = false
    await loadMyMissions()
    await loadMyStatus()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  }
}

function pilotAvailLabel(a: string) {
  const m: Record<string, string> = { IDLE: '空闲', ON_MISSION: '任务中', LEAVE: '休假', OFFLINE: '离线' }
  return m[a] || a
}

function pilotAvailTag(a: string) {
  const m: Record<string, string> = { IDLE: 'success', ON_MISSION: 'warning', LEAVE: 'info', OFFLINE: 'info' }
  return m[a] || 'info'
}

async function loadAvailablePilots(missionId: number) {
  availablePilotsLoading.value = true
  try {
    const res = await http.get('/api/missions/org-available-pilots', { params: { missionId } })
    availablePilots.value = res || []
  } finally {
    availablePilotsLoading.value = false
  }
}

async function dispatchPilot(pilot: any) {
  try {
    await ElMessageBox.confirm(`确认指派飞手"${pilot.pilotName}"执行此任务？`, '指派确认', { type: 'warning' })
    await http.post(`/api/missions/${detailMission.value.id}/org-dispatch-pilot`, { pilotId: pilot.pilotId })
    ElMessage.success('已指派飞手')
    detailVisible.value = false
    await loadMyMissions()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '指派失败')
  }
}

async function autoDispatch() {
  try {
    await ElMessageBox.confirm('确认由系统自动指派飞手？', '自动指派确认', { type: 'warning' })
    const res = await http.post(`/api/missions/${detailMission.value.id}/org-auto-dispatch`)
    const pilotName = res?.matchDetail ? parsePilotName(res.matchDetail) : ''
    ElMessage.success(pilotName ? `系统已自动指派飞手：${pilotName}` : '系统已自动指派')
    detailVisible.value = false
    await loadMyMissions()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '自动指派失败')
  }
}

function parsePilotName(detail: string) {
  if (!detail) return ''
  try {
    const d = JSON.parse(detail)
    return d.pilotName || d.name || ''
  } catch { return '' }
}

async function viewDetail(row: any) {
  detailMission.value = row
  availablePilots.value = []
  try {
    const res = await http.get(`/api/missions/${row.id}/logs`)
    detailLogs.value = res || []
  } catch { /* ignore */ }
  if (props.role === 'ORG' && row.status === 'ACCEPTED') {
    loadAvailablePilots(row.id)
  }
  detailVisible.value = true
}

onMounted(() => {
  loadMyMissions()
  loadMyStatus()
})
</script>

<style scoped>
.pending-alert {
  background: linear-gradient(135deg, #fef0f0, #fff5f5);
  border: 1px solid #fbc4c4;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 15px;
  color: #f56c6c;
}

.alert-header strong {
  font-size: 18px;
}

.pending-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pending-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.pending-urgency {
  width: 4px;
  height: 40px;
  border-radius: 2px;
  flex-shrink: 0;
}

.urg-red { background: #f56c6c; }
.urg-yellow { background: #e6a23c; }
.urg-blue { background: #409eff; }

.pending-info {
  flex: 1;
  min-width: 0;
}

.pending-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.pending-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.pending-deadline {
  color: #f56c6c;
  font-weight: 500;
}

.pending-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-tags {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.dispatch-section {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
}

.dispatch-mode {
  flex: 1;
  min-width: 0;
}
</style>
