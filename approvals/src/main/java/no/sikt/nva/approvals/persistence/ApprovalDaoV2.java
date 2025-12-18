package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.KEY_FIELD_DELIMITER;

import java.net.URI;
import java.util.UUID;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;

public class ApprovalDaoV2 extends Dao {

  private static final String TYPE_FIELD = "ApprovalDao";
  private UUID identifier;
  private URI sourceUri;

  public ApprovalDaoV2() {
    super();
  }

  @DynamoDbIgnore
  public static String createPartitionKey(String identifier) {
    return String.join(KEY_FIELD_DELIMITER, TYPE_FIELD, identifier);
  }

  public UUID getIdentifier() {
    return identifier;
  }

  public void setIdentifier(UUID identifier) {
    this.identifier = identifier;
  }

  public URI getSourceUri() {
    return sourceUri;
  }

  public void setSourceUri(URI sourceUri) {
    this.sourceUri = sourceUri;
  }
}
