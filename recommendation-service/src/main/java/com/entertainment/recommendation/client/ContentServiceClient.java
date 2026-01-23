package com.entertainment.recommendation.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ContentServiceClient {

    private final RestClient restClient;

    public ContentServiceClient(@Value("${content-service.url}") String contentServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(contentServiceUrl)
                .build();
    }

    public Optional<ContentResponse> getContent(Long id) {
        try {
            ContentResponse response = restClient.get()
                    .uri("/api/content/{id}", id)
                    .retrieve()
                    .body(ContentResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Failed to fetch content with id: {}", id, e);
            return Optional.empty();
        }
    }

    public List<ContentResponse> getAllContent() {
        try {
            ContentResponse[] response = restClient.get()
                    .uri("/api/content")
                    .retrieve()
                    .body(ContentResponse[].class);
            return response != null ? List.of(response) : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch all content", e);
            return List.of();
        }
    }

    public record ContentResponse(Long id, String title, String type, String createdAt) {}
}
