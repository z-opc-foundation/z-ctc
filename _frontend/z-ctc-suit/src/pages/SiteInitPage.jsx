import { SiteInitManagement } from '@yuku123/z-ctc-component'

/**
 * 建站管理 —— 系统初造时的建站体系主控台。
 *
 * 三段 tab: 站点台账 / 初始化能力 (ctc 感知到的域注册表) / 执行记录。
 * 各域实现 SiteInitProvider 并向 4A 中心登记后, 这里的「初始化能力」会列出
 * 各域能替站点做的事, 「执行建站初始化」按序调各域的标准协议。
 */
export default function SiteInitPage() {
    return <SiteInitManagement />
}
