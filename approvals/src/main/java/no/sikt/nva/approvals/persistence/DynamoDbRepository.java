package no.sikt.nva.approvals.persistence;

import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;

public class DynamoDbRepository implements Repository {

    public DynamoDbRepository() {
    }

    @Override
    public void save(Approval approval) {

    }

    @Override
    public Approval getApprovalByIdentifier(UUID identifier) {
        return null;
    }
}
