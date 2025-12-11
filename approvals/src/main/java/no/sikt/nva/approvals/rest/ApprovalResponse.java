package no.sikt.nva.approvals.rest;

import static java.util.Objects.nonNull;
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

    public static ApprovalResponse fromApproval(Approval approval, String apiHost) {
        var id = buildId(apiHost, approval.identifier());
        return new ApprovalResponse(
            id,
            approval.identifier(),
            approval.namedIdentifiers(),
            approval.source(),
            extractHandle(approval)
        );
    }

    private static String extractHandle(Approval approval) {
        return nonNull(approval.handle()) ? approval.handle().value().toString() : null;
    }

    private static URI buildId(String apiHost, UUID identifier) {
        return UriWrapper.fromHost(apiHost)
            .addChild(APPROVAL_PATH)
            .addChild(identifier.toString())
            .getUri();
    }
}
