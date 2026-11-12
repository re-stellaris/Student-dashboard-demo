import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useLockStore from '@/store/modules/lock'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

/**
 * 教育平台中「本人数据」页面的角色要求。
 *
 * 这些页面的数据由后端按 JWT 身份反查 edu_student / edu_teacher，
 * 角色不匹配时后端会返回「当前账号未绑定学生/教师信息」。
 * 与其让用户进去只看到一条红色 500 报错，不如在前端直接拦到统一提示页。
 * 注：/education/course、/education/curriculum、/education/analytics 是公共数据，不在此列。
 */
const EDUCATION_SCOPE_RULES: Array<{ match: RegExp; role: string; scope: 'student' | 'teacher' }> = [
  { match: /^\/education\/(dashboard|records|skill|planner)(\/|$)/, role: 'STUDENT', scope: 'student' },
  { match: /^\/education\/teaching(\/|$)/, role: 'TEACHER', scope: 'teacher' }
]

const isWhiteList = (path: string): boolean => {
  return whiteList.some((pattern: string) => isPathMatch(pattern, path))
}

router.beforeEach(async (to, from) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title as string)
    const isLock = useLockStore().isLock
    if (to.path === '/login') {
      NProgress.done()
      return { path: '/' }
    }
    if (isWhiteList(to.path)) {
      return true
    }
    if (isLock && to.path !== '/lock') {
      NProgress.done()
      return { path: '/lock' }
    }
    if (!isLock && to.path === '/lock') {
      NProgress.done()
      return { path: '/' }
    }
    if (useUserStore().roles.length === 0) {
      isRelogin.show = true
      try {
        // 拉取user_info信息
        await useUserStore().getInfo()
        isRelogin.show = false
        // 根据roles权限生成可访问的路由
        const accessRoutes = await usePermissionStore().generateRoutes()
        accessRoutes.forEach((route: any) => {
          if (!isHttp(route.path)) {
            router.addRoute(route)
          }
        })
        // 重新导航到目标路由，确保动态路由已注册
        return { ...to, replace: true }
      } catch (err) {
        await useUserStore().logOut()
        ElMessage.error(err)
        return { path: '/' }
      }
    }

    // ---- 教育平台：按角色直达 + 角色不匹配的友好拦截 ----
    const roles = useUserStore().roles

    // 学生/教师登录后（或访问根路径时）直接进教育平台，不再落到若依通用首页。
    // 管理员的首页仍是若依控制台首页，故不做跳转。
    if ((to.path === '/' || to.path === '/index') && !to.query.redirect) {
      if (roles.includes('STUDENT')) {
        NProgress.done()
        return { path: '/education/dashboard', replace: true }
      }
      if (roles.includes('TEACHER')) {
        NProgress.done()
        return { path: '/education/teaching', replace: true }
      }
    }

    // 「本人数据」页面需要对应角色；否则统一跳到提示页（见 views/education/no-access）
    const scopeRule = EDUCATION_SCOPE_RULES.find((rule) => rule.match.test(to.path))
    if (scopeRule && !roles.includes(scopeRule.role)) {
      NProgress.done()
      return { path: '/no-access', query: { scope: scopeRule.scope, from: to.fullPath } }
    }

    return true
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      return true
    }
    NProgress.done()
    return `/login?redirect=${to.fullPath}` // 否则全部重定向到登录页
  }
})

router.afterEach(() => {
  NProgress.done()
})
