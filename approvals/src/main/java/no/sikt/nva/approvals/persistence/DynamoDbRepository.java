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
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeValueType.S;
import static software.amazon.awssdk.enhanced.dynamodb.TableMetadata.primaryIndexName;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.Identifier;
import no.unit.nva.commons.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbRepository implements Repository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbRepository.class);
    private static final EnhancedType<? super String> STRING = EnhancedType.of(String.class);
    private final DynamoDbTable<EnhancedDocument> table;
    private final DynamoDbEnhancedClient client;

    public DynamoDbRepository(DynamoDbClient client) {
        this.client = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
        this.table = this.client.table(TABLE_NAME, documentTableSchema());
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
    public Optional<Approval> findApprovalByIdentifier(UUID identifier) throws RepositoryException {
        try {
            var entities = fetchEntitiesByApprovalIdentifier(identifier);
            return entities.isEmpty() ? Optional.empty() : Optional.of(constructApproval(entities));
        } catch (Exception exception) {
            LOGGER.error("Failed to find approval by identifier {} {}", identifier, exception.getMessage());
            throw new RepositoryException("Could not find approval: " + exception.getMessage());
        }
    }

    @Override
    public Optional<Approval> findApprovalByHandle(Handle handle) throws RepositoryException {
        try {
            var entities = fetchEntitiesByHandle(handle);
            return entities.isEmpty() ? Optional.empty() : Optional.of(constructApproval(entities));
        } catch (Exception exception) {
            LOGGER.error("Failed to find approval by handle: {} {}", handle.value(), exception.getMessage());
            throw new RepositoryException("Could not find approval by handle: " + exception.getMessage());
        }
    }

    @Override
    public Optional<Approval> findApprovalByIdentifier(Identifier identifier) throws RepositoryException {
        try {
            var entities = fetchEntitiesByIdentifier(identifier);
            return entities.isEmpty() ? Optional.empty() : Optional.of(constructApproval(entities));
        } catch (Exception exception) {
            LOGGER.error("Failed to find approval by identifier with type {} and value {} {}", identifier.type(),
                         identifier.value(), exception.getMessage());
            throw new RepositoryException("Could not find approval by identifier: " + exception.getMessage());
        }
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

    private static List<Identifier> getIdentifiers(List<DatabaseEntry> entities) {
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

    private void saveApproval(Approval approval) {
        var documents = createDocuments(approval);
        var requestbuilder = TransactWriteItemsEnhancedRequest.builder();
        for (EnhancedDocument document : documents) {
            var putRequest = TransactPutItemEnhancedRequest.builder(EnhancedDocument.class)
                                 .item(document)
                                 .conditionExpression(newDaoCondition())
                                 .build();
            requestbuilder.addPutItem(table, putRequest);
        }
        client.transactWriteItems(requestbuilder.build());
    }

    private List<DatabaseEntry> fetchEntitiesByApprovalIdentifier(UUID identifier) {
        return table.index(GSI1)
                   .query(keyEqualTo(Key.builder().partitionValue(toDatabaseIdentifier(identifier)).build()))
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

    private List<DatabaseEntry> fetchEntitiesByIdentifier(Identifier identifier) {
        var databaseIdentifier = IdentifierDao.fromIdentifier(identifier).getDatabaseIdentifier();
        var item = table.getItem(toPrimaryKey(databaseIdentifier));
        var approvalIdentifier = ApprovalDao.fromDatabaseIdentifier(item.getString(PK1));
        return fetchEntitiesByApprovalIdentifier(approvalIdentifier);
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
        var approvalDao = ApprovalDao.fromApproval(approval);
        var handleDao = HandleDao.fromHandle(approval.handle());
        return EnhancedDocument.builder()
                   .json(approvalDao.toJsonString())
                   .put(PK0, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(SK0, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(PK1, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(SK1, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(PK2, handleDao.getDatabaseIdentifier(), STRING)
                   .put(SK2, handleDao.getDatabaseIdentifier(), STRING)
                   .build();
    }

    private List<EnhancedDocument> createIdentifiersEntities(Approval approval) {
        return approval.identifiers()
                   .stream()
                   .map(identifier -> createIdentifierDocument(identifier, approval))
                   .toList();
    }

    private EnhancedDocument createIdentifierDocument(Identifier identifier, Approval approval) {
        var identifierDao = IdentifierDao.fromIdentifier(identifier);
        var handleDao = HandleDao.fromHandle(approval.handle());
        var approvalDao = ApprovalDao.fromApproval(approval);
        return EnhancedDocument.builder()
                   .json(identifierDao.toJsonString())
                   .put(PK0, identifierDao.getDatabaseIdentifier(), STRING)
                   .put(SK0, identifierDao.getDatabaseIdentifier(), STRING)
                   .put(PK1, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(SK1, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(PK2, handleDao.getDatabaseIdentifier(), STRING)
                   .put(SK2, handleDao.getDatabaseIdentifier(), STRING)
                   .build();
    }

    private EnhancedDocument createHandleEntity(Approval approval) {
        var handleDao = HandleDao.fromHandle(approval.handle());
        var approvalDao = ApprovalDao.fromApproval(approval);
        return EnhancedDocument.builder()
                   .json(handleDao.toJsonString())
                   .put(PK0, handleDao.getDatabaseIdentifier(), STRING)
                   .put(SK0, handleDao.getDatabaseIdentifier(), STRING)
                   .put(PK1, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(SK1, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(PK2, handleDao.getDatabaseIdentifier(), STRING)
                   .put(SK2, handleDao.getDatabaseIdentifier(), STRING)
                   .build();
    }
}
