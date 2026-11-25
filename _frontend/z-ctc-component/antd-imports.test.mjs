/**
 * antd 具名导入完整性守卫（2026-11-25）。
 *
 * 事故：z-ctc-component 的 DictPage 用了 14 处 `message.*`，却从未 import message
 * ⇒ 所有成功/失败提示都是 ReferenceError ⇒ 点「确定」既不关窗也不报错，
 *   列表加载失败也静默（页面只显示「字典分类 (0)」）。
 *
 * 同类事故的通用形态：**组件从别仓搬进组件包时，漏搬 import**（本仓 46 域
 * pages/components 收纳那次改动引入）。所以守卫覆盖全包，不只盯着某一页。
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import {fileURLToPath} from 'node:url'

const SRC = path.join(path.dirname(fileURLToPath(import.meta.url)), 'src')

const TOAST_GLOBALS = ['message', 'notification']

function walk(dir, out = []) {
    for (const entry of fs.readdirSync(dir, {withFileTypes: true})) {
        const p = path.join(dir, entry.name)
        if (entry.isDirectory()) walk(p, out)
        else if (/\.jsx?$/.test(entry.name)) out.push(p)
    }
    return out
}

/** 抓 from 'antd' 的具名导入（支持多行 import 块） */
function antdNamedImports(code) {
    const names = new Set()
    const re = /import\s*\{([\s\S]*?)\}\s*from\s*['"]antd['"]/g
    let m
    while ((m = re.exec(code))) {
        for (const raw of m[1].split(',')) {
            const n = raw.trim().split(/\s+as\s+/)[0].trim()
            if (n) names.add(n)
        }
    }
    return names
}

/** 返回该文件里用到的、但没从 antd 导入的 toast 具名 */
export function missingToastImports(code) {
    const imported = antdNamedImports(code)
    const used = new Set()
    for (const g of TOAST_GLOBALS) {
        const re = new RegExp(`(?<![\\w.$])${g}\\s*\\.`, 'g')
        if (re.test(code)) used.add(g)
    }
    return [...used].filter((g) => !imported.has(g))
}

test('守卫自身有效：能抓住漏 import 的片段，放过已 import 的片段', () => {
    assert.deepEqual(
        missingToastImports("import {Button} from 'antd'\nmessage.success('ok')"),
        ['message'],
        '漏 import 必须被抓到'
    )
    assert.deepEqual(
        missingToastImports("import {Button, message} from 'antd'\nmessage.success('ok')"),
        [],
        '已 import 必须放过'
    )
    assert.deepEqual(
        missingToastImports("import {\n  Button,\n  message,\n} from 'antd'\nmessage.warning('x')"),
        [],
        '多行 import 块必须识别到'
    )
    assert.deepEqual(
        missingToastImports("const o = {message: 1}\nfoo.message.success()"),
        [],
        '属性链上的同名标识不算全局 toast'
    )
})

test('组件包内每个用 toast 的文件都从 antd 具名导入了它', () => {
    const offenders = []
    for (const file of walk(SRC)) {
        const code = fs.readFileSync(file, 'utf8')
        const missing = missingToastImports(code)
        if (missing.length) {
            offenders.push(`${path.relative(SRC, file)} 缺 import: ${missing.join(', ')}`)
        }
    }
    assert.deepEqual(offenders, [], `以下文件用了 toast 却没从 antd 导入：\n${offenders.join('\n')}`)
})

test('DictPage 必须导入 message（本次事故点，单独钉死）', () => {
    const code = fs.readFileSync(path.join(SRC, 'components/DictPage/index.jsx'), 'utf8')
    assert.ok(
        antdNamedImports(code).has('message'),
        'DictPage 必须从 antd 导入 message —— 漏了会让所有提示静默失败'
    )
})
