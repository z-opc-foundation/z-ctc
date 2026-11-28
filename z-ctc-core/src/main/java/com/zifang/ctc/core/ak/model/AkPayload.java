package com.zifang.ctc.core.ak.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** AK 创建载荷与列表过滤参数。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AkPayload {

    /** 创建：PERSONAL / APPLICATION。 */
    private String ownerType;
    private String name;
    /** 列表过滤：名称关键词。 */
    private String keyword;
    /** 列表过滤：归属类型，空表示不过滤。 */
    private String ownerTypeFilter;
    /** 列表过滤：归属人关键词。 */
    private String ownerName;
    /** 创建时的结构化范围；空集合创建的 AK 默认拒绝所有调用。 */
    private java.util.Map<String, java.util.List<com.zifang.ctc.core.ak.model.AkScope>> scopesByDomain;

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getOwnerTypeFilter() {
        return ownerTypeFilter;
    }

    public void setOwnerTypeFilter(String ownerTypeFilter) {
        this.ownerTypeFilter = ownerTypeFilter;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public java.util.Map<String, java.util.List<com.zifang.ctc.core.ak.model.AkScope>> getScopesByDomain() {
        return scopesByDomain;
    }

    public void setScopesByDomain(
            java.util.Map<String, java.util.List<com.zifang.ctc.core.ak.model.AkScope>> scopesByDomain) {
        this.scopesByDomain = scopesByDomain;
    }
}