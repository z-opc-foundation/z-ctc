import {useCallback, useEffect, useState} from 'react'
import {
    Button,
    Card,
    Descriptions,
    Dropdown,
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
import {DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {configureApiBaseURL, ctcAcDeptApi, ctcAcGroupApi, ctcAcOrgApi} from './api'

/**
 * 组织 / 部门 / 组 管理 (4A AC 域) — 左树右表布局。
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │ ▼ tenant_1                                                  │
 * │   ▼ 默认域                                                  │
 * │     ▼ 组织A          ┌────────────────────────────────────┐│
 * │       ▼ 部门A-1      │ 组织A 信息                          ││
 * │         · 组1        │ 编码 / 名称 / 描述 / 状态           ││
 * │       · 组2          ├────────────────────────────────────┤│
 * │     · 组织B          │ 下级（部门/组）                      ││
 * │                      ├────────────────────────────────────┤│
 * │                      │ 扩展配置 (K-V)                      ││
 * │                      └────────────────────────────────────┘│
 * └──────────────────────────────────────────────────────────────┘
 *
 * Props:
 *   - tenants: Array<{tenantCode, tenantName}>          租户列表 (必填)
 *   - domains: Array<{domainCode, domainName}>          当前租户的域列表 (必填)
 *   - selectedTenant: string                            当前选中租户 (必填)
 *   - selectedDomain: string                            当前选中域 (必填)
 *   - onTenantChange: (code) => void                    租户切换回调
 *   - onDomainChange: (code) => void                    域切换回调
 *   - apiBaseURL: string                                后端 baseURL, 默认 '/api'
 *   - extConfigStorageKey: string                       K-V 扩展配置 localStorage key, 默认 'z_org_ext_config'
 *
 * 后端契约 (z-ctc/z-ctc-ac/OrgController @ /api/ctc/ac):
 *   - /orgs  POST / GET(list) / GET(code) / PUT(code) / DELETE(code)
 *   - /depts /groups 同上
 *
 * K-V 扩展配置: 暂走 localStorage, 未落库.
 */

const EXT_CONFIG_KEY_DEFAULT = 'z_org_ext_config'

const STATUS_MAP = {
    1: {text: '正常', color: 'success'},
    0: {text: '停用', color: 'default'},
}

const TYPE_LABEL = {
    org: {label: '组织', color: '#1677ff'},
    dept: {label: '部门', color: '#52c41a'},
    group: {label: '组别', color: '#fa8c16'},
}

function nodeKey(type, code) {
    return `${type}:${code}`
}

function codeOf(node, type) {
    if (type === 'org') return node.orgCode
    if (type === 'dept') return node.deptCode
    return node.groupCode
}

function nameOf(node) {
    return node.orgName || node.deptName || node.groupName || ''
}

function detectType(r) {
    if (r.groupCode) return 'group'
    if (r.deptCode) return 'dept'
    return 'org'
}

// localStorage K-V 配置 (暂留前端, 后续落库)
function loadExtConfig(key, storageKey) {
    try {
        const all = JSON.parse(localStorage.getItem(storageKey) || '{}')
        return all[key] || []
    } catch {
        return []
    }
}

function saveExtConfig(key, list, storageKey) {
    const all = JSON.parse(localStorage.getItem(storageKey) || '{}')
    all[key] = list
    localStorage.setItem(storageKey, JSON.stringify(all))
}

export default function OrgManagement({
                                          tenants = [],
                                          domains = [],
                                          selectedTenant = null,
                                          selectedDomain = null,
                                          onTenantChange,
                                          onDomainChange,
                                          apiBaseURL,
                                          extConfigStorageKey = EXT_CONFIG_KEY_DEFAULT,
                                      }) {
    // 配置 baseURL (组件 mount 时生效)
    useEffect(() => {
        if (apiBaseURL) configureApiBaseURL(apiBaseURL)
    }, [apiBaseURL])

    // ── 实体缓存 (org/dept/group) — 拉自后端 ──
    const [orgMap, setOrgMap] = useState({})
    const [deptMap, setDeptMap] = useState({})
    const [groupMap, setGroupMap] = useState({})

    // ── 树 ──
    const [treeData, setTreeData] = useState([])
    const [expandedKeys, setExpandedKeys] = useState([])
    const [selectedKey, setSelectedKey] = useState([])

    // ── 选中节点 / 右侧面板 ──
    const [selectedNode, setSelectedNode] = useState(null)
    const [configData, setConfigData] = useState([])
    const [childList, setChildList] = useState([])

    // ── 弹窗：K-V ──
    const [kvModalOpen, setKvModalOpen] = useState(false)
    const [editingKv, setEditingKv] = useState(null)
    const [kvForm] = Form.useForm()

    // ── 弹窗：新增实体 ──
    const [entityModalOpen, setEntityModalOpen] = useState(false)
    const [entityType, setEntityType] = useState(null)
    const [entityParent, setEntityParent] = useState(null)
    const [entityForm] = Form.useForm()

    // ── 拉取 ──
    const loadOrgsForDomain = useCallback(async (tenantCode, domainCode) => {
        if (!tenantCode || !domainCode) return []
        const list = await ctcAcOrgApi.listByDomain(tenantCode, domainCode)
        const arr = Array.isArray(list) ? list : []
        setOrgMap(prev => ({...prev, [domainCode]: arr}))
        return arr
    }, [])

    const loadDeptsForOrg = useCallback(async (tenantCode, domainCode, orgCode) => {
        if (!tenantCode || !domainCode || !orgCode) return []
        const list = await ctcAcDeptApi.listByOrg(tenantCode, domainCode, orgCode)
        const arr = Array.isArray(list) ? list : []
        setDeptMap(prev => ({...prev, [orgCode]: arr}))
        return arr
    }, [])

    const loadGroupsForDept = useCallback(async (tenantCode, domainCode, deptCode) => {
        if (!tenantCode || !domainCode || !deptCode) return []
        const list = await ctcAcGroupApi.listByDept(tenantCode, domainCode, deptCode)
        const arr = Array.isArray(list) ? list : []
        setGroupMap(prev => ({...prev, [deptCode]: arr}))
        return arr
    }, [])

    // ── 构造树 ──
    useEffect(() => {
        if (!selectedTenant || !selectedDomain) {
            setTreeData([])
            return
        }
        if (!orgMap[selectedDomain]) {
            loadOrgsForDomain(selectedTenant, selectedDomain)
            return
        }
        const orgs = orgMap[selectedDomain] || []
        const tree = orgs.map(o => ({
            key: nodeKey('org', o.orgCode),
            title: (
                <span style={{display: 'inline-flex', alignItems: 'center', gap: 6}}>
                    <Tag color={TYPE_LABEL.org.color} style={{margin: 0}}>组织</Tag>
                    {o.orgName || o.orgCode}
                    {o.status === 0 && <Tag color="default" style={{margin: 0, fontSize: 10}}>停用</Tag>}
                </span>
            ),
            data: {type: 'org', data: o},
            isLeaf: false,
        }))
        setTreeData(tree)
    }, [selectedTenant, selectedDomain, orgMap, loadOrgsForDomain])

    const onLoadData = async (treeNode) => {
        const {key, data, type} = treeNode
        let children = []
        if (type === 'org') {
            const depts = await loadDeptsForOrg(selectedTenant, selectedDomain, data.orgCode)
            children = depts.map(d => ({
                key: nodeKey('dept', d.deptCode),
                title: (
                    <span style={{display: 'inline-flex', alignItems: 'center', gap: 6}}>
                        <Tag color={TYPE_LABEL.dept.color} style={{margin: 0}}>部门</Tag>
                        {d.deptName || d.deptCode}
                        {d.status === 0 && <Tag color="default" style={{margin: 0, fontSize: 10}}>停用</Tag>}
                    </span>
                ),
                data: {type: 'dept', data: d},
                isLeaf: false,
            }))
        } else if (type === 'dept') {
            const groups = await loadGroupsForDept(selectedTenant, selectedDomain, data.deptCode)
            children = groups.map(g => ({
                key: nodeKey('group', g.groupCode),
                title: (
                    <span style={{display: 'inline-flex', alignItems: 'center', gap: 6}}>
                        <Tag color={TYPE_LABEL.group.color} style={{margin: 0}}>组</Tag>
                        {g.groupName || g.groupCode}
                        {g.status === 0 && <Tag color="default" style={{margin: 0, fontSize: 10}}>停用</Tag>}
                    </span>
                ),
                data: {type: 'group', data: g},
                isLeaf: true,
            }))
        }
        setTreeData(prev => updateTreeData(prev, key, children))
    }

    const updateTreeData = (list, key, children) =>
        list.map(item => {
            if (item.key === key) return {...item, children}
            if (item.children) return {...item, children: updateTreeData(item.children, key, children)}
            return item
        })

    // ── 节点选中 ──
    const onSelect = (keys, info) => {
        if (!keys || keys.length === 0) return
        const k = keys[0]
        setSelectedKey([k])
        const meta = info.node.data
        setSelectedNode(meta)
        setConfigData(loadExtConfig(k, extConfigStorageKey))
        if (meta.type === 'org') {
            setChildList(deptMap[meta.data.orgCode] || [])
        } else if (meta.type === 'dept') {
            setChildList(groupMap[meta.data.deptCode] || [])
        } else {
            setChildList([])
        }
    }

    // ── 树节点操作按钮 ──
    const titleRender = (nodeData) => {
        const meta = nodeData.data
        const type = meta?.type
        const data = meta?.data
        const addItems =
            type === 'org'
                ? [
                    {key: 'org', label: <span><Tag color={TYPE_LABEL.org.color}>组织</Tag> 新增组织</span>},
                    {key: 'dept', label: <span><Tag color={TYPE_LABEL.dept.color}>部门</Tag> 新增部门</span>},
                ]
                : type === 'dept'
                    ? [
                        {key: 'group', label: <span><Tag color={TYPE_LABEL.group.color}>组别</Tag> 新增组别</span>},
                    ]
                    : []
        const typeLabel = type ? (TYPE_LABEL[type]?.label || '节点') : '节点'
        return (
            <span style={{display: 'flex', alignItems: 'center', gap: 6, width: '100%'}}
                  onClick={e => e.stopPropagation()}>
                <span style={{flex: 1}}>{nodeData.title}</span>
                {addItems.length > 0 && (
                    <Dropdown
                        menu={{items: addItems, onClick: ({key}) => openEntityModal(key, data)}}
                        trigger={['click']}
                    >
                        <Button size="small" type="text" icon={<PlusOutlined style={{fontSize: 11}}/>}
                                onClick={e => e.stopPropagation()}/>
                    </Dropdown>
                )}
                <Popconfirm
                    title={`确定删除该${typeLabel}？`}
                    onConfirm={() => type && handleDeleteEntity(type, data)}
                    onCancel={e => e?.stopPropagation()}
                    okText="确定" cancelText="取消"
                >
                    <Button size="small" type="text" danger icon={<DeleteOutlined style={{fontSize: 11}}/>}
                            onClick={e => e.stopPropagation()}/>
                </Popconfirm>
            </span>
        )
    }

    // ── 删除实体 ──
    const handleDeleteEntity = async (type, data) => {
        try {
            const tenantCode = data.tenantCode || selectedTenant
            const domainCode = data.domainCode || selectedDomain
            if (!tenantCode || !domainCode) {
                message.error('租户/域信息缺失, 无法删除')
                return
            }
            if (type === 'org') {
                await ctcAcOrgApi.delete(tenantCode, domainCode, data.orgCode)
                await loadOrgsForDomain(tenantCode, domainCode)
            } else if (type === 'dept') {
                await ctcAcDeptApi.delete(tenantCode, domainCode, data.deptCode, data.orgCode)
                if (data.orgCode) await loadDeptsForOrg(tenantCode, domainCode, data.orgCode)
            } else if (type === 'group') {
                await ctcAcGroupApi.delete(tenantCode, domainCode, data.deptCode, data.groupCode)
                if (data.deptCode) await loadGroupsForDept(tenantCode, domainCode, data.deptCode)
            }
            message.success('删除成功')
            if (selectedNode && codeOf(selectedNode.data, selectedNode.type) === codeOf(data, type)) {
                setSelectedNode(null)
                setConfigData([])
                setChildList([])
                setSelectedKey([])
            }
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    // ── 新增实体 ──
    const openEntityModal = (type, parent) => {
        setEntityType(type)
        setEntityParent(parent)
        entityForm.resetFields()
        if (parent) {
            entityForm.setFieldsValue({
                tenantCode: parent.tenantCode || selectedTenant,
                domainCode: parent.domainCode || selectedDomain,
                orgCode: parent.orgCode,
                deptCode: parent.deptCode,
            })
        }
        setEntityModalOpen(true)
    }

    const handleEntitySave = async () => {
        if (!entityType) return
        try {
            const values = await entityForm.validateFields()
            const newNode = {
                ...values,
                status: values.status ?? 1,
                description: values.description || '',
                extConfig: '[]',
            }
            if (entityType === 'org') {
                await ctcAcOrgApi.create(newNode)
                const domainCode = newNode.domainCode || selectedDomain
                if (domainCode) await loadOrgsForDomain(newNode.tenantCode, domainCode)
            } else if (entityType === 'dept') {
                await ctcAcDeptApi.create(newNode)
                const orgCode = newNode.orgCode
                if (orgCode) await loadDeptsForOrg(newNode.tenantCode, newNode.domainCode, orgCode)
                if (selectedNode?.type === 'org' && selectedNode.data.orgCode === orgCode) {
                    const list = await ctcAcDeptApi.listByOrg(newNode.tenantCode, newNode.domainCode, orgCode)
                    setChildList(list)
                }
            } else if (entityType === 'group') {
                await ctcAcGroupApi.create(newNode)
                const deptCode = newNode.deptCode
                if (deptCode) await loadGroupsForDept(newNode.tenantCode, newNode.domainCode, deptCode)
            }
            message.success('创建成功')
            setEntityModalOpen(false)
        } catch (e) {
            if (e?.errorFields) return
            message.error('创建失败：' + (e?.message || '未知错误'))
        }
    }

    // ── K-V 编辑 ──
    const handleSaveKv = async () => {
        if (!selectedNode) return
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
            const nk = nodeKey(selectedNode.type, codeOf(selectedNode.data, selectedNode.type))
            saveExtConfig(nk, next, extConfigStorageKey)
            setConfigData(next)
            setKvModalOpen(false)
            setEditingKv(null)
            kvForm.resetFields()
            message.success('保存成功')
        } catch (e) {
            if (e?.errorFields) return
            message.error('保存失败')
        }
    }

    const handleDeleteKv = (record) => {
        if (!selectedNode) return
        const nk = nodeKey(selectedNode.type, codeOf(selectedNode.data, selectedNode.type))
        const next = configData.filter(r => r.key !== record.key)
        saveExtConfig(nk, next, extConfigStorageKey)
        setConfigData(next)
        message.success('删除成功')
    }

    // ── 子列表点击切换 ──
    const handleChildClick = (record, type) => {
        const code = codeOf(record, type)
        setSelectedKey([nodeKey(type, code)])
        setSelectedNode({type, data: record})
        setConfigData(loadExtConfig(nodeKey(type, code), extConfigStorageKey))
        if (type === 'org') setChildList(deptMap[record.orgCode] || [])
        else if (type === 'dept') setChildList(groupMap[record.deptCode] || [])
        else setChildList([])
    }

    // ── 子列表 / K-V 列 ──
    const childColumns = [
        {title: '编码', dataIndex: 'code', width: 140, render: (_, r) => codeOf(r, detectType(r))},
        {title: '名称', dataIndex: 'name', ellipsis: true, render: (_, r) => nameOf(r)},
        {
            title: '类型', dataIndex: 'type', width: 100,
            render: (_, r) => {
                const t = detectType(r)
                return <Tag color={TYPE_LABEL[t].color}>{TYPE_LABEL[t].label}</Tag>
            },
        },
        {
            title: '操作', dataIndex: 'action', width: 100,
            render: (_, r) => {
                const t = detectType(r)
                return (
                    <Button type="link" size="small" icon={<EditOutlined/>}
                            onClick={() => handleChildClick(r, t)}>详情</Button>
                )
            },
        },
    ]

    const configColumns = [
        {title: 'Key', dataIndex: 'key', width: 200, ellipsis: true},
        {title: 'Value', dataIndex: 'value', ellipsis: true},
        {
            title: '操作', width: 140,
            render: (_, r) => (
                <Space>
                    <Button size="small" type="link" icon={<EditOutlined/>}
                            onClick={() => {
                                setEditingKv(r)
                                kvForm.setFieldsValue({dataId: r.key, content: r.value})
                                setKvModalOpen(true)
                            }}>编辑</Button>
                    <Popconfirm title="确认删除" onConfirm={() => handleDeleteKv(r)}>
                        <Button size="small" type="link" danger icon={<DeleteOutlined/>}>删除</Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ]

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 0, flex: 1, minHeight: 0}}>
            {/* 顶部 — 租户/域选择 (组件使用者可在外层提供, 这里只是兜底渲染) */}
            <div style={{
                display: 'flex',
                justifyContent: 'flex-start',
                alignItems: 'center',
                marginBottom: 12,
            }}>
                <Space>
                    <span style={{color: '#666'}}>租户</span>
                    <select
                        value={selectedTenant || ''}
                        onChange={e => onTenantChange && onTenantChange(e.target.value || null)}
                        style={{padding: '4px 8px', border: '1px solid #d9d9d9', borderRadius: 4, minWidth: 160}}
                    >
                        <option value="">请选择</option>
                        {tenants.map(o => <option key={o.value || o.tenantCode}
                                                  value={o.value || o.tenantCode}>{o.label || `${o.tenantName || o.tenantCode} (${o.tenantCode})`}</option>)}
                    </select>
                    <span style={{color: '#666'}}>域</span>
                    <select
                        value={selectedDomain || ''}
                        onChange={e => onDomainChange && onDomainChange(e.target.value || null)}
                        disabled={!selectedTenant}
                        style={{padding: '4px 8px', border: '1px solid #d9d9d9', borderRadius: 4, minWidth: 160}}
                    >
                        <option value="">请选择</option>
                        {domains.map(o => <option key={o.value || o.domainCode}
                                                  value={o.value || o.domainCode}>{o.label || `${o.domainName || o.domainCode} (${o.domainCode})`}</option>)}
                    </select>
                </Space>
            </div>

            {/* 左树 + 右表 */}
            <div style={{display: 'flex', gap: 16, flex: 1, minHeight: 0}}>
                <Card
                    size="small"
                    style={{width: 340, overflow: 'auto', flexShrink: 0}}
                    title="组织树"
                    extra={
                        <Space size={4}>
                            {selectedDomain && (
                                <Tooltip title="新增组织">
                                    <Button size="small" type="primary" icon={<PlusOutlined/>}
                                            onClick={() => openEntityModal('org', {
                                                tenantCode: selectedTenant,
                                                domainCode: selectedDomain,
                                            })}/>
                                </Tooltip>
                            )}
                            <Tooltip title="刷新">
                                <Button size="small" icon={<ReloadOutlined/>}
                                        onClick={() => loadOrgsForDomain(selectedTenant, selectedDomain)}/>
                            </Tooltip>
                        </Space>
                    }
                >
                    {selectedDomain ? (
                        treeData.length > 0 ? (
                            <Tree
                                treeData={treeData}
                                onSelect={onSelect}
                                loadData={onLoadData}
                                titleRender={titleRender}
                                blockNode
                                showLine
                                showIcon={false}
                                expandedKeys={expandedKeys}
                                onExpand={setExpandedKeys}
                                selectedKeys={selectedKey}
                            />
                        ) : (
                            <Empty description="该域下暂无组织，请点击右上角新增"/>
                        )
                    ) : (
                        <Empty description="请先选择租户和域"/>
                    )}
                </Card>

                <Card size="small" style={{flex: 1, overflow: 'auto'}}>
                    {!selectedNode ? (
                        <Empty description="请从左侧选择组织/部门/组"/>
                    ) : (
                        <>
                            <Descriptions
                                title={`${TYPE_LABEL[selectedNode.type].label}信息 — ${nameOf(selectedNode.data)}`}
                                size="small" bordered column={2} style={{marginBottom: 16}}
                            >
                                <Descriptions.Item
                                    label="编码">{codeOf(selectedNode.data, selectedNode.type)}</Descriptions.Item>
                                <Descriptions.Item label="名称">{nameOf(selectedNode.data)}</Descriptions.Item>
                                <Descriptions.Item
                                    label="租户">{selectedNode.data.tenantCode || '-'}</Descriptions.Item>
                                <Descriptions.Item label="域">{selectedNode.data.domainCode || '-'}</Descriptions.Item>
                                {selectedNode.type !== 'org' && (
                                    <Descriptions.Item label="所属组织">{selectedNode.data.orgCode}</Descriptions.Item>
                                )}
                                {selectedNode.type === 'group' && (
                                    <Descriptions.Item label="所属部门">{selectedNode.data.deptCode}</Descriptions.Item>
                                )}
                                <Descriptions.Item label="状态" span={selectedNode.type === 'group' ? 1 : 2}>
                                    <Tag color={STATUS_MAP[selectedNode.data.status ?? 1]?.color}>
                                        {STATUS_MAP[selectedNode.data.status ?? 1]?.text}
                                    </Tag>
                                </Descriptions.Item>
                                <Descriptions.Item label="描述" span={2}>
                                    {selectedNode.data.description || '-'}
                                </Descriptions.Item>
                            </Descriptions>

                            {selectedNode.type !== 'group' && (
                                <Card type="inner"
                                      title={selectedNode.type === 'org' ? '下级部门 / 组' : '下级组'}
                                      size="small" style={{marginBottom: 16}}>
                                    <Table
                                        columns={childColumns}
                                        dataSource={childList.map((r, i) => ({...r, key: i}))}
                                        rowKey="key"
                                        pagination={false} size="small"
                                        locale={{emptyText: '暂无下级'}}
                                    />
                                </Card>
                            )}

                            <Card type="inner"
                                  title="扩展配置 (K-V)"
                                  size="small"
                                  extra={
                                      <Button size="small" type="primary" icon={<PlusOutlined/>}
                                              onClick={() => {
                                                  setEditingKv(null)
                                                  kvForm.resetFields()
                                                  setKvModalOpen(true)
                                              }}>新增</Button>
                                  }>
                                <Table
                                    columns={configColumns}
                                    dataSource={configData.map((r, i) => ({...r, key: r.key || i}))}
                                    rowKey="key"
                                    pagination={false} size="small"
                                    locale={{emptyText: '暂无扩展配置'}}
                                />
                            </Card>
                        </>
                    )}
                </Card>
            </div>

            <Modal
                title={editingKv ? '编辑配置' : '新增配置'}
                open={kvModalOpen}
                onCancel={() => {
                    setKvModalOpen(false);
                    setEditingKv(null)
                }}
                onOk={handleSaveKv}
                destroyOnHidden
            >
                <Form form={kvForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="dataId" label="Key" rules={[{required: true}]}>
                        <Input disabled={!!editingKv}/>
                    </Form.Item>
                    <Form.Item name="content" label="Value" rules={[{required: true}]}>
                        <Input.TextArea rows={4}/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title={entityType ? `新增${TYPE_LABEL[entityType].label}` : '新增'}
                open={entityModalOpen}
                onCancel={() => setEntityModalOpen(false)}
                onOk={handleEntitySave}
                destroyOnHidden
            >
                <Form form={entityForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="tenantCode" label="租户"><Input disabled/></Form.Item>
                    <Form.Item name="domainCode" label="域"><Input disabled/></Form.Item>
                    {(entityType === 'dept' || entityType === 'group') && (
                        <Form.Item name="orgCode" label="所属组织"><Input disabled/></Form.Item>
                    )}
                    {entityType === 'group' && (
                        <Form.Item name="deptCode" label="所属部门"><Input disabled/></Form.Item>
                    )}
                    <Form.Item
                        name={entityType === 'org' ? 'orgCode' : entityType === 'dept' ? 'deptCode' : 'groupCode'}
                        label="编码" rules={[{required: true}]}>
                        <Input placeholder="唯一标识"/>
                    </Form.Item>
                    <Form.Item
                        name={entityType === 'org' ? 'orgName' : entityType === 'dept' ? 'deptName' : 'groupName'}
                        label="名称" rules={[{required: true}]}>
                        <Input/>
                    </Form.Item>
                    <Form.Item name="status" label="状态" initialValue={1}>
                        <select
                            style={{width: '100%', padding: '4px 8px', border: '1px solid #d9d9d9', borderRadius: 4}}>
                            <option value={1}>正常</option>
                            <option value={0}>停用</option>
                        </select>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2}/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}