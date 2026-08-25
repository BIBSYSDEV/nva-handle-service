package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.unit.nva.commons.json.JsonSerializable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("IdentifierPolicy")
public record IdentifierPolicyDao(UUID customerId, Set<String> allowedIdentifierNames)
    implements JsonSerializable {

  private static final String CUSTOMER_KEY = "Customer:%s";
  private static final String IDENTIFIER_POLICY_SORT_KEY = "IdentifierPolicy";

  public static IdentifierPolicyDao fromIdentifierPolicy(IdentifierPolicy identifierPolicy) {
    return new IdentifierPolicyDao(
        identifierPolicy.customerId(), identifierPolicy.allowedIdentifierNames());
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
