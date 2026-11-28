package com.zifang.ctc.core.ak.model;

import java.time.Instant;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/** 兼容页码与 future-ak 游标/epoch 毫秒参数的调用日志查询。 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class AkCallLogQuery {
    private int page = 1;
    private int size = 20;
    private String akCode;
    private String akId;
    private Instant from;
    private Instant to;
    private Long startTime;
    private Long endTime;
    private String cursor;
    private String method;
    private String pathKeyword;
    private String outcome;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getAkCode() { return akCode; }
    public void setAkCode(String akCode) { this.akCode = akCode; }
    public String getAkId() { return akId; }
    public void setAkId(String akId) { this.akId = akId; }
    public Instant getFrom() { return from; }
    public void setFrom(Instant from) { this.from = from; }
    public Instant getTo() { return to; }
    public void setTo(Instant to) { this.to = to; }
    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public String getCursor() { return cursor; }
    public void setCursor(String cursor) { this.cursor = cursor; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPathKeyword() { return pathKeyword; }
    public void setPathKeyword(String pathKeyword) { this.pathKeyword = pathKeyword; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
}
