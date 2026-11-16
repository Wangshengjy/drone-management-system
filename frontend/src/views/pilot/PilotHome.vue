<template>
  <div class="pilot-home">
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card v-loading="loading" header="我的资料">
          <el-alert
            title="提交审核只校验必填项是否填写，不再按备案号或手机号位数限制。"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
          />
          <el-descriptions v-if="pilot" :column="2" border>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(pilot.status)">{{ statusLabel(pilot.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="自动预审">
              <el-tag v-if="pilot.autoResult" :type="autoResultTagType(pilot.autoResult)" size="small">
                {{ autoResultLabel(pilot.autoResult) }}
              </el-tag>
              <span v-else style="color: #909399">未预审</span>
              <span v-if="pilot.autoScore != null" style="margin-left: 8px; color: #606266">
                分数: {{ pilot.autoScore }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
          <el-form :model="form" label-width="100px" style="max-width: 640px; margin-top: 16px">
            <el-form-item label="姓名"><el-input v-model="form.fullName" placeholder="如：张三" /></el-form-item>
            <el-form-item label="手机"><el-input v-model="form.phone" placeholder="如：13800010001" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="如：alpha@example.com" /></el-form-item>
            <el-form-item label="身份证"><el-input v-model="form.idCard" placeholder="如：110101199001010011" /></el-form-item>
            <el-form-item label="备案号"><el-input v-model="form.recordNo" placeholder="如：UAS-SAMPLE-ALPHA" /></el-form-item>
            <el-form-item label="地区"><el-input v-model="form.region" placeholder="如：Shanghai" /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="save">保存</el-button>
              <el-button type="success" @click="submit" v-if="pilot?.status === 'DRAFT' || pilot?.status === 'REJECTED'">提交审核(触发自动预审)</el-button>
              <el-button type="warning" @click="openFlowDialog">查看审核流程</el-button>
            </el-form-item>
          </el-form>

          <el-divider />
          <div class="sub">资质证书</div>
          <el-table :data="certs" size="small" style="margin-top: 8px">
            <el-table-column prop="certType" label="类型" />
            <el-table-column prop="certNo" label="编号" />
            <el-table-column prop="validUntil" label="有效期" />
            <el-table-column label="附件">
              <template #default="{ row }">
                <el-link v-if="row.attachmentUrl" :href="row.attachmentUrl" target="_blank" type="primary">查看</el-link>
              </template>
            </el-table-column>
          </el-table>
          <el-form :inline="true" style="margin-top: 12px">
            <el-form-item label="类型"><el-input v-model="certForm.certType" placeholder="如 AOPA" /></el-form-item>
            <el-form-item label="编号"><el-input v-model="certForm.certNo" /></el-form-item>
            <el-form-item label="有效期"><el-date-picker v-model="certForm.validUntil" value-format="YYYY-MM-DD" /></el-form-item>
            <el-form-item label="附件URL">
              <el-input v-model="certForm.attachmentUrl" style="width: 260px" placeholder="先上传文件或手填URL" />
            </el-form-item>
            <el-form-item>
              <el-upload :show-file-list="false" :http-request="uploadFile">
                <el-button>上传文件</el-button>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="addCert">添加证书</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card header="审核状态速览">
          <div v-if="pilot" class="status-overview">
            <div class="current-status">
              <div class="status-icon" :class="statusClass(pilot.status)">
                <el-icon :size="48">
                  <component :is="statusIcon(pilot.status)" />
                </el-icon>
              </div>
              <div class="status-text">{{ statusLabel(pilot.status) }}</div>
            </div>
            <el-divider />
            <div class="quick-info">
              <div class="info-item">
                <span class="info-label">自动预审结果</span>
                <span class="info-value">{{ pilot.autoResult ? autoResultLabel(pilot.autoResult) : '未预审' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">预审分数</span>
                <span class="info-value">{{ pilot.autoScore != null ? pilot.autoScore : '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">提交时间</span>
                <span class="info-value">{{ pilot.updatedAt ? pilot.updatedAt.replace('T', ' ').substring(0, 16) : '-' }}</span>
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

        <el-card header="我的单位" style="margin-top: 16px">
          <div v-if="myOrg" class="org-info">
            <div class="info-item">
              <span class="info-label">单位名称</span>
              <span class="info-value">{{ myOrg.orgName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">单位类型</span>
              <span class="info-value">{{ orgTypeLabel(myOrg.orgType) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">联系人</span>
              <span class="info-value">{{ myOrg.orgContactPerson || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">单位状态</span>
              <span class="info-value">
                <el-tag :type="myOrg.orgStatus === 'APPROVED' ? 'success' : 'warning'" size="small">
                  {{ orgStatusLabel(myOrg.orgStatus) }}
                </el-tag>
              </span>
            </div>
            <el-button type="danger" style="width: 100%; margin-top: 12px" @click="handleLeaveOrg">退出单位</el-button>
          </div>
          <div v-else class="org-empty">
            <el-empty description="暂未加入任何单位" :image-size="60" />
            <el-button type="primary" style="width: 100%" @click="openApplyDialog">申请加入单位</el-button>
          </div>
        </el-card>

        <el-card header="待处理邀请" style="margin-top: 16px">
          <div v-if="pendingInvitations.length > 0">
            <div v-for="inv in pendingInvitations" :key="inv.id" class="invitation-item">
              <div class="invitation-info">
                <span class="invitation-org">{{ inv.orgName || ('单位#' + inv.orgId) }}</span>
              </div>
              <div class="invitation-actions">
                <el-button type="success" size="small" @click="handleAcceptInvitation(inv.id)">接受</el-button>
                <el-button type="danger" size="small" @click="handleRejectInvitation(inv.id)">拒绝</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无待处理邀请" :image-size="60" />
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

    <el-dialog v-model="applyDialogVisible" title="申请加入单位" width="600px" destroy-on-close>
      <el-table :data="availableOrgs" v-loading="availableOrgsLoading" size="small" border>
        <el-table-column prop="name" label="单位名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="orgType" label="类型" width="110">
          <template #default="{ row }">{{ orgTypeLabel(row.orgType) }}</template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="100">
          <template #default="{ row }">{{ row.contactPerson || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleApplyOrg(row.id)">申请加入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import http from '@/api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Edit, Upload, Search, Check, Close, Lock, ArrowRight, Document, WarningFilled, CircleCheckFilled
} from '@element-plus/icons-vue'

const loading = ref(false)
const pilot = ref<any>(null)
const certs = ref<any[]>([])
const auditLogs = ref<any[]>([])
const form = reactive<any>({})
const certForm = reactive<any>({ certType: '', certNo: '', validUntil: '', attachmentUrl: '' })

const myOrg = ref<any>(null)
const pendingInvitations = ref<any[]>([])
const applyDialogVisible = ref(false)
const availableOrgs = ref<any[]>([])
const availableOrgsLoading = ref(false)

const flowDialogVisible = ref(false)
const flowData = ref<any>({ currentStatus: 'DRAFT', autoResult: '', autoScore: 0, auditLogs: [] })
const selectedStepIdx = ref(0)

const flowSteps = [
  { idx: 0, label: '草稿', icon: Edit, description: '填写个人基本信息、上传资质证书。资料保存在草稿状态，可随时修改。' },
  { idx: 1, label: '自动预审', icon: Search, description: '提交后系统自动进行预审评分，根据分数判定：通过(≥70)→人工复核，需人工(45-69)→待审，不通过(<45)→直接驳回。' },
  { idx: 2, label: '人工审核', icon: Document, description: '管理员对预审通过的资料进行人工复核，决定最终通过或驳回。' },
  { idx: 3, label: '审核通过', icon: Check, description: '恭喜！您的飞手备案已通过审核，可以在公开页面被查询到。' },
]

const statusOrder: Record<string, number> = {
  DRAFT: 0,
  PENDING_REVIEW: 2,
  APPROVED: 3,
  REJECTED: 3,
  FROZEN: 3,
}

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
  if (s === 'PENDING_REVIEW') return '您的资料正在审核中'
  if (s === 'APPROVED') return '您的备案已通过审核！'
  if (s === 'REJECTED') return '您的备案已被驳回'
  if (s === 'FROZEN') return '您的账号已被冻结'
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
  if (s === 'DRAFT') return '请完善个人资料后点击"提交审核"按钮，系统将自动进行预审。'
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

function auditActionLabel(a: string) {
  const map: Record<string, string> = {
    CREATE: '创建', UPDATE: '更新', DELETE: '删除',
    SUBMIT_AUTO: '提交(自动预审)', MANUAL_REVIEW: '人工审核',
    CERT_ADD: '添加证书', FREEZE: '冻结'
  }
  return map[a] || a
}

function auditActionType(a: string) {
  const map: Record<string, string> = {
    CREATE: 'primary', UPDATE: '', DELETE: 'danger',
    SUBMIT_AUTO: 'warning', MANUAL_REVIEW: 'success',
    CERT_ADD: 'primary', FREEZE: 'danger'
  }
  return map[a] || 'info'
}

async function load() {
  loading.value = true
  try {
    pilot.value = await http.get('/api/pilots/me')
    Object.assign(form, pilot.value)
    const c = await http.get(`/api/pilots/${pilot.value.id}/certificates`)
    certs.value = c.records || []
    const logs = await http.get('/api/pilots/me/audit-logs', { params: { page: 1, size: 20 } })
    auditLogs.value = logs.records || []
  } finally {
    loading.value = false
  }
}

async function save() {
  await http.put(`/api/pilots/${pilot.value.id}`, form)
  ElMessage.success('已保存')
  await load()
}

async function submit() {
  await http.post(`/api/pilots/${pilot.value.id}/submit`)
  ElMessage.success('已提交审核，可在管理员审核队列查看')
  await load()
}

async function addCert() {
  await http.post(`/api/pilots/${pilot.value.id}/certificates`, certForm)
  ElMessage.success('已添加证书，系统已自动重新评分')
  certForm.certType = ''
  certForm.certNo = ''
  certForm.validUntil = ''
  certForm.attachmentUrl = ''
  await load()
}

async function uploadFile(opt: { file: File }) {
  const fd = new FormData()
  fd.append('file', opt.file as File)
  const data = await http.post('/api/files/upload', fd)
  certForm.attachmentUrl = data.url
  ElMessage.success('上传成功，已填入附件URL')
}

async function openFlowDialog() {
  try {
    flowData.value = await http.get('/api/pilots/me/status-flow')
    selectedStepIdx.value = currentStepIndex.value
    flowDialogVisible.value = true
  } catch {
    ElMessage.error('获取审核流程失败')
  }
}

function orgTypeLabel(t: string) {
  const map: Record<string, string> = { ENTERPRISE: '企业', VOLUNTEER: '志愿者机构', OTHER: '其他' }
  return map[t] || t
}

function orgStatusLabel(s: string) {
  const map: Record<string, string> = { PENDING_REVIEW: '审核中', APPROVED: '已通过', REJECTED: '已驳回', FROZEN: '已冻结' }
  return map[s] || s
}

async function loadMyOrg() {
  try {
    myOrg.value = await http.get('/api/pilot-org/my-org')
  } catch {
    myOrg.value = null
  }
}

async function loadPendingInvitations() {
  try {
    const res = await http.get('/api/pilot-org/pending-invitations')
    pendingInvitations.value = res || []
  } catch {
    pendingInvitations.value = []
  }
}

async function openApplyDialog() {
  applyDialogVisible.value = true
  availableOrgsLoading.value = true
  try {
    const res = await http.get('/api/public/organizations', { params: { page: 1, size: 100 } })
    availableOrgs.value = res?.records || []
  } catch {
    availableOrgs.value = []
  } finally {
    availableOrgsLoading.value = false
  }
}

async function handleApplyOrg(orgId: number) {
  try {
    await http.post('/api/pilot-org/apply', { orgId })
    ElMessage.success('已提交申请')
    applyDialogVisible.value = false
  } catch {
    ElMessage.error('申请失败')
  }
}

async function handleLeaveOrg() {
  if (!myOrg.value) return
  try {
    await ElMessageBox.confirm('确定要退出当前单位吗？', '退出确认', { type: 'warning' })
    await http.post('/api/pilot-org/leave', { orgId: myOrg.value.id })
    ElMessage.success('已退出单位')
    myOrg.value = null
    await loadMyOrg()
  } catch {
    // cancelled or error
  }
}

async function handleAcceptInvitation(id: number) {
  try {
    await http.post(`/api/pilot-org/accept-invitation/${id}`)
    ElMessage.success('已接受邀请')
    await loadPendingInvitations()
    await loadMyOrg()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleRejectInvitation(id: number) {
  try {
    await http.post(`/api/pilot-org/reject-invitation/${id}`)
    ElMessage.success('已拒绝邀请')
    await loadPendingInvitations()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  load()
  loadMyOrg()
  loadPendingInvitations()
})
</script>

<style scoped>
.pilot-home {
  padding: 0;
}

.sub {
  font-weight: 600;
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

.flow-step.completed + .step-arrow,
.flow-step.completed ~ .step-arrow {
  color: #67c23a;
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

.org-info .info-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.org-empty {
  text-align: center;
}

.invitation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.invitation-item:last-child {
  border-bottom: none;
}

.invitation-org {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.invitation-actions {
  display: flex;
  gap: 8px;
}
</style>
