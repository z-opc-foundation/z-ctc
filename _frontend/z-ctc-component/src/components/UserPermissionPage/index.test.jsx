// Run with the existing Vitest CLI: --globals --environment jsdom.
import React, {act} from 'react'
import {createRoot} from 'react-dom/client'
import UserPermissionPage from './index'

const api = vi.hoisted(() => ({
    listAccounts: vi.fn(), listUserRoles: vi.fn(), listUserPermissions: vi.fn(), listRoleResources: vi.fn(),
}))
vi.mock('../_api/userPermission', () => ({userPermissionApi: api}))
vi.mock('@ant-design/icons', () => ({ReloadOutlined: () => null}))
vi.mock('antd', () => {
    const Box = ({children}) => <div>{children}</div>
    const List = ({dataSource, renderItem}) => <div>{dataSource.map(row => <div key={row.id}>{renderItem(row)}</div>)}</div>
    List.Item = ({children, actions}) => <article>{children}{actions}</article>
    List.Item.Meta = ({title}) => <div>{title}</div>
    return {
        List, Space: Box, Tag: Box, Typography: {Text: Box},
        Button: ({children, onClick, disabled}) => <button onClick={onClick} disabled={disabled}>{children}</button>,
        Card: ({children, title, loading}) => <section data-title={title} aria-busy={!!loading}>{children}</section>,
        Empty: ({description}) => <div>{description}</div>, Input: {Search: () => null},
        Select: ({value, options, onChange, allowClear}) => <select aria-label="user" value={value ?? ''} onChange={e => onChange(e.target.value || undefined)}>
            {allowClear && <option value="">clear</option>}
            {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>,
        Tree: ({treeData}) => <pre>{JSON.stringify(treeData)}</pre>,
    }
})
vi.mock('@yuku123/z-frontend-common/ui', () => ({
    TableToolbar: ({left, right}) => <div>{left}{right}</div>,
    ListTable: ({rows, cardProps}) => <section data-title="permissions" aria-busy={cardProps.loading}>{JSON.stringify(rows)}</section>,
}))

function deferred() {
    let resolve, reject
    const promise = new Promise((yes, no) => { resolve = yes; reject = no })
    return {promise, resolve, reject}
}
const roles = user => [{id: 'shared', roleName: `${user}-shared`}, {id: `${user}-other`, roleName: `${user}-other`}]
const resources = name => [{id: name, resourceName: name, resourceCode: name, resourceType: 'API'}]
let root, container
const mount = () => act(async () => root.render(<UserPermissionPage />))
const chooseUser = value => act(async () => {
    const select = container.querySelector('select')
    select.value = value
    select.dispatchEvent(new Event('change', {bubbles: true}))
})
const chooseRole = name => act(async () => [...container.querySelectorAll('article')].find(a => a.textContent.includes(name)).querySelector('button').click())
const resourceCard = () => container.querySelector('section[data-title^="角色资源"]')
const roleCard = () => container.querySelector('section[data-title^="用户角色"]')
const permissionsCard = () => container.querySelector('section[data-title="permissions"]')
const refresh = () => act(async () => [...container.querySelectorAll('button')].find(b => b.textContent === '刷新用户').click())

beforeEach(() => {
    globalThis.IS_REACT_ACT_ENVIRONMENT = true
    vi.resetAllMocks()
    api.listAccounts.mockResolvedValue([{id: 'A'}, {id: 'B'}])
    api.listUserRoles.mockImplementation(async user => roles(user))
    api.listUserPermissions.mockImplementation(async user => [{code: `${user}-permission`}])
    api.listRoleResources.mockImplementation(async role => resources(`${role}-resource`))
    container = document.createElement('div')
    document.body.append(container)
    root = createRoot(container)
})
afterEach(async () => {
    await act(async () => root.unmount())
    container.remove()
})

it('clears displayed role/resources on user switch and refetches even a shared role ID', async () => {
    await mount()
    await chooseRole('A-shared')
    expect(container.textContent).toContain('shared-resource')
    const pendingRoles = deferred()
    api.listUserRoles.mockReturnValueOnce(pendingRoles.promise)
    await chooseUser('B')
    expect(container.querySelector('pre')).toBeNull()
    expect(container.querySelectorAll('article')).toHaveLength(0)
    expect(container.textContent).not.toContain('A-permission')
    expect(resourceCard().getAttribute('aria-busy')).toBe('false')
    await act(async () => pendingRoles.resolve(roles('B')))
    await chooseRole('B-shared')
    expect(api.listRoleResources.mock.calls).toEqual([['shared'], ['shared']])
    expect(container.textContent).toContain('B-permission')
})

it.each(['resolve', 'reject'])('ignores old user roles/permissions that later %s, including their loading updates', async outcome => {
    const oldRoles = deferred(), oldPermissions = deferred(), currentRoles = deferred(), currentPermissions = deferred()
    api.listUserRoles.mockReturnValueOnce(oldRoles.promise).mockReturnValueOnce(currentRoles.promise)
    api.listUserPermissions.mockReturnValueOnce(oldPermissions.promise).mockReturnValueOnce(currentPermissions.promise)
    await mount()
    await chooseUser('B')
    await act(async () => {
        oldRoles[outcome](outcome === 'resolve' ? roles('A') : new Error('old roles failure'))
        oldPermissions[outcome](outcome === 'resolve' ? [{code: 'A-permission'}] : new Error('old permission failure'))
    })
    expect(container.textContent).not.toContain('A-shared')
    expect(container.textContent).not.toContain('A-permission')
    expect(roleCard().getAttribute('aria-busy')).toBe('true')
    expect(permissionsCard().getAttribute('aria-busy')).toBe('true')
    await act(async () => {
        currentRoles.resolve(roles('B'))
        currentPermissions.resolve([{code: 'B-permission'}])
    })
    expect(container.textContent).toContain('B-shared')
    expect(container.textContent).toContain('B-permission')
})

it.each(['resolve', 'reject'])('ignores an older role-resource request that later %ss', async outcome => {
    const old = deferred(), current = deferred()
    api.listRoleResources.mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise)
    await mount()
    await chooseRole('A-shared')
    await chooseRole('A-other')
    await act(async () => current.resolve(resources('current-resource')))
    await act(async () => old[outcome](outcome === 'resolve' ? resources('old-resource') : new Error('old failure')))
    expect(container.textContent).toContain('current-resource')
    expect(container.textContent).not.toContain('old-resource')
})

