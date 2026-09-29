package com.cirs.controller;

import com.cirs.dto.request.AssignInvestigatorRequest;
import com.cirs.dto.request.InvestigatorCreateRequest;
import com.cirs.dto.response.*;
import com.cirs.entity.User;
import com.cirs.enums.IncidentStatus;
import com.cirs.service.AdminService;
import com.cirs.service.AuditService;
import com.cirs.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;
    private final AuditService auditService;

    public AdminController(AdminService adminService, AuthService authService, AuditService auditService) {
        this.adminService = adminService;
        this.authService = authService;
        this.auditService = auditService;
    }

    /**
     * Search and filter all incidents across the organization.
     */
    @GetMapping("/incidents")
    public ResponseEntity<ApiResponse<Page<IncidentSummaryResponse>>> getAllIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<IncidentSummaryResponse> incidents = adminService.getAllIncidents(status, categoryId, query, pageable);
        return ResponseEntity.ok(ApiResponse.success(incidents));
    }

    /**
     * Assign an incident to a designated investigator.
     */
    @PutMapping("/incidents/{incidentNumber}/assign")
    public ResponseEntity<ApiResponse<IncidentResponse>> assignInvestigator(
            @PathVariable String incidentNumber,
            @Valid @RequestBody AssignInvestigatorRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User admin = authService.getUserByUsername(authentication.getName());
        IncidentResponse response = adminService.assignInvestigator(incidentNumber, request.getInvestigatorId(), admin, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Incident assigned successfully", response));
    }

    /**
     * System statistics and analytics for dashboard.
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * List all registered user accounts with pagination.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserProfileResponse> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * List all investigators available for assignment.
     */
    @GetMapping("/investigators")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getInvestigators() {
        List<UserProfileResponse> investigators = adminService.getAllInvestigators();
        return ResponseEntity.ok(ApiResponse.success(investigators));
    }

    /**
     * Provision a new investigator account.
     */
    @PostMapping("/investigators")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createInvestigator(
            @Valid @RequestBody InvestigatorCreateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User admin = authService.getUserByUsername(authentication.getName());
        UserProfileResponse response = adminService.createInvestigator(request, admin, httpRequest);
        return new ResponseEntity<>(ApiResponse.success("Investigator account created successfully", response), HttpStatus.CREATED);
    }

    /**
     * Enable or disable an account.
     */
    @PatchMapping("/users/{userId}/toggle-status")
    public ResponseEntity<ApiResponse<UserProfileResponse>> toggleUserStatus(
            @PathVariable Long userId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User admin = authService.getUserByUsername(authentication.getName());
        UserProfileResponse response = adminService.toggleUserStatus(userId, admin, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("User account status updated", response));
    }

    /**
     * View security audit logs with pagination.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogResponse> logs = auditService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
