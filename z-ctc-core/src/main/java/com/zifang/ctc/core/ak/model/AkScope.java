package com.zifang.ctc.core.ak.model;

/** 结构化授权范围：域、动作、资源三者必须同时明确，空范围默认拒绝。 */
public class AkScope {
    private String domain;
    private String action;
    private String resource;
    private String name;

    public AkScope() {
    }

    public AkScope(String domain, String action, String resource, String name) {
        this.domain = domain;
        this.action = action;
        this.resource = resource;
        this.name = name;
    }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
