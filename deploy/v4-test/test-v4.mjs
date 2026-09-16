// V4 验证脚本：跨仓 file: 协议 + import React 组件
// 用 esbuild bundle 整个依赖图（react + 组件层 + 测试代码），跑在 Node 里
import React from 'react';
import { renderToString } from 'react-dom/server';
import { HelloCtc } from '@yuku123/z-ctc-frontend-component';

// z-ctc 组件层 z-ctc-frontend-component/src/index.ts 暴露的 HelloCtc 组件
// props: { name?: string }
// 渲染 <Button type="primary">Hello {name}（来自 @yuku123/z-ctc-frontend-component）</Button>
const html = renderToString(React.createElement(HelloCtc, { name: 'V4' }));

console.log('=== V4 验证输出 ===');
console.log('组件 HTML:', html);
console.log('');

console.log('=== 断言 ===');
const assertions = [
    ['来自组件层署名', html.includes('@yuku123/z-ctc-frontend-component')],
    ['包含自定义 name=V4', html.includes('Hello V4')],
    ['渲染 antd 按钮', html.includes('ant-btn')],
    ['包含 Hello 默认前缀', html.includes('Hello')],
];
let pass = 0;
for (const [name, ok] of assertions) {
    console.log(`  ${ok ? '✓' : '✗'} ${name}`);
    if (ok) pass++;
}
console.log('');

if (pass === assertions.length) {
    console.log(`✓ V4 验证通过：file: 协议跨仓消费 @yuku123/z-ctc-frontend-component 生效（${pass}/${assertions.length}）`);
    process.exit(0);
} else {
    console.error(`✗ V4 验证失败（${pass}/${assertions.length}）`);
    process.exit(1);
}