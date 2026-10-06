package no.sikt.nva.approvals.domain;

import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;

public class InvalidCursorException extends ApprovalServiceException {

  private static final String MESSAGE = "Cursor %s is not a change of approval %s";

  public InvalidCursorException(UUID approvalIdentifier, SortableIdentifier cursor) {
    super(MESSAGE.formatted(cursor, approvalIdentifier));
  }
}
