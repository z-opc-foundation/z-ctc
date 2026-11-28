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
 * DictEntryDO —— 字典条目实体。
 *
 * @author zifang
 * @since 1.0.8
 */
@TableName("z_ctc_dictionary")
@BizModel(name = "字典条目", module = "z-ctc", description = "灵活字典的条目（flat/tree 两种形状，支持有效期与跨分类引用）")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class DictEntryDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    /** 所属分类编码 (z_ctc_dict_category.cat_code) */
    @BizField(name = "所属分类编码 (z_ctc_dict_category.cat_code)", type = BizField.FieldType.STRING)
    private String category;

    /** 条目键 (分类内唯一) */
    @BizField(name = "条目键 (分类内唯一)", type = BizField.FieldType.STRING)
    private String dictKey;

    /** 条目显示值 */
    @BizField(name = "条目显示值", type = BizField.FieldType.STRING)
    private String dictValue;

    /** 条目数据值 (value_type=number/boolean/json 时的载荷) */
    @BizField(name = "条目数据值 (value_type=number/boolean/json 时的载荷)", type = BizField.FieldType.STRING)
    private String dataValue;

    /** 父条目键 (entry_shape=tree 时成树, NULL=顶层) */
    @BizField(name = "父条目键 (entry_shape=tree 时成树, NULL=顶层)", type = BizField.FieldType.STRING)
    private String parentKey;

    /** 作用域租户 (空=全局) */
    @BizField(name = "作用域租户 (空=全局)", type = BizField.FieldType.STRING)
    private String tenantCode;

    /** 作用域域 (空=继承租户) */
    @BizField(name = "作用域域 (空=继承租户)", type = BizField.FieldType.STRING)
    private String domainCode;

    /** 条目扩展属性 (JSON) */
    @BizField(name = "条目扩展属性 (JSON)", type = BizField.FieldType.JSON)
    private String extProps;

    /** 标签 (逗号分隔) */
    @BizField(name = "标签 (逗号分隔)", type = BizField.FieldType.STRING)
    private String tags;

    /** 图标 */
    @BizField(name = "图标", type = BizField.FieldType.STRING)
    private String icon;

    /** 颜色 */
    @BizField(name = "颜色", type = BizField.FieldType.STRING)
    private String color;

    /** 引用分类编码 (跨分类引用) */
    @BizField(name = "引用分类编码 (跨分类引用)", type = BizField.FieldType.STRING)
    private String refCategory;

    /** 引用条目键 (跨分类引用) */
    @BizField(name = "引用条目键 (跨分类引用)", type = BizField.FieldType.STRING)
    private String refKey;

    /** 生效时间 */
    @BizField(name = "生效时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime effectiveFrom;

    /** 失效时间 */
    @BizField(name = "失效时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime effectiveTo;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDictKey() {
        return dictKey;
    }

    public void setDictKey(String dictKey) {
        this.dictKey = dictKey;
    }

    public String getDictValue() {
        return dictValue;
    }

    public void setDictValue(String dictValue) {
        this.dictValue = dictValue;
    }

    public String getDataValue() {
        return dataValue;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    public String getParentKey() {
        return parentKey;
    }

    public void setParentKey(String parentKey) {
        this.parentKey = parentKey;
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

    public String getExtProps() {
        return extProps;
    }

    public void setExtProps(String extProps) {
        this.extProps = extProps;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getRefCategory() {
        return refCategory;
    }

    public void setRefCategory(String refCategory) {
        this.refCategory = refCategory;
    }

    public String getRefKey() {
        return refKey;
    }

    public void setRefKey(String refKey) {
        this.refKey = refKey;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
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
