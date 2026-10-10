import { FileSearchOutlined, HomeOutlined, SafetyCertificateOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons'
import HomePage from './HomePage'
import AccountsPage from './pages/AccountsPage'
import AuthnPage from './pages/AuthnPage'
import AuthzPage from './pages/AuthzPage'
import AuditPage from './pages/AuditPage'

/** 菜单 + 路由清单（lead 008 §10/§14/§16）。 */
export const menuItems = [
    { key: '/z-ctc/home', label: '首页', icon: <HomeOutlined /> },
    { key: '/z-ctc/accounts', label: '账号管理', icon: <UserOutlined /> },
    { key: '/z-ctc/authn', label: '认证配置', icon: <SafetyCertificateOutlined /> },
    { key: '/z-ctc/authz', label: '授权策略', icon: <TeamOutlined /> },
    { key: '/z-ctc/audit', label: '审计日志', icon: <FileSearchOutlined /> },
]

export const routeTable = [
    { path: '/z-ctc/home', Component: HomePage },
    { path: '/z-ctc/accounts', Component: AccountsPage },
    { path: '/z-ctc/authn', Component: AuthnPage },
    { path: '/z-ctc/authz', Component: AuthzPage },
    { path: '/z-ctc/audit', Component: AuditPage },
]
