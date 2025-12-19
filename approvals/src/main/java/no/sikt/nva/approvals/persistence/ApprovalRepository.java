package no.sikt.nva.approvals.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;

public interface ApprovalRepository {

    void save(Approval approval);

    void updateApprovalIdentifiers(Approval approval);

    Optional<Approval> findByApprovalIdentifier(UUID approvalIdentifier);

    Optional<Approval> findByHandle(Handle handle);

    Optional<Approval> findByIdentifier(NamedIdentifier namedIdentifier);

    List<NamedIdentifierQueryObject> findIdentifiers(Collection<NamedIdentifier> namedIdentifiers);
}
