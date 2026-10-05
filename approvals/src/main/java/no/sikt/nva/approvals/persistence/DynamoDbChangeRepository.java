package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.ChangeDao.CHANGES_PARTITION;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.GSI1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.defaultDynamoClient;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.documentTableSchema;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBeginsWith;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortGreaterThan;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Change;
import no.sikt.nva.approvals.domain.ChangeList;
import no.sikt.nva.approvals.domain.ListChangesRequest;
import no.sikt.nva.approvals.domain.ListChangesRequest.After;
import no.sikt.nva.approvals.domain.ListChangesRequest.Since;
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

  private static final String SINCE_LOWER_BOUND_FORMAT = "%012x";
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
    return toChangeList(table.query(request.build()).iterator().next());
  }

  @Override
  public ChangeList listChanges(ListChangesRequest request) {
    var startKey = getExclusiveStartKey(request);
    var query = createListChangesQuery(startKey, request.pageSize());
    return toChangeList(table.index(GSI1).query(query).iterator().next());
  }

  private static QueryEnhancedRequest createListChangesQuery(String startKey, int limit) {
    return QueryEnhancedRequest.builder()
               .queryConditional(
                   sortGreaterThan(
                       Key.builder().partitionValue(CHANGES_PARTITION).sortValue(startKey).build()))
               .scanIndexForward(true)
               .limit(limit)
               .build();
  }

  private static String getExclusiveStartKey(ListChangesRequest request) {
      return switch (request) {
        case Since since -> SINCE_LOWER_BOUND_FORMAT.formatted(since.timestamp().toEpochMilli());
        case After after -> after.changeIdentifier().toString();
      };
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

  private static ChangeList toChangeList(Page<EnhancedDocument> page) {
    var changes =
        page.items().stream()
            .map(EnhancedDocument::toJson)
            .map(ChangeDao::fromJson)
            .map(ChangeDao::toChange)
            .toList();
    return new ChangeList(changes, hasMore(page));
  }
}
