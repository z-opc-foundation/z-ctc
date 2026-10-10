// z-ctc 4A 组件层（library mode）
// 暴露给业务方（z-opc / 其它业务项目）通过 file: 或 npm 消费
import { Button } from 'antd';
import type { FC } from 'react';

export interface HelloCtcProps {
    name?: string;
}

/** 4A 中心示例组件 —— 演示业务方怎么消费 */
export const HelloCtc: FC<HelloCtcProps> = ({ name = '4A' }) => {
    return <Button type="primary">Hello {name}（来自 @yuku123/z-ctc-component）</Button>;
};

/** 重导出 antd 常用组件，省得业务方自己再 import */
export { Button, Table, Form, Input, Card, Space } from 'antd';

export default { HelloCtc };
// §8.7 域目录清退：ctc 域 named exports
/**
 * CTC 组件库 - 导出所有可复用组件
 * 打包后发布到私有npm仓库，供其他模块使用
 */
export {default as UserTable} from './components/UserTable'
export {default as StatusTag} from './components/StatusTag'

// 组织 / 部门 / 组 管理 (FEATURE016) — 业务组件 (需要传 tenants/domains props)
export {default as OrgManagement} from './components/OrgManagement'

// 4A 中心各域管理页 (FEATURE015+ 后从 z-opc-main-starter-frontend 迁入)
//   - 自带租户/域/应用拉取, 不依赖外部 store
//   - 走统一的 ctcRequest / ctcAc / ctcAuthorization / metaApp 后端
export {default as OrgPage} from './components/OrgPage'
export {default as TenantManagement} from './components/TenantManagement'
export {default as AccountManagement} from './components/AccountManagement'
export {default as ApplicationManagement} from './components/ApplicationManagement'
export {default as RoleManagement} from './components/RoleManagement'
export {default as PermissionManagement} from './components/PermissionManagement'
export {default as SurlManagement} from './components/SurlManagement'

// 4A 中心补齐 FEATURE015+ 后未迁入的页面
export {default as DictPage} from './components/DictPage'
export {default as AuditPage} from './components/AuditPage'
export {default as CtcOverview} from './components/CtcOverview'

// FEATURE016: 暴露 z-ctc-ac 的 API client (供 App.jsx 顶栏 租户/域 下拉调用)
export {ctcAcTenantApi, ctcAcDomainApi} from './components/TenantManagement/api'
