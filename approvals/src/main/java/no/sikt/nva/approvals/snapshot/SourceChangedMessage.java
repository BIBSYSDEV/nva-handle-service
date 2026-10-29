package no.sikt.nva.approvals.snapshot;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.SourceChangedEventTrigger;
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
  public CreateSnapshotRequest toCreateSnapshotRequest() {
    return new CreateSnapshotRequest(
        approvalIdentifier,
        new Agent(customerIdentifier, clientIdentifier),
        source,
        new SourceChangedEventTrigger(eventIdentifier, cloudEventIdentifier),
        timestamp);
  }
}
