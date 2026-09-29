package com.cirs.entity;

import com.cirs.enums.IncidentStatus;
import com.cirs.enums.Severity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents", indexes = {
    @Index(name = "idx_incidents_number", columnList = "incident_number"),
    @Index(name = "idx_incidents_status", columnList = "status"),
    @Index(name = "idx_incidents_reporter", columnList = "reporter_id"),
    @Index(name = "idx_incidents_investigator", columnList = "assigned_investigator_id")
})
public class Incident extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_number", nullable = false, unique = true, length = 30)
    private String incidentNumber; // Format: CIR-YYYYMMDD-XXXXX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private IncidentCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "description", nullable = false, columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "incident_date", nullable = false)
    private LocalDateTime incidentDate;

    @Column(name = "location_ip", length = 100)
    private String locationIp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity = Severity.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_investigator_id")
    private User assignedInvestigator;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IncidentAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<IncidentNote> notes = new ArrayList<>();

    public Incident() {}

    public Incident(String incidentNumber, User reporter, IncidentCategory category, String title,
                    String description, LocalDateTime incidentDate, String locationIp, Severity severity) {
        this.incidentNumber = incidentNumber;
        this.reporter = reporter;
        this.category = category;
        this.title = title;
        this.description = description;
        this.incidentDate = incidentDate;
        this.locationIp = locationIp;
        this.severity = severity != null ? severity : Severity.MEDIUM;
        this.status = IncidentStatus.SUBMITTED;
    }

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

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
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

    public User getAssignedInvestigator() {
        return assignedInvestigator;
    }

    public void setAssignedInvestigator(User assignedInvestigator) {
        this.assignedInvestigator = assignedInvestigator;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public List<IncidentAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<IncidentAttachment> attachments) {
        this.attachments = attachments;
    }

    public List<IncidentNote> getNotes() {
        return notes;
    }

    public void setNotes(List<IncidentNote> notes) {
        this.notes = notes;
    }

    public void addAttachment(IncidentAttachment attachment) {
        attachments.add(attachment);
        attachment.setIncident(this);
    }

    public void addNote(IncidentNote note) {
        notes.add(note);
        note.setIncident(this);
    }
}
