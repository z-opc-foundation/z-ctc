import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, Form, Input, message } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { at } from './routes-base'

/**
 * 登录页（lead 008 §16）—— 账号密码登录 z-ctc 后端（/api/ctc/authn/login）。
 *
 * 成功即把真 token 写进 localStorage：@yuku123/z-frontend-common 的请求拦截器
 * 统一补 Authorization: Bearer，页面这才真正连得上后端。
 * 构建期若注入了 VITE_MOCK_USER / VITE_MOCK_PASSWORD，那一组账号仍走本地 mock
 * （离线演示用）；账号本身不写死在仓里（001_凭证管理规范）。
 */
const MOCK_AUTH = {
    username: import.meta.env.VITE_MOCK_USER || '',
    password: import.meta.env.VITE_MOCK_PASSWORD || '',
    user: { name: import.meta.env.VITE_MOCK_USER || '', role: '管理员' },
}

export default function LoginPage() {
    const [loading, setLoading] = useState(false)
    const navigate = useNavigate()

    const handleSubmit = async (values) => {
        setLoading(true)
        try {
            // 1) 离线演示兜底：构建期注入的 mock 账号（没起后端时用）。
            //    账号不写死在仓里（001_凭证管理规范），未注入时这一段自然跳过。
            if (MOCK_AUTH.username && values.username === MOCK_AUTH.username
                && values.password === MOCK_AUTH.password) {
                localStorage.setItem('token', 'mock-' + MOCK_AUTH.username)
                localStorage.setItem('userInfo', JSON.stringify(MOCK_AUTH.user))
                message.success('登录成功（mock 模式，未连接后端）')
                navigate(at('/home'), { replace: true })
                return
            }

            // 2) 正常路径：后端账号密码登录，拿真 token。
            //    token 存进 localStorage 后，@yuku123/z-frontend-common 的拦截器
            //    会给每个请求补 Authorization: Bearer —— 页面能不能真联后端就差这一步。
            const resp = await fetch('/api/ctc/authn/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ identifier: values.username, password: values.password }),
            })
            const body = await resp.json().catch(() => null)
            const data = body?.data
            if (!resp.ok || body?.success === false || !data?.token) {
                message.error(body?.message || '用户名或密码错误')
                return
            }
            localStorage.setItem('token', data.token)
            const account = data.account || {}
            localStorage.setItem('userInfo', JSON.stringify({
                name: account.nickname || account.username || values.username,
                role: account.isPlatformAdmin ? '平台管理员' : '租户用户',
            }))
            message.success('登录成功')
            navigate(at('/home'), { replace: true })
        } catch (e) {
            message.error('登录失败：' + (e?.message || '网络异常'))
        } finally {
            setLoading(false)
        }
    }

    return (
        <div style={{
            height: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center',
            background: 'linear-gradient(135deg, #eef2ff 0%, #f5f3ff 50%, #eff6ff 100%)',
        }}>
            <Card style={{ width: 400, boxShadow: '0 10px 40px rgba(124,58,237,0.12)' }}>
                <div style={{ textAlign: 'center', marginBottom: 20 }}>
                    <div style={{
                        width: 48, height: 48, borderRadius: 12, margin: '0 auto 12px',
                        background: 'linear-gradient(135deg, #2563eb, #6d28d9)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        color: '#fff', fontSize: 24, fontWeight: 700,
                    }}>z</div>
                    <div style={{ fontSize: 18, fontWeight: 600, color: '#0f172a' }}>z-ctc 4A 中心</div>
                </div>
                <Form onFinish={handleSubmit} size="large">
                    <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                        <Input prefix={<UserOutlined />} placeholder="用户名" />
                    </Form.Item>
                    <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                        <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                    </Form.Item>
                    <Form.Item style={{ marginBottom: 8 }}>
                        <Button type="primary" htmlType="submit" loading={loading} block
                                style={{ background: 'linear-gradient(135deg, #2563eb 0%, #6d28d9 100%)', border: 'none' }}>
                            登 录
                        </Button>
                    </Form.Item>
                    <div style={{ textAlign: 'center', color: '#94a3b8', fontSize: 12 }}>
                        账号密码登录（构建期注入 mock 账号时走离线演示）
                    </div>
                </Form>
            </Card>
        </div>
    )
}
