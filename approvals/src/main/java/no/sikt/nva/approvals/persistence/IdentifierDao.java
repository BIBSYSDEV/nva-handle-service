package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.sikt.nva.approvals.domain.Identifier;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Identifier")
public record IdentifierDao(String name, String value) implements DatabaseEntry {

    public static IdentifierDao fromIdentifier(Identifier identifier) {
        return new IdentifierDao(identifier.type(), identifier.value());
    }

    @Override
    public String getDatabaseIdentifier() {
        return "Identifier:%s#%s".formatted(name, value);
    }

    @JsonIgnore
    public Identifier toIdentifier() {
        return new Identifier(name, value);
    }

    public EnhancedDocument toEnhancedDocument(ApprovalDao approvalDao, HandleDao handleDao) {
        return EnhancedDocument.builder()
                   .json(toJsonString())
                   .put(PK0, getDatabaseIdentifier(), STRING)
                   .put(SK0, getDatabaseIdentifier(), STRING)
                   .put(PK1, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(SK1, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(PK2, handleDao.getDatabaseIdentifier(), STRING)
                   .put(SK2, handleDao.getDatabaseIdentifier(), STRING)
                   .build();
    }
}
