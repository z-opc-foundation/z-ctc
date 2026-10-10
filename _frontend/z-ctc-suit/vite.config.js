import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

// z-ctc 应用层：SPA，输出 dist/ 给 admin pom 通过 frontend-maven-plugin 注入。
// base 必须匹配 z-ctc-admin 的 context-path（dev profile 用 /ctc/，生产可改成根 /）。
// 当前 application-dev.yml = /ctc，application.yml = /，两者不一致：
//   - dev：base: '/ctc/'
//   - 生产：base: '/'（参考 application.yml 注释）
// 这里默认 dev，**生产部署**（生产 application.yml context-path /）需要改成 '/'
// 或通过环境变量动态注入（Vite 5 不支持，需 Vite 6 + mode 参数）。
// 当前与 z-schedule-admin 一致做法：dev profile = /ctc/，production = /
export default defineConfig({
    base: '/ctc/',  // ← 关键：dev profile 必须用 /ctc/，否则 /assets/* 会 404
    resolve: {
    dedupe: ['react', 'react-dom', 'react-router-dom', 'antd', '@ant-design/icons', 'axios'],
        alias: { '@': path.resolve(__dirname, 'src'), ...(process.env.LOCAL_SIBLINGS === '1' ? { '@yuku123/z-ctc-component': '../z-ctc-component/src' } : {}) }
    },
    plugins: [react()],
    server: {
        port: 5174,
        host: '0.0.0.0',
        proxy: {
            '/api': {
                target: 'http://localhost:8888',
                changeOrigin: true
            }
        }
    },
    build: {
        outDir: 'dist',
        assetsDir: 'assets',
        emptyOutDir: true,
        sourcemap: false
    }
});