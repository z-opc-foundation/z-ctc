import { TenantManagement } from '@yuku123/z-ctc-component'

/**
 * 系统租户 —— 平台管理员视角：全平台租户 + 各自域（左树右表）。
 *
 * 与「租户管理」共用同一个组件，只把租户来源换成平台全量接口
 * （/tenants/list-all，非平台管理员后端直接 403）。菜单在本页之外按
 * whoami 的 isPlatformAdmin 隐藏，这里不再各判一次。
 */
export default function SystemTenantPage() {
    return <TenantManagement platformView />
}
