package no.sikt.nva.approvals.domain;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.MICROS;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.snapshot.CreateSnapshotRequest;
import no.unit.nva.identifiers.SortableIdentifier;

public record SourceSnapshot(
    SortableIdentifier identifier,
    UUID approvalIdentifier,
    Agent agent,
    Trigger activity,
    URI source,
    Content content,
    Instant createdDate)
    implements Change {

  private static final String IDENTIFIER_SEED = "%s:%s";

  public static SourceSnapshot create(
      CreateSnapshotRequest createSnapshotRequest, Content content, Instant createdDate) {
    var truncatedCreatedDate = createdDate.truncatedTo(MICROS);
    return new SourceSnapshot(
        createIdentifier(createSnapshotRequest, truncatedCreatedDate),
        createSnapshotRequest.approvalIdentifier(),
        createSnapshotRequest.agent(),
        createSnapshotRequest.trigger(),
        createSnapshotRequest.source(),
        content,
        truncatedCreatedDate);
  }

  /**
   * The identifier is ordered by when the snapshot was created, so the latest snapshot is the one
   * saved last. The rest of the identifier is derived from the approval and event, so a redelivered
   * message created at the same time resolves to the same snapshot.
   */
  private static SortableIdentifier createIdentifier(
      CreateSnapshotRequest createSnapshotRequest, Instant createdDate) {
    var seed =
        IDENTIFIER_SEED.formatted(
            createSnapshotRequest.approvalIdentifier(),
            createSnapshotRequest.trigger().identifier());
    var uuid = UUID.nameUUIDFromBytes(seed.getBytes(UTF_8));
    return SortableIdentifier.create(createdDate, uuid);
  }
}
