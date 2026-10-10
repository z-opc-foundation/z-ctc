import {useEffect, useState} from 'react'
import {OrgManagement} from '../OrgManagement'
import {configureApiBaseURL, ctcAcDomainApi, ctcAcTenantApi} from '../TenantManagement/api'
import {PageHeader} from '@/common/components/ui'

/**
 * 4A 中心 / 组织管理 (4a/org).
 *
 * FEATURE016: 页面逻辑抽取为可复用组件 @yuku123/z-ctc-frontend-component/OrgManagement
 * 本组件是 4A 中心 / 组织管理 的标准页面 — 自动拉取租户 / 域, 维护 selected 状态, 传给 OrgManagement.
 *
 * 组件本身负责:
 *   - 组织/部门/组 三级树 CRUD (走 /api/ctc/ac/{orgs,depts,groups})
 *   - K-V 扩展配置 (暂留 localStorage)
 *
 * Props:
 *   - apiBaseURL?: string   后端 baseURL, 默认 '/api'
 *   - defaultTenant?: string 初始选中的租户 (默认从 localStorage z_tenant 读取)
 *   - defaultDomain?: string 初始选中的域 (默认从 localStorage z_domain 读取)
 *
 * 从 z-opc-main-starter-frontend/src/pages/ctc/4a/org/index.tsx 迁入.
 */
export default function OrgPage({apiBaseURL, defaultTenant, defaultDomain}) {
    // 配置 baseURL (组件 mount 时生效)
    useEffect(() => {
        if (apiBaseURL) configureApiBaseURL(apiBaseURL)
    }, [apiBaseURL])

    const [tenants, setTenants] = useState([])
    const [domains, setDomains] = useState([])
    const [selectedTenant, setSelectedTenant] = useState(null)
    const [selectedDomain, setSelectedDomain] = useState(null)

    // 加载租户列表
    useEffect(() => {
        (async () => {
            try {
                const list = await ctcAcTenantApi.listAll()
                const arr = Array.isArray(list) ? list : []
                const opts = arr.map(t => ({
                    value: t.tenantCode,
                    label: `${t.tenantName || t.tenantCode} (${t.tenantCode})`,
                }))
                setTenants(opts)
                const saved = defaultTenant || localStorage.getItem('z_tenant')
                if (saved && arr.some(t => t.tenantCode === saved)) {
                    setSelectedTenant(saved)
                } else if (arr.length > 0) {
                    setSelectedTenant(arr[0].tenantCode)
                }
            } catch (e) {
                // 静默失败 — 业务侧可选用 prop 接管数据源
                setTenants([])
            }
        })()
    }, [defaultTenant])

    // 选中租户变化 → 拉该租户下的域
    useEffect(() => {
        if (!selectedTenant) {
            setDomains([])
            setSelectedDomain(null)
            return
        }
        (async () => {
            try {
                const list = await ctcAcDomainApi.listByTenant(selectedTenant)
                const arr = Array.isArray(list) ? list : []
                const opts = arr.map(d => ({
                    value: d.domainCode,
                    label: `${d.domainName || d.domainCode} (${d.domainCode})`,
                }))
                setDomains(opts)
                const saved = defaultDomain || localStorage.getItem('z_domain')
                if (saved && arr.some(d => d.domainCode === saved)) {
                    setSelectedDomain(saved)
                } else if (arr.length > 0) {
                    setSelectedDomain(arr[0].domainCode)
                }
            } catch (e) {
                setDomains([])
            }
        })()
    }, [selectedTenant, defaultDomain])

    const onTenantChange = (code) => {
        setSelectedTenant(code)
        localStorage.setItem('z_tenant', code || '')
        localStorage.removeItem('z_domain')
    }

    const onDomainChange = (code) => {
        setSelectedDomain(code)
        localStorage.setItem('z_domain', code || '')
    }

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 12, height: '100%'}}>
            <PageHeader
                title="组织管理"
                subtitle="4A AC 域 · 组织/部门/组 三级树管理"
                breadcrumb={[{label: '4A 中心'}, {label: '组织管理'}]}
            />
            <OrgManagement
                tenants={tenants}
                domains={domains}
                selectedTenant={selectedTenant}
                selectedDomain={selectedDomain}
                onTenantChange={onTenantChange}
                onDomainChange={onDomainChange}
                apiBaseURL="/api"
            />
        </div>
    )
}
