<template>
  <div class="login-page">
    <div class="login-panel">
      <section class="login-intro">
        <div class="intro-brand">
          <div class="intro-mark">z</div>
          <div class="intro-brand-text">
            <strong>zSearch</strong>
            <span>舆情工作台</span>
          </div>
        </div>

        <div class="intro-copy">
          <h1>全域舆情，一屏掌握</h1>
          <p>汇聚抖音、小红书、bilibili、微博、今日头条等平台内容，支持关键词监测、信息源定向监测与风险研判。</p>
        </div>

        <ul class="intro-points">
          <li><span class="point-dot"></span>多平台内容统一检索与统计</li>
          <li><span class="point-dot"></span>关键词方案与定向来源监测</li>
          <li><span class="point-dot"></span>情感、风险与热词多维分析</li>
        </ul>

        <div class="intro-foot">zSearch · 让每一次洞察都有据可依</div>
      </section>

      <section class="login-form-wrap">
        <div class="form-head">
          <h2>登录工作台</h2>
          <p>使用已分配的账号进入舆情监测系统</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @submit.prevent="submit"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="用户名"
              autocomplete="username"
              :prefix-icon="User"
              autofocus
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              placeholder="密码"
              autocomplete="current-password"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button
            class="submit-btn"
            type="primary"
            size="large"
            native-type="submit"
            :loading="loading"
          >
            登录
          </el-button>
        </el-form>

        <div class="form-foot">
          <span class="foot-dot"></span>
          登录后默认拥有系统全部功能权限
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { login } from '../api'

const emit = defineEmits(['success'])

const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  if (loading.value) return
  try {
    await formRef.value?.validate()
  } catch (error) {
    return
  }

  loading.value = true
  try {
    const data = await login({
      username: form.username.trim(),
      password: form.password
    })
    ElMessage.success(`欢迎回来，${data.user?.nickname || data.user?.username || ''}`)
    emit('success', data)
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 32px;
  overflow: auto;
  background:
    radial-gradient(760px 460px at 12% 8%, rgba(37, 99, 235, 0.14), transparent 62%),
    radial-gradient(680px 420px at 92% 92%, rgba(96, 165, 250, 0.16), transparent 60%),
    linear-gradient(150deg, #eef4ff 0%, #f7faff 48%, #eef3fb 100%);
}

.login-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(380px, 0.95fr);
  width: min(980px, 100%);
  min-height: 560px;
  overflow: hidden;
  background: #fff;
  border: 1px solid rgba(214, 224, 238, 0.8);
  border-radius: 20px;
  box-shadow: 0 30px 80px rgba(23, 53, 105, 0.16);
}

.login-intro {
  display: flex;
  flex-direction: column;
  padding: 42px 46px 34px;
  color: #fff;
  background:
    radial-gradient(420px 260px at 82% 12%, rgba(255, 255, 255, 0.18), transparent 60%),
    linear-gradient(150deg, #1d4ed8 0%, #2563eb 46%, #3b82f6 100%);
}

.intro-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.intro-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  color: #1d4ed8;
  font-family: Georgia, serif;
  font-size: 28px;
  font-style: italic;
  font-weight: 700;
  background: #fff;
  border-radius: 13px;
  box-shadow: 0 10px 24px rgba(12, 37, 96, 0.28);
}

.intro-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.intro-brand-text strong {
  font-size: 19px;
  letter-spacing: 0.5px;
}

.intro-brand-text span {
  margin-top: 3px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.74);
  letter-spacing: 2.4px;
}

.intro-copy {
  margin-top: 58px;
}

.intro-copy h1 {
  margin: 0;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: 0.5px;
}

.intro-copy p {
  max-width: 360px;
  margin: 18px 0 0;
  font-size: 13px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.82);
}

.intro-points {
  margin: 34px 0 0;
  padding: 0;
  list-style: none;
}

.intro-points li {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
}

.point-dot {
  width: 7px;
  height: 7px;
  background: #bfdbfe;
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(191, 219, 254, 0.18);
}

.intro-foot {
  margin-top: auto;
  padding-top: 26px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 1.2px;
}

.login-form-wrap {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 52px 58px;
  background: #fff;
}

.form-head h2 {
  margin: 0;
  font-size: 25px;
  font-weight: 700;
  color: var(--text);
}

.form-head p {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--text-muted);
}

.login-form {
  margin-top: 34px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 2px 14px;
  border-radius: 10px;
}

.login-form :deep(.el-input__inner) {
  height: 42px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  font-size: 15px;
  letter-spacing: 2px;
  border-radius: 10px;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.24);
}

.form-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 26px;
  font-size: 12px;
  color: var(--text-muted);
}

.foot-dot {
  width: 6px;
  height: 6px;
  background: var(--success);
  border-radius: 50%;
  animation: pulse-dot 2.2s ease-in-out infinite;
}

@media (max-width: 900px) {
  .login-panel {
    grid-template-columns: 1fr;
    min-height: 0;
    width: min(440px, 100%);
  }

  .login-intro {
    display: none;
  }

  .login-form-wrap {
    padding: 40px 32px;
  }
}
</style>