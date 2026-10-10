import { Card, Typography } from 'antd'

const { Paragraph } = Typography

/** 审计日志 —— 占位页（lead 008 §13：菜单直达页不渲染 PageHeader）。 */
export default function AuditPage() {
    return (
        <Card title="审计日志">
            <Paragraph type="secondary">占位内容，由后续迭代填充（FEATURE035 / FEATURE036）。</Paragraph>
        </Card>
    )
}
