import {ComponentType} from 'react'

export interface OrgPageProps {
    apiBaseURL?: string
    defaultTenant?: string
    defaultDomain?: string
}

declare const OrgPage: ComponentType<OrgPageProps>
export default OrgPage
export {OrgPage}
