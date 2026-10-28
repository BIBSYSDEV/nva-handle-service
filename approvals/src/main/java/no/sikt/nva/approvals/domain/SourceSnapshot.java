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
    return new SourceSnapshot(
        createIdentifier(createSnapshotRequest),
        createSnapshotRequest.approvalIdentifier(),
        createSnapshotRequest.agent(),
        createSnapshotRequest.trigger(),
        createSnapshotRequest.source(),
        content,
        createdDate.truncatedTo(MICROS));
  }

  /**
   * The identifier is derived from the approval and event, so a redelivered message resolves to the
   * same snapshot and the conditional write stores it only once.
   */
  private static SortableIdentifier createIdentifier(CreateSnapshotRequest createSnapshotRequest) {
    var seed =
        IDENTIFIER_SEED.formatted(
            createSnapshotRequest.approvalIdentifier(), createSnapshotRequest.trigger().identifier());
    var uuid = UUID.nameUUIDFromBytes(seed.getBytes(UTF_8));
    return SortableIdentifier.create(createSnapshotRequest.timestamp(), uuid);
  }
}
