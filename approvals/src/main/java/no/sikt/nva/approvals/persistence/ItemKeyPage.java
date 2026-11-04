package no.sikt.nva.approvals.persistence;

import java.util.List;
import java.util.Optional;

public record ItemKeyPage(List<ItemKey> keys, ScanCursor nextCursor) {

  public Optional<ScanCursor> next() {
    return Optional.ofNullable(nextCursor);
  }
}
