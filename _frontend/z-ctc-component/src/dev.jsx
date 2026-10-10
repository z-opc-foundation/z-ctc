/**
 * 组件库开发调试入口
 */
import React from 'react'
import ReactDOM from 'react-dom/client'
import {
    AccountManagement,
    ApplicationManagement,
    OrgManagement,
    OrgPage,
    PermissionManagement,
    RoleManagement,
    StatusTag,
    SurlManagement,
    TenantManagement,
    UserTable,
} from './index.js'

const App = () => {
    const users = [
        {id: 1, userName: 'admin', nickName: '管理员', phone: '13800138000', email: 'admin@test.com', status: 1},
        {id: 2, userName: 'test', nickName: '测试', phone: '13900001111', email: 'test@test.com', status: 0},
    ]

    return (
        <div style={{padding: 20}}>
            <h1>CTC 组件库 Dev</h1>

            <h2>StatusTag 示例</h2>
            <StatusTag status={1}/>
            <StatusTag status={0}/>
            <StatusTag status="pending"/>

            <h2>UserTable 示例</h2>
            <UserTable
                dataSource={users}
                loading={false}
                onEdit={(record) => console.log('edit', record)}
                onDelete={(record) => console.log('delete', record)}
            />

            <h2>OrgManagement 示例 (FEATURE016 — 业务组件, 需传 tenants/domains props)</h2>
            <div style={{height: 400, border: '1px solid #eee'}}>
                <OrgManagement/>
            </div>

            <h2>OrgPage 示例 (FEATURE015 4A 中心 / 组织管理)</h2>
            <div style={{height: 400, border: '1px solid #eee'}}>
                <OrgPage/>
            </div>

            <h2>TenantManagement 示例 (4A 中心 / 租户管理)</h2>
            <div style={{height: 600, border: '1px solid #eee'}}>
                <TenantManagement/>
            </div>

            <h2>AccountManagement 示例 (4A 中心 / 账号管理)</h2>
            <div style={{border: '1px solid #eee'}}>
                <AccountManagement/>
            </div>

            <h2>ApplicationManagement 示例 (4A 中心 / 应用管理)</h2>
            <div style={{height: 600, border: '1px solid #eee'}}>
                <ApplicationManagement/>
            </div>

            <h2>RoleManagement 示例 (4A 中心 / 角色管理)</h2>
            <div style={{border: '1px solid #eee'}}>
                <RoleManagement/>
            </div>

            <h2>PermissionManagement 示例 (4A 中心 / 权限管理)</h2>
            <div style={{border: '1px solid #eee'}}>
                <PermissionManagement/>
            </div>

            <h2>SurlManagement 示例 (4A 中心 / 短链生成器)</h2>
            <div style={{border: '1px solid #eee'}}>
                <SurlManagement/>
            </div>
        </div>
    )
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>)
