package no.sikt.nva.approvals.persistence;

import static java.util.Objects.isNull;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.unit.nva.commons.json.JsonUtils;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("IdentifierPolicy")
public record IdentifierPolicyDao(UUID customerIdentifier, Set<String> allowedIdentifierNames)
    implements DatabaseEntry {

  private static final String CUSTOMER_KEY = "Customer:%s";
  private static final String IDENTIFIER_POLICY_SORT_KEY = "IdentifierPolicy";

  // An empty set is omitted by the NON_EMPTY inclusion of the serializing object mapper
  public IdentifierPolicyDao {
    allowedIdentifierNames = isNull(allowedIdentifierNames) ? Set.of() : allowedIdentifierNames;
  }

  public static IdentifierPolicyDao fromIdentifierPolicy(IdentifierPolicy identifierPolicy) {
    return new IdentifierPolicyDao(
        identifierPolicy.customerIdentifier(), identifierPolicy.allowedIdentifierNames());
  }

  public static IdentifierPolicyDao fromJson(String json) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, IdentifierPolicyDao.class))
        .orElseThrow();
  }

  public static Key primaryKey(UUID customerIdentifier) {
    return Key.builder()
        .partitionValue(customerKey(customerIdentifier))
        .sortValue(IDENTIFIER_POLICY_SORT_KEY)
        .build();
  }

  @Override
  public String getDatabaseIdentifier() {
    return customerKey(customerIdentifier);
  }

  public IdentifierPolicy toIdentifierPolicy() {
    return new IdentifierPolicy(customerIdentifier, allowedIdentifierNames);
  }

  public EnhancedDocument toEnhancedDocument() {
    return EnhancedDocument.builder()
        .json(toJsonString())
        .put(PK0, getDatabaseIdentifier(), STRING)
        .put(SK0, IDENTIFIER_POLICY_SORT_KEY, STRING)
        .build();
  }

  private static String customerKey(UUID customerIdentifier) {
    return CUSTOMER_KEY.formatted(customerIdentifier);
  }
}
