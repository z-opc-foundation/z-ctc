/**
 * z-ctc-surl 短链 API (从 z-opc-main-starter-frontend/src/services/ctcSurl 迁入).
 *
 * 后端 (z-ctc/z-ctc-surl):
 *   - POST   /api/ctc/surl/shorten                — 生成短码
 *   - GET    /api/ctc/surl/info?mapKey=xxx        — 查询元信息 (请求参数)
 *   - DELETE /api/ctc/surl?mapKey=xxx             — 失效 (请求参数)
 *   - GET    /s/{mapKey}                          — 短链跳转 (保留路径变量, 公共短链)
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

export const ctcSurlApi = {
    /** POST /api/ctc/surl/shorten */
    shorten: (req) => makeRequest().post('/ctc/surl/shorten', req),

    /** GET /api/ctc/surl/info?mapKey=xxx — 查询元信息 (请求参数) */
    info: (mapKey) => makeRequest().get('/ctc/surl/info', {params: {mapKey}}),

    /** DELETE /api/ctc/surl?mapKey=xxx — 失效 (请求参数) */
    invalidate: (mapKey) => makeRequest().delete('/ctc/surl', {params: {mapKey}}),

    /** 前端拼接短链 (短码 → 完整 URL) */
    buildUrl: (mapKey) => {
        const base = (typeof window !== 'undefined' && window.location && window.location.origin) || ''
        return `${base}/s/${encodeURIComponent(mapKey)}`
    },
}

export default ctcSurlApi
