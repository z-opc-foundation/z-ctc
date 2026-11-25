import {useEffect, useState} from 'react'
import {Button, Drawer, Descriptions, Select, Space, Tabs, Tag, message} from 'antd'
import {ReloadOutlined, RocketOutlined} from '@ant-design/icons'
import {ListTable} from '@yuku123/z-frontend-common/ui'
import {siteInitApi} from '../_api/siteInit'
import {configureApiBaseURL} from '../_api/base'
import {ctcAcTenantApi} from '../TenantManagement/api'
import TenantManagement from '../TenantManagement'

/**
 * 建站管理 —— 系统初造时的建站体系主控台。
 *
 * 建站板块分自动 / 非自动两条路 (2026-11-25 CEO 裁定「一般是自动」):
 * 自动路径 = 建租户时后端自动跑本地 + 远程域初始化, 不经本页;
 * 本页是非自动的补齐入口 —— 站点状态看哪些站没就绪, 全量补齐/单域重跑兜底。
 *
 * 四段:
 *   ① 站点台账   平台管理员视角的站点(租户)列表 (左树右表, 复用 TenantManagement)
 *   ② 站点状态   每站点按最近一轮初始化任务聚合的就绪度, 未就绪可一键全量补齐
 *   ③ 初始化能力 ctc 感知到的域注册表: 各域实现 SiteInitProvider 后自登记到 ctc,
 *                 这里列出域/能力/回调地址, 可实时拉某域的能力清单, 可停用某域
 *   ④ 执行记录   一次建站初始化的逐域执行明细
 *
 * 后端:
 *   GET  /api/ctc/site-init/registrations
 *   POST /api/ctc/site-init/registrations/disable
 *   POST /api/ctc/site-init/runs
 *   POST /api/ctc/site-init/runs/domain
 *   GET  /api/ctc/site-init/runs/page
 *   GET  /api/ctc/site-init/tenants/status
 *   GET  /api/ctc/site-init/manifest/{domainCode}
 */

const TASK_STATUS = {
    0: {text: '待执行', color: 'default'},
    1: {text: '执行中', color: 'processing'},
    2: {text: '成功', color: 'success'},
    3: {text: '失败', color: 'error'},
    4: {text: '跳过', color: 'warning'},
}

const READY_STATUS = {
    uninitialized: {text: '未初始化', color: 'default'},
    running: {text: '初始化中', color: 'processing'},
    failed: {text: '需补齐', color: 'error'},
    incomplete: {text: '域未跑全', color: 'warning'},
    ready: {text: '可用', color: 'success'},
}

