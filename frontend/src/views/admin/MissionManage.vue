<template>
  <div class="mission-manage">
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>应急任务列表</span>
              <div>
                <el-button type="primary" @click="showCreateDialog">一键发单</el-button>
                <el-button @click="loadMissions(); loadStats()">刷新</el-button>
              </div>
            </div>
          </template>
          <el-table :data="missions" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="55" />
            <el-table-column prop="title" label="任务标题" min-width="150" show-overflow-tooltip />
            <el-table-column prop="missionType" label="类型" width="95">
              <template #default="{ row }"><el-tag :type="missionTypeTag(row.missionType)" size="small">{{ missionTypeLabel(row.missionType) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="urgency" label="紧急" width="75">
              <template #default="{ row }"><el-tag :type="urgencyTagType(row.urgency)" size="small" effect="dark">{{ urgencyLabel(row.urgency) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="85">
              <template #default="{ row }"><el-tag :type="missionStatusTag(row.status)" size="small">{{ missionStatusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="需求" width="90">
              <template #default="{ row }">
                <span style="font-size:12px">{{ row.requiredPilotCount ?? 1 }}飞手 / {{ row.requiredOrgCount ?? 1 }}单位</span>
              </template>
            </el-table-column>
            <el-table-column prop="locationName" label="地点" min-width="100" show-overflow-tooltip />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="viewDetail(row)">详情</el-button>
                <el-button size="small" type="warning" @click="doDispatch(row)" :disabled="row.status !== 'PUBLISHED'">智能派单</el-button>
                <el-button size="small" type="success" @click="showManualDispatch(row)" :disabled="row.status !== 'PUBLISHED' && row.status !== 'DISPATCHED'">手动派单</el-button>
                <el-dropdown trigger="click" @command="(cmd: string) => changeStatus(row, cmd)">
                  <el-button size="small" type="info">状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="IN_PROGRESS" :disabled="row.status !== 'ACCEPTED'">开始作业</el-dropdown-item>
                      <el-dropdown-item command="COMPLETED" :disabled="row.status !== 'IN_PROGRESS'">完成</el-dropdown-item>
                      <el-dropdown-item command="CANCELLED" :disabled="row.status === 'COMPLETED' || row.status === 'CANCELLED'">取消</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-if="total > pageSize" style="margin-top: 12px; justify-content: flex-end" :total="total" :page-size="pageSize" :current-page="currentPage" layout="prev, pager, next" @current-change="(p: number) => { currentPage = p; loadMissions() }" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card header="快速统计">
          <div class="stat-grid">
            <div class="stat-item clickable" @click="showStatDetail('total')">
              <div class="stat-num">{{ stats.totalMissions ?? '-' }}</div>
              <div class="stat-lbl">总任务</div>
              <div class="stat-arrow">→</div>
            </div>
            <div class="stat-item clickable" @click="showStatDetail('active')">
              <div class="stat-num" style="color: #e6a23c">{{ stats.activeMissions ?? '-' }}</div>
              <div class="stat-lbl">进行中</div>
              <div class="stat-arrow">→</div>
            </div>
            <div class="stat-item clickable" @click="showStatDetail('completed')">
              <div class="stat-num" style="color: #67c23a">{{ stats.completedMissions ?? '-' }}</div>
              <div class="stat-lbl">已完成</div>
              <div class="stat-arrow">→</div>
            </div>
            <div class="stat-item clickable" @click="showStatDetail('idlePilots')">
              <div class="stat-num" style="color: #409eff">{{ stats.idlePilots ?? '-' }}</div>
              <div class="stat-lbl">空闲飞手</div>
              <div class="stat-arrow">→</div>
            </div>
            <div class="stat-item clickable" @click="showStatDetail('idleOrgs')">
              <div class="stat-num" style="color: #409eff">{{ stats.idleOrgs ?? '-' }}</div>
              <div class="stat-lbl">空闲单位</div>
              <div class="stat-arrow">→</div>
            </div>
            <div class="stat-item clickable" @click="showStatDetail('onMissionPilots')">
              <div class="stat-num" style="color: #f56c6c">{{ stats.onMissionPilots ?? '-' }}</div>
              <div class="stat-lbl">任务中飞手</div>
              <div class="stat-arrow">→</div>
            </div>
          </div>
        </el-card>
        <el-card header="筛选" style="margin-top: 12px">
          <el-form label-width="70px" size="small">
            <el-form-item label="状态">
              <el-select v-model="filterStatus" clearable placeholder="全部" @change="loadMissions">
                <el-option label="已发布" value="PUBLISHED" /><el-option label="已派单" value="DISPATCHED" /><el-option label="已接单" value="ACCEPTED" /><el-option label="进行中" value="IN_PROGRESS" /><el-option label="已完成" value="COMPLETED" /><el-option label="已取消" value="CANCELLED" />
              </el-select>
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="filterType" clearable placeholder="全部" @change="loadMissions">
                <el-option label="森林灭火" value="FIRE_FIGHTING" /><el-option label="洪涝搜救" value="FLOOD_RESCUE" /><el-option label="事故勘察" value="ACCIDENT_SURVEY" /><el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
            <el-form-item label="紧急程度">
              <el-select v-model="filterUrgency" clearable placeholder="全部" @change="loadMissions">
                <el-option label="红色" value="RED" /><el-option label="黄色" value="YELLOW" /><el-option label="蓝色" value="BLUE" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="createVisible" title="一键发单 - 创建应急任务" width="800px" destroy-on-close>
      <el-form :model="createForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="任务标题" required><el-input v-model="createForm.title" placeholder="如：XX山区森林火灾紧急救援" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="任务类型" required><el-select v-model="createForm.missionType" style="width: 100%"><el-option label="森林灭火" value="FIRE_FIGHTING" /><el-option label="洪涝搜救" value="FLOOD_RESCUE" /><el-option label="事故勘察" value="ACCIDENT_SURVEY" /><el-option label="其他" value="OTHER" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="紧急程度" required><el-radio-group v-model="createForm.urgency"><el-radio-button value="RED"><span style="color:#fff">红色</span></el-radio-button><el-radio-button value="YELLOW">黄色</el-radio-button><el-radio-button value="BLUE">蓝色</el-radio-button></el-radio-group></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="需要无人机类型"><el-input v-model="createForm.requiredDroneType" placeholder="如：多旋翼-载重" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="需要飞手数量" required><el-input-number v-model="createForm.requiredPilotCount" :min="0" :max="20" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="需要单位数量" required><el-input-number v-model="createForm.requiredOrgCount" :min="0" :max="20" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="任务描述" required><el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="详细描述任务需求、现场情况等" /></el-form-item>
        <el-form-item label="需要资质"><el-input v-model="createForm.requiredCertTypes" placeholder="如：AOPA,UTC-SURVEY (逗号分隔)" /></el-form-item>
        <el-divider content-position="left">地图选点（点击地图选择事发位置）</el-divider>
        <el-form-item label="事发地点"><el-input v-model="createForm.locationName" placeholder="如：浙江杭州西湖山区" /></el-form-item>
        <div class="map-container">
          <div class="map-placeholder" @click="onMapClick">
            <div v-if="createForm.latitude && createForm.longitude" class="map-pin">
              <el-icon :size="32" color="#f56c6c"><Location /></el-icon>
              <div class="pin-label">{{ createForm.locationName || '选中位置' }}</div>
            </div>
            <div class="map-grid"><div v-for="r in 8" :key="'r'+r" class="map-row"><div v-for="c in 12" :key="'c'+c" class="map-cell" @click="selectCoord(r, c)"></div></div></div>
            <div class="map-labels"><span class="map-label-top">120°E ────── 115°E</span><span class="map-label-left">35°N</span><span class="map-label-bottom">25°N</span><span class="map-label-right">110°E</span></div>
            <div class="map-cities"><span class="city" style="top:25%;left:75%">●北京</span><span class="city" style="top:50%;left:82%">●上海</span><span class="city" style="top:48%;left:76%">●杭州</span><span class="city" style="top:60%;left:65%">●广州</span><span class="city" style="top:55%;left:55%">●成都</span></div>
          </div>
        </div>
        <el-row :gutter="16" style="margin-top: 12px">
          <el-col :span="12"><el-form-item label="纬度"><el-input v-model="createForm.latitude" placeholder="如：30.2741" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="经度"><el-input v-model="createForm.longitude" placeholder="如：120.1551" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="气象信息"><el-input v-model="createForm.weatherInfo" placeholder="如：风速12m/s 西北风, 能见度8km" /></el-form-item>
        <el-divider content-position="left">派单方式</el-divider>
        <el-form-item label="派单模式">
          <el-radio-group v-model="createForm.dispatchMode">
            <el-radio value="AUTO">自动派单（智能匹配后自动发送通知）</el-radio>
            <el-radio value="MANUAL">仅发布（稍后手动派单）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">发布任务</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="'任务详情 #' + detailMission?.id" width="960px" destroy-on-close>
      <template v-if="detailMission">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务标题" :span="2">{{ detailMission.title }}</el-descriptions-item>
          <el-descriptions-item label="任务类型"><el-tag :type="missionTypeTag(detailMission.missionType)">{{ missionTypeLabel(detailMission.missionType) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="紧急程度"><el-tag :type="urgencyTagType(detailMission.urgency)" effect="dark">{{ urgencyLabel(detailMission.urgency) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="missionStatusTag(detailMission.status)">{{ missionStatusLabel(detailMission.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="地点">{{ detailMission.locationName }}</el-descriptions-item>
          <el-descriptions-item label="需要飞手">{{ detailMission.requiredPilotCount ?? 1 }} 人</el-descriptions-item>
          <el-descriptions-item label="需要单位">{{ detailMission.requiredOrgCount ?? 1 }} 个</el-descriptions-item>
          <el-descriptions-item label="坐标">{{ detailMission.latitude }}, {{ detailMission.longitude }}</el-descriptions-item>
          <el-descriptions-item label="气象">{{ detailMission.weatherInfo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需要资质">{{ detailMission.requiredCertTypes || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需要机型">{{ detailMission.requiredDroneType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接单截止">{{ detailMission.confirmDeadline?.replace('T', ' ').substring(0, 16) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailMission.createdAt?.replace('T', ' ').substring(0, 16) }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detailMission.description }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">
          参与者完成状态
          <el-tag v-if="assignmentDetails.length > 0" size="small" style="margin-left:8px">
            {{ assignmentDetails.filter((a: any) => a.status === 'COMPLETED').length }}/{{ assignmentDetails.filter((a: any) => ['ACCEPTED','COMPLETED'].includes(a.status)).length }} 已完成
          </el-tag>
        </el-divider>
        <el-table :data="assignmentDetails" size="small" border v-if="assignmentDetails.length > 0">
          <el-table-column prop="rankOrder" label="顺位" width="55" />
          <el-table-column prop="assigneeType" label="类型" width="70">
            <template #default="{ row }">
              <el-tag :type="row.assigneeType === 'PILOT' ? '' : 'warning'" size="small">{{ row.assigneeType === 'PILOT' ? '飞手' : '单位' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="120">
            <template #default="{ row }">{{ row.assigneeName || '-' }}</template>
          </el-table-column>
          <el-table-column label="联系电话" width="130">
            <template #default="{ row }">{{ row.assigneePhone || '-' }}</template>
          </el-table-column>
          <el-table-column prop="matchScore" label="匹配分" width="80">
            <template #default="{ row }">
              <el-progress v-if="row.matchScore > 0" :percentage="Math.min(100, Math.round(row.matchScore))" :stroke-width="14" :text-inside="true" />
              <span v-else style="color:#909399">手动</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="assignmentStatusTag(row.status)" size="small">{{ assignmentStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="respondedAt" label="接单时间" width="140">
            <template #default="{ row }">{{ row.respondedAt?.replace('T', ' ').substring(0, 16) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="completedAt" label="完成时间" width="140">
            <template #default="{ row }">{{ row.completedAt?.replace('T', ' ').substring(0, 16) || '-' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无派单记录" :image-size="60" />

        <el-divider content-position="left">任务日志</el-divider>
        <el-table :data="logs" size="small" border v-if="logs.length > 0">
          <el-table-column prop="action" label="操作" width="120" /><el-table-column prop="commentText" label="说明" /><el-table-column prop="createdAt" label="时间" width="150"><template #default="{ row }">{{ row.createdAt?.replace('T', ' ').substring(0, 16) }}</template></el-table-column>
        </el-table>
        <el-empty v-else description="暂无日志" :image-size="60" />
      </template>
    </el-dialog>

    <el-dialog v-model="dispatchResultVisible" title="智能派单结果" width="700px" destroy-on-close>
      <el-alert type="success" :closable="false" title="智能派单完成" :description="'已派发 ' + dispatchResults.length + ' 个候选'" style="margin-bottom: 16px" />
      <el-table :data="dispatchResults" border>
        <el-table-column prop="rankOrder" label="顺位" width="55" />
        <el-table-column prop="assigneeType" label="类型" width="70"><template #default="{ row }">{{ row.assigneeType === 'PILOT' ? '飞手' : '单位' }}</template></el-table-column>
        <el-table-column label="名称" min-width="120"><template #default="{ row }">{{ parseMatchDetail(row.matchDetail, row.assigneeType) }}</template></el-table-column>
        <el-table-column prop="matchScore" label="匹配分" width="80"><template #default="{ row }"><el-progress :percentage="Math.min(100, Math.round(row.matchScore))" :stroke-width="14" :text-inside="true" /></template></el-table-column>
        <el-table-column prop="status" label="状态" width="80"><template #default="{ row }"><el-tag :type="assignmentStatusTag(row.status)" size="small">{{ assignmentStatusLabel(row.status) }}</el-tag></template></el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="manualDispatchVisible" :title="'手动派单 - ' + manualMission?.title" width="600px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="派单对象">
          <el-radio-group v-model="manualForm.assigneeType">
            <el-radio value="ORG">协作单位</el-radio>
            <el-radio value="PILOT">飞手</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="选择对象">
          <el-select v-model="manualForm.assigneeId" placeholder="请选择" filterable style="width: 100%">
            <el-option v-for="item in manualCandidates" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualDispatchVisible = false">取消</el-button>
        <el-button type="primary" @click="doManualDispatch">确认派单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statDetailVisible" :title="statDetailTitle" width="700px" destroy-on-close>
      <el-table :data="statDetailData" border stripe size="small">
        <el-table-column v-for="col in statDetailColumns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" />
      </el-table>
      <el-empty v-if="statDetailData.length === 0" description="暂无数据" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import http from '@/api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Location } from '@element-plus/icons-vue'

const loading = ref(false)
const missions = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const filterStatus = ref('')
const filterType = ref('')
const filterUrgency = ref('')
const stats = ref<any>({})

const createVisible = ref(false)
const createForm = ref<any>({ title: '', description: '', missionType: 'FIRE_FIGHTING', urgency: 'RED', latitude: null, longitude: null, locationName: '', weatherInfo: '', requiredCertTypes: '', requiredDroneType: '', dispatchMode: 'AUTO', requiredPilotCount: 1, requiredOrgCount: 1 })

const detailVisible = ref(false)
const detailMission = ref<any>(null)
const assignments = ref<any[]>([])
const assignmentDetails = ref<any[]>([])
const logs = ref<any[]>([])

const dispatchResultVisible = ref(false)
const dispatchResults = ref<any[]>([])

const manualDispatchVisible = ref(false)
const manualMission = ref<any>(null)
const manualForm = ref<any>({ assigneeType: 'ORG', assigneeId: null })
const manualCandidates = ref<any[]>([])

const statDetailVisible = ref(false)
const statDetailTitle = ref('')
const statDetailData = ref<any[]>([])
const statDetailColumns = ref<any[]>([])

function missionTypeLabel(t: string) { const m: Record<string, string> = { FIRE_FIGHTING: '森林灭火', FLOOD_RESCUE: '洪涝搜救', ACCIDENT_SURVEY: '事故勘察', OTHER: '其他' }; return m[t] || t }
function missionTypeTag(t: string) { const m: Record<string, string> = { FIRE_FIGHTING: 'danger', FLOOD_RESCUE: 'warning', ACCIDENT_SURVEY: 'info', OTHER: '' }; return m[t] || '' }
function urgencyLabel(u: string) { const m: Record<string, string> = { RED: '红色', YELLOW: '黄色', BLUE: '蓝色' }; return m[u] || u }
function urgencyTagType(u: string) { const m: Record<string, string> = { RED: 'danger', YELLOW: 'warning', BLUE: '' }; return m[u] || '' }
function missionStatusLabel(s: string) { const m: Record<string, string> = { PUBLISHED: '已发布', DISPATCHED: '已派单', ACCEPTED: '已接单', IN_PROGRESS: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }; return m[s] || s }
function missionStatusTag(s: string) { const m: Record<string, string> = { PUBLISHED: 'info', DISPATCHED: 'warning', ACCEPTED: '', IN_PROGRESS: 'success', COMPLETED: 'success', CANCELLED: 'info' }; return m[s] || '' }
function assignmentStatusLabel(s: string) { const m: Record<string, string> = { PENDING: '待响应', ACCEPTED: '已接单', REJECTED: '已拒绝', TIMEOUT: '已超时', COMPLETED: '已完成' }; return m[s] || s }
function assignmentStatusTag(s: string) { const m: Record<string, string> = { PENDING: 'warning', ACCEPTED: '', REJECTED: 'danger', TIMEOUT: 'info', COMPLETED: 'success' }; return m[s] || '' }
function parseMatchDetail(detail: string, type: string) { if (!detail) return '-'; try { const d = JSON.parse(detail); return type === 'PILOT' ? (d.pilotName || d.name || d.pilotId) : (d.orgName || d.name || d.orgId) } catch { return '-' } }

async function loadMissions() {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, size: pageSize }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterType.value) params.missionType = filterType.value
    if (filterUrgency.value) params.urgency = filterUrgency.value
    const res = await http.get('/api/missions', { params })
    missions.value = res?.records || []
    total.value = res?.total || 0
  } finally { loading.value = false }
}

async function loadStats() {
  try { const res = await http.get('/api/missions/dashboard'); stats.value = res || {} } catch { /* ignore */ }
}

function showCreateDialog() {
  createForm.value = { title: '', description: '', missionType: 'FIRE_FIGHTING', urgency: 'RED', latitude: null, longitude: null, locationName: '', weatherInfo: '', requiredCertTypes: '', requiredDroneType: '', dispatchMode: 'AUTO', requiredPilotCount: 1, requiredOrgCount: 1 }
  createVisible.value = true
}

function selectCoord(row: number, col: number) {
  createForm.value.latitude = Math.round((35 - (row - 1) * (10 / 7)) * 10000) / 10000
  createForm.value.longitude = Math.round((120 - (col - 1) * (10 / 11)) * 10000) / 10000
}

function onMapClick() {}

async function submitCreate() {
  if (!createForm.value.title || !createForm.value.missionType) { ElMessage.warning('请填写任务标题和类型'); return }
  if ((createForm.value.requiredPilotCount || 0) + (createForm.value.requiredOrgCount || 0) < 1) {
    ElMessage.warning('飞手和单位数量至少需要1'); return
  }
  try {
    const mission = await http.post('/api/missions', createForm.value)
    ElMessage.success('任务发布成功')
    createVisible.value = false
    await loadMissions()
    await loadStats()
    if (createForm.value.dispatchMode === 'AUTO' && mission?.id) {
      try {
        const res = await http.post(`/api/missions/${mission.id}/dispatch`)
        dispatchResults.value = res || []
        dispatchResultVisible.value = true
        ElMessage.success('已自动派单并通知飞手/单位')
        await loadMissions()
        await loadStats()
      } catch (e: any) { ElMessage.warning('任务已发布，但自动派单失败：' + (e.response?.data?.message || '未知错误')) }
    }
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '发布失败') }
}

async function viewDetail(row: any) {
  detailMission.value = row
  try {
    const [aRes, lRes, adRes] = await Promise.all([
      http.get(`/api/missions/${row.id}/assignments`),
      http.get(`/api/missions/${row.id}/logs`),
      http.get(`/api/missions/${row.id}/assignment-detail`)
    ])
    assignments.value = aRes || []
    assignmentDetails.value = adRes || []
    logs.value = lRes || []
  } catch { /* ignore */ }
  detailVisible.value = true
}

async function doDispatch(row: any) {
  try {
    await ElMessageBox.confirm(`确认对任务"${row.title}"执行智能派单？系统将自动匹配候选飞手和单位并发送通知。`, '智能派单确认', { type: 'warning' })
    const res = await http.post(`/api/missions/${row.id}/dispatch`)
    dispatchResults.value = res || []
    dispatchResultVisible.value = true
    await loadMissions()
    await loadStats()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '派单失败') }
}

async function showManualDispatch(row: any) {
  manualMission.value = row
  manualForm.value = { assigneeType: 'ORG', assigneeId: null }
  await loadManualCandidates()
  manualDispatchVisible.value = true
}

async function loadManualCandidates() {
  try {
    const type = manualForm.value.assigneeType
    if (type === 'PILOT') {
      const res = await http.get('/api/public/pilots', { params: { page: 1, size: 100 } })
      manualCandidates.value = (res?.records || []).map((p: any) => ({ id: p.id, name: p.fullName + (p.region ? ' (' + p.region + ')' : '') }))
    } else {
      const res = await http.get('/api/public/organizations', { params: { page: 1, size: 100 } })
      manualCandidates.value = (res?.records || []).map((o: any) => ({ id: o.id, name: o.name + (o.orgType ? ' (' + o.orgType + ')' : '') }))
    }
  } catch { manualCandidates.value = [] }
}

async function doManualDispatch() {
  if (!manualForm.value.assigneeId) { ElMessage.warning('请选择派单对象'); return }
  try {
    await http.post(`/api/missions/${manualMission.value.id}/manual-dispatch`, manualForm.value)
    ElMessage.success('手动派单成功，已通知对方')
    manualDispatchVisible.value = false
    await loadMissions()
    await loadStats()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '派单失败') }
}

async function changeStatus(row: any, newStatus: string) {
  try {
    await http.post(`/api/missions/${row.id}/status`, { status: newStatus, comment: `管理员变更状态为${missionStatusLabel(newStatus)}` })
    ElMessage.success('状态已更新')
    await loadMissions()
    await loadStats()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '操作失败') }
}

async function showStatDetail(type: string) {
  statDetailData.value = []
  statDetailColumns.value = []
  try {
    if (type === 'idlePilots') {
      statDetailTitle.value = '空闲飞手列表'
      statDetailColumns.value = [{ prop: 'pilotName', label: '姓名', width: 100 }, { prop: 'availability', label: '状态', width: 80 }, { prop: 'droneType', label: '无人机类型', width: 120 }, { prop: 'missionCount', label: '完成任务', width: 80 }]
      const res = await http.get('/api/missions/dashboard')
      statDetailData.value = (res?.pilotStatuses || []).filter((p: any) => p.availability === 'IDLE')
    } else if (type === 'idleOrgs') {
      statDetailTitle.value = '空闲单位列表'
      statDetailColumns.value = [{ prop: 'orgName', label: '单位名称', width: 140 }, { prop: 'availability', label: '状态', width: 80 }, { prop: 'missionCount', label: '完成任务', width: 80 }]
      const res = await http.get('/api/missions/dashboard')
      statDetailData.value = (res?.orgStatuses || []).filter((o: any) => o.availability === 'IDLE')
    } else if (type === 'onMissionPilots') {
      statDetailTitle.value = '任务中飞手列表'
      statDetailColumns.value = [{ prop: 'pilotName', label: '姓名', width: 100 }, { prop: 'availability', label: '状态', width: 80 }, { prop: 'droneType', label: '无人机类型', width: 120 }, { prop: 'activeMissionId', label: '当前任务ID', width: 100 }]
      const res = await http.get('/api/missions/dashboard')
      statDetailData.value = (res?.pilotStatuses || []).filter((p: any) => p.availability === 'ON_MISSION')
    } else if (type === 'active') {
      statDetailTitle.value = '进行中的任务'
      statDetailColumns.value = [{ prop: 'title', label: '任务标题' }, { prop: 'urgency', label: '紧急程度', width: 90 }, { prop: 'locationName', label: '地点', width: 140 }]
      const res = await http.get('/api/missions', { params: { status: 'IN_PROGRESS', page: 1, size: 50 } })
      statDetailData.value = res?.records || []
    } else if (type === 'completed') {
      statDetailTitle.value = '已完成的任务'
      statDetailColumns.value = [{ prop: 'title', label: '任务标题' }, { prop: 'urgency', label: '紧急程度', width: 90 }, { prop: 'locationName', label: '地点', width: 140 }]
      const res = await http.get('/api/missions', { params: { status: 'COMPLETED', page: 1, size: 50 } })
      statDetailData.value = res?.records || []
    } else {
      statDetailTitle.value = '全部任务'
      statDetailColumns.value = [{ prop: 'title', label: '任务标题' }, { prop: 'status', label: '状态', width: 90 }, { prop: 'urgency', label: '紧急程度', width: 90 }]
      const res = await http.get('/api/missions', { params: { page: 1, size: 50 } })
      statDetailData.value = res?.records || []
    }
  } catch { /* ignore */ }
  statDetailVisible.value = true
}

onMounted(() => { loadMissions(); loadStats() })

watch(() => manualForm.value.assigneeType, () => {
  manualForm.value.assigneeId = null
  loadManualCandidates()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.stat-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
.stat-item { text-align: center; padding: 10px; background: #f5f7fa; border-radius: 8px; position: relative; }
.stat-item.clickable { cursor: pointer; transition: all 0.2s; }
.stat-item.clickable:hover { background: #ecf5ff; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.stat-num { font-size: 24px; font-weight: 700; }
.stat-lbl { color: #909399; font-size: 12px; margin-top: 4px; }
.stat-arrow { position: absolute; top: 6px; right: 8px; font-size: 12px; color: #c0c4cc; opacity: 0; transition: opacity 0.2s; }
.stat-item.clickable:hover .stat-arrow { opacity: 1; color: #409eff; }
.map-container { border: 1px solid #dcdfe6; border-radius: 6px; overflow: hidden; }
.map-placeholder { height: 300px; background: linear-gradient(135deg, #e8f4f8 0%, #d4e8f0 50%, #c0dce8 100%); position: relative; cursor: crosshair; }
.map-grid { position: absolute; inset: 0; display: flex; flex-direction: column; }
.map-row { flex: 1; display: flex; }
.map-cell { flex: 1; border: 1px solid rgba(100, 150, 180, 0.2); transition: background 0.15s; }
.map-cell:hover { background: rgba(64, 158, 255, 0.25); }
.map-labels { position: absolute; inset: 0; pointer-events: none; font-size: 11px; color: #606266; }
.map-label-top { position: absolute; top: 4px; left: 50%; transform: translateX(-50%); }
.map-label-left { position: absolute; left: 4px; top: 50%; transform: translateY(-50%); }
.map-label-bottom { position: absolute; bottom: 4px; left: 50%; transform: translateX(-50%); }
.map-label-right { position: absolute; right: 4px; top: 50%; transform: translateY(-50%); }
.map-cities { position: absolute; inset: 0; pointer-events: none; }
.city { position: absolute; font-size: 11px; color: #303133; font-weight: 600; }
.map-pin { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -100%); text-align: center; z-index: 10; pointer-events: none; }
.pin-label { background: rgba(245, 108, 108, 0.9); color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 12px; white-space: nowrap; margin-top: 2px; }
</style>
