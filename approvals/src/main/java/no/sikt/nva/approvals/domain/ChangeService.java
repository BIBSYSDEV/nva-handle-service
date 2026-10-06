package no.sikt.nva.approvals.domain;

import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;

@FunctionalInterface
public interface ChangeService {

  ChangeList listChangesByApproval(UUID approvalIdentifier, SortableIdentifier cursor)
      throws ApprovalNotFoundException, InvalidCursorException;
}
