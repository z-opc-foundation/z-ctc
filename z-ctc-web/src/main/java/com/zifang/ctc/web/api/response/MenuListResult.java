package com.zifang.ctc.web.api.response;

import com.zifang.ctc.core.vo.MenuVO;

import java.util.List;

public class MenuListResult {

    private List<MenuVO> data;
    private long total;

    public MenuListResult() {
    }

    public MenuListResult(List<MenuVO> data, long total) {
        this.data = data;
        this.total = total;
    }

    public List<MenuVO> getData() {
        return data;
    }

    public void setData(List<MenuVO> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
