import {useEffect, useState} from 'react'
import {Card, Col, Row, Statistic} from 'antd'
import {AppstoreOutlined, LinkOutlined, SafetyCertificateOutlined, TeamOutlined} from '@ant-design/icons'
import {PageHeader} from '@/common/components/ui'
import {createRequest} from '@/common/utils/request'

const request = createRequest({baseURL: '/api', timeout: 10000})

const extractTotal = (r) => {
    if (!r) return 0
    if (typeof r.total === 'number') return r.total
    if (Array.isArray(r)) return r.length
    if (Array.isArray(r.records)) return r.records.length
    return 0
}

export default function CtcOverview() {
    const [stats, setStats] = useState({account: null, role: null, tenant: null, surl: null})
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        let mounted = true
        Promise.all([
            request.get('/ctc/ac/accounts', {params: {pageSize: 1}}).then(r => extractTotal(r)).catch(() => 0),
            request.get('/ctc/authz/roles/list').then(r => extractTotal(r)).catch(() => 0),
            request.get('/ctc/ac/tenants/list').then(r => r && Array.isArray(r) ? r.length : 0).catch(() => 0),
            request.get('/ctc/surl/list', {params: {pageSize: 1}}).then(r => extractTotal(r)).catch(() => 0),
        ]).then(([account, role, tenant, surl]) => {
            if (mounted) {
                setStats({account, role, tenant, surl})
                setLoading(false)
            }
        })
        return () => {
            mounted = false
        }
    }, [])

    return (
        <div style={{padding: 24}}>
            <PageHeader
                title="4A 中心概览"
                subtitle="账号 · 角色 · 租户 · 短链总量指标"
            />
            <Row gutter={[16, 16]}>
                <Col xs={24} sm={12} lg={6}>
                    <Card loading={loading}><Statistic title="账号数" value={stats.account}
                                                       prefix={<TeamOutlined/>}/></Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card loading={loading}><Statistic title="角色数" value={stats.role}
                                                       prefix={<SafetyCertificateOutlined/>}/></Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card loading={loading}><Statistic title="租户数" value={stats.tenant}
                                                       prefix={<AppstoreOutlined/>}/></Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card loading={loading}><Statistic title="短链数" value={stats.surl}
                                                       prefix={<LinkOutlined/>}/></Card>
                </Col>
            </Row>
        </div>
    )
}
