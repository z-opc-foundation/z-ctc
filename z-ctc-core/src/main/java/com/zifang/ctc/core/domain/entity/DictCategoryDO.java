package com.zifang.ctc.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DictCategoryDO —— 字典分类实体。
 *
 * @author zifang
 * @since 1.0.8
 */
@TableName("z_ctc_dict_category")
@BizModel(name = "字典分类", module = "z-ctc", description = "灵活字典的分类（可成树，作用域三级：全局/租户/域）")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class DictCategoryDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    /** 分类编码 (业务引用键, 如 user_status) */
    @BizField(name = "分类编码 (业务引用键, 如 user_status)", type = BizField.FieldType.STRING)
    private String catCode;

    /** 分类显示名 */
    @BizField(name = "分类显示名", type = BizField.FieldType.STRING)
    private String catName;

    /** 父分类编码 (分类自身可成树, NULL=顶层) */
    @BizField(name = "父分类编码 (分类自身可成树, NULL=顶层)", type = BizField.FieldType.STRING)
    private String parentCode;

    /** 作用域 GLOBAL/TENANT/DOMAIN */
    @BizField(name = "作用域 GLOBAL/TENANT/DOMAIN", type = BizField.FieldType.ENUM)
    private String scope;

    /** 作用域租户 (空=全局) */
    @BizField(name = "作用域租户 (空=全局)", type = BizField.FieldType.STRING)
    private String tenantCode;

    /** 作用域域 (空=继承租户) */
    @BizField(name = "作用域域 (空=继承租户)", type = BizField.FieldType.STRING)
    private String domainCode;

    /** 条目值类型 string/number/boolean/json */
    @BizField(name = "条目值类型 string/number/boolean/json", type = BizField.FieldType.ENUM)
    private String valueType;

    /** 条目形状 flat/tree */
    @BizField(name = "条目形状 flat/tree", type = BizField.FieldType.ENUM)
    private String entryShape;

    /** 条目扩展属性 schema (JSON: 字段名->类型/标签/必填) */
    @BizField(name = "条目扩展属性 schema (JSON: 字段名->类型/标签/必填)", type = BizField.FieldType.JSON)
    private String extSchema;

    /** 系统内置 (1=代码登记枚举, 禁改编码禁删) */
    @BizField(name = "系统内置 (1=代码登记枚举, 禁改编码禁删)", type = BizField.FieldType.INTEGER)
    private Integer builtin;

    /** 状态 (1=启用 0=禁用) */
    @BizField(name = "状态 (1=启用 0=禁用)", type = BizField.FieldType.INTEGER)
    private Integer status;

    /** 排序 */
    @BizField(name = "排序", type = BizField.FieldType.INTEGER)
    private Integer sortOrder;

    /** 描述 */
    @BizField(name = "描述", type = BizField.FieldType.TEXT)
    private String description;

    /** 创建时间 */
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdAt;

    /** 创建者 */
    @BizField(name = "创建者", type = BizField.FieldType.STRING)
    private String createdBy;

    /** 更新时间 */
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime updatedAt;

    /** 更新者 */
    @BizField(name = "更新者", type = BizField.FieldType.STRING)
    private String updatedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCatCode() {
        return catCode;
    }

    public void setCatCode(String catCode) {
        this.catCode = catCode;
    }

    public String getCatName() {
        return catName;
    }

    public void setCatName(String catName) {
        this.catName = catName;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getEntryShape() {
        return entryShape;
    }

    public void setEntryShape(String entryShape) {
        this.entryShape = entryShape;
    }

    public String getExtSchema() {
        return extSchema;
    }

    public void setExtSchema(String extSchema) {
        this.extSchema = extSchema;
    }

    public Integer getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Integer builtin) {
        this.builtin = builtin;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

}
