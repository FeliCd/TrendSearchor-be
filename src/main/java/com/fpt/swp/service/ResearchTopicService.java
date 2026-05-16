package com.fpt.swp.service;

import com.fpt.swp.dto.ResearchTopicDto;
import com.fpt.swp.model.ResearchTopic;
import com.fpt.swp.repository.ResearchTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResearchTopicService {

    private final ResearchTopicRepository repository;

    @Transactional(readOnly = true)
    public List<ResearchTopicDto> getAllTopics() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResearchTopicDto getTopicById(Long id) {
        return repository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Research Topic not found with id: " + id));
    }

    @Transactional
    public ResearchTopicDto createTopic(ResearchTopicDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw new RuntimeException("Research Topic name already exists!");
        }
        ResearchTopic entity = mapToEntity(dto);
        entity.setId(null);
        return mapToDto(repository.save(entity));
    }

    @Transactional
    public ResearchTopicDto updateTopic(Long id, ResearchTopicDto dto) {
        ResearchTopic entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Research Topic not found with id: " + id));
        
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCategory(dto.getCategory());
        if (dto.getPopularityScore() != null) {
            entity.setPopularityScore(dto.getPopularityScore());
        }
        
        return mapToDto(repository.save(entity));
    }

    @Transactional
    public void deleteTopic(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Research Topic not found with id: " + id);
        }
        repository.deleteById(id);
    }

    private ResearchTopicDto mapToDto(ResearchTopic entity) {
        return ResearchTopicDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .popularityScore(entity.getPopularityScore())
                .build();
    }

    private ResearchTopic mapToEntity(ResearchTopicDto dto) {
        return ResearchTopic.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .popularityScore(dto.getPopularityScore() != null ? dto.getPopularityScore() : 0.0)
                .build();
    }
}
