package com.zifang.ctc.core.ak.model;

import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/** 保存范围：按 domain 分组，每组至少一项；服务端整组原子替换。 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class AkScopeSaveRequest {
    private String akCode;
    private Map<String, java.util.List<AkScope>> scopesByDomain = new LinkedHashMap<>();

    public String getAkCode() { return akCode; }
    public void setAkCode(String akCode) { this.akCode = akCode; }
    public Map<String, java.util.List<AkScope>> getScopesByDomain() { return scopesByDomain; }
    public void setScopesByDomain(Map<String, java.util.List<AkScope>> scopesByDomain) {
        this.scopesByDomain = scopesByDomain;
    }
}
