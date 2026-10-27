import { Suspense, useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { AppLayout, ErrorBoundary } from '@yuku123/z-frontend-common'
import { menuItems, routes } from './pages-manifest'
import { at } from './routes-base'
import LoginPage from './LoginPage'
import { Result, Button, Spin } from 'antd'
// 版本号只认 package.json，避免顶栏版本与发版号两处各写一遍慢慢漂移。
import { version as appVersion } from '../package.json'

function NotFound() {
    const navigate = useNavigate()
    return (
        <Result
            status="404"
            title="页面不存在"
            subTitle="路由表里没有这条路径。"
            extra={<Button type="primary" onClick={() => navigate(at('/home'))}>去首页</Button>}
        />
    )
}

function LoginRoute() {
    return <LoginPage />
}

function ProtectedShell() {
    const navigate = useNavigate()
    const [user, setUser] = useState(null)
    const [ready, setReady] = useState(false)

    useEffect(() => {
        if (!localStorage.getItem('token')) {
            navigate(at('/login'), { replace: true })
            return
        }
        const raw = localStorage.getItem('userInfo')
        if (raw) { try { setUser(JSON.parse(raw)) } catch { setUser({ name: raw }) } }
        setReady(true)
    }, [])

    if (!ready) return null

    return (
        <AppLayout
            menuItems={menuItems}
            appTitle="z-ctc 4A 中心"
            appShort="CTC"
            appVersion={appVersion}
            appIcon={{
                icon: <img src="/icon.png" alt="z-ctc 4A 中心" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: 8 }} />,
                color: '#0ea5e9',
                label: 'z-ctc 4A 中心',
            }}
            appUser={user}
        />
    )
}

/** lead 008 §16 suit 一次整合：登录路由 + 鉴权壳 + URL 即状态（§11/§14）。 */
export default function App() {
    // vite base：dev 是 '/ctc/'（对齐 z-ctc-admin 的 dev context-path），生产是 '/'。
    // BrowserRouter 必须吃掉这个前缀，否则 dev 下真实 URL /ctc/z-ctc/home 与路由表里的
    // /z-ctc/home 永远对不上 —— 所有菜单点开都是 404（生产 base='/' 时不加 basename）。
    const basename = import.meta.env.BASE_URL.replace(/\/$/, '') || undefined

    return (
        <ErrorBoundary>
            <BrowserRouter basename={basename}>
                {/* 页面按路由懒加载（见 pages-manifest.jsx），首次进入某页时补一个居中加载态。 */}
                <Suspense fallback={<div style={{ padding: 96, textAlign: 'center' }}><Spin size="large" /></div>}>
                    <Routes>
                        <Route path={at('/login')} element={<LoginRoute />} />
                        <Route element={<ProtectedShell />}>
                            <Route path="/" element={<Navigate to={menuItems[0].key} replace />} />
                            {routes.map((r) => (
                                <Route key={r.path} path={r.path} element={<r.Component />} />
                            ))}
                            <Route path="*" element={<NotFound />} />
                        </Route>
                    </Routes>
                </Suspense>
            </BrowserRouter>
        </ErrorBoundary>
    )
}