it('clears the previous tree immediately when selecting another role', async () => {
    await mount()
    await chooseRole('A-shared')
    const pending = deferred()
    api.listRoleResources.mockReturnValueOnce(pending.promise)
    await chooseRole('A-other')
    expect(container.textContent).not.toContain('shared-resource')
    expect(resourceCard().getAttribute('aria-busy')).toBe('true')
    await act(async () => pending.resolve(resources('other-resource')))
    expect(container.textContent).toContain('other-resource')
})

it('ties pending shared-role resources and loading to the user as well as the role', async () => {
    const old = deferred(), current = deferred()
    api.listRoleResources.mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise)
    await mount()
    await chooseRole('A-shared')
    await chooseUser('B')
    await chooseRole('B-shared')
    await act(async () => old.resolve(resources('A-resource')))
    expect(container.textContent).not.toContain('A-resource')
    expect(resourceCard().getAttribute('aria-busy')).toBe('true')
    await act(async () => current.resolve(resources('B-resource')))
    expect(container.textContent).toContain('B-resource')
})

it('clearing the user invalidates pending resources and resets every loading state', async () => {
    const pending = deferred()
    api.listRoleResources.mockReturnValueOnce(pending.promise)
    await mount()
    await chooseRole('A-shared')
    await chooseUser('')
    await act(async () => pending.resolve(resources('late-resource')))
    expect(container.querySelectorAll('article')).toHaveLength(0)
    expect(container.querySelector('pre')).toBeNull()
    expect(container.textContent).not.toContain('A-permission')
    expect(container.querySelector('[aria-busy="true"]')).toBeNull()
    expect(api.listRoleResources).toHaveBeenCalledTimes(1)
})

it('clearing the user invalidates pending roles and permissions', async () => {
    const pendingRoles = deferred(), pendingPermissions = deferred()
    api.listUserRoles.mockReturnValueOnce(pendingRoles.promise)
    api.listUserPermissions.mockReturnValueOnce(pendingPermissions.promise)
    await mount()
    await chooseUser('')
    await act(async () => {
        pendingRoles.resolve(roles('A'))
        pendingPermissions.resolve([{code: 'A-permission'}])
    })
    expect(container.querySelectorAll('article')).toHaveLength(0)
    expect(permissionsCard().textContent).toBe('[]')
    expect(container.querySelector('[aria-busy="true"]')).toBeNull()
})

it.each(['B', ''])('late account refresh cannot override a newer user selection or clear (%s)', async selection => {
    await mount()
    await chooseUser('')
    const pending = deferred()
    api.listAccounts.mockReturnValueOnce(pending.promise)
    await refresh()
    await chooseUser('B')
    if (!selection) await chooseUser('')
    await act(async () => pending.resolve([{id: 'A'}, {id: 'B'}]))
    expect(container.querySelector('select').value).toBe(selection)
    expect(container.textContent).not.toContain('A-permission')
})
