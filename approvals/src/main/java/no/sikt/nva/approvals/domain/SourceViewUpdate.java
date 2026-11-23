package no.sikt.nva.approvals.domain;

import static java.util.Objects.requireNonNull;

import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;

public record SourceViewUpdate(
    UUID approvalIdentifier,
    SortableIdentifier snapshotIdentifier,
    String contentHash,
    int viewVersion,
    SourceView view) {

  public SourceViewUpdate {
    requireNonNull(approvalIdentifier);
    requireNonNull(snapshotIdentifier);
    requireNonNull(contentHash);
    requireNonNull(view);
  }
}
