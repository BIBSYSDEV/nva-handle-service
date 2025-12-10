package no.sikt.nva.approvals.rest;

import static java.util.Objects.isNull;
import static no.sikt.nva.approvals.utils.ValidationUtils.shouldNotBeEmpty;
import java.net.URI;
import java.util.List;
import no.sikt.nva.approvals.domain.Identifier;

public record CreateApprovalRequest(List<Identifier> identifiers, URI source) {

    public void validate() {
        shouldNotBeEmpty(identifiers, "At least one identifier is mandatory for approval creation");
        if (isNull(source())) {
            throw new IllegalArgumentException("Source is mandatory for approval creation");
        }
    }
}
