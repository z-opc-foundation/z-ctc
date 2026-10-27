import {useEffect, useMemo, useState} from 'react'
import {Button, Card, Empty, Input, List, Select, Space, Tag, Tree, Typography} from 'antd'
import {ReloadOutlined} from '@ant-design/icons'
import {userPermissionApi} from '../_api/userPermission'
import { ListTable, TableToolbar } from '@yuku123/z-frontend-common/ui'
/**
 * 4A 用户权限 — 查看某用户拥有的角色 + 资源（权限点）树
 *
 * 后端:
 *   - /api/ctc/ac/accounts                                  — 用户列表
 *   - /api/ctc/authorization/users/{id}/roles               — 用户的角色列表
 *   - /api/ctc/authorization/users/{id}/permissions         — 用户的权限编码列表
 *   - /api/ctc/authorization/roles/{roleId}/resources       — 角色拥有的资源
 */
const {Text} = Typography

export default function UserPermissionPage({apiBaseURL}) {
    const [accounts, setAccounts] = useState([])
    const [accountLoading, setAccountLoading] = useState(false)
    const [selectedUserId, setSelectedUserId] = useState(null)
    const [roles, setRoles] = useState([])
    const [rolesLoading, setRolesLoading] = useState(false)
    const [permissions, setPermissions] = useState([])
    const [permissionsLoading, setPermissionsLoading] = useState(false)
    const [resourceTree, setResourceTree] = useState([])
    const [activeRoleId, setActiveRoleId] = useState(null)
    const [roleResourcesLoading, setRoleResourcesLoading] = useState(false)

    const loadAccounts = async () => {
        setAccountLoading(true)
        try {
            const res = await userPermissionApi.listAccounts({pageNum: 1, pageSize: 200})
            const list = Array.isArray(res) ? res : (res?.records || res?.data || [])
            setAccounts(list)
            if (list.length > 0 && !selectedUserId) {
                setSelectedUserId(list[0].id)
            }
        } catch {
            setAccounts([])
        } finally {
            setAccountLoading(false)
        }
    }

    useEffect(() => {
        loadAccounts()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useEffect(() => {
        if (!selectedUserId) {
            setRoles([])
            setPermissions([])
            return
        }
        setRolesLoading(true)
        setPermissionsLoading(true)
        userPermissionApi.listUserRoles(selectedUserId)
            .then(res => setRoles(Array.isArray(res) ? res : []))
            .catch(() => setRoles([]))
            .finally(() => setRolesLoading(false))
        userPermissionApi.listUserPermissions(selectedUserId)
            .then(res => setPermissions(Array.isArray(res) ? res : []))
            .catch(() => setPermissions([]))
            .finally(() => setPermissionsLoading(false))
    }, [selectedUserId])

    useEffect(() => {
        if (!activeRoleId) {
            setResourceTree([])
            return
        }
        setRoleResourcesLoading(true)
        userPermissionApi.listRoleResources(activeRoleId)
            .then(res => {
                const list = Array.isArray(res) ? res : []
                // 按 resourceType 分组
                const groups = {}
                list.forEach(r => {
                    const t = r.resourceType || 'OTHER'
                    if (!groups[t]) groups[t] = []
                    groups[t].push(r)
                })
                const tree = Object.entries(groups).map(([type, items]) => ({
                    title: `${type} (${items.length})`,
                    key: `type-${type}`,
                    children: items.map(r => ({
                        title: `${r.resourceName} (${r.resourceCode})`,
                        key: `res-${r.id}`,
                        raw: r,
                    })),
                }))
                setResourceTree(tree)
            })
            .catch(() => setResourceTree([]))
            .finally(() => setRoleResourcesLoading(false))
    }, [activeRoleId])

    const accountOptions = accounts.map(a => ({
        value: a.id,
        label: `${a.username || a.nickname || a.id} (${a.tenantCode || 'default'})`,
    }))

    // ── 角色列表查询过滤（lead 008 §2 规则 3，端侧过滤） ──
    // 过滤字段取角色条目上真实存在的字段：roleCode / roleName。
    const [roleKeyword, setRoleKeyword] = useState('')
    const filteredRoles = useMemo(() => {
        const kw = roleKeyword.trim().toLowerCase()
        if (!kw) return roles
        return roles.filter(
            (r) => `${r.roleCode || ''} ${r.roleName || ''}`.toLowerCase().includes(kw),
        )
    }, [roles, roleKeyword])

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 12}}>

            <TableToolbar
                left={
                    <Select
                        showSearch
                        placeholder="选择用户"
                        style={{width: 320}}
                        value={selectedUserId}
                        onChange={setSelectedUserId}
                        loading={accountLoading}
                        options={accountOptions}
                        filterOption={(input, opt) => opt.label.toLowerCase().includes(input.toLowerCase())}
                    />
                }
                right={<Button icon={<ReloadOutlined/>} onClick={loadAccounts}>刷新用户</Button>}
            />

            <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16}}>
                <Card size="small" title={`用户角色 (${roles.length})`} loading={rolesLoading}
                      extra={
                          <Input.Search
                              allowClear
                              placeholder="按角色编码 / 名称过滤"
                              style={{width: 220}}
                              onChange={(e) => setRoleKeyword(e.target.value)}
                          />
                      }>
                    {roles.length === 0 && !rolesLoading ? (
                        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该用户暂无角色"/>
                    ) : (
                        <List
                            size="small"
                            dataSource={filteredRoles}
                            renderItem={r => (
                                <List.Item
                                    actions={[
                                        <Button key="res" type="link" size="small"
                                                onClick={() => setActiveRoleId(r.id)}
                                                disabled={r.id === activeRoleId}>
                                            {r.id === activeRoleId ? '查看中' : '查看资源'}
                                        </Button>,
                                    ]}
                                >
                                    <List.Item.Meta
                                        title={
                                            <Space>
                                                <Text strong>{r.roleName || r.roleCode}</Text>
                                                <Tag color="blue" style={{margin: 0}}>{r.roleCode}</Tag>
                                            </Space>
                                        }
                                        description={r.description || '-'}
                                    />
                                </List.Item>
                            )}
                        />
                    )}
                </Card>

                <ListTable
                    cardTitle={`用户权限 (${permissions.length})`}
                    rows={permissions}
                    rowKey={(r, i) => r.code || r.resourceCode || r.id || i}
                    // 后端返回的权限对象里编码/名称可能是 code/name，也可能是 resourceCode/resourceName，
                    // 两套字段名都列上，否则用户按列上的字样搜却搜不到。
                    searchKeys={['code', 'name', 'resourceCode', 'resourceName']}
                    searchPlaceholder="按资源编码 / 名称过滤"
                    emptyTitle="该用户暂无直接权限"
                    columns={[
                        {
                            title: '资源编码', dataIndex: 'code', width: 200,
                            render: (_, r) => <span style={{fontFamily: 'monospace'}}>
                                {r.code || r.resourceCode || '-'}
                            </span>,
                        },
                        {
                            title: '资源名称', dataIndex: 'name', ellipsis: true,
                            render: (_, r) => r.name || r.resourceName || '-'
                        },
                    ]}
                    cardProps={{size: 'small', loading: permissionsLoading}}
                />

                <Card size="small" title={
                    activeRoleId
                        ? `角色资源 (${resourceTree.reduce((s, n) => s + (n.children?.length || 0), 0)})`
                        : '角色资源'
                } loading={roleResourcesLoading}>
                    {!activeRoleId ? (
                        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE}
                               description="左侧点击「查看资源」以查看该角色拥有的权限点"/>
                    ) : resourceTree.length === 0 && !roleResourcesLoading ? (
                        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该角色暂无资源"/>
                    ) : (
                        <Tree
                            treeData={resourceTree}
                            defaultExpandAll
                            selectable={false}
                            showLine
                            blockNode
                            height={420}
                        />
                    )}
                </Card>
            </div>
        </div>
    )
}