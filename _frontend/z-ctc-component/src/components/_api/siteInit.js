/**
 * 建站初始化 API (4A 中心的建站编排面).
 *
 * 后端端点 (z-ctc SiteInitController):
 *   - GET  /api/ctc/site-init/registrations              : 注册表 (ctc 感知到的域)
 *   - POST /api/ctc/site-init/registrations/disable      : 停用某域注册
 *   - POST /api/ctc/site-init/runs                        : 对某站点跑全量域初始化
 *   - POST /api/ctc/site-init/runs/domain                 : 只跑某域
 *   - GET  /api/ctc/site-init/runs/page                   : 任务记录分页
 *   - GET  /api/ctc/site-init/manifest/{domainCode}       : 某域的能力清单 (实时拉)
 *
 * 注意: @yuku123/z-frontend-common 的响应拦截器已统一解包,
 *       业务层拿到的 r 已经是内层数据, 不需要再 .then(r => r.data)。
 */

import {createRequest} from '@yuku123/z-frontend-common'

import {getApiBaseURL} from './base'

function makeRequest() {
    return createRequest({baseURL: getApiBaseURL(), timeout: 60000})
}

export const siteInitApi = {
    /** GET /api/ctc/site-init/registrations — 注册表列表 */
    listRegistrations: () => makeRequest().get('/ctc/site-init/registrations'),

    /** POST /api/ctc/site-init/registrations/disable — 停用某域注册 */
    disableRegistration: (domainCode) =>
        makeRequest().post('/ctc/site-init/registrations/disable', domainCode, {
            headers: {'Content-Type': 'application/json'},
        }),

    /** POST /api/ctc/site-init/runs — 对某站点执行建站初始化 (全量域) */
    run: (tenantCode, params) => makeRequest().post('/ctc/site-init/runs', {tenantCode, params}),

    /** POST /api/ctc/site-init/runs/domain — 只跑某域 */
    runDomain: (tenantCode, domainCode, params) =>
        makeRequest().post('/ctc/site-init/runs/domain', {tenantCode, domainCode, params}),

    /** GET /api/ctc/site-init/runs/page — 任务记录分页 */
    pageRuns: (params) => makeRequest().get('/ctc/site-init/runs/page', {params}),

    /** GET /api/ctc/site-init/manifest/{domainCode} — 某域能力清单 */
    manifest: (domainCode) => makeRequest().get(`/ctc/site-init/manifest/${domainCode}`),
}
