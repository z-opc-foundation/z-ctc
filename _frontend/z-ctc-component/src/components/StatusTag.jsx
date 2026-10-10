/**
 * 状态标签组件
 */
import {Tag} from 'antd'

const statusMap = {
    1: {color: 'green', text: '正常'},
    0: {color: 'red', text: '禁用'},
    pending: {color: 'orange', text: '待处理'},
    approved: {color: 'blue', text: '已通过'},
    rejected: {color: 'red', text: '已拒绝'},
}

export default function StatusTag({status, map}) {
    const config = map?.[status] || statusMap[status] || {color: 'default', text: status}
    return <Tag color={config.color}>{config.text}</Tag>
}
