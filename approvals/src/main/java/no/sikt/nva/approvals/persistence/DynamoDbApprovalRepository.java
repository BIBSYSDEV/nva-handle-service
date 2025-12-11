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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class DynamoDbApprovalRepository implements ApprovalRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbApprovalRepository.class);
    private static final int BATCH_GET_ITEM_LIMIT = 80;
    private static final int TRANSACT_WRITE_ITEM_LIMIT = 80;
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
    public void save(Approval approval) throws RepositoryException {
        try {
            saveApproval(approval);
        } catch (Exception exception) {
            LOGGER.error("Failed to save approval: {}", exception.getMessage());
            throw new RepositoryException("Could not save approval: " + exception.getMessage());
        }
    }

    @Override
    public Optional<Approval> findByApprovalIdentifier(UUID approvalIdentifier) throws RepositoryException {
        try {
            var entities = fetchEntitiesByApprovalIdentifier(approvalIdentifier);
            return entities.isEmpty() ? Optional.empty() : Optional.of(constructApproval(entities));
        } catch (Exception exception) {
            LOGGER.error("Failed to find approval by identifier {} {}", approvalIdentifier, exception.getMessage());
            throw new RepositoryException("Could not find approval: " + exception.getMessage());
        }
    }

    @Override
    public Optional<Approval> findByHandle(Handle handle) throws RepositoryException {
        try {
            var entities = fetchEntitiesByHandle(handle);
            return entities.isEmpty() ? Optional.empty() : Optional.of(constructApproval(entities));
        } catch (Exception exception) {
            LOGGER.error("Failed to find approval by handle: {} {}", handle.value(), exception.getMessage());
            throw new RepositoryException("Could not find approval by handle: " + exception.getMessage());
        }
    }

    @Override
    public Optional<Approval> findByIdentifier(NamedIdentifier namedIdentifier) throws RepositoryException {
        try {
            var entities = fetchEntitiesByIdentifier(namedIdentifier);
            return entities.isEmpty() ? Optional.empty() : Optional.of(constructApproval(entities));
        } catch (Exception exception) {
            LOGGER.error("Failed to find approval by identifier with type {} and value {} {}", namedIdentifier.name(),
                         namedIdentifier.value(), exception.getMessage());
            throw new RepositoryException("Could not find approval by identifier: " + exception.getMessage());
        }
    }

    @Override
    public List<NamedIdentifier> findIdentifiers(Collection<NamedIdentifier> namedIdentifiers)
        throws RepositoryException {
        return namedIdentifiers.isEmpty() ? List.of() : fetchIdentifiers(namedIdentifiers);
    }

    private static <T> List<List<T>> splitToChunks(List<T> list) {
        return IntStream.range(0, (list.size() + BATCH_GET_ITEM_LIMIT - 1) / BATCH_GET_ITEM_LIMIT)
                   .mapToObj(i -> list.subList(i * BATCH_GET_ITEM_LIMIT,
                                               Math.min((i + 1) * BATCH_GET_ITEM_LIMIT, list.size())))
                   .toList();
    }

    private static Approval constructApproval(List<DatabaseEntry> entities) {
        var handle = getHandle(entities);
        var identifiers = getIdentifiers(entities);
        var approvalDao = getApproval(entities);
        return new Approval(approvalDao.identifier(), identifiers, approvalDao.source(), handle);
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

    private static Key toPrimaryKey(String databaseIdentifier) {
        return Key.builder().partitionValue(databaseIdentifier).sortValue(databaseIdentifier).build();
    }

    private List<NamedIdentifier> fetchIdentifiers(Collection<NamedIdentifier> namedIdentifiers)
        throws RepositoryException {
        try {
            var keys = namedIdentifiers.stream()
                           .map(IdentifierDao::fromIdentifier)
                           .map(IdentifierDao::getDatabaseIdentifier)
                           .map(DynamoDbApprovalRepository::toPrimaryKey)
                           .toList();

            return splitToChunks(keys).stream()
                       .flatMap(keyBatch -> fetchIdentifiersBatch(keyBatch).stream())
                       .distinct()
                       .toList();
        } catch (Exception exception) {
            LOGGER.error("Failed to find identifiers {}", exception.getMessage());
            throw new RepositoryException("Could not find identifiers %s".formatted(exception.getMessage()));
        }
    }

    private List<NamedIdentifier> fetchIdentifiersBatch(List<Key> keys) {
        var readBatchBuilder = ReadBatch.builder(EnhancedDocument.class).mappedTableResource(table);

        keys.forEach(readBatchBuilder::addGetItem);

        var batchRequest = BatchGetItemEnhancedRequest.builder().addReadBatch(readBatchBuilder.build()).build();

        var batchResults = client.batchGetItem(batchRequest);

        return batchResults.resultsForTable(table)
                   .stream()
                   .map(EnhancedDocument::toJson)
                   .map(this::toDatabaseEntity)
                   .filter(IdentifierDao.class::isInstance)
                   .map(IdentifierDao.class::cast)
                   .map(IdentifierDao::toIdentifier)
                   .toList();
    }

    private void saveApproval(Approval approval) {
        var allDocuments = createDocuments(approval);
        if (allDocuments.size() <= TRANSACT_WRITE_ITEM_LIMIT) {
            saveDocumentsInTransaction(allDocuments);
        } else {
            splitToChunks(allDocuments).forEach(this::saveDocumentsInTransaction);
        }
    }

    private void saveDocumentsInTransaction(List<EnhancedDocument> documents) {
        var requestBuilder = TransactWriteItemsEnhancedRequest.builder();

        documents.forEach(document -> {
            var putRequest = TransactPutItemEnhancedRequest.builder(EnhancedDocument.class)
                                 .item(document)
                                 .conditionExpression(newDaoCondition())
                                 .build();
            requestBuilder.addPutItem(table, putRequest);
        });

        client.transactWriteItems(requestBuilder.build());
    }

    private List<DatabaseEntry> fetchEntitiesByApprovalIdentifier(UUID identifier) {
        return fetchEntitiesByApprovalIdentifier(toDatabaseIdentifier(identifier));
    }

    private List<DatabaseEntry> fetchEntitiesByApprovalIdentifier(String databaseIdentifier) {
        return table.index(GSI1)
                   .query(keyEqualTo(Key.builder().partitionValue(databaseIdentifier).build()))
                   .stream()
                   .map(Page::items)
                   .flatMap(List::stream)
                   .map(EnhancedDocument::toJson)
                   .map(this::toDatabaseEntity)
                   .toList();
    }

    private List<DatabaseEntry> fetchEntitiesByHandle(Handle handle) {
        var databaseIdentifier = HandleDao.fromHandle(handle).getDatabaseIdentifier();
        return table.index(GSI2)
                   .query(keyEqualTo(Key.builder().partitionValue(databaseIdentifier).build()))
                   .stream()
                   .map(Page::items)
                   .flatMap(List::stream)
                   .map(EnhancedDocument::toJson)
                   .map(this::toDatabaseEntity)
                   .toList();
    }

    private List<DatabaseEntry> fetchEntitiesByIdentifier(NamedIdentifier namedIdentifier) {
        var databaseIdentifier = IdentifierDao.fromIdentifier(namedIdentifier).getDatabaseIdentifier();
        var item = table.getItem(toPrimaryKey(databaseIdentifier));
        var approvalDatabaseIdentifier = item.getString(PK1);
        return fetchEntitiesByApprovalIdentifier(approvalDatabaseIdentifier);
    }

    private DatabaseEntry toDatabaseEntity(String value) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, DatabaseEntry.class)).orElseThrow();
    }

    private List<EnhancedDocument> createDocuments(Approval approval) {
        var documents = new ArrayList<EnhancedDocument>();
        documents.add(createHandleEntity(approval));
        documents.add(createApprovalEntity(approval));
        documents.addAll(createIdentifiersEntities(approval));
        return documents;
    }

    private EnhancedDocument createApprovalEntity(Approval approval) {
        var handleDao = HandleDao.fromHandle(approval.handle());
        return ApprovalDao.fromApproval(approval).toEnhancedDocument(handleDao);
    }

    private List<EnhancedDocument> createIdentifiersEntities(Approval approval) {
        return approval.namedIdentifiers()
                   .stream()
                   .map(identifier -> createIdentifierDocument(identifier, approval))
                   .toList();
    }

    private EnhancedDocument createIdentifierDocument(NamedIdentifier namedIdentifier, Approval approval) {
        var handleDao = HandleDao.fromHandle(approval.handle());
        var approvalDao = ApprovalDao.fromApproval(approval);
        return IdentifierDao.fromIdentifier(namedIdentifier).toEnhancedDocument(approvalDao, handleDao);
    }

    private EnhancedDocument createHandleEntity(Approval approval) {
        var approvalDao = ApprovalDao.fromApproval(approval);
        return HandleDao.fromHandle(approval.handle()).toEnhancedDocument(approvalDao);
    }
}
