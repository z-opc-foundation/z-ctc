import React, { useState } from 'react';
import { Card, Space, Tabs, Table, Tag, Button } from 'antd';
import { HelloCtc } from '@yuku123/z-ctc-frontend-component';

// z-ctc 应用层示例：4A 中心主要管理页面（认证 / 授权 / 账号 / 审计）
// 实际页面由后续迭代填充（FEATURE035 / FEATURE036 ...）
const { TabPane } = Tabs;

const mockAccounts = [
    { id: 1, username: 'admin', role: '管理员', status: '启用', createdAt: '2026-09-01 10:00' },
    { id: 2, username: 'auditor', role: '审计员', status: '启用', createdAt: '2026-09-05 14:20' },
    { id: 3, username: 'readonly', role: '只读', status: '停用', createdAt: '2026-09-10 09:15' }
];

const App = () => {
    const [tab, setTab] = useState('accounts');

    return (
        <div style={{ padding: 24 }}>
            <Card title="z-ctc 4A 中心" extra={<HelloCtc name={tab} />}>
                <Tabs activeKey={tab} onChange={setTab}>
                    <TabPane tab="账号 (Accounts)" key="accounts">
                        <Table
                            rowKey="id"
                            dataSource={mockAccounts}
                            columns={[
                                { title: 'ID', dataIndex: 'id', width: 60 },
                                { title: '用户名', dataIndex: 'username' },
                                {
                                    title: '角色',
                                    dataIndex: 'role',
                                    render: (r) => <Tag color="blue">{r}</Tag>
                                },
                                {
                                    title: '状态',
                                    dataIndex: 'status',
                                    render: (s) => (
                                        <Tag color={s === '启用' ? 'green' : 'red'}>{s}</Tag>
                                    )
                                },
                                { title: '创建时间', dataIndex: 'createdAt' },
                                {
                                    title: '操作',
                                    render: () => (
                                        <Space>
                                            <Button size="small">编辑</Button>
                                            <Button size="small" danger>删除</Button>
                                        </Space>
                                    )
                                }
                            ]}
                        />
                    </TabPane>
                    <TabPane tab="认证 (Authn)" key="authn">
                        <p>认证配置（LDAP / OAuth2 / SAML）—— 占位</p>
                    </TabPane>
                    <TabPane tab="授权 (Authz)" key="authz">
                        <p>授权策略（RBAC / ABAC）—— 占位</p>
                    </TabPane>
                    <TabPane tab="审计 (Audit)" key="audit">
                        <p>审计日志 —— 占位</p>
                    </TabPane>
                </Tabs>
            </Card>
        </div>
    );
};

export default App;