package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK2;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

class DynamoDbApprovalRepositoryTest {

    private static final Environment ENVIRONMENT = new Environment();
    private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
    private ApprovalRepository approvalRepository;
    private DynamoDbLocal dynamoDbLocal;

    @BeforeEach
    void setUp() {
        dynamoDbLocal = dynamoDBLocal(TABLE);
        approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
    }

    @AfterEach
    void tearDown() {
        dynamoDbLocal.cleanTable(TABLE);
    }

    @Test
    void shouldPersistApproval() {
        var approval = randomApproval(randomHandle());
        approvalRepository.save(approval);
        var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

        assertEquals(approval, persistedApproval.orElseThrow());
    }

    @Test
    void shouldThrowExceptionWhenPersistingApprovalWithExistingIdentifier() {
        var identifier = randomIdentifier();
        var approval = randomApproval(identifier);
        approvalRepository.save(approval);

        assertThrows(Exception.class, () -> approvalRepository.save(randomApproval(identifier)));
    }

    @Test
    void shouldThrowExceptionWhenPersistingApprovalWithExistingHandle() {
        var handle = randomHandle();
        var approval = randomApproval(handle);
        approvalRepository.save(approval);

        assertThrows(Exception.class, () -> approvalRepository.save(randomApproval(handle)));
    }

    @Test
    void shouldThrowExceptionWhenPersistingApprovalWithExistingApprovalIdentifier() {
        var identifier = randomUUID();
        var approval = randomApproval(identifier, randomUri());
        approvalRepository.save(approval);

        assertThrows(Exception.class, () -> approvalRepository.save(randomApproval(identifier, randomUri())));
    }

