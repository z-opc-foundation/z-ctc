import {useEffect, useRef, useState} from 'react'
import {ProTable} from '@ant-design/pro-components'
import {Button, Card, DatePicker, Drawer, Form, Input, message, Modal, Popconfirm, Select, Space, Tag, Tree} from 'antd'
import {
    DeleteOutlined,
    DownloadOutlined,
    EditOutlined,
    PlusOutlined,
    ReloadOutlined,
    SafetyOutlined,
    SearchOutlined,
    UndoOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {ctcAuthorizationApi} from './api'
import {PageHeader, SearchInput} from '@/common/components/ui'

/**
 * 角色管理 (4A AUTHZ 域) — 标准数据管理布局
 *
 * 从 z-opc-main-starter-frontend/src/pages/ctc/4a/role/index.tsx 迁入.
 */
const STATUS_MAP = {
    1: {text: '正常', color: 'success'},
    0: {text: '停用', color: 'default'},
}

const RESOURCE_TYPE_MAP = {
    MENU: {text: '菜单', color: 'blue'},
    API: {text: 'API', color: 'orange'},
    BUTTON: {text: '按钮', color: 'green'},
    DATA: {text: '数据', color: 'purple'},
}

const listToTree = (list) => {
    const map = {}
    const roots = []
    list.forEach(item => {
        map[item.id] = {
            key: item.id,
            title: (
                <span style={{display: 'inline-flex', alignItems: 'center', gap: 6}}>
                    {item.resourceName}
                    <Tag color={RESOURCE_TYPE_MAP[item.resourceType || 'MENU']?.color}
                         style={{margin: 0, fontSize: 10}}>
                        {RESOURCE_TYPE_MAP[item.resourceType || 'MENU']?.text}
                    </Tag>
                </span>
            ),
            rawTitle: item.resourceName,
            children: [],
        }
    })
    list.forEach(item => {
        if (roots.findIndex(r => r.key === item.id) === -1) {
            roots.push(map[item.id])
        }
    })
    return roots
}

export default function RoleManagement({apiBaseURL}) {
    const actionRef = useRef(undefined)
    const [searchForm] = Form.useForm()
    const [searchParams, setSearchParams] = useState({})

    const [roles, setRoles] = useState([])
    const [createOpen, setCreateOpen] = useState(false)
    const [editing, setEditing] = useState(null)
    const [form] = Form.useForm()

    const [selectedRowKeys, setSelectedRowKeys] = useState([])
    const [selectedRows, setSelectedRows] = useState([])

    const [grantOpen, setGrantOpen] = useState(false)
    const [grantRole, setGrantRole] = useState(null)
    const [resourceTree, setResourceTree] = useState([])
    const [resourceTreeData, setResourceTreeData] = useState([])
    const [checkedKeys, setCheckedKeys] = useState([])
    const [granting, setGranting] = useState(false)

    const STORAGE_KEY = 'z_authz_role_codes'
    const [knownIds, setKnownIds] = useState(
        () => JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]'),
    )

    const rememberRole = (id, role) => {
        const sid = String(id)
        if (!knownIds.includes(sid)) {
            const next = [...knownIds, sid]
            setKnownIds(next)
            localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
        }
        setRoles(prev => {
            const found = prev.findIndex(r => r.id?.toString() === sid)
            if (found >= 0) {
                const cp = [...prev]
                cp[found] = role
                return cp
            }
            return [...prev, role]
        })
    }

    // ── 数据请求 ──
    const request_ = async () => {
        try {
            let fetched = []
            try {
                fetched = await ctcAuthorizationApi.listAllRoles()
            } catch (e) {
                message.warning('从后端拉取角色失败，已回退到本地缓存：' + (e?.message || '未知错误'))
                for (const id of knownIds) {
                    const cached = roles.find(r => r.id?.toString() === id)
                    if (cached) fetched.push(cached)
                }
                for (const r of roles) {
                    if (!fetched.find(x => x.id?.toString() === r.id?.toString())) {
                        fetched.push(r)
                    }
                }
            }

            // 前端筛选
            const roleCode = searchParams.roleCode
            const status = searchParams.status
            const startDate = searchParams.startDate
            const endDate = searchParams.endDate
            fetched = fetched.filter(r => {
                if (roleCode && !r.roleCode?.includes(roleCode)) return false
                if (status !== undefined && r.status !== status) return false
                if (startDate && r.createdAt && dayjs(r.createdAt).isBefore(startDate, 'day')) return false
                if (endDate && r.createdAt && dayjs(r.createdAt).isAfter(endDate, 'day')) return false
                return true
            })

            return {data: fetched, success: true, total: fetched.length}
        } catch (e) {
            message.error('获取角色失败：' + (e?.message || '未知错误'))
            return {data: [], success: false, total: 0}
        }
    }

    // ── 搜索 ──
    const handleSearch = () => {
        const values = searchForm.getFieldsValue()
        const p = {}
        if (values.roleCode) p.roleCode = values.roleCode
        if (values.status !== undefined) p.status = values.status
        if (values.dateRange) {
            p.startDate = values.dateRange[0].format('YYYY-MM-DD')
            p.endDate = values.dateRange[1].format('YYYY-MM-DD')
        }
        setSearchParams(p)
        actionRef.current?.reload()
    }

    const handleResetSearch = () => {
        searchForm.resetFields()
        setSearchParams({})
        actionRef.current?.reload()
    }

    const handleCreate = async () => {
        try {
            const values = await form.validateFields()
            const id = await ctcAuthorizationApi.createRole({...values, status: 1})
            message.success(`角色创建成功，ID=${id}`)
            rememberRole(id, {id, ...values, status: 1})
            setCreateOpen(false)
            form.resetFields()
            actionRef.current?.reload()
        } catch (e) {
            if (e?.errorFields) return
            message.error('创建失败：' + (e?.message || '未知错误'))
        }
    }

    const openEdit = (r) => {
        setEditing(r)
        form.setFieldsValue({roleCode: r.roleCode, roleName: r.roleName, description: r.description})
    }

    const handleEditOk = async () => {
        if (!editing?.id) return
        try {
            const values = await form.validateFields()
            message.warning('后端暂未提供 role 更新接口，本地更新占位')
            const updated = {...editing, ...values}
            rememberRole(editing.id, updated)
            form.resetFields()
            setEditing(null)
            actionRef.current?.reload()
        } catch (e) {
            if (e?.errorFields) return
            message.error('更新失败：' + (e?.message || '未知错误'))
        }
    }

    const handleDelete = (r) => {
        message.warning('后端暂未提供 role 删除接口')
        setRoles(prev => prev.filter(x => x.id?.toString() !== r.id?.toString()))
        setKnownIds(prev => prev.filter(x => x !== r.id?.toString()))
        localStorage.setItem(STORAGE_KEY, JSON.stringify(knownIds.filter(x => x !== r.id?.toString())))
        actionRef.current?.reload()
    }

    const handleBatchDelete = () => {
        message.warning('后端暂未提供 role 删除接口')
        selectedRows.forEach(r => {
            setRoles(prev => prev.filter(x => x.id?.toString() !== r.id?.toString()))
            setKnownIds(prev => prev.filter(x => x !== r.id?.toString()))
        })
        localStorage.setItem(STORAGE_KEY, JSON.stringify(knownIds.filter(x =>
            !selectedRows.find(r => r.id?.toString() === x)
        )))
        setSelectedRowKeys([])
        setSelectedRows([])
        actionRef.current?.reload()
    }

    // ── 导出 ──
    const handleExport = () => {
        const rows = selectedRows.length > 0 ? selectedRows : roles
        if (rows.length === 0) {
            message.warning('暂无数据可导出')
            return
        }
        const header = 'ID,角色编码,角色名称,描述,租户,状态,创建时间\n'
        const csv = header + rows.map(r =>
            [r.id, r.roleCode, r.roleName, r.description, r.tenantCode, (STATUS_MAP[r.status ?? 1] || {text: String(r.status)}).text, r.createdAt].join(',')
        ).join('\n')
        const blob = new Blob(['\uFEFF' + csv], {type: 'text/csv;charset=utf-8'})
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `roles_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
        a.click()
        URL.revokeObjectURL(url)
        message.success(`已导出 ${rows.length} 条数据`)
    }

    // 加载资源树 (mock, 等后端 /api/ctc/authorization/resources/list 实现后替换)
    const fetchResourceTree = async () => {
        try {
            const mockResources = [
                {id: 1, resourceCode: 'user:read', resourceName: '查看用户', resourceType: 'API'},
                {id: 2, resourceCode: 'user:write', resourceName: '编辑用户', resourceType: 'API'},
                {id: 3, resourceCode: 'role:read', resourceName: '查看角色', resourceType: 'API'},
                {id: 4, resourceCode: 'role:write', resourceName: '编辑角色', resourceType: 'API'},
                {id: 5, resourceCode: 'menu:dashboard', resourceName: '工作台', resourceType: 'MENU'},
                {id: 6, resourceCode: 'menu:user', resourceName: '用户管理', resourceType: 'MENU'},
                {id: 7, resourceCode: 'menu:role', resourceName: '角色管理', resourceType: 'MENU'},
            ]
            setResourceTree(mockResources)
            setResourceTreeData(listToTree(mockResources))
        } catch (e) {
            message.error('加载资源失败：' + (e?.message || '未知错误'))
        }
    }

    const openGrant = async (r) => {
        setGrantRole(r)
        setCheckedKeys([])
        setGrantOpen(true)
        await fetchResourceTree()
    }

    const handleGrantSave = async () => {
        if (!grantRole?.id) return
        setGranting(true)
        try {
            for (const key of checkedKeys) {
                await ctcAuthorizationApi.grantRoleResource(grantRole.id, Number(key))
            }
            message.success(`已为角色 ${grantRole.roleName} 授权 ${checkedKeys.length} 个资源`)
            setGrantOpen(false)
        } catch (e) {
            message.error('授权失败：' + (e?.message || '未知错误'))
        } finally {
            setGranting(false)
        }
    }

    const columns = [
        {title: 'ID', dataIndex: 'id', width: 60, search: false},
        {title: '角色编码', dataIndex: 'roleCode', width: 140, search: false},
        {title: '角色名称', dataIndex: 'roleName', width: 140, search: false},
        {title: '描述', dataIndex: 'description', width: 220, ellipsis: true, search: false},
        {
            title: '状态', dataIndex: 'status', width: 80, search: false,
            render: (_, r) => {
                const s = STATUS_MAP[r.status ?? 1] || {text: String(r.status ?? '未知'), color: 'default'}
                return <Tag color={s.color}>{s.text}</Tag>
            },
        },
        {title: '租户', dataIndex: 'tenantCode', width: 100, search: false},
        {title: '创建时间', dataIndex: 'createdAt', width: 170, valueType: 'dateTime', search: false},
        {
            title: '操作', valueType: 'option', width: 240, fixed: 'right', search: false,
            render: (_, record) => [
                <Button key="edit" type="text" size="small" icon={<EditOutlined/>}
                        onClick={() => openEdit(record)}>编辑</Button>,
                <Button key="grant" type="text" size="small" icon={<SafetyOutlined/>}
                        onClick={() => openGrant(record)}>授权</Button>,
                <Popconfirm key="delete" title={`删除角色 ${record.roleName}？`} onConfirm={() => handleDelete(record)}>
                    <Button type="text" danger size="small" icon={<DeleteOutlined/>}>删除</Button>
                </Popconfirm>,
            ],
        },
    ]

    useEffect(() => {
        actionRef.current?.reload()
    }, [knownIds])

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 12}}>
            <PageHeader
                title="角色管理"
                subtitle="4A AUTHZ 域 · 角色创建、权限授权与生命周期管理"
                breadcrumb={[{label: '4A 中心'}, {label: '角色管理'}]}
            />

            <Card size="small" styles={{body: {padding: '12px 16px'}}}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12}}>
                <Form form={searchForm} layout="inline">
                    <Form.Item name="roleCode" label="角色编码">
                        <SearchInput
                            placeholder="请输入角色编码"
                            size="large"
                            onChange={(v) => searchForm.setFieldValue('roleCode', v)}
                        />
                    </Form.Item>
                    <Form.Item name="status" label="状态">
                        <Select placeholder="全部" allowClear style={{width: 120}}
                                options={[{value: 1, label: '正常'}, {value: 0, label: '停用'}]}/>
                    </Form.Item>
                    <Form.Item name="dateRange" label="创建时间">
                        <DatePicker.RangePicker style={{width: 240}}/>
                    </Form.Item>
                    <Form.Item>
                        <Space>
                            <Button type="primary" icon={<SearchOutlined/>} onClick={handleSearch}>查询</Button>
                            <Button icon={<UndoOutlined/>} onClick={handleResetSearch}>重置</Button>
                        </Space>
                    </Form.Item>
                </Form>
                <Space style={{flexShrink: 0}}>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={() => setCreateOpen(true)}>新增</Button>
                    <Button danger icon={<DeleteOutlined/>} disabled={selectedRowKeys.length === 0}
                            onClick={handleBatchDelete}>批量删除</Button>
                    <Button icon={<DownloadOutlined/>} onClick={handleExport}>导出</Button>
                    <Button icon={<ReloadOutlined/>} onClick={() => actionRef.current?.reload()}>刷新</Button>
                </Space>
                </div>
            </Card>

            <ProTable
                actionRef={actionRef}
                rowKey="id"
                columns={columns}
                request={request_}
                params={searchParams}
                search={false}
                size="small"
                options={false}
                toolBarRender={false}
                pagination={false}
                rowSelection={{
                    selectedRowKeys,
                    onChange: (keys, rows) => {
                        setSelectedRowKeys(keys)
                        setSelectedRows(rows)
                    },
                }}
                tableAlertRender={({selectedRowKeys: keys}) => (
                    <span>已选择 <a style={{fontWeight: 600}}>{keys.length}</a> 项</span>
                )}
            />

            <Modal title="新建角色" open={createOpen} onOk={handleCreate}
                   onCancel={() => setCreateOpen(false)} size="large" destroyOnHidden>
                <Form form={form} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="roleCode" label="角色编码" rules={[{required: true}]}
                               extra="唯一标识，建议英文下划线">
                        <Input placeholder="如 admin / dev / viewer"/>
                    </Form.Item>
                    <Form.Item name="roleName" label="角色名称" rules={[{required: true}]}>
                        <Input placeholder="如 管理员"/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} placeholder="可选"/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal title="编辑角色" open={!!editing} onOk={handleEditOk}
                   onCancel={() => setEditing(null)} size="large" destroyOnHidden>
                <Form form={form} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="roleCode" label="角色编码" rules={[{required: true}]}>
                        <Input disabled={!!editing}/>
                    </Form.Item>
                    <Form.Item name="roleName" label="角色名称" rules={[{required: true}]}>
                        <Input/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2}/>
                    </Form.Item>
                </Form>
            </Modal>

            <Drawer
                title={grantRole ? `为角色授权 — ${grantRole.roleName}` : '授权'}
                open={grantOpen}
                onClose={() => setGrantOpen(false)}
                size={520}
                extra={
                    <Space>
                        <Button onClick={() => setGrantOpen(false)}>取消</Button>
                        <Button type="primary" loading={granting} onClick={handleGrantSave}>
                            保存 ({checkedKeys.length})
                        </Button>
                    </Space>
                }
            >
                <div style={{color: '#666', fontSize: 12, marginBottom: 12}}>
                    勾选授予该角色的资源。注：后端 /api/ctc/authorization/resources/list 端点尚未实现，
                    当前展示为 mock 数据；生产环境替换 fetchResourceTree()。
                </div>
                <Tree
                    checkable
                    treeData={resourceTreeData}
                    checkedKeys={checkedKeys}
                    onCheck={keys => setCheckedKeys(keys)}
                    blockNode
                    height={520}
                />
            </Drawer>
        </div>
    )
}
