package com.fpt.swp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PaperUploadRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 1000, message = "Title must not exceed 1000 characters")
    private String title;

    @Size(max = 10000, message = "Abstract must not exceed 10000 characters")
    private String abstractText;

    private Integer year;

    private List<String> keywords;

    private String pdfUrl;
}
