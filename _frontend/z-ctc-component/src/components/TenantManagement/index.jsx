import {useCallback, useEffect, useState} from 'react'
import {
    Button,
    Card,
    Descriptions,
    Empty,
    Form,
    Input,
    message,
    Modal,
    Popconfirm,
    Space,
    Table,
    Tag,
    Tooltip,
    Tree,
} from 'antd'
import {DeleteOutlined, EditOutlined, InfoCircleOutlined, PlusOutlined, ReloadOutlined,} from '@ant-design/icons'
import dayjs from 'dayjs'
import {configureApiBaseURL, ctcAcDomainApi, ctcAcTenantApi} from './api'
import {PageHeader} from '@/common/components/ui'

/**
 * 租户管理 (4A AC 域) — 左树右表布局
 *
 * ┌──────────────┬───────────────────────────────────────────┐
 * │ 租户 / 域    │ 选中节点 → 基本信息（Descriptions）       │
 * │ ▼ 默认租户   │ + 扩展配置 (K-V) 表格                      │
 * │   · 默认域   │                                           │
 * │ ▼ tenant_a  │                                           │
 * │   · 默认域   │                                           │
 * └──────────────┴───────────────────────────────────────────┘
 *
 * 后端契约（FEATURE016：所有页面不能使用 localStorage 作为存储）：
 *   - 租户 CRUD：z-ctc-ac / TenantController @ /api/ctc/ac/tenants/*
 *   - 域 CRUD：z-ctc-ac / DomainController @ /api/ctc/ac/domains/*
 *   - 扩展配置 K-V：当前纯前端内存态 — 待后端 ExtConfigController 上线后接入
 *
 * Props:
 *   - apiBaseURL?: string    后端 baseURL, 默认 '/api'
 *
 * 修复 (从 z-opc-main-starter-frontend 迁过来时应用):
 *   1. domainMap[t.tenantCode] 加 Array.isArray 防御, 避免后端返回非数组时 .map 报错
 *   2. titleRender: 移除外层 span 的 stopPropagation, 让 AntD Tree onSelect 能响应点击
 *   3. handleDomainCreate: 强制 setExpandedKeys, 让新加的域在树中可见
 */

const STATUS_MAP = {
    1: {text: '正常', color: 'success'},
    0: {text: '停用', color: 'default'},
}

const nodeKey = (type, tenantCode, domainCode) =>
    type === 'tenant' ? `tenant:${tenantCode}` : `domain:${tenantCode}:${domainCode}`

