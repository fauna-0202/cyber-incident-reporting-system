package com.cirs.service.impl;

import com.cirs.dto.response.AuditLogResponse;
import com.cirs.entity.AuditLog;
import com.cirs.entity.User;
import com.cirs.repository.AuditLogRepository;
import com.cirs.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void log(User actor, String action, String entityType, String entityId, String details, HttpServletRequest request) {
        String ipAddress = "127.0.0.1";
        String userAgent = "Unknown";
        if (request != null) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isEmpty()) {
                ipAddress = xfHeader.split(",")[0].trim();
            } else {
                ipAddress = request.getRemoteAddr();
            }
            userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 255) {
                userAgent = userAgent.substring(0, 255);
            }
        }
        log(actor, action, entityType, entityId, details, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public void log(User actor, String action, String entityType, String entityId, String details, String ipAddress, String userAgent) {
        AuditLog auditLog = new AuditLog(actor, action, entityType, entityId, ipAddress, userAgent, details);
        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        String actorUsername = log.getActor() != null ? log.getActor().getUsername() : "SYSTEM";
        String actorRole = log.getActor() != null ? log.getActor().getRole().name() : "SYSTEM";
        return new AuditLogResponse(
                log.getId(),
                actorUsername,
                actorRole,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getIpAddress(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
