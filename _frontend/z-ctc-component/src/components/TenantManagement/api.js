/**
 * z-ctc-ac 租户 / 域 API (FEATURE016 后端持久化).
 *
 * 后端端点 (z-ctc/z-ctc-ac):
 *   - TenantController   (/api/ctc/ac/tenants)  : create / getByCode / updateStatus / listAll
 *   - DomainController   (/api/ctc/ac/domains)  : listByTenant / listAll / getById / create / update / delete
 *
 * 数据模型字段:
 *   TenantDO: id, tenantCode, tenantName, status, description, createdAt, updatedAt
 *   DomainDO: id, domainCode, domainName, tenantCode, status, description, createdAt, updatedAt
 *
 * 注意: @yuku123/z-frontend-common 的响应拦截器已统一解包 Result<T>,
 *       业务层拿到的 r 已经是内层数据, 不需要再 .then(r => r.data).
 * 统一规约: 全部接口均使用请求参数 (?xxx=), 不使用路径变量.
 */

import {createRequest} from '@yuku123/z-frontend-common'

let _baseURL = '/api'

export function configureApiBaseURL(baseURL) {
    _baseURL = baseURL || '/api'
}

function makeRequest() {
    return createRequest({baseURL: _baseURL, timeout: 15000})
}

// ========== Tenant (租户) ==========

export const ctcAcTenantApi = {
    /** POST /api/ctc/ac/tenants — 创建租户, 返回新 ID */
    create: (tenant) => makeRequest().post('/ctc/ac/tenants', tenant),

    /** GET /api/ctc/ac/tenants?tenantCode=xxx — 按 code 查询 (请求参数) */
    getByCode: (tenantCode) =>
        makeRequest().get('/ctc/ac/tenants', {params: {tenantCode}}),

    /** POST /api/ctc/ac/tenants/status?tenantCode=&status= — 启/停用 (请求参数) */
    updateStatus: (tenantCode, status) =>
        makeRequest().post('/ctc/ac/tenants/status', null, {
            params: {tenantCode, status},
        }),

    /** GET /api/ctc/ac/tenants/list — 我登录后能看到的租户 (平台管理员=全量, 其他人=自己的 membership) */
    listAll: () => makeRequest().get('/ctc/ac/tenants/list'),

    /**
     * GET /api/ctc/ac/tenants/list-all — 全平台租户 (系统租户面板, 仅平台管理员)
     *
     * 与 listAll 的区别是「问题」不同而非「参数」不同: listAll 回答我能看哪些,
     * 这个回答平台上一共有哪些。非平台管理员调用返回 403 —— 前端据此显示无权限,
     * 而不是把空表当成「全平台没有租户」。
     */
    listAllForPlatform: () => makeRequest().get('/ctc/ac/tenants/list-all'),
}

// ========== Domain (域) ==========

export const ctcAcDomainApi = {
    /** GET /api/ctc/ac/domains/list?tenantCode=xxx — 租户下域列表 */
    listByTenant: (tenantCode) =>
        makeRequest().get('/ctc/ac/domains/list', {params: {tenantCode}}),

    /** GET /api/ctc/ac/domains/list-all — 全量 */
    listAll: () => makeRequest().get('/ctc/ac/domains/list-all'),

    /** GET /api/ctc/ac/domains?id=xxx — 按主键查询 (请求参数) */
    getById: (id) => makeRequest().get('/ctc/ac/domains', {params: {id}}),

    /** POST /api/ctc/ac/domains — 创建, 返回新 ID */
    create: (domain) => makeRequest().post('/ctc/ac/domains', domain),

    /** PUT /api/ctc/ac/domains?id=xxx — 更新 (请求参数) */
    update: (id, patch) => makeRequest().put('/ctc/ac/domains', patch, {params: {id}}),

    /** DELETE /api/ctc/ac/domains?id=xxx — 删除 (请求参数) */
    delete: (id) => makeRequest().delete('/ctc/ac/domains', {params: {id}}),
}

export const ctcAcTenantDomainApi = {
    tenant: ctcAcTenantApi,
    domain: ctcAcDomainApi,
}

/**
 * 默认域判定 —— 库里同时存在两种历史编码：早期建的叫 `default`，
 * 后来按租户建的叫 `<tenantCode>-default`。两种都不允许删：
 * 删掉之后该租户就没有兜底域，按租户取域的链路会空转。
 *
 * 放在这里而不是各页面各写一份：此前两处判定都只认 `<tenantCode>-default`，
 * 于是早期那条 `default` 域看着是默认域，实际能被删掉。
 */
export function isDefaultDomain(tenantCode, domain) {
    const code = domain?.domainCode
    if (!code) return false
    return code === 'default' || (!!tenantCode && code === `${tenantCode}-default`)
}

export default ctcAcTenantDomainApi
