package com.fpt.swp.repository;

import com.fpt.swp.model.SyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    Page<SyncLog> findBySourceIdOrderByCreatedAtDesc(Long sourceId, Pageable pageable);
}
