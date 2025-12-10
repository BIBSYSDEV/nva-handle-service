package no.sikt.nva.approvals.persistence;

import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;

public interface Repository {

    void save(Approval approval);

    Approval getApprovalByIdentifier(UUID identifier);
}