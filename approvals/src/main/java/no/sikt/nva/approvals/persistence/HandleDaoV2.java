package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.KEY_FIELD_DELIMITER;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;

import java.net.URI;

public class HandleDaoV2 extends Dao {

  private static final String TYPE_FIELD = "HandleDao";
  private URI handleUri;

  public HandleDaoV2() {
    super();
  }

  @DynamoDbIgnore
  public static String createPartitionKey(String identifier) {
    return String.join(KEY_FIELD_DELIMITER, TYPE_FIELD, identifier);
  }

  public URI getHandleUri() {
    return handleUri;
  }

  public void setHandleUri(URI handleUri) {
    this.handleUri = handleUri;
  }
}
