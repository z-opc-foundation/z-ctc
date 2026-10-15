/**
 * UI 通用组件库 - 借鉴 wenming7/marketplace 视觉语言 + GitHub/awesome-copilot 交互原则
 *
 * 视觉类 (借鉴 wenming7/marketplace):
 *   - GradientStatCard    渐变数据卡 (替代 antd Statistic)
 *   - CategoryDot         分类圆点
 *   - SectionHeader       段落标题 (圆点 + 标题 + 全部>)
 *   - FeatureCard         功能卡 (16:9 封面 + 多 chip + 元信息)
 *   - DarkBanner          深色 CTA 横幅
 *
 * 交互类 (借鉴 ui-ux-pro-max + frontend-ui-engineering):
 *   - EmptyState          统一空状态 (图标 + 文案 + 行动)
 *   - ErrorState          错误状态 (原因 + 重试)
 *   - LoadingState        骨架屏 (cards/table/list/detail)
 *   - SearchInput         Debounced 搜索 (300ms, Enter 立即, Esc 清空)
 *   - PageHeader          页面标题 (面包屑 + 返回 + 操作区)
 */

export {default as GradientStatCard} from './GradientStatCard'
export {default as CategoryDot} from './CategoryDot'
export {default as SectionHeader} from './SectionHeader'
export {default as FeatureCard} from './FeatureCard'
export {default as DarkBanner} from './DarkBanner'

export {default as EmptyState} from './EmptyState'
export {default as ErrorState} from './ErrorState'
export {default as LoadingState} from './LoadingState'
export {default as SearchInput} from './SearchInput'
export {default as PageHeader} from './PageHeader'
export {default as TableToolbar, RefreshButton} from './TableToolbar'
export {default as StatusBadge} from './StatusBadge'
export {default as PagedTable} from './PagedTable'

export {brand, palette, neutral, radius, shadow, lift, space, fontSize, antdTheme} from './tokens'
