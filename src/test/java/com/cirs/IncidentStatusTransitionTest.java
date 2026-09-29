package com.cirs;

import com.cirs.enums.IncidentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncidentStatusTransitionTest {

    @Test
    @DisplayName("Should validate correct sequential workflow transitions")
    void testValidTransitions() {
        // SUBMITTED -> UNDER_REVIEW / ASSIGNED
        assertTrue(IncidentStatus.SUBMITTED.canTransitionTo(IncidentStatus.UNDER_REVIEW));
        assertTrue(IncidentStatus.SUBMITTED.canTransitionTo(IncidentStatus.ASSIGNED));

        // UNDER_REVIEW -> ASSIGNED / CLOSED
        assertTrue(IncidentStatus.UNDER_REVIEW.canTransitionTo(IncidentStatus.ASSIGNED));
        assertTrue(IncidentStatus.UNDER_REVIEW.canTransitionTo(IncidentStatus.CLOSED));

        // ASSIGNED -> INVESTIGATING
        assertTrue(IncidentStatus.ASSIGNED.canTransitionTo(IncidentStatus.INVESTIGATING));

        // INVESTIGATING -> NEED_MORE_INFORMATION / RESOLVED
        assertTrue(IncidentStatus.INVESTIGATING.canTransitionTo(IncidentStatus.NEED_MORE_INFORMATION));
        assertTrue(IncidentStatus.INVESTIGATING.canTransitionTo(IncidentStatus.RESOLVED));

        // NEED_MORE_INFORMATION -> INVESTIGATING
        assertTrue(IncidentStatus.NEED_MORE_INFORMATION.canTransitionTo(IncidentStatus.INVESTIGATING));

        // RESOLVED -> CLOSED
        assertTrue(IncidentStatus.RESOLVED.canTransitionTo(IncidentStatus.CLOSED));
    }

    @Test
    @DisplayName("Should block invalid / illegal status skips")
    void testInvalidTransitions() {
        // Cannot jump directly from SUBMITTED to RESOLVED without investigation
        assertFalse(IncidentStatus.SUBMITTED.canTransitionTo(IncidentStatus.RESOLVED));

        // Cannot jump from SUBMITTED directly to CLOSED
        assertFalse(IncidentStatus.SUBMITTED.canTransitionTo(IncidentStatus.CLOSED));

        // Cannot jump from ASSIGNED directly to RESOLVED
        assertFalse(IncidentStatus.ASSIGNED.canTransitionTo(IncidentStatus.RESOLVED));

        // Closed is a terminal state
        assertFalse(IncidentStatus.CLOSED.canTransitionTo(IncidentStatus.INVESTIGATING));
        assertFalse(IncidentStatus.CLOSED.canTransitionTo(IncidentStatus.SUBMITTED));
    }
}
