package no.sikt.nva.approvals.domain;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;

public interface ApprovalService {

  Approval create(Collection<NamedIdentifier> namedIdentifiers, URI source, Agent agent)
      throws ApprovalServiceException, ApprovalConflictException;

  Optional<Approval> getApprovalByIdentifier(UUID approvalId);

  Approval updateApproval(
      UUID approvalId,
      Collection<NamedIdentifier> namedIdentifiers,
      URI source,
      Agent agent,
      SortableIdentifier expectedVersion)
      throws ApprovalServiceException, ApprovalConflictException;
}
