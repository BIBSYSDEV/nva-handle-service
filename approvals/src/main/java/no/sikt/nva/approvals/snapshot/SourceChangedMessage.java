package no.sikt.nva.approvals.snapshot;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.SourceChangedEventTrigger;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeName(SourceChangedMessage.TYPE)
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

  public static final String TYPE = "SourceChangedEvent";

  @Override
  public SourceChange toSourceChange() {
    return new SourceChange(
        approvalIdentifier,
        new Agent(customerIdentifier, clientIdentifier),
        source,
        new SourceChangedEventTrigger(eventIdentifier, cloudEventIdentifier),
        timestamp);
  }
}
