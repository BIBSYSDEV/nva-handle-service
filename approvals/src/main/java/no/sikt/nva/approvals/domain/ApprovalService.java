package no.sikt.nva.approvals.domain;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalService {

    Approval create(Collection<NamedIdentifier> namedIdentifiers, URI source)
        throws ApprovalServiceException, ApprovalConflictException;

    Optional<Approval> getApprovalByIdentifier(UUID approvalId);

    Optional<Approval> getApprovalByHandle(Handle handle);

    Optional<Approval> getApprovalByNamedIdentifier(NamedIdentifier namedIdentifier);

    Approval updateApprovalIdentifiers(UUID approvalId, Collection<NamedIdentifier> namedIdentifiers)
        throws ApprovalServiceException, ApprovalConflictException;
}
