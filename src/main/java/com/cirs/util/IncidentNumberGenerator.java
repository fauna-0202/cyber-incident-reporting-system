package com.cirs.util;

import com.cirs.repository.IncidentRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class IncidentNumberGenerator {

    private static final String ALPHANUMERIC = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // Excluded 0, 1, I, O to avoid confusion
    private static final int SUFFIX_LENGTH = 5;
    private final SecureRandom random = new SecureRandom();
    private final IncidentRepository incidentRepository;

    public IncidentNumberGenerator(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    /**
     * Generates a unique, non-enumerable incident tracking number in the format: CIR-YYYYMMDD-XXXXX
     */
    public String generateUniqueIncidentNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String incidentNumber;
        int attempts = 0;
        do {
            StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
            for (int i = 0; i < SUFFIX_LENGTH; i++) {
                int index = random.nextInt(ALPHANUMERIC.length());
                suffix.append(ALPHANUMERIC.charAt(index));
            }
            incidentNumber = "CIR-" + datePart + "-" + suffix;
            attempts++;
            if (attempts > 50) {
                // Highly unlikely collision fallback
                incidentNumber = "CIR-" + datePart + "-" + System.currentTimeMillis() % 100000;
                break;
            }
        } while (incidentRepository.existsByIncidentNumber(incidentNumber));

        return incidentNumber;
    }
}
