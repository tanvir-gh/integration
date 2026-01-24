package com.entertainment.catalog.controller;

import com.entertainment.catalog.domain.Content;
import com.entertainment.catalog.domain.ContentType;
import com.entertainment.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @PostMapping
    public ResponseEntity<Content> createContent(@RequestBody CreateContentRequest request) {
        Content content = Content.builder()
                .title(request.title())
                .type(ContentType.valueOf(request.type()))
                .durationMinutes(request.durationMinutes())
                .genre(request.genre())
                .build();
        Content created = catalogService.createContent(content);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Content> getContent(@PathVariable Long id) {
        return catalogService.getContent(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Content>> getAllContent() {
        return ResponseEntity.ok(catalogService.getAllContent());
    }

    @GetMapping("/batch")
    public ResponseEntity<List<Content>> getContentBatch(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(catalogService.getContentByIds(ids));
    }

    public record CreateContentRequest(String title, String type, Integer durationMinutes, String genre) {}
}
