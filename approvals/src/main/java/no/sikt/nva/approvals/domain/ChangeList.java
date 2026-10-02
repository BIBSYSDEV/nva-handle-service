package no.sikt.nva.approvals.domain;

import java.util.List;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;

public record ChangeList(List<Change> changes, boolean hasMore) {

  public Optional<SortableIdentifier> next() {
    return hasMore ? Optional.of(changes.getLast().identifier()) : Optional.empty();
  }
}
