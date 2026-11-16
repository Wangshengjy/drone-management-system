<template>
  <el-row :gutter="16">
    <el-col :span="6" v-for="c in cards" :key="c.k">
      <el-card shadow="hover">
        <div class="num">{{ c.v }}</div>
        <div class="lbl">{{ c.label }}</div>
      </el-card>
    </el-col>
  </el-row>

  <el-row :gutter="16" style="margin-top: 16px" v-loading="loading">
    <el-col :span="12">
      <el-card header="通过率">
        <div class="rate-row">
          <div class="rate-label">飞手通过率</div>
          <el-progress :percentage="pilotApprovalRate" />
        </div>
        <div class="rate-row">
          <div class="rate-label">单位通过率</div>
          <el-progress :percentage="orgApprovalRate" status="success" />
        </div>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card header="审核状态概览">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="待审飞手">{{ reviewCounts.pending_pilots ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="已通过飞手">{{ reviewCounts.approved_pilots ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="待审单位">{{ reviewCounts.pending_orgs ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="已通过单位">{{ reviewCounts.approved_orgs ?? 0 }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </el-col>
  </el-row>

  <el-row :gutter="16" style="margin-top: 16px" v-loading="loading">
    <el-col :span="12">
      <el-card header="飞手资质分布">
        <div v-if="pilotCertDistribution.length === 0" class="empty">暂无数据</div>
        <div v-else v-for="(item, idx) in pilotCertDistribution" :key="idx" class="dist-row">
          <div class="dist-title">{{ item.name }}</div>
          <el-progress :percentage="calcPct(item.value, pilotCertTotal)" :show-text="false" />
          <div class="dist-count">{{ item.value }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card header="单位类型分布">
        <div v-if="orgTypeDistribution.length === 0" class="empty">暂无数据</div>
        <div v-else v-for="(item, idx) in orgTypeDistribution" :key="idx" class="dist-row">
          <div class="dist-title">{{ orgTypeLabel(item.name) }}</div>
          <el-progress :percentage="calcPct(item.value, orgTypeTotal)" :show-text="false" status="success" />
          <div class="dist-count">{{ item.value }}</div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import http from '@/api/http'

const loading = ref(false)
const dash = ref<any>(null)

const cards = computed(() => {
  const r = dash.value?.reviewCounts || {}
  return [
    { k: 'ap', label: '已通过飞手', v: r.approved_pilots ?? '-' },
    { k: 'pp', label: '待审飞手', v: r.pending_pilots ?? '-' },
    { k: 'ao', label: '已通过单位', v: r.approved_orgs ?? '-' },
    { k: 'po', label: '待审单位', v: r.pending_orgs ?? '-' },
  ]
})

const pilotApprovalRate = computed(() => Number(dash.value?.pilotApprovalRate ?? 0))
const orgApprovalRate = computed(() => Number(dash.value?.orgApprovalRate ?? 0))
const reviewCounts = computed(() => dash.value?.reviewCounts || {})
const pilotCertDistribution = computed(() => dash.value?.pilotCertDistribution || [])
const orgTypeDistribution = computed(() => dash.value?.orgTypeDistribution || [])
const pilotCertTotal = computed(() =>
  pilotCertDistribution.value.reduce((sum: number, item: any) => sum + Number(item?.value || 0), 0),
)
const orgTypeTotal = computed(() =>
  orgTypeDistribution.value.reduce((sum: number, item: any) => sum + Number(item?.value || 0), 0),
)

function calcPct(value: number, total: number) {
  if (!total) return 0
  return Math.round((Number(value || 0) / total) * 100)
}

function orgTypeLabel(type: string) {
  const map: Record<string, string> = {
    ENTERPRISE: '企业',
    VOLUNTEER: '志愿者机构',
    OTHER: '其他',
  }
  return map[type] || type
}

onMounted(async () => {
  loading.value = true
  try {
    dash.value = await http.get('/api/public/stats/dashboard')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.num {
  font-size: 28px;
  font-weight: 700;
}
.lbl {
  color: #909399;
  margin-top: 4px;
}
.rate-row + .rate-row {
  margin-top: 12px;
}
.rate-label {
  margin-bottom: 6px;
  color: #606266;
}
.dist-row {
  display: grid;
  grid-template-columns: 1fr 2fr 36px;
  gap: 10px;
  align-items: center;
}
.dist-row + .dist-row {
  margin-top: 10px;
}
.dist-title {
  color: #606266;
}
.dist-count {
  text-align: right;
  color: #303133;
}
.empty {
  color: #909399;
}
</style>
