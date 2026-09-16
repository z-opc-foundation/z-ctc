package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.MenuDO;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单 VO.
 */
public class MenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String appCode;
    private String menuCode;
    private String menuName;
    private String parentCode;
    private String menuType;
    private String icon;
    private String path;
    private Integer sortOrder;
    private Integer status;
    private String sourceType;
    private Long pageId;
    private String component;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MenuVO> children = new ArrayList<>();

    public MenuVO() {
    }

    public static MenuVO from(MenuDO menuDO) {
        if (menuDO == null) {
            return null;
        }
        MenuVO vo = new MenuVO();
        vo.setId(menuDO.getId());
        vo.setAppCode(menuDO.getAppCode());
        vo.setMenuCode(menuDO.getMenuCode());
        vo.setMenuName(menuDO.getMenuName());
        vo.setParentCode(menuDO.getParentCode());
        vo.setMenuType(menuDO.getMenuType());
        vo.setIcon(menuDO.getIcon());
        vo.setPath(menuDO.getPath());
        vo.setSortOrder(menuDO.getSortOrder());
        vo.setStatus(menuDO.getStatus());
        vo.setSourceType(menuDO.getSourceType());
        vo.setPageId(menuDO.getPageId());
        vo.setComponent(menuDO.getComponent());
        vo.setDescription(menuDO.getDescription());
        vo.setCreatedAt(menuDO.getCreatedAt());
        vo.setUpdatedAt(menuDO.getUpdatedAt());
        // 递归转换子节点, 让 tree API 真正返回嵌套 children 而非空数组
        if (menuDO.getChildren() != null && !menuDO.getChildren().isEmpty()) {
            List<MenuVO> childVOs = new ArrayList<>(menuDO.getChildren().size());
            for (MenuDO child : menuDO.getChildren()) {
                childVOs.add(MenuVO.from(child));
            }
            vo.setChildren(childVOs);
        }
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<MenuVO> getChildren() {
        return children;
    }

    public void setChildren(List<MenuVO> children) {
        this.children = children;
    }
}