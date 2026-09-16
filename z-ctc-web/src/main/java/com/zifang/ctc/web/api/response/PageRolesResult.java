package com.zifang.ctc.web.api.response;

import com.zifang.ctc.core.vo.RoleVO;

import java.util.List;

public class PageRolesResult {

    private List<RoleVO> data;
    private long total;

    public PageRolesResult() {
    }

    public PageRolesResult(List<RoleVO> data, long total) {
        this.data = data;
        this.total = total;
    }

    public List<RoleVO> getData() {
        return data;
    }

    public void setData(List<RoleVO> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
