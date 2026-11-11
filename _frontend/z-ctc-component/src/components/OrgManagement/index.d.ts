import {ComponentType} from 'react'

export interface OrgManagementProps {
    selectedTenant?: string | null
    selectedDomain?: string | null
    apiBaseURL?: string
    extConfigStorageKey?: string
}

declare const OrgManagement: ComponentType<OrgManagementProps>
export default OrgManagement
export {OrgManagement}