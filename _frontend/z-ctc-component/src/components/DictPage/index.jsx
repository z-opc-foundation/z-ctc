import {useEffect, useState} from 'react'
import {Button, Card, Form, Input, InputNumber, message, Modal, Popconfirm, Select, Space, Table, Tag} from 'antd'
import {DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {dictApi} from '../_api/dict'
import {PageHeader, SearchInput} from '@/common/components/ui'

/**
 * 元典管理 — 字典类型 + 字典项 树形管理
 *
 * 后端:
 *   GET  /api/dict/type/list?pageNum=&pageSize=&dictCode=&dictName=&status=
 *   GET  /api/dict/type/get?id=
 *   POST /api/dict/type
 *   PUT  /api/dict/type/{id}
 *   DEL  /api/dict/type/{id}
 *   GET  /api/dict/item/list?dictId=
 *   GET  /api/dict/items/get?dictCode=
 *   POST /api/dict/item
 *   PUT  /api/dict/item/{id}
 *   DEL  /api/dict/item/{id}
 */
// 原签名带一个未使用的 apiBaseURL 形参：noImplicitAny 下被推成必填 props，
// 而 SystemShell 渲染 <DictPage/> 不传它 → TS2741。该形参在函数体内从未被读取，
// 直接删除，不影响任何行为。
export default function DictPage() {
    const [types, setTypes] = useState([])
    const [typesLoading, setTypesLoading] = useState(false)
    const [selectedType, setSelectedType] = useState(null)
    const [items, setItems] = useState([])
    const [itemsLoading, setItemsLoading] = useState(false)

    const [typeFormOpen, setTypeFormOpen] = useState(false)
    const [editingType, setEditingType] = useState(null)
    const [typeForm] = Form.useForm()

    const [itemFormOpen, setItemFormOpen] = useState(false)
    const [editingItem, setEditingItem] = useState(null)
    const [itemForm] = Form.useForm()

    const [searchText, setSearchText] = useState('')

    const loadTypes = async () => {
        setTypesLoading(true)
        try {
            const res = await dictApi.listDictType({pageNum: 1, pageSize: 200})
            const list = res?.records || res?.data || res || []
            setTypes(Array.isArray(list) ? list : [])
        } catch (e) {
            message.error('加载字典类型失败：' + (e?.message || '未知错误'))
            setTypes([])
        } finally {
            setTypesLoading(false)
        }
    }

    useEffect(() => {
        loadTypes()
    }, [])

    const loadItems = async (typeId) => {
        if (!typeId) {
            setItems([])
            return
        }
        setItemsLoading(true)
        try {
            const res = await dictApi.listDictItem(typeId)
            setItems(Array.isArray(res) ? res : [])
        } catch (e) {
            message.error('加载字典项失败：' + (e?.message || '未知错误'))
            setItems([])
        } finally {
            setItemsLoading(false)
        }
    }

    useEffect(() => {
        if (selectedType?.id) {
            loadItems(selectedType.id)
        } else {
            setItems([])
        }
    }, [selectedType?.id])

    // === 字典类型 CRUD ===
    const openCreateType = () => {
        setEditingType(null)
        typeForm.resetFields()
        setTypeFormOpen(true)
    }
    const openEditType = (t) => {
        setEditingType(t)
        typeForm.setFieldsValue({
            dictCode: t.dictCode,
            dictName: t.dictName,
            status: t.status ?? 1,
            remark: t.remark,
        })
        setTypeFormOpen(true)
    }
    const handleTypeOk = async () => {
        try {
            const v = await typeForm.validateFields()
            if (editingType) {
                await dictApi.updateDictType(editingType.id, v)
                message.success('更新成功')
            } else {
                await dictApi.createDictType(v)
                message.success('创建成功')
            }
            setTypeFormOpen(false)
            setEditingType(null)
            typeForm.resetFields()
            loadTypes()
        } catch (e) {
            if (e?.errorFields) return
            message.error((editingType ? '更新失败：' : '创建失败：') + (e?.message || '未知错误'))
        }
    }
    const handleDeleteType = async (id) => {
        try {
            await dictApi.deleteDictType(id)
            message.success('已删除')
            if (selectedType?.id === id) setSelectedType(null)
            loadTypes()
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    // === 字典项 CRUD ===
    const openCreateItem = () => {
        if (!selectedType) {
            message.warning('请先选择左侧的字典类型')
            return
        }
        setEditingItem(null)
        itemForm.resetFields()
        setItemFormOpen(true)
    }
    const openEditItem = (it) => {
        setEditingItem(it)
        itemForm.setFieldsValue({
            itemCode: it.itemCode,
            itemName: it.itemName,
            sortOrder: it.sortOrder ?? 0,
            status: it.status ?? 1,
        })
        setItemFormOpen(true)
    }
    const handleItemOk = async () => {
        try {
            const v = await itemForm.validateFields()
            const payload = {...v, dictId: selectedType.id}
            if (editingItem) {
                await dictApi.updateDictItem(editingItem.id, payload)
                message.success('更新成功')
            } else {
                await dictApi.createDictItem(payload)
                message.success('创建成功')
            }
            setItemFormOpen(false)
            setEditingItem(null)
            itemForm.resetFields()
            loadItems(selectedType.id)
        } catch (e) {
            if (e?.errorFields) return
            message.error((editingItem ? '更新失败：' : '创建失败：') + (e?.message || '未知错误'))
        }
    }
    const handleDeleteItem = async (id) => {
        try {
            await dictApi.deleteDictItem(id)
            message.success('已删除')
            loadItems(selectedType.id)
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    const filteredTypes = types.filter(t => {
        if (!searchText) return true
        const k = searchText.toLowerCase()
        return (t.dictCode?.toLowerCase().includes(k) || t.dictName?.toLowerCase().includes(k))
    })

    const itemColumns = [
        {title: 'ID', dataIndex: 'id', width: 70},
        {
            title: '项编码',
            dataIndex: 'itemCode',
            width: 160,
            render: v => <span style={{fontFamily: 'monospace'}}>{v}</span>
        },
        {title: '项名称', dataIndex: 'itemName', width: 160},
        {title: '排序', dataIndex: 'sortOrder', width: 80},
        {
            title: '状态', dataIndex: 'status', width: 80,
            render: v => <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>,
        },
        {
            title: '操作', width: 160,
            render: (_, r) => (
                <Space>
                    <Button type="text" size="small" icon={<EditOutlined/>}
                            onClick={() => openEditItem(r)}>编辑</Button>
                    <Popconfirm title="删除该字典项?" okText="删除" cancelText="取消" okButtonProps={{danger: true}}
                                onConfirm={() => handleDeleteItem(r.id)}>
                        <Button type="text" size="small" danger icon={<DeleteOutlined/>}>删除</Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ]

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 12}}>
            <PageHeader
                title="字典管理"
                subtitle="左侧为字典类型，右侧为该类型下的字典项"
                breadcrumb={[{label: '4A 中心'}, {label: '字典管理'}]}
            />
            <div style={{display: 'grid', gridTemplateColumns: '380px 1fr', gap: 16}}>
                <Card
                    size="small"
                    title={`字典类型 (${types.length})`}
                    loading={typesLoading}
                    extra={
                        <Space>
                            <Button size="small" icon={<PlusOutlined/>} onClick={openCreateType}>新增</Button>
                            <Button size="small" icon={<ReloadOutlined/>} onClick={loadTypes}/>
                        </Space>
                    }
                >
                    <div style={{marginBottom: 12}}>
                        <SearchInput
                            placeholder="搜索编码 / 名称"
                            value={searchText}
                            onChange={(v) => setSearchText(v)}
                            width="100%"
                        />
                    </div>
                    <Table
                        size="small"
                        rowKey="id"
                        pagination={{pageSize: 10, size: 'small'}}
                        dataSource={filteredTypes}
                        onRow={(r) => ({
                            onClick: () => setSelectedType(r),
                            style: {
                                cursor: 'pointer',
                                background: selectedType?.id === r.id ? '#e6f4ff' : undefined,
                            },
                        })}
                        columns={[
                            {
                                title: '编码', dataIndex: 'dictCode', width: 100, ellipsis: true,
                                render: v => <span style={{fontFamily: 'monospace'}}>{v}</span>
                            },
                            {title: '名称', dataIndex: 'dictName', width: 110, ellipsis: true},
                            {
                                title: '状态', dataIndex: 'status', width: 60,
                                render: v => <Tag color={v === 1 ? 'success' : 'default'}>
                                    {v === 1 ? '启' : '禁'}
                                </Tag>,
                            },
                            {
                                title: '操作', width: 100,
                                render: (_, r) => (
                                    <Space onClick={e => e.stopPropagation()}>
                                        <Button type="text" size="small" icon={<EditOutlined/>}
                                                onClick={() => openEditType(r)}/>
                                        <Popconfirm title="删除该类型?" okText="删除" cancelText="取消"
                                                    okButtonProps={{danger: true}}
                                                    onConfirm={() => handleDeleteType(r.id)}>
                                            <Button type="text" size="small" danger icon={<DeleteOutlined/>}/>
                                        </Popconfirm>
                                    </Space>
                                ),
                            },
                        ]}
                    />
                </Card>

                <Card
                    size="small"
                    title={selectedType ? `字典项 — ${selectedType.dictName} (${items.length})` : '字典项'}
                    extra={
                        <Space>
                            <Button size="small" icon={<PlusOutlined/>} onClick={openCreateItem}
                                    disabled={!selectedType}>新增项</Button>
                            <Button size="small" icon={<ReloadOutlined/>}
                                    disabled={!selectedType}
                                    onClick={() => selectedType && loadItems(selectedType.id)}/>
                        </Space>
                    }
                >
                    {!selectedType ? (
                        <div style={{padding: 40, textAlign: 'center', color: '#999'}}>
                            请在左侧选择一个字典类型以查看其字典项
                        </div>
                    ) : (
                        <Table
                            size="small"
                            rowKey="id"
                            dataSource={items}
                            columns={itemColumns}
                            pagination={{pageSize: 15, size: 'small', showTotal: t => `共 ${t} 条`}}
                            loading={itemsLoading}
                        />
                    )}
                </Card>
            </div>

            <Modal
                title={editingType ? '编辑字典类型' : '新建字典类型'}
                open={typeFormOpen}
                onOk={handleTypeOk}
                onCancel={() => {
                    setTypeFormOpen(false);
                    setEditingType(null);
                    typeForm.resetFields()
                }}
                destroyOnHidden
                size="large"
            >
                <Form form={typeForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="dictCode" label="字典编码" rules={[{required: true, max: 64}]}
                               extra="业务唯一，如 user_status">
                        <Input disabled={!!editingType} placeholder="如 user_status"/>
                    </Form.Item>
                    <Form.Item name="dictName" label="字典名称" rules={[{required: true, max: 128}]}>
                        <Input placeholder="如 用户状态"/>
                    </Form.Item>
                    <Form.Item name="status" label="状态" initialValue={1}>
                        <Select options={[{value: 1, label: '启用'}, {value: 0, label: '禁用'}]}/>
                    </Form.Item>
                    <Form.Item name="remark" label="备注">
                        <Input.TextArea rows={2} maxLength={512}/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title={editingItem ? `编辑字典项 — ${selectedType?.dictName || ''}` : `新建字典项 — ${selectedType?.dictName || ''}`}
                open={itemFormOpen}
                onOk={handleItemOk}
                onCancel={() => {
                    setItemFormOpen(false);
                    setEditingItem(null);
                    itemForm.resetFields()
                }}
                destroyOnHidden
                size="large"
            >
                <Form form={itemForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="itemCode" label="项编码" rules={[{required: true, max: 64}]}
                               extra="业务唯一">
                        <Input placeholder="如 enabled"/>
                    </Form.Item>
                    <Form.Item name="itemName" label="项名称" rules={[{required: true, max: 128}]}>
                        <Input placeholder="如 启用"/>
                    </Form.Item>
                    <Form.Item name="sortOrder" label="排序" initialValue={0}>
                        <InputNumber min={0} max={9999} style={{width: '100%'}}/>
                    </Form.Item>
                    <Form.Item name="status" label="状态" initialValue={1}>
                        <Select options={[{value: 1, label: '启用'}, {value: 0, label: '禁用'}]}/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}