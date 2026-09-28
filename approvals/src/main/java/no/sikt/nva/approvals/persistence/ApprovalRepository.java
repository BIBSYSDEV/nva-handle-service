package no.sikt.nva.approvals.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.events.ApprovalEvent;

public interface ApprovalRepository {

  void save(Approval approval);

  void updateApproval(Approval approval);

  Optional<Approval> findByApprovalIdentifier(UUID approvalIdentifier);

  Optional<Approval> findByHandle(Handle handle);

  Optional<Approval> findByIdentifier(NamedIdentifier namedIdentifier);

  List<NamedIdentifierQueryObject> findIdentifiers(Collection<NamedIdentifier> namedIdentifiers);

  Optional<IdentifierPolicy> findIdentifierPolicy(UUID customerIdentifier);

  void saveIdentifierPolicy(UUID customerIdentifier, IdentifierPolicy identifierPolicy);

  /**
   * Stores the event together with a pending marker for its pull, unless an event with the same key
   * is already stored.
   *
   * @return true when the event was stored, false when it had been stored before
   */
  boolean saveEventIfAbsent(ApprovalEvent approvalEvent);
}
