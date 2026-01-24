package com.entertainment.catalog.service;

import com.entertainment.catalog.domain.Content;
import com.entertainment.catalog.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final ContentRepository contentRepository;

    @Transactional
    public Content createContent(Content content) {
        Content savedContent = contentRepository.save(content);
        log.info("Created content with id: {}", savedContent.getId());
        return savedContent;
    }

    @Transactional(readOnly = true)
    public Optional<Content> getContent(Long id) {
        return contentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Content> getAllContent() {
        return contentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Content> getContentByIds(List<Long> ids) {
        return contentRepository.findByIdIn(ids);
    }
}
