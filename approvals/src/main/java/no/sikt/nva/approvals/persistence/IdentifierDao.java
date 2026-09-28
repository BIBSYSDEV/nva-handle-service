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
import java.time.Instant;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Identifier")
public record IdentifierDao(String name, String value, Instant createdDate)
    implements DatabaseEntry {

  private static final String IDENTIFIER_KEY = "Identifier:%s#%s";

  public static IdentifierDao fromIdentifier(NamedIdentifier namedIdentifier, Instant createdDate) {
    return new IdentifierDao(namedIdentifier.name(), namedIdentifier.value(), createdDate);
  }

  @Override
  public String getDatabaseIdentifier() {
    return IDENTIFIER_KEY.formatted(name, value);
  }

  public static String toDatabaseIdentifier(NamedIdentifier namedIdentifier) {
    return IDENTIFIER_KEY.formatted(namedIdentifier.name(), namedIdentifier.value());
  }

  public static Key primaryKey(NamedIdentifier namedIdentifier) {
    var databaseIdentifier = toDatabaseIdentifier(namedIdentifier);
    return Key.builder().partitionValue(databaseIdentifier).sortValue(databaseIdentifier).build();
  }

  @JsonIgnore
  public NamedIdentifier toIdentifier() {
    return new NamedIdentifier(name, value);
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
