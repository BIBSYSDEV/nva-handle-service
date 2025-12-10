package no.sikt.nva.approvals.persistence;

import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;

public interface ApprovalRepository {

    void save(Approval approval) throws RepositoryException;

    Optional<Approval> findByApprovalIdentifier(UUID approvalIdentifier) throws RepositoryException;

    Optional<Approval> findByHandle(Handle handle) throws RepositoryException;

    Optional<Approval> findByIdentifier(NamedIdentifier namedIdentifier) throws RepositoryException;
}
