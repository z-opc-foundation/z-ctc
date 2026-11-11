import {useCallback, useEffect, useMemo, useRef, useState} from 'react'
import {ProTable} from '@ant-design/pro-components'
import {
    Button,
    Card,
    Descriptions,
    Dropdown,
    Drawer,
    Empty,
    Form,
    Input,
    InputNumber,
    message,
    Modal,
    Popconfirm,
    Select,
    Space,
    Switch,
    Tabs,
    Tag,
    Tooltip,
    Tree,
} from 'antd'
import {
    ApartmentOutlined,
    AuditOutlined,
    DeleteOutlined,
    EllipsisOutlined,
    PlusOutlined,
    ReloadOutlined,
    SearchOutlined,
    TeamOutlined,
    UndoOutlined,
} from '@ant-design/icons'
import {useSearchParams} from 'react-router-dom'
import {ctcAuthorizationApi} from './api'

/**
 * 权限管理（4A AUTHZ 域）— 角色 / 权限点 / 权限树 三视图合一
 *
 * 为什么要合并（CEO 2026-11-11：「角色管理和权限管理是分不开的，你给他做成同一个东西」）：
 *   角色是权限点的命名集合，权限点是粒度单位，权限树是二者的层级视图 ——
 *   拆成两个顶级菜单，用户必须自己在「角色」与「权限点」两张表之间来回对照
 *   才能回答「这个角色到底能干什么」。现在：同一页，左侧分菜单，右侧是内容。
 *
 * 三视图：
 *   角色   —— 角色列表；新增/编辑带「启用」滑块；点角色 → 右侧展开该角色的权限点树，
 *              勾选即挂载/卸载（真调 grant-resource / revoke-resource）。
 *   权限点 —— 最细粒度的权限点清单（资源 CRUD）。
 *   权限树 —— 全部权限点按「应用 → 父子层级」组织的只读树，浏览权限模型全貌。
 *
 * 与旧页面的差别（都是真接口，不再有假动作）：
 *   · 角色的编辑/删除/启停旧实现是「后端暂未提供接口」占位（本地假改），
 *     现在后端 PUT/DELETE /api/ctc/authz/roles 都在，直接落库。
 *   · 旧授权抽屉的权限树是 7 条 mock 资源（注释里写明等后端端点），
 *     现在走真实 resources/list + roles/resources。
 */

const TYPE_MAP = {
    0: {text: '功能', color: 'geekblue'},
    1: {text: '菜单', color: 'blue'},
    2: {text: '数据', color: 'purple'},
    3: {text: 'API', color: 'orange'},
    4: {text: '按钮', color: 'green'},
}

const ROLE_STATUS_MAP = {
    1: {text: '正常', color: 'success'},
    0: {text: '停用', color: 'default'},
}

const METHOD_OPTIONS = [
    {value: '', label: '任意'},
    {value: 'GET', label: 'GET'},
    {value: 'POST', label: 'POST'},
    {value: 'PUT', label: 'PUT'},
    {value: 'DELETE', label: 'DELETE'},
    {value: 'PATCH', label: 'PATCH'},
]

const VIEW_MENU = [
    {key: 'roles', label: '角色', icon: <TeamOutlined/>},
    {key: 'points', label: '权限点', icon: <AuditOutlined/>},
    {key: 'tree', label: '权限树', icon: <ApartmentOutlined/>},
]

/** 类型徽标 —— 旧表把 type=0 渲染成裸「0」（枚举里没有 0 的文案） */
function typeTag(resourceType) {
    const t = TYPE_MAP[resourceType] || {text: String(resourceType ?? '?'), color: 'default'}
    return <Tag color={t.color} style={{margin: 0, fontSize: 11}}>{t.text}</Tag>
}

/**
 * 权限树：先按应用分组，组内按 parentId 建父子层级。
 * 父节点若落在别的应用分组里（跨应用引用），在本组内降级成根节点，
 * 不丢节点（丢节点 = 权限点从树上消失 = 用户以为它不存在）。
 */
function buildPermissionTree(resources, {search = ''} = {}) {
    const kw = search.trim().toLowerCase()
    const hit = (r) =>
        !kw ||
        (r.resourceCode || '').toLowerCase().includes(kw) ||
        (r.resourceName || '').toLowerCase().includes(kw)
    const matched = resources.filter(hit)

    const groups = new Map()
    matched.forEach((r) => {
        const app = r.appCode || '公共'
        if (!groups.has(app)) groups.set(app, [])
        groups.get(app).push(r)
    })

    const nodeTitle = (r) => (
        <span style={{display: 'inline-flex', alignItems: 'center', gap: 6}}>
            <span>{r.resourceName || r.resourceCode}</span>
            {typeTag(r.resourceType)}
            <span style={{fontFamily: 'monospace', fontSize: 11, color: '#999'}}>{r.resourceCode}</span>
        </span>
    )

    const tree = []
    groups.forEach((items, app) => {
        const nodeMap = new Map()
        items.forEach((r) => nodeMap.set(r.id, {key: r.id, title: nodeTitle(r), raw: r, children: []}))
        const roots = []
        items.forEach((r) => {
            const node = nodeMap.get(r.id)
            const pid = r.parentId ?? 0
            if (pid && nodeMap.has(pid)) {
                nodeMap.get(pid).children.push(node)
            } else {
                roots.push(node)
            }
        })
        tree.push({
            key: `app:${app}`,
            title: (
                <span style={{display: 'inline-flex', alignItems: 'center', gap: 6, fontWeight: 600}}>
                    {app}
                    <Tag color="cyan" style={{margin: 0, fontSize: 11}}>{items.length}</Tag>
                </span>
            ),
            selectable: false,
            checkable: false,
            children: roots,
        })
    })
    return tree
}

