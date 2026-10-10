import { Card, Typography } from 'antd'

const { Paragraph } = Typography

/** 认证配置（LDAP / OAuth2 / SAML） —— 占位页（lead 008 §13：菜单直达页不渲染 PageHeader）。 */
export default function AuthnPage() {
    return (
        <Card title="认证配置（LDAP / OAuth2 / SAML）">
            <Paragraph type="secondary">占位内容，由后续迭代填充（FEATURE035 / FEATURE036）。</Paragraph>
        </Card>
    )
}
