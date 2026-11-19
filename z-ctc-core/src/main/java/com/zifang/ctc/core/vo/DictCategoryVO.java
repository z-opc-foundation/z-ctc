package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.DictCategoryDO;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DictCategoryVO —— 字典条目实体。
 *
 * @author zifang
 * @since 1.0.8
 */
public class DictCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 分类编码 (业务引用键, 如 user_status) */
    private String catCode;

    /** 分类显示名 */
    private String catName;

    /** 父分类编码 (分类自身可成树, NULL=顶层) */
    private String parentCode;

    /** 作用域 GLOBAL/TENANT/DOMAIN */
    private String scope;

    /** 作用域租户 (空=全局) */
    private String tenantCode;

    /** 作用域域 (空=继承租户) */
    private String domainCode;

    /** 条目值类型 string/number/boolean/json */
    private String valueType;

    /** 条目形状 flat/tree */
    private String entryShape;

    /** 条目扩展属性 schema (JSON: 字段名->类型/标签/必填) */
    private String extSchema;

    /** 系统内置 (1=代码登记枚举, 禁改编码禁删) */
    private Integer builtin;

    /** 状态 (1=启用 0=禁用) */
    private Integer status;

    /** 排序 */
    private Integer sortOrder;

    /** 描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 创建者 */
    private String createdBy;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 更新者 */
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

    /**
     * 从 DictCategoryDO 转成 VO。
     */
    public static DictCategoryVO from(DictCategoryDO src) {
        if (src == null) return null;
        DictCategoryVO vo = new DictCategoryVO();
        vo.setId(src.getId());
        vo.setCatCode(src.getCatCode());
        vo.setCatName(src.getCatName());
        vo.setParentCode(src.getParentCode());
        vo.setScope(src.getScope());
        vo.setTenantCode(src.getTenantCode());
        vo.setDomainCode(src.getDomainCode());
        vo.setValueType(src.getValueType());
        vo.setEntryShape(src.getEntryShape());
        vo.setExtSchema(src.getExtSchema());
        vo.setBuiltin(src.getBuiltin());
        vo.setStatus(src.getStatus());
        vo.setSortOrder(src.getSortOrder());
        vo.setDescription(src.getDescription());
        vo.setCreatedAt(src.getCreatedAt());
        vo.setCreatedBy(src.getCreatedBy());
        vo.setUpdatedAt(src.getUpdatedAt());
        vo.setUpdatedBy(src.getUpdatedBy());
        return vo;
    }

}
