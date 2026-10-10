import {useCallback, useRef, useState} from 'react'
import {ProTable} from '@ant-design/pro-components'
import {
    Button,
    Card,
    DatePicker,
    Descriptions,
    Drawer,
    Form,
    Input,
    Menu,
    message,
    Modal,
    Select,
    Space,
    Spin,
    Table,
    Tag,
    Tooltip
} from 'antd'
import {
    CheckCircleOutlined,
    DeleteOutlined,
    DownloadOutlined,
    FileTextOutlined,
    KeyOutlined,
    PlusOutlined,
    ReloadOutlined,
    SearchOutlined,
    StopOutlined,
    UndoOutlined,
    UserOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {ctcAcAccountApi, ctcAcLoginLogApi} from './api'
import {PageHeader, SearchInput} from '@/common/components/ui'

/**
 * 账号管理页 (4A AC 域) — 标准数据管理布局
 *
 * ┌──────────────────────────────────────────────┐
 * │ 账号管理       [+新增] [批量删除] [导出] [刷新] │
 * ├──────────────────────────────────────────────┤
 * │ 用户名□  状态▾  创建时间区间□  [查询] [重置]    │
 * ├──────────────────────────────────────────────┤
 * │ 数据表格                                      │
 * └──────────────────────────────────────────────┘
 *
 * 从 z-opc-main-starter-frontend/src/pages/ctc/4a/account/index.tsx 迁入.
 */
const STATUS_MAP = {
    0: {text: '禁用', color: 'default'},
    1: {text: '正常', color: 'success'},
    2: {text: '锁定', color: 'warning'},
}

const TYPE_MAP = {
    1: {text: '超级管理员', color: 'red'},
    2: {text: '租户管理员', color: 'volcano'},
    3: {text: '普通成员', color: 'blue'},
    4: {text: '临时账号', color: 'orange'},
}

const IDENTITY_MAP = {
    1: {text: '用户名', color: 'blue'},
    2: {text: '邮箱', color: 'purple'},
    3: {text: '手机', color: 'cyan'},
}

export default function AccountManagement({apiBaseURL}) {
    const actionRef = useRef(undefined)
    const [searchForm] = Form.useForm()
    const [searchParams, setSearchParams] = useState({})

    // 批量选择
    const [selectedRowKeys, setSelectedRowKeys] = useState([])
    const [selectedRows, setSelectedRows] = useState([])

    // 新建 / 重置密码
    const [createOpen, setCreateOpen] = useState(false)
    const [resetOpen, setResetOpen] = useState(false)
    const [resetTarget, setResetTarget] = useState(null)
    const [createForm] = Form.useForm()
    const [resetForm] = Form.useForm()

    // 详情抽屉
    const [detailOpen, setDetailOpen] = useState(false)
    const [detailAccount, setDetailAccount] = useState(null)
    const [detailTab, setDetailTab] = useState('info')
    const [loginLogs, setLoginLogs] = useState([])
    const [loginLogsLoading, setLoginLogsLoading] = useState(false)

    const fetchLoginLogs = async (accountId) => {
        if (!accountId) {
            setLoginLogs([])
            return
        }
        setLoginLogsLoading(true)
        try {
            const res = await ctcAcLoginLogApi.list({accountId: Number(accountId), pageSize: 50})
            setLoginLogs(res?.records || [])
        } catch {
            setLoginLogs([])
        } finally {
            setLoginLogsLoading(false)
        }
    }

    const openDetail = (r) => {
        setDetailAccount(r)
        setDetailTab('info')
        setDetailOpen(true)
        fetchLoginLogs(r.id)
    }

    const closeDetail = () => {
        setDetailOpen(false)
        setDetailAccount(null)
        setLoginLogs([])
    }

    const handleTabChange = (key) => {
        setDetailTab(key)
        if (key === 'logs' && detailAccount?.id) {
            fetchLoginLogs(detailAccount.id)
        }
    }

    // 搜索
    const handleSearch = useCallback(() => {
        const values = searchForm.getFieldsValue()
        const p = {}
        if (values.username) p.username = values.username
        if (values.status !== undefined) p.status = values.status
        if (values.dateRange) {
            p.startDate = values.dateRange[0].format('YYYY-MM-DD')
            p.endDate = values.dateRange[1].format('YYYY-MM-DD')
        }
        setSearchParams(p)
        actionRef.current?.reload()
    }, [searchForm])

    const handleReset = useCallback(() => {
        searchForm.resetFields()
        setSearchParams({})
        actionRef.current?.reload()
    }, [searchForm])

    // 数据请求
    const request_ = async (params) => {
        try {
            const res = await ctcAcAccountApi.listByTenant({
                tenant: searchParams.tenant,
                pageNum: params.current,
                pageSize: params.pageSize,
            })
            let records = (res || [])

            // 前端筛选
            const username = searchParams.username
            const status = searchParams.status
            const startDate = searchParams.startDate
            const endDate = searchParams.endDate

            records = records.filter(r => {
                if (username && !r.username?.includes(username)) return false
                if (status !== undefined && r.status !== status) return false
                if (startDate && r.createdAt && dayjs(r.createdAt).isBefore(startDate, 'day')) return false
                if (endDate && r.createdAt && dayjs(r.createdAt).isAfter(endDate, 'day')) return false
                return true
            })
            return {data: records, success: true, total: records.length}
        } catch (e) {
            message.error('获取账号列表失败：' + (e?.message || '未知错误'))
            return {data: [], success: false, total: 0}
        }
    }

    // 新建
    const handleCreate = async () => {
        try {
            const values = await createForm.validateFields()
            await ctcAcAccountApi.create(values)
            message.success('创建成功')
            setCreateOpen(false)
            createForm.resetFields()
            actionRef.current?.reload()
        } catch (e) {
            if (e?.errorFields) return
            message.error('创建失败：' + (e?.message || '未知错误'))
        }
    }

    // 启停
    const handleStatus = async (id, current) => {
        const next = current === 1 ? 0 : 1
        try {
            await ctcAcAccountApi.updateStatus(id, next)
            message.success(next === 1 ? '已启用' : '已停用')
            actionRef.current?.reload()
        } catch (e) {
            message.error('操作失败：' + (e?.message || '未知错误'))
        }
    }

    // 批量删除
    const handleBatchDelete = async () => {
        try {
            await Promise.all(selectedRows.map(r => ctcAcAccountApi.updateStatus(r.id, 0)))
            message.success(`已停用 ${selectedRows.length} 个账号`)
            setSelectedRowKeys([])
            setSelectedRows([])
            actionRef.current?.reload()
        } catch (e) {
            message.error('批量操作失败：' + (e?.message || '未知错误'))
        }
    }

    // 导出
    const handleExport = () => {
        const header = 'ID,账号,昵称,类型,邮箱,手机,租户,状态,创建时间\n'
        const rows = selectedRows.length > 0 ? selectedRows : []
        if (rows.length === 0) {
            message.warning('请先选择要导出的数据，或取消选择导出全部')
            return
        }
        const csv = header + rows.map(r =>
            [r.id, r.username, r.nickname, (TYPE_MAP[r.accountType ?? 3] || {text: String(r.accountType)}).text, r.email, r.phone, r.tenantCode, (STATUS_MAP[r.status ?? 1] || {text: String(r.status)}).text, r.createdAt].join(',')
        ).join('\n')
        const blob = new Blob(['\uFEFF' + csv], {type: 'text/csv;charset=utf-8'})
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `accounts_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
        a.click()
        URL.revokeObjectURL(url)
        message.success(`已导出 ${rows.length} 条数据`)
    }

    // 重置密码
    const openReset = (r) => {
        setResetTarget(r)
        resetForm.resetFields()
        setResetOpen(true)
    }

    const handleResetPassword = async () => {
        if (!resetTarget?.id) return
        try {
            const values = await resetForm.validateFields()
            await ctcAcAccountApi.resetPassword(resetTarget.id, {newPassword: values.newPassword})
            message.success(`已重置账号 ${resetTarget.username} 的密码`)
            setResetOpen(false)
            resetForm.resetFields()
        } catch (e) {
            if (e?.errorFields) return
            message.error('重置失败：' + (e?.message || '未知错误'))
        }
    }

    const loginLogColumns = [
        {title: '登录标识', dataIndex: 'identifier', width: 140},
        {
            title: '登录方式', dataIndex: 'identityType', width: 90,
            render: (_, r) => {
                const m = IDENTITY_MAP[r.identityType] || {text: `${r.identityType}`, color: 'default'}
                return <Tag color={m.color}>{m.text}</Tag>
            },
        },
        {
            title: '结果', dataIndex: 'loginStatus', width: 80,
            render: (_, r) => (
                <Tag color={r.loginStatus === 1 ? 'success' : 'error'}>
                    {r.loginStatus === 1 ? '成功' : '失败'}
                </Tag>
            ),
        },
        {title: '失败原因', dataIndex: 'failureReason', ellipsis: true},
        {title: '客户端IP', dataIndex: 'clientIp', width: 120},
        {
            title: '登录时间',
            dataIndex: 'loginTime',
            width: 170,
            render: (v) => v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
        },
    ]

    const columns = [
        {title: 'ID', dataIndex: 'id', width: 60, search: false},
        {title: '账号', dataIndex: 'username', width: 120, fixed: 'left', search: false},
        {title: '昵称', dataIndex: 'nickname', width: 120, search: false},
        {
            title: '类型', dataIndex: 'accountType', width: 110, search: false,
            render: (_, r) => {
                const t = TYPE_MAP[r.accountType ?? 3] || {text: String(r.accountType ?? '未知'), color: 'default'}
                return <Tag color={t.color}>{t.text}</Tag>
            },
        },
        {title: '邮箱', dataIndex: 'email', width: 160, ellipsis: true, search: false},
        {title: '手机', dataIndex: 'phone', width: 120, search: false},
        {title: '租户', dataIndex: 'tenantCode', width: 100, search: false},
        {
            title: '状态', dataIndex: 'status', width: 80, search: false,
            render: (_, r) => {
                const s = STATUS_MAP[r.status ?? 1] || {text: String(r.status ?? '未知'), color: 'default'}
                return <Tag color={s.color}>{s.text}</Tag>
            },
        },
        {title: '创建时间', dataIndex: 'createdAt', width: 170, valueType: 'dateTime', search: false},
        {
            title: '操作', valueType: 'option', width: 220, fixed: 'right', search: false,
            render: (_, record) => [
                <Tooltip key="status" title={record.status === 1 ? '停用' : '启用'}>
                    <Button type="text" size="small" danger={record.status === 1}
                            icon={record.status === 1 ? <StopOutlined/> : <CheckCircleOutlined/>}
                            onClick={() => handleStatus(record.id, record.status ?? 1)}>
                        {record.status === 1 ? '停用' : '启用'}
                    </Button>
                </Tooltip>,
                <Tooltip key="reset" title="重置密码">
                    <Button type="text" size="small" icon={<KeyOutlined/>} onClick={() => openReset(record)}>
                        重置密码
                    </Button>
                </Tooltip>,
                <Tooltip key="detail" title="账号详情 / 登录日志">
                    <Button type="text" size="small" icon={<FileTextOutlined/>}
                            onClick={() => openDetail(record)}>
                        详情
                    </Button>
                </Tooltip>,
            ],
        },
    ]

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 12}}>
            <PageHeader
                title="账号管理"
                subtitle="4A AC 域 · 账号生命周期管理、状态控制与登录日志"
                breadcrumb={[{label: '4A 中心'}, {label: '账号管理'}]}
            />

            <Card size="small" styles={{body: {padding: '12px 16px'}}}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12}}>
                    <Form form={searchForm} layout="inline">
                    <Form.Item name="username" label="用户名">
                        <SearchInput
                            placeholder="请输入用户名"
                            size="large"
                            onChange={(v) => searchForm.setFieldValue('username', v)}
                        />
                    </Form.Item>
                    <Form.Item name="status" label="状态">
                        <Select placeholder="全部" allowClear style={{width: 120}}
                                options={[{value: 1, label: '正常'}, {value: 0, label: '禁用'}, {
                                    value: 2,
                                    label: '锁定'
                                }]}/>
                    </Form.Item>
                    <Form.Item name="dateRange" label="创建时间">
                        <DatePicker.RangePicker style={{width: 240}}/>
                    </Form.Item>
                    <Form.Item>
                        <Space>
                            <Button type="primary" icon={<SearchOutlined/>} onClick={handleSearch}>
                                查询
                            </Button>
                            <Button icon={<UndoOutlined/>} onClick={handleReset}>
                                重置
                            </Button>
                        </Space>
                    </Form.Item>
                </Form>
                    <Space style={{flexShrink: 0}}>
                        <Button type="primary" icon={<PlusOutlined/>} onClick={() => setCreateOpen(true)}>
                            新增
                        </Button>
                        <Button danger icon={<DeleteOutlined/>} disabled={selectedRowKeys.length === 0}
                                onClick={handleBatchDelete}>
                            批量停用
                        </Button>
                        <Button icon={<DownloadOutlined/>} onClick={handleExport}>
                            导出
                        </Button>
                        <Button icon={<ReloadOutlined/>} onClick={() => actionRef.current?.reload()}>
                            刷新
                        </Button>
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
            />

            <Modal title="新建账号" open={createOpen} onOk={handleCreate}
                   onCancel={() => setCreateOpen(false)} size="large" destroyOnHidden>
                <Form form={createForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="username" label="用户名" rules={[{required: true, min: 3, max: 32}]}>
                        <Input placeholder="3-32 位字母数字下划线"/>
                    </Form.Item>
                    <Form.Item name="password" label="初始密码" rules={[{required: true, min: 8}]}>
                        <Input.Password placeholder="至少 8 位"/>
                    </Form.Item>
                    <Form.Item name="nickname" label="昵称">
                        <Input placeholder="可选"/>
                    </Form.Item>
                    <Form.Item name="email" label="邮箱" rules={[{type: 'email'}]}>
                        <Input placeholder="可选"/>
                    </Form.Item>
                    <Form.Item name="phone" label="手机号">
                        <Input placeholder="可选"/>
                    </Form.Item>
                    <Form.Item name="accountType" label="账号类型" initialValue={3}>
                        <Select options={[
                            {value: 1, label: '超级管理员'},
                            {value: 2, label: '租户管理员'},
                            {value: 3, label: '普通成员'},
                            {value: 4, label: '临时账号'},
                        ]}/>
                    </Form.Item>
                    <Form.Item name="tenantCode" label="租户编码">
                        <Input placeholder="如 default / tenant1"/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal title={resetTarget ? `重置密码 — ${resetTarget.username}` : '重置密码'}
                   open={resetOpen} onOk={handleResetPassword} onCancel={() => setResetOpen(false)}
                   size="large" okText="确认重置" okButtonProps={{danger: true}} destroyOnHidden>
                <Form form={resetForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="newPassword" label="新密码"
                               rules={[{required: true, min: 8, message: '至少 8 位'}]}
                               extra="重置后建议通知账号持有人当面修改">
                        <Input.Password placeholder="至少 8 位"/>
                    </Form.Item>
                </Form>
            </Modal>

            <Drawer
                title={detailAccount ? `账号详情 — ${detailAccount.username}` : '账号详情'}
                open={detailOpen}
                onClose={closeDetail}
                size={900}
                destroyOnHidden
            >
                {detailAccount && (
                    <div style={{display: 'flex', gap: 16, minHeight: 480}}>
                        <div style={{width: 160, borderRight: '1px solid #f0f0f0', paddingRight: 8}}>
                            <Menu
                                mode="inline"
                                selectedKeys={[detailTab]}
                                onClick={({key}) => handleTabChange(key)}
                                style={{border: 'none', background: 'transparent'}}
                                items={[
                                    {key: 'info', icon: <UserOutlined/>, label: '基本信息'},
                                    {key: 'logs', icon: <FileTextOutlined/>, label: '登录日志'},
                                ]}
                            />
                        </div>

                        <div style={{flex: 1, minWidth: 0}}>
                            {detailTab === 'info' ? (
                                <Descriptions
                                    title="账号信息"
                                    size="small"
                                    bordered
                                    column={2}
                                >
                                    <Descriptions.Item label="账号ID">{detailAccount.id}</Descriptions.Item>
                                    <Descriptions.Item label="用户名">{detailAccount.username}</Descriptions.Item>
                                    <Descriptions.Item label="昵称">{detailAccount.nickname || '-'}</Descriptions.Item>
                                    <Descriptions.Item label="账号类型">
                                        <Tag color={TYPE_MAP[detailAccount.accountType ?? 3]?.color}>
                                            {TYPE_MAP[detailAccount.accountType ?? 3]?.text || detailAccount.accountType}
                                        </Tag>
                                    </Descriptions.Item>
                                    <Descriptions.Item label="邮箱">{detailAccount.email || '-'}</Descriptions.Item>
                                    <Descriptions.Item label="手机">{detailAccount.phone || '-'}</Descriptions.Item>
                                    <Descriptions.Item
                                        label="租户">{detailAccount.tenantCode || '-'}</Descriptions.Item>
                                    <Descriptions.Item label="状态">
                                        <Tag color={STATUS_MAP[detailAccount.status ?? 1]?.color}>
                                            {STATUS_MAP[detailAccount.status ?? 1]?.text}
                                        </Tag>
                                    </Descriptions.Item>
                                    <Descriptions.Item label="到期时间">
                                        {detailAccount.expireAt ? dayjs(detailAccount.expireAt).format('YYYY-MM-DD HH:mm:ss') : '永久'}
                                    </Descriptions.Item>
                                    <Descriptions.Item label="创建时间">
                                        {detailAccount.createdAt ? dayjs(detailAccount.createdAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
                                    </Descriptions.Item>
                                    <Descriptions.Item label="更新时间" span={2}>
                                        {detailAccount.updatedAt ? dayjs(detailAccount.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
                                    </Descriptions.Item>
                                </Descriptions>
                            ) : (
                                <div>
                                    <div style={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                        marginBottom: 12,
                                    }}>
                                        <span style={{fontWeight: 600}}>
                                            登录日志（{loginLogs.length}）
                                        </span>
                                        <Button size="small" icon={<ReloadOutlined/>}
                                                onClick={() => detailAccount.id && fetchLoginLogs(detailAccount.id)}>
                                            刷新
                                        </Button>
                                    </div>
                                    <Spin spinning={loginLogsLoading}>
                                        <Table
                                            rowKey="id"
                                            columns={loginLogColumns}
                                            dataSource={loginLogs}
                                            pagination={{pageSize: 10, size: 'small'}}
                                            size="small"
                                            locale={{emptyText: '暂无登录记录'}}
                                        />
                                    </Spin>
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </Drawer>
        </div>
    )
}
