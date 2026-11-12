<template>
  <!-- 教育平台（管理/教师侧）外壳：沿用旧版 students-interface 的顶部导航视觉，
       替代若依 Layout（侧边栏 + 面包屑 + 标签页）。见 ADR-0009。 -->
  <div class="sp-root lg-shell">
    <nav class="topnav">
      <router-link class="nav-logo" to="/education/dashboard">
        <span aria-hidden="true">🎓</span> CUHK-SZ · Study Dashboard
      </router-link>

      <router-link
        v-for="link in links"
        :key="link.path"
        class="nav-link"
        :class="{ 'is-active': isActive(link.path) }"
        :to="link.path"
      >
        {{ link.title }}
      </router-link>

      <div class="nav-spacer"></div>

      <router-link v-if="isAdmin" class="nav-link" to="/index">控制台</router-link>
      <router-link v-if="isStudent" class="nav-link" to="/student">学生端</router-link>

      <div class="user-pill">
        <span>{{ displayName }}</span>
        <button class="ub" type="button" @click="signOut">Sign out</button>
      </div>
    </nav>

    <main class="lg-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts" name="LegacyShell">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import auth from '@/plugins/auth'

interface ShellLink {
  path: string
  title: string
  /** 需要的权限标识；为空表示登录即可见 */
  permission?: string
}

/**
 * 教育平台导航项。与原版一致：顶部一行文字链，不做多级菜单。
 * 只显示当前账号有权限的项（权限来自若依 /getInfo 的 permissions）。
 */
const ALL_LINKS: ShellLink[] = [
  { path: '/education/dashboard', title: '学习仪表盘', permission: 'edu:dashboard:view' },
  { path: '/education/course', title: '课程中心', permission: 'edu:course:list' },
  { path: '/education/curriculum', title: '培养方案', permission: 'edu:curriculum:list' },
  { path: '/education/planner', title: '选课规划', permission: 'edu:plan:list' },
  { path: '/education/records', title: '我的成绩', permission: 'edu:record:list' },
  { path: '/education/teaching', title: '教师授课', permission: 'edu:offering:list' },
  { path: '/education/skill', title: '技能画像', permission: 'edu:skill:view' },
  { path: '/education/analytics', title: '学情分析', permission: 'edu:analytics:view' },
  { path: '/education/advisor', title: '导师看板', permission: 'edu:advisor:view' }
]

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const isAdmin = computed<boolean>(() => userStore.roles.includes('admin'))
const isStudent = computed<boolean>(() => userStore.roles.includes('STUDENT'))

const links = computed<ShellLink[]>(() => {
  // 必须用若依的 auth.hasPermi 判断：超级管理员拿到的是通配权限 "*:*:*"，
  // 直接写 permissions.includes('*') 会判假，导致 admin 的整条导航栏为空。
  return ALL_LINKS.filter((link) => !link.permission || auth.hasPermi(link.permission))
})

const displayName = computed<string>(() => userStore.nickName || userStore.name || '未登录')

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(path + '/')
}

async function signOut(): Promise<void> {
  await userStore.logOut()
  router.push('/login')
}
</script>

<!-- 注意：这里刻意不加 scoped。education.css 的所有规则都以 .sp-root 为前缀，
     全局引入不会泄漏到若依自带页面；而一旦加 scoped，Vue 只会把 data-v 加到
     外套自身节点，页面内容（.lg-head / .lg-card / el-table 内部节点）将全部匹配不到，
     主题实际失效。见 ADR-0009。 -->
<style src="../assets/student/education.css"></style>
