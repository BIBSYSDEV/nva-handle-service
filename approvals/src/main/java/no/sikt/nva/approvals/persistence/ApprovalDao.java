package no.sikt.nva.approvals.persistence;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Approval")
public record ApprovalDao(UUID identifier, URI source) implements DatabaseEntry {

    public static ApprovalDao fromApproval(Approval approval) {
        return new ApprovalDao(approval.identifier(), approval.source());
    }

    @Override
    public String getDatabaseIdentifier() {
        return "Approval:%s".formatted(identifier.toString());
    }

    public static String toDatabaseIdentifier(UUID identifier) {
        return "Approval:%s".formatted(identifier.toString());
    }

    public static UUID fromDatabaseIdentifier(String databaseIdentifier) {
        return UUID.fromString(databaseIdentifier.replace("Approval:", ""));
    }
}
