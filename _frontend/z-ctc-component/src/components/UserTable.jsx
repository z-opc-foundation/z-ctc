/**
 * 用户管理表格组件 - 可复用
 */
import {Button, Space, Table, Tag} from 'antd'
import {DeleteOutlined, EditOutlined} from '@ant-design/icons'

export default function UserTable({dataSource, loading, onEdit, onDelete, onRefresh}) {
    const columns = [
        {title: '用户名', dataIndex: 'userName', key: 'userName'},
        {title: '昵称', dataIndex: 'nickName', key: 'nickName'},
        {title: '手机号', dataIndex: 'phone', key: 'phone'},
        {title: '邮箱', dataIndex: 'email', key: 'email'},
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status) => (
                <Tag color={status === 1 ? 'green' : 'red'}>
                    {status === 1 ? '正常' : '禁用'}
                </Tag>
            )
        },
        {
            title: '操作',
            key: 'action',
            render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined/>} onClick={() => onEdit?.(record)}>
                        编辑
                    </Button>
                    <Button size="small" danger icon={<DeleteOutlined/>} onClick={() => onDelete?.(record)}>
                        删除
                    </Button>
                </Space>
            ),
        },
    ]

    return (
        <Table
            columns={columns}
            dataSource={dataSource}
            loading={loading}
            rowKey="id"
            pagination={{pageSize: 10}}
        />
    )
}
