package com.entertainment.recommendation.service;

import com.entertainment.recommendation.client.ContentServiceClient;
import com.entertainment.recommendation.client.ContentServiceClient.ContentResponse;
import com.entertainment.recommendation.domain.ContentView;
import com.entertainment.recommendation.repository.ContentViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ContentViewRepository contentViewRepository;
    private final ContentServiceClient contentServiceClient;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations() {
        List<ContentView> recentViews = contentViewRepository.findTop10ByOrderByViewedAtDesc();

        return recentViews.stream()
                .map(view -> {
                    ContentResponse content = contentServiceClient.getContent(view.getContentId())
                            .orElse(null);
                    return new RecommendationResponse(
                            view.getContentId(),
                            view.getContentTitle(),
                            view.getContentType(),
                            content != null ? content.createdAt() : null,
                            view.getViewedAt().toString()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContentView> getAllContentViews() {
        return contentViewRepository.findAll();
    }

    public record RecommendationResponse(
            Long contentId,
            String title,
            String type,
            String contentCreatedAt,
            String viewedAt
    ) {}
}
