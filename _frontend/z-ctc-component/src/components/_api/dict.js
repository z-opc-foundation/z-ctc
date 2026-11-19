/**
 * z-ctc 灵活字典 API (承接自 z-meta 字典能力).
 *
 * 后端端点 (z-ctc DictController -> /api/ctc/ac/dict):
 *   - GET  /categories/list?tenantCode=&keyword=  : 可见分类列表
 *   - GET  /categories/{id}                       : 按主键取分类
 *   - POST /categories                            : 新建分类, 返回新 ID
 *   - PUT  /categories/{id}                       : 改分类 (patch 语义, 只覆盖非空字段)
 *   - DEL  /categories/{id}                       : 删分类 (有子分类/条目时 409)
 *   - GET  /entries/list?category=&tenantCode=&domainCode= : 列条目 (只含生效条目)
 *   - GET  /entries/{id}                          : 按主键取条目
 *   - POST /entries                               : 新建条目 (作用域继承分类)
 *   - PUT  /entries/{id}                          : 改条目
 *   - DEL  /entries/{id}                          : 删条目 (tree 有子条目时 409)
 *   - GET  /lookup?category=&key=&tenantCode=&domainCode=  : 按分类+键查单条 (回退链)
 *   - GET  /resolve?categories=a,b&...            : 批量查多分类条目
 *
 * 注意: @yuku123/z-frontend-common 的响应拦截器已统一解包,
 *       业务层拿到的 r 已经是内层数据, 不需要再 .then(r => r.data).
 */

import {createRequest} from '@yuku123/z-frontend-common'

let _baseURL = '/api'

export function configureApiBaseURL(baseURL) {
    _baseURL = baseURL || '/api'
}

function makeRequest() {
    return createRequest({baseURL: _baseURL, timeout: 15000})
}

export const dictApi = {
    // 字典分类
    /** GET /api/ctc/ac/dict/categories/list — 可见分类列表 */
    listCategories: (params) => makeRequest().get('/ctc/ac/dict/categories/list', {params}),

    /** GET /api/ctc/ac/dict/categories/{id} — 按主键取分类 */
    getCategory: (id) => makeRequest().get(`/ctc/ac/dict/categories/${id}`),

    /** POST /api/ctc/ac/dict/categories — 新建分类, 返回新 ID */
    createCategory: (data) => makeRequest().post('/ctc/ac/dict/categories', data),

    /** PUT /api/ctc/ac/dict/categories/{id} — 改分类 (patch, catCode 编辑禁用) */
    updateCategory: (id, data) => makeRequest().put(`/ctc/ac/dict/categories/${id}`, data),

    /** DELETE /api/ctc/ac/dict/categories/{id} — 删分类 (内置分类 409) */
    deleteCategory: (id) => makeRequest().delete(`/ctc/ac/dict/categories/${id}`),

    // 字典条目
    /** GET /api/ctc/ac/dict/entries/list?category=xxx — 列某分类下生效条目 */
    listEntries: (category, params) => makeRequest().get('/ctc/ac/dict/entries/list', {params: {category, ...params}}),

    /** GET /api/ctc/ac/dict/entries/{id} — 按主键取条目 */
    getEntry: (id) => makeRequest().get(`/ctc/ac/dict/entries/${id}`),

    /** POST /api/ctc/ac/dict/entries — 新建条目, 返回新 ID */
    createEntry: (data) => makeRequest().post('/ctc/ac/dict/entries', data),

    /** PUT /api/ctc/ac/dict/entries/{id} — 改条目 (patch) */
    updateEntry: (id, data) => makeRequest().put(`/ctc/ac/dict/entries/${id}`, data),

    /** DELETE /api/ctc/ac/dict/entries/{id} — 删条目 */
    deleteEntry: (id) => makeRequest().delete(`/ctc/ac/dict/entries/${id}`),

    /** GET /api/ctc/ac/dict/lookup — 按分类+键查单条 (GLOBAL→TENANT→DOMAIN 回退链) */
    lookup: (params) => makeRequest().get('/ctc/ac/dict/lookup', {params}),

    /** GET /api/ctc/ac/dict/resolve?categories=a,b — 批量查多分类条目, 返回 {分类: [条目]} */
    resolve: (categories, params) => makeRequest().get('/ctc/ac/dict/resolve', {params: {categories: categories.join(','), ...params}}),
}

export default dictApi
