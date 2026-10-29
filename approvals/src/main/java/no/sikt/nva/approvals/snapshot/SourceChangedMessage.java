package no.sikt.nva.approvals.snapshot;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeName("SourceChangedEvent")
public record SourceChangedMessage(
    SortableIdentifier eventIdentifier,
    String cloudEventIdentifier,
    UUID approvalIdentifier,
    UUID customerIdentifier,
    String clientIdentifier,
    URI handle,
    URI source,
    Instant timestamp)
    implements CreateSnapshotMessage {

  @Override
  public SourceChange toSourceChange() {
    return new SourceChange(
        approvalIdentifier,
        new Agent(customerIdentifier, clientIdentifier),
        eventIdentifier,
        cloudEventIdentifier,
        source,
        timestamp);
  }
}
