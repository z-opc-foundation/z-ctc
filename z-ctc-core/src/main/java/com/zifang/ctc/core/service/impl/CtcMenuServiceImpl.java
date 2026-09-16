package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.MenuDO;
import com.zifang.ctc.core.domain.service.MenuDbService;
import com.zifang.ctc.core.service.MenuService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 应用菜单 Service 实现类.
 * <p>
 * 树构建算法:
 * <ol>
 *   <li>一次性 selectByAppCode 拿扁平行 (已按 sort_order 排序)</li>
 *   <li>建 parent_code → 子节点列表 索引</li>
 *   <li>parent_code 为 '' / NULL 的视为根, 加入结果集</li>
 *   <li>每个父节点递归挂载 children (子节点也按 sort_order 已排好)</li>
 * </ol>
 * <p>
 * 业务语义（菜单编码唯一性校验 + 级联删除） + 事务,
 * 持久化委托给 {@link MenuDbService}.
 *
 * @author zifang
 * @since 1.0.0
 * @see MenuService
 * @see MenuDbService
 */
@Service
public class CtcMenuServiceImpl implements MenuService {

    private static final Logger log = LogManager.getLogger(CtcMenuServiceImpl.class);

    private final MenuDbService menuDbService;

    public CtcMenuServiceImpl(MenuDbService menuDbService) {
        this.menuDbService = menuDbService;
    }

    /**
     * 树构建核心: 给一个按 sort_order 排序的扁平 list, 返回根节点列表 (children 已挂载).
     *
     * @param flat 扁平的菜单列表（已按 sort_order 排序）
     * @return 根节点列表（children 已递归挂载）
     */
    static List<MenuDO> buildTree(List<MenuDO> flat) {
        // 1. 按 id 索引, 用于回挂 parent
        Map<Long, MenuDO> byId = new LinkedHashMap<>();
        for (MenuDO m : flat) {
            m.setChildren(new ArrayList<>());
            byId.put(m.getId(), m);
        }

        // 2. 按 menu_code 索引, 用于按 parent_code 找父节点 (跨 id 重启场景友好)
        Map<String, MenuDO> byCode = new HashMap<>();
        for (MenuDO m : flat) {
            if (m.getMenuCode() != null) {
                byCode.put(m.getMenuCode(), m);
            }
        }

        // 3. 根节点
        List<MenuDO> roots = new ArrayList<>();
        for (MenuDO m : flat) {
            String parentCode = m.getParentCode();
            boolean isRoot = parentCode == null || parentCode.isEmpty();
            if (isRoot) {
                roots.add(m);
            } else {
                MenuDO parent = byCode.get(parentCode);
                if (parent != null) {
                    parent.getChildren().add(m);
                } else {
                    // 父节点找不到 — 降级为根 (防御)
                    log.warn("MenuDO id={} menuCode={} 的 parent_code={} 找不到, 降级为根",
                            m.getId(), m.getMenuCode(), parentCode);
                    roots.add(m);
                }
            }
        }
        return roots;
    }

    /**
     * 查询应用菜单树（按层级结构返回）.
     *
     * @param appCode 应用编码
     * @return 菜单树根节点列表
     */
    @Override
    public List<MenuDO> listAsTree(String appCode) {
        if (appCode == null || appCode.isEmpty()) {
            return Collections.emptyList();
        }
        List<MenuDO> flat = menuDbService.selectByAppCode(appCode);
        if (flat == null || flat.isEmpty()) {
            return Collections.emptyList();
        }
        return buildTree(flat);
    }

    /**
     * 查询应用菜单列表（扁平结构）.
     *
     * @param appCode 应用编码
     * @return 菜单列表
     */
    @Override
    public List<MenuDO> listFlat(String appCode) {
        if (appCode == null || appCode.isEmpty()) {
            return Collections.emptyList();
        }
        List<MenuDO> flat = menuDbService.selectByAppCode(appCode);
        return flat == null ? Collections.emptyList() : flat;
    }

