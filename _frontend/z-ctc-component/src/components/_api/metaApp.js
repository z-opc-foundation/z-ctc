/**
 * z-meta 应用 API (从 z-opc-main-starter-frontend/src/services/metaApp 迁入).
 *
 * 后端端点 (z-meta):
 *   - MetaApplicationController (/api/meta-app/*) : 应用 CRUD + 分页查询
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

export const metaAppApi = {
    /** GET /api/meta-app/list — 分页查询 */
    listApplications: (params) => makeRequest().get('/meta-app/list', {params}),

    /** GET /api/meta-app/get?id=  */
    getApplication: (id) => makeRequest().get('/meta-app/get', {params: {id}}),

    /** POST /api/meta-app */
    create: (app) => makeRequest().post('/meta-app', app),

    /** PUT /api/meta-app?id= */
    update: (id, patch) => makeRequest().put('/meta-app', patch, {params: {id}}),

    /** DELETE /api/meta-app?id= */
    delete: (id) => makeRequest().delete('/meta-app', {params: {id}}),
}

export default metaAppApi
