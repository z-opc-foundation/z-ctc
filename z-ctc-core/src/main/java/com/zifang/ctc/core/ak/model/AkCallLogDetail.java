package com.zifang.ctc.core.ak.model;

import java.util.Map;

public class AkCallLogDetail extends AkCallLogRecord {
    private Map<String, String> requestHeaders;
    private String requestBody;
    private String responseBody;
    private AkCallLogRecord summary;
    private Map<String, String> payload;

    public AkCallLogRecord getSummary() { return summary; }
    public void setSummary(AkCallLogRecord summary) { this.summary = summary; }
    public boolean isPayloadAvailable() { return requestBody != null || responseBody != null; }
    public Map<String, String> getPayload() { return payload; }
    public void setPayload(Map<String, String> payload) { this.payload = payload; }

    public Map<String, String> getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(Map<String, String> requestHeaders) { this.requestHeaders = requestHeaders; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
}
