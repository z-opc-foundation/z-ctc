import {useEffect, useState} from 'react'
import {Card, Col, Row, Select, Space, Statistic, Table, Tabs, Tag, Tooltip} from 'antd'
import {LoginOutlined, ReloadOutlined, SearchOutlined} from '@ant-design/icons'
import dayjs from 'dayjs'
import {loginLogApi} from '../_api/loginLog'
import {PageHeader, SearchInput} from '@/common/components/ui'

/**
 * 审计中心 — 按类型 Tabs 隔离日志
 *
 * 当前已实现日志类型:
 *   - 登录日志 (z-ctc-ac login log)
 *
 * 未来可扩展:
 *   - 操作日志 (谁在什么时间做了什么, z-ctc-ac authz audit)
 *   - 资源访问日志 (谁访问了哪个资源, z-ops access)
 *   - 系统日志 (应用错误, 平台告警)
 *
 * 后端:
 *   GET /api/ctc/ac/login-log/list?page=&size=&identifier=&tenantCode=&loginStatus=
 *   GET /api/ctc/ac/login-log/recent
 */
// 原签名带一个未使用的 apiBaseURL 形参（全文件仅签名处出现一次，函数体内从未读取）：
// noImplicitAny 下被推成必填 props，而 SystemShell 渲染 <CtcAuditPage/> 不传它 → TS2741。
// 删除该形参，不影响任何行为。
export default function AuditPage() {
    return (
        <div>
            <PageHeader
                title="审计中心"
                subtitle="按日志类型隔离. 登录行为、操作行为、资源访问等不同维度的审计追溯."
                breadcrumb={[{label: '4A 中心'}, {label: '审计中心'}]}
            />
            <Tabs
                defaultActiveKey="login"
                size="small"
                items={[
                    {
                        key: 'login',
                        label: <span><LoginOutlined/> 登录日志</span>,
                        children: <LoginLogTab/>,
                    },
                    {
                        key: 'operation',
                        label: <span><LoginOutlined/> 操作日志</span>,
                        children: (
                            <Card>
                                <div style={{padding: 40, textAlign: 'center', color: '#8a8f98'}}>
                                    <div style={{marginBottom: 8, fontSize: 14}}>操作日志 - 规划中</div>
                                    <div style={{fontSize: 12}}>
                                        未来记录: 谁在什么时间对什么资源做了什么操作<br/>
                                        (创建/修改/删除/审批/授权等)
                                    </div>
                                </div>
                            </Card>
                        ),
                        disabled: true,
                    },
                ]}
            />
        </div>
    )
}

/**
 * 登录日志 Tab — 完整统计 + 单个列表
 *
 * 单列表 (无重复):
 *   - 顶部 4 个统计 (最近成功/失败/总 + 总日志数)
 *   - 1 个主表格 (带筛选 + 分页 + 完整 9 字段)
 */
