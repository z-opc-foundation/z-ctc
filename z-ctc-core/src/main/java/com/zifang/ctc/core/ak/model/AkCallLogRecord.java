package com.zifang.ctc.core.ak.model;

import java.time.Instant;

public class AkCallLogRecord {
    private String recordId;
    private String traceId;
    private String akId;
    private String akCode;
    private String akMasked;
    private String akName;
    private String ownerId;
    private String ownerName;
    private Instant startedAt;
    private Instant completedAt;
    private long durationMs;
    private String httpMethod;
    private String requestPath;
    private String query;
    private int httpStatus;
    private String outcome;
    private String errorSummary;

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getAkId() { return akId; }
    public void setAkId(String akId) { this.akId = akId; }
    public String getAkCode() { return akCode; }
    public void setAkCode(String akCode) { this.akCode = akCode; }
    public String getAkMasked() { return akMasked; }
    public void setAkMasked(String akMasked) { this.akMasked = akMasked; }
    public String getAkName() { return akName; }
    public void setAkName(String akName) { this.akName = akName; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
}
