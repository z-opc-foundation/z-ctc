import { lazy } from 'react'
import {
    ApartmentOutlined,
    AppstoreOutlined,
    BankOutlined,
    BookOutlined,
    FileSearchOutlined,
    HomeOutlined,
    KeyOutlined,
    LinkOutlined,
    LockOutlined,
    ReadOutlined,
    SafetyCertificateOutlined,
    SolutionOutlined,
    TeamOutlined,
    UserOutlined,
} from '@ant-design/icons'
import HomePage from './HomePage'
import { at } from './routes-base'

// 除首页外的页面全部按需加载：组件层把 pro-components / dayjs 打包进 dist，
// 一次性静态引入会让首屏 chunk 冲到 2MB+；拆开后首屏只带首页与架子的依赖。
const AccountsPage = lazy(() => import('./pages/AccountsPage'))
const OrgPage = lazy(() => import('./pages/OrgPage'))
const TenantPage = lazy(() => import('./pages/TenantPage'))
const ApplicationPage = lazy(() => import('./pages/ApplicationPage'))
const RolePage = lazy(() => import('./pages/RolePage'))
const AuthzPage = lazy(() => import('./pages/AuthzPage'))
const ResourcePage = lazy(() => import('./pages/ResourcePage'))
const UserPermissionPage = lazy(() => import('./pages/UserPermissionPage'))
const DictPage = lazy(() => import('./pages/DictPage'))
const SurlPage = lazy(() => import('./pages/SurlPage'))
const AuditPage = lazy(() => import('./pages/AuditPage'))
const AuthnPage = lazy(() => import('./pages/AuthnPage'))
const ComponentDocPage = lazy(() => import('./pages/ComponentDoc'))

/**
 * 菜单 + 路由清单（lead 008 §10/§14/§16）。前缀由 routes-base 按环境注入。
 *
 * 除首页、认证配置占位、组件文档外，其余页面都是 `@yuku123/z-ctc-component` 里真实组件的
 * 薄包装（见 src/pages/*.jsx）—— 菜单直达的就是组件层那套带分页 / 空态 / 错误态的页面，
 * 不再维护第二套 mock 页面（两套写法曾让「菜单能点但数据是假的」）。
 */
export const menuItems = [
    { key: at('/home'), label: '首页', icon: <HomeOutlined /> },
    { key: at('/accounts'), label: '账号管理', icon: <UserOutlined />, desc: '账号列表、启停与分配角色' },
    { key: at('/orgs'), label: '组织管理', icon: <ApartmentOutlined />, desc: '租户 / 域 / 组织-部门-组' },
    { key: at('/tenants'), label: '租户管理', icon: <BankOutlined />, desc: '租户维护与域管理' },
    { key: at('/applications'), label: '应用管理', icon: <AppstoreOutlined />, desc: '应用与 API 资产 / 菜单 / 按钮' },
    { key: at('/roles'), label: '角色管理', icon: <TeamOutlined />, desc: '角色增删改查' },
    { key: at('/permissions'), label: '权限管理', icon: <SafetyCertificateOutlined />, desc: '资源 × 角色的授权配置' },
    { key: at('/resources'), label: '资源权限点', icon: <KeyOutlined />, desc: '按应用分页查全域权限点' },
    { key: at('/user-permissions'), label: '用户权限', icon: <SolutionOutlined />, desc: '用户角色与权限资源树' },
    { key: at('/dicts'), label: '字典管理', icon: <BookOutlined />, desc: '字典类型与字典项' },
    { key: at('/surls'), label: '短链管理', icon: <LinkOutlined />, desc: '短链生成与失效' },
    { key: at('/audit'), label: '审计日志', icon: <FileSearchOutlined />, desc: '登录日志与操作审计' },
    { key: at('/authn'), label: '认证配置', icon: <LockOutlined />, desc: '密码策略与外部身份源（未接入）' },
    // 组件文档页（lead 008 §7）：菜单数组最末项。
    // §14：路径带产品名前缀 —— 但本壳的前缀由 routes-base 按环境注入（生产为空串），
    // 所以这里用 at('/docs') 而不是写死 '/z-ctc/docs'：两种环境都跟着 BASE 走。
    { key: at('/docs'), label: '组件文档', icon: <ReadOutlined />, desc: '本壳能力与 API 说明' },
]

export const routes = [
    { path: at('/home'), Component: HomePage },
    { path: at('/accounts'), Component: AccountsPage },
    { path: at('/orgs'), Component: OrgPage },
    { path: at('/tenants'), Component: TenantPage },
    { path: at('/applications'), Component: ApplicationPage },
    { path: at('/roles'), Component: RolePage },
    { path: at('/permissions'), Component: AuthzPage },
    { path: at('/resources'), Component: ResourcePage },
    { path: at('/user-permissions'), Component: UserPermissionPage },
    { path: at('/dicts'), Component: DictPage },
    { path: at('/surls'), Component: SurlPage },
    { path: at('/audit'), Component: AuditPage },
    { path: at('/authn'), Component: AuthnPage },
    { path: at('/docs'), Component: ComponentDocPage },
]
