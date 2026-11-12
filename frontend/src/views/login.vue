<template>
  <div class="sp-root">
    <!-- ═══ LOGIN GATE（视觉沿用 legacy/students-interface/login.html）═══ -->
    <div
      id="login-gate"
      class="login-gate"
      :class="{ 'is-leaving': leaving }"
      role="dialog"
      aria-modal="true"
      aria-labelledby="auth-title"
    >
      <div class="bg-shapes" aria-hidden="true">
        <span class="shape s1"></span>
        <span class="shape s2"></span>
        <span class="shape s3"></span>
      </div>
      <div class="grid-overlay" aria-hidden="true"></div>

      <div class="auth-card">
        <div class="auth-brand">
          <img class="auth-logo" :src="logoUrl" alt="CUHK-Shenzhen logo" />
          <div>
            <div class="auth-eyebrow">CUHK-Shenzhen · Academic Advisory</div>
            <h1 class="auth-title" id="auth-title">Undergraduate Study Dashboard</h1>
            <p class="auth-subtitle">
              课程规划、培养方案与技能画像的一体化平台。个人画像仅保存在本机浏览器。
            </p>
          </div>
        </div>

        <!-- 角色切换：保留原设计，同时用于预填演示账号 -->
        <div
          class="role-toggle"
          :class="{ advisor: role === 'advisor' }"
          role="tablist"
          aria-label="Select role"
        >
          <button
            type="button"
            class="role-btn"
            :class="{ active: role === 'student' }"
            role="tab"
            :aria-selected="role === 'student'"
            @click="pickRole('student')"
          >
            🎓 我是学生
          </button>
          <button
            type="button"
            class="role-btn"
            :class="{ active: role === 'advisor' }"
            role="tab"
            :aria-selected="role === 'advisor'"
            @click="pickRole('advisor')"
          >
            👔 学业导师
          </button>
          <span class="role-indicator" aria-hidden="true"></span>
        </div>

        <form id="auth-form" class="auth-form" novalidate @submit.prevent="handleLogin">
          <label class="auth-field">
            <span>账号 / Username</span>
            <input
              v-model.trim="loginForm.username"
              type="text"
              name="username"
              autocomplete="username"
              :placeholder="role === 'advisor' ? '如 teacher_li' : '如 stu_chen'"
              required
            />
          </label>

          <label class="auth-field">
            <span>密码 / Password</span>
            <input
              v-model="loginForm.password"
              type="password"
              name="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              required
            />
          </label>

          <label v-if="captchaEnabled" class="auth-field">
            <span>验证码 / Captcha</span>
            <div class="captcha-row">
              <input
                v-model.trim="loginForm.code"
                type="text"
                name="code"
                autocomplete="off"
                placeholder="填写计算结果"
                required
              />
              <img
                v-if="codeUrl"
                class="captcha-img"
                :src="codeUrl"
                alt="captcha"
                title="点击刷新验证码"
                @click="getCode"
              />
            </div>
            <small class="field-hint" :class="{ error: Boolean(errorMessage) }">
              {{ errorMessage || hintText }}
            </small>
          </label>

          <label class="remember-row">
            <input id="rememberMe" v-model="loginForm.rememberMe" type="checkbox" />
            <span>记住账号（仅本机 Cookie）</span>
          </label>

          <button type="submit" class="auth-submit" :class="{ loading }" id="auth-submit" :disabled="loading">
            <span class="label">{{ loading ? '正在登录' : '登录 / Sign in' }}</span>
            <span class="arrow" aria-hidden="true">→</span>
          </button>
        </form>

        <div class="auth-meta">
          <span>🔐 平台账号鉴权</span>
          <span class="dot">·</span>
          <span>📊 学业数据按身份授权</span>
          <span class="dot">·</span>
          <span>⚡ 个人画像仅存本机</span>
        </div>
      </div>
    </div>

    <!-- ═══ PRIVACY BAR ═══ -->
    <footer class="privacy-bar" role="contentinfo">
      <div class="pc">
        <span class="lock" aria-hidden="true">🔒</span>
        <span class="pt">
          <strong>隐私说明：</strong>
          学业数据由平台后端按当前登录身份鉴权后提供；个人技能画像与备注只保存在浏览器本地（IndexedDB），不会上传。
        </span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts" name="Login">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { LocationQueryRaw } from 'vue-router'
