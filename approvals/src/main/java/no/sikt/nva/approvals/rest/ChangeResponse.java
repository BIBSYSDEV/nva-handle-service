package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.rest.RestConstants.approvalId;
import static no.sikt.nva.approvals.rest.RestConstants.changeId;
import static no.sikt.nva.approvals.rest.RestConstants.context;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.sikt.nva.approvals.domain.ApprovalActivity;
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
      case ApprovalRevision revision -> createApprovalRevisionResponse(revision, apiHost);
      case SourceSnapshot snapshot -> createSourceSnapshotResponse(apiHost, snapshot);
    };
  }

  private static SourceSnapshotResponse createSourceSnapshotResponse(
      String apiHost, SourceSnapshot snapshot) {
    return new SourceSnapshotResponse(
        context(apiHost),
        changeId(apiHost, snapshot.approvalIdentifier(), snapshot.identifier()),
        approvalId(apiHost, snapshot.approvalIdentifier()),
        snapshot.source(),
        snapshot.createdDate(),
        snapshot.content().type(),
        snapshot.content().hash(),
        HarvestSource.create(snapshot.eventIdentifier()));
  }

  private static ApprovalRevisionResponse createApprovalRevisionResponse(
      ApprovalRevision revision, String apiHost) {
    return new ApprovalRevisionResponse(
        context(apiHost),
        changeId(apiHost, revision.approval().identifier(), revision.identifier()),
        approvalId(apiHost, revision.approval().identifier()),
        revision.createdDate(),
        generatedBy(revision.activity()));
  }

  private static WasGeneratedBy generatedBy(ApprovalActivity activity) {
    return switch (activity) {
      case CREATE_APPROVAL -> new CreateApprovalActivity();
      case UPDATE_APPROVAL -> new UpdateApprovalActivity();
    };
  }
}
