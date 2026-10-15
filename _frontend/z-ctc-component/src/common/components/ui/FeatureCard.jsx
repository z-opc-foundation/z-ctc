import {useState} from 'react'
import {palette, radius, shadow, space} from './tokens'

/**
 * 功能卡 - 借鉴 wenming7/marketplace 应用卡设计
 *
 * 视觉:
 *   ┌────────────────────────────┐
 *   │ [封面/图标区 16:9 / 4:3]   │  ← 可选渐变色背景 + 大图标
 *   ├────────────────────────────┤
 *   │ [chip] [chip]              │  ← 标签 chips
 *   │ 模块标题                    │
 *   │ 副描述 · 1234 次使用         │  ← 元信息
 *   └────────────────────────────�
 *
 * 单位面积信息密度高于 antd Card, 视觉更现代化.
 *
 * @param {Object} props
 * @param {string} props.title - 标题
 * @param {string} [props.description] - 描述
 * @param {ReactNode} [props.icon] - 封面图标 (如不传 cover 则渲染渐变封面)
 * @param {string} [props.cover] - 封面图片 URL
 * @param {string} [props.color] - 配色 (用于渐变封面)
 * @param {Array<{label: string, color?: string}>} [props.tags] - 标签 chips
 * @param {string} [props.meta] - 元信息 (如 "1234 次使用")
 * @param {string} [props.href] - 跳转路径
 * @param {ReactNode} [props.extra] - 右上角额外内容 (如 NEW 角标)
 */
export default function FeatureCard({
                                        title,
                                        description,
                                        icon,
                                        cover,
                                        color = 'violet',
                                        tags = [],
                                        meta,
                                        href,
                                        extra,
                                    }) {
    const c = palette[color] || palette.violet
    const [hover, setHover] = useState(false)

    const handleClick = () => {
        if (href) {
            if (typeof window !== 'undefined') {
                window.location.href = href
            }
        }
    }

    return (
        <div
            onClick={handleClick}
            onMouseEnter={() => setHover(true)}
            onMouseLeave={() => setHover(false)}
            style={{
                background: '#ffffff',
                borderRadius: radius.lg,
                overflow: 'hidden',
                cursor: href ? 'pointer' : 'default',
                boxShadow: hover ? shadow.lg : shadow.sm,
                transform: hover ? 'translateY(-2px)' : 'translateY(0)',
                transition: 'transform 200ms ease, box-shadow 200ms ease',
                border: '1px solid #f1f5f9',
                display: 'flex',
                flexDirection: 'column',
            }}
        >
            {/* 封面区 */}
            <div style={{
                position: 'relative',
                aspectRatio: '16 / 9',
                background: cover ? '#f1f5f9' : c.gradient,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                overflow: 'hidden',
            }}>
                {cover ? (
                    <img
                        src={cover}
                        alt={title}
                        style={{
                            width: '100%',
                            height: '100%',
                            objectFit: 'cover',
                        }}
                    />
                ) : (
                    <div style={{
                        fontSize: 48,
                        color: 'rgba(255, 255, 255, 0.9)',
                        display: 'flex',
                    }}>
                        {icon}
                    </div>
                )}
                {/* 装饰圆 */}
                {!cover && (
                    <div style={{
                        position: 'absolute',
                        top: -24,
                        right: -24,
                        width: 80,
                        height: 80,
                        borderRadius: '50%',
                        background: 'rgba(255, 255, 255, 0.15)',
                    }}/>
                )}
                {/* 右上角额外内容 (如 NEW 角标) */}
                {extra && (
                    <div style={{
                        position: 'absolute',
                        top: 8,
                        right: 8,
                    }}>
                        {extra}
                    </div>
                )}
            </div>

            {/* 内容区 */}
            <div style={{
                padding: space[4],
                display: 'flex',
                flexDirection: 'column',
                gap: space[2],
                flex: 1,
            }}>
                {/* chips */}
                {tags.length > 0 && (
                    <div style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        gap: 6,
                    }}>
                        {tags.slice(0, 3).map((t, i) => {
                            const tc = palette[t.color] || palette.blue
                            return (
                                <span
                                    key={i}
                                    style={{
                                        fontSize: 11,
                                        fontWeight: 500,
                                        padding: '2px 8px',
                                        borderRadius: 999,
                                        background: tc.soft,
                                        color: tc.bg,
                                    }}
                                >
                                    {t.label}
                                </span>
                            )
                        })}
                    </div>
                )}

                {/* 标题 */}
                <div style={{
                    fontSize: 15,
                    fontWeight: 600,
                    color: '#111827',
                    lineHeight: 1.3,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: 1,
                    WebkitBoxOrient: 'vertical',
                }}>
                    {title}
                </div>

                {/* 描述 */}
                {description && (
                    <div style={{
                        fontSize: 12,
                        color: '#6b7280',
                        lineHeight: 1.5,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        minHeight: 36,
                    }}>
                        {description}
                    </div>
                )}

                {/* 元信息 */}
                {meta && (
                    <div style={{
                        fontSize: 11,
                        color: '#9ca3af',
                        marginTop: 'auto',
                        paddingTop: space[2],
                        borderTop: '1px dashed #f1f5f9',
                    }}>
                        {meta}
                    </div>
                )}
            </div>
        </div>
    )
}
