<template>
  <div class="sp-root no-access-page">
    <div class="app-container">
    <el-result icon="info" :title="title" :sub-title="subTitle">
      <template #extra>
        <div class="no-access">
          <el-alert type="info" :closable="false" show-icon class="no-access__tip">
            <template #title>为什么会看到这个页面？</template>
            <p>
              教育平台采用<strong>「身份即权限」</strong>的设计：与本页对应的数据由后端按<strong>当前登录身份</strong>
              从 JWT 反查（<code>edu_student.sys_user_id</code> / <code>edu_teacher.sys_user_id</code>），
              不接受前端传入的任何 ID。因此没有对应学籍或教师档案的账号无法查看这类「本人数据」。
            </p>
          </el-alert>

          <el-table :data="accounts" size="small" border class="no-access__table">
            <el-table-column prop="username" label="演示账号" width="150" />
            <el-table-column prop="password" label="密码" width="120" />
            <el-table-column prop="role" label="角色" width="110" />
            <el-table-column prop="scope" label="可访问的本人数据" />
          </el-table>

          <div class="no-access__actions">
            <el-button type="primary" @click="goHome">返回首页</el-button>
            <el-button @click="goBack">回到上一页</el-button>
            <el-button v-if="scope === 'student'" @click="relogin">用学生账号重新登录</el-button>
            <el-button v-else @click="relogin">用教师账号重新登录</el-button>
          </div>
        </div>
      </template>
    </el-result>
  </div>
  </div>
</template>

<script setup lang="ts" name="EducationNoAccess">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

/** student = 学生本人数据页；teacher = 教师本人授课页 */
const scope = computed(() => (route.query.scope === 'teacher' ? 'teacher' : 'student'))

const title = computed(() =>
  scope.value === 'student' ? '该页面仅对学生账号开放' : '该页面仅对教师账号开放'
)

const subTitle = computed(() =>
  scope.value === 'student'
    ? '当前登录账号没有关联学生学籍，无法展示「本人」的学习数据（学分进度、成绩、选课规划、技能画像）。'
    : '当前登录账号没有关联教师档案，无法展示「本人」的授课数据（开课、作业、成绩录入）。'
)

const accounts = computed(() =>
  scope.value === 'student'
    ? [
        { username: 'stu_chen', password: 'admin123', role: '学生 STUDENT', scope: '学习仪表盘 / 我的成绩 / 选课规划 / 技能画像' },
        { username: 'stu_wang', password: 'admin123', role: '学生 STUDENT', scope: '同上（另一名演示学生）' },
        { username: 'admin', password: 'admin123', role: '管理员', scope: '课程中心 / 培养方案 / 学情分析等公共数据' }
      ]
    : [
        { username: 'teacher_li', password: 'admin123', role: '教师 TEACHER', scope: '教师授课（本人 6 门开课、作业与评分）' },
        { username: 'admin', password: 'admin123', role: '管理员', scope: '课程中心 / 培养方案 / 学情分析等公共数据' }
      ]
)

function goHome(): void {
  // 学生端门户 / 若依控制台首页
  router.push(scope.value === 'student' ? '/student' : '/education/teaching')
}

function goBack(): void {
  router.back()
}

function relogin(): void {
  router.push({ path: '/login', query: { redirect: route.query.from as string || undefined } })
}
</script>

<style scoped lang="scss">
.no-access-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.no-access-page .app-container {
  width: 100%;
  max-width: 980px;
  padding: 24px;
}

.no-access {
  max-width: 760px;
  margin: 0 auto;
  text-align: left;

  &__tip {
    margin-bottom: 16px;
    line-height: 1.7;

    p {
      margin: 4px 0 0;
      word-break: break-word;
    }

    code {
      padding: 0 4px;
      background: var(--el-fill-color-light);
      border-radius: 3px;
    }
  }

  &__table {
    margin-bottom: 20px;
  }

  &__actions {
    display: flex;
    gap: 12px;
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>
