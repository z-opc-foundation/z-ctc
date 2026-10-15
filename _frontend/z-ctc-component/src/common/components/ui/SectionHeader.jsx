import CategoryDot from './CategoryDot'

/**
 * 段落标题 - 借鉴 wenming7/marketplace 分类段头
 *
 * 视觉:
 *   � 热门爆款                          全部 >
 *   圆点 + 标题                          右侧跳转链接
 *
 * @param {Object} props
 * @param {string} props.color - 圆点配色
 * @param {string} props.title - 标题
 * @param {string} [props.subtitle] - 副标题
 * @param {string} [props.moreText] - "全部 >" 文案
 * @param {string} [props.moreHref] - 跳转路径
 */
export default function SectionHeader({
                                          color = 'blue',
                                          title,
                                          subtitle,
                                          moreText = '全部',
                                          moreHref,
                                          onMore,
                                      }) {
    return (
        <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: 16,
        }}>
            <div style={{display: 'flex', alignItems: 'baseline', gap: 12}}>
                <CategoryDot color={color} label={title} size={10}/>
                {subtitle && (
                    <span style={{
                        fontSize: 13,
                        color: '#6b7280',
                        fontWeight: 400,
                    }}>
                        {subtitle}
                    </span>
                )}
            </div>
            {(moreHref || onMore) && (
                <a
                    href={moreHref}
                    onClick={(e) => {
                        if (onMore) {
                            e.preventDefault()
                            onMore()
                        }
                    }}
                    style={{
                        fontSize: 13,
                        color: '#6b7280',
                        textDecoration: 'none',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 4,
                        cursor: 'pointer',
                        transition: 'color 150ms ease',
                    }}
                    onMouseEnter={(e) => {
                        e.currentTarget.style.color = '#7c3aed'
                    }}
                    onMouseLeave={(e) => {
                        e.currentTarget.style.color = '#6b7280'
                    }}
                >
                    {moreText}
                    <span style={{fontSize: 12}}>›</span>
                </a>
            )}
        </div>
    )
}
