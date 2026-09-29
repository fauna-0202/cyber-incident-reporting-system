package com.cirs.service;

import com.cirs.dto.request.IncidentCreateRequest;
import com.cirs.dto.request.NoteCreateRequest;
import com.cirs.dto.request.StatusUpdateRequest;
import com.cirs.dto.response.IncidentAttachmentResponse;
import com.cirs.dto.response.IncidentResponse;
import com.cirs.dto.response.IncidentSummaryResponse;
import com.cirs.entity.IncidentAttachment;
import com.cirs.entity.User;
import com.cirs.enums.IncidentStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IncidentService {
    IncidentResponse createIncident(IncidentCreateRequest request, List<MultipartFile> evidenceFiles, User reporter, HttpServletRequest httpRequest);
    Page<IncidentSummaryResponse> getMyIncidents(User reporter, Pageable pageable);
    IncidentResponse getIncidentDetails(String incidentNumber, User currentUser);
    Page<IncidentSummaryResponse> getAssignedIncidents(User investigator, Pageable pageable);
    IncidentResponse updateStatus(String incidentNumber, StatusUpdateRequest request, User actor, HttpServletRequest httpRequest);
    IncidentResponse addNote(String incidentNumber, NoteCreateRequest request, User actor, HttpServletRequest httpRequest);
    IncidentResponse respondToInformationRequest(String incidentNumber, String responseText, List<MultipartFile> files, User reporter, HttpServletRequest httpRequest);
    IncidentAttachment getAttachmentForDownload(String incidentNumber, Long attachmentId, User currentUser);
}
