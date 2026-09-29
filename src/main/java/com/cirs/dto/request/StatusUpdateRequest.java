package com.cirs.dto.request;

import com.cirs.enums.IncidentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateRequest {

    @NotNull(message = "New incident status is required")
    private IncidentStatus status;

    @NotBlank(message = "A status update comment/rationale is required")
    private String comment;

    public StatusUpdateRequest() {}

    public StatusUpdateRequest(IncidentStatus status, String comment) {
        this.status = status;
        this.comment = comment;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
