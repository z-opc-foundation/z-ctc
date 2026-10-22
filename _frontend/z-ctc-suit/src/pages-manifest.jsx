import { FileSearchOutlined, HomeOutlined, ReadOutlined, SafetyCertificateOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons'
import HomePage from './HomePage'
import AccountsPage from './pages/AccountsPage'
import AuthnPage from './pages/AuthnPage'
import AuthzPage from './pages/AuthzPage'
import AuditPage from './pages/AuditPage'
import ComponentDocPage from './pages/ComponentDoc'
import { at } from './routes-base'

/** 菜单 + 路由清单（lead 008 §10/§14/§16）。前缀由 routes-base 按环境注入。 */
export const menuItems = [
    { key: at('/home'), label: '首页', icon: <HomeOutlined /> },
    { key: at('/accounts'), label: '账号管理', icon: <UserOutlined /> },
    { key: at('/authn'), label: '认证配置', icon: <SafetyCertificateOutlined /> },
    { key: at('/authz'), label: '授权策略', icon: <TeamOutlined /> },
    { key: at('/audit'), label: '审计日志', icon: <FileSearchOutlined /> },
    // 组件文档页（lead 008 §7）：菜单数组最末项。
    // §14：路径带产品名前缀 —— 但本壳的前缀由 routes-base 按环境注入（生产为空串），
    // 所以这里用 at('/docs') 而不是写死 '/z-ctc/docs'：两种环境都跟着 BASE 走。
    { key: at('/docs'), label: '组件文档', icon: <ReadOutlined /> },
]

export const routes = [
    { path: at('/home'), Component: HomePage },
    { path: at('/accounts'), Component: AccountsPage },
    { path: at('/authn'), Component: AuthnPage },
    { path: at('/authz'), Component: AuthzPage },
    { path: at('/audit'), Component: AuditPage },
    { path: at('/docs'), Component: ComponentDocPage },
]
