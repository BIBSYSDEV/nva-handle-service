package no.sikt.nva.approvals.domain;

import java.util.UUID;

public class ApprovalModifiedException extends RuntimeException {

  private static final String MESSAGE =
      "Approval %s was modified by another request. Read it again and retry.";

  public ApprovalModifiedException(UUID approvalIdentifier) {
    super(MESSAGE.formatted(approvalIdentifier));
  }
}
