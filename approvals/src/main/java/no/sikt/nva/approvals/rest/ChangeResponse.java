package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.Change;
import no.sikt.nva.approvals.domain.SourceSnapshot;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(ApprovalRevisionResponse.class),
  @JsonSubTypes.Type(SourceSnapshotResponse.class)
})
public sealed interface ChangeResponse permits ApprovalRevisionResponse, SourceSnapshotResponse {

  static ChangeResponse fromChange(Change change, String apiHost) {
    return switch (change) {
      case ApprovalRevision revision -> ApprovalRevisionResponse.create(revision, apiHost);
      case SourceSnapshot snapshot -> SourceSnapshotResponse.create(snapshot, apiHost);
    };
  }

  URI context();

  ChangeResponse withContext(URI context);

  URI specializationOf();

  Instant generatedAtTime();

  Activity wasGeneratedBy();
}
