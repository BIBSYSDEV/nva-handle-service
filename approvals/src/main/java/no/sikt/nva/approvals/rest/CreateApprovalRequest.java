package no.sikt.nva.approvals.rest;

import java.net.URI;
import java.util.List;
import no.sikt.nva.approvals.domain.Identifier;

public record CreateApprovalRequest(List<Identifier> identifiers, URI source) {

}
