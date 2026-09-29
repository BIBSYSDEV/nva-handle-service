package no.sikt.nva.approvals.domain;

public class SourceMismatchException extends ApprovalServiceException {

  private static final String MESSAGE =
      "Source provided in request does not match requested approval source";

  public SourceMismatchException() {
    super(MESSAGE);
  }
}
