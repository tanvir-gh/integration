package com.entertainment.content.service;

import com.entertainment.content.domain.Content;
import com.entertainment.content.kafka.ContentEventProducer;
import com.entertainment.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentEventProducer contentEventProducer;

    @Transactional
    public Content createContent(Content content) {
        Content savedContent = contentRepository.save(content);
        log.info("Created content with id: {}", savedContent.getId());
        contentEventProducer.publishContentCreated(savedContent);
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
}
