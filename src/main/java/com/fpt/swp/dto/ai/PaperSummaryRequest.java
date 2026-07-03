package com.fpt.swp.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperSummaryRequest {
    private String title;
    private String abstractText;
    private String authors;
    private String year;
}
