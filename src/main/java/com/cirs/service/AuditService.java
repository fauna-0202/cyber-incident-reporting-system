package com.cirs.service;

import com.cirs.dto.response.AuditLogResponse;
import com.cirs.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    void log(User actor, String action, String entityType, String entityId, String details, HttpServletRequest request);
    void log(User actor, String action, String entityType, String entityId, String details, String ipAddress, String userAgent);
    Page<AuditLogResponse> getAuditLogs(Pageable pageable);
}
