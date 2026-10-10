/**
 * z-ctc-ac 账号 / 登录日志 API (从 z-opc-main-starter-frontend/src/services/ctcAc 迁入).
 *
 * 后端端点 (z-ctc/z-ctc-ac -> /api/ctc/ac/accounts), 统一规约: 全部使用请求参数 (?xxx=).
 *   - AccountController  (/api/ctc/ac/accounts)  : 全部 CRUD + 状态/密码/分配角色
 *   - LoginLogController (/api/ctc/ac/login-log) : 1 endpoint
 *
 * 注意: @/common/utils/request 的响应拦截器已统一解包,
 *       业务层拿到的 r 已经是内层数据, 不需要再 .then(r => r.data).
 */

import {createRequest} from '@/common/utils/request'

let _baseURL = '/api'

export function configureApiBaseURL(baseURL) {
    _baseURL = baseURL || '/api'
}

function makeRequest() {
    return createRequest({baseURL: _baseURL, timeout: 15000})
}

export const ctcAcAccountApi = {
    /** POST /api/ctc/ac/accounts — 创建账号, 返回新 ID */
    create: (req) => makeRequest().post('/ctc/ac/accounts', req),

    /** GET /api/ctc/ac/accounts?id=xxx — 按主键查询 (请求参数) */
    getById: (id) => makeRequest().get('/ctc/ac/accounts', {params: {id}}),

    /** GET /api/ctc/ac/accounts?tenant=&pageNum=&pageSize= — 按租户分页 */
    listByTenant: (params) => makeRequest().get('/ctc/ac/accounts', {params}),

    /** POST /api/ctc/ac/accounts/status?id=&status= — 启/停用 (请求参数) */
    updateStatus: (id, status) => makeRequest().post('/ctc/ac/accounts/status', null, {
        params: {id, status},
    }),

    /** POST /api/ctc/ac/accounts/password/change?id= — 修改密码 (请求参数) */
    changePassword: (id, req) => makeRequest().post('/ctc/ac/accounts/password/change', req, {
        params: {id},
    }),

    /** POST /api/ctc/ac/accounts/password/reset?id= — 重置密码 (请求参数) */
    resetPassword: (id, req) => makeRequest().post('/ctc/ac/accounts/password/reset', req, {
        params: {id},
    }),

    /** PUT /api/ctc/ac/accounts?id= — 更新账号字段 (请求参数) */
    update: (id, patch) => makeRequest().put('/ctc/ac/accounts', patch, {params: {id}}),

    /** DELETE /api/ctc/ac/accounts?id= — 删除账号 (请求参数) */
    delete: (id) => makeRequest().delete('/ctc/ac/accounts', {params: {id}}),

    /** POST /api/ctc/ac/accounts/assign-role?userId=&roleId= — 分配角色 (请求参数) */
    assignRole: (userId, roleId) => makeRequest().post('/ctc/ac/accounts/assign-role', null, {
        params: {userId, roleId},
    }),
}

export const ctcAcLoginLogApi = {
    /** POST /api/ctc/ac/login-log/list — 分页查询登录日志 */
    list: (params) => makeRequest().post('/ctc/ac/login-log/list', params),
}

export default ctcAcAccountApi
