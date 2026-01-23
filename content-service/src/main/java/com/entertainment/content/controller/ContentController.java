package com.entertainment.content.controller;

import com.entertainment.content.domain.Content;
import com.entertainment.content.domain.ContentType;
import com.entertainment.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping
    public ResponseEntity<Content> createContent(@RequestBody CreateContentRequest request) {
        Content content = Content.builder()
                .title(request.title())
                .type(ContentType.valueOf(request.type()))
                .build();
        Content created = contentService.createContent(content);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Content> getContent(@PathVariable Long id) {
        return contentService.getContent(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Content>> getAllContent() {
        return ResponseEntity.ok(contentService.getAllContent());
    }

    public record CreateContentRequest(String title, String type) {}
}
