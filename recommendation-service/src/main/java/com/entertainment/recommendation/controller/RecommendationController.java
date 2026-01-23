package com.entertainment.recommendation.controller;

import com.entertainment.recommendation.domain.ContentView;
import com.entertainment.recommendation.service.RecommendationService;
import com.entertainment.recommendation.service.RecommendationService.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations() {
        return ResponseEntity.ok(recommendationService.getRecommendations());
    }

    @GetMapping("/content-views")
    public ResponseEntity<List<ContentView>> getContentViews() {
        return ResponseEntity.ok(recommendationService.getAllContentViews());
    }
}
