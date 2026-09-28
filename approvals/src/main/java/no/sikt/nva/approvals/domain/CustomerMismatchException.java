package no.sikt.nva.approvals.domain;

public class CustomerMismatchException extends ApprovalServiceException {

  private static final String MESSAGE = "Customer id does not match requested approval customer id";

  public CustomerMismatchException() {
    super(MESSAGE);
  }
}