    @Test
    void shouldReturnEmptyOptionalWhenApprovalNotFound() {
        var result = approvalRepository.findByApprovalIdentifier(randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenHandleNotFoundInDatabase() {
        var approvalId = randomUUID();
        var identifierValue = randomString();

        insertIdentifierOnly(approvalId, identifierValue);

        assertThrows(IllegalStateException.class, () -> approvalRepository.findByApprovalIdentifier(approvalId));
    }

    @Test
    void shouldThrowExceptionWhenApprovalNotFoundInDatabase() {
        var approvalId = randomUUID();
        var identifierValue = randomString();
        insertIdentifierOnly(approvalId, identifierValue);
        insertHandleOnly(approvalId, randomHandle().value().toString());

        assertThrows(IllegalStateException.class, () -> approvalRepository.findByApprovalIdentifier(approvalId));
    }

    @Test
    void shouldFindByHandle() {
        var handle = randomHandle();
        var approval = randomApproval(handle);
        approvalRepository.save(approval);
        var persistedApproval = approvalRepository.findByHandle(handle);

        assertEquals(approval, persistedApproval.orElseThrow());
    }

    @Test
    void shouldFindByApprovalIdentifier() {
        var identifier = randomIdentifier();
        var approval = randomApproval(identifier);
        approvalRepository.save(approval);
        var persistedApproval = approvalRepository.findByIdentifier(identifier);

        assertEquals(approval, persistedApproval.orElseThrow());
    }

    @Test
    void shouldFindIdentifiers() {
        var identifiers = randomIdentifiers();
        var approval = randomApproval(identifiers, randomUUID());
        approvalRepository.save(approval);
        var persistedIdentifiers = approvalRepository.findIdentifiers(identifiers);

        assertEquals(identifiers, persistedIdentifiers.stream().map(NamedIdentifierQueryObject::toNamedIdentifier).toList());
    }

    @Test
    void shouldSaveAndFindIdentifiersWhenMoreThan100Provided() {
        var identifiers = randomIdentifiers(110);
        var approval = randomApproval(identifiers, randomUUID());
        approvalRepository.save(approval);
        var persistedIdentifiers = approvalRepository.findIdentifiers(identifiers);

        assertEquals(identifiers.size(), persistedIdentifiers.size());
    }

    @Test
    void shouldUpdateApprovalIdentifiersByAddingNewIdentifiers() {
        var initialIdentifiers = randomIdentifiers(2);
        var approval = randomApproval(initialIdentifiers, randomUUID());
        approvalRepository.save(approval);

        var newIdentifiers = randomIdentifiers(3);
        var allIdentifiers = new java.util.ArrayList<>(initialIdentifiers);
        allIdentifiers.addAll(newIdentifiers);
        var updatedApproval = new Approval(approval.identifier(), allIdentifiers, approval.source(), approval.handle());

        approvalRepository.updateApprovalIdentifiers(updatedApproval);
        var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

        assertTrue(persistedApproval.orElseThrow().namedIdentifiers().containsAll(allIdentifiers));
    }

    @Test
    void shouldUpdateApprovalIdentifiersByRemovingIdentifiers() {
        var initialIdentifiers = randomIdentifiers(5);
        var approval = randomApproval(initialIdentifiers, randomUUID());
        approvalRepository.save(approval);

        var remainingIdentifiers = initialIdentifiers.stream().limit(2).toList();
        var updatedApproval = new Approval(approval.identifier(), remainingIdentifiers, approval.source(),
            approval.handle());

        approvalRepository.updateApprovalIdentifiers(updatedApproval);
        var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

        assertTrue(persistedApproval.orElseThrow().namedIdentifiers().containsAll(remainingIdentifiers));
    }

    @Test
    void shouldUpdateApprovalIdentifiersByAddingAndRemovingIdentifiers() {
        var initialIdentifiers = randomIdentifiers(3);
        var approval = randomApproval(initialIdentifiers, randomUUID());
        approvalRepository.save(approval);

        var keptIdentifiers = initialIdentifiers.stream().limit(1).toList();
        var newIdentifiers = randomIdentifiers(2);
        var finalIdentifiers = new ArrayList<>(keptIdentifiers);
        finalIdentifiers.addAll(newIdentifiers);

        var updatedApproval = new Approval(approval.identifier(), finalIdentifiers, approval.source(), approval.handle());

        approvalRepository.updateApprovalIdentifiers(updatedApproval);
        var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

        assertTrue(persistedApproval.orElseThrow().namedIdentifiers().containsAll(finalIdentifiers));
    }

    @Test
    void shouldUpdateApprovalIdentifiersWhenMoreThen80ItemsToModifyInTransaction() {
        var initialIdentifiers = randomIdentifiers(50);
        var approval = randomApproval(initialIdentifiers, randomUUID());
        approvalRepository.save(approval);

        var keptIdentifiers = initialIdentifiers.stream().limit(10).toList();
        var newIdentifiers = randomIdentifiers(60);
        var finalIdentifiers = new ArrayList<>(keptIdentifiers);
        finalIdentifiers.addAll(newIdentifiers);

        var updatedApproval = new Approval(
            approval.identifier(),
            finalIdentifiers,
            approval.source(),
            approval.handle()
        );

        approvalRepository.updateApprovalIdentifiers(updatedApproval);
        var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

        assertTrue(persistedApproval.orElseThrow().namedIdentifiers().containsAll(finalIdentifiers));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentApproval() {
        var approval = randomApproval(randomHandle());

        assertThrows(IllegalStateException.class, () -> approvalRepository.updateApprovalIdentifiers(approval));
    }

    @Test
    void shouldThrowTransactionCanceledExceptionWhenAddingIdentifierThatAlreadyExistsInAnotherApproval() {
        var sharedIdentifier = randomIdentifier();
        var firstApproval = randomApproval(sharedIdentifier);
        approvalRepository.save(firstApproval);

        var secondApproval = randomApproval(randomHandle());
        approvalRepository.save(secondApproval);

        var updatedSecondApproval = new Approval(
            secondApproval.identifier(),
            List.of(sharedIdentifier),
            secondApproval.source(),
            secondApproval.handle()
        );

        assertThrows(TransactionCanceledException.class, () -> approvalRepository.updateApprovalIdentifiers(updatedSecondApproval));
    }

    private void insertIdentifierOnly(UUID approvalId, String identifierValue) {
        var item = createBaseItem(identifierValue, identifierValue, approvalId, identifierValue);
        item.put("type", AttributeValue.builder().s("Identifier").build());
        item.put("name", AttributeValue.builder().s(randomString()).build());
        item.put("value", AttributeValue.builder().s(identifierValue).build());

        dynamoDbLocal.client().putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
    }

    private void insertHandleOnly(UUID approvalId, String handleUri) {
        var item = createBaseItem(handleUri, handleUri, approvalId, handleUri);
        item.put("type", AttributeValue.builder().s("Handle").build());
        item.put("uri", AttributeValue.builder().s(handleUri).build());

        dynamoDbLocal.client().putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
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
