/**
 * Vue 模块类型增强（module augmentation）。
 *
 * 说明：`src/types/global.d.ts` 是“全局脚本”文件（没有顶层 import/export），
 * 在其中写 `declare module 'vue' { ... }` 会被 TypeScript 视为**重新声明**整个 vue 模块，
 * 从而抹掉 vue 的真实导出类型（App / ref / computed 全部丢失，vue-tsc 满天报错）。
 *
 * 因此把对 vue 的增强单独放在本文件：文件里有顶层 `import 'vue'`，
 * 会自动成为模块，`declare module 'vue'` 于是被当作合法的接口合并（augmentation）。
 *
 * 新增 vue 类型增强请一律写在这里，不要写回 global.d.ts。
 */
import 'vue'

declare module 'vue' {
  interface ComponentInternalInstance {
    proxy: any
  }
}
