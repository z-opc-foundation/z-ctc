import {useCallback, useEffect, useRef, useState} from 'react'
import {ProTable} from '@ant-design/pro-components'
import {
    Button,
    Card,
    Descriptions,
    Drawer,
    Form,
    Input,
    InputNumber,
    Menu,
    message,
    Modal,
    Popconfirm,
    Select,
    Space,
    Table,
    Tag,
    Tooltip
} from 'antd'
import {
    DeleteOutlined,
    EditOutlined,
    FileTextOutlined,
    InfoCircleOutlined,
    PlusOutlined,
    ReloadOutlined,
    SearchOutlined,
    TeamOutlined,
    UndoOutlined,
} from '@ant-design/icons'
import {ctcAuthorizationApi} from './api'
import {PageHeader} from '@/common/components/ui'

/**
 * 权限管理 (FEATURE015) — 按"应用维度"管理权限点（资源 CRUD）
 *
 * 从 z-opc-main-starter-frontend/src/pages/ctc/4a/permission/index.tsx 迁入.
 */
const TYPE_MAP = {
    1: {text: '菜单', color: 'blue'},
    2: {text: '数据', color: 'purple'},
    3: {text: 'API', color: 'orange'},
    4: {text: '按钮', color: 'green'},
}

const METHOD_OPTIONS = [
    {value: '', label: '任意'},
    {value: 'GET', label: 'GET'},
    {value: 'POST', label: 'POST'},
    {value: 'PUT', label: 'PUT'},
    {value: 'DELETE', label: 'DELETE'},
    {value: 'PATCH', label: 'PATCH'},
]

