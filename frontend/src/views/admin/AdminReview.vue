<template>
  <div class="review-page">
    <div class="toolbar">
      <div class="toolbar-copy">
        <div class="toolbar-title">审核队列</div>
        <div class="toolbar-sub">管理员可直接新建资料，并自动提交到待审列表。</div>
      </div>
      <div class="toolbar-actions">
        <el-button type="primary" @click="openCreate('pilot')">新增待审飞手</el-button>
        <el-button type="success" @click="openCreate('org')">新增待审单位</el-button>
      </div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="待审飞手" name="p">
        <el-table :data="pilots" size="small" v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="fullName" label="姓名" />
          <el-table-column prop="recordNo" label="备案号" />
          <el-table-column prop="region" label="地区" />
          <el-table-column prop="autoResult" label="自动预审" />
          <el-table-column prop="autoScore" label="分数" width="80" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="reviewPilot(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="reviewPilot(row, false)">驳回</el-button>
              <el-button size="small" @click="freezePilot(row)">冻结</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="待审单位" name="o">
        <el-table :data="orgs" size="small" v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="orgType" label="类型">
            <template #default="{ row }">
              {{ orgTypeLabel(row.orgType) }}
            </template>
          </el-table-column>
          <el-table-column prop="creditCode" label="统一社会信用代码" />
          <el-table-column prop="autoResult" label="自动预审" />
          <el-table-column prop="autoScore" label="分数" width="80" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="reviewOrg(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="reviewOrg(row, false)">驳回</el-button>
              <el-button size="small" @click="freezeOrg(row)">冻结</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>

  <el-dialog v-model="dlg" title="审核意见" width="420px">
    <el-input v-model="comment" type="textarea" rows="3" placeholder="可选" />
    <template #footer>
      <el-button @click="dlg = false">取消</el-button>
      <el-button type="primary" @click="confirmReview">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="createDlg"
    :title="createType === 'pilot' ? '新增待审飞手' : '新增待审单位'"
    width="640px"
    destroy-on-close
  >
    <el-form
      v-if="createType === 'pilot'"
      :model="pilotForm"
      label-width="110px"
      class="create-form"
    >
      <el-form-item label="姓名" required>
        <el-input v-model="pilotForm.fullName" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="pilotForm.phone" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="pilotForm.email" />
      </el-form-item>
      <el-form-item label="身份证">
        <el-input v-model="pilotForm.idCard" />
      </el-form-item>
      <el-form-item label="备案号">
        <el-input v-model="pilotForm.recordNo" />
      </el-form-item>
      <el-form-item label="地区">
        <el-input v-model="pilotForm.region" />
      </el-form-item>
    </el-form>

    <el-form v-else :model="orgForm" label-width="140px" class="create-form">
      <el-form-item label="单位名称" required>
        <el-input v-model="orgForm.name" />
      </el-form-item>
      <el-form-item label="单位类型">
        <el-select v-model="orgForm.orgType" style="width: 100%">
          <el-option label="企业" value="ENTERPRISE" />
          <el-option label="志愿者机构" value="VOLUNTEER" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>
      <el-form-item label="统一社会信用代码">
        <el-input v-model="orgForm.creditCode" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="orgForm.contactName" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="orgForm.phone" />
      </el-form-item>
      <el-form-item label="地址">
        <el-input v-model="orgForm.address" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="createDlg = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="submitCreate">创建并加入待审</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import http from '@/api/http'
import { ElMessage } from 'element-plus'

type ReviewPending = { type: 'p' | 'o'; id: number; pass: boolean } | null
type CreateType = 'pilot' | 'org'

const tab = ref('p')
const loading = ref(false)
const creating = ref(false)
const pilots = ref<any[]>([])
const orgs = ref<any[]>([])
const dlg = ref(false)
const comment = ref('')
const pending = ref<ReviewPending>(null)

const createDlg = ref(false)
const createType = ref<CreateType>('pilot')
const pilotForm = reactive({
  fullName: '',
  phone: '',
  email: '',
  idCard: '',
  recordNo: '',
  region: '',
})
const orgForm = reactive({
  name: '',
  orgType: 'ENTERPRISE',
  creditCode: '',
  contactName: '',
  phone: '',
  address: '',
})

async function load() {
  loading.value = true
  try {
    const [p, o] = await Promise.all([
      http.get('/api/admin/review/pilots/pending', { params: { page: 1, size: 50 } }),
      http.get('/api/admin/review/organizations/pending', { params: { page: 1, size: 50 } }),
    ])
    pilots.value = p.records || []
    orgs.value = o.records || []
  } finally {
    loading.value = false
  }
}

function reviewPilot(row: any, pass: boolean) {
  pending.value = { type: 'p', id: row.id, pass }
  comment.value = ''
  dlg.value = true
}

function reviewOrg(row: any, pass: boolean) {
  pending.value = { type: 'o', id: row.id, pass }
  comment.value = ''
  dlg.value = true
}

async function confirmReview() {
  if (!pending.value) return
  const body = { decision: pending.value.pass ? 'APPROVE' : 'REJECT', comment: comment.value }
  if (pending.value.type === 'p') {
    await http.post(`/api/admin/review/pilots/${pending.value.id}`, body)
  } else {
    await http.post(`/api/admin/review/organizations/${pending.value.id}`, body)
  }
  dlg.value = false
  ElMessage.success('已处理')
  await load()
}

async function freezePilot(row: any) {
  await http.post(`/api/admin/review/pilots/${row.id}/freeze`)
  ElMessage.success('已冻结')
  await load()
}

async function freezeOrg(row: any) {
  await http.post(`/api/admin/review/organizations/${row.id}/freeze`)
  ElMessage.success('已冻结')
  await load()
}

function openCreate(type: CreateType) {
  createType.value = type
  createDlg.value = true
  if (type === 'pilot') {
    resetPilotForm()
  } else {
    resetOrgForm()
  }
}

function resetPilotForm() {
  pilotForm.fullName = ''
  pilotForm.phone = ''
  pilotForm.email = ''
  pilotForm.idCard = ''
  pilotForm.recordNo = ''
  pilotForm.region = ''
}

function resetOrgForm() {
  orgForm.name = ''
  orgForm.orgType = 'ENTERPRISE'
  orgForm.creditCode = ''
  orgForm.contactName = ''
  orgForm.phone = ''
  orgForm.address = ''
}

async function submitCreate() {
  creating.value = true
  try {
    if (createType.value === 'pilot') {
      if (!pilotForm.fullName.trim()) {
        ElMessage.warning('请填写飞手姓名')
        return
      }
      const created = await http.post('/api/pilots', { ...pilotForm })
      await http.post(`/api/pilots/${created.id}/submit`)
      tab.value = 'p'
      ElMessage.success('待审飞手已创建')
    } else {
      if (!orgForm.name.trim()) {
        ElMessage.warning('请填写单位名称')
        return
      }
      const created = await http.post('/api/organizations', { ...orgForm })
      await http.post(`/api/organizations/${created.id}/submit`)
      tab.value = 'o'
      ElMessage.success('待审单位已创建')
    }
    createDlg.value = false
    await load()
  } finally {
    creating.value = false
  }
}

function orgTypeLabel(type: string) {
  const map: Record<string, string> = {
    ENTERPRISE: '企业',
    VOLUNTEER: '志愿者机构',
    OTHER: '其他',
  }
  return map[type] || type
}

onMounted(load)
</script>

<style scoped>
.review-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
}

.toolbar-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.toolbar-sub {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.create-form {
  padding-right: 20px;
}
</style>
