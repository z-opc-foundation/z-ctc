package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用菜单 DO.
 * <p>
 * 对应表 z_ctc_app_menu (CREATE TABLE 在生产 DB / _doc/004_sql/z-ctc-micro-frontend.sql):
 * <ul>
 *   <li>id — PK</li>
 *   <li>app_code — 应用编码 (如 'z-opc')</li>
 *   <li>menu_code — 菜单编码 (在 app_code 内唯一, 如 'ctc-overview')</li>
 *   <li>menu_name — 菜单名称</li>
 *   <li>parent_code — 父菜单编码 (一级菜单为空字符串 '' 或 NULL)</li>
 *   <li>menu_type — 类型: MENU / BUTTON / DIVIDER 等</li>
 *   <li>icon — antd icon 名称 (如 'UserOutlined'), 由前端 iconMap 解析</li>
 *   <li>path — 路由路径 (如 '/ctc/overview')</li>
 *   <li>sort_order — 同级排序, asc</li>
 *   <li>status — 1启用 / 0禁用</li>
 *   <li>source_type — CODE / LOW_CODE</li>
 *   <li>page_id — LOW_CODE 类型对应的低代码 pageId</li>
 *   <li>component — 动态 component 名 (前端按需 import)</li>
 *   <li>description / created_at / updated_at</li>
 * </ul>
 * <p>
 * 树形结构: {@link #children} 由 service 在构造 tree 时填充, 不入库.
 */
@TableName("z_ctc_app_menu")
public class MenuDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("app_code")
    private String appCode;

    @TableField("menu_code")
    private String menuCode;

    @TableField("menu_name")
    private String menuName;

    /**
     * 父菜单编码 (一级菜单为 '' 或 null). 业务上比 parent_id 更直观, 便于跨环境迁移.
     */
    @TableField("parent_code")
    private String parentCode;

    @TableField("menu_type")
    private String menuType;

    @TableField("icon")
    private String icon;

    @TableField("path")
    private String path;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private Integer status;

    @TableField("source_type")
    private String sourceType;

    @TableField("page_id")
    private Long pageId;

    @TableField("component")
    private String component;

    @TableField("description")
    private String description;

    /**
     * RBAC+Override: 菜单关联的权限资源 ID (关联 z_ctc_spc_resource.id).
     * NULL 表示无权限控制 (所有人都可见).
     */
    @TableField("resource_id")
    private Long resourceId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 树形子节点 — 由 service.listAsTree 填充, 不入库.
     */
    @TableField(exist = false)
    private List<MenuDO> children = new ArrayList<>();

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

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
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

    public List<MenuDO> getChildren() {
        return children;
    }

    public void setChildren(List<MenuDO> children) {
        this.children = children;
    }
}