export default function PermissionManagement({apiBaseURL}) {
    const actionRef = useRef(undefined)
    const [searchForm] = Form.useForm()
    const [searchParams, setSearchParams] = useState({})

    const [selectedRowKeys, setSelectedRowKeys] = useState([])
    const [selectedRows, setSelectedRows] = useState([])

    const [appOptions, setAppOptions] = useState([])

    const [formOpen, setFormOpen] = useState(false)
    const [editingTarget, setEditingTarget] = useState(null)
    const [form] = Form.useForm()

    const [detailOpen, setDetailOpen] = useState(false)
    const [detailRecord, setDetailRecord] = useState(null)
    const [detailTab, setDetailTab] = useState('info')
    const [detailRoles, setDetailRoles] = useState([])
    const [detailRolesLoading, setDetailRolesLoading] = useState(false)

    const loadAppOptions = useCallback(async () => {
        // 来源: AuthzController.distinctApps() — 后端资源表中实际注册过的 appCode 集合
        // 与 metaApp.listApplications 列出"中台管理的所有应用"相比, 这里只显示有权限点落库的应用,
        // 避免出现"选了应用但列表为空"的体验问题.
        try {
            const res = await ctcAuthorizationApi.distinctApps()
            const codes = Array.isArray(res) ? res : []
            setAppOptions(codes.map((code) => ({value: code, label: code})))
        } catch {
            setAppOptions([])
        }
    }, [])

    useEffect(() => {
        loadAppOptions()
    }, [loadAppOptions])

    // ── 搜索 ──
    const handleSearch = useCallback(() => {
        const v = searchForm.getFieldsValue()
        const p = {}
        if (v.appCode) p.appCode = v.appCode
        if (v.type !== undefined && v.type !== null) p.type = v.type
        if (v.keyword) p.keyword = v.keyword
        if (v.status !== undefined && v.status !== null) p.status = v.status
        setSearchParams(p)
        actionRef.current?.reload()
    }, [searchForm])

    const handleReset = useCallback(() => {
        searchForm.resetFields()
        setSearchParams({})
        actionRef.current?.reload()
    }, [searchForm])

    // ── 数据请求 ──
    const request_ = async (params) => {
        try {
            const res = await ctcAuthorizationApi.listResourcesByApp({
                appCode: searchParams.appCode,
                type: searchParams.type,
                keyword: searchParams.keyword,
                pageNum: params.current,
                pageSize: params.pageSize,
            })
            const records = (res?.data || [])
            const total = (res?.total ?? records.length)
            return {data: records, success: true, total}
        } catch (e) {
            message.error('查询权限点失败：' + (e?.message || '未知错误'))
            return {data: [], success: false, total: 0}
        }
    }

    // ── 新建 ──
    const handleCreate = async () => {
        try {
            const values = await form.validateFields()
            await ctcAuthorizationApi.createResource(values)
            message.success('创建成功')
            setFormOpen(false)
            form.resetFields()
            actionRef.current?.reload()
            loadAppOptions()
        } catch (e) {
            if (e?.errorFields) return
            message.error('创建失败：' + (e?.message || '未知错误'))
        }
    }

    // ── 编辑 ──
    const openEdit = (r) => {
        setEditingTarget(r)
        form.setFieldsValue(r)
        setFormOpen(true)
    }

    const handleUpdate = async () => {
        if (!editingTarget?.id) return
        try {
            const values = await form.validateFields()
            await ctcAuthorizationApi.updateResource(editingTarget.id, values)
            message.success('更新成功')
            setFormOpen(false)
            setEditingTarget(null)
            form.resetFields()
            actionRef.current?.reload()
            loadAppOptions()
        } catch (e) {
            if (e?.errorFields) return
            message.error('更新失败：' + (e?.message || '未知错误'))
        }
    }

    // ── 删除 ──
    const handleDelete = async (id) => {
        try {
            await ctcAuthorizationApi.deleteResource(id)
            message.success('已删除')
            actionRef.current?.reload()
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    const handleBatchDelete = async () => {
        if (selectedRows.length === 0) return
        try {
            await Promise.all(selectedRows.map(r => ctcAuthorizationApi.deleteResource(r.id)))
            message.success(`已删除 ${selectedRows.length} 条`)
            setSelectedRowKeys([])
            setSelectedRows([])
            actionRef.current?.reload()
        } catch (e) {
            message.error('批量删除失败：' + (e?.message || '未知错误'))
        }
    }

    // ── 详情抽屉 ──
    const fetchResourceRoles = async (resourceId) => {
        if (!resourceId) {
            setDetailRoles([])
            return
        }
        setDetailRolesLoading(true)
        try {
            const res = await ctcAuthorizationApi.listResourceRoles(resourceId)
            setDetailRoles(res || [])
        } catch {
            setDetailRoles([])
        } finally {
            setDetailRolesLoading(false)
        }
    }

    const openDetail = (r) => {
        setDetailRecord(r)
        setDetailTab('info')
        setDetailOpen(true)
        fetchResourceRoles(r.id)
    }

    const closeDetail = () => {
        setDetailOpen(false)
        setDetailRecord(null)
        setDetailRoles([])
    }

    const handleTabChange = (key) => {
        setDetailTab(key)
        if (key === 'roles' && detailRecord?.id) {
            fetchResourceRoles(detailRecord.id)
        }
    }

    // ── 表格列 ──
    const columns = [
        {title: 'ID', dataIndex: 'id', width: 60, search: false},
        {
            title: '编码', dataIndex: 'resourceCode', width: 200, fixed: 'left', search: false,
            render: (_, r) => <Tooltip title={r.resourceCode}><span
                style={{fontFamily: 'monospace'}}>{r.resourceCode}</span></Tooltip>
        },
        {title: '名称', dataIndex: 'resourceName', width: 160, search: false},
        {
            title: '类型', dataIndex: 'resourceType', width: 80, search: false,
            render: (_, r) => {
                const t = TYPE_MAP[r.resourceType ?? 3] || {text: String(r.resourceType ?? '?'), color: 'default'}
                return <Tag color={t.color}>{t.text}</Tag>
            },
        },
        {
            title: '应用', dataIndex: 'appCode', width: 140, search: false,
            render: (_, r) => r.appCode
                ? <Tag color="cyan">{r.appCode}</Tag>
                : <Tag color="default">公共</Tag>,
        },
        {title: '路径', dataIndex: 'path', width: 220, ellipsis: true, search: false},
        {
            title: '方法', dataIndex: 'method', width: 80, search: false,
            render: (_, r) => r.method ? <Tag>{r.method}</Tag> : <span style={{color: '#bbb'}}>-</span>
        },
        {
            title: '状态', dataIndex: 'status', width: 80, search: false,
            render: (_, r) => <Tag color={r.status === 1 ? 'success' : 'default'}>
                {r.status === 1 ? '启用' : '禁用'}
            </Tag>,
        },
        {title: '创建时间', dataIndex: 'createdAt', width: 170, valueType: 'dateTime', search: false},
        {
            title: '操作', valueType: 'option', width: 200, fixed: 'right', search: false,
            render: (_, record) => [
                <Tooltip key="edit" title="编辑">
                    <Button type="text" size="small" icon={<EditOutlined/>} onClick={(e) => {
                        e.stopPropagation()
                        openEdit(record)
                    }}>编辑</Button>
                </Tooltip>,
                <Popconfirm key="delete" title="确认删除该权限点？"
                            okText="删除" cancelText="取消" okButtonProps={{danger: true}}
                            onConfirm={(e) => {
                                e?.stopPropagation?.();
                                handleDelete(record.id)
                            }}>
                    <Button type="text" size="small" danger icon={<DeleteOutlined/>}
                            onClick={(e) => e.stopPropagation()}>删除</Button>
                </Popconfirm>,
                <Tooltip key="detail" title="详情 / 关联角色">
                    <Button type="text" size="small" icon={<FileTextOutlined/>}
                            onClick={(e) => {
                                e.stopPropagation();
                                openDetail(record)
                            }}>详情</Button>
                </Tooltip>,
            ],
        },
    ]

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 0}}>
            <PageHeader
                title="权限管理"
                subtitle="4A AUTHZ 域 · 按应用维度管理权限点（资源 CRUD）"
                breadcrumb={[{label: '4A 中心'}, {label: '权限管理'}]}
            />

            <Card size="small" style={{marginBottom: 16}} styles={{body: {padding: '12px 16px'}}}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12}}>
                <Form form={searchForm} layout="inline">
                    <Form.Item name="appCode" label="应用">
                        <Select placeholder="全部应用" allowClear style={{width: 200}}
                                options={appOptions}
                                showSearch optionFilterProp="label"/>
                    </Form.Item>
                    <Form.Item name="type" label="类型">
                        <Select placeholder="全部类型" allowClear style={{width: 120}}
                                options={Object.entries(TYPE_MAP).map(([k, v]) => ({
                                    value: Number(k),
                                    label: v.text
                                }))}/>
                    </Form.Item>
                    <Form.Item name="keyword" label="关键字">
                        <Input placeholder="编码 / 名称" allowClear style={{width: 200}}/>
                    </Form.Item>
                    <Form.Item name="status" label="状态">
                        <Select placeholder="全部" allowClear style={{width: 100}}
                                options={[{value: 1, label: '启用'}, {value: 0, label: '禁用'}]}/>
                    </Form.Item>
                    <Form.Item>
                        <Space>
                            <Button type="primary" icon={<SearchOutlined/>} onClick={handleSearch}>查询</Button>
                            <Button icon={<UndoOutlined/>} onClick={handleReset}>重置</Button>
                        </Space>
                    </Form.Item>
                </Form>
                <Space style={{flexShrink: 0}}>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={() => {
                        setEditingTarget(null)
                        form.resetFields()
                        setFormOpen(true)
                    }}>新增</Button>
                    <Button danger icon={<DeleteOutlined/>} disabled={selectedRowKeys.length === 0}
                            onClick={handleBatchDelete}>批量删除</Button>
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
                pagination={{pageSize: 10, showSizeChanger: true, showTotal: t => `共 ${t} 条`}}
                dateFormatter="string"
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
                onRow={(r) => ({
                    onClick: () => openDetail(r),
                    style: {cursor: 'pointer'},
                })}
            />

            <Modal
                title={editingTarget ? `编辑权限点 — ${editingTarget.resourceCode}` : '新建权限点'}
                open={formOpen}
                onOk={editingTarget ? handleUpdate : handleCreate}
                onCancel={() => {
                    setFormOpen(false);
                    setEditingTarget(null);
                    form.resetFields()
                }}
                size="large"
                destroyOnHidden
                okText={editingTarget ? '保存' : '创建'}
            >
                <Form form={form} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="resourceCode" label="资源编码"
                               rules={[{
                                   required: true,
                                   min: 2,
                                   max: 128,
                                   pattern: /^[a-zA-Z0-9_:.-]+$/,
                                   message: '2-128 位字母数字下划线冒号'
                               }]}
                               extra="业务唯一，如 user:read / order:create">
                        <Input placeholder="如 user:read" disabled={!!editingTarget}/>
                    </Form.Item>
                    <Form.Item name="resourceName" label="资源名称" rules={[{required: true, max: 128}]}>
                        <Input placeholder="如 用户查询"/>
                    </Form.Item>
                    <Form.Item name="resourceType" label="资源类型" rules={[{required: true}]} initialValue={3}>
                        <Select
                            options={Object.entries(TYPE_MAP).map(([k, v]) => ({value: Number(k), label: v.text}))}/>
                    </Form.Item>
                    <Form.Item name="appCode" label="所属应用" extra="选择权限点归属的应用；不选则为「公共」">
                        <Select placeholder="公共（跨应用）" allowClear showSearch
                                optionFilterProp="label"
                                options={appOptions}/>
                    </Form.Item>
                    <Form.Item name="path" label="URL 路径" extra="菜单 / API 资源对应路径">
                        <Input placeholder="如 /api/ctc/authorization/users"/>
                    </Form.Item>
                    <Form.Item name="method" label="HTTP 方法" extra="API 资源填 HTTP 方法；菜单/数据/按钮可留空">
                        <Select options={METHOD_OPTIONS}/>
                    </Form.Item>
                    <Form.Item name="icon" label="图标 (菜单用)">
                        <Input placeholder="如 UserOutlined"/>
                    </Form.Item>
                    <Form.Item name="sortOrder" label="排序" initialValue={0}>
                        <InputNumber min={0} max={9999} style={{width: '100%'}}/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} maxLength={512} showCount/>
                    </Form.Item>
                    <Form.Item name="status" label="状态" initialValue={1}>
                        <Select options={[{value: 1, label: '启用'}, {value: 0, label: '禁用'}]}/>
                    </Form.Item>
                </Form>
            </Modal>

            <Drawer
                title={detailRecord ? `权限点详情 — ${detailRecord.resourceCode}` : '权限点详情'}
                open={detailOpen}
                onClose={closeDetail}
                size={900}
                destroyOnHidden
            >
                {detailRecord && (
                    <div style={{display: 'flex', gap: 16, minHeight: 480}}>
                        <div style={{width: 160, borderRight: '1px solid #f0f0f0', paddingRight: 8}}>
                            <Menu
                                mode="inline"
                                selectedKeys={[detailTab]}
                                onClick={({key}) => handleTabChange(key)}
                                style={{border: 'none', background: 'transparent'}}
                                items={[
                                    {key: 'info', icon: <InfoCircleOutlined/>, label: '基本信息'},
                                    {key: 'roles', icon: <TeamOutlined/>, label: `关联角色 (${detailRoles.length})`},
                                ]}
                            />
                        </div>

                        <div style={{flex: 1, minWidth: 0}}>
                            {detailTab === 'info' ? (
                                <Descriptions title="权限点信息" size="small" bordered column={2}>
                                    <Descriptions.Item label="资源ID">{detailRecord.id}</Descriptions.Item>
                                    <Descriptions.Item label="资源编码">
                                        <span style={{fontFamily: 'monospace'}}>{detailRecord.resourceCode}</span>
                                    </Descriptions.Item>
                                    <Descriptions.Item label="资源名称">{detailRecord.resourceName}</Descriptions.Item>
                                    <Descriptions.Item label="资源类型">
                                        <Tag color={TYPE_MAP[detailRecord.resourceType ?? 3]?.color}>
                                            {TYPE_MAP[detailRecord.resourceType ?? 3]?.text || detailRecord.resourceType}
                                        </Tag>
                                    </Descriptions.Item>
                                    <Descriptions.Item label="所属应用" span={2}>
                                        {detailRecord.appCode
                                            ? <Tag color="cyan">{detailRecord.appCode}</Tag>
                                            : <Tag color="default">公共</Tag>}
                                    </Descriptions.Item>
                                    <Descriptions.Item label="URL 路径" span={2}>
                                        {detailRecord.path || '-'}
                                    </Descriptions.Item>
                                    <Descriptions.Item label="HTTP 方法">
                                        {detailRecord.method ? <Tag>{detailRecord.method}</Tag> : '-'}
                                    </Descriptions.Item>
                                    <Descriptions.Item label="图标">{detailRecord.icon || '-'}</Descriptions.Item>
                                    <Descriptions.Item label="排序">{detailRecord.sortOrder ?? 0}</Descriptions.Item>
                                    <Descriptions.Item label="状态">
                                        <Tag color={detailRecord.status === 1 ? 'success' : 'default'}>
                                            {detailRecord.status === 1 ? '启用' : '禁用'}
                                        </Tag>
                                    </Descriptions.Item>
                                    <Descriptions.Item label="租户"
                                                       span={2}>{detailRecord.tenantCode || '公共'}</Descriptions.Item>
                                    <Descriptions.Item label="描述"
                                                       span={2}>{detailRecord.description || '-'}</Descriptions.Item>
                                    <Descriptions.Item
                                        label="创建时间">{detailRecord.createdAt || '-'}</Descriptions.Item>
                                    <Descriptions.Item
                                        label="更新时间">{detailRecord.updatedAt || '-'}</Descriptions.Item>
                                </Descriptions>
                            ) : (
                                <Table
                                    rowKey="id"
                                    loading={detailRolesLoading}
                                    dataSource={detailRoles}
                                    pagination={{pageSize: 10, size: 'small'}}
                                    size="small"
                                    columns={[
                                        {title: '角色ID', dataIndex: 'id', width: 80},
                                        {title: '角色编码', dataIndex: 'roleCode', width: 160},
                                        {title: '角色名称', dataIndex: 'roleName', width: 160},
                                        {
                                            title: '状态', dataIndex: 'status', width: 80,
                                            render: (_, r) => <Tag color={r.status === 1 ? 'success' : 'default'}>
                                                {r.status === 1 ? '正常' : '停用'}
                                            </Tag>,
                                        },
                                        {title: '租户', dataIndex: 'tenantCode', width: 100},
                                        {title: '描述', dataIndex: 'description', ellipsis: true},
                                    ]}
                                    locale={{emptyText: '该权限点暂未被任何角色绑定'}}
                                />
                            )}
                        </div>
                    </div>
                )}
            </Drawer>
        </div>
    )
}
