package no.sikt.nva.approvals.persistence;

import static java.util.stream.Collectors.toMap;
import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.defaultDynamoClient;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.documentTableSchema;
import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

public class DynamoDbApprovalSourceRepository implements ApprovalSourceRepository {

  private static final int BATCH_GET_ITEM_LIMIT = 80;
  private static final String SOURCE = "source";
  private static final String SOURCE_ATTRIBUTE = "#source";
  private static final String EXPECTED_SOURCE_VALUE = ":expectedSource";
  private static final String SOURCE_MATCHES = "#source = :expectedSource";

  private final DynamoDbTable<EnhancedDocument> table;
  private final DynamoDbEnhancedClient client;

  public DynamoDbApprovalSourceRepository(DynamoDbClient client, Environment environment) {
    this.client = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    this.table = this.client.table(environment.readEnv(TABLE), documentTableSchema());
  }

  @JacocoGenerated
  public static ApprovalSourceRepository defaultInstance(Environment environment) {
    return new DynamoDbApprovalSourceRepository(defaultDynamoClient(environment), environment);
  }

  @Override
  public Map<UUID, URI> findSources(Collection<UUID> approvalIdentifiers) {
    var keys = approvalIdentifiers.stream().map(DynamoDbApprovalSourceRepository::key).toList();
    return splitToChunks(keys).stream()
        .flatMap(batch -> fetchBatch(batch).stream())
        .collect(toMap(ApprovalDao::identifier, ApprovalDao::source));
  }

  @Override
  public boolean replaceSource(UUID approvalIdentifier, URI expectedSource, URI newSource) {
    return Optional.ofNullable(table.getItem(key(approvalIdentifier)))
        .map(document -> putWithNewSource(document, expectedSource, newSource))
        .orElse(false);
  }

  private boolean putWithNewSource(EnhancedDocument document, URI expectedSource, URI newSource) {
    try {
      table.putItem(
          PutItemEnhancedRequest.builder(EnhancedDocument.class)
              .item(document.toBuilder().putString(SOURCE, newSource.toString()).build())
              .conditionExpression(sourceMatches(expectedSource))
              .build());
      return true;
    } catch (ConditionalCheckFailedException ignored) {
      return false;
    }
  }

  private static Expression sourceMatches(URI expectedSource) {
    return Expression.builder()
        .expression(SOURCE_MATCHES)
        .expressionNames(Map.of(SOURCE_ATTRIBUTE, SOURCE))
        .expressionValues(
            Map.of(EXPECTED_SOURCE_VALUE, AttributeValue.fromS(expectedSource.toString())))
        .build();
  }

  private static Key key(UUID approvalIdentifier) {
    var databaseIdentifier = toDatabaseIdentifier(approvalIdentifier);
    return Key.builder().partitionValue(databaseIdentifier).sortValue(databaseIdentifier).build();
  }

  private List<ApprovalDao> fetchBatch(List<Key> keys) {
    var readBatchBuilder = ReadBatch.builder(EnhancedDocument.class).mappedTableResource(table);
    keys.forEach(key -> readBatchBuilder.addGetItem(get -> get.key(key).consistentRead(true)));
    var request =
        BatchGetItemEnhancedRequest.builder().addReadBatch(readBatchBuilder.build()).build();
    return client.batchGetItem(request).resultsForTable(table).stream()
        .map(EnhancedDocument::toJson)
        .map(DynamoDbApprovalSourceRepository::toApprovalDao)
        .flatMap(Optional::stream)
        .toList();
  }

  private static Optional<ApprovalDao> toApprovalDao(String json) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, DatabaseEntry.class))
        .toOptional()
        .filter(ApprovalDao.class::isInstance)
        .map(ApprovalDao.class::cast);
  }

  private static List<List<Key>> splitToChunks(List<Key> keys) {
    return IntStream.range(0, (keys.size() + BATCH_GET_ITEM_LIMIT - 1) / BATCH_GET_ITEM_LIMIT)
        .mapToObj(
            chunk ->
                keys.subList(
                    chunk * BATCH_GET_ITEM_LIMIT,
                    Math.min((chunk + 1) * BATCH_GET_ITEM_LIMIT, keys.size())))
        .toList();
  }
}
