package com.zifang.ctc.common.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页响应 VO.
 */
public class PageResponseVO<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<T> data;
    private long total;
    private int pageNum;
    private int pageSize;

    public PageResponseVO() {
    }

    public PageResponseVO(List<T> data, long total, int pageNum, int pageSize) {
        this.data = data;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
