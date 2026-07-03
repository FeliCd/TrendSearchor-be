package com.fpt.swp.dto.ai;

import com.fpt.swp.dto.PaperDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperRerankRequest {
    private String query;
    private List<PaperDto> papers;
}
