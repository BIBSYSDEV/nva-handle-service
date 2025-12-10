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
import no.sikt.nva.approvals.domain.Handle;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Handle")
public record HandleDao(URI uri) implements DatabaseEntry {

    public static HandleDao fromHandle(Handle handle) {
        return new HandleDao(handle.value());
    }

    public Handle toHandle() {
        return new Handle(uri);
    }

    @Override
    public String getDatabaseIdentifier() {
        return "Handle:%s".formatted(uri.toString());
    }

    public EnhancedDocument toEnhancedDocument(ApprovalDao approvalDao) {
        return EnhancedDocument.builder()
                   .json(toJsonString())
                   .put(PK0, getDatabaseIdentifier(), STRING)
                   .put(SK0, getDatabaseIdentifier(), STRING)
                   .put(PK1, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(SK1, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(PK2, getDatabaseIdentifier(), STRING)
                   .put(SK2, getDatabaseIdentifier(), STRING)
                   .build();
    }
}
