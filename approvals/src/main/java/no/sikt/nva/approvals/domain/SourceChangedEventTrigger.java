package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeName(SourceChangedEventTrigger.TYPE)
public record SourceChangedEventTrigger(SortableIdentifier identifier, String cloudEventIdentifier)
    implements Trigger {

  public static final String TYPE = "SourceChangedEvent";
  public static final String SOURCE_CHANGE_ACTIVITY_TYPE = "SourceChange";

  @Override
  public String activityType() {
    return SOURCE_CHANGE_ACTIVITY_TYPE;
  }
}
