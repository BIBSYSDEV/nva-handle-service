package no.sikt.nva.approvals.domain;

import java.util.UUID;

public class CustomerMismatchException extends ApprovalServiceException {

  private static final String MESSAGE = "Customer is not allowed to update approval %s";

  public CustomerMismatchException(UUID approvalId) {
    super(MESSAGE.formatted(approvalId));
  }
}