    /**
     * 创建菜单.
     *
     * @param menu      菜单信息，必须包含 appCode 和 menuCode
     * @param createdBy 创建人
     * @return 菜单ID
     * @throws IllegalArgumentException 当 menu 为空或必填字段缺失时
     * @throws IllegalStateException    当 menu_code 在 app_code 下已存在时
     */
    @Override
    @Transactional
    public Long createMenu(MenuDO menu, String createdBy) {
        if (menu == null || menu.getAppCode() == null || menu.getMenuCode() == null) {
            throw new IllegalArgumentException("appCode / menuCode 必填");
        }
        // 判重
        MenuDO exist = menuDbService.selectByMenuCode(menu.getAppCode(), menu.getMenuCode());
        if (exist != null) {
            throw new IllegalStateException(
                    "menu_code=" + menu.getMenuCode() + " 在 app_code=" + menu.getAppCode() + " 下已存在");
        }
        if (menu.getStatus() == null) menu.setStatus(1);

        if (menu.getSortOrder() == null) menu.setSortOrder(0);

        if (menu.getMenuType() == null) menu.setMenuType("MENU");

        if (menu.getParentCode() == null) menu.setParentCode("");

        menu.setCreatedAt(LocalDateTime.now());
        menu.setUpdatedAt(LocalDateTime.now());
        menuDbService.insert(menu);
        log.info("CtcMenuServiceImpl.createMenu: appCode={} menuCode={} id={}",
                menu.getAppCode(), menu.getMenuCode(), menu.getId());
        return menu.getId();
    }

    /**
     * 更新菜单信息.
     *
     * @param id        菜单ID
     * @param patch     更新内容
     * @param updatedBy 更新人
     * @return true 更新成功，false 菜单不存在或更新失败
     */
    @Override
    @Transactional
    public boolean updateMenu(Long id, MenuDO patch, String updatedBy) {
        if (id == null || patch == null) { return false; }

        MenuDO exist = menuDbService.selectById(id);
        if (exist == null) { return false; }

        if (patch.getMenuName() != null) exist.setMenuName(patch.getMenuName());

        if (patch.getParentCode() != null) exist.setParentCode(patch.getParentCode());

        if (patch.getMenuType() != null) exist.setMenuType(patch.getMenuType());

        if (patch.getIcon() != null) exist.setIcon(patch.getIcon());

        if (patch.getPath() != null) exist.setPath(patch.getPath());

        if (patch.getSortOrder() != null) exist.setSortOrder(patch.getSortOrder());

        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());

        if (patch.getSourceType() != null) exist.setSourceType(patch.getSourceType());

        if (patch.getPageId() != null) exist.setPageId(patch.getPageId());

        if (patch.getComponent() != null) exist.setComponent(patch.getComponent());

        if (patch.getDescription() != null) exist.setDescription(patch.getDescription());

        exist.setUpdatedAt(LocalDateTime.now());
        return menuDbService.updateById(exist) > 0;
    }

    /**
     * 删除菜单（级联删除所有子孙菜单）.
     *
     * @param id 菜单ID
     * @return true 删除成功，false 菜单不存在或删除失败
     */
    @Override
    @Transactional
    public boolean deleteMenu(Long id) {
        if (id == null) { return false; }

        MenuDO exist = menuDbService.selectById(id);
        if (exist == null) { return false; }

        // 级联删除: 找所有 menu_code 以 id.menuCode 为前缀的? 这里用精确 parent_code 匹配更稳.
        // 拉一次所有同 app 的扁平, 递归收集所有后代 id
        List<MenuDO> flat = menuDbService.selectByAppCode(exist.getAppCode());
        Set<Long> toDelete = new HashSet<>();
        collectDescendants(flat, exist.getMenuCode(), toDelete);
        toDelete.add(exist.getId());
        int deleted = menuDbService.deleteBatchIds(toDelete);
        log.info("CtcMenuServiceImpl.deleteMenu: id={} menuCode={} 共删除 {} 条",
                id, exist.getMenuCode(), deleted);
        return deleted > 0;
    }

    /**
     * 递归收集 menuCode 节点的所有后代 id (含子孙).
     *
     * @param flat     菜单列表（扁平）
     * @param parentCode 父菜单编码
     * @param acc      累积器，收集所有后代 id
     */
    private void collectDescendants(List<MenuDO> flat, String parentCode, Set<Long> acc) {
        for (MenuDO m : flat) {
            if (parentCode.equals(m.getParentCode())) {
                acc.add(m.getId());
                collectDescendants(flat, m.getMenuCode(), acc);
            }
        }
    }

    /**
     * 根据ID查询菜单.
     *
     * @param id 菜单ID
     * @return 菜单信息，不存在则返回空
     */
    @Override
    public Optional<MenuDO> findById(Long id) {
        return Optional.ofNullable(menuDbService.selectById(id));
    }
}
