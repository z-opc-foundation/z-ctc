import { LockOutlined } from '@ant-design/icons'
import { EmptyState } from '@yuku123/z-frontend-common/ui'
/**
 * 认证配置（密码策略 / LDAP / OAuth2 / SAML）—— 后端尚未提供配置端点，
 * 因此这里只给一个明确的空态，不做假表单（lead 008 §2：占位不等于可用）。
 */
export default function AuthnPage() {
    return (
        <div>
            <EmptyState
                icon={<LockOutlined />}
                title="认证源配置尚未接入"
                description="后端还没有暴露认证源配置端点；接入后这里会提供密码策略与第三方登录的维护入口。"
            />
        </div>
    )
}
