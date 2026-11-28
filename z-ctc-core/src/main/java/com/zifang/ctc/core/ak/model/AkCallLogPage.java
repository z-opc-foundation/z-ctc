package com.zifang.ctc.core.ak.model;

import java.util.ArrayList;
import java.util.List;

public class AkCallLogPage {
    private final List<AkCallLogRecord> records = new ArrayList<>();
    private final int page;
    private final int size;
    private final boolean hasMore;
    private final long startTime;
    private final long endTime;

    public AkCallLogPage(List<AkCallLogRecord> records, int page, int size, boolean hasMore,
                         long startTime, long endTime) {
        this.records.addAll(records);
        this.page = page;
        this.size = size;
        this.hasMore = hasMore;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public List<AkCallLogRecord> getRecords() { return records; }
    /** future-ak 兼容别名。 */
    public List<AkCallLogRecord> getItems() { return records; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public boolean isHasMore() { return hasMore; }
    public String getNextCursor() { return hasMore ? Integer.toString(page + 1) : null; }
    public boolean isScanLimited() { return false; }
    public int getRetentionDays() { return 14; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
}
