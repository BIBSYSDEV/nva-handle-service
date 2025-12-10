package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE_NAME;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamoDbRepositoryTest {

    public static final AmazonDynamoDBLocal database = DynamoDBEmbedded.create();
    private Repository repository;
    private DynamoDbLocal dynamoDbLocal;

    @BeforeEach
    void setUp() {
        dynamoDbLocal = dynamoDBLocal(database, TABLE_NAME);
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

        var persistedApproval = repository.getApprovalByIdentifier(approval.identifier());

        assertEquals(approval, persistedApproval);
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
    void shouldThrowRepositoryExceptionWhenPersistingApprovalWithExistingApprovalId() throws RepositoryException {
        var identifier = randomUUID();
        var approval = randomApproval(identifier, randomUri());
        repository.save(approval);

        assertThrows(RepositoryException.class, () -> repository.save(randomApproval(identifier, randomUri())));
    }
}
