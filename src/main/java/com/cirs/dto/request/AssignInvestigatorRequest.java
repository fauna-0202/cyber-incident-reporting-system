package com.cirs.dto.request;

import jakarta.validation.constraints.NotNull;

public class AssignInvestigatorRequest {

    @NotNull(message = "Investigator ID is required")
    private Long investigatorId;

    public AssignInvestigatorRequest() {}

    public AssignInvestigatorRequest(Long investigatorId) {
        this.investigatorId = investigatorId;
    }

    public Long getInvestigatorId() {
        return investigatorId;
    }

    public void setInvestigatorId(Long investigatorId) {
        this.investigatorId = investigatorId;
    }
}
