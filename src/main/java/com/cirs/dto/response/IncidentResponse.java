package com.cirs.dto.response;

import com.cirs.enums.IncidentStatus;
import com.cirs.enums.Severity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IncidentResponse {
    private Long id;
    private String incidentNumber;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;
    private IncidentStatus status;
    private Severity severity;
    private LocalDateTime incidentDate;
    private String locationIp;
    private Long reporterId;
    private String reporterName;
    private String reporterEmail;
    private Long assignedInvestigatorId;
    private String assignedInvestigatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    private List<IncidentAttachmentResponse> attachments = new ArrayList<>();
    private List<IncidentNoteResponse> notes = new ArrayList<>();

    public IncidentResponse() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public LocalDateTime getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(LocalDateTime incidentDate) {
        this.incidentDate = incidentDate;
    }

    public String getLocationIp() {
        return locationIp;
    }

    public void setLocationIp(String locationIp) {
        this.locationIp = locationIp;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReporterEmail() {
        return reporterEmail;
    }

    public void setReporterEmail(String reporterEmail) {
        this.reporterEmail = reporterEmail;
    }

    public Long getAssignedInvestigatorId() {
        return assignedInvestigatorId;
    }

    public void setAssignedInvestigatorId(Long assignedInvestigatorId) {
        this.assignedInvestigatorId = assignedInvestigatorId;
    }

    public String getAssignedInvestigatorName() {
        return assignedInvestigatorName;
    }

    public void setAssignedInvestigatorName(String assignedInvestigatorName) {
        this.assignedInvestigatorName = assignedInvestigatorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public List<IncidentAttachmentResponse> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<IncidentAttachmentResponse> attachments) {
        this.attachments = attachments;
    }

    public List<IncidentNoteResponse> getNotes() {
        return notes;
    }

    public void setNotes(List<IncidentNoteResponse> notes) {
        this.notes = notes;
    }
}
