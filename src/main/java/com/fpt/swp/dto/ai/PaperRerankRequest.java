package com.fpt.swp.dto.ai;

import com.fpt.swp.dto.PaperDto;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperRerankRequest {

    @Size(max = 500, message = "Query must not exceed 500 characters")
    private String query;

    @Size(max = 20, message = "Cannot rerank more than 20 papers at once")
    private List<PaperDto> papers;
}
