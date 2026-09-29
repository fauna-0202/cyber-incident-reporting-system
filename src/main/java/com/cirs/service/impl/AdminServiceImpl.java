package com.cirs.service.impl;

import com.cirs.dto.request.InvestigatorCreateRequest;
import com.cirs.dto.response.*;
import com.cirs.entity.Incident;
import com.cirs.entity.IncidentCategory;
import com.cirs.entity.IncidentNote;
import com.cirs.entity.User;
import com.cirs.enums.IncidentStatus;
import com.cirs.enums.NoteType;
import com.cirs.enums.Role;
import com.cirs.exception.BadRequestException;
import com.cirs.exception.ResourceNotFoundException;
import com.cirs.repository.IncidentCategoryRepository;
import com.cirs.repository.IncidentNoteRepository;
import com.cirs.repository.IncidentRepository;
import com.cirs.repository.UserRepository;
import com.cirs.service.AdminService;
import com.cirs.service.AuditService;
import com.cirs.service.IncidentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final IncidentRepository incidentRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final IncidentNoteRepository noteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final IncidentService incidentService;

    public AdminServiceImpl(IncidentRepository incidentRepository,
                            IncidentCategoryRepository categoryRepository,
                            UserRepository userRepository,
                            IncidentNoteRepository noteRepository,
                            PasswordEncoder passwordEncoder,
                            AuditService auditService,
                            IncidentService incidentService) {
        this.incidentRepository = incidentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.incidentService = incidentService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> getAllIncidents(IncidentStatus status, Long categoryId, String query, Pageable pageable) {
        String searchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return incidentRepository.searchIncidents(status, categoryId, searchQuery, pageable)
                .map(this::mapToSummary);
    }

    @Override
    @Transactional
    public IncidentResponse assignInvestigator(String incidentNumber, Long investigatorId, User admin, HttpServletRequest httpRequest) {
        Incident incident = incidentRepository.findByIncidentNumber(incidentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with number: " + incidentNumber));

        User investigator = userRepository.findById(investigatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Investigator not found with id: " + investigatorId));

        if (investigator.getRole() != Role.ROLE_INVESTIGATOR) {
            throw new BadRequestException("User " + investigator.getUsername() + " does not have the INVESTIGATOR role.");
        }

        if (!investigator.isActive()) {
            throw new BadRequestException("Cannot assign an inactive investigator to this incident.");
        }

        incident.setAssignedInvestigator(investigator);
        if (incident.getStatus() == IncidentStatus.SUBMITTED || incident.getStatus() == IncidentStatus.UNDER_REVIEW) {
            incident.setStatus(IncidentStatus.ASSIGNED);
        }

        Incident saved = incidentRepository.save(incident);

        IncidentNote assignNote = new IncidentNote(
                saved,
                admin,
                NoteType.PUBLIC_UPDATE,
                "Incident officially assigned to Investigator: " + investigator.getFullName() + " (" + investigator.getUsername() + ")"
        );
        noteRepository.save(assignNote);
        saved.getNotes().add(assignNote);

        auditService.log(admin, "ASSIGN_INVESTIGATOR", "INCIDENT", incidentNumber,
                "Assigned incident " + incidentNumber + " to investigator " + investigator.getUsername(), httpRequest);

        return incidentService.getIncidentDetails(incidentNumber, admin);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();
        stats.setTotalIncidents(incidentRepository.count());
        stats.setSubmittedCount(incidentRepository.countByStatus(IncidentStatus.SUBMITTED));
        stats.setUnderReviewCount(incidentRepository.countByStatus(IncidentStatus.UNDER_REVIEW));
        stats.setAssignedCount(incidentRepository.countByStatus(IncidentStatus.ASSIGNED));
        stats.setInvestigatingCount(incidentRepository.countByStatus(IncidentStatus.INVESTIGATING));
        stats.setNeedMoreInfoCount(incidentRepository.countByStatus(IncidentStatus.NEED_MORE_INFORMATION));
        stats.setResolvedCount(incidentRepository.countByStatus(IncidentStatus.RESOLVED));
        stats.setClosedCount(incidentRepository.countByStatus(IncidentStatus.CLOSED));

        stats.setTotalUsers(userRepository.findByRole(Role.ROLE_USER).size());
        stats.setTotalInvestigators(userRepository.findByRole(Role.ROLE_INVESTIGATOR).size());

        // Category distribution
        Map<String, Long> categoryMap = new HashMap<>();
        List<IncidentCategory> categories = categoryRepository.findAll();
        for (IncidentCategory cat : categories) {
            long count = incidentRepository.countByCategory(cat);
            categoryMap.put(cat.getName(), count);
        }
        stats.setCategoryDistribution(categoryMap);

        // Recent 5 incidents
        Page<Incident> recentPage = incidentRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<IncidentSummaryResponse> recentList = recentPage.getContent().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
        stats.setRecentIncidents(recentList);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToUserProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllInvestigators() {
        return userRepository.findByRoleOrderByFullNameAsc(Role.ROLE_INVESTIGATOR).stream()
                .filter(User::isActive)
                .map(this::mapToUserProfile)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserProfileResponse createInvestigator(InvestigatorCreateRequest request, User admin, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("Email is already registered");
        }

        User investigator = new User(
                request.getUsername().trim(),
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null,
                Role.ROLE_INVESTIGATOR
        );

        User saved = userRepository.save(investigator);

        auditService.log(admin, "CREATE_INVESTIGATOR", "USER", saved.getId().toString(),
                "Created new investigator account: " + saved.getUsername(), httpRequest);

        return mapToUserProfile(saved);
    }

    @Override
    @Transactional
    public UserProfileResponse toggleUserStatus(Long userId, User admin, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getId().equals(admin.getId())) {
            throw new BadRequestException("Security safeguard: You cannot disable your own administrator account.");
        }

        user.setActive(!user.isActive());
        User updated = userRepository.save(user);

        auditService.log(admin, "TOGGLE_USER_STATUS", "USER", updated.getId().toString(),
                "User " + updated.getUsername() + " active status set to: " + updated.isActive(), httpRequest);

        return mapToUserProfile(updated);
    }

    private IncidentSummaryResponse mapToSummary(Incident incident) {
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

    private UserProfileResponse mapToUserProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
