package com.fpt.swp.service;

import com.fpt.swp.dto.ApiDataSourceDto;
import com.fpt.swp.model.ApiDataSource;
import com.fpt.swp.repository.ApiDataSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiDataSourceService {

    private final ApiDataSourceRepository repository;

    @Transactional(readOnly = true)
    public List<ApiDataSourceDto> getAllSources() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApiDataSourceDto getSourceById(Long id) {
        return repository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("API Data Source not found with id: " + id));
    }

    @Transactional
    public ApiDataSourceDto createSource(ApiDataSourceDto dto) {
        if (repository.existsBySourceName(dto.getSourceName())) {
            throw new RuntimeException("API Data Source name already exists!");
        }
        ApiDataSource entity = mapToEntity(dto);
        entity.setId(null);
        return mapToDto(repository.save(entity));
    }

    @Transactional
    public ApiDataSourceDto updateSource(Long id, ApiDataSourceDto dto) {
        ApiDataSource entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("API Data Source not found with id: " + id));
        
        entity.setSourceName(dto.getSourceName());
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setApiKey(dto.getApiKey());
        entity.setRateLimitPerDay(dto.getRateLimitPerDay() != null ? dto.getRateLimitPerDay() : 1000);
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        return mapToDto(repository.save(entity));
    }

    @Transactional
    public void deleteSource(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("API Data Source not found with id: " + id);
        }
        repository.deleteById(id);
    }

    private ApiDataSourceDto mapToDto(ApiDataSource entity) {
        return ApiDataSourceDto.builder()
                .id(entity.getId())
                .sourceName(entity.getSourceName())
                .baseUrl(entity.getBaseUrl())
                .apiKey(entity.getApiKey())
                .rateLimitPerDay(entity.getRateLimitPerDay())
                .isActive(entity.getIsActive())
                .lastSyncAt(entity.getLastSyncAt())
                .lastSyncStatus(entity.getLastSyncStatus())
                .recordsSynced(entity.getRecordsSynced())
                .build();
    }

    private ApiDataSource mapToEntity(ApiDataSourceDto dto) {
        return ApiDataSource.builder()
                .id(dto.getId())
                .sourceName(dto.getSourceName())
                .baseUrl(dto.getBaseUrl())
                .apiKey(dto.getApiKey())
                .rateLimitPerDay(dto.getRateLimitPerDay() != null ? dto.getRateLimitPerDay() : 1000)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
    }
}
