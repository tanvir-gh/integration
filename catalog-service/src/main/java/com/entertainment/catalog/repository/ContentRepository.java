package com.entertainment.catalog.repository;

import com.entertainment.catalog.domain.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {
    List<Content> findByIdIn(List<Long> ids);
}
