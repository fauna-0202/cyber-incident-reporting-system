package com.cirs.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum IncidentStatus {
    SUBMITTED,
    UNDER_REVIEW,
    ASSIGNED,
    INVESTIGATING,
    NEED_MORE_INFORMATION,
    RESOLVED,
    CLOSED;

    /**
     * Defines valid status transitions for the Cyber Incident workflow.
     */
    public boolean canTransitionTo(IncidentStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case SUBMITTED -> target == UNDER_REVIEW || target == ASSIGNED;
            case UNDER_REVIEW -> target == ASSIGNED || target == CLOSED;
            case ASSIGNED -> target == INVESTIGATING || target == UNDER_REVIEW;
            case INVESTIGATING -> target == NEED_MORE_INFORMATION || target == RESOLVED || target == CLOSED;
            case NEED_MORE_INFORMATION -> target == INVESTIGATING || target == CLOSED;
            case RESOLVED -> target == CLOSED || target == INVESTIGATING;
            case CLOSED -> false; // Terminal state unless reopened by Admin
        };
    }
}
