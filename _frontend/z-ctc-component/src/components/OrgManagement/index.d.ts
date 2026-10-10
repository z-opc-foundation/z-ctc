import {ComponentType} from 'react'

export interface OrgManagementProps {
    tenants?: Array<{ value?: string; label?: string; tenantCode?: string; tenantName?: string }>
    domains?: Array<{ value?: string; label?: string; domainCode?: string; domainName?: string }>
    selectedTenant?: string | null
    selectedDomain?: string | null
    onTenantChange?: (code: string | null) => void
    onDomainChange?: (code: string | null) => void
    apiBaseURL?: string
    extConfigStorageKey?: string
}

declare const OrgManagement: ComponentType<OrgManagementProps>
export default OrgManagement
export {OrgManagement}