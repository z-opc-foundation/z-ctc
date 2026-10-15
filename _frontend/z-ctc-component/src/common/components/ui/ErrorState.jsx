import {Button} from 'antd'
import {ExclamationCircleOutlined, ReloadOutlined} from '@ant-design/icons'
import {neutral, radius} from './tokens'

/**
 * 统一错误状态 - 借鉴 ui-ux-pro-max P1/P8 + frontend-design "Treat failure as moments for direction".
 *
 * 原则:
 *   - 错误就近显示 (不要只发一个顶部 message)
 *   - 明确"发生了什么" + "下一步怎么做"
 *   - 提供重试入口
 *   - 不用道歉 ("对不起..." 是 AI 默认)
 *
 * @param {Object} props
 * @param {Error|string} props.error - 错误对象或文案
 * @param {string} [props.title] - 主标题
 * @param {string} [props.description] - 解释发生了什么 + 下一步
 * @param {Function} [props.onRetry] - 重试回调
 * @param {string} [props.retryText] - 重试按钮文案
 * @param {'inline'|'full'} [props.variant] - inline 紧凑嵌入页面, full 占满卡片
 * @param {Object} [props.style]
 */
export default function ErrorState({
                                       error,
                                       title = '加载失败',
                                       description,
                                       onRetry,
                                       retryText = '重新加载',
                                       variant = 'inline',
                                       style,
                                   }) {
    const isFull = variant === 'full'

    // 提取错误消息。**先取后端响应体里的业务 message**，再退回 axios/字符串：
    // HTTP 500 时 axios 只给 "Request failed with status code 500"，而真因在同一个响应的 `message` 里
    // （实测 `/api/config/pageConfig` ⇒ "Unknown column 'encrypted_data_key' in 'field list'"）。
    // 原来无论拿到什么都在末尾追加"请检查网络或稍后重试" —— 那是把**服务端缺陷**说成**用户网络问题**。
    const body = error?.response?.data
    const backendMsg = (body && typeof body === 'object')
        ? (typeof body.message === 'string' && body.message) || (typeof body.msg === 'string' && body.msg) || null
        : null
    const errMsg = backendMsg
        || (typeof error === 'string'
            ? error
            : (error?.message || error?.msg || (error ? String(error) : null)))

    const finalDescription = description
        || (backendMsg
            ? `原因: ${backendMsg}`
            : (errMsg ? `原因: ${errMsg}。请检查网络或稍后重试。` : '请检查网络或稍后重试。'))

    return (
        <div
            role="alert"
            aria-live="assertive"
            style={{
                padding: isFull ? '48px 24px' : '20px 24px',
                background: isFull ? '#ffffff' : '#fef2f2',
                border: isFull ? `1px dashed ${neutral.border}` : '1px solid #fecaca',
                borderRadius: radius.md,
                textAlign: isFull ? 'center' : 'left',
                display: 'flex',
                flexDirection: isFull ? 'column' : 'row',
                alignItems: isFull ? 'center' : 'flex-start',
                gap: isFull ? 12 : 16,
                ...style,
            }}
        >
            <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: isFull ? 56 : 36, height: isFull ? 56 : 36,
                borderRadius: '50%',
                background: isFull ? '#fef2f2' : '#ffffff',
                color: '#dc2626',
                fontSize: isFull ? 28 : 18,
                flexShrink: 0,
            }}>
                <ExclamationCircleOutlined/>
            </div>
            <div style={{flex: 1, minWidth: 0}}>
                <div style={{
                    fontSize: isFull ? 16 : 14,
                    fontWeight: 600,
                    color: '#991b1b',
                    marginBottom: 4,
                }}>
                    {title}
                </div>
                <div style={{
                    fontSize: 13,
                    color: '#7f1d1d',
                    lineHeight: 1.6,
                    wordBreak: 'break-word',
                }}>
                    {finalDescription}
                </div>
                {onRetry && (
                    <Button
                        type="primary"
                        danger
                        icon={<ReloadOutlined/>}
                        onClick={onRetry}
                        size={isFull ? 'middle' : 'small'}
                        style={{marginTop: 12}}
                    >
                        {retryText}
                    </Button>
                )}
            </div>
        </div>
    )
}