import Cookies from 'js-cookie'
import { getCodeImg } from '@/api/login'
import { decrypt, encrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'
import type { CaptchaInfoResult, LoginForm } from '@/types/api/login'
import logoUrl from '@/assets/student/Image_20260513001106_704_33.png'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const role = ref<'student' | 'advisor'>('student')
const leaving = ref(false)
const loading = ref(false)
const captchaEnabled = ref(true)
const codeUrl = ref('')
const errorMessage = ref('')
const redirect = ref<string | undefined>(undefined)

const loginForm = reactive<LoginForm>({
  username: 'stu_chen',
  password: 'admin123',
  rememberMe: false,
  code: '',
  uuid: ''
})

const hintText = computed(() =>
  role.value === 'advisor'
    ? '演示导师账号：teacher_li / admin123（导师端页面暂未上线，登录后可见教育平台）'
    : '演示学生账号：stu_chen / admin123，另有 stu_wang、stu_liu 等 8 个演示学生'
)

watch(
  route,
  (newRoute) => {
    redirect.value = (newRoute.query && newRoute.query.redirect) as string | undefined
  },
  { immediate: true }
)

function pickRole(next: 'student' | 'advisor'): void {
  role.value = next
  errorMessage.value = ''
  // 仅预填演示口令，真实角色由后端授予，前端切换不改变权限
  loginForm.username = next === 'advisor' ? 'teacher_li' : 'stu_chen'
  loginForm.password = 'admin123'
}

function validate(): boolean {
  if (!loginForm.username) {
    errorMessage.value = '请输入账号'
    return false
  }
  if (!loginForm.password) {
    errorMessage.value = '请输入密码'
    return false
  }
  if (captchaEnabled.value && !loginForm.code) {
    errorMessage.value = '请输入验证码'
    return false
  }
  errorMessage.value = ''
  return true
}

async function handleLogin(): Promise<void> {
  if (!validate()) {
    return
  }
  loading.value = true
  try {
    if (loginForm.rememberMe) {
      Cookies.set('username', loginForm.username, { expires: 30 })
      Cookies.set('password', encrypt(loginForm.password), { expires: 30 })
      Cookies.set('rememberMe', String(loginForm.rememberMe), { expires: 30 })
    } else {
      Cookies.remove('username')
      Cookies.remove('password')
      Cookies.remove('rememberMe')
    }
    await userStore.login(loginForm)
    // 角色落地页由 router.beforeEach 统一处理（学生 -> /student），这里只做过渡
    leaving.value = true
    const query = route.query
    const otherQueryParams: LocationQueryRaw = {}
    Object.keys(query).forEach((key) => {
      if (key !== 'redirect') {
        otherQueryParams[key] = query[key]
      }
    })
    await router.push({ path: redirect.value || '/', query: otherQueryParams })
  } catch {
    loading.value = false
    leaving.value = false
    if (captchaEnabled.value) {
      getCode()
      loginForm.code = ''
    }
  }
}

function getCode(): void {
  getCodeImg().then((res: CaptchaInfoResult) => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      loginForm.uuid = res.uuid
    }
  })
}

function getCookie(): void {
  const username = Cookies.get('username')
  const password = Cookies.get('password')
  const rememberMe = Cookies.get('rememberMe')
  if (username === undefined && password === undefined) {
    return
  }
  loginForm.username = username === undefined ? loginForm.username : username
  if (password !== undefined) {
    // decrypt 失败会返回 false（密钥不匹配 / Cookie 被篡改），此时保留原密码不做覆盖
    const decrypted = decrypt(password)
    loginForm.password = typeof decrypted === 'string' ? decrypted : loginForm.password
  }
  loginForm.rememberMe = rememberMe === undefined ? false : Boolean(rememberMe)
}

getCode()
getCookie()
</script>

<style scoped src="../assets/student/login.css"></style>

<style scoped>
/* 根容器填满视口：原页面由 body 承担，这里由 .sp-root 承担 */
.sp-root {
  min-height: 100vh;
}

/* 验证码行：保持原有输入框外观，右侧加图形验证码 */
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: stretch;
}

.captcha-row input {
  flex: 1;
  min-width: 0;
}

.captcha-img {
  height: 46px;
  border-radius: 8px;
  border: 1px solid var(--border, #e2e5ed);
  cursor: pointer;
  background: #fff;
  flex-shrink: 0;
}

/* 记住账号：沿用 auth-field 的字号体系，横排 */
.remember-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--text2, #5a6076);
  cursor: pointer;
}

.remember-row input {
  width: auto;
  accent-color: var(--primary-purple, #7b1fa2);
}
</style>
