package com.entertainment.watchhistory.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CatalogServiceClient {

    private final RestClient restClient;

    public CatalogServiceClient(@Value("${catalog-service.url}") String catalogServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(catalogServiceUrl)
                .build();
    }

    public List<ContentResponse> getContentBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        try {
            String idsParam = ids.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            List<ContentResponse> response = restClient.get()
                    .uri("/api/catalog/batch?ids={ids}", idsParam)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return response != null ? response : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch content batch for ids: {}", ids, e);
            return List.of();
        }
    }

    public record ContentResponse(
            Long id,
            String title,
            String type,
            Integer durationMinutes,
            String genre,
            String publishedAt
    ) {}
}
