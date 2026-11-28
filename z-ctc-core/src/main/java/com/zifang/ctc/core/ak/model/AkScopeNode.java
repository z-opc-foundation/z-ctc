package com.zifang.ctc.core.ak.model;

import java.util.ArrayList;
import java.util.List;

/** 范围树节点；资源节点携带结构化范围，语义节点可只含名称和 children。 */
public class AkScopeNode {
    private AkScope scope;
    private String name;
    private List<AkScopeNode> children = new ArrayList<>();

    public AkScope getScope() { return scope; }
    public void setScope(AkScope scope) { this.scope = scope; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<AkScopeNode> getChildren() { return children; }
    public void setChildren(List<AkScopeNode> children) {
        this.children = children == null ? new ArrayList<>() : children;
    }
}
