package com.fpt.swp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body cho PATCH /api/moderation/copyright-reports/{id}/resolve. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveCopyrightReportRequest {

    /** Quyết định của moderator đối với report. */
    public enum Action {
        /** Không vi phạm — đóng report, bài giữ nguyên trạng thái. */
        DISMISS,
        /** Có vi phạm — gỡ bài (status = TAKEN_DOWN) và đóng mọi report đang chờ của bài đó. */
        TAKE_DOWN
    }

    @NotNull(message = "Action is required. Choose DISMISS or TAKE_DOWN")
    private Action action;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
