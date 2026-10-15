import {Breadcrumb} from 'antd'
import {useNavigate} from 'react-router-dom'

/**
 * PageHeader - 通用页面标题（紧凑单行版）.
 *
 * 解决 z-opc 后台每个页面都重复写 Title + Description + 返回按钮的问题.
 *
 * 原则 (来自 anthropics/frontend-design):
 *   - Structure is information. 面包屑末级即当前页名, 与操作按钮同行, 信息密度优先.
 *   - 不用 emoji/装饰元素, 用 typography 表达层级.
 *
 * @param {Object} props
 * @param {string} props.title - 主标题（有 breadcrumb 时以面包屑末级为准, 不再单独渲染）
 * @param {string} [props.subtitle] - 副标题/描述（同行小灰字, 超长截断, hover 可见全文）
 * @param {Array<{label: string, href?: string}>} [props.breadcrumb] - 面包屑
 * @param {ReactNode} [props.extra] - 右上角操作按钮区
 * @param {ReactNode} [props.back] - 是否显示返回按钮 + onBack 回调
 * @param {Function} [props.onBack] - 返回回调
 */
export default function PageHeader({
                                       title,
                                       subtitle,
                                       breadcrumb,
                                       extra,
                                       back,
                                       onBack,
                                   }) {
    const navigate = useNavigate()
    const handleBack = () => {
        if (onBack) onBack()
        else navigate(-1)
    }

    return (
        <div style={{
            marginBottom: 12,
            paddingBottom: 10,
            borderBottom: '1px solid #f1f5f9',
        }}>
            {/* 单行: 面包屑/标题 + 副标题 | 操作区 */}
            <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 12,
                flexWrap: 'wrap',
            }}>
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    minWidth: 0,
                    flexWrap: 'wrap',
                }}>
                    {back && (
                        <button
                            onClick={handleBack}
                            aria-label="返回上一页"
                            style={{
                                width: 28, height: 28,
                                borderRadius: 6,
                                border: '1px solid #e5e7eb',
                                background: '#ffffff',
                                color: '#6b7280',
                                cursor: 'pointer',
                                display: 'inline-flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                fontSize: 13,
                                transition: 'all 150ms ease',
                                flexShrink: 0,
                            }}
                            onMouseEnter={(e) => {
                                e.currentTarget.style.background = '#f8fafc'
                                e.currentTarget.style.color = '#111827'
                                e.currentTarget.style.borderColor = '#7c3aed'
                            }}
                            onMouseLeave={(e) => {
                                e.currentTarget.style.background = '#ffffff'
                                e.currentTarget.style.color = '#6b7280'
                                e.currentTarget.style.borderColor = '#e5e7eb'
                            }}
                        >
                            ←
                        </button>
                    )}
                    {breadcrumb && breadcrumb.length > 0 ? (
                        <Breadcrumb
                            items={breadcrumb.map((b, i) => {
                                const last = i === breadcrumb.length - 1
                                return {
                                    title: b.href && !last
                                        ? <a onClick={(e) => {
                                            e.preventDefault();
                                            navigate(b.href)
                                        }}>{b.label}</a>
                                        : <span style={last ? {
                                            fontWeight: 600,
                                            color: '#111827'
                                        } : undefined}>{b.label}</span>,
                                }
                            })}
                        />
                    ) : (
                        title && (
                            <span role="heading" aria-level={1} style={{
                                fontSize: 15,
                                fontWeight: 600,
                                color: '#111827',
                                lineHeight: 1.4,
                            }}>
                                {title}
                            </span>
                        )
                    )}
                    {subtitle && (
                        <span title={subtitle} style={{
                            fontSize: 12,
                            color: '#94a3b8',
                            lineHeight: 1.4,
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            maxWidth: 360,
                        }}>
                            {subtitle}
                        </span>
                    )}
                </div>
                {extra && (
                    <div style={{
                        display: 'flex',
                        gap: 8,
                        flexShrink: 0,
                    }}>
                        {extra}
                    </div>
                )}
            </div>
        </div>
    )
}
