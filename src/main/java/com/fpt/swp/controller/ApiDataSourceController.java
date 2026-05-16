package com.fpt.swp.controller;

import com.fpt.swp.dto.ApiDataSourceDto;
import com.fpt.swp.service.ApiDataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/api-sources")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApiDataSourceController {

    private final ApiDataSourceService apiDataSourceService;

    @GetMapping
    public ResponseEntity<List<ApiDataSourceDto>> getAllSources() {
        return ResponseEntity.ok(apiDataSourceService.getAllSources());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiDataSourceDto> getSourceById(@PathVariable Long id) {
        return ResponseEntity.ok(apiDataSourceService.getSourceById(id));
    }

    @PostMapping
    public ResponseEntity<ApiDataSourceDto> createSource(@RequestBody ApiDataSourceDto dto) {
        return ResponseEntity.ok(apiDataSourceService.createSource(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiDataSourceDto> updateSource(@PathVariable Long id, @RequestBody ApiDataSourceDto dto) {
        return ResponseEntity.ok(apiDataSourceService.updateSource(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable Long id) {
        apiDataSourceService.deleteSource(id);
        return ResponseEntity.ok().build();
    }
}
