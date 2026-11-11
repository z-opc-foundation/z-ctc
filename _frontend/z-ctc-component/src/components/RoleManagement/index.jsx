import PermissionManagement from '../PermissionManagement'

/**
 * 角色管理 —— 已并入「权限管理」（CEO 2026-11-11）。
 *
 * 角色是权限点的命名集合，两者拆成两个顶级菜单时，用户必须自己在
 * 「角色」与「权限点」两张表之间来回对照才能回答「这个角色能干什么」。
 * 现在统一在 /ctc/4a/permission：左侧页内菜单分 角色 / 权限点 / 权限树，
 * 选角色时右侧展开它的权限点树。
 *
 * 本文件保留只是为了兼容既有引用（主壳曾按名导出 RoleManagement）：
 * 渲染合并页，默认落在「角色」视图（视图由 URL 的 ?view= 决定）。
 */
export default function RoleManagement(props) {
    return <PermissionManagement {...props}/>
}
