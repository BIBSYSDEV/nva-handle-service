package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE_NAME;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.Identifier;
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
        var approval = randomApproval();
        repository.save(approval);

        var persistedApproval = repository.getApprovalByIdentifier(approval.identifier());

        assertEquals(approval, persistedApproval);
    }

    @Test
    void shouldThrowRepositoryExceptionWhenPersistingApprovalWithExistingIdentifier() throws RepositoryException {
        var identifier = randomIdentifier();
        var approval = approvalWithIdentifier(identifier);
        repository.save(approval);

        assertThrows(RepositoryException.class, () -> repository.save(approvalWithIdentifier(identifier)));
    }

    @Test
    void shouldThrowRepositoryExceptionWhenPersistingApprovalWithExistingHandle() throws RepositoryException {
        var handle = randomHandle();
        var approval = approvalWithHandle(handle);
        repository.save(approval);

        assertThrows(RepositoryException.class, () -> repository.save(approvalWithHandle(handle)));
    }

    @Test
    void shouldThrowRepositoryExceptionWhenPersistingApprovalWithExistingApprovalId() throws RepositoryException {
        var identifier = randomUUID();
        var approval = approvalWithIdentifier(identifier);
        repository.save(approval);

        assertThrows(RepositoryException.class, () -> repository.save(approvalWithIdentifier(identifier)));
    }

    private static Approval randomApproval() {
        return new Approval(randomUUID(), randomIdentifiers(), randomUri(), randomHandle());
    }

    private static Approval approvalWithIdentifier(Identifier identifier) {
        return new Approval(randomUUID(), List.of(identifier), randomUri(), randomHandle());
    }

    private static Approval approvalWithHandle(Handle handle) {
        return new Approval(randomUUID(), List.of(randomIdentifier()), randomUri(), handle);
    }

    private static Approval approvalWithIdentifier(UUID identifier) {
        return new Approval(identifier, List.of(randomIdentifier()), randomUri(), randomHandle());
    }

    private static Handle randomHandle() {
        return new Handle(URI.create("https://www.handle.net/123/123"));
    }

    private static List<Identifier> randomIdentifiers() {
        return List.of(randomIdentifier());
    }

    private static Identifier randomIdentifier() {
        return new Identifier(randomString(), randomString());
    }
}
