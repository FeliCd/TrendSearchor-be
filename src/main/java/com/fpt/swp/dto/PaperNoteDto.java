package com.fpt.swp.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperNoteDto {
    private Long id;
    private String paperExternalId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
