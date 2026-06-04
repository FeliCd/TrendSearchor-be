package com.fpt.swp.dto;

import com.fpt.swp.model.Collection;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CollectionDto {
    private Long id;
    private String name;
    private String description;
    private int bookmarkCount;
    private LocalDateTime createdAt;

    public static CollectionDto fromCollection(Collection collection) {
        return CollectionDto.builder()
                .id(collection.getId())
                .name(collection.getName())
                .description(collection.getDescription())
                .bookmarkCount(collection.getBookmarks() != null ? collection.getBookmarks().size() : 0)
                .createdAt(collection.getCreatedAt())
                .build();
    }
}
