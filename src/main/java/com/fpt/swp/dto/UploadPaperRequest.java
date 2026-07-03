package com.fpt.swp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadPaperRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Abstract text is required")
    private String abstractText;

    private Integer year;

    private String paperUri;

    private List<String> authors;

    private List<String> journals;

    private List<String> keywords;
}
