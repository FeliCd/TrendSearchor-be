package com.fpt.swp.dto.ai;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperSummaryRequest {

    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 6000, message = "Abstract must not exceed 6000 characters")
    private String abstractText;

    @Size(max = 1000, message = "Authors must not exceed 1000 characters")
    private String authors;

    @Size(max = 10, message = "Year must not exceed 10 characters")
    private String year;
}
