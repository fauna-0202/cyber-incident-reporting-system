package com.cirs.service.impl;

import com.cirs.dto.request.IncidentCreateRequest;
import com.cirs.dto.request.NoteCreateRequest;
import com.cirs.dto.request.StatusUpdateRequest;
import com.cirs.dto.response.IncidentAttachmentResponse;
import com.cirs.dto.response.IncidentNoteResponse;
import com.cirs.dto.response.IncidentResponse;
import com.cirs.dto.response.IncidentSummaryResponse;
import com.cirs.entity.*;
import com.cirs.enums.IncidentStatus;
import com.cirs.enums.NoteType;
import com.cirs.enums.Role;
import com.cirs.exception.BadRequestException;
import com.cirs.exception.InvalidStatusTransitionException;
import com.cirs.exception.ResourceNotFoundException;
import com.cirs.exception.UnauthorizedAccessException;
import com.cirs.repository.IncidentAttachmentRepository;
import com.cirs.repository.IncidentCategoryRepository;
import com.cirs.repository.IncidentNoteRepository;
import com.cirs.repository.IncidentRepository;
import com.cirs.service.AuditService;
import com.cirs.service.FileStorageService;
import com.cirs.service.IncidentService;
import com.cirs.util.IncidentNumberGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final IncidentAttachmentRepository attachmentRepository;
    private final IncidentNoteRepository noteRepository;
    private final IncidentNumberGenerator numberGenerator;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    public IncidentServiceImpl(IncidentRepository incidentRepository,
                               IncidentCategoryRepository categoryRepository,
                               IncidentAttachmentRepository attachmentRepository,
                               IncidentNoteRepository noteRepository,
                               IncidentNumberGenerator numberGenerator,
                               FileStorageService fileStorageService,
                               AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.categoryRepository = categoryRepository;
        this.attachmentRepository = attachmentRepository;
        this.noteRepository = noteRepository;
        this.numberGenerator = numberGenerator;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public IncidentResponse createIncident(IncidentCreateRequest request, List<MultipartFile> evidenceFiles,
                                           User reporter, HttpServletRequest httpRequest) {
        IncidentCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident category not found with id: " + request.getCategoryId()));

        if (!category.isActive()) {
            throw new BadRequestException("The selected category is currently inactive. Please choose an active category.");
        }

        String incidentNumber = numberGenerator.generateUniqueIncidentNumber();

        Incident incident = new Incident(
                incidentNumber,
                reporter,
                category,
                request.getTitle().trim(),
                request.getDescription().trim(),
                request.getIncidentDate(),
                request.getLocationIp(),
                request.getSeverity()
        );

        Incident savedIncident = incidentRepository.save(incident);

        // Store evidence files if provided
        if (evidenceFiles != null && !evidenceFiles.isEmpty()) {
            for (MultipartFile file : evidenceFiles) {
                if (file != null && !file.isEmpty()) {
                    IncidentAttachment attachment = fileStorageService.storeEvidence(file, savedIncident, reporter);
                    savedIncident.getAttachments().add(attachment);
                    auditService.log(reporter, "UPLOAD_EVIDENCE", "ATTACHMENT", attachment.getId().toString(),
                            "Evidence uploaded for incident " + incidentNumber + ": " + attachment.getOriginalFilename(), httpRequest);
                }
            }
        }

        // Add initial system note
        IncidentNote initialNote = new IncidentNote(
                savedIncident,
                reporter,
                NoteType.PUBLIC_UPDATE,
                "Incident report submitted by " + reporter.getFullName() + ". Status: " + savedIncident.getStatus()
        );
        noteRepository.save(initialNote);
        savedIncident.getNotes().add(initialNote);

        // Audit log
        auditService.log(reporter, "SUBMIT_INCIDENT", "INCIDENT", incidentNumber,
                "Reported cyber incident: " + incident.getTitle(), httpRequest);

        return mapToDetailedResponse(savedIncident, reporter);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> getMyIncidents(User reporter, Pageable pageable) {
        return incidentRepository.findByReporterOrderByCreatedAtDesc(reporter, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getIncidentDetails(String incidentNumber, User currentUser) {
        Incident incident = findByIncidentNumberOrThrow(incidentNumber);
        validateIncidentAccess(incident, currentUser);
        return mapToDetailedResponse(incident, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> getAssignedIncidents(User investigator, Pageable pageable) {
        return incidentRepository.findByAssignedInvestigatorOrderByCreatedAtDesc(investigator, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional
    public IncidentResponse updateStatus(String incidentNumber, StatusUpdateRequest request, User actor, HttpServletRequest httpRequest) {
        Incident incident = findByIncidentNumberOrThrow(incidentNumber);
        validateStatusUpdatePermissions(incident, actor, request.getStatus());

        IncidentStatus oldStatus = incident.getStatus();
        IncidentStatus newStatus = request.getStatus();

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition incident status from " + oldStatus + " to " + newStatus + 
                    ". Allowed transitions from " + oldStatus + " are defined in the incident workflow.");
        }

        incident.setStatus(newStatus);
        if (newStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(LocalDateTime.now());
        }

        Incident updatedIncident = incidentRepository.save(incident);

        // Record status change note
        NoteType noteType = (newStatus == IncidentStatus.NEED_MORE_INFORMATION) ? NoteType.INFO_REQUEST : NoteType.PUBLIC_UPDATE;
        IncidentNote statusNote = new IncidentNote(
                updatedIncident,
                actor,
                noteType,
                "Status changed from " + oldStatus + " to " + newStatus + ". Reason / Details: " + request.getComment().trim()
        );
        noteRepository.save(statusNote);
        updatedIncident.getNotes().add(statusNote);

        auditService.log(actor, "STATUS_CHANGE", "INCIDENT", incidentNumber,
                "Transitioned status from " + oldStatus + " to " + newStatus + ". Comment: " + request.getComment().trim(), httpRequest);

        return mapToDetailedResponse(updatedIncident, actor);
    }

    @Override
    @Transactional
    public IncidentResponse addNote(String incidentNumber, NoteCreateRequest request, User actor, HttpServletRequest httpRequest) {
        Incident incident = findByIncidentNumberOrThrow(incidentNumber);
        validateIncidentAccess(incident, actor);

        // Security check: Only Investigators and Admins can create INTERNAL_NOTE
        if (request.getNoteType() == NoteType.INTERNAL_NOTE && actor.getRole() == Role.ROLE_USER) {
            throw new UnauthorizedAccessException("Citizens/Users are not authorized to create internal investigator notes.");
        }

        // If investigator adds INFO_REQUEST, check if status should move to NEED_MORE_INFORMATION
        if (request.getNoteType() == NoteType.INFO_REQUEST && incident.getStatus() == IncidentStatus.INVESTIGATING) {
            incident.setStatus(IncidentStatus.NEED_MORE_INFORMATION);
            incidentRepository.save(incident);
        }

        IncidentNote note = new IncidentNote(
                incident,
                actor,
                request.getNoteType(),
                request.getContent().trim()
        );
        noteRepository.save(note);
        incident.getNotes().add(note);

        auditService.log(actor, "ADD_NOTE", "INCIDENT_NOTE", incidentNumber,
                "Added note of type " + request.getNoteType() + " to incident " + incidentNumber, httpRequest);

        return mapToDetailedResponse(incident, actor);
    }

    @Override
    @Transactional
    public IncidentResponse respondToInformationRequest(String incidentNumber, String responseText,
                                                        List<MultipartFile> files, User reporter,
                                                        HttpServletRequest httpRequest) {
        Incident incident = findByIncidentNumberOrThrow(incidentNumber);

        // Only reporting user can respond to information requests
        if (!incident.getReporter().getId().equals(reporter.getId())) {
            throw new UnauthorizedAccessException("Only the original reporter can respond to information requests for this incident.");
        }

        if (incident.getStatus() != IncidentStatus.NEED_MORE_INFORMATION) {
            throw new BadRequestException("This incident is currently in status " + incident.getStatus() + 
                    " and does not have an active information request.");
        }

        // Store additional files if provided
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    IncidentAttachment attachment = fileStorageService.storeEvidence(file, incident, reporter);
                    incident.getAttachments().add(attachment);
                    auditService.log(reporter, "UPLOAD_EVIDENCE", "ATTACHMENT", attachment.getId().toString(),
                            "Additional evidence uploaded for incident " + incidentNumber, httpRequest);
                }
            }
        }

        // Add user response note
        IncidentNote responseNote = new IncidentNote(
                incident,
                reporter,
                NoteType.USER_RESPONSE,
                "Citizen Response: " + responseText.trim()
        );
        noteRepository.save(responseNote);
        incident.getNotes().add(responseNote);

        // Transition back to INVESTIGATING
        incident.setStatus(IncidentStatus.INVESTIGATING);
        Incident updatedIncident = incidentRepository.save(incident);

        auditService.log(reporter, "RESPOND_INFO_REQUEST", "INCIDENT", incidentNumber,
                "Citizen responded to information request; status returned to INVESTIGATING", httpRequest);

        return mapToDetailedResponse(updatedIncident, reporter);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentAttachment getAttachmentForDownload(String incidentNumber, Long attachmentId, User currentUser) {
        Incident incident = findByIncidentNumberOrThrow(incidentNumber);
        validateIncidentAccess(incident, currentUser);

        return attachmentRepository.findByIdAndIncident(attachmentId, incident)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment with id " + attachmentId + 
                        " does not belong to incident " + incidentNumber));
    }

    private Incident findByIncidentNumberOrThrow(String incidentNumber) {
        return incidentRepository.findByIncidentNumber(incidentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with incident number: " + incidentNumber));
    }

    /**
     * Enforces Ownership and RBAC:
     * - ADMIN: access all
     * - INVESTIGATOR: only assigned incidents
     * - USER: only own incidents
     */
    private void validateIncidentAccess(Incident incident, User user) {
        if (user.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (user.getRole() == Role.ROLE_INVESTIGATOR) {
            if (incident.getAssignedInvestigator() == null || !incident.getAssignedInvestigator().getId().equals(user.getId())) {
                throw new UnauthorizedAccessException("Access denied: You are not assigned to investigate incident " + incident.getIncidentNumber());
            }
            return;
        }
        if (user.getRole() == Role.ROLE_USER) {
            if (!incident.getReporter().getId().equals(user.getId())) {
                throw new UnauthorizedAccessException("Access denied: You are not authorized to view incident " + incident.getIncidentNumber());
            }
            return;
        }
        throw new UnauthorizedAccessException("Access denied for role: " + user.getRole());
    }

    private void validateStatusUpdatePermissions(Incident incident, User user, IncidentStatus targetStatus) {
        if (user.getRole() == Role.ROLE_ADMIN) {
            return; // Admin can update any status
        }
        if (user.getRole() == Role.ROLE_INVESTIGATOR) {
            if (incident.getAssignedInvestigator() == null || !incident.getAssignedInvestigator().getId().equals(user.getId())) {
                throw new UnauthorizedAccessException("You are not assigned to investigate incident " + incident.getIncidentNumber());
            }
            // Investigators cannot close an incident (only Admin or workflow auto-close)
            if (targetStatus == IncidentStatus.CLOSED) {
                throw new UnauthorizedAccessException("Investigators cannot close incidents. Please mark as RESOLVED for administrative closure.");
            }
            return;
        }
        throw new UnauthorizedAccessException("Users/Citizens cannot directly change incident status. Please respond using the reply form.");
    }

    private IncidentSummaryResponse mapToSummaryResponse(Incident incident) {
        IncidentSummaryResponse summary = new IncidentSummaryResponse();
        summary.setId(incident.getId());
        summary.setIncidentNumber(incident.getIncidentNumber());
        summary.setTitle(incident.getTitle());
        summary.setCategoryName(incident.getCategory().getName());
        summary.setStatus(incident.getStatus());
        summary.setSeverity(incident.getSeverity());
        summary.setReporterName(incident.getReporter().getFullName());
        summary.setInvestigatorName(incident.getAssignedInvestigator() != null ? incident.getAssignedInvestigator().getFullName() : "Unassigned");
        summary.setAttachmentCount(incident.getAttachments() != null ? incident.getAttachments().size() : 0);
        summary.setNoteCount(incident.getNotes() != null ? incident.getNotes().size() : 0);
        summary.setIncidentDate(incident.getIncidentDate());
        summary.setCreatedAt(incident.getCreatedAt());
        summary.setResolvedAt(incident.getResolvedAt());
        return summary;
    }

    private IncidentResponse mapToDetailedResponse(Incident incident, User currentUser) {
        IncidentResponse response = new IncidentResponse();
        response.setId(incident.getId());
        response.setIncidentNumber(incident.getIncidentNumber());
        response.setTitle(incident.getTitle());
        response.setDescription(incident.getDescription());
        response.setCategoryId(incident.getCategory().getId());
        response.setCategoryName(incident.getCategory().getName());
        response.setStatus(incident.getStatus());
        response.setSeverity(incident.getSeverity());
        response.setIncidentDate(incident.getIncidentDate());
        response.setLocationIp(incident.getLocationIp());
        response.setReporterId(incident.getReporter().getId());
        response.setReporterName(incident.getReporter().getFullName());
        response.setReporterEmail(incident.getReporter().getEmail());
        if (incident.getAssignedInvestigator() != null) {
            response.setAssignedInvestigatorId(incident.getAssignedInvestigator().getId());
            response.setAssignedInvestigatorName(incident.getAssignedInvestigator().getFullName());
        }
        response.setCreatedAt(incident.getCreatedAt());
        response.setUpdatedAt(incident.getUpdatedAt());
        response.setResolvedAt(incident.getResolvedAt());

        // Map attachments
        if (incident.getAttachments() != null) {
            List<IncidentAttachmentResponse> attachments = incident.getAttachments().stream()
                    .map(att -> new IncidentAttachmentResponse(
                            att.getId(),
                            att.getOriginalFilename(),
                            att.getFileSizeBytes(),
                            att.getMimeType(),
                            att.getSha256Hash(),
                            att.getUploadedBy().getFullName(),
                            att.getUploadedAt()
                    )).collect(Collectors.toList());
            response.setAttachments(attachments);
        }

        // Map notes: If user is ROLE_USER, filter out INTERNAL_NOTE
        if (incident.getNotes() != null) {
            boolean isUser = (currentUser.getRole() == Role.ROLE_USER);
            List<IncidentNoteResponse> notes = incident.getNotes().stream()
                    .filter(note -> !isUser || note.getNoteType() != NoteType.INTERNAL_NOTE)
                    .map(note -> new IncidentNoteResponse(
                            note.getId(),
                            note.getAuthor().getFullName(),
                            note.getAuthor().getRole(),
                            note.getNoteType(),
                            note.getContent(),
                            note.getCreatedAt()
                    )).collect(Collectors.toList());
            response.setNotes(notes);
        }

        return response;
    }
}
