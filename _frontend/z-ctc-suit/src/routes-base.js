/** 路由前缀：生产 '' （context-path /），开发 '/z-ctc'。由 vite define 注入。 */
export const BASE = __CTC_BASE__

export const at = (path) => `${BASE}${path}`
