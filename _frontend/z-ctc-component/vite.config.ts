import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import dts from 'vite-plugin-dts';
import path from 'node:path';

// z-ctc 组件层：library mode，输出 ES + CJS + d.ts
// 与 z-schedule-frontend-component 完全同形态（详见
// lead/005_技术架构/005_前端工程与中间件部署架构规范.md §3）
export default defineConfig({
    plugins: [
        react(),
        dts({ insertTypesEntry: true, cleanVueFile: false, rollupTypes: false })
    ],
    build: {
        outDir: 'dist',
        emptyOutDir: true,
        sourcemap: false,
        lib: {
            // 两个 entry 对应 exports 两张脸: '.'(index.js/index.cjs) 与 './pages'(pages.js,
            // lead 005 §8.2/§8.6 #3)。只声明 index 时产物里没有 pages.js, 而 exports 还指着它。
            entry: {
                index: path.resolve(__dirname, 'src/index.tsx'),
                pages: path.resolve(__dirname, 'src/pages.ts'),
            },
            name: 'ZCtcFrontendComponent',
            formats: ['es', 'cjs']
        },
        rollupOptions: {
            external: [
                'react', 'react-dom', 'react-dom/client', 'react/jsx-runtime',
                'react-router-dom', 'antd', '@ant-design/icons', 'axios',
                '@yuku123/z-frontend-common',
            ],
            output: {
                globals: {
                    react: 'React', 'react-dom': 'ReactDOM', antd: 'antd',
                    '@yuku123/z-frontend-common': 'ZFrontendCommon',
                }
            }
        }
    },
    resolve: {
        alias: { '@': path.resolve(__dirname, 'src') }
    }
});