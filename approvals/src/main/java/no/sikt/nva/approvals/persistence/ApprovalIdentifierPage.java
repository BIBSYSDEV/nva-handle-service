package no.sikt.nva.approvals.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record ApprovalIdentifierPage(List<UUID> identifiers, ScanCursor nextCursor) {

  public Optional<ScanCursor> next() {
    return Optional.ofNullable(nextCursor);
  }
}
