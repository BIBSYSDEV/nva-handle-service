package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.NamedIdentifier;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Approval")
public record ApprovalResponse(
    URI id,
    UUID identifier,
    Collection<NamedIdentifier> identifiers,
    URI source,
    String handle
) {

    public static ApprovalResponse fromApproval(Approval approval, URI baseUri) {
        var id = URI.create(baseUri.toString() + "/" + approval.identifier().toString());
        return new ApprovalResponse(
            id,
            approval.identifier(),
            approval.namedIdentifiers(),
            approval.source(),
            approval.handle().value().toString()
        );
    }
}
