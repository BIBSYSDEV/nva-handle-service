package no.sikt.nva.approvals.rest;

import static java.util.Objects.isNull;
import java.net.URI;
import java.util.List;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Identifier;

public record CreateApprovalRequest(List<Identifier> identifiers, URI source) {

    public Approval toNewApproval() {
        return new Approval(null, identifiers(), source());
    }

    public void validate() {
        if (identifiers().isEmpty()) {
            throw new IllegalArgumentException("At least one identifier is mandatory for approval creation");
        }
        if (isNull(source())) {
            throw new IllegalArgumentException("Source is mandatory for approval creation");
        }
    }
}
