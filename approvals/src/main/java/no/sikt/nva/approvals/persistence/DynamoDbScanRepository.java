package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.defaultDynamoClient;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.documentTableSchema;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DynamoDbScanRepository implements ScanRepository {

  private static final String ENTITY_TYPE_FILTER = "#type = :type";
  private static final String TYPE_PLACEHOLDER = "#type";
  private static final String TYPE_VALUE_PLACEHOLDER = ":type";
  private static final String TYPE_ATTRIBUTE = "type";

  private final DynamoDbTable<EnhancedDocument> table;

  public DynamoDbScanRepository(DynamoDbClient client, Environment environment) {
    this.table =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build()
            .table(environment.readEnv(TABLE), documentTableSchema());
  }

  @JacocoGenerated
  public static ScanRepository defaultInstance(Environment environment) {
    return new DynamoDbScanRepository(defaultDynamoClient(environment), environment);
  }

  @Override
  public ItemKeyPage scan(ScanQuery query) {
    var page = table.scan(scanRequest(query)).stream().findFirst().orElseThrow();
    return new ItemKeyPage(keys(page), nextCursor(query.cursor(), page));
  }

  private static ScanEnhancedRequest scanRequest(ScanQuery query) {
    var cursor = query.cursor();
    var builder =
        ScanEnhancedRequest.builder()
            .filterExpression(entityItemsOnly(query.entityType()))
            .limit(query.pageSize())
            .segment(cursor.segment())
            .totalSegments(cursor.totalSegments());
    return cursor.isStart() ? builder.build() : builder.exclusiveStartKey(toKey(cursor)).build();
  }

  private static Expression entityItemsOnly(String entityType) {
    return Expression.builder()
        .expression(ENTITY_TYPE_FILTER)
        .putExpressionName(TYPE_PLACEHOLDER, TYPE_ATTRIBUTE)
        .putExpressionValue(TYPE_VALUE_PLACEHOLDER, AttributeValue.fromS(entityType))
        .build();
  }

  private static List<ItemKey> keys(Page<EnhancedDocument> page) {
    return page.items().stream().map(document -> new ItemKey(document.getString(PK0))).toList();
  }

  private static ScanCursor nextCursor(ScanCursor cursor, Page<EnhancedDocument> page) {
    var lastEvaluatedKey = page.lastEvaluatedKey();
    if (Objects.isNull(lastEvaluatedKey)) {
      return null;
    }
    return new ScanCursor(
        cursor.segment(),
        cursor.totalSegments(),
        lastEvaluatedKey.get(PK0).s(),
        lastEvaluatedKey.get(SK0).s());
  }

  private static Map<String, AttributeValue> toKey(ScanCursor cursor) {
    return Map.of(
        PK0,
        AttributeValue.fromS(cursor.partitionKey()),
        SK0,
        AttributeValue.fromS(cursor.sortKey()));
  }
}
