import {useEffect, useRef, useState} from 'react'
import {ProTable} from '@ant-design/pro-components'
import {Button, Card, Form, Input, message, Popconfirm, Select, Space, Tag} from 'antd'
import {DeleteOutlined, ReloadOutlined, SearchOutlined, UndoOutlined} from '@ant-design/icons'
import {ctcAuthorizationApi} from '../_api/ctcAuthorization'
import {metaAppApi} from '../_api/metaApp'
import {PageHeader} from '@/common/components/ui'

/**
 * 4A 资源管理 — 4A AUTHZ 域所有资源（权限点）的统一视图
 *
 * 后端: GET /api/ctc/authorization/resources/page?keyword=&pageNum=&pageSize=
 *      返回: Result<Map<data, total>>
 */
const TYPE_MAP = {
    1: {text: '菜单', color: 'blue'},
    2: {text: '数据', color: 'purple'},
    3: {text: 'API', color: 'orange'},
    4: {text: '按钮', color: 'green'},
}

export default function ResourcePage({apiBaseURL}) {
    const actionRef = useRef(undefined)
    const [searchForm] = Form.useForm()
    const [searchParams, setSearchParams] = useState({})
    const [appOptions, setAppOptions] = useState([])

    useEffect(() => {
        metaAppApi.listApplications({status: 1}).then(res => {
            const opts = (res?.records || []).map(a => ({
                value: a.appCode,
                label: `${a.appName} (${a.appCode})`,
            }))
            setAppOptions(opts)
        }).catch(() => setAppOptions([]))
    }, [])

    const request_ = async (params) => {
        try {
            const res = await ctcAuthorizationApi.listResourcesByApp({
                appCode: searchParams.appCode,
                type: searchParams.type,
                keyword: searchParams.keyword,
                pageNum: params.current,
                pageSize: params.pageSize,
            })
            const records = res?.data || []
            const total = res?.total ?? records.length
            return {data: records, success: true, total}
        } catch (e) {
            message.error('查询资源失败：' + (e?.message || '未知错误'))
            return {data: [], success: false, total: 0}
        }
    }

    const handleSearch = () => {
        const v = searchForm.getFieldsValue()
        const p = {}
        if (v.appCode) p.appCode = v.appCode
        if (v.type !== undefined && v.type !== null) p.type = v.type
        if (v.keyword) p.keyword = v.keyword
        setSearchParams(p)
        actionRef.current?.reload()
    }

    const handleReset = () => {
        searchForm.resetFields()
        setSearchParams({})
        actionRef.current?.reload()
    }

    const handleDelete = async (id) => {
        try {
            await ctcAuthorizationApi.deleteResource(id)
            message.success('已删除')
            actionRef.current?.reload()
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    const columns = [
        {title: 'ID', dataIndex: 'id', width: 70, search: false},
        {
            title: '资源编码', dataIndex: 'resourceCode', width: 220, fixed: 'left', search: false,
            render: (_, r) => <span style={{fontFamily: 'monospace'}}>{r.resourceCode}</span>,
        },
        {title: '资源名称', dataIndex: 'resourceName', width: 160, search: false},
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
        {title: 'URL', dataIndex: 'path', width: 220, ellipsis: true, search: false},
        {
            title: '方法', dataIndex: 'method', width: 80, search: false,
            render: (_, r) => r.method ? <Tag>{r.method}</Tag> : <span style={{color: '#bbb'}}>-</span>,
        },
        {
            title: '状态', dataIndex: 'status', width: 80, search: false,
            render: (_, r) => <Tag color={r.status === 1 ? 'success' : 'default'}>
                {r.status === 1 ? '启用' : '禁用'}
            </Tag>,
        },
        {title: '租户', dataIndex: 'tenantCode', width: 100, search: false},
        {title: '创建时间', dataIndex: 'createdAt', width: 170, valueType: 'dateTime', search: false},
        {
            title: '操作', valueType: 'option', width: 120, fixed: 'right', search: false,
            render: (_, record) => [
                <Popconfirm key="delete" title={`删除资源 ${record.resourceCode}?`}
                            okText="删除" cancelText="取消" okButtonProps={{danger: true}}
                            onConfirm={() => handleDelete(record.id)}>
                    <Button type="text" size="small" danger icon={<DeleteOutlined/>}>删除</Button>
                </Popconfirm>,
            ],
        },
    ]

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 0}}>
            <PageHeader
                title="资源管理"
                subtitle="4A AUTHZ 域 · 所有资源（权限点）的统一视图"
                breadcrumb={[{label: '4A 中心'}, {label: '资源管理'}]}
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
                        <Select placeholder="全部类型" allowClear style={{width: 110}}
                                options={Object.entries(TYPE_MAP).map(([k, v]) => ({
                                    value: Number(k),
                                    label: v.text
                                }))}/>
                    </Form.Item>
                    <Form.Item name="keyword" label="关键字">
                        <Input placeholder="编码 / 名称" allowClear style={{width: 180}}/>
                    </Form.Item>
                    <Form.Item>
                        <Space>
                            <Button type="primary" icon={<SearchOutlined/>} onClick={handleSearch}>查询</Button>
                            <Button icon={<UndoOutlined/>} onClick={handleReset}>重置</Button>
                        </Space>
                    </Form.Item>
                </Form>
                <Space style={{flexShrink: 0}}>
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
                pagination={{pageSize: 15, showSizeChanger: true, showTotal: t => `共 ${t} 条`}}
                dateFormatter="string"
            />
        </div>
    )
}