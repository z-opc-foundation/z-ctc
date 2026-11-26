/**
 * 字典管理页的局部样式（运行时自注入）。
 *
 * 为什么不用包内 .css 文件：z-ctc-component 是 library mode，vite 把样式抽成
 * dist/z-ctc-component.css，而包 JS 并不 import 它 ⇒ 主壳打包时不加载 ⇒
 * 包内 CSS 实际是死的（实测主壳产物里 .ctc-tenant-tree 零命中）。
 * 组件自注入 <style> 才能保证宿主（主壳 / z-ctc-admin 独立 UI）都生效，
 * 与 z-frontend-common 的 MenuTree 同一套路。
 *
 * 所有类名带 .zdict- 前缀，避免污染宿主。
 */

const STYLE_ID = 'zdict-style'

const CSS = `
.zdict-modal .ant-modal-content{padding:22px 26px 18px;border-radius:10px;box-shadow:0 14px 34px rgba(15,27,45,.14)}
.zdict-modal .ant-modal-header{margin-bottom:16px}
.zdict-modal .ant-modal-body{padding-top:0}
.zdict-modal .ant-modal-footer{margin-top:20px;padding-top:14px;border-top:1px solid #f0f1f3}
.zdict-modal .ant-modal-footer .ant-btn{min-width:76px;border-radius:8px}
.zdict-title{display:inline-flex;align-items:center;gap:10px;font-size:16px;font-weight:600;color:#1f2329}
.zdict-title-ic{display:inline-flex;align-items:center;justify-content:center;width:26px;height:26px;border-radius:8px;
  background:#e8f3ff;color:#0052d9;font-size:14px}
.zdict-form .ant-form-item{margin-bottom:16px}
.zdict-form .ant-form-item-label{padding:0 0 6px}
.zdict-form .ant-form-item-label>label{font-size:13px;font-weight:500;color:#1f2329;height:auto}
.zdict-form .ant-form-item-extra{font-size:12px;line-height:1.5;color:#8a9099;margin-top:5px}
.zdict-form .ant-input,.zdict-form .ant-input-number,.zdict-form .ant-select-selector{border-radius:9px !important}
.zdict-form .ant-input,.zdict-form .ant-input-affix-wrapper,.zdict-form .ant-select-selector{font-size:13px}
.zdict-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 18px}
.zdict-full{grid-column:1/-1}
.zdict-section{grid-column:1/-1;display:flex;align-items:center;gap:10px;margin:4px 0 12px;
  font-size:12px;font-weight:600;color:#8a9099;letter-spacing:.03em}
.zdict-section::after{content:"";flex:1;height:1px;background:#edeff2}
.zdict-code .ant-input{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:13px}
.zdict-hint{display:inline-flex;align-items:center;gap:6px;margin:-6px 0 14px;padding:8px 10px;border-radius:8px;
  background:#f7f8fa;color:#6b7178;font-size:12px;line-height:1.5}
@media (max-width:640px){
  .zdict-grid{grid-template-columns:1fr}
}
`

export function ensureDictStyles() {
    if (typeof document === 'undefined') return
    if (document.getElementById(STYLE_ID)) return
    const el = document.createElement('style')
    el.id = STYLE_ID
    el.textContent = CSS
    document.head.appendChild(el)
}

export default ensureDictStyles
