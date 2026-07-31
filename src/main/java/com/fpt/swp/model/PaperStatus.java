package com.fpt.swp.model;

public enum PaperStatus {
    PENDING,
    APPROVED,
    REJECTED,
    REVOKED,
    /**
     * Đã từng APPROVED nhưng bị gỡ sau khi có báo cáo vi phạm bản quyền được xử lý.
     */
    TAKEN_DOWN,
    /**
     * Admin thu hồi một bài đã APPROVED — gỡ khỏi công khai, không tự về PENDING.
     */

}
