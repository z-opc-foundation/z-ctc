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
 * 注意: @/common/utils/request 的响应拦截器已统一解包 Result<T>,
 *       业务层拿到的 r 已经是内层数据, 不需要再 .then(r => r.data).
 * 统一规约: 全部接口均使用请求参数 (?xxx=), 不使用路径变量.
 */

import {createRequest} from '@/common/utils/request'

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

    /** GET /api/ctc/ac/tenants/list — 全量列表 */
    listAll: () => makeRequest().get('/ctc/ac/tenants/list'),
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

export default ctcAcTenantDomainApi
