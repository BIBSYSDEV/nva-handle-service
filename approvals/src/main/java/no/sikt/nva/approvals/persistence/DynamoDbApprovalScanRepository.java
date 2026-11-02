package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.defaultDynamoClient;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.documentTableSchema;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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

public class DynamoDbApprovalScanRepository implements ApprovalScanRepository {

  private static final String APPROVAL_ITEM_FILTER = "#type = :type";
  private static final String TYPE_PLACEHOLDER = "#type";
  private static final String TYPE_VALUE_PLACEHOLDER = ":type";
  private static final String TYPE_ATTRIBUTE = "type";
  private static final String IDENTIFIER_ATTRIBUTE = "identifier";
  public static final String APPROVAL_TYPE = "Approval";

  private final DynamoDbTable<EnhancedDocument> table;

  public DynamoDbApprovalScanRepository(DynamoDbClient client, Environment environment) {
    this.table =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build()
            .table(environment.readEnv(TABLE), documentTableSchema());
  }

  @JacocoGenerated
  public static ApprovalScanRepository defaultInstance(Environment environment) {
    return new DynamoDbApprovalScanRepository(defaultDynamoClient(environment), environment);
  }

  @Override
  public ApprovalIdentifierPage scanApprovals(ScanCursor cursor) {
    var page = table.scan(scanRequest(cursor)).stream().findFirst().orElseThrow();
    return new ApprovalIdentifierPage(approvalIdentifiers(page), nextCursor(cursor, page));
  }

  private static ScanEnhancedRequest scanRequest(ScanCursor cursor) {
    var builder =
        ScanEnhancedRequest.builder()
            .filterExpression(approvalItemsOnly())
            .segment(cursor.segment())
            .totalSegments(cursor.totalSegments());
    return cursor.isStart() ? builder.build() : builder.exclusiveStartKey(toKey(cursor)).build();
  }

  private static Expression approvalItemsOnly() {
    return Expression.builder()
        .expression(APPROVAL_ITEM_FILTER)
        .putExpressionName(TYPE_PLACEHOLDER, TYPE_ATTRIBUTE)
        .putExpressionValue(TYPE_VALUE_PLACEHOLDER, AttributeValue.fromS(APPROVAL_TYPE))
        .build();
  }

  private static List<UUID> approvalIdentifiers(Page<EnhancedDocument> page) {
    return page.items().stream()
        .map(document -> document.getString(IDENTIFIER_ATTRIBUTE))
        .map(UUID::fromString)
        .toList();
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
