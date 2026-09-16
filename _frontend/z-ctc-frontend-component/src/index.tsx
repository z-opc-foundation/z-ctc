// z-ctc 4A 组件层（library mode）
// 暴露给业务方（z-opc / 其它业务项目）通过 file: 或 npm 消费
import { Button } from 'antd';
import type { FC } from 'react';

export interface HelloCtcProps {
    name?: string;
}

/** 4A 中心示例组件 —— 演示业务方怎么消费 */
export const HelloCtc: FC<HelloCtcProps> = ({ name = '4A' }) => {
    return <Button type="primary">Hello {name}（来自 @yuku123/z-ctc-frontend-component）</Button>;
};

/** 重导出 antd 常用组件，省得业务方自己再 import */
export { Button, Table, Form, Input, Card, Space } from 'antd';

export default { HelloCtc };