export default function SiteInitManagement({apiBaseURL}) {
    const [registrations, setRegistrations] = useState([])
    const [registrationsLoading, setRegistrationsLoading] = useState(false)
    const [tasks, setTasks] = useState([])
    const [tasksLoading, setTasksLoading] = useState(false)
    const [tenants, setTenants] = useState([])
    const [tenantCode, setTenantCode] = useState()
    const [running, setRunning] = useState(false)
    const [manifestDomain, setManifestDomain] = useState(null)
    const [manifest, setManifest] = useState(null)
    const [statusRows, setStatusRows] = useState([])
    const [statusLoading, setStatusLoading] = useState(false)
    const [repairing, setRepairing] = useState(null)

    useEffect(() => {
        if (apiBaseURL) configureApiBaseURL(apiBaseURL)
    }, [apiBaseURL])

    const loadRegistrations = async () => {
        setRegistrationsLoading(true)
        try {
            const res = await siteInitApi.listRegistrations()
            setRegistrations(Array.isArray(res) ? res : [])
        } catch (e) {
            message.error('加载初始化域注册表失败：' + (e?.message || '未知错误'))
            setRegistrations([])
        } finally {
            setRegistrationsLoading(false)
        }
    }

    const loadTasks = async () => {
        setTasksLoading(true)
        try {
            const res = await siteInitApi.pageRuns({pageNum: 1, pageSize: 20})
            setTasks(Array.isArray(res?.data) ? res.data : [])
        } catch (e) {
            message.error('加载执行记录失败：' + (e?.message || '未知错误'))
            setTasks([])
        } finally {
            setTasksLoading(false)
        }
    }

    const loadTenants = async () => {
        try {
            const res = await ctcAcTenantApi.listAllForPlatform()
            const rows = Array.isArray(res) ? res : []
            setTenants(rows.map(t => ({value: t.tenantCode, label: `${t.tenantName} (${t.tenantCode})`})))
        } catch (e) {
            // 非平台管理员没有全量接口, 站点改为手填
            setTenants([])
        }
    }

    const loadTenantStatus = async () => {
        setStatusLoading(true)
        try {
            const res = await siteInitApi.tenantStatus()
            setStatusRows(Array.isArray(res) ? res : [])
        } catch (e) {
            message.error('加载站点状态失败：' + (e?.message || '未知错误'))
            setStatusRows([])
        } finally {
            setStatusLoading(false)
        }
    }

    useEffect(() => {
        loadRegistrations()
        loadTasks()
        loadTenants()
        loadTenantStatus()
    }, [])

    const runAll = async () => {
        if (!tenantCode) {
            message.warning('先选一个站点')
            return
        }
        setRunning(true)
        try {
            const res = await siteInitApi.run(tenantCode, {})
            const failed = res?.failureCount || 0
            if (failed > 0) {
                message.warning(`建站初始化完成，但有 ${failed} 个域失败`)
            } else {
                message.success(`建站初始化完成，成功 ${res?.successCount || 0} 个域`)
            }
            loadTasks()
        } catch (e) {
            message.error('建站初始化失败：' + (e?.message || '未知错误'))
        } finally {
            setRunning(false)
        }
    }

    const runDomain = async (domainCode) => {
        if (!tenantCode) {
            message.warning('先选一个站点')
            return
        }
        setRunning(true)
        try {
            const res = await siteInitApi.runDomain(tenantCode, domainCode, {})
            if (res?.successCount > 0) {
                message.success(`域 ${domainCode} 初始化完成`)
            } else {
                message.warning(`域 ${domainCode} 初始化失败：` + (res?.domains?.[0]?.message || '未知原因'))
            }
            loadTasks()
        } catch (e) {
            message.error('域初始化失败：' + (e?.message || '未知错误'))
        } finally {
            setRunning(false)
        }
    }

    const repairSite = async (code) => {
        setRepairing(code)
        try {
            const res = await siteInitApi.run(code, {})
            const failed = res?.failureCount || 0
            if (failed > 0) {
                message.warning(`站点 ${code} 补齐完成，但有 ${failed} 个域失败`)
            } else {
                message.success(`站点 ${code} 补齐完成，成功 ${res?.successCount || 0} 个域`)
            }
            loadTenantStatus()
            loadTasks()
        } catch (e) {
            message.error('补齐失败：' + (e?.message || '未知错误'))
        } finally {
            setRepairing(null)
        }
    }

    const openManifest = async (domainCode) => {
        setManifestDomain(domainCode)
        setManifest(null)
        try {
            const res = await siteInitApi.manifest(domainCode)
            setManifest(res)
        } catch (e) {
            message.error('拉取能力清单失败：' + (e?.message || '未知错误'))
            setManifest({domainCode, capabilities: []})
        }
    }

    const disableDomain = async (domainCode) => {
        try {
            await siteInitApi.disableRegistration(domainCode)
            message.success(`已停用域 ${domainCode}`)
            loadRegistrations()
        } catch (e) {
            message.error('停用失败：' + (e?.message || '未知错误'))
        }
    }

    const registrationColumns = [
        {title: '域编码', dataIndex: 'domainCode', width: 140, ellipsis: true},
        {title: '域名称', dataIndex: 'domainName', width: 120, ellipsis: true},
        {title: '顺序', dataIndex: 'orderNo', width: 70},
        {title: '回调地址', dataIndex: 'endpoint', ellipsis: true},
        {
            title: '能力', dataIndex: 'capabilities', width: 220,
            render: v => (v ? String(v).split(',').filter(Boolean).map(c => (
                <Tag key={c} style={{marginInlineEnd: 4}}>{c}</Tag>
            )) : <span style={{color: '#999'}}>—</span>),
        },
        {
            title: '状态', dataIndex: 'status', width: 80,
            render: v => <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '停用'}</Tag>,
        },
        {
            title: '操作', width: 190,
            render: (_, r) => (
                <Space onClick={e => e.stopPropagation()}>
                    <Button type="link" size="small" onClick={() => openManifest(r.domainCode)}>能力清单</Button>
                    <Button type="link" size="small" disabled={running || !tenantCode}
                            onClick={() => runDomain(r.domainCode)}>仅建本站</Button>
                    {r.status === 1 && (
                        <Button type="link" size="small" danger onClick={() => disableDomain(r.domainCode)}>停用</Button>
                    )}
                </Space>
            ),
        },
    ]

    const taskColumns = [
        {title: '站点', dataIndex: 'tenantCode', width: 120, ellipsis: true},
        {title: '域', dataIndex: 'domainCode', width: 140, ellipsis: true},
        {
            title: '状态', dataIndex: 'status', width: 90,
            render: v => {
                const s = TASK_STATUS[v] || {text: String(v), color: 'default'}
                return <Tag color={s.color}>{s.text}</Tag>
            },
        },
        {title: '说明', dataIndex: 'message', ellipsis: true},
        {title: '耗时', dataIndex: 'costMs', width: 90, render: v => (v == null ? '—' : `${v} ms`)},
    ]

    const statusColumns = [
        {title: '站点', dataIndex: 'tenantCode', width: 130, ellipsis: true},
        {title: '名称', dataIndex: 'tenantName', width: 150, ellipsis: true},
        {
            title: '状态', dataIndex: 'statusText', width: 100,
            render: (v, r) => {
                const s = READY_STATUS[r.status] || {text: v || String(r.status), color: 'default'}
                return <Tag color={s.color}>{s.text}</Tag>
            },
        },
        {title: '已成功域', dataIndex: 'successDomains', width: 90},
        {title: '失败域', dataIndex: 'failedDomains', width: 80},
        {title: '注册域', dataIndex: 'expectedDomains', width: 80},
        {
            title: '最近初始化', dataIndex: 'lastRunAt', width: 170,
            render: v => (v ? String(v).replace('T', ' ').slice(0, 19) : '—'),
        },
        {
            title: '操作', width: 110,
            render: (_, r) => (
                <Space onClick={e => e.stopPropagation()}>
                    {r.status !== 'ready' && (
                        <Button type="link" size="small" loading={repairing === r.tenantCode}
                                onClick={() => repairSite(r.tenantCode)}>全量补齐</Button>
                    )}
                </Space>
            ),
        },
    ]

    const capabilityTab = (
        <ListTable
            cardTitle={`初始化能力 (${registrations.length})`}
            cardExtra={
                <Space>
                    <Button size="small" icon={<ReloadOutlined/>} onClick={loadRegistrations}>刷新</Button>
                    <Select
                        style={{minWidth: 220}}
                        placeholder="选择站点"
                        allowClear
                        showSearch
                        options={tenants}
                        value={tenantCode}
                        onChange={setTenantCode}
                    />
                    <Button size="small" type="primary" icon={<RocketOutlined/>} loading={running}
                            onClick={runAll}>执行建站初始化</Button>
                </Space>
            }
            rows={registrations}
            rowKey="id"
            columns={registrationColumns}
            cardProps={{size: 'small', loading: registrationsLoading}}
            emptyTitle="还没有域登记初始化能力"
            emptyDescription="各域实现 SiteInitProvider 并引 z-ctc-site-init-starter 后，启动即自动登记到这里。"
        />
    )

    const taskTab = (
        <ListTable
            cardTitle={`执行记录 (${tasks.length})`}
            cardExtra={<Button size="small" icon={<ReloadOutlined/>} onClick={loadTasks}>刷新</Button>}
            rows={tasks}
            rowKey="id"
            columns={taskColumns}
            cardProps={{size: 'small', loading: tasksLoading}}
            emptyTitle="还没有建站初始化记录"
            emptyDescription="在「初始化能力」里选站点执行一次，这里就会出现逐域明细。"
        />
    )

    const statusTab = (
        <ListTable
            cardTitle={`站点状态 (${statusRows.length})`}
            cardExtra={
                <Space>
                    <Button size="small" icon={<ReloadOutlined/>} onClick={loadTenantStatus}>刷新</Button>
                </Space>
            }
            rows={statusRows}
            rowKey="tenantCode"
            columns={statusColumns}
            cardProps={{size: 'small', loading: statusLoading}}
            emptyTitle="还没有站点"
            emptyDescription="建租户时建站初始化自动执行（自动路径）；这里看每个站点是否就绪，未就绪的站点可全量补齐。"
        />
    )

    const manifestDrawer = (
        <Drawer
            title={`能力清单 · ${manifestDomain || ''}`}
            open={!!manifestDomain}
            onClose={() => setManifestDomain(null)}
            width={560}
        >
            {manifest ? (
                <Descriptions column={1} size="small" bordered
                             items={[
                                 {key: 'code', label: '域编码', children: manifest.domainCode},
                                 {key: 'name', label: '域名称', children: manifest.domainName},
                                 {key: 'ver', label: '协议版本', children: manifest.protocolVersion},
                             ]}
                />
            ) : (
                <div style={{padding: 24, textAlign: 'center'}}>加载中…</div>
            )}
            {manifest?.capabilities?.length > 0 && (
                <div style={{marginTop: 16}}>
                    {(manifest.capabilities || []).map(c => (
                        <div key={c.code} style={{padding: '8px 0', borderBottom: '1px solid #f0f0f0'}}>
                            <Space>
                                <Tag>{c.code}</Tag>
                                <b>{c.name}</b>
                                <span style={{color: '#888'}}>{c.description}</span>
                            </Space>
                        </div>
                    ))}
                </div>
            )}
            {manifest && !(manifest.capabilities || []).length && (
                <div style={{padding: 24, textAlign: 'center', color: '#999'}}>该域未声明能力项</div>
            )}
        </Drawer>
    )

    return (
        <>
            <Tabs
                defaultActiveKey="status"
                items={[
                    {key: 'sites', label: '站点台账', children: <TenantManagement platformView/>},
                    {key: 'status', label: '站点状态', children: statusTab},
                    {key: 'capabilities', label: '初始化能力', children: capabilityTab},
                    {key: 'tasks', label: '执行记录', children: taskTab},
                ]}
            />
            {manifestDrawer}
        </>
    )
}
