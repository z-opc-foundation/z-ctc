import {Button, Space} from 'antd'
import {ReloadOutlined} from '@ant-design/icons'

/**
 * 表格上方工具栏 — 统一搜索 + 过滤 + 操作按钮布局
 *
 * Props:
 *   - left:    ReactNode   左侧过滤区 (Select, Radio, Checkbox 等)
 *   - right:   ReactNode   右侧操作区 (按钮组)
 *   - extra:   ReactNode   最右侧插入
 *
 * 用法:
 * <TableToolbar
 *   left={
 *     <>
 *       <SearchInput placeholder="搜索..."/>
 *       <Select placeholder="状态"/>
 *     </>
 *   }
 *   right={
 *     <>
 *       <Button icon={<ReloadOutlined/>}>刷新</Button>
 *       <Button type="primary" icon={<PlusOutlined/>}>新建</Button>
 *     </>
 *   }
 * />
 */
export default function TableToolbar({left, right, extra}) {
    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 12,
                marginBottom: 12,
                flexWrap: 'wrap',
            }}
        >
            <Space size={8} wrap>{left}</Space>
            <Space size={8} wrap>
                {right}
                {extra}
            </Space>
        </div>
    )
}

/**
 * 紧凑版刷新按钮 — 表格内常用
 */
export function RefreshButton({onClick, loading, title = '刷新'}) {
    return (
        <Button icon={<ReloadOutlined/>} onClick={onClick} loading={loading} title={title}>
            {title}
        </Button>
    )
}