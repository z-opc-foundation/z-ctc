import {useCallback, useRef, useState} from 'react'
import {ProTable} from '@ant-design/pro-components'
import {Button, Card, Drawer, Form, Input, message, Modal, Popconfirm, Select, Space, Table, Tabs, Tag, Tooltip} from 'antd'
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    ReloadOutlined,
    SearchOutlined,
    StopOutlined,
    UndoOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {ctcAuthorizationApi, metaAppApi} from './api'
import {EmptyState, PageHeader} from '@/common/components/ui'


/**
 * 应用管理 (4A-AUTHZ) — split-panel 布局
 *
 * 左：应用列表（紧凑），每行 [编辑] [停用] [删除]
 * 右：选中应用后整块替换为：
 *   - 顶部：应用基本信息
 *   - 底部：Tabs（API 资产 / 菜单 / 按钮 / 数据），每个 Tab 一个 Table + 新建
 *
 * 数据源（FEATURE016：所有页面不能使用 localStorage 作为存储）：
 *   - 应用 CRUD：z-meta / MetaApplicationController @ /api/meta-app/*
 *   - 资源 CRUD：z-ctc-authz / AuthorizationController @ /api/ctc/authorization/resources/*
 *   - 资源类型枚举（前端 String ↔ 后端 Integer）：
 *       API=1, MENU=2, BUTTON=3, DATA=4
 *
 * 从 z-opc-main-starter-frontend/src/pages/ctc/4a/application/index.tsx 迁入.
 */
const RESOURCE_TYPE_INT = {
    API: 1, MENU: 2, BUTTON: 3, DATA: 4,
}

const TYPE_MAP = {
    API: {text: 'API', color: 'orange', int: 1},
    MENU: {text: '菜单', color: 'blue', int: 2},
    BUTTON: {text: '按钮', color: 'green', int: 3},
    DATA: {text: '数据', color: 'purple', int: 4},
}

const METHOD_COLOR = {
    GET: 'green', POST: 'blue', PUT: 'orange', DELETE: 'red', PATCH: 'purple',
}

const STATUS_MAP = {
    1: {text: '正常', color: 'success'},
    0: {text: '停用', color: 'default'},
}

