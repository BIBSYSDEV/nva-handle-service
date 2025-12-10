package no.sikt.nva.approvals.persistence;

import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.Identifier;

public interface Repository {

    void save(Approval approval) throws RepositoryException;

    Optional<Approval> findApprovalByIdentifier(UUID identifier) throws RepositoryException;

    Optional<Approval> findApprovalByHandle(Handle handle) throws RepositoryException;

    Optional<Approval> findApprovalByIdentifier(Identifier identifier) throws RepositoryException;
}
