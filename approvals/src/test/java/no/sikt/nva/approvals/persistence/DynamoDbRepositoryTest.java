package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE_NAME;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.AssertionsKt.assertNull;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
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
        repository = new DynamoDbRepository();
    }

    @AfterEach
    void tearDown() {
        dynamoDbLocal.cleanTable();
    }

    @Test
    void shouldSaveApproval() {
        var approval = new Approval(UUID.randomUUID(), List.of(), randomUri());

        repository.save(approval);

        var fetchedApproval = repository.getApprovalByIdentifier(UUID.randomUUID());

        assertNull(fetchedApproval);
    }
}