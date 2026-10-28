package no.sikt.nva.approvals.domain;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.MICROS;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.snapshot.SourceChange;
import no.unit.nva.identifiers.SortableIdentifier;

public record SourceSnapshot(
    SortableIdentifier identifier,
    UUID approvalIdentifier,
    Agent agent,
    SnapshotTrigger activity,
    URI source,
    Content content,
    Instant createdDate)
    implements Change {

  private static final String IDENTIFIER_SEED = "%s:%s";

  public static SourceSnapshot create(
      SourceChange sourceChange, Content content, Instant createdDate) {
    return new SourceSnapshot(
        createIdentifier(sourceChange),
        sourceChange.approvalIdentifier(),
        sourceChange.agent(),
        sourceChange.trigger(),
        sourceChange.source(),
        content,
        createdDate.truncatedTo(MICROS));
  }

  /**
   * The identifier is derived from the approval and event, so a redelivered message resolves to the
   * same snapshot and the conditional write stores it only once.
   */
  private static SortableIdentifier createIdentifier(SourceChange sourceChange) {
    var seed =
        IDENTIFIER_SEED.formatted(
            sourceChange.approvalIdentifier(), sourceChange.trigger().identifier());
    var uuid = UUID.nameUUIDFromBytes(seed.getBytes(UTF_8));
    return SortableIdentifier.create(sourceChange.timestamp(), uuid);
  }
}
