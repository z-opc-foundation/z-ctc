package com.zifang.ctc.core.ak.model;

import java.util.ArrayList;
import java.util.List;

public class AkScopeDomain {
    private String domain;
    private String displayName;
    private List<AkScopeNode> nodes = new ArrayList<>();

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public List<AkScopeNode> getNodes() { return nodes; }
    public void setNodes(List<AkScopeNode> nodes) { this.nodes = nodes; }
}
