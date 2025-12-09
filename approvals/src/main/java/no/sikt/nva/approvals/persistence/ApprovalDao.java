package no.sikt.nva.approvals.persistence;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Approval")
public record ApprovalDao(UUID identifier, URI source) implements DatabaseEntity {

    public static ApprovalDao fromApproval(Approval approval) {
        return new ApprovalDao(approval.getIdentifier(), approval.getSource());
    }

    @Override
    public String getDatabaseIdentifier() {
        return identifier.toString();
    }
}
