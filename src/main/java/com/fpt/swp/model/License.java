package com.fpt.swp.model;

/**
 * Giấy phép mà uploader chọn cho bài báo/luận văn khi upload, xác định
 * người đọc được phép làm gì với nội dung (chia sẻ, phân phối lại, thương mại hóa...).
 */
public enum License {
    /** Creative Commons Attribution — cho phép chia sẻ/phân phối lại, phải ghi công tác giả. */
    CC_BY,
    /** Creative Commons Attribution-NonCommercial — như CC_BY nhưng cấm mục đích thương mại. */
    CC_BY_NC,
    /** Tác giả/nhà xuất bản giữ toàn bộ quyền; chỉ hiển thị metadata/abstract trên hệ thống. */
    ALL_RIGHTS_RESERVED,
    /** Tác giả cấp quyền đăng tải cho riêng nền tảng TrendSearchor theo thỏa thuận upload. */
    AUTHOR_AGREEMENT
}