export default function TenantManagement({apiBaseURL}) {
    // 配置 baseURL (组件 mount 时生效)
    useEffect(() => {
        if (apiBaseURL) configureApiBaseURL(apiBaseURL)
    }, [apiBaseURL])

    // ── 租户列表（后端） ──
    const [tenants, setTenants] = useState([])
    const [loadingTenants, setLoadingTenants] = useState(false)

    // ── 域：从后端拉取 ──
    const [domainMap, setDomainMap] = useState({})
    const [extConfigMap, setExtConfigMap] = useState({})

    // ── 树 ──
    const [treeData, setTreeData] = useState([])
    const [selectedKey, setSelectedKey] = useState([])
    const [expandedKeys, setExpandedKeys] = useState([])
    const [selectedNode, setSelectedNode] = useState(null)
    const [configData, setConfigData] = useState([])

    // ── 新建租户 ──
    const [createOpen, setCreateOpen] = useState(false)
    const [createForm] = Form.useForm()

    // ── 新建域（在某个租户条目上加 + 触发） ──
    const [domainCreateOpen, setDomainCreateOpen] = useState(false)
    const [domainCreateParent, setDomainCreateParent] = useState(null)
    const [domainForm] = Form.useForm()

    // ── K-V 编辑 ──
    const [kvModalOpen, setKvModalOpen] = useState(false)
    const [editingKv, setEditingKv] = useState(null)
    const [kvForm] = Form.useForm()

    // 拉取所有租户 — 走后端
    const fetchTenants = useCallback(async () => {
        setLoadingTenants(true)
        try {
            const all = await ctcAcTenantApi.listAll()
            setTenants(Array.isArray(all) ? all : [])
        } catch (e) {
            message.error('获取租户列表失败：' + (e?.message || '未知错误'))
            setTenants([])
        } finally {
            setLoadingTenants(false)
        }
    }, [])

    useEffect(() => {
        fetchTenants()
    }, [fetchTenants])

    // 拉取每个租户下的域（后端）
    const fetchDomainsForTenants = useCallback(async (tenantList) => {
        const map = {}
        for (const t of tenantList) {
            try {
                const list = await ctcAcDomainApi.listByTenant(t.tenantCode)
                map[t.tenantCode] = Array.isArray(list) ? list : []
            } catch {
                map[t.tenantCode] = []
            }
        }
        setDomainMap(map)
    }, [])

    // 租户加载完成后同步拉域
    useEffect(() => {
        if (tenants.length > 0) {
            fetchDomainsForTenants(tenants)
        }
    }, [tenants, fetchDomainsForTenants])

    // 构建树
    useEffect(() => {
        const tree = tenants.map(t => ({
            key: nodeKey('tenant', t.tenantCode),
            title: (
                <span style={{display: 'inline-flex', alignItems: 'center', gap: 6}}>
                    {t.tenantName || t.tenantCode}
                    <Tag color={STATUS_MAP[t.status ?? 1]?.color} style={{margin: 0, fontSize: 10}}>
                        {STATUS_MAP[t.status ?? 1]?.text}
                    </Tag>
                </span>
            ),
            rawTitle: `${t.tenantName || t.tenantCode} (${t.tenantCode})`,
            data: {type: 'tenant', data: t},
            // 🆕 修复 1: Array.isArray 防御 — 避免后端返回非数组时 .map 报错
            children: (Array.isArray(domainMap[t.tenantCode]) ? domainMap[t.tenantCode] : []).map(d => ({
                key: nodeKey('domain', t.tenantCode, d.domainCode),
                title: (
                    <span style={{display: 'inline-flex', alignItems: 'center', gap: 6}}>
                        {d.domainName}
                        <Tag color={STATUS_MAP[d.status]?.color} style={{margin: 0, fontSize: 10}}>
                            {STATUS_MAP[d.status]?.text}
                        </Tag>
                    </span>
                ),
                rawTitle: `${d.domainName} (${d.domainCode})`,
                data: {type: 'domain', data: d},
                isLeaf: true,
            })),
        }))
        setTreeData(tree)
    }, [tenants, domainMap])

    // 删除域 — 走后端
    const handleDeleteDomain = async (tenantCode, domain) => {
        const isDefault = domain.domainCode === `${tenantCode}-default` && domain.domainName === '默认域'
        if (isDefault) {
            message.warning('默认域受保护，不可删除')
            return
        }
        if (domain.id == null) {
            message.error('域 ID 缺失')
            return
        }
        try {
            await ctcAcDomainApi.delete(domain.id)
            message.success('删除成功')
            const list = await ctcAcDomainApi.listByTenant(tenantCode)
            setDomainMap(prev => ({...prev, [tenantCode]: Array.isArray(list) ? list : []}))
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    const handleCreate = async () => {
        try {
            const values = await createForm.validateFields()
            await ctcAcTenantApi.create({...values, status: 1})
            message.success('创建成功')
            setCreateOpen(false)
            createForm.resetFields()
            await fetchTenants()
        } catch (e) {
            if (e?.errorFields) return
            message.error('创建失败：' + (e?.message || '未知错误'))
        }
    }

    const handleStatus = async (tenantCode, current) => {
        const next = current === 1 ? 0 : 1
        try {
            await ctcAcTenantApi.updateStatus(tenantCode, next)
            message.success(next === 1 ? '已启用' : '已停用')
            await fetchTenants()
        } catch (e) {
            message.error('操作失败：' + (e?.message || '未知错误'))
        }
    }

    // ── 新增域（在某个租户条目上点 + 触发） ──
    const openDomainCreate = (tenant) => {
        setDomainCreateParent(tenant)
        domainForm.resetFields()
        domainForm.setFieldsValue({status: 1})
        setDomainCreateOpen(true)
    }

    const handleDomainCreate = async () => {
        if (!domainCreateParent) return
        try {
            const values = await domainForm.validateFields()
            const tenantCode = domainCreateParent.tenantCode
            await ctcAcDomainApi.create({
                domainCode: values.domainCode,
                domainName: values.domainName,
                tenantCode,
                status: values.status ?? 1,
            })
            message.success(`已为租户「${domainCreateParent.tenantName || tenantCode}」新增域「${values.domainName}」`)
            setDomainCreateOpen(false)
            domainForm.resetFields()
            // 刷新该租户的域列表
            const list = await ctcAcDomainApi.listByTenant(tenantCode)
            const safeList = Array.isArray(list) ? list : []
            setDomainMap(prev => ({...prev, [tenantCode]: safeList}))
            // 🆕 修复 3: 强制展开该租户, 让新域可见
            const tenantKey = nodeKey('tenant', tenantCode)
            setExpandedKeys(prev => prev.includes(tenantKey) ? prev : [...prev, tenantKey])
        } catch (e) {
            if (e?.errorFields) return
            message.error('新增域失败：' + (e?.message || '未知错误'))
        }
    }

    // 树节点选中
    const onSelect = (keys, info) => {
        if (keys.length === 0) return
        const key = keys[0]
        setSelectedKey([key])
        const meta = info.node.data
        setSelectedNode(meta)
        setConfigData(extConfigMap[key] || [])
    }

    // 保存扩展配置 K-V (内存态)
    const handleSaveKv = async () => {
        if (!selectedKey[0]) return
        try {
            const values = await kvForm.validateFields()
            let next
            if (editingKv) {
                next = configData.map(r => r.key === editingKv.key ? {key: values.dataId, value: values.content} : r)
            } else {
                if (configData.some(r => r.key === values.dataId)) {
                    message.error('Key 已存在')
                    return
                }
                next = [...configData, {key: values.dataId, value: values.content}]
            }
            setExtConfigMap(prev => ({...prev, [selectedKey[0]]: next}))
            setConfigData(next)
            message.success(editingKv ? '更新成功' : '创建成功')
            setKvModalOpen(false)
            setEditingKv(null)
            kvForm.resetFields()
        } catch (e) {
            if (e?.errorFields) return
            message.error('保存失败')
        }
    }

    const handleDeleteKv = (record) => {
        const next = configData.filter(r => r.key !== record.key)
        setExtConfigMap(prev => ({...prev, [selectedKey[0]]: next}))
        setConfigData(next)
        message.success('删除成功')
    }

    // ── 导出 ──
    const handleExport = () => {
        if (tenants.length === 0) {
            message.warning('暂无数据可导出')
            return
        }
        const header = '租户编码,租户名称,描述,状态,创建时间,更新时间\n'
        const csv = header + tenants.map(r =>
            [r.tenantCode, r.tenantName, r.description, (STATUS_MAP[r.status ?? 1] || {text: String(r.status)}).text, r.createdAt, r.updatedAt].join(',')
        ).join('\n')
        const blob = new Blob(['\uFEFF' + csv], {type: 'text/csv;charset=utf-8'})
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `tenants_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
        a.click()
        URL.revokeObjectURL(url)
        message.success(`已导出 ${tenants.length} 条数据`)
    }

    // 当前选中节点对应的 tenant object
    const selectedTenant = selectedNode?.type === 'tenant' ? selectedNode.data : null
    const selectedDomain = selectedNode?.type === 'domain' ? selectedNode.data : null
    const selectedTenantForDomain = selectedDomain
        ? tenants.find(t => t.tenantCode === selectedDomain.tenantCode)
        : null

    const kvColumns = [
        {title: 'Key', dataIndex: 'key', key: 'key', width: 200},
        {title: 'Value', dataIndex: 'value', key: 'value', ellipsis: true},
        {
            title: '操作', key: 'action', width: 160,
            render: (_, record) => (
                <Space>
                    <Button type="link" size="small" icon={<EditOutlined/>}
                            onClick={() => {
                                setEditingKv(record)
                                kvForm.setFieldsValue({dataId: record.key, content: record.value})
                                setKvModalOpen(true)
                            }}>
                        编辑
                    </Button>
                    <Popconfirm title="确认删除该配置？" onConfirm={() => handleDeleteKv(record)}>
                        <Button type="link" danger size="small" icon={<DeleteOutlined/>}>
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ]

    // ── 树节点自定义渲染：在租户条目上加 + 触发新增域；域条目上加 × 触发删除 ──
    // 🆕 修复 2: 移除外层 span 的 onClick={e=>e.stopPropagation()}, 让 AntD Tree onSelect 能响应点击
    const titleRender = (nodeData) => {
        const meta = nodeData.data
        if (meta.type === 'tenant') {
            const tenant = meta.data
            return (
                <span
                    style={{display: 'flex', alignItems: 'center', gap: 6, width: '100%'}}
                >
                    <span style={{flex: 1}}>{nodeData.title}</span>
                    <Tooltip title="新增域">
                        <Button
                            size="small"
                            type="text"
                            icon={<PlusOutlined style={{fontSize: 11}}/>}
                            onClick={e => {
                                e.stopPropagation()
                                openDomainCreate(tenant)
                            }}
                        />
                    </Tooltip>
                </span>
            )
        }
        // domain
        const domain = meta.data
        const tenantCode = domain.tenantCode
        return (
            <span
                style={{display: 'flex', alignItems: 'center', gap: 6, width: '100%'}}
            >
                <span style={{flex: 1}}>{nodeData.title}</span>
                <Popconfirm
                    title="确定删除该域？"
                    description="删除后不可恢复"
                    onConfirm={e => {
                        e?.stopPropagation()
                        handleDeleteDomain(tenantCode, domain)
                    }}
                    onCancel={e => e?.stopPropagation()}
                    okText="确定" cancelText="取消"
                >
                    <Button
                        size="small"
                        type="text"
                        danger
                        icon={<DeleteOutlined style={{fontSize: 11}}/>}
                        onClick={e => e.stopPropagation()}
                    />
                </Popconfirm>
            </span>
        )
    }

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 0}}>
            <PageHeader
                title="租户管理"
                subtitle="4A AC 域 · 多租户与域的层级管理"
                breadcrumb={[{label: '4A 中心'}, {label: '租户管理'}]}
            />
            {/* ── 左树 + 右表 ── */}
            <div style={{display: 'flex', gap: 16, height: 'calc(100vh - 140px)'}}>
                {/* 左：租户/域树 */}
                <Card
                    title="租户 / 域"
                    size="small"
                    style={{width: 300, flexShrink: 0}}
                    styles={{body: {padding: 8, overflow: 'auto', height: 'calc(100% - 40px)'}}}
                    extra={
                        <Space size={4}>
                            <Tooltip title="新增租户">
                                <Button size="small" type="text" icon={<PlusOutlined/>} onClick={() => {
                                    createForm.resetFields()
                                    setCreateOpen(true)
                                }}/>
                            </Tooltip>
                            <Tooltip
                                title="租户由 FEATURE049 多租户身份模型在用户加入/创建租户时自动建立, 此处仅维护已有租户 (不能新建)">
                                <Button size="small" type="text"
                                        icon={<InfoCircleOutlined style={{color: '#1890ff'}}/>}/>
                            </Tooltip>
                            <Tooltip title="刷新">
                                <Button size="small" type="text" icon={<ReloadOutlined/>}
                                        onClick={fetchTenants}/>
                            </Tooltip>
                        </Space>
                    }
                >
                    {treeData.length > 0 ? (
                        <Tree
                            treeData={treeData}
                            selectedKeys={selectedKey}
                            onSelect={onSelect}
                            titleRender={titleRender}
                            expandedKeys={expandedKeys}
                            onExpand={setExpandedKeys}
                            defaultExpandAll
                            showLine
                            blockNode
                        />
                    ) : (
                        <Empty description={loadingTenants ? '加载中...' : '暂无租户，点击右上"新增租户"图标创建'}/>
                    )}
                </Card>

                {/* 右：详情 + K-V 配置 */}
                <Card size="small" style={{flex: 1, overflow: 'auto'}} styles={{body: {padding: 16}}}>
                    {!selectedNode ? (
                        <Empty description="请从左侧选择租户或域" style={{marginTop: 80}}/>
                    ) : (
                        <>
                            <Descriptions
                                title={selectedTenant ? '租户信息' : '域信息'}
                                size="small"
                                bordered
                                column={2}
                                style={{marginBottom: 16}}
                            >
                                {selectedTenant ? (
                                    <>
                                        <Descriptions.Item label="编码">{selectedTenant.tenantCode}</Descriptions.Item>
                                        <Descriptions.Item label="名称">{selectedTenant.tenantName}</Descriptions.Item>
                                        <Descriptions.Item label="描述" span={2}>
                                            {selectedTenant.description || '-'}
                                        </Descriptions.Item>
                                        <Descriptions.Item label="状态">
                                            <Popconfirm
                                                title={`确认${selectedTenant.status === 1 ? '停用' : '启用'}该租户？`}
                                                onConfirm={() => handleStatus(selectedTenant.tenantCode, selectedTenant.status ?? 1)}
                                            >
                                                <Button size="small" type="link" danger={selectedTenant.status === 1}>
                                                    <Tag color={STATUS_MAP[selectedTenant.status ?? 1]?.color}>
                                                        {STATUS_MAP[selectedTenant.status ?? 1]?.text}
                                                    </Tag>
                                                    <span style={{marginLeft: 4, fontSize: 12}}>
                                                        点击切换
                                                    </span>
                                                </Button>
                                            </Popconfirm>
                                        </Descriptions.Item>
                                        <Descriptions.Item
                                            label="创建时间">{selectedTenant.createdAt || '-'}</Descriptions.Item>
                                    </>
                                ) : (
                                    <>
                                        <Descriptions.Item label="编码">{selectedDomain.domainCode}</Descriptions.Item>
                                        <Descriptions.Item label="名称">{selectedDomain.domainName}</Descriptions.Item>
                                        <Descriptions.Item label="所属租户">
                                            {selectedTenantForDomain?.tenantName || selectedDomain.tenantCode}
                                        </Descriptions.Item>
                                        <Descriptions.Item label="状态">
                                            <Tag color={STATUS_MAP[selectedDomain.status]?.color}>
                                                {STATUS_MAP[selectedDomain.status]?.text}
                                            </Tag>
                                        </Descriptions.Item>
                                    </>
                                )}
                            </Descriptions>

                            <Card
                                type="inner"
                                title="扩展配置 (K-V)"
                                size="small"
                                extra={
                                    <Button
                                        type="primary"
                                        size="small"
                                        icon={<PlusOutlined/>}
                                        onClick={() => {
                                            setEditingKv(null)
                                            kvForm.resetFields()
                                            setKvModalOpen(true)
                                        }}
                                    >
                                        新增
                                    </Button>
                                }
                            >
                                <Table
                                    columns={kvColumns}
                                    dataSource={configData.map((r, i) => ({...r, _idx: i}))}
                                    rowKey={r => `${r.key}_${r._idx}`}
                                    pagination={false}
                                    size="small"
                                    locale={{emptyText: '暂无扩展配置，点击右上"新增"添加'}}
                                />
                            </Card>
                        </>
                    )}
                </Card>
            </div>

            {/* 新建租户 */}
            <Modal
                title="新建租户"
                open={createOpen}
                onOk={handleCreate}
                onCancel={() => setCreateOpen(false)}
                size="large"
                destroyOnHidden
            >
                <Form form={createForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item
                        name="tenantCode"
                        label="租户编码"
                        rules={[
                            {required: true, message: '请输入租户编码'},
                            {pattern: /^[a-z0-9_]+$/, message: '仅允许小写字母数字下划线'},
                        ]}
                        extra="唯一标识，创建后不可修改"
                    >
                        <Input placeholder="如 default / tenant_a"/>
                    </Form.Item>
                    <Form.Item name="tenantName" label="租户名称" rules={[{required: true}]}>
                        <Input placeholder="如 默认租户"/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={3} placeholder="可选"/>
                    </Form.Item>
                </Form>
            </Modal>

            {/* K-V 编辑 */}
            <Modal
                title={editingKv ? '编辑配置' : '新增配置'}
                open={kvModalOpen}
                onOk={handleSaveKv}
                onCancel={() => {
                    setKvModalOpen(false)
                    setEditingKv(null)
                }}
                destroyOnHidden
            >
                <Form form={kvForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="dataId" label="Key" rules={[{required: true}]}>
                        <Input disabled={!!editingKv} placeholder="如 max_users"/>
                    </Form.Item>
                    <Form.Item name="content" label="Value" rules={[{required: true}]}>
                        <Input.TextArea rows={4} placeholder="配置值"/>
                    </Form.Item>
                </Form>
            </Modal>

            {/* 新增域（点租户 + 触发） */}
            <Modal
                title={domainCreateParent ? `为「${domainCreateParent.tenantName || domainCreateParent.tenantCode}」新增域` : '新增域'}
                open={domainCreateOpen}
                onOk={handleDomainCreate}
                onCancel={() => {
                    setDomainCreateOpen(false)
                    domainForm.resetFields()
                }}
                size="large"
                destroyOnHidden
            >
                <Form form={domainForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item label="所属租户">
                        <Input value={domainCreateParent?.tenantName || ''} disabled/>
                    </Form.Item>
                    <Form.Item
                        name="domainCode"
                        label="域编码"
                        rules={[
                            {required: true, message: '请输入域编码'},
                            {pattern: /^[a-z0-9_-]+$/, message: '仅允许小写字母、数字、下划线、短横线'},
                        ]}
                        extra="租户内唯一，建议形如 prod / staging / dev"
                    >
                        <Input placeholder="如 prod / staging"/>
                    </Form.Item>
                    <Form.Item
                        name="domainName"
                        label="域名称"
                        rules={[{required: true, message: '请输入域名称'}]}
                    >
                        <Input placeholder="如 生产环境"/>
                    </Form.Item>
                    <Form.Item name="status" label="状态" initialValue={1}>
                        <select
                            style={{width: '100%', padding: '4px 8px', border: '1px solid #d9d9d9', borderRadius: 4}}>
                            <option value={1}>正常</option>
                            <option value={0}>停用</option>
                        </select>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}
