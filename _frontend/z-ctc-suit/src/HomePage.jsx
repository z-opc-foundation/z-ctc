import { Card, Col, Row, Space, Tag, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { menuItems } from './pages-manifest'
import { at } from './routes-base'

const { Title, Paragraph } = Typography

/** 首页（lead 008 §16）—— 登录后着陆页：欢迎语 + 快捷入口卡。 */
export default function HomePage() {
    const navigate = useNavigate()
    const [user, setUser] = useState(null)

    useEffect(() => {
        const raw = localStorage.getItem('userInfo')
        if (raw) { try { setUser(JSON.parse(raw)) } catch { setUser({ name: raw }) } }
    }, [])

    const cards = menuItems.filter((m) => m.key !== at('/home'))

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 0}}>
            <Card style={{background: '#fff', border: '1px solid #e2e4e8', boxShadow: 'none'}}>
                <Space direction="vertical" size={4} style={{ color: 'rgba(0,0,0,0.9)' }}>
                    <Title level={3} style={{ color: 'rgba(0,0,0,0.9)', margin: 0 }}>
                        欢迎{user?.name ? `，${user.name}` : ''}
                    </Title>
                    <Paragraph style={{ color: 'rgba(0,0,0,0.6)', margin: 0 }}>
                        z-ctc 4A 中心 管理台
                    </Paragraph>
                    {user?.role && (
                        <Tag color="blue" style={{ marginTop: 8 }}>{user.role}</Tag>
                    )}
                </Space>
            </Card>

            <Row gutter={[0, 0]}>
                {cards.map((m) => (
                    <Col key={m.key} xs={24} sm={12} md={12} lg={8}>
                        <Card hoverable onClick={() => navigate(m.key)} style={{
                            marginTop: -1, marginLeft: -1, borderTop: '1px solid #0052d9', boxShadow: 'none',
                        }}>
                            <Space align="start" size={12}>
                                <div style={{
                                    width: 44, height: 44, borderRadius: 8, flexShrink: 0,
                                    background: '#e8f3ff', color: '#0052d9',
                                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20,
                                }}>
                                    {m.icon}
                                </div>
                                <div style={{ minWidth: 0 }}>
                                    <div style={{ fontSize: 15, fontWeight: 600, color: 'rgba(0,0,0,0.9)' }}>{m.label}</div>
                                    <div style={{ fontSize: 12, color: 'rgba(0,0,0,0.48)', marginTop: 2 }}>{m.desc || '进入'}</div>
                                </div>
                            </Space>
                        </Card>
                    </Col>
                ))}
            </Row>
        </div>
    )
}
