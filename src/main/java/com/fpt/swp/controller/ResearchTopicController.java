package com.fpt.swp.controller;

import com.fpt.swp.dto.ResearchTopicDto;
import com.fpt.swp.service.ResearchTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/research-topics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ResearchTopicController {

    private final ResearchTopicService researchTopicService;

    @GetMapping
    public ResponseEntity<List<ResearchTopicDto>> getAllTopics() {
        return ResponseEntity.ok(researchTopicService.getAllTopics());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResearchTopicDto> getTopicById(@PathVariable Long id) {
        return ResponseEntity.ok(researchTopicService.getTopicById(id));
    }

    @PostMapping
    public ResponseEntity<ResearchTopicDto> createTopic(@RequestBody ResearchTopicDto dto) {
        return ResponseEntity.ok(researchTopicService.createTopic(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResearchTopicDto> updateTopic(@PathVariable Long id, @RequestBody ResearchTopicDto dto) {
        return ResponseEntity.ok(researchTopicService.updateTopic(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        researchTopicService.deleteTopic(id);
        return ResponseEntity.ok().build();
    }
}
