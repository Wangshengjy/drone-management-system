<template>
  <el-card v-loading="loading" header="操作日志">
    <el-table :data="rows" size="small">
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column prop="entityType" label="实体" width="80" />
      <el-table-column prop="entityId" label="ID" width="80" />
      <el-table-column prop="action" label="动作" />
      <el-table-column prop="commentText" label="备注" />
    </el-table>
    <el-pagination
      style="margin-top: 12px"
      v-model:current-page="page"
      layout="prev, pager, next"
      :total="total"
      :page-size="20"
      @current-change="load"
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)

async function load() {
  loading.value = true
  try {
    const data = await http.get('/api/admin/audit-logs', { params: { page: page.value, size: 20 } })
    rows.value = data.records || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
