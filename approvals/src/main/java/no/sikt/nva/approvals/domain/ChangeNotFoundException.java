package no.sikt.nva.approvals.domain;

import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;

public class ChangeNotFoundException extends ApprovalServiceException {

  private static final String MESSAGE = "Change %s not found for approval %s";

  public ChangeNotFoundException(UUID approvalIdentifier, SortableIdentifier changeIdentifier) {
    super(MESSAGE.formatted(changeIdentifier, approvalIdentifier));
  }
}
