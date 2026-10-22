import {ComponentDoc} from '@yuku123/z-frontend-common/ui'
import docMarkdown from '../DOC.md?raw'

/**
 * 组件文档页（lead 008 §7）—— 文档内容来自本包 src/DOC.md。
 *
 * 为什么用 `?raw`：vite 编译期把 DOC.md 内联成字符串，零额外请求；
 * 文档随包发布，宿主不用管文件怎么放。
 *
 * 为什么用基座的 ComponentDoc：左 240px TOC + 右正文两栏这套渲染逻辑在 25 个仓里逐字重复，
 * 基座已经收成共享组件（@yuku123/z-frontend-common/ui），各仓只提供 DOC.md。
 *
 * 主壳按**统一名**取本页：`lazyNamed(() => import('@yuku123/z-ctc-component'), 'ComponentDocPage')`
 * ⇒ 本仓主入口必须具名再导出 ComponentDocPage（见 src/index.tsx）。
 */
export default function ComponentDocPage() {
    return <ComponentDoc markdown={docMarkdown} title="组件文档 — z-ctc" />
}
