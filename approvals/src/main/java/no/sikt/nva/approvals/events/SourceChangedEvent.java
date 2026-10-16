package no.sikt.nva.approvals.events;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.Handle;
import no.unit.nva.identifiers.SortableIdentifier;

public record SourceChangedEvent(
    SortableIdentifier identifier,
    String eventIdentifier,
    URI source,
    Handle handle,
    Instant timestamp,
    Agent agent) {

  private static final String IDENTIFIER_SEED = "%s:%s:%s";

  public static SourceChangedEvent create(
      String eventIdentifier, URI source, Handle handle, Instant timestamp, Agent agent) {
    return new SourceChangedEvent(
        createIdentifier(eventIdentifier, handle, timestamp, agent),
        eventIdentifier,
        source,
        handle,
        timestamp,
        agent);
  }

  /**
   * The identifier is derived from the event, the handle and the customer, placed under the event
   * timestamp, so a redelivery reporting the same timestamp resolves to the same item and the
   * conditional write stores it only once.
   */
  private static SortableIdentifier createIdentifier(
      String eventIdentifier, Handle handle, Instant timestamp, Agent agent) {
    var seed =
        IDENTIFIER_SEED.formatted(eventIdentifier, handle.value(), agent.customerIdentifier());
    var uuid = UUID.nameUUIDFromBytes(seed.getBytes(UTF_8));
    return SortableIdentifier.create(timestamp, uuid);
  }
}
