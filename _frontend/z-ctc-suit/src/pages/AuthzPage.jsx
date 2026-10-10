import { Card, Typography } from 'antd'

const { Paragraph } = Typography

/** 授权策略（RBAC / ABAC） —— 占位页（lead 008 §13：菜单直达页不渲染 PageHeader）。 */
export default function AuthzPage() {
    return (
        <Card title="授权策略（RBAC / ABAC）">
            <Paragraph type="secondary">占位内容，由后续迭代填充（FEATURE035 / FEATURE036）。</Paragraph>
        </Card>
    )
}
