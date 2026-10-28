package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.UriConstants.eventId;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.sikt.nva.approvals.domain.SourceChangedEventTrigger;
import no.sikt.nva.approvals.domain.SourceSnapshot;

@JsonTypeName("SourceIngestion")
public record SourceIngestion(
    URI used, SourceChangedEvent wasStartedBy, SoftwareAgent wasAssociatedWith)
    implements Activity {

  static SourceIngestion create(SourceSnapshot snapshot, String apiHost) {
    return new SourceIngestion(
        snapshot.source(),
        new SourceChangedEvent(
            eventId(apiHost, snapshot.approvalIdentifier(), snapshot.activity().identifier()),
            snapshot.activity() instanceof SourceChangedEventTrigger trigger
                ? trigger.cloudEventIdentifier()
                : null),
        SoftwareAgent.actingFor(snapshot.agent(), apiHost));
  }

  @JsonTypeInfo(use = Id.NAME, property = "type")
  @JsonTypeName("SourceChangedEvent")
  public record SourceChangedEvent(URI id, String cloudEventIdentifier) {}
}
