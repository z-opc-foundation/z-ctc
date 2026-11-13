import {useEffect, useState} from 'react'
import OrgManagement from '../OrgManagement'
import {configureApiBaseURL, ctcAcDomainApi} from '../TenantManagement/api'
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

    // 兜底：登录 / select-tenant 的返回体不带 domainCode（account 表不存），z_ctc_domain
    // 只有显式点过顶栏「切换」才写入 —— 从未显式选过域（新浏览器 / 清过 localStorage）时
    // 本页会永远卡在「未获取到登录租户/域上下文」。与 TenantSwitcher chip 的 implied 口径
    // 一致：租户下**恰好一个**域才借它（多个域仍要求显式选，防跨组织串台），借到后落盘
    // z_ctc_domain，与显式选择同一数据形状。
    useEffect(() => {
        if (!tenantCode || domainCode) return
        let cancelled = false
        ctcAcDomainApi.listByTenant(tenantCode).then((list) => {
            if (cancelled || !Array.isArray(list) || list.length !== 1) return
            const d = list[0]
            const code = String(d?.domainCode ?? '')
            if (!code) return
            const sel = {tenantCode, domainCode: code, domainName: String(d?.domainName ?? code)}
            try {
                localStorage.setItem('z_ctc_domain', JSON.stringify(sel))
            } catch { /* 落盘失败只影响下次进入, 本次 state 已兜住 */ }
            setCtx((prev) => (prev.domainCode ? prev : {...prev, domainCode: code}))
        }).catch(() => { /* 拉不到域保持空态提示, 不打断页面 */ })
        return () => { cancelled = true }
    }, [tenantCode, domainCode])

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
