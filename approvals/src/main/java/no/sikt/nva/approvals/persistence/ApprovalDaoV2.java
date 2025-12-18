package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.KEY_FIELD_DELIMITER;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;

@DynamoDbBean
public class ApprovalDaoV2 extends Dao {

  private static final String TYPE_FIELD = "Approval";
  private UUID identifier;
  private URI sourceUri;

  public ApprovalDaoV2() {
    super();
    setType(TYPE_FIELD);
  }

  public static ApprovalDaoV2 create(UUID identifier, URI handleUri, URI sourceUri) {
    var dao = new ApprovalDaoV2();
    var approvalKey = createPartitionKey(identifier.toString());
    var handleKey = HandleDaoV2.createPartitionKey(handleUri.toString());

    dao.setPartitionKey0(approvalKey);
    dao.setSortKey0(approvalKey);
    dao.setPartitionKey1(approvalKey);
    dao.setSortKey1(approvalKey);
    dao.setPartitionKey2(handleKey);
    dao.setSortKey2(handleKey);
    dao.setIdentifier(identifier);
    dao.setSourceUri(sourceUri);
    dao.setCreatedAt(Instant.now());
    return dao;
  }

  public static ApprovalDaoV2 key(UUID identifier) {
    var dao = new ApprovalDaoV2();
    var key = createPartitionKey(identifier.toString());
    dao.setPartitionKey0(key);
    dao.setSortKey0(key);
    return dao;
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
