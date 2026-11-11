import {useCallback, useEffect, useMemo, useState} from 'react'
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
    Select,
    Space,
    Table,
    Tag,
    Tooltip,
} from 'antd'
import {DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {configureApiBaseURL, ctcAcDomainApi, ctcAcTenantApi, isDefaultDomain} from '../TenantManagement/api'

/**
 * 租户管理 —— 登录者自己所属的租户, 以「域」为主体, 不画租户树.
 *
 * 和「系统租户」的区别只是视角, 不是两套权限:
 *   本页     = 我登录后能看到的租户 (GET /tenants/list, 按 membership 分层)
 *   系统租户 = 全平台所有租户 (GET /tenants/list-all, 仅平台管理员)
 *
 * 为什么不用左树: 一个人通常只属于一个租户, 树里只有一个根节点 +
 * 几个域, 左边那 300px 换不来任何导航收益, 反而把「域」挤成树的叶子 ——
 * 域本身的编码/状态/描述在表格里一行就能看全。只有真的多于一个租户时,
 * 才在右上角出现租户切换 (Select), 默认永远是登录者自己的那个。
 *
 * 写操作 (新增/改/删域) 的边界在后端: 平台管理员或该租户 owner/admin.
 * 普通成员能看到按钮但提交会被拒 —— 提示文案直接透传后端结果, 不在这里猜角色。
 *
 * Props:
 *   - apiBaseURL?: string    后端 baseURL, 默认 '/api'
 */

const STATUS_MAP = {
    1: {text: '正常', color: 'success'},
    0: {text: '停用', color: 'default'},
}

export default function MyTenant({apiBaseURL}) {
    useEffect(() => {
        if (apiBaseURL) configureApiBaseURL(apiBaseURL)
    }, [apiBaseURL])

    const [tenants, setTenants] = useState([])
    const [activeCode, setActiveCode] = useState('')
    const [loadingTenants, setLoadingTenants] = useState(false)

    const [domains, setDomains] = useState([])
    const [loadingDomains, setLoadingDomains] = useState(false)

    // 新增 / 编辑共用一个表单: editing 为 null 表示新增
    const [formOpen, setFormOpen] = useState(false)
    const [editing, setEditing] = useState(null)
    const [form] = Form.useForm()

    const fetchTenants = useCallback(async () => {
        setLoadingTenants(true)
        try {
            const list = await ctcAcTenantApi.listAll()
            const safe = Array.isArray(list) ? list : []
            setTenants(safe)
            // 当前选中的租户不在列表里 (首次进入 / 被移出租户) ⇒ 落到第一个
            setActiveCode(prev => (safe.some(t => t.tenantCode === prev) ? prev : (safe[0]?.tenantCode || '')))
        } catch (e) {
            message.error('获取我的租户失败：' + (e?.message || '未知错误'))
            setTenants([])
            setActiveCode('')
        } finally {
            setLoadingTenants(false)
        }
    }, [])

    const fetchDomains = useCallback(async (tenantCode) => {
        if (!tenantCode) {
            setDomains([])
            return
        }
        setLoadingDomains(true)
        try {
            const list = await ctcAcDomainApi.listByTenant(tenantCode)
            setDomains(Array.isArray(list) ? list : [])
        } catch (e) {
            message.error('获取域列表失败：' + (e?.message || '未知错误'))
            setDomains([])
        } finally {
            setLoadingDomains(false)
        }
    }, [])

    useEffect(() => {
        fetchTenants()
    }, [fetchTenants])

    useEffect(() => {
        fetchDomains(activeCode)
    }, [activeCode, fetchDomains])

    const activeTenant = useMemo(
        () => tenants.find(t => t.tenantCode === activeCode) || null,
        [tenants, activeCode],
    )

    const openCreate = () => {
        setEditing(null)
        form.resetFields()
        form.setFieldsValue({status: 1})
        setFormOpen(true)
    }

    const openEdit = (domain) => {
        setEditing(domain)
        form.resetFields()
        form.setFieldsValue({
            domainCode: domain.domainCode,
            domainName: domain.domainName,
            description: domain.description,
            status: domain.status ?? 1,
        })
        setFormOpen(true)
    }

    const handleSubmit = async () => {
        if (!activeCode) return
        try {
            const values = await form.validateFields()
            if (editing) {
                await ctcAcDomainApi.update(editing.id, {
                    domainName: values.domainName,
                    description: values.description || '',
                    status: values.status ?? 1,
                })
                message.success('已保存')
            } else {
                await ctcAcDomainApi.create({
                    domainCode: values.domainCode,
                    domainName: values.domainName,
                    description: values.description || '',
                    tenantCode: activeCode,
                    status: values.status ?? 1,
                })
                message.success(`已新增域「${values.domainName}」`)
            }
            setFormOpen(false)
            setEditing(null)
            form.resetFields()
            await fetchDomains(activeCode)
        } catch (e) {
            if (e?.errorFields) return
            message.error('保存失败：' + (e?.message || '未知错误'))
        }
    }

    const handleDelete = async (domain) => {
        if (isDefaultDomain(activeCode, domain)) {
            message.warning('默认域受保护，不可删除')
            return
        }
        try {
            await ctcAcDomainApi.delete(domain.id)
            message.success('已删除')
            await fetchDomains(activeCode)
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    const columns = [
        {title: '域编码', dataIndex: 'domainCode', key: 'domainCode', width: 220},
        {title: '域名称', dataIndex: 'domainName', key: 'domainName', width: 200},
        {
            title: '状态', dataIndex: 'status', key: 'status', width: 100,
            render: (status) => {
                const meta = STATUS_MAP[status] || {text: String(status ?? '-'), color: 'default'}
                return <Tag color={meta.color}>{meta.text}</Tag>
            },
        },
        {title: '描述', dataIndex: 'description', key: 'description', ellipsis: true},
        {title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180},
        {
            title: '操作', key: 'action', width: 150,
            render: (_, record) => (
                <Space size={0}>
                    <Button type="link" size="small" onClick={() => openEdit(record)}>
                        编辑
                    </Button>
                    {isDefaultDomain(activeCode, record) ? (
                        <Tooltip title="默认域受保护，不可删除">
                            <Button type="link" size="small" danger disabled>
                                删除
                            </Button>
                        </Tooltip>
                    ) : (
                        <Popconfirm
                            title="确定删除该域？"
                            description="删除后不可恢复"
                            onConfirm={() => handleDelete(record)}
                            okText="确定"
                            cancelText="取消"
                        >
                            <Button type="link" size="small" danger>
                                删除
                            </Button>
                        </Popconfirm>
                    )}
                </Space>
            ),
        },
    ]

    if (!loadingTenants && tenants.length === 0) {
        return (
            <Card size="small">
                <Empty description="当前账号还未加入任何租户"/>
            </Card>
        )
    }

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 16}}>
            <Card
                size="small"
                title="我的租户"
                extra={
                    <Space size={8}>
                        {tenants.length > 1 && (
                            <Select
                                size="small"
                                style={{minWidth: 200}}
                                value={activeCode || undefined}
                                onChange={setActiveCode}
                                options={tenants.map(t => ({
                                    value: t.tenantCode,
                                    label: `${t.tenantName || t.tenantCode} (${t.tenantCode})`,
                                }))}
                            />
                        )}
                        <Tooltip title="刷新">
                            <Button size="small" type="text" icon={<ReloadOutlined/>} onClick={() => {
                                fetchTenants()
                                fetchDomains(activeCode)
                            }}/>
                        </Tooltip>
                    </Space>
                }
            >
                {activeTenant ? (
                    // 列数按容器宽度退让：写死 4 列在窄屏会把「租户编码」压成一列一个字
                    <Descriptions size="small" bordered column={{xs: 1, sm: 2, md: 2, lg: 4}}>
                        <Descriptions.Item label="租户编码">{activeTenant.tenantCode}</Descriptions.Item>
                        <Descriptions.Item label="租户名称">{activeTenant.tenantName || '-'}</Descriptions.Item>
                        <Descriptions.Item label="状态">
                            <Tag color={(STATUS_MAP[activeTenant.status ?? 1] || {}).color}>
                                {(STATUS_MAP[activeTenant.status ?? 1] || {text: '-'}).text}
                            </Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="创建时间">{activeTenant.createdAt || '-'}</Descriptions.Item>
                    </Descriptions>
                ) : (
                    <Empty description={loadingTenants ? '加载中...' : '暂无租户'}/>
                )}
            </Card>

            <Card
                size="small"
                title="域"
                extra={
                    <Space size={4}>
                        <Button
                            type="primary"
                            size="small"
                            icon={<PlusOutlined/>}
                            disabled={!activeCode}
                            onClick={openCreate}
                        >
                            新增域
                        </Button>
                    </Space>
                }
            >
                <Table
                    columns={columns}
                    dataSource={domains}
                    rowKey={r => r.id ?? r.domainCode}
                    loading={loadingDomains}
                    size="small"
                    scroll={{x: 880}}
                    pagination={{pageSize: 10, showSizeChanger: false, hideOnSinglePage: true}}
                    locale={{emptyText: '该租户下暂无域，点击右上「新增域」创建'}}
                />
            </Card>

            <Modal
                title={editing ? `编辑域「${editing.domainName}」` : `为「${activeTenant?.tenantName || activeCode}」新增域`}
                open={formOpen}
                onOk={handleSubmit}
                onCancel={() => {
                    setFormOpen(false)
                    setEditing(null)
                    form.resetFields()
                }}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item
                        name="domainCode"
                        label="域编码"
                        rules={editing ? [] : [
                            {required: true, message: '请输入域编码'},
                            {pattern: /^[a-z0-9_-]+$/, message: '仅允许小写字母、数字、下划线、短横线'},
                        ]}
                        extra={editing ? '编码创建后不可修改' : '租户内唯一，建议形如 prod / staging / dev'}
                    >
                        <Input placeholder="如 prod / staging" disabled={!!editing}/>
                    </Form.Item>
                    <Form.Item name="domainName" label="域名称" rules={[{required: true, message: '请输入域名称'}]}>
                        <Input placeholder="如 生产环境"/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={3} placeholder="可选"/>
                    </Form.Item>
                    <Form.Item name="status" label="状态" initialValue={1}>
                        <Select
                            options={[
                                {value: 1, label: '正常'},
                                {value: 0, label: '停用'},
                            ]}
                        />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}
