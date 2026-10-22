/**
 * 用户管理表格组件 - 可复用
 */
import {Button, Space, Tag} from 'antd'
import {DeleteOutlined, EditOutlined} from '@ant-design/icons'
import {ListTable} from '@yuku123/z-frontend-common/ui'

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

    // 列表页三件套（lead 008 §2：分页 + 查询 + EmptyState）由基座 ListTable 一次带齐，
    // 这里只声明「按哪几个字段过滤」——过滤口径统一收在基座 useListFilter，各仓不再各写各的。
    return (
        <ListTable
            rows={dataSource}
            columns={columns}
            loading={loading}
            rowKey="id"
            searchKeys={['userName', 'nickName', 'phone', 'email']}
            searchPlaceholder="按用户名 / 昵称 / 手机号 / 邮箱过滤"
        />
    )
}
