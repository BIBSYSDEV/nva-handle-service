package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import nva.commons.core.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

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

    public static UUID identifierFromDatabaseIdentifier(String identifier) {
        return UUID.fromString(identifier.replace("Approval:", StringUtils.EMPTY_STRING));
    }

    public EnhancedDocument toEnhancedDocument(HandleDao handleDao) {
        return EnhancedDocument.builder()
                   .json(toJsonString())
                   .put(PK0, getDatabaseIdentifier(), STRING)
                   .put(SK0, getDatabaseIdentifier(), STRING)
                   .put(PK1, getDatabaseIdentifier(), STRING)
                   .put(SK1, getDatabaseIdentifier(), STRING)
                   .put(PK2, handleDao.getDatabaseIdentifier(), STRING)
                   .put(SK2, handleDao.getDatabaseIdentifier(), STRING)
                   .build();
    }
}
