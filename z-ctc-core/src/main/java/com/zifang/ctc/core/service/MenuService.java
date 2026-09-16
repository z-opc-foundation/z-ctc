package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.MenuDO;

import java.util.List;
import java.util.Optional;

/**
 * 应用菜单 Service 接口.
 * <p>
 * 主入口: {@link #listAsTree(String)} — 给前端 {@code getDynamicMenu(appCode)} 调用.
 */
public interface MenuService {

    /**
     * 拉取指定 app_code 的所有菜单, 拼成树 (parent_code == '' 或 NULL 的为根).
     * 返回的节点 children 已按 sort_order 排序.
     *
     * @param appCode 应用编码, 必填 (如 'z-opc')
     * @return 树形根节点列表; 若 appCode 无任何菜单则返回空列表 (不是 null)
     */
    List<MenuDO> listAsTree(String appCode);

    /**
     * 扁平列表 — 给管理页面用.
     */
    List<MenuDO> listFlat(String appCode);

    /**
     * 创建菜单.
     */
    Long createMenu(MenuDO menu, String createdBy);

    /**
     * 更新菜单 (非空字段).
     */
    boolean updateMenu(Long id, MenuDO patch, String updatedBy);

    /**
     * 删除菜单 (级联删除其所有后代).
     */
    boolean deleteMenu(Long id);

    /**
     * 按 ID 查询.
     */
    Optional<MenuDO> findById(Long id);
}