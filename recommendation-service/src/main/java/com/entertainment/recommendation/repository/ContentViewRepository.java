package com.entertainment.recommendation.repository;

import com.entertainment.recommendation.domain.ContentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentViewRepository extends JpaRepository<ContentView, Long> {
    List<ContentView> findTop10ByOrderByViewedAtDesc();
}
