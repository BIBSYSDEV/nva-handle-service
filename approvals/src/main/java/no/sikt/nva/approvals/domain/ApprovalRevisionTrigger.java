package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeName("ApprovalRevision")
public record ApprovalRevisionTrigger(SortableIdentifier identifier, ApprovalActivity activity)
    implements Trigger {

  @Override
  public String activityType() {
    return activity.getValue();
  }
}
