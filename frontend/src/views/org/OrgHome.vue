<template>
  <div class="org-home">
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card v-loading="loading" header="单位资料">
          <el-alert
            title="提交审核只校验必填项是否填写，不再按手机号或统一信用代码位数限制。"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
          />
          <el-descriptions v-if="org" :column="2" border>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(org.status)">{{ statusLabel(org.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="自动预审">
              <el-tag v-if="org.autoResult" :type="autoResultTagType(org.autoResult)" size="small">
                {{ autoResultLabel(org.autoResult) }}
              </el-tag>
              <span v-else style="color: #909399">未预审</span>
              <span v-if="org.autoScore != null" style="margin-left: 8px; color: #606266">
                分数: {{ org.autoScore }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
          <el-form :model="form" label-width="120px" style="max-width: 640px; margin-top: 16px">
            <el-form-item label="名称"><el-input v-model="form.name" placeholder="如：XX无人机救援有限公司" /></el-form-item>
            <el-form-item label="类型">
              <el-select v-model="form.orgType">
                <el-option label="企业" value="ENTERPRISE" />
                <el-option label="志愿者机构" value="VOLUNTEER" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
            <el-form-item label="统一社会信用代码"><el-input v-model="form.creditCode" placeholder="如：91310000SAMPLE0001" /></el-form-item>
            <el-form-item label="联系人"><el-input v-model="form.contactName" placeholder="如：李四" /></el-form-item>
            <el-form-item label="手机"><el-input v-model="form.phone" placeholder="如：02100010001" /></el-form-item>
            <el-form-item label="地址"><el-input v-model="form.address" placeholder="如：上海市浦东新区XX路100号" /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="save">保存</el-button>
              <el-button type="success" @click="submit">提交审核</el-button>
              <el-button type="warning" @click="openFlowDialog">查看审核流程</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card header="审核状态速览">
          <div v-if="org" class="status-overview">
            <div class="current-status">
              <div class="status-icon" :class="statusClass(org.status)">
                <el-icon :size="48">
                  <component :is="statusIcon(org.status)" />
                </el-icon>
              </div>
              <div class="status-text">{{ statusLabel(org.status) }}</div>
            </div>
            <el-divider />
            <div class="quick-info">
              <div class="info-item">
                <span class="info-label">自动预审结果</span>
                <span class="info-value">{{ org.autoResult ? autoResultLabel(org.autoResult) : '未预审' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">预审分数</span>
                <span class="info-value">{{ org.autoScore != null ? org.autoScore : '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">提交时间</span>
                <span class="info-value">{{ org.updatedAt ? org.updatedAt.replace('T', ' ').substring(0, 16) : '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">单位类型</span>
                <span class="info-value">{{ orgTypeLabel(org.orgType) }}</span>
              </div>
            </div>
            <el-button type="warning" style="width: 100%; margin-top: 16px" @click="openFlowDialog">
              查看详细审核流程
            </el-button>
          </div>
          <el-empty v-else description="暂无资料" />
        </el-card>

        <el-card header="审核记录" style="margin-top: 16px">
          <el-timeline v-if="auditLogs.length > 0">
            <el-timeline-item
              v-for="(log, idx) in auditLogs.slice(0, 8)"
              :key="idx"
              :timestamp="log.createdAt ? log.createdAt.replace('T', ' ').substring(0, 16) : ''"
              :type="auditActionType(log.action)"
              placement="top"
            >
              <div class="audit-action">{{ auditActionLabel(log.action) }}</div>
              <div v-if="log.commentText" class="audit-comment">{{ log.commentText }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审核记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <el-card header="单位飞手" v-loading="pilotsLoading">
          <el-table :data="orgPilots" border stripe size="small">
            <el-table-column prop="pilotName" label="飞手姓名" min-width="100" />
            <el-table-column prop="pilotPhone" label="手机号" min-width="120">
              <template #default="{ row }">{{ maskPhone(row.pilotPhone) }}</template>
            </el-table-column>
            <el-table-column prop="pilotStatus" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="pilotStatusTag(row.pilotStatus)" size="small">{{ pilotStatusLabel(row.pilotStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="danger" size="small" plain @click="removePilot(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!pilotsLoading && orgPilots.length === 0" description="暂无飞手" :image-size="60" />
          <div style="margin-top: 12px">
            <el-button type="primary" @click="openInviteDialog">邀请飞手加入</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card header="待处理申请" v-loading="applicationsLoading">
          <div v-if="pendingApplications.length > 0" class="application-list">
            <div v-for="app in pendingApplications" :key="app.id" class="application-item">
              <div class="application-info">
                <span class="application-name">{{ app.pilotName }}</span>
                <el-tag type="warning" size="small">待处理</el-tag>
              </div>
              <div class="application-actions">
                <el-button type="success" size="small" @click="acceptApplication(app)">接受</el-button>
                <el-button type="danger" size="small" plain @click="rejectApplication(app)">拒绝</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无待处理申请" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="flowDialogVisible" title="审核流程查询" width="720px" destroy-on-close>
      <div class="flow-container">
        <div class="flow-steps">
          <div
            v-for="(step, idx) in flowSteps"
            :key="idx"
            class="flow-step"
            :class="{
              active: idx === currentStepIndex,
              completed: idx < currentStepIndex,
              rejected: isRejected && idx === currentStepIndex,
              frozen: isFrozen && idx === currentStepIndex
            }"
            @click="onStepClick(idx)"
          >
            <div class="step-circle">
              <el-icon :size="24">
                <component :is="step.icon" />
              </el-icon>
            </div>
            <div class="step-label">{{ step.label }}</div>
            <div v-if="idx < flowSteps.length - 1" class="step-arrow">
              <el-icon :size="20"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>

        <div class="flow-detail">
          <el-card shadow="never" class="detail-card">
            <template #header>
              <div class="detail-header">
                <el-tag :type="selectedStepTagType" size="large">{{ selectedStep?.label }}</el-tag>
                <span class="detail-status">{{ stepStatusText }}</span>
              </div>
            </template>
            <div v-if="selectedStep" class="detail-content">
              <p class="detail-desc">{{ selectedStep.description }}</p>
              <div v-if="selectedStep.idx === currentStepIndex" class="detail-current">
                <el-alert
                  :title="currentStepAlertTitle"
                  :type="currentStepAlertType"
                  :description="currentStepAlertDesc"
                  show-icon
                  :closable="false"
                />
              </div>
              <div v-if="selectedStep.idx === 1 && flowData.autoResult" class="detail-auto">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="自动预审结果">
                    <el-tag :type="autoResultTagType(flowData.autoResult)" size="small">
                      {{ autoResultLabel(flowData.autoResult) }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="预审分数">
                    {{ flowData.autoScore }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </div>
          </el-card>

          <div v-if="stepAuditLogs.length > 0" class="step-logs">
            <div class="step-logs-title">相关操作记录</div>
            <el-table :data="stepAuditLogs" size="small" border>
              <el-table-column prop="action" label="操作" width="140">
                <template #default="{ row }">{{ auditActionLabel(row.action) }}</template>
              </el-table-column>
              <el-table-column prop="commentText" label="说明" />
              <el-table-column prop="createdAt" label="时间" width="160">
                <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T', ' ').substring(0, 16) : '' }}</template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="inviteDialogVisible" title="邀请飞手加入" width="600px" destroy-on-close>
      <el-table :data="publicPilots" v-loading="publicPilotsLoading" border stripe size="small">
        <el-table-column prop="fullName" label="飞手姓名" min-width="100" />
        <el-table-column prop="phoneMasked" label="手机号" min-width="120" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="!isOrgPilot(row.id)"
              type="primary"
              size="small"
              @click="invitePilot(row)"
            >邀请</el-button>
            <el-tag v-else type="info" size="small">已加入</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!publicPilotsLoading && publicPilots.length === 0" description="暂无可邀请的飞手" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import http from '@/api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Edit, Search, Check, Close, Lock, ArrowRight, Document, OfficeBuilding
} from '@element-plus/icons-vue'

const loading = ref(false)
const org = ref<any>(null)
const auditLogs = ref<any[]>([])
const form = reactive<any>({})

const flowDialogVisible = ref(false)
const flowData = ref<any>({ currentStatus: 'DRAFT', autoResult: '', autoScore: 0, auditLogs: [] })
const selectedStepIdx = ref(0)

const pilotsLoading = ref(false)
const orgPilots = ref<any[]>([])

const inviteDialogVisible = ref(false)
const publicPilotsLoading = ref(false)
const publicPilots = ref<any[]>([])

const applicationsLoading = ref(false)
const pendingApplications = ref<any[]>([])

const flowSteps = [
  { idx: 0, label: '草稿', icon: Edit, description: '填写单位基本信息（名称、类型、信用代码等）。资料保存在草稿状态，可随时修改。' },
  { idx: 1, label: '自动预审', icon: Search, description: '提交后系统自动进行预审评分，根据分数判定：通过(≥70)→人工复核，需人工(45-69)→待审，不通过(<45)→直接驳回。' },
  { idx: 2, label: '人工审核', icon: Document, description: '管理员对预审通过的资料进行人工复核，决定最终通过或驳回。' },
  { idx: 3, label: '审核通过', icon: Check, description: '恭喜！您的单位备案已通过审核，可以在公开页面被查询到。' },
]

const currentStepIndex = computed(() => {
  const s = flowData.value.currentStatus
  if (s === 'DRAFT') return 0
  if (s === 'PENDING_REVIEW') {
    if (flowData.value.autoResult === 'PASS') return 2
    return 1
  }
  if (s === 'APPROVED') return 3
  if (s === 'REJECTED') return 2
  if (s === 'FROZEN') return 3
  return 0
})

const isRejected = computed(() => flowData.value.currentStatus === 'REJECTED')
const isFrozen = computed(() => flowData.value.currentStatus === 'FROZEN')

const selectedStep = computed(() => flowSteps[selectedStepIdx.value] || null)
const selectedStepTagType = computed(() => {
  const idx = selectedStepIdx.value
  if (idx < currentStepIndex.value) return 'success'
  if (idx === currentStepIndex.value) {
    if (isRejected.value) return 'danger'
    if (isFrozen.value) return 'warning'
    return 'primary'
  }
  return 'info'
})

const stepStatusText = computed(() => {
  const idx = selectedStepIdx.value
  if (idx < currentStepIndex.value) return '✅ 已完成'
  if (idx === currentStepIndex.value) {
    if (isRejected.value) return '❌ 已驳回'
    if (isFrozen.value) return '🔒 已冻结'
    return '🔄 当前阶段'
  }
  return '⏳ 待进行'
})

const currentStepAlertTitle = computed(() => {
  const s = flowData.value.currentStatus
  if (s === 'DRAFT') return '您当前处于草稿阶段'
  if (s === 'PENDING_REVIEW') return '您的单位资料正在审核中'
  if (s === 'APPROVED') return '您的单位备案已通过审核！'
  if (s === 'REJECTED') return '您的单位备案已被驳回'
  if (s === 'FROZEN') return '您的单位账号已被冻结'
  return ''
})

const currentStepAlertType = computed(() => {
  const s = flowData.value.currentStatus
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'error'
  if (s === 'FROZEN') return 'warning'
  return 'info'
})

const currentStepAlertDesc = computed(() => {
  const s = flowData.value.currentStatus
  if (s === 'DRAFT') return '请完善单位资料后点击"提交审核"按钮，系统将自动进行预审。'
  if (s === 'PENDING_REVIEW') return '请耐心等待管理员审核，您可以在审核记录中查看进度。'
  if (s === 'APPROVED') return '您已通过审核，可在公开页面被查询到。'
  if (s === 'REJECTED') return '您可以修改资料后重新提交审核。'
  if (s === 'FROZEN') return '账号已被冻结，如需解冻请联系管理员。'
  return ''
})

const stepAuditLogs = computed(() => {
  return flowData.value.auditLogs || []
})

function onStepClick(idx: number) {
  selectedStepIdx.value = idx
}

function statusLabel(s: string) {
  const map: Record<string, string> = { DRAFT: '草稿', PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已驳回', FROZEN: '已冻结' }
  return map[s] || s
}

function statusTagType(s: string) {
  const map: Record<string, string> = { DRAFT: 'info', PENDING_REVIEW: 'warning', APPROVED: 'success', REJECTED: 'danger', FROZEN: 'warning' }
  return map[s] || 'info'
}

function statusClass(s: string) {
  const map: Record<string, string> = { DRAFT: 'draft', PENDING_REVIEW: 'pending', APPROVED: 'approved', REJECTED: 'rejected', FROZEN: 'frozen' }
  return map[s] || ''
}

function statusIcon(s: string) {
  const map: Record<string, any> = { DRAFT: Edit, PENDING_REVIEW: Search, APPROVED: Check, REJECTED: Close, FROZEN: Lock }
  return map[s] || Edit
}

function autoResultLabel(r: string) {
  const map: Record<string, string> = { PASS: '自动通过', NEED_MANUAL: '需人工复核', REJECT: '自动驳回' }
  return map[r] || r
}

function autoResultTagType(r: string) {
  const map: Record<string, string> = { PASS: 'success', NEED_MANUAL: 'warning', REJECT: 'danger' }
  return map[r] || 'info'
}

function orgTypeLabel(t: string) {
  const map: Record<string, string> = { ENTERPRISE: '企业', VOLUNTEER: '志愿者机构', OTHER: '其他' }
  return map[t] || t
}

function auditActionLabel(a: string) {
  const map: Record<string, string> = {
    CREATE: '创建', UPDATE: '更新', DELETE: '删除',
    SUBMIT_AUTO: '提交(自动预审)', MANUAL_REVIEW: '人工审核',
    FREEZE: '冻结'
  }
  return map[a] || a
}

function auditActionType(a: string) {
  const map: Record<string, string> = {
    CREATE: 'primary', UPDATE: '', DELETE: 'danger',
    SUBMIT_AUTO: 'warning', MANUAL_REVIEW: 'success',
    FREEZE: 'danger'
  }
  return map[a] || 'info'
}

async function load() {
  loading.value = true
  try {
    org.value = await http.get('/api/organizations/me')
    Object.assign(form, org.value)
    const logs = await http.get('/api/organizations/me/audit-logs', { params: { page: 1, size: 20 } })
    auditLogs.value = logs.records || []
  } finally {
    loading.value = false
  }
}

async function save() {
  await http.put(`/api/organizations/${org.value.id}`, form)
  ElMessage.success('已保存')
  await load()
}

async function submit() {
  await http.post(`/api/organizations/${org.value.id}/submit`)
  ElMessage.success('已提交审核，可在管理员审核队列查看')
  await load()
}

function maskPhone(phone: string) {
  if (!phone) return '-'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

function pilotStatusLabel(s: string) {
  const map: Record<string, string> = { ACTIVE: '在职', INACTIVE: '离线', ON_MISSION: '任务中' }
  return map[s] || s
}

function pilotStatusTag(s: string) {
  const map: Record<string, string> = { ACTIVE: 'success', INACTIVE: 'info', ON_MISSION: 'warning' }
  return map[s] || 'info'
}

function isOrgPilot(pilotId: number) {
  return orgPilots.value.some(p => p.pilotId === pilotId)
}

async function loadOrgPilots() {
  pilotsLoading.value = true
  try {
    const res = await http.get('/api/pilot-org/org-pilots')
    orgPilots.value = res || []
  } catch {
    orgPilots.value = []
  } finally {
    pilotsLoading.value = false
  }
}

async function removePilot(pilot: any) {
  try {
    await ElMessageBox.confirm(`确认移除飞手"${pilot.pilotName || pilot.pilotId}"？`, '移除确认', { type: 'warning' })
    await http.post('/api/pilot-org/remove-pilot', { pilotId: pilot.pilotId })
    ElMessage.success('已移除')
    await loadOrgPilots()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function openInviteDialog() {
  inviteDialogVisible.value = true
  publicPilotsLoading.value = true
  try {
    const res = await http.get('/api/public/pilots', { params: { page: 1, size: 100 } })
    publicPilots.value = res?.records || []
  } finally {
    publicPilotsLoading.value = false
  }
}

async function invitePilot(pilot: any) {
  try {
    await http.post('/api/pilot-org/invite', { pilotId: pilot.id })
    ElMessage.success('已发送邀请')
    await loadOrgPilots()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '邀请失败')
  }
}

async function loadPendingApplications() {
  applicationsLoading.value = true
  try {
    const res = await http.get('/api/pilot-org/pending-applications')
    pendingApplications.value = res || []
  } catch {
    pendingApplications.value = []
  } finally {
    applicationsLoading.value = false
  }
}

async function acceptApplication(app: any) {
  try {
    await http.post(`/api/pilot-org/accept-application/${app.id}`)
    ElMessage.success('已接受申请')
    await loadPendingApplications()
    await loadOrgPilots()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function rejectApplication(app: any) {
  try {
    await ElMessageBox.confirm(`确认拒绝飞手"${app.pilotName}"的申请？`, '拒绝确认', { type: 'warning' })
    await http.post(`/api/pilot-org/reject-application/${app.id}`)
    ElMessage.info('已拒绝申请')
    await loadPendingApplications()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function openFlowDialog() {
  try {
    flowData.value = await http.get('/api/organizations/me/status-flow')
    selectedStepIdx.value = currentStepIndex.value
    flowDialogVisible.value = true
  } catch {
    ElMessage.error('获取审核流程失败')
  }
}

onMounted(() => {
  load()
  loadOrgPilots()
  loadPendingApplications()
})
</script>

<style scoped>
.org-home {
  padding: 0;
}

.status-overview {
  text-align: center;
}

.current-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.status-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.status-icon.draft { background: #909399; }
.status-icon.pending { background: #e6a23c; }
.status-icon.approved { background: #67c23a; }
.status-icon.rejected { background: #f56c6c; }
.status-icon.frozen { background: #909399; }

.status-text {
  font-size: 18px;
  font-weight: 700;
}

.quick-info {
  text-align: left;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-label {
  color: #909399;
  font-size: 13px;
}

.info-value {
  color: #303133;
  font-size: 13px;
  font-weight: 500;
}

.audit-action {
  font-weight: 600;
  font-size: 13px;
}

.audit-comment {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}

.flow-container {
  padding: 8px 0;
}

.flow-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.flow-step {
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
}

.flow-step:hover .step-circle {
  transform: scale(1.1);
}

.step-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e4e7ed;
  color: #909399;
  transition: all 0.3s;
  border: 3px solid transparent;
}

.flow-step.completed .step-circle {
  background: #67c23a;
  color: #fff;
  border-color: #67c23a;
}

.flow-step.active .step-circle {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
  box-shadow: 0 0 12px rgba(64, 158, 255, 0.4);
}

.flow-step.rejected .step-circle {
  background: #f56c6c;
  color: #fff;
  border-color: #f56c6c;
  box-shadow: 0 0 12px rgba(245, 108, 108, 0.4);
}

.flow-step.frozen .step-circle {
  background: #909399;
  color: #fff;
  border-color: #909399;
}

.step-label {
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-top: 6px;
  min-width: 56px;
}

.flow-step.completed .step-label { color: #67c23a; }
.flow-step.active .step-label { color: #409eff; }
.flow-step.rejected .step-label { color: #f56c6c; }

.step-arrow {
  color: #c0c4cc;
  margin: 0 8px;
  margin-bottom: 22px;
}

.detail-card {
  margin-bottom: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-status {
  font-size: 14px;
  color: #606266;
}

.detail-desc {
  color: #606266;
  line-height: 1.8;
  margin-bottom: 12px;
}

.detail-current {
  margin-top: 12px;
}

.detail-auto {
  margin-top: 12px;
}

.step-logs {
  margin-top: 16px;
}

.step-logs-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.application-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.application-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fafafa;
}

.application-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.application-name {
  font-weight: 600;
  font-size: 14px;
}

.application-actions {
  display: flex;
  gap: 6px;
}
</style>
