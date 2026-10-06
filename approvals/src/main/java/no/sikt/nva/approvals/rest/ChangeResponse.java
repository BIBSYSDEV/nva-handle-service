package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.rest.RestConstants.APPROVAL_PATH;
import static no.sikt.nva.approvals.rest.RestConstants.CHANGE_PATH;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.Change;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.paths.UriWrapper;

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
        changeUri(apiHost, snapshot.approvalIdentifier(), snapshot.identifier()),
        approvalUri(apiHost, snapshot.approvalIdentifier()));
  }

  private static ApprovalRevisionResponse createApprovalRevisionResponse(
      ApprovalRevision revision, String apiHost) {
    return new ApprovalRevisionResponse(
        changeUri(apiHost, revision.approval().identifier(), revision.identifier()),
        approvalUri(apiHost, revision.approval().identifier()));
  }

  private static URI approvalUri(String apiHost, UUID approvalIdentifier) {
    return UriWrapper.fromHost(apiHost)
        .addChild(APPROVAL_PATH)
        .addChild(approvalIdentifier.toString())
        .getUri();
  }

  private static URI changeUri(
      String apiHost, UUID approvalIdentifier, SortableIdentifier changeIdentifier) {
    return UriWrapper.fromUri(approvalUri(apiHost, approvalIdentifier))
        .addChild(CHANGE_PATH)
        .addChild(changeIdentifier.toString())
        .getUri();
  }
}
