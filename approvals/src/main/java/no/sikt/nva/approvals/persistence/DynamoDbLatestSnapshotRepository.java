package no.sikt.nva.approvals.persistence;

import static java.util.Objects.isNull;
import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.defaultDynamoClient;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.documentTableSchema;
import static no.sikt.nva.approvals.persistence.LatestRevisionCondition.APPROVAL_ITEM_INDEX;
import static no.sikt.nva.approvals.persistence.LatestRevisionCondition.CONDITIONAL_CHECK_FAILED;
import static no.sikt.nva.approvals.persistence.LatestRevisionCondition.expectedRevisionCondition;
import static no.sikt.nva.approvals.persistence.LatestRevisionCondition.hasCancellationReasonAt;
import static no.sikt.nva.approvals.persistence.LatestRevisionCondition.toModifiedExceptionIfConditionFailed;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.IgnoreNullsMode;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

public class DynamoDbLatestSnapshotRepository implements LatestSnapshotRepository {

  private static final String SNAPSHOT_VERSION = "snapshotVersion";
  private static final long FIRST_SNAPSHOT_VERSION = 1L;
  private static final int SNAPSHOT_ITEM_INDEX = 1;
  private static final String NO_FAILURE = "None";
  private static final String AND = " AND ";
  private static final String PARTITION_KEY_ATTRIBUTE = "#pk";
  private static final String SNAPSHOT_VERSION_ATTRIBUTE = "#snapshotVersion";
  private static final String EXPECTED_SNAPSHOT_VERSION_VALUE = ":expectedSnapshotVersion";
  private static final String APPROVAL_EXISTS =
      "attribute_exists(%s)".formatted(PARTITION_KEY_ATTRIBUTE);
  private static final String SNAPSHOT_VERSION_MISSING =
      "attribute_not_exists(%s)".formatted(SNAPSHOT_VERSION_ATTRIBUTE);
  private static final String SNAPSHOT_VERSION_MATCHES =
      "%s = %s".formatted(SNAPSHOT_VERSION_ATTRIBUTE, EXPECTED_SNAPSHOT_VERSION_VALUE);

  private final DynamoDbEnhancedClient client;
  private final DynamoDbTable<EnhancedDocument> table;

  public DynamoDbLatestSnapshotRepository(DynamoDbClient client, Environment environment) {
    this.client = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    this.table = this.client.table(environment.readEnv(TABLE), documentTableSchema());
  }

  @JacocoGenerated
  public static LatestSnapshotRepository defaultInstance(Environment environment) {
    return new DynamoDbLatestSnapshotRepository(defaultDynamoClient(environment), environment);
  }

  @Override
  public Optional<SnapshotState> findSnapshotStateConsistently(UUID approvalIdentifier) {
    var request =
        GetItemEnhancedRequest.builder()
            .key(ApprovalDao.primaryKey(approvalIdentifier))
            .consistentRead(true)
            .build();
    return Optional.ofNullable(table.getItem(request))
        .map(DynamoDbLatestSnapshotRepository::toSnapshotState);
  }

  @Override
  public void saveAsLatestSnapshot(SourceSnapshot snapshot, SnapshotState expectedState) {
    var request =
        TransactWriteItemsEnhancedRequest.builder()
            .addUpdateItem(
                table, snapshotVersionUpdate(snapshot.approvalIdentifier(), expectedState))
            .addPutItem(
                table,
                DynamoDbApprovalRepository.newItemRequest(
                    SourceSnapshotDao.fromSourceSnapshot(snapshot).toEnhancedDocument()))
            .build();
    try {
      client.transactWriteItems(request);
    } catch (TransactionCanceledException exception) {
      if (!isExistingSnapshot(exception)) {
        throw toModifiedExceptionIfConditionFailed(exception, snapshot.approvalIdentifier());
      }
    }
  }

  private static SnapshotState toSnapshotState(EnhancedDocument approvalItem) {
    var approvalDao = ApprovalDao.fromJson(approvalItem.toJson());
    var snapshotVersion =
        approvalItem.isPresent(SNAPSHOT_VERSION)
            ? approvalItem.getNumber(SNAPSHOT_VERSION).longValue()
            : null;
    return new SnapshotState(approvalDao.source(), approvalDao.latestRevision(), snapshotVersion);
  }

  private static TransactUpdateItemEnhancedRequest<EnhancedDocument> snapshotVersionUpdate(
      UUID approvalIdentifier, SnapshotState expectedState) {
    return TransactUpdateItemEnhancedRequest.builder(EnhancedDocument.class)
        .item(snapshotVersionDocument(approvalIdentifier, nextSnapshotVersion(expectedState)))
        .conditionExpression(condition(expectedState))
        .ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY)
        .build();
  }

  private static long nextSnapshotVersion(SnapshotState expectedState) {
    return isNull(expectedState.snapshotVersion())
        ? FIRST_SNAPSHOT_VERSION
        : expectedState.snapshotVersion() + 1;
  }

  private static EnhancedDocument snapshotVersionDocument(
      UUID approvalIdentifier, long snapshotVersion) {
    var databaseIdentifier = toDatabaseIdentifier(approvalIdentifier);
    return EnhancedDocument.builder()
        .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
        .put(PK0, databaseIdentifier, STRING)
        .put(SK0, databaseIdentifier, STRING)
        .putNumber(SNAPSHOT_VERSION, snapshotVersion)
        .build();
  }

  private static Expression condition(SnapshotState expectedState) {
    var approvalExists =
        Expression.builder()
            .expression(APPROVAL_EXISTS)
            .expressionNames(Map.of(PARTITION_KEY_ATTRIBUTE, PK0))
            .build();
    return Expression.join(
        Expression.join(
            approvalExists, expectedRevisionCondition(expectedState.latestRevision()), AND),
        snapshotVersionCondition(expectedState.snapshotVersion()),
        AND);
  }

  private static Expression snapshotVersionCondition(Long snapshotVersion) {
    var names = Map.of(SNAPSHOT_VERSION_ATTRIBUTE, SNAPSHOT_VERSION);
    if (isNull(snapshotVersion)) {
      return Expression.builder()
          .expression(SNAPSHOT_VERSION_MISSING)
          .expressionNames(names)
          .build();
    }
    return Expression.builder()
        .expression(SNAPSHOT_VERSION_MATCHES)
        .expressionNames(names)
        .expressionValues(
            Map.of(
                EXPECTED_SNAPSHOT_VERSION_VALUE, AttributeValue.fromN(snapshotVersion.toString())))
        .build();
  }

  private static boolean isExistingSnapshot(TransactionCanceledException exception) {
    return hasCancellationReasonAt(exception, APPROVAL_ITEM_INDEX, NO_FAILURE)
        && hasCancellationReasonAt(exception, SNAPSHOT_ITEM_INDEX, CONDITIONAL_CHECK_FAILED);
  }
}
