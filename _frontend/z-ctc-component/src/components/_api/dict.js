/**
 * z-meta 字典 API (从 z-opc-main-starter-frontend/src/services/dict 迁入).
 *
 * 后端端点 (z-meta):
 *   - DictController (/api/dict) : 字典类型 + 字典项 CRUD
 *
 * 注意: @/common/utils/request 已统一在响应拦截器中解包 Result<T> 与裸 Map,
 *       所以业务层拿到的 r 已经是内层 data, 不需要再 .then(r => r.data).
 */

import {createRequest} from '@/common/utils/request'

let _baseURL = '/api'

export function configureApiBaseURL(baseURL) {
    _baseURL = baseURL || '/api'
}

function makeRequest() {
    return createRequest({baseURL: _baseURL, timeout: 15000})
}

export const dictApi = {
    // 字典类型
    listDictType: (params) => makeRequest().get('/dict/type/list', {params}),
    getDictType: (id) => makeRequest().get('/dict/type/get', {params: {id}}),
    createDictType: (data) => makeRequest().post('/dict/type', data),
    updateDictType: (id, data) => makeRequest().put('/dict/type', data, {params: {id}}),
    deleteDictType: (id) => makeRequest().delete('/dict/type', {params: {id}}),

    // 字典项
    listDictItem: (dictId) => makeRequest().get('/dict/item/list', {params: {dictId}}),
    listDictItemByCode: (dictCode) => makeRequest().get('/dict/items/get', {params: {dictCode}}),
    createDictItem: (data) => makeRequest().post('/dict/item', data),
    updateDictItem: (id, data) => makeRequest().put('/dict/item', data, {params: {id}}),
    deleteDictItem: (id) => makeRequest().delete('/dict/item', {params: {id}}),
}

export default dictApi