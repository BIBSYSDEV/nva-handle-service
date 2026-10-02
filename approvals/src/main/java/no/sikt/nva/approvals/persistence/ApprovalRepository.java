package no.sikt.nva.approvals.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.events.SourceChangedEvent;

public interface ApprovalRepository {

  void save(ApprovalRevision revision);

  void save(SourceChangedEvent event, UUID approvalIdentifier);

  void save(SourceSnapshot snapshot);

  void updateApproval(ApprovalRevision revision);

  Optional<Approval> findByApprovalIdentifier(UUID approvalIdentifier);

  Optional<Approval> findByHandle(Handle handle);

  Optional<Approval> findByIdentifier(NamedIdentifier namedIdentifier);

  List<NamedIdentifierQueryObject> findIdentifiers(Collection<NamedIdentifier> namedIdentifiers);

  Optional<IdentifierPolicy> findIdentifierPolicy(UUID customerIdentifier);

  void saveIdentifierPolicy(UUID customerIdentifier, IdentifierPolicy identifierPolicy);
}
