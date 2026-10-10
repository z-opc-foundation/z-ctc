import { Button, Card, Space, Table, Tag } from 'antd'
import { HelloCtc } from '@yuku123/z-ctc-component'

// 原 App.jsx TabPane 内容迁出（lead 008 §13/§14）；数据仍为占位 mock
const mockAccounts = [
    { id: 1, username: 'admin', role: '管理员', status: '启用', createdAt: '2026-09-01 10:00' },
    { id: 2, username: 'auditor', role: '审计员', status: '启用', createdAt: '2026-09-05 14:20' },
    { id: 3, username: 'readonly', role: '只读', status: '停用', createdAt: '2026-09-10 09:15' },
]

const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '用户名', dataIndex: 'username' },
    { title: '角色', dataIndex: 'role', render: (r) => <Tag color="blue">{r}</Tag> },
    { title: '状态', dataIndex: 'status', render: (s) => <Tag color={s === '启用' ? 'green' : 'red'}>{s}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt' },
    { title: '操作', render: () => (
        <Space>
            <Button size="small">编辑</Button>
            <Button size="small" danger>删除</Button>
        </Space>
    ) },
]

export default function AccountsPage() {
    return (
        <Card title="账号管理" extra={<HelloCtc name="accounts" />}>
            <Table rowKey="id" dataSource={mockAccounts} columns={columns} />
        </Card>
    )
}
