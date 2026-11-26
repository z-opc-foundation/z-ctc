// V4 验证脚手架入口 —— 真正逻辑在 test-v4.mjs。
// 通过 esbuild 把整个依赖图（react + 组件层 + 测试代码）打成单一 .mjs，
// 然后用 node 跑。这样不依赖任何运行时 bundler。
import { build } from 'esbuild';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const here = path.dirname(fileURLToPath(import.meta.url));
const local = (p) => path.join(here, 'node_modules', p);

await build({
    entryPoints: ['test-v4.mjs'],
    bundle: true,
    format: 'esm',
    platform: 'node',
    target: 'node18',
    outfile: 'test-v4.bundle.mjs',
    banner: { js: "import { createRequire } from 'node:module'; const require = createRequire(import.meta.url);" },
    // file: 依赖是符号链接，esbuild 沿链接路径向上查 node_modules 会穿过链接
    // 命中组件仓自己的 react（19.x，其 peerDependencies 即 ^19）——于是组件层
    // 产 react 19 元素、本目录 react-dom@18 渲染，19 的
    // react.transitional.element 符号在 18 渲染器里不被认作 element，
    // 直接 "Objects are not valid as a React child"。alias 把全图的
    // react / react-dom（含子路径）统一钉到本目录这一份。
    alias: {
        react: local('react'),
        'react-dom': local('react-dom'),
    },
    logLevel: 'warning'
});

console.log('✓ bundle 完成 → test-v4.bundle.mjs');
console.log('  跑 node test-v4.bundle.mjs 即可验证');