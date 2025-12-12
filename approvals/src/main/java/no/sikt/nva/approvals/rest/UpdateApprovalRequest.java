package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.ValidationUtils.shouldNotBeEmpty;
import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;

public record UpdateApprovalRequest(Collection<NamedIdentifier> identifiers, URI source, URI handle) {

    public UpdateApprovalRequest {
        Objects.requireNonNull(identifiers, "identifiers must not be null");
        shouldNotBeEmpty(identifiers, "identifiers must not be empty");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(handle, "handle must not be null");
    }

    public Approval toApproval(UUID identifier) {
        return new Approval(identifier, identifiers(), source(), new Handle(handle()));
    }
}
