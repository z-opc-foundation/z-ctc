/**
 * 4A 组件（@yuku123/z-ctc-component）统一后端 base。
 *
 * 为什么要有这个：此前 9 个 api 模块各自持一份 `_baseURL`（OrgManagement /
 * TenantManagement / SurlManagement / _api/{ctcAcAccount,ctcAuthorization,dict,
 * loginLog,metaApp,userPermission}），每份各有一个 configureApiBaseURL ⇒
 * 「改 base」要改 9 处、漏一处就有一个页面还在打老路径（2026-11-25 生产上
 * 组织管理页就是这种半残状态）。现在收敛成**一处**。
 *
 * 两种落法：
 *   - 默认 '/api'：z-ctc-admin 独立 UI 自己就映射 /api/ctc/**（同进程同端口），
 *     所以默认不能动。
 *   - 主壳（z-team）把 4A 页面挂在自己的域下时传 apiBaseURL='/kapi/z-ctc/api'
 *     ⇒ 走 kapi 统一网关（fleet 终局口径），不与主壳自身的 /api/** 抢路径。
 */
let _baseURL = '/api'

/** 当前生效的 base */
export function getApiBaseURL() {
    return _baseURL
}

/**
 * 设置统一 base（所有 4A api 模块立即生效）。
 *
 * @param baseURL string 传空则回落 '/api'
 * @returns 生效后的 base
 */
export function configureApiBaseURL(baseURL) {
    _baseURL = baseURL || '/api'
    return _baseURL
}
