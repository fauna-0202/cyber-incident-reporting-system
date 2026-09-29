package com.cirs.dto.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardStatsResponse {
    private long totalIncidents;
    private long submittedCount;
    private long underReviewCount;
    private long assignedCount;
    private long investigatingCount;
    private long needMoreInfoCount;
    private long resolvedCount;
    private long closedCount;

    private long totalUsers;
    private long totalInvestigators;

    private Map<String, Long> categoryDistribution = new HashMap<>();
    private List<IncidentSummaryResponse> recentIncidents;

    public DashboardStatsResponse() {}

    public long getTotalIncidents() {
        return totalIncidents;
    }

    public void setTotalIncidents(long totalIncidents) {
        this.totalIncidents = totalIncidents;
    }

    public long getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(long submittedCount) {
        this.submittedCount = submittedCount;
    }

    public long getUnderReviewCount() {
        return underReviewCount;
    }

    public void setUnderReviewCount(long underReviewCount) {
        this.underReviewCount = underReviewCount;
    }

    public long getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(long assignedCount) {
        this.assignedCount = assignedCount;
    }

    public long getInvestigatingCount() {
        return investigatingCount;
    }

    public void setInvestigatingCount(long investigatingCount) {
        this.investigatingCount = investigatingCount;
    }

    public long getNeedMoreInfoCount() {
        return needMoreInfoCount;
    }

    public void setNeedMoreInfoCount(long needMoreInfoCount) {
        this.needMoreInfoCount = needMoreInfoCount;
    }

    public long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }

    public long getClosedCount() {
        return closedCount;
    }

    public void setClosedCount(long closedCount) {
        this.closedCount = closedCount;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalInvestigators() {
        return totalInvestigators;
    }

    public void setTotalInvestigators(long totalInvestigators) {
        this.totalInvestigators = totalInvestigators;
    }

    public Map<String, Long> getCategoryDistribution() {
        return categoryDistribution;
    }

    public void setCategoryDistribution(Map<String, Long> categoryDistribution) {
        this.categoryDistribution = categoryDistribution;
    }

    public List<IncidentSummaryResponse> getRecentIncidents() {
        return recentIncidents;
    }

    public void setRecentIncidents(List<IncidentSummaryResponse> recentIncidents) {
        this.recentIncidents = recentIncidents;
    }
}
