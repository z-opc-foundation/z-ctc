package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * 应用菜单 DO.
 * <p>
 * 对应表 z_ctc_app_menu (CREATE TABLE 在生产 DB / _doc/002_deploy/init/z-ctc-authz-override.sql):
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
@BizModel(name = "应用菜单 DO", module = "z-ctc", description = "应用菜单 DO. 对应表 z_ctc_app_menu (CREATE TABLE 在生产 DB / _doc/002_deploy/init/z-ctc-authz-override.sql): id — PK app_code — 应用编码 (如 'z-opc') menu_code — 菜单编码 (在 app_code 内唯一, 如 'ctc-overview') menu_name — 菜单名称 parent_code — 父菜单编码 (一级菜单为空字符串 '' 或 NULL) menu_type — 类型: MENU / BUTTON / DIVIDER 等 icon — antd icon 名称 (如 'UserOutlined'), 由前端 iconMap 解析 path — 路由路径 (如 '/ctc/overview') sort_order — 同级排序, asc status — 1启用 / 0禁用 source_type — CODE / LOW_CODE page_id — LOW_CODE 类型对应的低代码 pageId component — 动态 component 名 (前端按需 import) description / created_at / updated_at 树形结构: {@link #children} 由 service 在构造 tree 时填充, 不入库.")
@TableName("z_ctc_app_menu")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class MenuDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("app_code")
    @BizField(name = "所属应用编码", type = BizField.FieldType.STRING)
    private String appCode;

    @TableField("menu_code")
    @BizField(name = "菜单编码", type = BizField.FieldType.STRING)
    private String menuCode;

    @TableField("menu_name")
    @BizField(name = "菜单名称", type = BizField.FieldType.STRING)
    private String menuName;

    /**
     * 父菜单编码 (一级菜单为 '' 或 null). 业务上比 parent_id 更直观, 便于跨环境迁移.
     */
    @TableField("parent_code")
    @BizField(name = "父菜单编码", type = BizField.FieldType.STRING)
    private String parentCode;

    @TableField("menu_type")
    @BizField(name = "MENU/GROUP/IFRAME/LINK", type = BizField.FieldType.STRING)
    private String menuType;

    @TableField("icon")
    @BizField(name = "图标", type = BizField.FieldType.STRING)
    private String icon;

    @TableField("path")
    @BizField(name = "路由路径", type = BizField.FieldType.STRING)
    private String path;

    @TableField("sort_order")
    @BizField(name = "排序号", type = BizField.FieldType.INTEGER)
    private Integer sortOrder;

    @TableField("status")
    @BizField(name = "状态 0-禁用 1-启用", type = BizField.FieldType.INTEGER)
    private Integer status;

    @TableField("source_type")
    @BizField(name = "CODE/LOW_CODE", type = BizField.FieldType.STRING)
    private String sourceType;

    @TableField("page_id")
    @BizField(name = "低代码页面ID (LOW_CODE 类型使用)", type = BizField.FieldType.INTEGER)
    private Long pageId;

    @TableField("component")
    @BizField(name = "前端组件名", type = BizField.FieldType.STRING)
    private String component;

    @TableField("description")
    @BizField(name = "描述", type = BizField.FieldType.STRING)
    private String description;

    /**
     * RBAC+Override: 菜单关联的权限资源 ID (关联 z_ctc_spc_resource.id).
     * NULL 表示无权限控制 (所有人都可见).
     */
    @TableField("resource_id")
    @BizField(name = "resourceId", type = BizField.FieldType.INTEGER)
    private Long resourceId;

    @TableField("created_at")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime updatedAt;

    /**
     * 树形子节点 — 由 service.listAsTree 填充, 不入库.
     */
    @TableField(exist = false)
    @BizField(name = "树形子节点 — 由 service.listAsTree 填充, 不入库", type = BizField.FieldType.JSON)
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

