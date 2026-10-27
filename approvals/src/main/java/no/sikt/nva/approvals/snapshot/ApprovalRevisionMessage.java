package no.sikt.nva.approvals.snapshot;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.persistence.ApprovalImage;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeName(ApprovalRevisionMessage.TYPE)
public record ApprovalRevisionMessage(
    SortableIdentifier revisionIdentifier,
    UUID approvalIdentifier,
    UUID customerIdentifier,
    String clientIdentifier,
    String approval,
    Instant timestamp)
    implements CreateSnapshotMessage {

  public static final String TYPE = "ApprovalRevision";

  public Approval toApproval() {
    return ApprovalImage.fromJson(approval).toApproval(approvalIdentifier, customerIdentifier);
  }

  @Override
  public SourceChange toSourceChange() {
    throw new UnsupportedOperationException(
        "SourceChange incompatible with ApprovalRevision yet %s".formatted(toApproval().source()));
  }
}
