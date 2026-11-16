<template>
  <el-card v-loading="loading" header="预审配置">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <el-card class="inner-card" header="阈值与关键词" shadow="never">
          <el-form label-width="150px" class="cfg-form">
            <el-form-item label="通过阈值">
              <el-input-number v-model="form.passThreshold" :min="0" :max="100" />
            </el-form-item>
            <el-form-item label="人工复核阈值">
              <el-input-number v-model="form.manualThreshold" :min="0" :max="100" />
            </el-form-item>
            <el-form-item label="软拒绝阈值">
              <el-input-number v-model="form.softRejectBelow" :min="0" :max="100" />
            </el-form-item>
            <el-form-item label="黑名单关键词">
              <el-input
                v-model="blacklistText"
                type="textarea"
                :rows="3"
                placeholder="例如：黑名单, 非法, 违规"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="14">
        <el-card class="inner-card" header="权重配置" shadow="never">
          <el-form label-width="130px" class="cfg-form">
            <el-form-item label="基础字段">
              <el-slider v-model="form.weights.basicFields" :min="0" :max="1" :step="0.01" show-input />
            </el-form-item>
            <el-form-item label="证书">
              <el-slider v-model="form.weights.certificates" :min="0" :max="1" :step="0.01" show-input />
            </el-form-item>
            <el-form-item label="备案号">
              <el-slider v-model="form.weights.recordNo" :min="0" :max="1" :step="0.01" show-input />
            </el-form-item>
            <el-form-item label="地区">
              <el-slider v-model="form.weights.region" :min="0" :max="1" :step="0.01" show-input />
            </el-form-item>
            <el-form-item label="信用代码">
              <el-slider v-model="form.weights.creditCode" :min="0" :max="1" :step="0.01" show-input />
            </el-form-item>
          </el-form>

          <el-alert
            :type="Math.abs(weightSum - 1) < 0.001 ? 'success' : 'warning'"
            :closable="false"
            show-icon
            class="weight-tip"
            :title="`当前权重总和：${weightSum.toFixed(2)}（建议为 1.00）`"
          />
        </el-card>
      </el-col>
    </el-row>

    <div class="actions">
      <el-button type="primary" @click="save">保存配置</el-button>
      <el-button @click="load">重新加载</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import http from '@/api/http'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const form = ref({
  passThreshold: 70,
  manualThreshold: 45,
  softRejectBelow: 30,
  weights: {
    basicFields: 0.25,
    certificates: 0.35,
    recordNo: 0.2,
    region: 0.1,
    creditCode: 0.1,
  },
})
const blacklistText = ref('')
const extra = ref<Record<string, any>>({})
const weightSum = computed(() => {
  const w = form.value.weights
  return Number(w.basicFields) + Number(w.certificates) + Number(w.recordNo) + Number(w.region) + Number(w.creditCode)
})

async function load() {
  loading.value = true
  try {
    const data = await http.get('/api/admin/review-config')
    const weights = data?.weights || {}
    form.value = {
      passThreshold: Number(data?.passThreshold ?? 70),
      manualThreshold: Number(data?.manualThreshold ?? 45),
      softRejectBelow: Number(data?.softRejectBelow ?? 30),
      weights: {
        basicFields: Number(weights?.basicFields ?? 0.25),
        certificates: Number(weights?.certificates ?? 0.35),
        recordNo: Number(weights?.recordNo ?? 0.2),
        region: Number(weights?.region ?? 0.1),
        creditCode: Number(weights?.creditCode ?? 0.1),
      },
    }
    blacklistText.value = Array.isArray(data?.blacklistKeywords)
      ? data.blacklistKeywords.join(', ')
      : ''

    const {
      passThreshold,
      manualThreshold,
      softRejectBelow,
      blacklistKeywords,
      weights: loadedWeights,
      ...rest
    } = data || {}
    const { basicFields, certificates, recordNo, region, creditCode, ...weightRest } = loadedWeights || {}
    extra.value = { ...rest, weights: weightRest }
  } finally {
    loading.value = false
  }
}

async function save() {
  const keywords = blacklistText.value
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  const obj = {
    ...extra.value,
    passThreshold: form.value.passThreshold,
    manualThreshold: form.value.manualThreshold,
    softRejectBelow: form.value.softRejectBelow,
    blacklistKeywords: keywords,
    weights: {
      ...(extra.value.weights || {}),
      ...form.value.weights,
    },
  }
  await http.put('/api/admin/review-config', obj)
  ElMessage.success('已保存')
}

onMounted(load)
</script>

<style scoped>
.inner-card {
  height: 100%;
}

.cfg-form :deep(.el-input-number) {
  width: 180px;
}

.weight-tip {
  margin-top: 8px;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
