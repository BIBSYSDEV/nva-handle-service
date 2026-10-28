package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeName(ApprovalRevisionTrigger.TYPE)
public record ApprovalRevisionTrigger(SortableIdentifier identifier, ApprovalActivity activity)
    implements SnapshotTrigger {

  public static final String TYPE = "ApprovalRevision";
}
