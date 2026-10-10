/**
 * 4A 用户权限 API — 查询某个用户的角色 + 资源（权限点）.
 *
 * 后端端点 (z-ctc-ac + z-ctc-authz), 统一规约: 全部使用请求参数 (?xxx=).
 *   - /api/ctc/ac/accounts                   — 用户列表
 *   - /api/ctc/authz/users/roles?userId=     — 用户的角色
 *   - /api/ctc/authz/users/permissions?userId= — 用户的权限
 *   - /api/ctc/authz/roles/resources?roleId= — 角色拥有的资源
 */

import {createRequest} from '@/common/utils/request'
import {ctcAuthorizationApi} from './ctcAuthorization'
import {ctcAcAccountApi} from './ctcAcAccount'

let _baseURL = '/api'

export function configureApiBaseURL(baseURL) {
    _baseURL = baseURL || '/api'
}

function makeRequest() {
    return createRequest({baseURL: _baseURL, timeout: 15000})
}

export const userPermissionApi = {
    /** 用户列表 — 来自 z-ctc-ac */
    listAccounts: (params) => ctcAcAccountApi.listByTenant(params),

    /** 查询某用户的角色 */
    listUserRoles: (userId) => ctcAuthorizationApi.listUserRoles(userId),

    /** 查询某用户的权限（资源编码列表） */
    listUserPermissions: (userId) => ctcAuthorizationApi.listUserPermissions(userId),

    /** 查询某角色拥有的资源 */
    listRoleResources: (roleId) => ctcAuthorizationApi.listRolePermissions(roleId),
}

export default userPermissionApi