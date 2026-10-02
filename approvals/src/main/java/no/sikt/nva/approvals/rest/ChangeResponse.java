package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.Change;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(ApprovalRevisionResponse.class),
  @JsonSubTypes.Type(SourceSnapshotResponse.class)
})
public sealed interface ChangeResponse permits ApprovalRevisionResponse, SourceSnapshotResponse {

  String APPROVAL_PATH = "approval";
  String CHANGES_PATH = "changes";

  static ChangeResponse fromChange(Change change, String apiHost) {
    return switch (change) {
      case ApprovalRevision revision -> {
        var approvalIdentifier = revision.approval().identifier();
        yield new ApprovalRevisionResponse(
            changeUri(apiHost, approvalIdentifier, change),
            approvalUri(apiHost, approvalIdentifier));
      }
      case SourceSnapshot snapshot ->
          new SourceSnapshotResponse(
              changeUri(apiHost, snapshot.approvalIdentifier(), change),
              approvalUri(apiHost, snapshot.approvalIdentifier()));
    };
  }

  private static URI approvalUri(String apiHost, UUID approvalIdentifier) {
    return UriWrapper.fromHost(apiHost)
        .addChild(APPROVAL_PATH)
        .addChild(approvalIdentifier.toString())
        .getUri();
  }

  private static URI changeUri(String apiHost, UUID approvalIdentifier, Change change) {
    return UriWrapper.fromUri(approvalUri(apiHost, approvalIdentifier))
        .addChild(CHANGES_PATH)
        .addChild(change.identifier().toString())
        .getUri();
  }
}
