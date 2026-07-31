package com.fpt.swp.model;

/** Vòng đời của một gói đăng ký của user. */
public enum SubscriptionStatus {
    /** Đã tạo, đang chờ thanh toán. */
    PENDING,
    /** Đã thanh toán, còn hiệu lực. */
    ACTIVE,
    /** Hết hạn (quá end_date). */
    EXPIRED,
    /** Bị hủy. */
    CANCELLED
}
