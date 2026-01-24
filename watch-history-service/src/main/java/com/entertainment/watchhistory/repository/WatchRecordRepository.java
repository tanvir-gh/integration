package com.entertainment.watchhistory.repository;

import com.entertainment.watchhistory.domain.WatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchRecordRepository extends JpaRepository<WatchRecord, Long> {
    List<WatchRecord> findByVisitorIdOrderByWatchedAtDesc(String visitorId);
}
