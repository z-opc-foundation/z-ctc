import {useEffect, useRef, useState} from 'react'
import {Input} from 'antd'
import {SearchOutlined} from '@ant-design/icons'
import {radius} from './tokens'

/**
 * Debounced SearchInput - 修复 z-opc 后台"搜索必须点按钮"的交互不合理.
 *
 * 原则 (来自 ui-ux-pro-max P8 / frontend-ui-engineering Forms):
 *   - 输入即时反馈, 不要让用户多一次点击
 *   - debounce 300ms 防止每个键击都发请求
 *   - 受控 value + onChange, 避免组件内部状态混乱
 *   - Enter 立即触发 (不等待 debounce)
 *   - Escape 清空
 *
 * @param {Object} props
 * @param {string} [props.value] - 受控值
 * @param {Function} props.onChange - 输入变化回调 (debounced)
 * @param {Function} [props.onSearch] - 显式搜索回调 (Enter / 点按钮)
 * @param {string} [props.placeholder]
 * @param {number} [props.debounceMs] - 防抖延迟 (默认 300)
 * @param {number} [props.width] - 宽度 (默认 280)
 */
export default function SearchInput({
                                        value: controlledValue,
                                        onChange,
                                        onSearch,
                                        placeholder = '搜索...',
                                        debounceMs = 300,
                                        width = 280,
                                        allowClear = true,
                                        // antd v6: enterButton 已被废弃,SearchInput 已通过 onKeyDown 自行处理 Enter 键,
                                        // 所以从 rest 里过滤掉,避免透传到原生 <input> 报 React 警告。
                                        enterButton: _ignored,
                                        ...rest
                                    }) {
    const [inner, setInner] = useState(controlledValue || '')
    const timerRef = useRef(null)
    const lastEmittedRef = useRef(controlledValue || '')

    // 受控/非受控兼容
    useEffect(() => {
        if (controlledValue !== undefined && controlledValue !== inner) {
            setInner(controlledValue || '')
            lastEmittedRef.current = controlledValue || ''
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [controlledValue])

    useEffect(() => {
        return () => {
            if (timerRef.current) clearTimeout(timerRef.current)
        }
    }, [])

    const emit = (v) => {
        lastEmittedRef.current = v
        if (onChange) onChange(v)
    }

    const handleChange = (e) => {
        const v = e.target.value
        setInner(v)
        if (timerRef.current) clearTimeout(timerRef.current)
        // 清空时立即触发 (用户期望立即看到结果)
        if (v === '') {
            emit('')
            return
        }
        timerRef.current = setTimeout(() => emit(v), debounceMs)
    }

    const handleSearch = (v) => {
        if (timerRef.current) clearTimeout(timerRef.current)
        emit(v)
        if (onSearch) onSearch(v)
    }

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            e.preventDefault()
            handleSearch(inner)
        } else if (e.key === 'Escape') {
            setInner('')
            handleSearch('')
        }
    }

    const handleClear = () => {
        setInner('')
        handleSearch('')
    }

    return (
        <Input
            {...rest}
            value={inner}
            onChange={handleChange}
            onPressEnter={handleSearch}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            allowClear={allowClear ? {clearIcon: <span onClick={handleClear}>×</span>} : false}
            prefix={<SearchOutlined style={{color: '#9ca3af'}}/>}
            style={{
                width,
                borderRadius: radius.md,
                ...(rest.style || {}),
            }}
        />
    )
}
