package com.cirs.repository;

import com.cirs.entity.AuditLog;
import com.cirs.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<AuditLog> findTop50ByOrderByCreatedAtDesc();
    List<AuditLog> findByActorOrderByCreatedAtDesc(User actor);
}
