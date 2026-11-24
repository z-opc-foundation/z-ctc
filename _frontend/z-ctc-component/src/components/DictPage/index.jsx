import {useEffect, useState} from 'react'
import {Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Tag} from 'antd'
import {ArrowLeftOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {ListTable} from '@yuku123/z-frontend-common/ui'
import {dictApi} from '../_api/dict'

/**
 * 灵活字典管理页（承接自 z-meta 字典能力）。
 *
 * 后端:
 *   GET  /api/ctc/ac/dict/categories/list          可见分类列表
 *   POST /api/ctc/ac/dict/categories               新建分类
 *   PUT  /api/ctc/ac/dict/categories/{id}          改分类
 *   DEL  /api/ctc/ac/dict/categories/{id}          删分类
 *   GET  /api/ctc/ac/dict/entries/list?category=   列生效条目
 *   POST /api/ctc/ac/dict/entries                  新建条目
 *   PUT  /api/ctc/ac/dict/entries/{id}             改条目
 *   DEL  /api/ctc/ac/dict/entries/{id}             删条目
 */
export default function DictPage() {
    const [categories, setCategories] = useState([])
    const [categoriesLoading, setCategoriesLoading] = useState(false)
    const [selectedCategory, setSelectedCategory] = useState(null)
    const [entries, setEntries] = useState([])
    const [entriesLoading, setEntriesLoading] = useState(false)

    const [categoryFormOpen, setCategoryFormOpen] = useState(false)
    const [editingCategory, setEditingCategory] = useState(null)
    const [categoryForm] = Form.useForm()

    const [entryFormOpen, setEntryFormOpen] = useState(false)
    const [editingEntry, setEditingEntry] = useState(null)
    const [entryForm] = Form.useForm()

    const loadCategories = async () => {
        setCategoriesLoading(true)
        try {
            const res = await dictApi.listCategories()
            setCategories(Array.isArray(res) ? res : [])
        } catch (e) {
            message.error('加载字典分类失败：' + (e?.message || '未知错误'))
            setCategories([])
        } finally {
            setCategoriesLoading(false)
        }
    }

    useEffect(() => {
        loadCategories()
    }, [])

    const loadEntries = async (catCode) => {
        if (!catCode) {
            setEntries([])
            return
        }
        setEntriesLoading(true)
        try {
            const res = await dictApi.listEntries(catCode)
            setEntries(Array.isArray(res) ? res : [])
        } catch (e) {
            message.error('加载字典条目失败：' + (e?.message || '未知错误'))
            setEntries([])
        } finally {
            setEntriesLoading(false)
        }
    }

    useEffect(() => {
        if (selectedCategory?.catCode) {
            loadEntries(selectedCategory.catCode)
        } else {
            setEntries([])
        }
    }, [selectedCategory?.catCode])

    // === 字典分类 CRUD ===
    const openCreateCategory = () => {
        setEditingCategory(null)
        categoryForm.resetFields()
        setCategoryFormOpen(true)
    }
    const openEditCategory = (c) => {
        setEditingCategory(c)
        categoryForm.setFieldsValue({
            catCode: c.catCode,
            catName: c.catName,
            scope: c.scope || 'TENANT',
            valueType: c.valueType || 'string',
            entryShape: c.entryShape || 'flat',
            status: c.status ?? 1,
            sortOrder: c.sortOrder ?? 0,
            description: c.description,
        })
        setCategoryFormOpen(true)
    }
    const handleCategoryOk = async () => {
        try {
            const v = await categoryForm.validateFields()
            if (editingCategory) {
                await dictApi.updateCategory(editingCategory.id, v)
                message.success('更新成功')
            } else {
                await dictApi.createCategory(v)
                message.success('创建成功')
            }
            setCategoryFormOpen(false)
            setEditingCategory(null)
            categoryForm.resetFields()
            loadCategories()
        } catch (e) {
            if (e?.errorFields) return
            message.error((editingCategory ? '更新失败：' : '创建失败：') + (e?.message || '未知错误'))
        }
    }
    const handleDeleteCategory = async (id) => {
        try {
            await dictApi.deleteCategory(id)
            message.success('已删除')
            if (selectedCategory?.id === id) setSelectedCategory(null)
            loadCategories()
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    // === 字典条目 CRUD ===
    const openCreateEntry = () => {
        if (!selectedCategory) {
            message.warning('请先选择左侧的字典分类')
            return
        }
        setEditingEntry(null)
        entryForm.resetFields()
        setEntryFormOpen(true)
    }
    const openEditEntry = (entry) => {
        setEditingEntry(entry)
        entryForm.setFieldsValue({
            dictKey: entry.dictKey,
            dictValue: entry.dictValue,
            dataValue: entry.dataValue,
            parentKey: entry.parentKey,
            sortOrder: entry.sortOrder ?? 0,
            status: entry.status ?? 1,
        })
        setEntryFormOpen(true)
    }
    const handleEntryOk = async () => {
        try {
            const v = await entryForm.validateFields()
            const payload = {...v, category: selectedCategory.catCode}
            if (editingEntry) {
                await dictApi.updateEntry(editingEntry.id, payload)
                message.success('更新成功')
            } else {
                await dictApi.createEntry(payload)
                message.success('创建成功')
            }
            setEntryFormOpen(false)
            setEditingEntry(null)
            entryForm.resetFields()
            loadEntries(selectedCategory.catCode)
        } catch (e) {
            if (e?.errorFields) return
            message.error((editingEntry ? '更新失败：' : '创建失败：') + (e?.message || '未知错误'))
        }
    }
    const handleDeleteEntry = async (id) => {
        try {
            await dictApi.deleteEntry(id)
            message.success('已删除')
            loadEntries(selectedCategory.catCode)
        } catch (e) {
            message.error('删除失败：' + (e?.message || '未知错误'))
        }
    }

    const scopeTag = (scope) => {
        const colorMap = {GLOBAL: 'purple', TENANT: 'blue', DOMAIN: 'cyan'}
        return <Tag color={colorMap[scope] || 'default'}>{scope || 'TENANT'}</Tag>
    }

    const entryColumns = [
        {
            title: '条目键',
            dataIndex: 'dictKey',
            width: 160,
            render: v => <span style={{fontFamily: 'monospace'}}>{v}</span>
        },
        {title: '显示值', dataIndex: 'dictValue', width: 180},
        {title: '标签', dataIndex: 'tags', width: 120, render: v => v || '-'},
        {title: '排序', dataIndex: 'sortOrder', width: 70},
        {
            title: '状态', dataIndex: 'status', width: 80,
            render: v => <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>,
        },
        {
            title: '操作', width: 120,
            render: (_, r) => (
                <Space>
                    <Button type="link" size="small"
                            onClick={() => openEditEntry(r)}>编辑</Button>
                    <Popconfirm title="删除该字典条目?" okText="删除" cancelText="取消" okButtonProps={{danger: true}}
                                onConfirm={() => handleDeleteEntry(r.id)}>
                        <Button type="link" size="small" danger>删除</Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ]

    return (
        // flex:1 + minWidth:0 —— 宿主的内容区是 flex 容器，页面根节点不撑满就会被
        // 挤成内容宽（曾把右侧「字典条目」压成 120px，列表列也被裁掉）。
        <div style={{display: 'flex', flexDirection: 'column', gap: 12, flex: 1, minWidth: 0, width: '100%'}}>
            {/* 两步走（CEO 2026-11-11）：第一步是字典分类列表，点进去才是这个字典的条目。
                原先是左右并列，右侧长期挂一个「请在左侧选择…」的空面板，且两栏被挤。 */}
            {selectedCategory ? (
                <>
                    <div style={{display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap'}}>
                        <Button size="small" icon={<ArrowLeftOutlined/>} onClick={() => setSelectedCategory(null)}>
                            返回字典分类
                        </Button>
                        <span style={{fontSize: 13, color: 'var(--text-secondary, #595959)'}}>
                            字典条目 — {selectedCategory.catName || selectedCategory.catCode}
                        </span>
                        {scopeTag(selectedCategory.scope)}
                    </div>
                    <ListTable
                        cardTitle={`字典条目 (${entries.length})`}
                        cardExtra={
                            <Space>
                                <Button size="small" icon={<PlusOutlined/>} onClick={openCreateEntry}
                                        disabled={!selectedCategory}>新增条目</Button>
                                <Button size="small" icon={<ReloadOutlined/>}
                                        disabled={!selectedCategory}
                                        onClick={() => selectedCategory && loadEntries(selectedCategory.catCode)}/>
                            </Space>
                        }
                        rows={entries}
                        rowKey="id"
                        loading={entriesLoading}
                        searchKeys={['dictKey', 'dictValue']}
                        searchPlaceholder="按条目键 / 显示值过滤"
                        cardProps={{size: 'small'}}
                        columns={entryColumns}
                        emptyTitle="这个字典还没有条目"
                        emptyDescription="条目是字典的取值明细（例如 启用 / 停用）。"
                        emptyActionText="新建条目"
                        emptyOnAction={openCreateEntry}
                    />
                </>
            ) : (
                <ListTable
                    cardTitle={`字典分类 (${categories.length})`}
                    cardExtra={
                        <Space>
                            <Button size="small" icon={<PlusOutlined/>} onClick={openCreateCategory}>新增</Button>
                            <Button size="small" icon={<ReloadOutlined/>} onClick={loadCategories}/>
                        </Space>
                    }
                    rows={categories}
                    rowKey="id"
                    searchKeys={['catCode', 'catName']}
                    searchPlaceholder="搜索编码 / 名称"
                    cardProps={{size: 'small', loading: categoriesLoading}}
                    emptyTitle="还没有字典分类"
                    emptyDescription="字典分类是条目的容器：先建一个分类，再往里录条目。"
                    emptyActionText="新建字典分类"
                    emptyOnAction={openCreateCategory}
                    tableProps={{
                        onRow: (r) => ({
                            onClick: () => setSelectedCategory(r),
                            style: {cursor: 'pointer'},
                        }),
                    }}
                    columns={[
                        {
                            title: '编码', dataIndex: 'catCode', width: 110, ellipsis: true,
                            render: v => <span style={{fontFamily: 'monospace'}}>{v}</span>
                        },
                        {title: '名称', dataIndex: 'catName', width: 110, ellipsis: true},
                        {
                            title: '作用域', dataIndex: 'scope', width: 80,
                            render: v => scopeTag(v),
                        },
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
                                    <Button type="link" size="small"
                                            onClick={() => openEditCategory(r)}>编辑</Button>
                                    <Popconfirm title="删除该分类?" okText="删除" cancelText="取消"
                                                okButtonProps={{danger: true}}
                                                onConfirm={() => handleDeleteCategory(r.id)}>
                                        <Button type="link" size="small" danger>删除</Button>
                                    </Popconfirm>
                                </Space>
                            ),
                        },
                    ]}
                />

            )}

            <Modal
                title={editingCategory ? '编辑字典分类' : '新建字典分类'}
                open={categoryFormOpen}
                onOk={handleCategoryOk}
                onCancel={() => {
                    setCategoryFormOpen(false);
                    setEditingCategory(null);
                    categoryForm.resetFields()
                }}
                destroyOnHidden
                size="large"
            >
                <Form form={categoryForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="catCode" label="分类编码" rules={[{required: true, max: 64}]}
                               extra="业务引用键，全局唯一，如 user_status">
                        <Input disabled={!!editingCategory} placeholder="如 user_status"/>
                    </Form.Item>
                    <Form.Item name="catName" label="分类名称" rules={[{required: true, max: 128}]}>
                        <Input placeholder="如 用户状态"/>
                    </Form.Item>
                    <Form.Item name="scope" label="作用域" initialValue="TENANT"
                               extra="GLOBAL 全平台共享；TENANT 租户内；DOMAIN 租户 + 域内">
                        <Select options={[
                            {value: 'GLOBAL', label: 'GLOBAL（全平台）'},
                            {value: 'TENANT', label: 'TENANT（租户）'},
                            {value: 'DOMAIN', label: 'DOMAIN（租户+域）'},
                        ]}/>
                    </Form.Item>
                    <Form.Item name="valueType" label="条目值类型" initialValue="string">
                        <Select options={[
                            {value: 'string', label: 'string'},
                            {value: 'number', label: 'number'},
                            {value: 'boolean', label: 'boolean'},
                            {value: 'json', label: 'json'},
                        ]}/>
                    </Form.Item>
                    <Form.Item name="entryShape" label="条目形状" initialValue="flat"
                               extra="tree 形状的分类下条目可用父键成树">
                        <Select options={[
                            {value: 'flat', label: 'flat（平铺）'},
                            {value: 'tree', label: 'tree（树形）'},
                        ]}/>
                    </Form.Item>
                    <Form.Item name="sortOrder" label="排序" initialValue={0}>
                        <InputNumber min={0} max={9999} style={{width: '100%'}}/>
                    </Form.Item>
                    <Form.Item name="status" label="状态" initialValue={1}>
                        <Select options={[{value: 1, label: '启用'}, {value: 0, label: '禁用'}]}/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} maxLength={512}/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title={editingEntry ? `编辑字典条目 — ${selectedCategory?.catName || ''}` : `新建字典条目 — ${selectedCategory?.catName || ''}`}
                open={entryFormOpen}
                onOk={handleEntryOk}
                onCancel={() => {
                    setEntryFormOpen(false);
                    setEditingEntry(null);
                    entryForm.resetFields()
                }}
                destroyOnHidden
                size="large"
            >
                <Form form={entryForm} layout="vertical" style={{marginTop: 16}}>
                    <Form.Item name="dictKey" label="条目键" rules={[{required: true, max: 64}]}
                               extra="分类内业务唯一，如 enabled">
                        <Input placeholder="如 enabled"/>
                    </Form.Item>
                    <Form.Item name="dictValue" label="显示值" rules={[{required: true, max: 128}]}>
                        <Input placeholder="如 启用"/>
                    </Form.Item>
                    <Form.Item name="dataValue" label="数据值"
                               extra="值类型为 number/boolean/json 时的载荷（JSON 字符串）">
                        <Input.TextArea rows={2} placeholder='如 {"min": 0, "max": 100}'/>
                    </Form.Item>
                    <Form.Item name="parentKey" label="父条目键"
                               extra="仅 tree 形状的分类生效，填父条目的条目键">
                        <Input placeholder="tree 分类下成树，留空为顶层"/>
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