/** 收集树里的全部 key（含应用分组与各层节点），用于默认全展开 */
function collectAllKeys(tree) {
    const keys = []
    const walk = (nodes) => {
        nodes.forEach((n) => {
            keys.push(n.key)
            if (n.children?.length) walk(n.children)
        })
    }
    walk(tree)
    return keys
}

/** 收集树里的全部资源 id（用于「全选/清空」） */
function collectResourceIds(tree) {
    const ids = []
    const walk = (nodes) => {
        nodes.forEach((n) => {
            if (n.raw) ids.push(n.key)
            if (n.children?.length) walk(n.children)
        })
    }
    walk(tree)
    return ids
}

export default function PermissionManagement({apiBaseURL}) {
    const [searchParams, setSearchParams] = useSearchParams()
    const view = ['roles', 'points', 'tree'].includes(searchParams.get('view'))
        ? searchParams.get('view')
        : 'roles'
    const setView = useCallback(
        (next) => {
            const p = new URLSearchParams(searchParams)
            if (next === 'roles') p.delete('view')
            else p.set('view', next)
            setSearchParams(p, {replace: true})
        },
        [searchParams, setSearchParams],
    )

    // ── 权限点（资源）全量 ──
    const [resources, setResources] = useState([])
    const [resourcesLoading, setResourcesLoading] = useState(false)

    // ── 角色 ──
    const [roles, setRoles] = useState([])
    const [rolesLoading, setRolesLoading] = useState(false)
    const [roleKeyword, setRoleKeyword] = useState('')

    // ── 角色表单（新增/编辑） ──
    const [roleFormOpen, setRoleFormOpen] = useState(false)
    const [editingRole, setEditingRole] = useState(null)
    const [roleForm] = Form.useForm()

    // ── 选中角色 → 右侧权限树 ──
    const [activeRole, setActiveRole] = useState(null)
    const [grantedKeys, setGrantedKeys] = useState([])
    const [checkedKeys, setCheckedKeys] = useState([])
    const [treeSearch, setTreeSearch] = useState('')
    // 权限树 tab：左树点条目 → 右侧详情（CEO 2026-11-12 定版）
    const [treeDetailId, setTreeDetailId] = useState(null)
    const [granting, setGranting] = useState(false)
    const [treeLoading, setTreeLoading] = useState(false)
    const [treeExpandedKeys, setTreeExpandedKeys] = useState([])

    // ── 权限点 CRUD ──
    const pointActionRef = useRef(undefined)
    const [pointSearchForm] = Form.useForm()
    const [pointSearchParams, setPointSearchParams] = useState({})
    const [pointSelectedKeys, setPointSelectedKeys] = useState([])
    const [pointSelectedRows, setPointSelectedRows] = useState([])
    const [appOptions, setAppOptions] = useState([])
    const [pointFormOpen, setPointFormOpen] = useState(false)
    const [editingPoint, setEditingPoint] = useState(null)
    const [pointForm] = Form.useForm()
    const [detailOpen, setDetailOpen] = useState(false)
    const [detailRecord, setDetailRecord] = useState(null)

    const loadResources = useCallback(async () => {
        setResourcesLoading(true)
        try {
            const res = await ctcAuthorizationApi.listAllResources()
            setResources(Array.isArray(res) ? res : [])
        } catch (e) {
            message.error('加载权限点失败：' + (e?.message || '未知错误'))
        } finally {
            setResourcesLoading(false)
        }
    }, [])

    const loadRoles = useCallback(async () => {
        setRolesLoading(true)
        try {
            const res = await ctcAuthorizationApi.listAllRoles()
            setRoles(Array.isArray(res) ? res : [])
        } catch (e) {
            message.error('加载角色失败：' + (e?.message || '未知错误'))
        } finally {
            setRolesLoading(false)
        }
    }, [])

    const loadAppOptions = useCallback(async () => {
        try {
            const res = await ctcAuthorizationApi.distinctApps()
            const codes = Array.isArray(res) ? res : []
            setAppOptions(codes.map((code) => ({value: code, label: code})))
        } catch {
            setAppOptions([])
        }
    }, [])

    useEffect(() => {
        loadResources()
        loadRoles()
        loadAppOptions()
    }, [loadResources, loadRoles, loadAppOptions])

    // ── 选中角色：拉它已挂载的权限点，作为树的勾选初值 ──
    const openRoleTree = useCallback(async (role) => {
        setActiveRole(role)
        setTreeSearch('')
        setTreeLoading(true)
        setGrantedKeys([])
        setCheckedKeys([])
        try {
            const res = await ctcAuthorizationApi.listRolePermissions(role.id)
            const ids = (Array.isArray(res) ? res : []).map((r) => r.id)
            setGrantedKeys(ids)
            setCheckedKeys(ids)
        } catch (e) {
            message.error('加载角色权限点失败：' + (e?.message || '未知错误'))
        } finally {
            setTreeLoading(false)
        }
    }, [])

    // ── 勾选保存：算差集后逐条 grant / revoke ──
    const handleSaveTree = useCallback(async () => {
        if (!activeRole?.id) return
        // 只认数字 id：应用分组节点不可勾选，但历史数据/异常态仍可能混入字符串 key，
        // Number('app:公共') = NaN 会让整批授权请求打空。
        const next = new Set(checkedKeys.map(Number).filter((n) => Number.isFinite(n)))
        const prev = new Set(grantedKeys.map(Number).filter((n) => Number.isFinite(n)))
        const toGrant = [...next].filter((k) => !prev.has(k))
        const toRevoke = [...prev].filter((k) => !next.has(k))
        if (toGrant.length === 0 && toRevoke.length === 0) {
            message.info('没有变更')
            return
        }
        setGranting(true)
        try {
            for (const id of toGrant) {
                await ctcAuthorizationApi.grantRoleResource(activeRole.id, id)
            }
            for (const id of toRevoke) {
                await ctcAuthorizationApi.revokeRoleResource(activeRole.id, id)
            }
            message.success(`已保存：+${toGrant.length} / -${toRevoke.length}`)
            setGrantedKeys([...next])
        } catch (e) {
            message.error('保存失败：' + (e?.message || '未知错误'))
        } finally {
            setGranting(false)
        }
    }, [activeRole, checkedKeys, grantedKeys])

    // ── 启用/停用滑块 ──
    const toggleRoleStatus = useCallback(async (role, next) => {
        try {
            await ctcAuthorizationApi.updateRole(role.id, {status: next ? 1 : 0})
            message.success(next ? `角色 ${role.roleName} 已启用` : `角色 ${role.roleName} 已停用`)
            await loadRoles()
        } catch (e) {
            message.error('更新状态失败：' + (e?.message || '未知错误'))
        }
    }, [loadRoles])

    const handleRoleCreate = useCallback(async () => {
        try {
            const values = await roleForm.validateFields()
            await ctcAuthorizationApi.createRole({
                roleCode: values.roleCode,
                roleName: values.roleName,
                description: values.description,
                status: values.enabled === false ? 0 : 1,
            })
            message.success('角色创建成功')
            setRoleFormOpen(false)
            roleForm.resetFields()
            await loadRoles()
        } catch (e) {
            if (e?.errorFields) return
            message.error('创建失败：' + (e?.message || '未知错误'))
        }
    }, [loadRoles, roleForm])

    const handleRoleEdit = useCallback(async () => {
        if (!editingRole?.id) return
        try {
            const values = await roleForm.validateFields()
            await ctcAuthorizationApi.updateRole(editingRole.id, {
                roleName: values.roleName,
                description: values.description,
                status: values.enabled === false ? 0 : 1,
            })
            message.success('角色已更新')
            setRoleFormOpen(false)
            setEditingRole(null)
            roleForm.resetFields()
            await loadRoles()
            if (activeRole?.id === editingRole.id) {
                setActiveRole((prev) =>
                    prev
                        ? {
                              ...prev,
                              roleName: values.roleName,
                              status: values.enabled === false ? 0 : 1,
                          }
                        : prev,
                )
            }
        } catch (e) {
            if (e?.errorFields) return
            message.error('更新失败：' + (e?.message || '未知错误'))
        }
    }, [activeRole, editingRole, loadRoles, roleForm])

    const handleRoleDelete = useCallback(async (role) => {
        try {
            await ctcAuthorizationApi.deleteRole(role.id)
            message.success('角色已删除')
            if (activeRole?.id === role.id) setActiveRole(null)
            await loadRoles()
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }, [activeRole, loadRoles])

    // ── 角色列表（端侧关键字过滤；量级几十条，不需要服务端分页/多条件筛选） ──
    const roleRows = useMemo(() => {
        const kw = roleKeyword.trim().toLowerCase()
        if (!kw) return roles
        return roles.filter(
            (r) =>
                (r.roleCode || '').toLowerCase().includes(kw) ||
                (r.roleName || '').toLowerCase().includes(kw),
        )
    }, [roles, roleKeyword])

    // ── 权限点表格 ──
    const pointRequest = useCallback(async (params) => {
        try {
            const res = await ctcAuthorizationApi.listResourcesByApp({
                appCode: pointSearchParams.appCode,
                type: pointSearchParams.type,
                keyword: pointSearchParams.keyword,
                pageNum: params.current,
                pageSize: params.pageSize,
            })
            const records = res?.data || []
            return {data: records, success: true, total: res?.total ?? records.length}
        } catch (e) {
            message.error('查询权限点失败：' + (e?.message || '未知错误'))
            return {data: [], success: false, total: 0}
        }
    }, [pointSearchParams])

    const pointColumns = [
        {
            title: '编码', dataIndex: 'resourceCode', width: 200, fixed: 'left',
            render: (_, r) => (
                <Tooltip title={r.resourceCode}>
                    <span style={{fontFamily: 'monospace'}}>{r.resourceCode}</span>
                </Tooltip>
            ),
        },
        {title: '名称', dataIndex: 'resourceName', width: 160},
        {title: '类型', dataIndex: 'resourceType', width: 80, render: (_, r) => typeTag(r.resourceType)},
        {
            title: '应用', dataIndex: 'appCode', width: 140,
            render: (_, r) => (r.appCode ? <Tag color="cyan">{r.appCode}</Tag> : <Tag color="default">公共</Tag>),
        },
        {title: '路径', dataIndex: 'path', width: 220, ellipsis: true},
        {
            title: '方法', dataIndex: 'method', width: 80,
            render: (_, r) => (r.method ? <Tag>{r.method}</Tag> : <span style={{color: '#bbb'}}>-</span>),
        },
        {
            title: '状态', dataIndex: 'status', width: 80,
            render: (_, r) => <Tag color={r.status === 1 ? 'success' : 'default'}>{r.status === 1 ? '启用' : '禁用'}</Tag>,
        },
        {title: '创建时间', dataIndex: 'createdAt', width: 170, render: (v) => v || '-'},
        {
            title: '操作', width: 160,
            render: (_, record) => [
                <Button key="edit" type="link" size="small" onClick={(e) => {
                    e.stopPropagation()
                    setEditingPoint(record)
                    pointForm.setFieldsValue(record)
                    setPointFormOpen(true)
                }}>编辑</Button>,
                <Popconfirm key="delete" title="确认删除该权限点？" okText="删除" cancelText="取消"
                            okButtonProps={{danger: true}}
                            onConfirm={async (e) => {
                                e?.stopPropagation?.()
                                try {
                                    await ctcAuthorizationApi.deleteResource(record.id)
                                    message.success('已删除')
                                    pointActionRef.current?.reload()
                                    loadResources()
                                } catch (err) {
                                    message.error('删除失败：' + (err?.message || '未知错误'))
                                }
                            }}>
                    <Button type="link" size="small" danger onClick={(e) => e.stopPropagation()}>删除</Button>
                </Popconfirm>,
                <Button key="detail" type="link" size="small" onClick={(e) => {
                    e.stopPropagation()
                    setDetailRecord(record)
                    setDetailOpen(true)
                }}>详情</Button>,
            ],
        },
    ]

    const permissionTree = useMemo(
        () => buildPermissionTree(resources, {search: treeSearch}),
        [resources, treeSearch],
    )

    // 树默认全展开 —— 角色面板要一眼看到「它挂了哪些权限点」，
    // 只给应用分组头还得再点一层，勾选动作就断了。
    useEffect(() => {
        setTreeExpandedKeys(collectAllKeys(permissionTree))
    }, [permissionTree])

    // 视口自适应高度：容器顶边到视口底部全部交给内容，列表/树向下占满（CEO 2026-11-11 定版）。
    // 不用 100vh 减固定 chrome 高度 —— 主壳头部高度变了这里自动跟着对。
    const pageRef = useRef(null)
    const [pageHeight, setPageHeight] = useState(undefined)
    useEffect(() => {
        const measure = () => {
            const el = pageRef.current
            if (!el) return
            const top = el.getBoundingClientRect().top
            setPageHeight(Math.max(420, window.innerHeight - top - 24))
        }
        measure()
        const t = setTimeout(measure, 300)
        window.addEventListener('resize', measure)
        return () => {
            clearTimeout(t)
            window.removeEventListener('resize', measure)
        }
    }, [])

    const renderRoles = () => (
        <div style={{display: 'flex', gap: 16, alignItems: 'stretch', height: '100%', minHeight: 0}}>
            {/* 左：角色列表 —— 顶部搜索 + 新增，下面就是角色条目（CEO 2026-11-11 定版，
                不再上表格/多条件筛选：角色量级几十条，一张清单扫一眼就够了） */}
            <div style={{width: 320, flexShrink: 0, display: 'flex'}}>
                <Card size="small"
                      style={{width: '100%', display: 'flex', flexDirection: 'column'}}
                      styles={{body: {padding: 12, flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', gap: 10, overflow: 'hidden'}}}>
                    <div style={{display: 'flex', gap: 8}}>
                        <Input.Search
                            allowClear
                            placeholder="搜索角色编码 / 名称"
                            onSearch={setRoleKeyword}
                            onChange={(e) => !e.target.value && setRoleKeyword('')}
                            style={{flex: 1}}
                        />
                        <Button type="primary" icon={<PlusOutlined/>} onClick={() => {
                            setEditingRole(null)
                            roleForm.setFieldsValue({enabled: true})
                            setRoleFormOpen(true)
                        }}>新增</Button>
                    </div>
                    <div style={{overflowY: 'auto', flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', gap: 4}}>
                        {roleRows.map((r) => {
                            const selected = activeRole?.id === r.id
                            return (
                                <div
                                    key={r.id}
                                    onClick={() => openRoleTree(r)}
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 8,
                                        padding: '8px 10px',
                                        borderRadius: 8,
                                        cursor: 'pointer',
                                        background: selected ? '#e6f4ff' : 'transparent',
                                        border: selected ? '1px solid #91caff' : '1px solid transparent',
                                    }}
                                >
                                    <div style={{flex: 1, minWidth: 0}}>
                                        <div style={{fontSize: 13, fontWeight: selected ? 600 : 400, color: '#1a1a2e', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>
                                            {r.roleName || r.roleCode}
                                            {r.status === 0 ? <Tag color="default" style={{margin: '0 0 0 6px', fontSize: 11}}>停用</Tag> : null}
                                        </div>
                                        <div style={{fontSize: 11, color: '#8c8c8c', fontFamily: 'monospace', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>
                                            {r.roleCode}{r.tenantCode ? ` · ${r.tenantCode}` : ''}
                                        </div>
                                    </div>
                                    {/* 滑块 = 启用/停用（真调 PUT /roles） */}
                                    <span onClick={(e) => e.stopPropagation()}>
                                        <Switch
                                            size="small"
                                            checked={r.status === 1}
                                            onChange={(checked) => toggleRoleStatus(r, checked)}
                                        />
                                    </span>
                                    <span onClick={(e) => e.stopPropagation()}>
                                        <Dropdown
                                            trigger={['click']}
                                            menu={{
                                                items: [
                                                    {key: 'edit', icon: <AuditOutlined/>, label: '编辑'},
                                                    {type: 'divider'},
                                                    {key: 'delete', icon: <DeleteOutlined/>, label: '删除', danger: true},
                                                ],
                                                onClick: ({key}) => {
                                                    if (key === 'edit') {
                                                        setEditingRole(r)
                                                        roleForm.setFieldsValue({
                                                            roleCode: r.roleCode,
                                                            roleName: r.roleName,
                                                            description: r.description,
                                                            enabled: r.status !== 0,
                                                        })
                                                        setRoleFormOpen(true)
                                                    } else if (key === 'delete') {
                                                        Modal.confirm({
                                                            title: `删除角色 ${r.roleName}？`,
                                                            okText: '删除',
                                                            okButtonProps: {danger: true},
                                                            cancelText: '取消',
                                                            onOk: () => handleRoleDelete(r),
                                                        })
                                                    }
                                                },
                                            }}
                                        >
                                            <Button type="text" size="small" icon={<EllipsisOutlined/>}/>
                                        </Dropdown>
                                    </span>
                                </div>
                            )
                        })}
                        {roleRows.length === 0 ? (
                            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE}
                                   description={roleKeyword ? '没有匹配的角色' : '还没有角色，点「新增」创建'}
                                   style={{margin: '24px 0'}}/>
                        ) : null}
                    </div>
                </Card>
            </div>

            {/* 右：选中角色的权限点树 */}
            <div style={{flex: 1, minWidth: 0, display: 'flex'}}>
                <Card
                    size="small"
                    style={{width: '100%', display: 'flex', flexDirection: 'column'}}
                    title={activeRole ? `权限点 — ${activeRole.roleName}` : '权限点'}
                    extra={activeRole ? (
                        <Space>
                            <Button size="small" onClick={() => setCheckedKeys(collectResourceIds(permissionTree))}>全选</Button>
                            <Button size="small" onClick={() => setCheckedKeys([])}>清空</Button>
                            <Button size="small" type="primary" loading={granting} onClick={handleSaveTree}>保存</Button>
                        </Space>
                    ) : null}
                    styles={{body: {padding: '12px 8px 12px 12px', flex: 1, minHeight: 0, overflowY: 'auto'}}}
                >
                    {!activeRole ? (
                        <Empty
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                            description="点左侧角色，这里展开它挂载的权限点树"
                            style={{margin: '24px 0'}}
                        />
                    ) : (
                        <>
                            <div style={{marginBottom: 8}}>
                                <Input.Search
                                    allowClear
                                    size="small"
                                    placeholder="按编码 / 名称过滤权限点"
                                    onSearch={setTreeSearch}
                                    onChange={(e) => !e.target.value && setTreeSearch('')}
                                />
                            </div>
                            <div style={{fontSize: 12, color: '#666', marginBottom: 8}}>
                                已挂载 <b>{grantedKeys.length}</b> / 共 {resources.length} 个权限点 —— 勾选即挂载，取消即卸载。
                            </div>
                            <Tree
                                checkable
                                blockNode
                                treeData={permissionTree}
                                checkedKeys={checkedKeys}
                                onCheck={(keys) => setCheckedKeys(Array.isArray(keys) ? keys : keys.checked)}
                                expandedKeys={treeExpandedKeys}
                                onExpand={(keys) => setTreeExpandedKeys(keys)}
                                loading={treeLoading}
                            />
                        </>
                    )}
                </Card>
            </div>
        </div>
    )

    const renderPoints = () => (
        <div style={{display: 'flex', flexDirection: 'column', gap: 12}}>
            <Card size="small" styles={{body: {padding: '12px 16px'}}}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12}}>
                    <Form form={pointSearchForm} layout="inline">
                        <Form.Item name="appCode" label="应用">
                            <Select placeholder="全部应用" allowClear style={{width: 180}}
                                    options={appOptions} showSearch optionFilterProp="label"/>
                        </Form.Item>
                        <Form.Item name="type" label="类型">
                            <Select placeholder="全部类型" allowClear style={{width: 120}}
                                    options={Object.entries(TYPE_MAP).map(([k, v]) => ({value: Number(k), label: v.text}))}/>
                        </Form.Item>
                        <Form.Item name="keyword" label="关键字">
                            <Input placeholder="编码 / 名称" allowClear style={{width: 180}}/>
                        </Form.Item>
                        <Form.Item name="status" label="状态">
                            <Select placeholder="全部" allowClear style={{width: 100}}
                                    options={[{value: 1, label: '启用'}, {value: 0, label: '禁用'}]}/>
                        </Form.Item>
                        <Form.Item>
                            <Space>
                                <Button type="primary" icon={<SearchOutlined/>} onClick={() => {
                                    const v = pointSearchForm.getFieldsValue()
                                    const p = {}
                                    if (v.appCode) p.appCode = v.appCode
                                    if (v.type !== undefined && v.type !== null) p.type = v.type
                                    if (v.keyword) p.keyword = v.keyword
                                    if (v.status !== undefined && v.status !== null) p.status = v.status
                                    setPointSearchParams(p)
                                }}>查询</Button>
                                <Button icon={<UndoOutlined/>} onClick={() => {
                                    pointSearchForm.resetFields()
                                    setPointSearchParams({})
                                }}>重置</Button>
                            </Space>
                        </Form.Item>
                    </Form>
                    <Space style={{flexShrink: 0}}>
                        <Button type="primary" icon={<PlusOutlined/>} onClick={() => {
                            setEditingPoint(null)
                            pointForm.resetFields()
                            pointForm.setFieldsValue({resourceType: 3, status: 1, sortOrder: 0})
                            setPointFormOpen(true)
                        }}>新增</Button>
                        <Button danger icon={<DeleteOutlined/>} disabled={pointSelectedKeys.length === 0}
                                onClick={async () => {
                                    try {
                                        await Promise.all(pointSelectedRows.map((r) => ctcAuthorizationApi.deleteResource(r.id)))
                                        message.success(`已删除 ${pointSelectedRows.length} 条`)
                                        setPointSelectedKeys([])
                                        setPointSelectedRows([])
                                        pointActionRef.current?.reload()
                                        loadResources()
                                    } catch (e) {
                                        message.error('批量删除失败：' + (e?.message || '未知错误'))
                                    }
                                }}>批量删除</Button>
                        <Button icon={<ReloadOutlined/>} onClick={() => pointActionRef.current?.reload()}>刷新</Button>
                    </Space>
                </div>
            </Card>

            <ProTable
                actionRef={pointActionRef}
                rowKey="id"
                columns={pointColumns}
                request={pointRequest}
                params={pointSearchParams}
                search={false}
                size="small"
                options={false}
                toolBarRender={false}
                pagination={{pageSize: 10, showSizeChanger: true, showTotal: (t) => `共 ${t} 条`}}
                dateFormatter="string"
                rowSelection={{
                    selectedRowKeys: pointSelectedKeys,
                    onChange: (keys, rows) => {
                        setPointSelectedKeys(keys)
                        setPointSelectedRows(rows)
                    },
                }}
                tableAlertRender={({selectedRowKeys: keys}) => (
                    <span>已选择 <a style={{fontWeight: 600}}>{keys.length}</a> 项</span>
                )}
            />
        </div>
    )

    const treeDetail = treeDetailId != null ? resources.find((r) => r.id === treeDetailId) : null

    // 层级路径：沿 parentId 往上走到底（防环），用于详情页面包屑
    const treeDetailPath = (() => {
        if (!treeDetail) return []
        const byId = new Map(resources.map((r) => [r.id, r]))
        const path = [treeDetail]
        const seen = new Set([treeDetail.id])
        let cur = treeDetail
        while (cur?.parentId && byId.has(cur.parentId) && !seen.has(cur.parentId)) {
            cur = byId.get(cur.parentId)
            seen.add(cur.id)
            path.unshift(cur)
        }
        return path
    })()

    const treeDetailChildren = treeDetail ? resources.filter((r) => r.parentId === treeDetail.id) : []

    const renderTree = () => (
        <div style={{display: 'flex', gap: 16, alignItems: 'stretch', height: '100%', minHeight: 0}}>
            {/* 左：权限树，拉到底部（CEO 2026-11-12 定版） */}
            <div style={{width: 460, flexShrink: 0, display: 'flex'}}>
                <Card size="small"
                      style={{width: '100%', display: 'flex', flexDirection: 'column'}}
                      styles={{body: {padding: 12, flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', gap: 10, overflow: 'hidden'}}}>
                    <div style={{display: 'flex', gap: 8, alignItems: 'center'}}>
                        <Input.Search
                            allowClear
                            size="small"
                            style={{flex: 1}}
                            placeholder="按编码 / 名称过滤"
                            onSearch={setTreeSearch}
                            onChange={(e) => !e.target.value && setTreeSearch('')}
                        />
                        <Button size="small" icon={<ReloadOutlined/>} onClick={loadResources}>刷新</Button>
                    </div>
                    <div style={{fontSize: 12, color: '#999'}}>
                        权限模型全貌：按「应用 → 父子层级」组织，共 {resources.length} 个。点条目看详情。
                    </div>
                    <div style={{flex: 1, minHeight: 0, overflowY: 'auto'}}>
                        <Tree
                            blockNode
                            treeData={permissionTree}
                            loading={resourcesLoading}
                            defaultExpandAll
                            selectedKeys={treeDetailId != null ? [treeDetailId] : []}
                            onSelect={(keys) => setTreeDetailId(keys.length ? keys[keys.length - 1] : null)}
                        />
                    </div>
                </Card>
            </div>

            {/* 右：选中条目的详情与扩展字段 */}
            <div style={{flex: 1, minWidth: 0, display: 'flex'}}>
                <Card
                    size="small"
                    style={{width: '100%', display: 'flex', flexDirection: 'column'}}
                    styles={{body: {padding: 16, flex: 1, minHeight: 0, overflowY: 'auto'}}}
                    title={treeDetail ? `权限点详情 — ${treeDetail.resourceName || treeDetail.resourceCode}` : '权限点详情'}
                >
                    {!treeDetail ? (
                        <Empty
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                            description="点左侧权限点，这里展示它的详情与扩展字段"
                            style={{margin: '48px 0'}}
                        />
                    ) : (
                        <div style={{display: 'flex', flexDirection: 'column', gap: 16}}>
                            {/* 层级路径面包屑 */}
                            <div style={{fontSize: 13, color: '#595959'}}>
                                {treeDetailPath.map((p, i) => (
                                    <span key={p.id}>
                                        {i > 0 ? <span style={{color: '#bfbfbf', margin: '0 6px'}}>/</span> : null}
                                        {p.resourceName || p.resourceCode}
                                    </span>
                                ))}
                                <span style={{fontFamily: 'monospace', fontSize: 11, color: '#999', marginLeft: 10}}>
                                    {treeDetail.resourceCode}
                                </span>
                            </div>
                            <Descriptions bordered size="small" column={2}>
                                <Descriptions.Item label="资源名称" span={2}>
                                    <span style={{fontWeight: 600}}>{treeDetail.resourceName || '-'}</span>
                                    <span style={{marginLeft: 8}}>{typeTag(treeDetail.resourceType)}</span>
                                </Descriptions.Item>
                                <Descriptions.Item label="编码" span={2}>
                                    <span style={{fontFamily: 'monospace'}}>{treeDetail.resourceCode}</span>
                                </Descriptions.Item>
                                <Descriptions.Item label="应用">
                                    {treeDetail.appCode
                                        ? <Tag color="cyan" style={{margin: 0}}>{treeDetail.appCode}</Tag>
                                        : <Tag color="default" style={{margin: 0}}>公共</Tag>}
                                </Descriptions.Item>
                                <Descriptions.Item label="状态">
                                    <Tag color={treeDetail.status === 1 ? 'success' : 'default'} style={{margin: 0}}>
                                        {treeDetail.status === 1 ? '启用' : '禁用'}
                                    </Tag>
                                </Descriptions.Item>
                                <Descriptions.Item label="ID">{treeDetail.id}</Descriptions.Item>
                                <Descriptions.Item label="排序">{treeDetail.sortOrder ?? 0}</Descriptions.Item>
                                <Descriptions.Item label="父权限">
                                    {treeDetail.parentId && resources.some((r) => r.id === treeDetail.parentId)
                                        ? (resources.find((r) => r.id === treeDetail.parentId)?.resourceName || treeDetail.parentId)
                                        : '顶层'}
                                </Descriptions.Item>
                                <Descriptions.Item label="子权限点">{treeDetailChildren.length} 个</Descriptions.Item>
                                <Descriptions.Item label="描述" span={2}>{treeDetail.description || '-'}</Descriptions.Item>
                                <Descriptions.Item label="创建时间">{treeDetail.createdAt || '-'}</Descriptions.Item>
                                <Descriptions.Item label="更新时间">{treeDetail.updatedAt || '-'}</Descriptions.Item>
                            </Descriptions>
                        </div>
                    )}
                </Card>
            </div>
        </div>
    )

    return (
        <div ref={pageRef} className="perm-mgmt-fullheight"
             style={{display: 'flex', flexDirection: 'column', minHeight: 420, height: pageHeight}}>
            <style>{`
                .perm-mgmt-fullheight .ant-tabs { display: flex; flex-direction: column; height: 100%; }
                .perm-mgmt-fullheight .ant-tabs-content-holder { flex: 1; min-height: 0; }
                .perm-mgmt-fullheight .ant-tabs-content,
                .perm-mgmt-fullheight .ant-tabs-tabpane { height: 100%; }
            `}</style>
            {/* 顶部横排三个 tab：角色 / 权限点 / 权限树（CEO 2026-11-11 定版） */}
            <Tabs
                style={{flex: 1, minHeight: 0}}
                activeKey={view}
                onChange={setView}
                items={VIEW_MENU.map((m) => ({
                    key: m.key,
                    label: (
                        <span style={{display: 'inline-flex', alignItems: 'center', gap: 6}}>
                            {m.icon}
                            {m.label}
                        </span>
                    ),
                    children: m.key === 'roles' ? renderRoles() : m.key === 'points' ? renderPoints() : renderTree(),
                }))}
                destroyInactiveTabPane
            />

            {/* 角色新增 / 编辑 */}
            <Modal
                title={editingRole ? `编辑角色 — ${editingRole.roleCode}` : '新增角色'}
                open={roleFormOpen}
                onOk={editingRole ? handleRoleEdit : handleRoleCreate}
                onCancel={() => {
                    setRoleFormOpen(false)
                    setEditingRole(null)
                    roleForm.resetFields()
                }}
                okText={editingRole ? '保存' : '创建'}
                destroyOnHidden
            >
                <Form form={roleForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="roleCode" label="角色编码"
                               rules={[{required: true, message: '请输入角色编码'}]}
                               extra="唯一标识，英文/下划线；创建后不可改">
                        <Input placeholder="如 admin / dev / viewer" disabled={!!editingRole}/>
                    </Form.Item>
                    <Form.Item name="roleName" label="角色名称" rules={[{required: true, message: '请输入角色名称'}]}>
                        <Input placeholder="如 管理员"/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} placeholder="可选"/>
                    </Form.Item>
                    <Form.Item name="enabled" label="启用" valuePropName="checked" initialValue>
                        <Switch checkedChildren="启用" unCheckedChildren="停用"/>
                    </Form.Item>
                </Form>
            </Modal>

            {/* 权限点新增 / 编辑 */}
            <Modal
                title={editingPoint ? `编辑权限点 — ${editingPoint.resourceCode}` : '新建权限点'}
                open={pointFormOpen}
                onOk={async () => {
                    try {
                        const values = await pointForm.validateFields()
                        if (editingPoint) {
                            await ctcAuthorizationApi.updateResource(editingPoint.id, values)
                            message.success('更新成功')
                        } else {
                            await ctcAuthorizationApi.createResource(values)
                            message.success('创建成功')
                        }
                        setPointFormOpen(false)
                        setEditingPoint(null)
                        pointForm.resetFields()
                        pointActionRef.current?.reload()
                        loadResources()
                    } catch (e) {
                        if (e?.errorFields) return
                        message.error('保存失败：' + (e?.message || '未知错误'))
                    }
                }}
                onCancel={() => {
                    setPointFormOpen(false)
                    setEditingPoint(null)
                    pointForm.resetFields()
                }}
                okText={editingPoint ? '保存' : '创建'}
                destroyOnHidden
            >
                <Form form={pointForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="resourceCode" label="资源编码"
                               rules={[{
                                   required: true,
                                   min: 2,
                                   max: 128,
                                   pattern: /^[a-zA-Z0-9_:.-]+$/,
                                   message: '2-128 位字母数字下划线冒号'
                               }]}
                               extra="业务唯一，如 user:read / order:create">
                        <Input placeholder="如 user:read" disabled={!!editingPoint}/>
                    </Form.Item>
                    <Form.Item name="resourceName" label="资源名称" rules={[{required: true, max: 128}]}>
                        <Input placeholder="如 用户查询"/>
                    </Form.Item>
                    <Form.Item name="resourceType" label="资源类型" rules={[{required: true}]} initialValue={3}>
                        <Select options={Object.entries(TYPE_MAP).map(([k, v]) => ({value: Number(k), label: v.text}))}/>
                    </Form.Item>
                    <Form.Item name="appCode" label="所属应用" extra="选择权限点归属的应用；不选则为「公共」">
                        <Select placeholder="公共（跨应用）" allowClear showSearch
                                optionFilterProp="label" options={appOptions}/>
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

            {/* 权限点详情 */}
            <Drawer
                title={detailRecord ? `权限点详情 — ${detailRecord.resourceCode}` : '权限点详情'}
                open={detailOpen}
                onClose={() => setDetailOpen(false)}
                size={720}
                destroyOnHidden
            >
                {detailRecord && (
                    <Descriptions title="权限点信息" size="small" bordered column={2}>
                        <Descriptions.Item label="资源ID">{detailRecord.id}</Descriptions.Item>
                        <Descriptions.Item label="资源编码">
                            <span style={{fontFamily: 'monospace'}}>{detailRecord.resourceCode}</span>
                        </Descriptions.Item>
                        <Descriptions.Item label="资源名称">{detailRecord.resourceName}</Descriptions.Item>
                        <Descriptions.Item label="资源类型">{typeTag(detailRecord.resourceType)}</Descriptions.Item>
                        <Descriptions.Item label="所属应用" span={2}>
                            {detailRecord.appCode
                                ? <Tag color="cyan">{detailRecord.appCode}</Tag>
                                : <Tag color="default">公共</Tag>}
                        </Descriptions.Item>
                        <Descriptions.Item label="URL 路径" span={2}>{detailRecord.path || '-'}</Descriptions.Item>
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
                        <Descriptions.Item label="租户" span={2}>{detailRecord.tenantCode || '公共'}</Descriptions.Item>
                        <Descriptions.Item label="描述" span={2}>{detailRecord.description || '-'}</Descriptions.Item>
                        <Descriptions.Item label="创建时间">{detailRecord.createdAt || '-'}</Descriptions.Item>
                        <Descriptions.Item label="更新时间">{detailRecord.updatedAt || '-'}</Descriptions.Item>
                    </Descriptions>
                )}
            </Drawer>
        </div>
    )
}
