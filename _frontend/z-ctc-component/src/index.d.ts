export {default as UserTable} from './components/UserTable'
export {default as StatusTag} from './components/StatusTag'
export {default as OrgManagement} from './components/OrgManagement'
export {default as OrgPage} from './components/OrgPage'
export {default as TenantManagement} from './components/TenantManagement'
export {default as AccountManagement} from './components/AccountManagement'
export {default as ApplicationManagement} from './components/ApplicationManagement'
export {default as RoleManagement} from './components/RoleManagement'
export {default as PermissionManagement} from './components/PermissionManagement'
export {default as SurlManagement} from './components/SurlManagement'

// 以下三项 index.js 已导出、此处原先漏声明：TS 走 .d.ts（同目录优先解析声明文件），
// 运行时走 .js，于是 SystemShell 报 TS2305「no exported member」而运行期正常。
// 补齐后与 index.js 保持一致。
export {default as DictPage} from './components/DictPage'
export {default as AuditPage} from './components/AuditPage'
export {default as CtcOverview} from './components/CtcOverview'

// FEATURE016: z-ctc-ac API client
export {ctcAcTenantApi, ctcAcDomainApi} from './components/TenantManagement/api'
