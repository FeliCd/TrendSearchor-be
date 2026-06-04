package com.fpt.swp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphResponse {

    private List<Node> nodes;
    private List<Link> links;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private String id; // paper external ID or internal ID as string
        private String title;
        private Integer year;
        private Integer citationCount;
        private String type; // e.g., "PAPER"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link {
        private String source; // citing paper ID
        private String target; // cited paper ID
    }
}
