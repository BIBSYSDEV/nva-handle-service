package no.sikt.nva.approvals.persistence;

import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;

public interface Repository {

    void save(Approval approval) throws RepositoryException;

    Optional<Approval> finalApprovalByIdentifier(UUID identifier);
}
