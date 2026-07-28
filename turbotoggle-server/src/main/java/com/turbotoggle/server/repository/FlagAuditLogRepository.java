package com.turbotoggle.server.repository;

import com.turbotoggle.server.domain.entity.FlagAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlagAuditLogRepository extends JpaRepository<FlagAuditLog, Long> {
    List<FlagAuditLog> findByFlagKeyOrderByTimestampDesc(String flagKey);
}