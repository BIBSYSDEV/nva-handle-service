package no.sikt.nva.approvals.persistence;

import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbRepository implements Repository {

    private final DynamoDbEnhancedClient client;

    public DynamoDbRepository(DynamoDbClient client) {
        this.client = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    @Override
    public void save(Approval approval) {

    }

    @Override
    public Approval getApprovalByIdentifier(UUID identifier) {
        return null;
    }
}
