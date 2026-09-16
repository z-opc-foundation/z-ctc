// V4 验证脚手架入口 —— 真正逻辑在 test-v4.mjs。
// 通过 esbuild 把整个依赖图（react + 组件层 + 测试代码）打成单一 .mjs，
// 然后用 node 跑。这样不依赖任何运行时 bundler。
import { build } from 'esbuild';
import { writeFile } from 'node:fs/promises';

await build({
    entryPoints: ['test-v4.mjs'],
    bundle: true,
    format: 'esm',
    platform: 'node',
    target: 'node18',
    outfile: 'test-v4.bundle.mjs',
    banner: { js: "import { createRequire } from 'node:module'; const require = createRequire(import.meta.url);" },
    logLevel: 'warning'
});

console.log('✓ bundle 完成 → test-v4.bundle.mjs');
console.log('  跑 node test-v4.bundle.mjs 即可验证');