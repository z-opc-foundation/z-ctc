package com.zifang.ctc.web.api.response;

import com.zifang.ctc.core.vo.ResourceVO;

import java.util.List;

public class PageResourcesByAppResult {

    private List<ResourceVO> data;
    private long total;

    public PageResourcesByAppResult() {
    }

    public PageResourcesByAppResult(List<ResourceVO> data, long total) {
        this.data = data;
        this.total = total;
    }

    public List<ResourceVO> getData() {
        return data;
    }

    public void setData(List<ResourceVO> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
