package com.buffturf.buffturf_backend.repository;

import com.buffturf.buffturf_backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
    List<AuditLog> findByTurfIdOrderByCreatedAtDesc(Long turfId);
}
