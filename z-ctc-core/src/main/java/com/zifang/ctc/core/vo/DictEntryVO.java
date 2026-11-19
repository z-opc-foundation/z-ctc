package com.zifang.ctc.core.vo;

import com.zifang.ctc.core.domain.entity.DictEntryDO;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DictEntryVO —— 字典条目实体。
 *
 * @author zifang
 * @since 1.0.8
 */
public class DictEntryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 所属分类编码 (z_ctc_dict_category.cat_code) */
    private String category;

    /** 条目键 (分类内唯一) */
    private String dictKey;

    /** 条目显示值 */
    private String dictValue;

    /** 条目数据值 (value_type=number/boolean/json 时的载荷) */
    private String dataValue;

    /** 父条目键 (entry_shape=tree 时成树, NULL=顶层) */
    private String parentKey;

    /** 作用域租户 (空=全局) */
    private String tenantCode;

    /** 作用域域 (空=继承租户) */
    private String domainCode;

    /** 条目扩展属性 (JSON) */
    private String extProps;

    /** 标签 (逗号分隔) */
    private String tags;

    /** 图标 */
    private String icon;

    /** 颜色 */
    private String color;

    /** 引用分类编码 (跨分类引用) */
    private String refCategory;

    /** 引用条目键 (跨分类引用) */
    private String refKey;

    /** 生效时间 */
    private LocalDateTime effectiveFrom;

    /** 失效时间 */
    private LocalDateTime effectiveTo;

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

    /**
     * 从 DictEntryDO 转成 VO。
     */
    public static DictEntryVO from(DictEntryDO src) {
        if (src == null) return null;
        DictEntryVO vo = new DictEntryVO();
        vo.setId(src.getId());
        vo.setCategory(src.getCategory());
        vo.setDictKey(src.getDictKey());
        vo.setDictValue(src.getDictValue());
        vo.setDataValue(src.getDataValue());
        vo.setParentKey(src.getParentKey());
        vo.setTenantCode(src.getTenantCode());
        vo.setDomainCode(src.getDomainCode());
        vo.setExtProps(src.getExtProps());
        vo.setTags(src.getTags());
        vo.setIcon(src.getIcon());
        vo.setColor(src.getColor());
        vo.setRefCategory(src.getRefCategory());
        vo.setRefKey(src.getRefKey());
        vo.setEffectiveFrom(src.getEffectiveFrom());
        vo.setEffectiveTo(src.getEffectiveTo());
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
