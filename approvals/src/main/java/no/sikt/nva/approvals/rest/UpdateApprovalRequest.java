package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.ValidationUtils.shouldNotBeEmpty;
import java.util.Collection;
import no.sikt.nva.approvals.domain.NamedIdentifier;

public record UpdateApprovalRequest(Collection<NamedIdentifier> identifiers) {

    public UpdateApprovalRequest {
        shouldNotBeEmpty(identifiers, "Identifiers must not be empty");
    }
}
