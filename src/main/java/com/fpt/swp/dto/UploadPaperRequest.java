package com.fpt.swp.dto;

import com.fpt.swp.model.License;
import com.fpt.swp.model.PublicationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadPaperRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 1000, message = "Title must not exceed 1000 characters")
    private String title;

    @NotBlank(message = "Abstract text is required")
    @Size(max = 10000, message = "Abstract must not exceed 10000 characters")
    private String abstractText;

    private Integer year;

    @Size(max = 500, message = "Paper URI must not exceed 500 characters")
    private String paperUri;

    @Size(max = 20, message = "Cannot list more than 20 authors")
    private List<String> authors;

    @Size(max = 10, message = "Cannot list more than 10 journals")
    private List<String> journals;

    @Size(max = 30, message = "Cannot list more than 30 keywords")
    private List<String> keywords;

    // ─── Legal / Copyright declarations ────────────────────────────────────────

    /** Giấy phép uploader chọn cho nội dung — bắt buộc với mọi upload mới. */
    @NotNull(message = "License is required. Choose one of: CC_BY, CC_BY_NC, ALL_RIGHTS_RESERVED, AUTHOR_AGREEMENT")
    private License license;

    /** Bài là nghiên cứu gốc hay đăng lại bài đã xuất bản — bắt buộc khai báo. */
    @NotNull(message = "Publication type is required. Choose ORIGINAL_THESIS or PREVIOUSLY_PUBLISHED")
    private PublicationType publicationType;

    /** Uploader phải xác nhận là tác giả hoặc có quyền hợp pháp để đăng bài. */
    @NotNull(message = "Ownership confirmation is required")
    @AssertTrue(message = "You must confirm that you are the author or have the legal right to upload this paper")
    private Boolean ownershipConfirmed;

    /** Uploader phải đồng ý Upload Agreement / Terms of Service. */
    @NotNull(message = "Terms acceptance is required")
    @AssertTrue(message = "You must accept the Terms of Service to upload a paper")
    private Boolean termsAccepted;

    /**
     * Tùy chọn: ngày hết hạn embargo — bài chỉ hiển thị công khai sau ngày này
     * dù đã được duyệt. Phải là một ngày trong tương lai nếu được cung cấp.
     */
    @Future(message = "Embargo date must be in the future")
    private LocalDate embargoUntil;
}
