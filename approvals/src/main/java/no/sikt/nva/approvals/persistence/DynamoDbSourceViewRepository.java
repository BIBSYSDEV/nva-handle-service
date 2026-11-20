package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.defaultDynamoClient;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.documentTableSchema;
import static no.sikt.nva.approvals.persistence.SourceViewDao.SNAPSHOT_IDENTIFIER;
import static no.sikt.nva.approvals.persistence.SourceViewDao.SORT_KEY;
import static no.sikt.nva.approvals.persistence.SourceViewDao.VIEW_VERSION;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.SourceViewUpdate;
import no.sikt.nva.approvals.domain.StoredSourceView;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

public class DynamoDbSourceViewRepository implements SourceViewRepository {

  private static final String SNAPSHOT_IDENTIFIER_ATTRIBUTE = "#snapshotIdentifier";
  private static final String VIEW_VERSION_ATTRIBUTE = "#viewVersion";
  private static final String SNAPSHOT_IDENTIFIER_VALUE = ":snapshotIdentifier";
  private static final String VIEW_VERSION_VALUE = ":viewVersion";
  private static final String NEWER_THAN_STORED =
      "attribute_not_exists(#snapshotIdentifier)"
          + " OR #snapshotIdentifier < :snapshotIdentifier"
          + " OR (#snapshotIdentifier = :snapshotIdentifier AND #viewVersion < :viewVersion)";

  private final DynamoDbTable<EnhancedDocument> table;
  private final Clock clock;

  public DynamoDbSourceViewRepository(DynamoDbClient client, Environment environment, Clock clock) {
    this.table =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build()
            .table(environment.readEnv(TABLE), documentTableSchema());
    this.clock = clock;
  }

  @JacocoGenerated
  public static SourceViewRepository defaultInstance(Environment environment) {
    return new DynamoDbSourceViewRepository(
        defaultDynamoClient(environment), environment, Clock.systemUTC());
  }

  @Override
  public Optional<StoredSourceView> find(UUID approvalIdentifier) {
    return Optional.ofNullable(
            table.getItem(request -> request.key(key(approvalIdentifier)).consistentRead(true)))
        .map(EnhancedDocument::toJson)
        .map(SourceViewDao::fromJson)
        .map(SourceViewDao::toStoredSourceView);
  }

  @Override
  public boolean save(SourceViewUpdate update) {
    var modifiedDate = clock.instant();
    var createdDate =
        find(update.approvalIdentifier()).map(StoredSourceView::createdDate).orElse(modifiedDate);
    try {
      table.putItem(
          PutItemEnhancedRequest.builder(EnhancedDocument.class)
              .item(
                  SourceViewDao.fromSourceViewUpdate(update, createdDate, modifiedDate)
                      .toEnhancedDocument())
              .conditionExpression(newerThanStored(update))
              .build());
      return true;
    } catch (ConditionalCheckFailedException ignored) {
      return false;
    }
  }

  private static Expression newerThanStored(SourceViewUpdate update) {
    return Expression.builder()
        .expression(NEWER_THAN_STORED)
        .expressionNames(
            Map.of(
                SNAPSHOT_IDENTIFIER_ATTRIBUTE, SNAPSHOT_IDENTIFIER,
                VIEW_VERSION_ATTRIBUTE, VIEW_VERSION))
        .expressionValues(
            Map.of(
                SNAPSHOT_IDENTIFIER_VALUE,
                AttributeValue.fromS(update.snapshotIdentifier().toString()),
                VIEW_VERSION_VALUE,
                AttributeValue.fromN(String.valueOf(update.viewVersion()))))
        .build();
  }

  private static Key key(UUID approvalIdentifier) {
    return Key.builder()
        .partitionValue(toDatabaseIdentifier(approvalIdentifier))
        .sortValue(SORT_KEY)
        .build();
  }
}
