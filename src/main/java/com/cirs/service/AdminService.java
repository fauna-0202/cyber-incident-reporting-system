package com.cirs.service;

import com.cirs.dto.request.InvestigatorCreateRequest;
import com.cirs.dto.response.DashboardStatsResponse;
import com.cirs.dto.response.IncidentResponse;
import com.cirs.dto.response.IncidentSummaryResponse;
import com.cirs.dto.response.UserProfileResponse;
import com.cirs.entity.User;
import com.cirs.enums.IncidentStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminService {
    Page<IncidentSummaryResponse> getAllIncidents(IncidentStatus status, Long categoryId, String query, Pageable pageable);
    IncidentResponse assignInvestigator(String incidentNumber, Long investigatorId, User admin, HttpServletRequest httpRequest);
    DashboardStatsResponse getDashboardStats();
    Page<UserProfileResponse> getAllUsers(Pageable pageable);
    List<UserProfileResponse> getAllInvestigators();
    UserProfileResponse createInvestigator(InvestigatorCreateRequest request, User admin, HttpServletRequest httpRequest);
    UserProfileResponse toggleUserStatus(Long userId, User admin, HttpServletRequest httpRequest);
}
