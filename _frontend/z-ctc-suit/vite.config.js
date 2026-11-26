import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import {safeEmptyOutDir} from '../../build-tools/safe-vite-plugin.mjs'

// z-ctc 应用层：SPA，输出 dist/ 给 admin pom 通过 frontend-maven-plugin 注入。
// base 必须匹配 z-ctc-admin 的 context-path，两者不一致就是线上白屏：
//   - vite dev（mode=development）对 application-dev.yml 的 /ctc
//   - vite build（mode=production）对 application.yml 的 /
// 曾因这里写死 '/ctc/' 导致生产构建产物引 /ctc/assets/*，而服务发在 / 下 → 404 白屏。
export default defineConfig(({ mode }) => ({
    base: mode === 'production' ? '/' : '/ctc/',
    define: {
        // 路由前缀必须跟 base 一起分环境：生产 context-path 是 /，路由不能再带 /z-ctc，
        // 否则线上刷新 /home 直接 404（与 base 同一个坑的两面）。
        __CTC_BASE__: JSON.stringify(mode === 'production' ? '' : '/z-ctc'),
    },
    resolve: {
    dedupe: ['react', 'react-dom', 'react-router-dom', 'antd', '@ant-design/icons', 'axios'],
        alias: { '@': path.resolve(__dirname, 'src'), ...(process.env.LOCAL_SIBLINGS === '1' ? { '@yuku123/z-ctc-component': '../z-ctc-component/src' } : {}) }
    },
    plugins: [safeEmptyOutDir(), react()],
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
        emptyOutDir: false,
        sourcemap: false
    }
}));