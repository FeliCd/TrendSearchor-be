package com.fpt.swp.model;

/** Vòng đời xử lý của một báo cáo vi phạm bản quyền (notice-and-takedown). */
public enum CopyrightReportStatus {
    /** Mới gửi, đang chờ moderator/admin xem xét. */
    PENDING,
    /** Đã xem xét, xác định không vi phạm — không hành động thêm. */
    DISMISSED,
    /** Đã xem xét, xác định có vi phạm — bài đã bị gỡ (status = TAKEN_DOWN). */
    ACTION_TAKEN
}
