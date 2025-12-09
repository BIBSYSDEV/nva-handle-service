package no.sikt.nva.approvals.utils;

import static java.util.Objects.isNull;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.rest.CreateApprovalRequest;

public final class ApprovalFactory {

    private ApprovalFactory() {
    }

    public static Approval newApprovalFromRequest(CreateApprovalRequest createApprovalRequest) {
        if (createApprovalRequest.identifiers().isEmpty()) {
            throw new IllegalArgumentException("At least one identifier is mandatory for approval creation");
        }
        if (isNull(createApprovalRequest.source())) {
            throw new IllegalArgumentException("Source is mandatory for approval creation");
        }
        return new Approval(null, createApprovalRequest.identifiers(), createApprovalRequest.source());
    }
}
