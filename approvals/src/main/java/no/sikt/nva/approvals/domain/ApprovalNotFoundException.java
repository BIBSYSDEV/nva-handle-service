package no.sikt.nva.approvals.domain;

import java.util.UUID;

public class ApprovalNotFoundException extends ApprovalServiceException {

  private static final String MESSAGE = "Approval not found for identifier %s";

  public ApprovalNotFoundException(UUID approvalId) {
    super(MESSAGE.formatted(approvalId));
  }
}
