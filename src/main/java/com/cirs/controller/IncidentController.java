package com.cirs.controller;

import com.cirs.dto.request.IncidentCreateRequest;
import com.cirs.dto.request.NoteCreateRequest;
import com.cirs.dto.request.StatusUpdateRequest;
import com.cirs.dto.response.ApiResponse;
import com.cirs.dto.response.IncidentResponse;
import com.cirs.dto.response.IncidentSummaryResponse;
import com.cirs.entity.IncidentAttachment;
import com.cirs.entity.User;
import com.cirs.service.AuditService;
import com.cirs.service.AuthService;
import com.cirs.service.FileStorageService;
import com.cirs.service.IncidentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final AuthService authService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    public IncidentController(IncidentService incidentService,
                              AuthService authService,
                              FileStorageService fileStorageService,
                              AuditService auditService) {
        this.incidentService = incidentService;
        this.authService = authService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
    }

    /**
     * Submit a new cyber incident report with optional supporting evidence files.
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<IncidentResponse>> createIncident(
            @Valid @ModelAttribute IncidentCreateRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getUserByUsername(authentication.getName());
        IncidentResponse response = incidentService.createIncident(request, files, currentUser, httpRequest);
        return new ResponseEntity<>(ApiResponse.success("Incident reported successfully with ID: " + response.getIncidentNumber(), response), HttpStatus.CREATED);
    }

    /**
     * View logged-in user's submitted incidents with pagination.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<IncidentSummaryResponse>>> getMyIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        User currentUser = authService.getUserByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<IncidentSummaryResponse> incidents = incidentService.getMyIncidents(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(incidents));
    }

    /**
     * View investigator's assigned incidents.
     */
    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('INVESTIGATOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<IncidentSummaryResponse>>> getAssignedIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        User investigator = authService.getUserByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<IncidentSummaryResponse> incidents = incidentService.getAssignedIncidents(investigator, pageable);
        return ResponseEntity.ok(ApiResponse.success(incidents));
    }

    /**
     * View full incident details and timeline.
     * Enforces ownership / assignment / role authorization.
     */
    @GetMapping("/{incidentNumber}")
    public ResponseEntity<ApiResponse<IncidentResponse>> getIncidentDetails(
            @PathVariable String incidentNumber,
            Authentication authentication) {

        User currentUser = authService.getUserByUsername(authentication.getName());
        IncidentResponse incident = incidentService.getIncidentDetails(incidentNumber, currentUser);
        return ResponseEntity.ok(ApiResponse.success(incident));
    }

    /**
     * Update incident status (Investigator or Admin).
     */
    @PatchMapping("/{incidentNumber}/status")
    @PreAuthorize("hasAnyRole('INVESTIGATOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<IncidentResponse>> updateStatus(
            @PathVariable String incidentNumber,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getUserByUsername(authentication.getName());
        IncidentResponse updated = incidentService.updateStatus(incidentNumber, request, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Status updated to " + updated.getStatus(), updated));
    }

    /**
     * Add note / public update / info request / internal note.
     */
    @PostMapping("/{incidentNumber}/notes")
    public ResponseEntity<ApiResponse<IncidentResponse>> addNote(
            @PathVariable String incidentNumber,
            @Valid @RequestBody NoteCreateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getUserByUsername(authentication.getName());
        IncidentResponse response = incidentService.addNote(incidentNumber, request, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Note added successfully", response));
    }

    /**
     * Citizen response to NEED_MORE_INFORMATION request with text and optional files.
     */
    @PostMapping(value = "/{incidentNumber}/respond", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<IncidentResponse>> respondToInfoRequest(
            @PathVariable String incidentNumber,
            @RequestParam("response") String responseText,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getUserByUsername(authentication.getName());
        IncidentResponse response = incidentService.respondToInformationRequest(incidentNumber, responseText, files, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Information submitted and status resumed to INVESTIGATING", response));
    }

    /**
     * Securely download evidence file after ownership and authorization verification.
     */
    @GetMapping("/{incidentNumber}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable String incidentNumber,
            @PathVariable Long attachmentId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User currentUser = authService.getUserByUsername(authentication.getName());
        IncidentAttachment attachment = incidentService.getAttachmentForDownload(incidentNumber, attachmentId, currentUser);
        Resource resource = fileStorageService.loadFileAsResource(attachment);

        auditService.log(currentUser, "DOWNLOAD_EVIDENCE", "ATTACHMENT", attachmentId.toString(),
                "Downloaded evidence " + attachment.getOriginalFilename() + " for incident " + incidentNumber, httpRequest);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getOriginalFilename() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
