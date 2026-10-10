/**
 * z-ctc-component 路由清单（lead 005 §8.6 #3：./pages 命名导出 routes，非空数组）
 *
 * 占位说明：z-ctc 的页面代码现仍住在 z-z-ctc-suit/src/ 下（manifest + page 文件），
 * 本 manifest 现阶段只列骨架路由供主壳的 domainRoutes 探测；正式消费请走 suit。
 * 下次重构把 page 文件搬入 component 后，Component 字段直接换成同模块 import 即可。
 */
import { HelloCtc } from '.';

export const appMeta = { title: 'z-ctc 控制台', short: 'z-ctc' }

export const routes = [
    { path: '/z-ctc/hello', title: 'Hello Ctc', order: 1, Component: HelloCtc },
]
