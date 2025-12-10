package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE_NAME;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

class DynamoDbRepositoryTest {

    private Repository repository;
    private DynamoDbLocal dynamoDbLocal;

    @BeforeEach
    void setUp() {
        dynamoDbLocal = dynamoDBLocal();
        repository = new DynamoDbRepository(dynamoDbLocal.client());
    }

    @AfterEach
    void tearDown() {
        dynamoDbLocal.cleanTable();
    }

    @Test
    void shouldPersistApproval() throws RepositoryException {
        var approval = randomApproval(randomHandle());
        repository.save(approval);
        var persistedApproval = repository.findApprovalByIdentifier(approval.identifier());

        assertEquals(approval, persistedApproval.orElseThrow());
    }

    @Test
    void shouldThrowRepositoryExceptionWhenPersistingApprovalWithExistingIdentifier() throws RepositoryException {
        var identifier = randomIdentifier();
        var approval = randomApproval(identifier);
        repository.save(approval);

        assertThrows(RepositoryException.class, () -> repository.save(randomApproval(identifier)));
    }

    @Test
    void shouldThrowRepositoryExceptionWhenPersistingApprovalWithExistingHandle() throws RepositoryException {
        var handle = randomHandle();
        var approval = randomApproval(handle);
        repository.save(approval);

        assertThrows(RepositoryException.class, () -> repository.save(randomApproval(handle)));
    }

    @Test
    void shouldThrowRepositoryExceptionWhenPersistingApprovalWithExistingApprovalIdentifier()
        throws RepositoryException {
        var identifier = randomUUID();
        var approval = randomApproval(identifier, randomUri());
        repository.save(approval);

        assertThrows(RepositoryException.class, () -> repository.save(randomApproval(identifier, randomUri())));
    }

    @Test
    void shouldReturnEmptyOptionalWhenApprovalNotFound() throws RepositoryException {
        var result = repository.findApprovalByIdentifier(randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenHandleNotFoundInDatabase() {
        var approvalId = randomUUID();
        var identifierValue = randomString();

        insertIdentifierOnly(approvalId, identifierValue);

        assertThrows(RepositoryException.class, () -> repository.findApprovalByIdentifier(approvalId));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenApprovalNotFoundInDatabase() {
        var approvalId = randomUUID();
        var identifierValue = randomString();
        insertIdentifierOnly(approvalId, identifierValue);
        insertHandleOnly(approvalId, randomHandle().value().toString());

        assertThrows(RepositoryException.class, () -> repository.findApprovalByIdentifier(approvalId));
    }

    @Test
    void shouldFindApprovalByHandle() throws RepositoryException {
        var handle = randomHandle();
        var approval = randomApproval(handle);
        repository.save(approval);
        var persistedApproval = repository.findApprovalByHandle(handle);

        assertEquals(approval, persistedApproval.orElseThrow());
    }

    @Test
    void shouldFindApprovalByIdentifier() throws RepositoryException {
        var identifier = randomIdentifier();
        var approval = randomApproval(identifier);
        repository.save(approval);
        var persistedApproval = repository.findApprovalByIdentifier(identifier);

        assertEquals(approval, persistedApproval.orElseThrow());
    }

    private void insertIdentifierOnly(UUID approvalId, String identifierValue) {
        var item = createBaseItem(identifierValue, identifierValue, approvalId, identifierValue);
        item.put("type", AttributeValue.builder().s("Identifier").build());
        item.put("source", AttributeValue.builder().s(randomString()).build());
        item.put("value", AttributeValue.builder().s(identifierValue).build());

        dynamoDbLocal.client().putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
    }

    private void insertHandleOnly(UUID approvalId, String handleUri) {
        var item = createBaseItem(handleUri, handleUri, approvalId, handleUri);
        item.put("type", AttributeValue.builder().s("Handle").build());
        item.put("uri", AttributeValue.builder().s(handleUri).build());

        dynamoDbLocal.client().putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
    }

    private Map<String, AttributeValue> createBaseItem(String pk0, String sk0, UUID approvalId, String pk2Sk2) {
        var item = new HashMap<String, AttributeValue>();
        item.put(PK0, AttributeValue.builder().s(pk0).build());
        item.put(SK0, AttributeValue.builder().s(sk0).build());
        item.put(PK1, AttributeValue.builder().s(ApprovalDao.toDatabaseIdentifier(approvalId)).build());
        item.put(SK1, AttributeValue.builder().s(ApprovalDao.toDatabaseIdentifier(approvalId)).build());
        item.put(PK2, AttributeValue.builder().s(pk2Sk2).build());
        item.put(SK2, AttributeValue.builder().s(pk2Sk2).build());
        return item;
    }
}
