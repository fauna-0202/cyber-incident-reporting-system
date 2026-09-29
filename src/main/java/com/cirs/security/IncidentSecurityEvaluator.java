package com.cirs.security;

import com.cirs.entity.Incident;
import com.cirs.entity.User;
import com.cirs.enums.Role;
import com.cirs.repository.IncidentRepository;
import com.cirs.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("incidentSecurity")
public class IncidentSecurityEvaluator {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public IncidentSecurityEvaluator(IncidentRepository incidentRepository, UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Checks if current user is the original reporter of the incident.
     */
    public boolean isReporter(String incidentNumber, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String currentUsername = authentication.getName();
        Optional<Incident> incidentOpt = incidentRepository.findByIncidentNumber(incidentNumber);
        return incidentOpt.map(incident -> incident.getReporter().getUsername().equals(currentUsername)).orElse(false);
    }

    /**
     * Checks if current user is the assigned investigator for the incident.
     */
    public boolean isAssignedInvestigator(String incidentNumber, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String currentUsername = authentication.getName();
        Optional<Incident> incidentOpt = incidentRepository.findByIncidentNumber(incidentNumber);
        return incidentOpt.map(incident -> incident.getAssignedInvestigator() != null &&
                incident.getAssignedInvestigator().getUsername().equals(currentUsername)).orElse(false);
    }

    /**
     * Checks whether the current user has access to view or interact with this incident:
     * - ADMIN: access all
     * - INVESTIGATOR: assigned incidents
     * - USER: own submitted incidents
     */
    public boolean canAccessIncident(String incidentNumber, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (user.getRole() == Role.ROLE_ADMIN) {
            return true;
        }

        Optional<Incident> incidentOpt = incidentRepository.findByIncidentNumber(incidentNumber);
        if (incidentOpt.isEmpty()) {
            return false; // Return false so 404 or 403 can be thrown at service layer
        }

        Incident incident = incidentOpt.get();
        if (user.getRole() == Role.ROLE_INVESTIGATOR) {
            return incident.getAssignedInvestigator() != null &&
                    incident.getAssignedInvestigator().getId().equals(user.getId());
        }

        if (user.getRole() == Role.ROLE_USER) {
            return incident.getReporter().getId().equals(user.getId());
        }

        return false;
    }
}
