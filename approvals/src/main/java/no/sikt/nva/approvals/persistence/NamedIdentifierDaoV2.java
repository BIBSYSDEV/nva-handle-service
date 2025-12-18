package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.KEY_FIELD_DELIMITER;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@DynamoDbBean
public class NamedIdentifierDaoV2 extends Dao {

  private static final String TYPE_FIELD = "Identifier";
  private String name;
  private String value;

  public NamedIdentifierDaoV2() {
    super();
    setType(TYPE_FIELD);
  }

  public static NamedIdentifierDaoV2 create(String name, String value, UUID approvalIdentifier, URI handleUri) {
    var dao = new NamedIdentifierDaoV2();
    var identifierKey = createPartitionKey(name, value);
    var approvalKey = ApprovalDaoV2.createPartitionKey(approvalIdentifier.toString());
    var handleKey = HandleDaoV2.createPartitionKey(handleUri.toString());

    dao.setPartitionKey0(identifierKey);
    dao.setSortKey0(identifierKey);
    dao.setPartitionKey1(approvalKey);
    dao.setSortKey1(approvalKey);
    dao.setPartitionKey2(handleKey);
    dao.setSortKey2(handleKey);
    dao.setName(name);
    dao.setValue(value);
    dao.setCreatedAt(Instant.now());
    return dao;
  }

  public static NamedIdentifierDaoV2 key(String name, String value) {
    var dao = new NamedIdentifierDaoV2();
    var key = createPartitionKey(name, value);
    dao.setPartitionKey0(key);
    dao.setSortKey0(key);
    return dao;
  }

  @DynamoDbIgnore
  public static String createPartitionKey(String name, String value) {
    return String.join(KEY_FIELD_DELIMITER, TYPE_FIELD, name, value);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