export default function ApplicationManagement({apiBaseURL}) {
    const actionRef = useRef(undefined)
    const [searchForm] = Form.useForm()
    const [searchParams, setSearchParams] = useState({})

    // --- 应用 CRUD ---
    const [createAppOpen, setCreateAppOpen] = useState(false)
    const [createAppForm] = Form.useForm()

    // --- 选中应用 (右侧抽屉) ---
    const [selectedApp, setSelectedApp] = useState(null)
    const [detailDrawerOpen, setDetailDrawerOpen] = useState(false)
    const [resources, setResources] = useState([])

    // --- 新建资源 Modal ---
    const [createResOpen, setCreateResOpen] = useState(false)
    const [createResForm] = Form.useForm()
    const [createResType, setCreateResType] = useState('API')

    const refreshResources = useCallback(async (appCode) => {
        try {
            const page = await ctcAuthorizationApi.listResourcesByApp({
                appCode,
                pageNum: 1,
                pageSize: 1000,
            })
            setResources(page.data || [])
        } catch (e) {
            message.error('拉取资源失败：' + (e?.message || '未知错误'))
            setResources([])
        }
    }, [])

    const openAppDetail = (app) => {
        setSelectedApp(app)
        setDetailDrawerOpen(true)
        if (app.appCode) {
            refreshResources(app.appCode)
        }
    }

    // ── 搜索 ──
    const handleSearch = useCallback(() => {
        const values = searchForm.getFieldsValue()
        const p = {}
        if (values.appCode) p.appCode = values.appCode
        if (values.appName) p.appName = values.appName
        setSearchParams(p)
        actionRef.current?.reload()
    }, [searchForm])

    const handleResetSearch = useCallback(() => {
        searchForm.resetFields()
        setSearchParams({})
        actionRef.current?.reload()
    }, [searchForm])

    // ── ProTable 数据请求 ──
    const request_ = async (params) => {
        try {
            const pageNum = params?.current || 1
            const pageSize = params?.pageSize || 20
            const result = await metaAppApi.listApplications({
                appCode: searchParams.appCode,
                appName: searchParams.appName,
                pageNum, pageSize,
            })
            return {
                data: result.records || [],
                success: true,
                total: result.total || 0,
            }
        } catch (e) {
            message.error('获取应用列表失败：' + (e?.message || '未知错误'))
            return {data: [], success: false, total: 0}
        }
    }

    // --- 应用操作 ---
    const handleCreateApp = async () => {
        try {
            const values = await createAppForm.validateFields()
            await metaAppApi.create({
                appCode: values.appCode,
                appName: values.appName,
                appType: values.appType || 'web',
                description: values.description,
                status: 1,
            })
            message.success('应用创建成功')
            setCreateAppOpen(false)
            createAppForm.resetFields()
            actionRef.current?.reload()
        } catch (e) {
            if (e?.errorFields) return
            message.error('创建失败：' + (e?.message || '未知错误'))
        }
    }

    const handleDeleteApp = async (app) => {
        try {
            if (app.id == null) {
                message.error('应用 ID 缺失')
                return
            }
            await metaAppApi.delete(app.id)
            message.success(`已删除应用 ${app.appCode}`)
            if (selectedApp?.id === app.id) {
                setSelectedApp(null)
                setResources([])
            }
            actionRef.current?.reload()
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    const handleToggleStatus = async (app) => {
        try {
            if (app.id == null) {
                message.error('应用 ID 缺失')
                return
            }
            const newStatus = app.status === 1 ? 0 : 1
            await metaAppApi.update(app.id, {status: newStatus})
            message.success(newStatus === 1 ? '已启用' : '已停用')
            if (selectedApp?.id === app.id) {
                setSelectedApp({...app, status: newStatus})
            }
            actionRef.current?.reload()
        } catch (e) {
            message.error('切换状态失败：' + (e?.message || '未知错误'))
        }
    }

    // --- 资源操作 ---
    const handleCreateResource = async () => {
        if (!selectedApp) return
        try {
            const values = await createResForm.validateFields()
            const payload = {
                resourceCode: values.resourceCode,
                resourceName: values.resourceName,
                resourceType: RESOURCE_TYPE_INT[createResType],
                appCode: selectedApp.appCode,
                path: values.path,
                method: values.method,
                description: values.description,
                status: 1,
            }
            await ctcAuthorizationApi.createResource(payload)
            message.success('资源创建成功')
            setCreateResOpen(false)
            createResForm.resetFields()
            refreshResources(selectedApp.appCode)
        } catch (e) {
            if (e?.errorFields) return
            message.error('创建失败：' + (e?.message || '未知错误'))
        }
    }

    const handleDeleteResource = async (r) => {
        if (r.id == null) {
            message.error('资源 ID 缺失')
            return
        }
        try {
            await ctcAuthorizationApi.deleteResource(r.id)
            message.success('资源已删除')
            if (selectedApp?.appCode) refreshResources(selectedApp.appCode)
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    const openCreateResourceFor = (type) => {
        setCreateResType(type)
        createResForm.resetFields()
        setCreateResOpen(true)
    }

    const appColumns = [
        {
            title: '应用', dataIndex: 'appName', width: 180, search: false,
            render: (_, r) => {
                const s = STATUS_MAP[r.status ?? 1] || {text: String(r.status ?? '未知'), color: 'default'}
                const active = selectedApp?.id === r.id
                return (
                    <div style={{
                        padding: '4px 0',
                        borderLeft: active ? '3px solid #1677ff' : '3px solid transparent',
                        paddingLeft: 8,
                    }}>
                        <div style={{fontWeight: 500}}>{r.appName}</div>
                        <div style={{fontSize: 12, color: '#999', marginTop: 2}}>
                            <Tag color={s.color} style={{marginRight: 4, marginInlineEnd: 4}}>{s.text}</Tag>
                            <code style={{fontSize: 11}}>{r.appCode}</code>
                        </div>
                    </div>
                )
            },
        },
        {
            title: '操作', valueType: 'option', width: 200, fixed: 'right', search: false,
            render: (_, record) => [
                <Button key="edit" type="link" size="small" icon={<EditOutlined/>}
                        onClick={() => openAppDetail(record)}>
                    编辑
                </Button>,
                <Tooltip key="toggle" title={record.status === 1 ? '停用' : '启用'}>
                    <Button type="text" size="small" danger={record.status === 1} icon={<StopOutlined/>}
                            onClick={() => handleToggleStatus(record)}>
                        {record.status === 1 ? '停用' : '启用'}
                    </Button>
                </Tooltip>,
                <Popconfirm
                    key="del"
                    title={`确认删除应用 ${record.appName}？`}
                    description="该应用下的所有资源会一并删除"
                    okText="删除"
                    okButtonProps={{danger: true}}
                    cancelText="取消"
                    onConfirm={() => handleDeleteApp(record)}
                >
                    <Button type="text" danger size="small" icon={<DeleteOutlined/>}>删除</Button>
                </Popconfirm>,
            ],
        },
    ]

    const resColumns = [
        {title: '资源编码', dataIndex: 'resourceCode', width: 180, ellipsis: true},
        {title: '资源名称', dataIndex: 'resourceName', width: 140, ellipsis: true},
        {
            title: '方法', dataIndex: 'method', width: 80,
            render: (_, r) => r.method ? <Tag color={METHOD_COLOR[r.method]}>{r.method}</Tag> : '-',
        },
        {title: 'URL / Path', dataIndex: 'path', ellipsis: true},
        {title: '描述', dataIndex: 'description', width: 200, ellipsis: true},
        {
            title: '操作', width: 80, fixed: 'right',
            render: (_, r) => (
                <Popconfirm title="确认删除该资源？" okText="删除" okButtonProps={{danger: true}} cancelText="取消"
                            onConfirm={() => handleDeleteResource(r)}>
                    <Button type="text" danger size="small">删除</Button>
                </Popconfirm>
            ),
        },
    ]

    const apiResources = resources.filter(r => (r.resourceType ?? 1) === 1)
    const menuResources = resources.filter(r => r.resourceType === 2)
    const buttonResources = resources.filter(r => r.resourceType === 3)
    const dataResources = resources.filter(r => r.resourceType === 4)

    const renderResTable = (list, type) => (
        <div>
            <div style={{marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                <span style={{color: '#666'}}>
                    共 <b>{list.length}</b> 条 {TYPE_MAP[type].text} 资源
                </span>
                <Button type="primary" size="small" icon={<PlusOutlined/>}
                        onClick={() => openCreateResourceFor(type)}>
                    新建{TYPE_MAP[type].text}
                </Button>
            </div>
            {list.length > 0 ? (
                <Table
                    rowKey="id"
                    columns={resColumns}
                    dataSource={list}
                    pagination={false}
                    size="small"
                    scroll={{x: 800}}
                />
            ) : (
                <EmptyState
                    title={`该应用暂无 ${TYPE_MAP[type].text} 资源`}
                    description="点击上方按钮新增"
                    size="sm"
                />
            )}
        </div>
    )

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 0}}>
            <PageHeader
                title="应用管理"
                subtitle="4A AUTHZ 域 · 应用与资源"
                breadcrumb={[{label: '4A 中心'}, {label: '应用管理'}]}
            />

            <Card size="small" styles={{body: {padding: '12px 16px'}}}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12}}>
                    <Form form={searchForm} layout="inline" size="small">
                        <Form.Item name="appCode" label="应用编码" style={{marginBottom: 8}}>
                            <Input placeholder="搜索" allowClear/>
                        </Form.Item>
                        <Form.Item name="appName" label="应用名称" style={{marginBottom: 8}}>
                            <Input placeholder="搜索" allowClear/>
                        </Form.Item>
                        <Form.Item style={{marginBottom: 8}}>
                            <Space>
                                <Button type="primary" icon={<SearchOutlined/>} onClick={handleSearch}>查询</Button>
                                <Button icon={<UndoOutlined/>} onClick={handleResetSearch}>重置</Button>
                            </Space>
                        </Form.Item>
                    </Form>
                    <Space style={{flexShrink: 0}}>
                        <Button type="primary" icon={<PlusOutlined/>} onClick={() => setCreateAppOpen(true)}>新增应用</Button>
                        <Button icon={<ReloadOutlined/>} onClick={() => {
                            actionRef.current?.reload()
                            if (selectedApp?.appCode) refreshResources(selectedApp.appCode)
                        }}>刷新</Button>
                    </Space>
                </div>
            </Card>

            <ProTable
                actionRef={actionRef}
                rowKey="id"
                columns={appColumns}
                request={request_}
                search={false}
                size="small"
                options={false}
                toolBarRender={false}
                pagination={{pageSize: 10, showSizeChanger: true, showTotal: t => `共 ${t} 条`}}
                onRow={(record) => ({
                    onClick: () => openAppDetail(record),
                    style: {cursor: 'pointer'},
                })}
            />

            <Drawer
                title={selectedApp ? (
                    <div style={{display: 'flex', alignItems: 'center', gap: 12}}>
                        <span style={{fontSize: 16, fontWeight: 600}}>{selectedApp.appName}</span>
                        <Tag color={STATUS_MAP[selectedApp.status ?? 1]?.color || 'default'}>
                            {STATUS_MAP[selectedApp.status ?? 1]?.text || '未知'}
                        </Tag>
                        <code style={{fontSize: 12, color: '#666'}}>{selectedApp.appCode}</code>
                    </div>
                ) : '应用详情'}
                open={detailDrawerOpen}
                onClose={() => setDetailDrawerOpen(false)}
                width="80vw"
                destroyOnHidden
                extra={selectedApp ? (
                    <Space>
                        <Button icon={<ReloadOutlined/>} onClick={() => refreshResources(selectedApp.appCode)}>刷新资源</Button>
                        <Button icon={<EditOutlined/>} onClick={() => setCreateAppOpen(true)}>编辑应用</Button>
                        <Button danger={selectedApp.status === 1} icon={<StopOutlined/>}
                                onClick={() => handleToggleStatus(selectedApp)}>
                            {selectedApp.status === 1 ? '停用' : '启用'}
                        </Button>
                    </Space>
                ) : null}
            >
                {selectedApp && (
                    <div>
                        <div style={{
                            marginBottom: 16,
                            paddingBottom: 12,
                            borderBottom: '1px solid #f0f0f0',
                            fontSize: 13,
                            color: '#666',
                            display: 'flex',
                            gap: 16,
                        }}>
                            {selectedApp.gmtCreate && (
                                <span>创建：{dayjs(selectedApp.gmtCreate).format('YYYY-MM-DD HH:mm:ss')}</span>
                            )}
                            {selectedApp.description && <span>描述：{selectedApp.description}</span>}
                        </div>
                        <Tabs
                            defaultActiveKey="API"
                            items={[
                                {key: 'API', label: <span><Tag color="orange">API</Tag> API 资产 ({apiResources.length})</span>, children: renderResTable(apiResources, 'API')},
                                {key: 'MENU', label: <span><Tag color="blue">菜单</Tag> 菜单 ({menuResources.length})</span>, children: renderResTable(menuResources, 'MENU')},
                                {key: 'BUTTON', label: <span><Tag color="green">按钮</Tag> 按钮 ({buttonResources.length})</span>, children: renderResTable(buttonResources, 'BUTTON')},
                                {key: 'DATA', label: <span><Tag color="purple">数据</Tag> 数据 ({dataResources.length})</span>, children: renderResTable(dataResources, 'DATA')},
                            ]}
                        />
                    </div>
                )}
            </Drawer>



            <Modal title="新建应用" open={createAppOpen} onOk={handleCreateApp}
                   onCancel={() => setCreateAppOpen(false)} size="large" destroyOnHidden>
                <Form form={createAppForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="appCode" label="应用编码" rules={[
                        {required: true},
                        {pattern: /^[a-z][a-z0-9_-]*$/, message: '小写字母开头，仅允许小写字母/数字/下划线/短横'},
                    ]} extra="唯一标识，如 z-ctc / z-task">
                        <Input placeholder="如 z-opc"/>
                    </Form.Item>
                    <Form.Item name="appName" label="应用名称" rules={[{required: true}]}>
                        <Input placeholder="如 统一管理平台"/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} placeholder="可选"/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal title={`新建资源 — ${selectedApp?.appCode || ''} (${TYPE_MAP[createResType].text})`}
                   open={createResOpen}
                   onOk={handleCreateResource} onCancel={() => setCreateResOpen(false)} size="large" destroyOnHidden>
                <Form form={createResForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="resourceCode" label="资源编码" rules={[{required: true}]}
                               extra="唯一标识，如 user:read / menu:dashboard">
                        <Input placeholder="如 user:read"/>
                    </Form.Item>
                    <Form.Item name="resourceName" label="资源名称" rules={[{required: true}]}>
                        <Input placeholder="如 查看用户"/>
                    </Form.Item>
                    {createResType === 'API' && (
                        <>
                            <Form.Item name="method" label="HTTP 方法">
                                <Select allowClear placeholder="选择 HTTP 方法" options={[
                                    {value: 'GET', label: 'GET'},
                                    {value: 'POST', label: 'POST'},
                                    {value: 'PUT', label: 'PUT'},
                                    {value: 'DELETE', label: 'DELETE'},
                                    {value: 'PATCH', label: 'PATCH'},
                                ]}/>
                            </Form.Item>
                            <Form.Item name="path" label="URL Pattern">
                                <Input placeholder="如 /api/users/*"/>
                            </Form.Item>
                        </>
                    )}
                    {createResType === 'MENU' && (
                        <Form.Item name="path" label="菜单路径">
                            <Input placeholder="如 /ctc/dashboard"/>
                        </Form.Item>
                    )}
                    {createResType === 'BUTTON' && (
                        <Form.Item name="resourceCode" label="按钮权限编码" extra="如 btn:user:create">
                            <Input placeholder="如 btn:user:create"/>
                        </Form.Item>
                    )}
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} placeholder="可选"/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}
