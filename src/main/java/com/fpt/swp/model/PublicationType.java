package com.fpt.swp.model;

/**
 * Phân loại bản chất công bố của bài upload — dùng để phân biệt luận văn/nghiên cứu
 * gốc của chính uploader với bài đã xuất bản ở nơi khác (nơi nhà xuất bản có thể giữ bản quyền).
 */
public enum PublicationType {
    /** Luận văn/nghiên cứu nguyên gốc của chính uploader, chưa từng công bố nơi khác. */
    ORIGINAL_THESIS,
    /** Bài đã được xuất bản trước đó (hội nghị, tạp chí...); uploader chỉ đăng lại. */
    PREVIOUSLY_PUBLISHED
}
