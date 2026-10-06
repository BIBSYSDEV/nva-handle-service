package no.sikt.nva.approvals.domain;

import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;

public interface ChangeService {

  Change fetchChange(UUID approvalIdentifier, SortableIdentifier changeIdentifier)
      throws ChangeNotFoundException;

  ChangeList listChangesByApproval(UUID approvalIdentifier, SortableIdentifier cursor)
      throws ApprovalNotFoundException, InvalidCursorException;
}
