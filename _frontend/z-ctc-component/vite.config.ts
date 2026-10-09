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
            entry: path.resolve(__dirname, 'src/index.tsx'),
            name: 'ZCtcFrontendComponent',
            fileName: (format) => `index.${format === 'es' ? 'js' : 'cjs'}`,
            formats: ['es', 'cjs']
        },
        rollupOptions: {
            external: ['react', 'react-dom', 'antd'],
            output: { globals: { react: 'React', 'react-dom': 'ReactDOM', antd: 'antd' } }
        }
    },
    resolve: {
        alias: { '@': path.resolve(__dirname, 'src') }
    }
});