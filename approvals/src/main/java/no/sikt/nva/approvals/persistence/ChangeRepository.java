package no.sikt.nva.approvals.persistence;

import java.util.UUID;
import no.sikt.nva.approvals.domain.ChangeList;
import no.unit.nva.identifiers.SortableIdentifier;

@FunctionalInterface
public interface ChangeRepository {

  ChangeList listChangesByApproval(UUID approvalIdentifier, SortableIdentifier after, int pageSize);
}
