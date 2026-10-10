/**
 * z-ctc-authorization 资源 / 角色 API (从 z-opc-main-starter-frontend/src/services/ctcAuthorization 迁入).
 *
 * 后端端点 (z-ctc/z-ctc-authorization -> /api/ctc/authz):
 *   - 资源/角色 全部采用请求参数 (?xxx=) 传递, 不使用路径变量.
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

export const ctcAuthorizationApi = {
    // ── 资源 (Resource) ──
    // 列表按应用维度分页查询 (FEATURE015 page-by-app 端点) — 避免与 findResourceById(?id=) 冲突
    listResourcesByApp: (params) => makeRequest().get('/ctc/authz/resources/page-by-app', {params}),
    // 全量资源 / 按关键字分页 (FEATURE014)
    listAllResources: () => makeRequest().get('/ctc/authz/resources/list'),
    pageResources: (params) => makeRequest().get('/ctc/authz/resources/page', {params}),
    // 资源表中已注册的去重 appCode 列表
    distinctApps: () => makeRequest().get('/ctc/authz/resources/distinct-apps'),
    getResource: (id) => makeRequest().get('/ctc/authz/resources', {params: {id}}),
    createResource: (resource) => makeRequest().post('/ctc/authz/resources', resource),
    updateResource: (id, patch) => makeRequest().put('/ctc/authz/resources', patch, {params: {id}}),
    deleteResource: (id) => makeRequest().delete('/ctc/authz/resources', {params: {id}}),

    // 资源-角色关系
    listResourceRoles: (resourceId) => makeRequest().get('/ctc/authz/resources/roles', {params: {resourceId}}),
    grantRoleResource: (roleId, resourceId, dataFilter) => {
        // POST /api/ctc/authz/roles/grant-resource?roleId=&resourceId= — body 可选 {dataFilter} (行级过滤)
        const body = dataFilter == null ? null : {dataFilter}
        return makeRequest().post('/ctc/authz/roles/grant-resource', body, {params: {roleId, resourceId}})
    },
    revokeRoleResource: (roleId, resourceId) => makeRequest().delete('/ctc/authz/roles/revoke-resource', {
        params: {
            roleId,
            resourceId
        }
    }),

    // ── 角色 (Role) ──
    listAllRoles: () => makeRequest().get('/ctc/authz/roles/list'),
    pageRoles: (params) => makeRequest().get('/ctc/authz/roles/page', {params}),
    getRole: (id) => makeRequest().get('/ctc/authz/roles', {params: {id}}),
    createRole: (role) => makeRequest().post('/ctc/authz/roles', role),
    updateRole: (id, patch) => makeRequest().put('/ctc/authz/roles', patch, {params: {id}}),
    deleteRole: (id) => makeRequest().delete('/ctc/authz/roles', {params: {id}}),

    // ── 用户-角色 关系 ──
    listUserRoles: (userId) => makeRequest().get('/ctc/authz/users/roles', {params: {userId}}),
    listUserPermissions: (userId) => makeRequest().get('/ctc/authz/users/permissions', {params: {userId}}),
    grantUserRole: (userId, roleId, expireAt) => {
        // POST /api/ctc/authz/users/grant-role?userId=&roleId=&expireAt= — expireAt 可选 ISO-8601
        const params = {userId, roleId}
        if (expireAt) params.expireAt = expireAt
        return makeRequest().post('/ctc/authz/users/grant-role', null, {params})
    },
    revokeUserRole: (userId, roleId) => makeRequest().delete('/ctc/authz/users/revoke-role', {
        params: {
            userId,
            roleId
        }
    }),

    // ── 角色-资源 关系 (查询) ──
    listRolePermissions: (roleId) => makeRequest().get('/ctc/authz/roles/resources', {params: {roleId}}),

    // ── 权限检查 (binary allow/deny, 不解析 dataFilter) ──
    // POST /api/ctc/authz/check  body: { userId, resourceCode }
    check: (request) => makeRequest().post('/ctc/authz/check', request),
}

export default ctcAuthorizationApi
