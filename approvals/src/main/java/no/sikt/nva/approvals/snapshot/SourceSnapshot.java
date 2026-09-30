package no.sikt.nva.approvals.snapshot;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record SourceSnapshot(
    UUID identifier,
    UUID approvalIdentifier,
    String eventIdentifier,
    URI source,
    Instant timestamp) {

  private static final String IDENTIFIER_SEED = "%s:%s";

  /**
   * The identifier is derived from the approval and event, so a redelivered message resolves to the
   * same snapshot and the conditional write stores it only once.
   */
  public static SourceSnapshot create(SourceChange sourceChange, Instant timestamp) {
    var seed =
        IDENTIFIER_SEED.formatted(
            sourceChange.approvalIdentifier(), sourceChange.eventIdentifier());
    return new SourceSnapshot(
        UUID.nameUUIDFromBytes(seed.getBytes(UTF_8)),
        sourceChange.approvalIdentifier(),
        sourceChange.eventIdentifier(),
        sourceChange.source(),
        timestamp);
  }
}
