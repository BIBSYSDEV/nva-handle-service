package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeName(SourceChangedEventTrigger.TYPE)
public record SourceChangedEventTrigger(SortableIdentifier identifier, String cloudEventIdentifier)
    implements SnapshotTrigger {

  public static final String TYPE = "SourceChangedEvent";
}
