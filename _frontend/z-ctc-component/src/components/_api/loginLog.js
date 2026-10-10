/**
 * z-ctc-ac 登录日志 API (走 GET 接口, 与原 ctcAcLoginLogApi 区别).
 *
 * 后端端点 (z-ctc-ac):
 *   - LoginLogController (/api/ctc/ac/login-log) : GET /list, /page, /recent
 *
 * 后端返回裸 Map { total, list, page, size }, 不做 Result 解包.
 * 注意: @/common/utils/request 的响应拦截器会保持 Map 原样向下传,
 *       所以业务层拿到的 r 已经是 Map, 不需要再 .then(r => r.data).
 */

import {createRequest} from '@/common/utils/request'

let _baseURL = '/api'

export function configureApiBaseURL(baseURL) {
    _baseURL = baseURL || '/api'
}

function makeRequest() {
    return createRequest({baseURL: _baseURL, timeout: 15000})
}

export const loginLogApi = {
    /** GET /api/ctc/ac/login-log/list — 条件分页查询 */
    list: (params) => makeRequest().get('/ctc/ac/login-log/list', {params}),

    /** GET /api/ctc/ac/login-log/page — pageNum/pageSize 别名 */
    page: (params) => makeRequest().get('/ctc/ac/login-log/page', {params}),

    /** GET /api/ctc/ac/login-log/recent — 最新 10 条 */
    recent: () => makeRequest().get('/ctc/ac/login-log/recent'),
}

export default loginLogApi