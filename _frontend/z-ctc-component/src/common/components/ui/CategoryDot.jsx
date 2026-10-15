import {palette} from './tokens'

/**
 * 分类圆点 - 借鉴 wenming7 分类标识
 *
 * 用法:
 *   <CategoryDot color="pink" />               // 仅圆点
 *   <CategoryDot color="blue" label="对话" />  // 圆点 + 文字
 *   <CategoryDot color="green" size={10} />    // 自定义大小
 *
 * @param {Object} props
 * @param {string} props.color - 配色 (pink|blue|green|orange|violet|cyan|red|yellow)
 * @param {string} [props.label] - 可选标签文字
 * @param {number} [props.size] - 圆点直径 (默认 8)
 */
export default function CategoryDot({color = 'blue', label, size = 8}) {
    const c = palette[color] || palette.blue
    return (
        <span style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 8,
            fontSize: 14,
            fontWeight: 600,
            color: '#111827',
        }}>
            <span style={{
                width: size,
                height: size,
                borderRadius: '50%',
                background: c.bg,
                boxShadow: `0 0 0 3px ${c.soft}`,
                flexShrink: 0,
            }}/>
            {label && <span>{label}</span>}
        </span>
    )
}
