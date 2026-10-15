import {radius, shadow} from './tokens'

/**
 * 深色 CTA 横幅 - 借鉴 wenming7 "FOR DEVELOPERS" 区
 *
 * 视觉:
 *   ┌──────────────────────────────────────────────┐
 *   │  [标签]                                       │
 *   │  把你的想法做成产品                            │  ← 深色渐变背景
 *   │  副标题/描述文案                                │
 *   │                              [行动按钮 →]     │
 *   └──────────────────────────────────────────────┘
 *
 * @param {Object} props
 * @param {string} [props.tag] - 顶部小标签 (如 "FOR DEVELOPERS")
 * @param {string} props.title - 主标题
 * @param {string} [props.description] - 描述
 * @param {string} [props.buttonText] - 按钮文案
 * @param {string} [props.buttonHref] - 跳转路径
 */
export default function DarkBanner({
                                       tag,
                                       title,
                                       description,
                                       buttonText,
                                       buttonHref,
                                       onButtonClick,
                                   }) {
    return (
        <div style={{
            position: 'relative',
            borderRadius: radius.lg,
            padding: '40px 48px',
            background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #312e81 100%)',
            color: '#ffffff',
            overflow: 'hidden',
            boxShadow: shadow.lg,
        }}>
            {/* 装饰光晕 */}
            <div style={{
                position: 'absolute',
                top: -64,
                right: -64,
                width: 200,
                height: 200,
                borderRadius: '50%',
                background: 'radial-gradient(circle, rgba(139, 92, 246, 0.4) 0%, transparent 70%)',
                pointerEvents: 'none',
            }}/>
            <div style={{
                position: 'absolute',
                bottom: -48,
                left: '30%',
                width: 160,
                height: 160,
                borderRadius: '50%',
                background: 'radial-gradient(circle, rgba(59, 130, 246, 0.3) 0%, transparent 70%)',
                pointerEvents: 'none',
            }}/>

            <div style={{
                position: 'relative',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 32,
                flexWrap: 'wrap',
            }}>
                <div style={{flex: 1, minWidth: 240}}>
                    {tag && (
                        <div style={{
                            fontSize: 11,
                            fontWeight: 600,
                            letterSpacing: '0.12em',
                            color: '#a5b4fc',
                            textTransform: 'uppercase',
                            marginBottom: 12,
                        }}>
                            {tag}
                        </div>
                    )}
                    <div style={{
                        fontSize: 24,
                        fontWeight: 700,
                        lineHeight: 1.3,
                        marginBottom: 8,
                    }}>
                        {title}
                    </div>
                    {description && (
                        <div style={{
                            fontSize: 14,
                            color: 'rgba(255, 255, 255, 0.7)',
                            lineHeight: 1.6,
                            maxWidth: 540,
                        }}>
                            {description}
                        </div>
                    )}
                </div>

                {(buttonText && (buttonHref || onButtonClick)) && (
                    <a
                        href={buttonHref || '#'}
                        onClick={(e) => {
                            if (onButtonClick) {
                                e.preventDefault()
                                onButtonClick()
                            }
                        }}
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 8,
                            padding: '12px 24px',
                            background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                            color: '#ffffff',
                            borderRadius: 999,
                            fontSize: 14,
                            fontWeight: 600,
                            textDecoration: 'none',
                            boxShadow: '0 4px 12px rgba(16, 185, 129, 0.4)',
                            transition: 'transform 150ms ease, box-shadow 150ms ease',
                            whiteSpace: 'nowrap',
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.transform = 'translateY(-1px)'
                            e.currentTarget.style.boxShadow = '0 6px 16px rgba(16, 185, 129, 0.5)'
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.transform = 'translateY(0)'
                            e.currentTarget.style.boxShadow = '0 4px 12px rgba(16, 185, 129, 0.4)'
                        }}
                    >
                        {buttonText}
                        <span>→</span>
                    </a>
                )}
            </div>
        </div>
    )
}
