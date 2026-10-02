package no.sikt.nva.approvals.domain;

import java.util.UUID;

public class ApprovalNotFoundException extends ApprovalServiceException {

  private static final String MESSAGE = "Approval not found for identifier %s";
  private static final String HANDLE_MESSAGE = "Approval not found for handle %s";

  public ApprovalNotFoundException(UUID approvalId) {
    super(MESSAGE.formatted(approvalId));
  }

  public ApprovalNotFoundException(Handle handle) {
    super(HANDLE_MESSAGE.formatted(handle));
  }
}
