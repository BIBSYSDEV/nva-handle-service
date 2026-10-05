package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.defaultDynamoClient;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.documentTableSchema;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBeginsWith;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Change;
import no.sikt.nva.approvals.domain.ChangeList;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DynamoDbChangeRepository implements ChangeRepository {

  private final DynamoDbTable<EnhancedDocument> table;

  public DynamoDbChangeRepository(DynamoDbClient client, Environment environment) {
    this.table =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build()
            .table(environment.readEnv(TABLE), documentTableSchema());
  }

  @JacocoGenerated
  public static ChangeRepository defaultInstance(Environment environment) {
    return new DynamoDbChangeRepository(defaultDynamoClient(environment), environment);
  }

  @Override
  public ChangeList listChangesByApproval(
      UUID approvalIdentifier, SortableIdentifier after, int pageSize) {
    var request = request(approvalIdentifier, pageSize);
    Optional.ofNullable(after)
        .map(identifier -> exclusiveStartKey(approvalIdentifier, identifier))
        .ifPresent(request::exclusiveStartKey);
    return sendRequest(request.build());
  }

  @Override
  public Optional<Change> findChange(UUID approvalIdentifier, SortableIdentifier changeIdentifier) {
    var key =
        createChangeKey(
            approvalIdentifier, ApprovalRevisionDao.CHANGE_KEY_PREFIX + changeIdentifier);
    return Optional.ofNullable(table.getItem(key)).map(DynamoDbChangeRepository::toChange);
  }

  private static Change toChange(EnhancedDocument document) {
    return ChangeDao.fromJson(document.toJson()).toChange();
  }

  private Map<String, AttributeValue> exclusiveStartKey(
      UUID approvalIdentifier, SortableIdentifier after) {
    return createChangeKey(approvalIdentifier, ApprovalRevisionDao.CHANGE_KEY_PREFIX + after)
        .primaryKeyMap(table.tableSchema());
  }

  private static QueryEnhancedRequest.Builder request(UUID approvalIdentifier, int pageSize) {
    var changeKeyPrefix =
        createChangeKey(approvalIdentifier, ApprovalRevisionDao.CHANGE_KEY_PREFIX);
    return QueryEnhancedRequest.builder()
        .queryConditional(sortBeginsWith(changeKeyPrefix))
        .scanIndexForward(false)
        .limit(pageSize);
  }

  private static Key createChangeKey(UUID approvalIdentifier, String sortValue) {
    return Key.builder()
        .partitionValue(toDatabaseIdentifier(approvalIdentifier))
        .sortValue(sortValue)
        .build();
  }

  private static boolean hasMore(Page<EnhancedDocument> page) {
    return Objects.nonNull(page.lastEvaluatedKey()) && !page.lastEvaluatedKey().isEmpty();
  }

  private ChangeList sendRequest(QueryEnhancedRequest request) {
    var page = table.query(request).iterator().next();
    var changes =
        page.items().stream()
            .map(EnhancedDocument::toJson)
            .map(ChangeDao::fromJson)
            .map(ChangeDao::toChange)
            .toList();
    return new ChangeList(changes, hasMore(page));
  }
}
