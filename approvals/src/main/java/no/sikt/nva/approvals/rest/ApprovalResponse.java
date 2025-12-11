package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Approval")
public record ApprovalResponse(
    URI id,
    UUID identifier,
    Collection<NamedIdentifier> identifiers,
    URI source,
    String handle
) {

    private static final String APPROVAL_PATH = "approval";

    public static ApprovalResponse fromApproval(Approval approval, URI requestUri) {
        var id = buildId(requestUri, approval.identifier());
        return new ApprovalResponse(
            id,
            approval.identifier(),
            approval.namedIdentifiers(),
            approval.source(),
            approval.handle().value().toString()
        );
    }

    private static URI buildId(URI requestUri, UUID identifier) {
        return UriWrapper.fromHost(requestUri.getHost())
            .addChild(APPROVAL_PATH)
            .addChild(identifier.toString())
            .getUri();
    }
}
