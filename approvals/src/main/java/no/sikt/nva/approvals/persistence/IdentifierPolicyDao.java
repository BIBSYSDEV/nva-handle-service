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
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("IdentifierPolicy")
public record IdentifierPolicyDao(UUID customerId, Set<String> allowedIdentifierNames)
    implements JsonSerializable {

  private static final String CUSTOMER_KEY = "Customer:%s";
  private static final String IDENTIFIER_POLICY_SORT_KEY = "IdentifierPolicy";

  // An empty set is omitted by the NON_EMPTY inclusion of the serializing object mapper
  public IdentifierPolicyDao {
    allowedIdentifierNames = isNull(allowedIdentifierNames) ? Set.of() : allowedIdentifierNames;
  }

  public static IdentifierPolicyDao fromIdentifierPolicy(IdentifierPolicy identifierPolicy) {
    return new IdentifierPolicyDao(
        identifierPolicy.customerId(), identifierPolicy.allowedIdentifierNames());
  }

  public static IdentifierPolicyDao fromJson(String json) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, IdentifierPolicyDao.class))
        .orElseThrow();
  }

  public static Key primaryKey(UUID customerId) {
    return Key.builder()
        .partitionValue(CUSTOMER_KEY.formatted(customerId))
        .sortValue(IDENTIFIER_POLICY_SORT_KEY)
        .build();
  }

  public IdentifierPolicy toIdentifierPolicy() {
    return new IdentifierPolicy(customerId, allowedIdentifierNames);
  }

  public EnhancedDocument toEnhancedDocument() {
    return EnhancedDocument.builder()
        .json(toJsonString())
        .put(PK0, CUSTOMER_KEY.formatted(customerId), STRING)
        .put(SK0, IDENTIFIER_POLICY_SORT_KEY, STRING)
        .build();
  }
}
