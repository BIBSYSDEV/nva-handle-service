package no.sikt.nva.approvals.persistence;

import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Change;
import no.sikt.nva.approvals.domain.ChangeList;
import no.unit.nva.identifiers.SortableIdentifier;

public interface ChangeRepository {

  ChangeList listChangesByApproval(UUID approvalIdentifier, SortableIdentifier after, int pageSize);

  Optional<Change> findChange(UUID approvalIdentifier, SortableIdentifier changeIdentifier);
}
