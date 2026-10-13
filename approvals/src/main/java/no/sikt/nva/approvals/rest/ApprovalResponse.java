package no.sikt.nva.approvals.rest;

import static java.util.Objects.nonNull;
import static no.sikt.nva.approvals.utils.UriConstants.CONTEXT_PROPERTY;
import static no.sikt.nva.approvals.utils.UriConstants.approvalId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.utils.UriConstants;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Approval")
public record ApprovalResponse(
    @JsonProperty(CONTEXT_PROPERTY) URI context,
    URI id,
    UUID identifier,
    Collection<NamedIdentifier> identifiers,
    URI source,
    String handle) {

  public static ApprovalResponse fromApproval(Approval approval, String apiHost) {
    var id = approvalId(apiHost, approval.identifier());
    var contextUri = UriConstants.context(apiHost);
    return new ApprovalResponse(
        contextUri,
        id,
        approval.identifier(),
        approval.namedIdentifiers(),
        approval.source(),
        extractHandle(approval));
  }

  private static String extractHandle(Approval approval) {
    return nonNull(approval.handle()) ? approval.handle().value().toString() : null;
  }
}
