package com.fpt.swp.model;

public enum PaperStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Đã từng APPROVED nhưng bị gỡ sau khi có báo cáo vi phạm bản quyền được xử lý. */
    TAKEN_DOWN
}