function LoginLogTab() {
    const [recent, setRecent] = useState([])
    const [recentLoading, setRecentLoading] = useState(false)
    const [rows, setRows] = useState([])
    const [loading, setLoading] = useState(false)
    const [total, setTotal] = useState(0)
    const [params, setParams] = useState({page: 1, size: 20})
    const [filter, setFilter] = useState({identifier: '', tenantCode: '', loginStatus: undefined})

    const loadRecent = async () => {
        setRecentLoading(true)
        try {
            const res = await loginLogApi.recent()
            setRecent(Array.isArray(res) ? res : [])
        } catch {
            setRecent([])
        } finally {
            setRecentLoading(false)
        }
    }

    const loadList = async (override) => {
        const p = override || params
        setLoading(true)
        try {
            const res = await loginLogApi.list({...filter, ...p})
            setRows(res?.list || [])
            setTotal(res?.total || 0)
        } catch {
            setRows([])
            setTotal(0)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadRecent()
        loadList({page: 1, size: 20})
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const onSearch = () => loadList({page: 1, size: 20})
    const onPageChange = (page, size) => {
        const next = {page, size: size || params.size}
        setParams(next)
        loadList(next)
    }

    // 统计从最近 10 条 + 列表第一条快速展示, 总数来自 list 接口
    const recentSuccess = recent.filter(r => r.loginStatus === 1).length
    const recentFail = recent.filter(r => r.loginStatus !== 1).length

    const columns = [
        {title: 'ID', dataIndex: 'id', width: 70},
        {title: '登录标识', dataIndex: 'identifier', width: 160},
        {
            title: '方式', dataIndex: 'identityType', width: 90,
            render: v => <Tag
                color={v === 1 ? 'blue' : v === 2 ? 'purple' : 'cyan'}>{v === 1 ? '用户名' : v === 2 ? '邮箱' : v === 3 ? '手机' : v || '-'}</Tag>
        },
        {
            title: '结果', dataIndex: 'loginStatus', width: 80,
            render: v => <Tag color={v === 1 ? 'success' : 'error'}>{v === 1 ? '成功' : '失败'}</Tag>
        },
        {
            title: '失败原因', dataIndex: 'failureReason', ellipsis: true,
            render: v => v ? <Tooltip title={v}>{v}</Tooltip> : <span style={{color: '#bbb'}}>-</span>
        },
        {title: 'IP', dataIndex: 'clientIp', width: 130},
        {title: '租户', dataIndex: 'tenantCode', width: 100},
        {title: '域', dataIndex: 'domainCode', width: 100},
        {
            title: 'UA', dataIndex: 'userAgent', ellipsis: true,
            render: v => v ? <Tooltip title={v}><span style={{
                fontFamily: 'monospace',
                fontSize: 12
            }}>{v.length > 60 ? v.slice(0, 60) + '…' : v}</span></Tooltip> : '-'
        },
        {
            title: '登录时间', dataIndex: 'loginTime', width: 170,
            render: v => v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
        },
    ]

    return (
        <Space orientation="vertical" size={12} style={{width: '100%'}}>
            <Row gutter={12}>
                <Col span={6}>
                    <Card size="small">
                        <Statistic title="最近成功登录 (10 条内)" value={recentSuccess}
                                   styles={{content: {color: '#52c41a', fontSize: 20}}}
                                   prefix={<LoginOutlined/>}/>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small">
                        <Statistic title="最近失败登录 (10 条内)" value={recentFail}
                                   styles={{content: {color: '#ff4d4f', fontSize: 20}}}/>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small">
                        <Statistic title="最近 10 条" value={recent.length}
                                   styles={{content: {fontSize: 20}}}/>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small">
                        <Statistic title="总日志数" value={total} loading={loading}
                                   styles={{content: {fontSize: 20}}}/>
                    </Card>
                </Col>
            </Row>

            <Card
                size="small"
                title={
                    <Space>
                        <span>登录日志</span>
                        <Tag color="cyan">{total} 条</Tag>
                    </Space>
                }
                extra={
                    <Space>
                        <a onClick={loadRecent}><ReloadOutlined spin={recentLoading}/> 刷新最近</a>
                        <a onClick={() => loadList()}><ReloadOutlined spin={loading}/> 刷新列表</a>
                    </Space>
                }
            >
                <Space wrap style={{marginBottom: 12}}>
                    <SearchInput
                        placeholder="登录标识"
                        value={filter.identifier}
                        onChange={(v) => setFilter({...filter, identifier: v})}
                        onSearch={onSearch}
                        size="large"
                    />
                    <SearchInput
                        placeholder="租户编码"
                        value={filter.tenantCode}
                        onChange={(v) => setFilter({...filter, tenantCode: v})}
                        onSearch={onSearch}
                        size="large"
                    />
                    <Select placeholder="结果" allowClear style={{width: 110}}
                            value={filter.loginStatus}
                            onChange={v => setFilter({...filter, loginStatus: v})}
                            options={[{value: 1, label: '成功'}, {value: 0, label: '失败'}]}/>
                    <a onClick={onSearch}><SearchOutlined/> 查询</a>
                </Space>
                <Table
                    size="small"
                    rowKey="id"
                    loading={loading}
                    dataSource={rows}
                    columns={columns}
                    pagination={{
                        current: params.page,
                        pageSize: params.size,
                        total,
                        showSizeChanger: true,
                        showTotal: t => `共 ${t} 条`,
                        onChange: onPageChange,
                    }}
                />
            </Card>
        </Space>
    )
}
