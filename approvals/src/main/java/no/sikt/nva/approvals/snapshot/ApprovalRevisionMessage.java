package no.sikt.nva.approvals.snapshot;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalActivity;
import no.sikt.nva.approvals.domain.ApprovalRevisionTrigger;
import no.sikt.nva.approvals.persistence.ApprovalImage;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeName(ApprovalRevisionMessage.TYPE)
public record ApprovalRevisionMessage(
    SortableIdentifier revisionIdentifier,
    UUID approvalIdentifier,
    UUID customerIdentifier,
    String clientIdentifier,
    ApprovalActivity activity,
    String approval,
    Instant timestamp)
    implements CreateSnapshotMessage {

  public static final String TYPE = "ApprovalRevision";

  private Approval toApproval() {
    return ApprovalImage.fromJson(approval).toApproval(approvalIdentifier, customerIdentifier);
  }

  @Override
  public CreateSnapshotRequest toCreateSnapshotRequest() {
    return new CreateSnapshotRequest(
        approvalIdentifier,
        new Agent(customerIdentifier, clientIdentifier),
        toApproval().source(),
        new ApprovalRevisionTrigger(revisionIdentifier, activity),
        timestamp);
  }
}
