package no.sikt.nva.approvals.persistence;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
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
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
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
            throw new RepositoryException("Could not save approval!");
        }
    }

    @Override
    public Approval getApprovalByIdentifier(UUID identifier) {
        var entities = fetchEntitiesFromIndexByIdentifier(identifier, GSI1);
        var handle = entities.stream()
                         .filter(HandleDao.class::isInstance)
                         .map(HandleDao.class::cast)
                         .findFirst()
                         .map(HandleDao::toHandle)
                         .orElseThrow();
        var identifiers = entities.stream()
                              .filter(IdentifierDao.class::isInstance)
                              .map(IdentifierDao.class::cast)
                              .map(IdentifierDao::toIdentifier)
                              .toList();
        var approval = entities.stream()
                           .filter(ApprovalDao.class::isInstance)
                           .map(ApprovalDao.class::cast)
                           .findFirst()
                           .orElseThrow();
        return new Approval(approval.identifier(), identifiers, approval.source(), handle);
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

    private void saveApproval(Approval approval) {
        var documents = createEntities(approval);
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

    private List<DatabaseEntity> fetchEntitiesFromIndexByIdentifier(UUID approvalIdentifier, String index) {
        return table.index(index)
                   .query(
                       QueryConditional.keyEqualTo(Key.builder().partitionValue(approvalIdentifier.toString()).build()))
                   .stream()
                   .map(Page::items)
                   .flatMap(List::stream)
                   .map(EnhancedDocument::toJson)
                   .map(this::toDatabaseEntity)
                   .toList();
    }

    private DatabaseEntity toDatabaseEntity(String value) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, DatabaseEntity.class)).orElseThrow();
    }

    private List<EnhancedDocument> createEntities(Approval approval) {
        var documents = new ArrayList<EnhancedDocument>();
        documents.add(createHandleEntity(approval));
        documents.add(createApprovalEntity(approval));
        documents.addAll(createIdentifiersEntities(approval));
        return documents;
    }

    private EnhancedDocument createApprovalEntity(Approval approval) {
        var approvalDao = ApprovalDao.fromApproval(approval);
        var handleDao = HandleDao.fromHandle(approval.getHandle());
        return EnhancedDocument.builder()
                   .json(approvalDao.toJsonString())
                   .put(PK0, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(SK0, approvalDao.getDatabaseIdentifier(), STRING)
                   .put(PK1, approval.getIdentifier().toString(), STRING)
                   .put(SK1, approval.getIdentifier().toString(), STRING)
                   .put(PK2, handleDao.getDatabaseIdentifier(), STRING)
                   .put(SK2, handleDao.getDatabaseIdentifier(), STRING)
                   .build();
    }

    private List<EnhancedDocument> createIdentifiersEntities(Approval approval) {
        return approval.getIdentifiers().stream().map(identifier -> createDocument(identifier, approval)).toList();
    }

    private EnhancedDocument createDocument(Identifier identifier, Approval approval) {
        var identifierDao = IdentifierDao.fromIdentifier(identifier);
        var handleDao = HandleDao.fromHandle(approval.getHandle());
        return EnhancedDocument.builder()
                   .json(identifierDao.toJsonString())
                   .put(PK0, identifierDao.getDatabaseIdentifier(), STRING)
                   .put(SK0, identifierDao.getDatabaseIdentifier(), STRING)
                   .put(PK1, approval.getIdentifier().toString(), STRING)
                   .put(SK1, approval.getIdentifier().toString(), STRING)
                   .put(PK2, handleDao.getDatabaseIdentifier(), STRING)
                   .put(SK2, handleDao.getDatabaseIdentifier(), STRING)
                   .build();
    }

    private EnhancedDocument createHandleEntity(Approval approval) {
        var handleDao = HandleDao.fromHandle(approval.getHandle());
        return EnhancedDocument.builder()
                   .json(handleDao.toJsonString())
                   .put(PK0, handleDao.getDatabaseIdentifier(), STRING)
                   .put(SK0, handleDao.getDatabaseIdentifier(), STRING)
                   .put(PK1, approval.getIdentifier().toString(), STRING)
                   .put(SK1, approval.getIdentifier().toString(), STRING)
                   .put(PK2, handleDao.getDatabaseIdentifier(), STRING)
                   .put(SK2, handleDao.getDatabaseIdentifier(), STRING)
                   .build();
    }
}
