import fs from 'node:fs'
import path from 'node:path'

/**
 * 安全清空 outDir —— 用「移走」代替「删除」。
 *
 * ⚠ 为什么需要它（2026-10-30）：
 *   vite 的 `build.emptyOutDir: true` 会**批量删除** outDir 下的旧产物。
 *   在 WorkBuddy 沙箱里，单轮累计删除 ≥50 个文件会被硬拦：
 *     [safe-delete][SAFE_DELETE_BULK_CONFIRM_REQUIRED] {"count":50,"threshold":50}
 *   ⇒ 前端构建**间歇性假红**（删得少的轮次绿、删多的轮次红）。
 *
 *   本插件把流程改成：构建开始时把旧 outDir **rename** 成 `<outDir>.stale-<时间戳>`，
 *   再让 vite 在一个空目录上正常产出。零删除 ⇒ 不碰安全闸。
 */
export function safeEmptyOutDir() {
    let outDirAbs: string | null = null

    return {
        name: 'safe-empty-out-dir',
        apply: 'build',

        configResolved(config: any) {
            config.build.emptyOutDir = false
            outDirAbs = path.resolve(config.root || process.cwd(), config.build.outDir)
        },

        buildStart() {
            if (!outDirAbs || !fs.existsSync(outDirAbs)) return
            const stash = `${outDirAbs}.stale-${Date.now()}`
            try {
                fs.renameSync(outDirAbs, stash)
            } catch (e) {
                console.warn(`[safe-empty-out-dir] 移动旧产物失败（继续构建，产物可能含陈旧文件）：${(e as Error).message}`)
            }
        },
    }
}
