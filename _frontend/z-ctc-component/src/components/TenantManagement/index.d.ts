import {ComponentType} from 'react'

export interface TenantManagementProps {
    apiBaseURL?: string
}

declare const TenantManagement: ComponentType<TenantManagementProps>
export default TenantManagement
export {TenantManagement}
