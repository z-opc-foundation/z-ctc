import {useEffect, useState} from 'react'
import OrgManagement from '../OrgManagement'
import {configureApiBaseURL} from '../TenantManagement/api'
/**
 * 4A 中心 / 组织管理 (4a/org).
 *
 * FEATURE016: 页面逻辑抽取为可复用组件 @yuku123/z-ctc-frontend-component/OrgManagement
 * 本组件是 4A 中心 / 组织管理 的标准页面 — 租户/域**直接取登录态**，不再提供页内选择器：
 *   - userInfo.tenantCode（登录写入）
 *   - z_ctc_domain.domainCode（顶栏 TenantSwitcher 写入；tenantCode 与 userInfo 一致才采纳，
 *     防跨组织串台——与 TenantSwitcher 同一套校验口径）
 * 想看别的租户/域的组织 ⇒ 顶栏切域，本页随登录态走。
 *
 * 组件本身负责:
 *   - 组织/部门/组 三级树 CRUD (走 /api/ctc/ac/{orgs,depts,groups})
 *   - K-V 扩展配置 (暂留 localStorage)
 *
 * Props:
 *   - apiBaseURL?: string   后端 baseURL, 默认 '/api'
 *
 * 从 z-opc-main-starter-frontend/src/pages/ctc/4a/org/index.tsx 迁入.
 */
function readLoginTenantDomain() {
    let tenantCode = ''
    let domainCode = ''
    try {
        const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
        tenantCode = String(info?.tenantCode ?? '')
        domainCode = String(info?.domainCode ?? '')
    } catch { /* 登录态缺失按空处理 */ }
    try {
        const sel = JSON.parse(localStorage.getItem('z_ctc_domain') || 'null')
        if (sel && sel.tenantCode && sel.tenantCode === tenantCode && sel.domainCode) {
            domainCode = String(sel.domainCode)
        }
    } catch { /* 保持 userInfo 的域 */ }
    return {
        tenantCode: tenantCode || null,
        domainCode: domainCode || null,
    }
}

export default function OrgPage({apiBaseURL}) {
    // 配置 baseURL (组件 mount 时生效)
    useEffect(() => {
        if (apiBaseURL) configureApiBaseURL(apiBaseURL)
    }, [apiBaseURL])

    // 登录态在 mount 时读一次；切域走顶栏 TenantSwitcher（会整页刷新），无需监听 storage
    const [{tenantCode, domainCode}, setCtx] = useState(readLoginTenantDomain)

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 12, height: '100%'}}>
            <OrgManagement
                selectedTenant={tenantCode}
                selectedDomain={domainCode}
                apiBaseURL="/api"
            />
        </div>
    )
}
