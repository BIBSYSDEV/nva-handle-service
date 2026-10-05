package no.sikt.nva.approvals.domain;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.util.UUID;
import no.sikt.nva.approvals.snapshot.SourceChange;
import no.unit.nva.identifiers.SortableIdentifier;

public record SourceSnapshot(
    SortableIdentifier identifier, UUID approvalIdentifier, String eventIdentifier, URI source)
    implements Change {

  private static final String IDENTIFIER_SEED = "%s:%s";

  public static SourceSnapshot create(SourceChange sourceChange) {
    return new SourceSnapshot(
        createIdentifier(sourceChange),
        sourceChange.approvalIdentifier(),
        sourceChange.eventIdentifier(),
        sourceChange.source());
  }

  /**
   * The identifier is derived from the approval and event, so a redelivered message resolves to the
   * same snapshot and the conditional write stores it only once.
   */
  private static SortableIdentifier createIdentifier(SourceChange sourceChange) {
    var seed =
        IDENTIFIER_SEED.formatted(
            sourceChange.approvalIdentifier(), sourceChange.eventIdentifier());
    var uuid = UUID.nameUUIDFromBytes(seed.getBytes(UTF_8));
    return SortableIdentifier.create(sourceChange.timestamp(), uuid);
  }
}
