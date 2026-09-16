# v4-test/

V4 验证脚手架：**file: 协议跨仓本地消费 @yuku123/z-ctc-frontend-component**。

这不是生产代码，是 V4 验收用的**一次性测试**。验证完成后可以删除整个目录。

## 它验证什么

- 业务仓（如未来 z-opc）可以通过 `file:` 协议在**不 npm publish** 的前提下引用 z-ctc 顶层仓的组件层
- 修改 z-ctc-frontend-component 源码 → Vite HMR / 直接 bundle → 立刻在消费方可见（**秒级反馈**，避免 npm publish 流程）
- 验证 esbuild 能成功 bundle `@yuku123/z-ctc-frontend-component` → React 组件 → renderToString 产出正确 HTML

## 跑法

```bash
cd deploy/v4-test

# 1. 安装依赖（file: 协议 + react + esbuild）
npm install --install-links

# 2. esbuild 把 test-v4.mjs + react + 组件层打成一个 bundle
npm run bundle
#   产物：test-v4.bundle.mjs

# 3. 跑验证（4 个断言）
npm test
#   ✓ V4 验证通过：file: 协议跨仓消费 @yuku123/z-ctc-frontend-component 生效（4/4）
```

## 断言内容

| 断言 | 说明 |
|---|---|
| 来自组件层署名 | HTML 包含 `@yuku123/z-ctc-frontend-component` |
| 包含自定义 name=V4 | props 正确穿透到组件 |
| 渲染 antd 按钮 | `<button class="ant-btn">` 出现 |
| 包含 Hello 默认前缀 | 组件模板渲染成功 |

## 为什么用 `--install-links`

> 详见 `lead/005_技术架构/005_前端工程与中间件部署架构规范.md §3`

`file:` 协议默认是**拷贝**（npm 8+），修改组件层源码不会同步。
加 `--install-links` 改成**软链**（symlink），源码改完消费方立即生效。

## 删除

验证通过后：
```bash
rm -rf deploy/v4-test
```

## 许可

MIT