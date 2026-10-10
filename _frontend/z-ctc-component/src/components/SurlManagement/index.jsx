import {useRef, useState} from 'react'
import {ProForm, ProFormDigit, ProFormText, ProTable} from '@ant-design/pro-components'
import {Button, Card, Form, message, Popconfirm, Space, Tag, Tooltip} from 'antd'
import {CopyOutlined, DeleteOutlined, LinkOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {configureApiBaseURL, ctcSurlApi} from './api'
import {PageHeader} from '@/common/components/ui'

/**
 * 短链生成器 (4A SURL 域)
 *
 * 后端契约：
 *   - POST   /api/ctc/surl/shorten           — 生成短码
 *   - GET    /api/ctc/surl/{mapKey}/info     — 查询元信息
 *   - DELETE /api/ctc/surl/{mapKey}          — 失效
 *
 * 风格：ProForm (生成表单) + ProTable (历史)
 * 短链点击 → 复制 + 打开新窗口跳转
 *
 * Props:
 *   - apiBaseURL?: string    后端 baseURL, 默认 '/api'
 *
 * 从 z-opc-main-starter-frontend/src/pages/ctc/4a/surl/index.tsx 迁入.
 */
const STORAGE_KEY = 'z_surl_history'

export default function SurlManagement({apiBaseURL}) {
    // 配置 baseURL (组件 mount 时生效)
    if (apiBaseURL) configureApiBaseURL(apiBaseURL)

    const [form] = Form.useForm()
    const actionRef = useRef(undefined)
    const [lastResult, setLastResult] = useState(null)
    const [history, setHistory] = useState(
        () => JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]'),
    )
    const [generating, setGenerating] = useState(false)

    const persistHistory = (next) => {
        setHistory(next)
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    }

    const handleGenerate = async (values) => {
        setGenerating(true)
        try {
            const res = await ctcSurlApi.shorten({
                originUrl: values.originUrl,
                ttlMillis: values.ttlMillis || 0,
                remark: values.remark,
            })
            setLastResult(res)
            const fullUrl = ctcSurlApi.buildUrl(res.mapKey)
            const record = {
                mapKey: res.mapKey,
                originUrl: res.originUrl,
                expireAt: res.expireAt,
                hits: res.hits,
                remark: values.remark,
                createdAt: new Date().toISOString(),
            }
            persistHistory([record, ...history])
            message.success(`短链生成成功：${fullUrl}`)
            actionRef.current?.reload()
        } catch (e) {
            message.error('生成失败：' + (e?.message || '未知错误'))
        } finally {
            setGenerating(false)
        }
    }

    const handleCopy = async (text) => {
        try {
            await navigator.clipboard.writeText(text)
            message.success('已复制到剪贴板')
        } catch {
            message.error('复制失败，请手动复制')
        }
    }

    const handleDelete = async (mapKey) => {
        try {
            await ctcSurlApi.invalidate(mapKey)
            message.success('已失效')
            persistHistory(history.filter(r => r.mapKey !== mapKey))
            actionRef.current?.reload()
        } catch (e) {
            // 即使后端失败，本地也清理
            persistHistory(history.filter(r => r.mapKey !== mapKey))
            message.warning('后端失效失败，已从本地移除：' + (e?.message || ''))
        }
    }

    const columns = [
        {title: '短码', dataIndex: 'mapKey', width: 120, fixed: 'left'},
        {
            title: '完整短链', dataIndex: 'mapKey', width: 280,
            render: (_, r) => {
                const url = ctcSurlApi.buildUrl(r.mapKey)
                return (
                    <Space size={4}>
                        <a href={url} target="_blank" rel="noreferrer">{url}</a>
                        <Tooltip title="复制">
                            <Button type="text" size="small" icon={<CopyOutlined/>} onClick={() => handleCopy(url)}/>
                        </Tooltip>
                    </Space>
                )
            },
        },
        {title: '原始URL', dataIndex: 'originUrl', width: 280, ellipsis: true},
        {title: '备注', dataIndex: 'remark', width: 160, ellipsis: true},
        {title: '命中次数', dataIndex: 'hits', width: 90, valueType: 'digit'},
        {
            title: '过期时间', dataIndex: 'expireAt', width: 170,
            render: (_, r) => r.expireAt ? new Date(r.expireAt).toLocaleString() : <Tag color="green">永久</Tag>,
        },
        {title: '创建时间', dataIndex: 'createdAt', width: 170, valueType: 'dateTime'},
        {
            title: '操作', valueType: 'option', width: 100, fixed: 'right',
            render: (_, record) => [
                <Popconfirm key="del" title="失效该短链？" onConfirm={() => handleDelete(record.mapKey)}>
                    <Button type="text" danger size="small" icon={<DeleteOutlined/>}>失效</Button>
                </Popconfirm>,
            ],
        },
    ]

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 0}}>
            <PageHeader
                title="短链生成器 (4A-SURL)"
                subtitle="z-ctc-surl 域 — 长链转短链，支持过期与命中统计"
                breadcrumb={[{label: '4A 中心'}, {label: '短链管理'}]}
            />

            <Card title={<><LinkOutlined/> 生成短链</>}>
                <ProForm
                    form={form}
                    layout="inline"
                    onFinish={handleGenerate}
                    submitter={{
                        render: (props) => [
                            <Button key="reset" onClick={() => props.form?.resetFields()}>
                                重置
                            </Button>,
                            <Button key="submit" type="primary" icon={<PlusOutlined/>}
                                    loading={generating}
                                    onClick={() => props.form?.submit()}>
                                生成
                            </Button>,
                        ],
                    }}
                >
                    <ProFormText
                        name="originUrl"
                        label="原始URL"
                        width="md"
                        rules={[
                            {required: true, message: '请输入要缩短的URL'},
                            {type: 'url', message: '请输入合法的URL'},
                        ]}
                        placeholder="https://example.com/some/very/long/path?param=1"
                    />
                    <ProFormDigit
                        name="ttlMillis"
                        label="有效期(ms)"
                        width="sm"
                        min={0}
                        placeholder="0 = 永久"
                        tooldescription="0 = 永久；其它填毫秒数，如 86400000 = 1天"
                    />
                    <ProFormText name="remark" label="备注" width="sm" placeholder="可选"/>
                </ProForm>
                {lastResult && (
                    <div style={{
                        marginTop: 16, padding: 16, background: '#f6ffed',
                        border: '1px solid #b7eb8f', borderRadius: 6,
                    }}>
                        <div style={{marginBottom: 8}}>
                            <b>生成结果：</b>
                        </div>
                        <Space orientation="vertical" style={{width: '100%'}}>
                            <div>
                                <span style={{color: '#999', marginRight: 8}}>短码:</span>
                                <Tag color="green">{lastResult.mapKey}</Tag>
                            </div>
                            <div>
                                <span style={{color: '#999', marginRight: 8}}>完整短链:</span>
                                <a href={ctcSurlApi.buildUrl(lastResult.mapKey)} target="_blank" rel="noreferrer">
                                    {ctcSurlApi.buildUrl(lastResult.mapKey)}
                                </a>
                                <Button type="link" size="small" icon={<CopyOutlined/>}
                                        onClick={() => handleCopy(ctcSurlApi.buildUrl(lastResult.mapKey))}>
                                    复制
                                </Button>
                            </div>
                            <div>
                                <span style={{color: '#999', marginRight: 8}}>原始URL:</span>
                                <span style={{wordBreak: 'break-all'}}>{lastResult.originUrl}</span>
                            </div>
                        </Space>
                    </div>
                )}
            </Card>

            <Card title="历史短链" style={{marginTop: 16}}>
                <ProTable
                    actionRef={actionRef}
                    rowKey="mapKey"
                    columns={columns}
                    dataSource={history}
                    search={false}
                    pagination={{pageSize: 10, showSizeChanger: true}}
                    toolBarRender={() => [
                        <Button key="clear" onClick={() => persistHistory([])}>
                            清空历史
                        </Button>,
                        <Button key="reload" icon={<ReloadOutlined/>} onClick={() => actionRef.current?.reload()}>
                            刷新
                        </Button>,
                    ]}
                />
            </Card>
        </div>
    )
}
