<template>
  <div class="wrap">
    <el-card class="card">
      <template #header>
        <div class="card-head">
          <span>线上报名 / 注册</span>
          <span class="sub">提交后会自动进入预审，符合条件的报名会直接出现在管理员审核队列中。</span>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="tip"
        title="飞手需填写手机号、备案号；单位需填写手机号、统一社会信用代码。"
      />

      <el-form :model="form" label-width="110px" @submit.prevent="onSubmit">
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio label="PILOT">飞手</el-radio>
            <el-radio label="ORG">协作单位</el-radio>
            <el-radio label="GUEST">访客（仅公开查询）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.role !== 'GUEST'" :label="form.role === 'PILOT' ? '姓名' : '单位名称'">
          <el-input v-model="form.displayName" :placeholder="form.role === 'PILOT' ? '如：张三' : '如：XX无人机救援有限公司'" />
        </el-form-item>
        <el-form-item v-else label="显示名称">
          <el-input v-model="form.displayName" placeholder="选填" clearable />
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" placeholder="如：zhangsan 或 org_example" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="new-password" show-password placeholder="如：123456" />
        </el-form-item>

        <template v-if="form.role === 'PILOT'">
          <el-divider content-position="left">飞手资料</el-divider>
          <el-form-item label="手机号" required>
            <el-input v-model="form.phone" placeholder="如：13800010001" />
          </el-form-item>
          <el-form-item label="备案号" required>
            <el-input v-model="form.recordNo" placeholder="如：UAS-SAMPLE-ALPHA" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="如：alpha@example.com" />
          </el-form-item>
          <el-form-item label="身份证">
            <el-input v-model="form.idCard" placeholder="如：110101199001010011" />
          </el-form-item>
          <el-form-item label="地区">
            <el-input v-model="form.region" placeholder="如：Shanghai" />
          </el-form-item>
        </template>

        <template v-if="form.role === 'ORG'">
          <el-divider content-position="left">单位资料</el-divider>
          <el-form-item label="手机号" required>
            <el-input v-model="form.phone" placeholder="如：02100010001" />
          </el-form-item>
          <el-form-item label="统一信用代码" required>
            <el-input v-model="form.creditCode" placeholder="如：91310000SAMPLE0001" />
          </el-form-item>
          <el-form-item label="单位类型">
            <el-select v-model="form.orgType" style="width: 100%">
              <el-option label="企业" value="ENTERPRISE" />
              <el-option label="志愿者机构" value="VOLUNTEER" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="form.contactName" placeholder="如：李四" />
          </el-form-item>
          <el-form-item label="地址">
            <el-input v-model="form.address" type="textarea" :rows="2" placeholder="如：上海市浦东新区XX路100号" />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading">
            {{ form.role === 'GUEST' ? '注册并登录' : '提交报名并登录' }}
          </el-button>
          <el-button link @click="$router.push('/login')">已有账号</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  role: 'PILOT',
  displayName: '',
  phone: '',
  email: '',
  idCard: '',
  recordNo: '',
  region: '',
  orgType: 'ENTERPRISE',
  creditCode: '',
  contactName: '',
  address: '',
})

async function onSubmit() {
  if (form.role !== 'GUEST' && !String(form.displayName || '').trim()) {
    ElMessage.warning('请填写姓名或单位名称')
    return
  }
  if (form.role === 'PILOT' && (!String(form.phone || '').trim() || !String(form.recordNo || '').trim())) {
    ElMessage.warning('飞手报名请填写手机号和备案号')
    return
  }
  if (form.role === 'ORG' && (!String(form.phone || '').trim() || !String(form.creditCode || '').trim())) {
    ElMessage.warning('单位报名请填写手机号和统一社会信用代码')
    return
  }

  loading.value = true
  try {
    const payload = {
      username: form.username.trim(),
      password: form.password,
      role: form.role,
      displayName: form.displayName.trim(),
      phone: form.phone.trim(),
      email: form.email.trim(),
      idCard: form.idCard.trim(),
      recordNo: form.recordNo.trim(),
      region: form.region.trim(),
      orgType: form.orgType,
      creditCode: form.creditCode.trim(),
      contactName: form.contactName.trim(),
      address: form.address.trim(),
    }
    const data = await http.post('/api/auth/register', payload)
    auth.setSession(data)

    if (form.role === 'PILOT' || form.role === 'ORG') {
      ElMessage.success('报名已提交，管理员审核队列将收到你的资料')
    } else {
      ElMessage.success('注册成功')
    }

    if (form.role === 'PILOT') {
      router.replace('/pilot')
    } else if (form.role === 'ORG') {
      router.replace('/org')
    } else {
      router.replace('/home')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 16px;
}

.card {
  width: 640px;
  max-width: 100%;
}

.card-head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sub {
  font-size: 12px;
  font-weight: normal;
  color: #909399;
  line-height: 1.5;
}

.tip {
  margin-bottom: 18px;
}
</style>
