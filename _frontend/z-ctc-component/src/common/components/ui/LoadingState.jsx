import {Skeleton} from 'antd'
import {radius} from './tokens'

/**
 * LoadingState - 统一骨架屏 - 借鉴 ui-ux-pro-max P3: Reserve space (CLS < 0.1).
 *
 * 原则:
 *   - 提前占位避免内容跳动 (CLS)
 *   - 不用 spinner (除全屏初次加载), 用骨架屏更专业
 *   - 提供常见布局预设
 *
 * @param {Object} props
 * @param {'cards'|'table'|'list'|'detail'} [props.variant] - 预设布局
 * @param {number} [props.rows] - 行数
 * @param {boolean} [props.active] - 是否显示流光效果
 * @param {Object} [props.style]
 */
export default function LoadingState({
                                         variant = 'cards',
                                         rows = 4,
                                         active = true,
                                         style,
                                     }) {
    if (variant === 'cards') {
        return (
            <div
                role="status"
                aria-busy="true"
                aria-label="加载中"
                style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
                    gap: 16,
                    ...style,
                }}
            >
                {Array.from({length: rows}).map((_, i) => (
                    <div
                        key={i}
                        style={{
                            background: '#ffffff',
                            borderRadius: radius.lg,
                            overflow: 'hidden',
                            border: '1px solid #f1f5f9',
                        }}
                    >
                        <Skeleton.Image
                            active={active}
                            style={{width: '100%', height: 140, borderRadius: 0}}
                        />
                        <div style={{padding: 16}}>
                            <Skeleton active={active} paragraph={{rows: 2}} title={{width: '60%'}}/>
                        </div>
                    </div>
                ))}
            </div>
        )
    }

    if (variant === 'table') {
        return (
            <div
                role="status"
                aria-busy="true"
                aria-label="加载中"
                style={{
                    background: '#ffffff',
                    padding: 24,
                    borderRadius: radius.md,
                    ...style,
                }}
            >
                <Skeleton active={active} paragraph={{rows}}/>
            </div>
        )
    }

    if (variant === 'list') {
        return (
            <div role="status" aria-busy="true" aria-label="加载中" style={style}>
                {Array.from({length: rows}).map((_, i) => (
                    <div
                        key={i}
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 12,
                            padding: '12px 0',
                            borderBottom: i < rows - 1 ? '1px solid #f1f5f9' : 'none',
                        }}
                    >
                        <Skeleton.Avatar active={active} size="default"/>
                        <Skeleton active={active} paragraph={{rows: 1}} title={{width: 120}} style={{flex: 1}}/>
                    </div>
                ))}
            </div>
        )
    }

    // detail: 用于详情页
    return (
        <div
            role="status"
            aria-busy="true"
            aria-label="加载中"
            style={{
                background: '#ffffff',
                padding: 32,
                borderRadius: radius.md,
                ...style,
            }}
        >
            <Skeleton active={active} title={{width: '40%'}} paragraph={{rows}}/>
        </div>
    )
}
