package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.KEY_FIELD_DELIMITER;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@DynamoDbBean
public class HandleDaoV2 extends Dao {

  private static final String TYPE_FIELD = "Handle";
  private URI handleUri;

  public HandleDaoV2() {
    super();
    setType(TYPE_FIELD);
  }

  public static HandleDaoV2 create(UUID approvalIdentifier, URI handleUri) {
    var dao = new HandleDaoV2();
    var approvalKey = ApprovalDaoV2.createPartitionKey(approvalIdentifier.toString());
    var handleKey = createPartitionKey(handleUri.toString());

    dao.setPartitionKey0(handleKey);
    dao.setSortKey0(handleKey);
    dao.setPartitionKey1(approvalKey);
    dao.setSortKey1(approvalKey);
    dao.setPartitionKey2(handleKey);
    dao.setSortKey2(handleKey);
    dao.setHandleUri(handleUri);
    dao.setCreatedAt(Instant.now());
    return dao;
  }

  public static HandleDaoV2 key(URI handleUri) {
    var dao = new HandleDaoV2();
    var key = createPartitionKey(handleUri.toString());
    dao.setPartitionKey0(key);
    dao.setSortKey0(key);
    return dao;
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
