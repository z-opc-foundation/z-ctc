/**
 * z-ctc-ac 组织 / 部门 / 组别 API (FEATURE016 后端持久化).
 *
 * 后端 16 端点 (z-ctc/z-ctc-ac/OrgController @ /api/ctc/ac):
 *   - /orgs  POST / GET(list) / GET(?orgCode=) / PUT(?orgCode=) / DELETE(?orgCode=) / GET(health)
 *   - /depts POST / GET(list) / GET(?deptCode=) / PUT(?deptCode=) / DELETE(?deptCode=)
 *   - /groups POST / GET(list) / GET(?groupCode=) / PUT(?groupCode=) / DELETE(?groupCode=)
 *
 * 数据模型字段:
 *   id, tenantCode, domainCode, orgCode, orgName, status, description, extConfig,
 *   createdAt, createdBy, updatedAt, updatedBy
 *
 * 注意: @/common/utils/request 的响应拦截器已统一解包,
 *       业务层拿到的 r 已经是内层数据, 不需要再 .then(r => r.data).
 * 统一规约: 全部接口均使用请求参数 (?xxx=), 不使用路径变量.
 *
 * 租户 / 域由组件使用者通过 props 传入 (本组件不耦合 z-meta).
 */

import {createRequest} from '@/common/utils/request'

let _baseURL = '/api'

export function configureApiBaseURL(baseURL) {
    _baseURL = baseURL || '/api'
}

function makeRequest() {
    return createRequest({baseURL: _baseURL, timeout: 15000})
}

// ========== Org (组织) ==========

export const ctcAcOrgApi = {
    /** POST /api/ctc/ac/orgs — 创建组织, 返回新 ID */
    create: (org) => makeRequest().post('/ctc/ac/orgs', org),

    /** GET /api/ctc/ac/orgs/list?tenantCode=&domainCode= — 按域列出组织 */
    listByDomain: (tenantCode, domainCode) =>
        makeRequest().get('/ctc/ac/orgs/list', {params: {tenantCode, domainCode}}),

    /** GET /api/ctc/ac/orgs?tenantCode=&domainCode=&orgCode= — 按编码查询 (请求参数) */
    getByCode: (tenantCode, domainCode, orgCode) =>
        makeRequest().get('/ctc/ac/orgs', {params: {tenantCode, domainCode, orgCode}}),

    /** PUT /api/ctc/ac/orgs?tenantCode=&domainCode=&orgCode= — 更新 (请求参数) */
    update: (tenantCode, domainCode, orgCode, patch) =>
        makeRequest().put('/ctc/ac/orgs', patch, {
            params: {tenantCode, domainCode, orgCode},
        }),

    /** DELETE /api/ctc/ac/orgs?tenantCode=&domainCode=&orgCode= — 删除 (请求参数) */
    delete: (tenantCode, domainCode, orgCode) =>
        makeRequest().delete('/ctc/ac/orgs', {
            params: {tenantCode, domainCode, orgCode},
        }),
}

// ========== Dept (部门) ==========

export const ctcAcDeptApi = {
    create: (dept) => makeRequest().post('/ctc/ac/depts', dept),

    listByOrg: (tenantCode, domainCode, orgCode) =>
        makeRequest().get('/ctc/ac/depts/list', {params: {tenantCode, domainCode, orgCode}}),

    /** GET /api/ctc/ac/depts?tenantCode=&domainCode=&orgCode=&deptCode= — 按编码查询 (请求参数) */
    getByCode: (tenantCode, domainCode, deptCode, orgCode) =>
        makeRequest().get('/ctc/ac/depts', {params: {tenantCode, domainCode, orgCode, deptCode}}),

    /** PUT /api/ctc/ac/depts?tenantCode=&domainCode=&deptCode= — 更新 (请求参数) */
    update: (tenantCode, domainCode, deptCode, patch) =>
        makeRequest().put('/ctc/ac/depts', patch, {
            params: {tenantCode, domainCode, deptCode},
        }),

    /** DELETE /api/ctc/ac/depts?tenantCode=&domainCode=&orgCode=&deptCode= — 删除 (请求参数) */
    delete: (tenantCode, domainCode, deptCode, orgCode) =>
        makeRequest().delete('/ctc/ac/depts', {
            params: {tenantCode, domainCode, orgCode, deptCode},
        }),
}

// ========== Group (组别) ==========

export const ctcAcGroupApi = {
    create: (group) => makeRequest().post('/ctc/ac/groups', group),

    listByDept: (tenantCode, domainCode, deptCode) =>
        makeRequest().get('/ctc/ac/groups/list', {params: {tenantCode, domainCode, deptCode}}),

    /** GET /api/ctc/ac/groups?tenantCode=&domainCode=&deptCode=&groupCode= — 按编码查询 (请求参数) */
    getByCode: (tenantCode, domainCode, deptCode, groupCode) =>
        makeRequest().get('/ctc/ac/groups', {params: {tenantCode, domainCode, deptCode, groupCode}}),

    /** PUT /api/ctc/ac/groups?tenantCode=&domainCode=&deptCode=&groupCode= — 更新 (请求参数) */
    update: (tenantCode, domainCode, deptCode, groupCode, patch) =>
        makeRequest().put('/ctc/ac/groups', patch, {
            params: {tenantCode, domainCode, deptCode, groupCode},
        }),

    /** DELETE /api/ctc/ac/groups?tenantCode=&domainCode=&deptCode=&groupCode= — 删除 (请求参数) */
    delete: (tenantCode, domainCode, deptCode, groupCode) =>
        makeRequest().delete('/ctc/ac/groups', {
            params: {tenantCode, domainCode, deptCode, groupCode},
        }),
}

export const ctcAcOrgDeptGroupApi = {
    org: ctcAcOrgApi,
    dept: ctcAcDeptApi,
    group: ctcAcGroupApi,
}

export default ctcAcOrgDeptGroupApi
