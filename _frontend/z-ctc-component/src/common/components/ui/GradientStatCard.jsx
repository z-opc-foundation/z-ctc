import {useEffect, useRef, useState} from 'react'
import {palette, radius, shadow, space} from './tokens'

/**
 * 渐变数据卡 - 借鉴 wenming7/marketplace 数据卡设计
 *
 * 视觉特征 (相对 antd Statistic 优势):
 *   - 渐变彩色背景 (pink/blue/green/orange/violet/cyan)
 *   - 圆角图标 + 大数字 + 小标签 (单位面积信息密度高)
 *   - 数字滚动动画 (现代化微交互)
 *   - 悬浮微抬升 + 阴影变化
 *   - 可选 trend (同比/环比箭头)
 *
 * @param {Object} props
 * @param {ReactNode} props.icon - 图标 (React 节点, 通常是 antd icon)
 * @param {string} props.label - 标签 (如 "上架应用")
 * @param {number} props.value - 数值
 * @param {string} [props.suffix] - 单位 (如 "万", "%", "+")
 * @param {string} [props.color] - 配色 key (pink|blue|green|orange|violet|cyan)
 * @param {number|string} [props.trend] - 同比变化 (正数绿箭头上升, 负数红箭头下降)
 * @param {string} [props.href] - 点击跳转路径
 * @param {boolean} [props.loading] - 加载态
 * @param {string} [props.precision] - 小数位
 */
export default function GradientStatCard({
                                             icon,
                                             label,
                                             value,
                                             suffix,
                                             color = 'blue',
                                             trend,
                                             href,
                                             loading = false,
                                             precision = 0,
                                         }) {
    const c = palette[color] || palette.blue
    const [display, setDisplay] = useState(0)
    const prevRef = useRef(0)

    // 数字滚动动画
    useEffect(() => {
        if (loading || value == null) return
        const from = prevRef.current
        const to = Number(value) || 0
        if (from === to) {
            setDisplay(to)
            return
        }
        const duration = 600
        const start = performance.now()
        let raf
        const tick = (now) => {
            const p = Math.min(1, (now - start) / duration)
            // easeOutCubic
            const eased = 1 - Math.pow(1 - p, 3)
            const cur = from + (to - from) * eased
            setDisplay(cur)
            if (p < 1) raf = requestAnimationFrame(tick)
            else prevRef.current = to
        }
        raf = requestAnimationFrame(tick)
        return () => raf && cancelAnimationFrame(raf)
    }, [value, loading])

    const handleClick = () => {
        if (href && typeof window !== 'undefined') {
            window.location.href = href
        }
    }

    const formatted = loading
        ? '--'
        : Number(display).toLocaleString('zh-CN', {
            minimumFractionDigits: precision,
            maximumFractionDigits: precision,
        })

    const trendNum = trend != null ? Number(trend) : null
    const trendUp = trendNum != null && trendNum >= 0

    return (
        <div
            onClick={handleClick}
            style={{
                position: 'relative',
                background: c.gradient,
                borderRadius: radius.lg,
                padding: space[5],
                color: c.fg,
                cursor: href ? 'pointer' : 'default',
                boxShadow: shadow.md,
                overflow: 'hidden',
                transition: 'transform 200ms ease, box-shadow 200ms ease',
                minHeight: 110,
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
            }}
            onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)'
                e.currentTarget.style.boxShadow = shadow.lg
            }}
            onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)'
                e.currentTarget.style.boxShadow = shadow.md
            }}
        >
            {/* 右上角装饰圆 - 借鉴 wenming7 数据卡的光感 */}
            <div
                style={{
                    position: 'absolute',
                    top: -32,
                    right: -32,
                    width: 96,
                    height: 96,
                    borderRadius: '50%',
                    background: 'rgba(255, 255, 255, 0.15)',
                    pointerEvents: 'none',
                }}
            />

            {/* 图标 + 标签 */}
            <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: space[2],
                position: 'relative',
            }}>
                <div style={{
                    width: 36,
                    height: 36,
                    borderRadius: radius.sm,
                    background: 'rgba(255, 255, 255, 0.20)',
                    backdropFilter: 'blur(8px)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 18,
                }}>
                    {icon}
                </div>
                <div style={{
                    fontSize: 13,
                    fontWeight: 500,
                    opacity: 0.92,
                }}>
                    {label}
                </div>
            </div>

            {/* 数值 + trend */}
            <div style={{
                display: 'flex',
                alignItems: 'baseline',
                gap: space[2],
                position: 'relative',
                marginTop: space[3],
            }}>
                <div style={{
                    fontSize: 30,
                    fontWeight: 700,
                    lineHeight: 1.1,
                    letterSpacing: '-0.02em',
                    fontVariantNumeric: 'tabular-nums',
                }}>
                    {formatted}
                </div>
                {suffix && (
                    <div style={{fontSize: 14, opacity: 0.85, fontWeight: 500}}>
                        {suffix}
                    </div>
                )}
                {trendNum != null && (
                    <div style={{
                        marginLeft: 'auto',
                        fontSize: 12,
                        fontWeight: 600,
                        padding: '2px 8px',
                        borderRadius: 999,
                        background: trendUp
                            ? 'rgba(255, 255, 255, 0.22)'
                            : 'rgba(0, 0, 0, 0.18)',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 2,
                    }}>
                        <span style={{fontSize: 10}}>
                            {trendUp ? '▲' : '▼'}
                        </span>
                        {Math.abs(trendNum).toFixed(1)}%
                    </div>
                )}
            </div>
        </div>
    )
}
