import React from 'react';
import { createRoot } from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App.jsx';

// z-ctc 应用层入口（与 z-schedule-frontend 同形态）。
// 演示：消费组件层 + 业务 SPA + Antd ConfigProvider
const container = document.getElementById('root');
const root = createRoot(container);

root.render(
    <ConfigProvider locale={zhCN}>
        <App />
    </ConfigProvider>
);