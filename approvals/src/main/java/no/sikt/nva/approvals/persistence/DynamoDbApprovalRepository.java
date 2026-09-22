package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.GSI1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.GSI2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.defaultDynamoClient;
import static nva.commons.core.attempt.Try.attempt;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeValueType.S;
import static software.amazon.awssdk.enhanced.dynamodb.TableMetadata.primaryIndexName;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

// FIXME: Suppressing warning in order to upgrade PMD version
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class DynamoDbApprovalRepository implements ApprovalRepository {

  private static final int BATCH_GET_ITEM_LIMIT = 80;
  private static final int TRANSACT_WRITE_ITEM_LIMIT = 80;
  private static final int FIRST_CHUNK = 0;
  private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found: %s";
  private final DynamoDbTable<EnhancedDocument> table;
  private final DynamoDbEnhancedClient client;

  public DynamoDbApprovalRepository(DynamoDbClient client, Environment environment) {
    this.client = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    this.table = this.client.table(environment.readEnv(TABLE), documentTableSchema());
  }

  @JacocoGenerated
  public static ApprovalRepository defaultInstance(Environment environment) {
    return new DynamoDbApprovalRepository(defaultDynamoClient(environment), environment);
  }

  @Override
  public void save(Approval approval) {
    var allDocuments = createDocuments(approval, Instant.now());
    if (allDocuments.size() <= TRANSACT_WRITE_ITEM_LIMIT) {
      saveDocumentsInTransaction(allDocuments);
    } else {
      splitToChunks(allDocuments, TRANSACT_WRITE_ITEM_LIMIT)
          .forEach(this::saveDocumentsInTransaction);
    }
  }

  @Override
  public void updateApprovalIdentifiers(Approval approval) {
    var entities = fetchEntitiesByApprovalIdentifier(toDatabaseIdentifier(approval.identifier()));
    if (entities.isEmpty()) {
      throw new IllegalStateException(APPROVAL_NOT_FOUND_MESSAGE.formatted(approval.identifier()));
    }
    var storedIdentifiers = getIdentifiers(entities);
    var originalCreatedDate = getApproval(entities).createdDate();
    var now = Instant.now();

    var operations = new ArrayList<Operation>();
    operations.addAll(deleteOperations(storedIdentifiers, approval.namedIdentifiers()));
    operations.addAll(createOperations(approval.namedIdentifiers(), storedIdentifiers, now));

    var approvalDao = ApprovalDao.fromApproval(approval, originalCreatedDate, now);
    var handleDao = HandleDao.fromHandle(approval.handle(), originalCreatedDate);
    updateIdentifiersForApproval(approvalDao, handleDao, operations);
  }

  @Override
  public Optional<Approval> findByApprovalIdentifier(UUID approvalIdentifier) {
    var entities = fetchEntitiesByApprovalIdentifier(toDatabaseIdentifier(approvalIdentifier));
    return entities.isEmpty() ? Optional.empty() : Optional.of(constructApproval(entities));
  }

  @Override
  public Optional<Approval> findByHandle(Handle handle) {
    var databaseIdentifier = HandleDao.toDatabaseIdentifier(handle);
    var entities =
        table
            .index(GSI2)
            .query(keyEqualTo(Key.builder().partitionValue(databaseIdentifier).build()))
            .stream()
            .map(Page::items)
            .flatMap(List::stream)
            .map(EnhancedDocument::toJson)
            .map(this::toDatabaseEntity)
            .toList();
    return entities.isEmpty() ? Optional.empty() : Optional.of(constructApproval(entities));
  }

  @Override
  public Optional<Approval> findByIdentifier(NamedIdentifier namedIdentifier) {
    var primaryKey = IdentifierDao.primaryKey(namedIdentifier);
    return Optional.ofNullable(table.getItem(primaryKey))
        .map(item -> item.getString(PK1))
        .map(this::fetchEntitiesByApprovalIdentifier)
        .filter(entities -> !entities.isEmpty())
        .map(DynamoDbApprovalRepository::constructApproval);
  }

  @Override
  public List<NamedIdentifierQueryObject> findIdentifiers(
      Collection<NamedIdentifier> namedIdentifiers) {
    if (namedIdentifiers.isEmpty()) {
      return List.of();
    }
    var keys = namedIdentifiers.stream().map(IdentifierDao::primaryKey).toList();

    return splitToChunks(keys, BATCH_GET_ITEM_LIMIT).stream()
        .flatMap(keyBatch -> fetchIdentifiersBatch(keyBatch).stream())
        .distinct()
        .toList();
  }

  @Override
  public Optional<IdentifierPolicy> findIdentifierPolicy(UUID customerIdentifier) {
    return Optional.ofNullable(table.getItem(IdentifierPolicyDao.primaryKey(customerIdentifier)))
        .map(EnhancedDocument::toJson)
        .map(IdentifierPolicyDao::fromJson)
        .map(IdentifierPolicyDao::toIdentifierPolicy);
  }

  @Override
  public void saveIdentifierPolicy(UUID customerIdentifier, IdentifierPolicy identifierPolicy) {
    table.putItem(
        IdentifierPolicyDao.fromIdentifierPolicy(
                customerIdentifier, identifierPolicy, Instant.now())
            .toEnhancedDocument());
  }

  private static <T> List<List<T>> splitToChunks(List<T> list, int chunkSize) {
    return IntStream.range(0, (list.size() + chunkSize - 1) / chunkSize)
        .mapToObj(
            chunkIndex ->
                list.subList(
                    chunkIndex * chunkSize, Math.min((chunkIndex + 1) * chunkSize, list.size())))
        .toList();
  }

  private static Approval constructApproval(List<DatabaseEntry> entities) {
    var handle = getHandle(entities);
    var identifiers = getIdentifiers(entities);
    var approvalDao = getApproval(entities);
    return new Approval(
        approvalDao.identifier(),
        identifiers,
        approvalDao.source(),
        handle,
        approvalDao.customerId());
  }

  private static Handle getHandle(List<DatabaseEntry> entities) {
    return entities.stream()
        .filter(HandleDao.class::isInstance)
        .map(HandleDao.class::cast)
        .findFirst()
        .map(HandleDao::toHandle)
        .orElseThrow(() -> new IllegalStateException("Handle not found for approval"));
  }

  private static List<NamedIdentifier> getIdentifiers(List<DatabaseEntry> entities) {
    return entities.stream()
        .filter(IdentifierDao.class::isInstance)
        .map(IdentifierDao.class::cast)
        .map(IdentifierDao::toIdentifier)
        .toList();
  }

  private static ApprovalDao getApproval(List<DatabaseEntry> entities) {
    return entities.stream()
        .filter(ApprovalDao.class::isInstance)
        .map(ApprovalDao.class::cast)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Approval not found"));
  }

  private static DocumentTableSchema documentTableSchema() {
    return TableSchema.documentSchemaBuilder()
        .addIndexPartitionKey(primaryIndexName(), PK0, S)
        .addIndexSortKey(primaryIndexName(), SK0, S)
        .addIndexPartitionKey(GSI1, PK1, S)
        .addIndexSortKey(GSI1, SK1, S)
        .addIndexPartitionKey(GSI2, PK2, S)
        .addIndexSortKey(GSI2, SK2, S)
        .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
        .build();
  }

  private static Expression newDaoCondition() {
    return Expression.builder()
        .expression("attribute_not_exists(#pk) AND attribute_not_exists(#sk)")
        .expressionNames(Map.of("#pk", PK0, "#sk", SK0))
        .build();
  }

  private static List<Operation> deleteOperations(
      Collection<NamedIdentifier> storedIdentifiers,
      Collection<NamedIdentifier> updatedIdentifiers) {
    return identifiersNotIn(storedIdentifiers, updatedIdentifiers).stream()
        .map(IdentifierDao::primaryKey)
        .<Operation>map(Operation.DeleteIdentifier::new)
        .toList();
  }

  private static List<Operation> createOperations(
      Collection<NamedIdentifier> updatedIdentifiers,
      Collection<NamedIdentifier> storedIdentifiers,
      Instant createdDate) {
    return identifiersNotIn(updatedIdentifiers, storedIdentifiers).stream()
        .map(namedIdentifier -> IdentifierDao.fromIdentifier(namedIdentifier, createdDate))
        .<Operation>map(Operation.CreateIdentifier::new)
        .toList();
  }

  private static List<NamedIdentifier> identifiersNotIn(
      Collection<NamedIdentifier> namedIdentifiers, Collection<NamedIdentifier> otherIdentifiers) {
    return namedIdentifiers.stream()
        .filter(namedIdentifier -> !otherIdentifiers.contains(namedIdentifier))
        .toList();
  }

  private List<NamedIdentifierQueryObject> fetchIdentifiersBatch(List<Key> keys) {
    var readBatchBuilder = ReadBatch.builder(EnhancedDocument.class).mappedTableResource(table);

    keys.forEach(readBatchBuilder::addGetItem);

    var batchRequest =
        BatchGetItemEnhancedRequest.builder().addReadBatch(readBatchBuilder.build()).build();

    var batchResults = client.batchGetItem(batchRequest);

    return batchResults.resultsForTable(table).stream()
        .map(EnhancedDocument::toJson)
        .map(NamedIdentifierQueryObject::fromJson)
        .toList();
  }

  private void saveDocumentsInTransaction(List<EnhancedDocument> documents) {
    var requestBuilder = TransactWriteItemsEnhancedRequest.builder();

    documents.forEach(
        document -> {
          var putRequest =
              TransactPutItemEnhancedRequest.builder(EnhancedDocument.class)
                  .item(document)
                  .conditionExpression(newDaoCondition())
                  .build();
          requestBuilder.addPutItem(table, putRequest);
        });

    client.transactWriteItems(requestBuilder.build());
  }

  private void updateIdentifiersForApproval(
      ApprovalDao approvalDao, HandleDao handleDao, List<Operation> operations) {
    var chunks = splitToChunks(operations, TRANSACT_WRITE_ITEM_LIMIT);
    IntStream.range(0, chunks.size())
        .forEach(
            chunkIndex ->
                sendTransaction(
                    approvalDao, handleDao, chunks.get(chunkIndex), chunkIndex == FIRST_CHUNK));
  }

  private void sendTransaction(
      ApprovalDao approvalDao,
      HandleDao handleDao,
      List<Operation> operations,
      boolean includeApproval) {
    var requestBuilder = TransactWriteItemsEnhancedRequest.builder();
    if (includeApproval) {
      requestBuilder.addPutItem(table, approvalDao.toEnhancedDocument(handleDao));
    }
    operations.forEach(
        operation -> addOperation(requestBuilder, operation, approvalDao, handleDao));
    client.transactWriteItems(requestBuilder.build());
  }

  private void addOperation(
      TransactWriteItemsEnhancedRequest.Builder requestBuilder,
      Operation operation,
      ApprovalDao approvalDao,
      HandleDao handleDao) {
    switch (operation) {
      case Operation.DeleteIdentifier(var primaryKey) ->
          requestBuilder.addDeleteItem(table, primaryKey);
      case Operation.CreateIdentifier(var identifierDao) ->
          requestBuilder.addPutItem(
              table,
              TransactPutItemEnhancedRequest.builder(EnhancedDocument.class)
                  .item(identifierDao.toEnhancedDocument(approvalDao, handleDao))
                  .conditionExpression(newDaoCondition())
                  .build());
    }
  }

  private List<DatabaseEntry> fetchEntitiesByApprovalIdentifier(String databaseIdentifier) {
    return table
        .index(GSI1)
        .query(keyEqualTo(Key.builder().partitionValue(databaseIdentifier).build()))
        .stream()
        .map(Page::items)
        .flatMap(List::stream)
        .map(EnhancedDocument::toJson)
        .map(this::toDatabaseEntity)
        .toList();
  }

  private DatabaseEntry toDatabaseEntity(String value) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, DatabaseEntry.class))
        .orElseThrow();
  }

  private List<EnhancedDocument> createDocuments(Approval approval, Instant createdDate) {
    var approvalDao = ApprovalDao.fromApproval(approval, createdDate, createdDate);
    var handleDao = HandleDao.fromHandle(approval.handle(), createdDate);

    var documents = new ArrayList<EnhancedDocument>();
    documents.add(handleDao.toEnhancedDocument(approvalDao));
    documents.add(approvalDao.toEnhancedDocument(handleDao));
    documents.addAll(createIdentifierDocuments(approval, approvalDao, handleDao, createdDate));
    return documents;
  }

  private List<EnhancedDocument> createIdentifierDocuments(
      Approval approval, ApprovalDao approvalDao, HandleDao handleDao, Instant createdDate) {
    return approval.namedIdentifiers().stream()
        .map(
            namedIdentifier ->
                IdentifierDao.fromIdentifier(namedIdentifier, createdDate)
                    .toEnhancedDocument(approvalDao, handleDao))
        .toList();
  }

  public sealed interface Operation {

    record CreateIdentifier(IdentifierDao identifierDao) implements Operation {}

    record DeleteIdentifier(Key primaryKey) implements Operation {}
  }
}
