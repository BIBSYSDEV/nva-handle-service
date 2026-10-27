package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.UriConstants.CONTEXT_PROPERTY;
import static no.sikt.nva.approvals.utils.UriConstants.approvalId;
import static no.sikt.nva.approvals.utils.UriConstants.changeId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import no.sikt.nva.approvals.domain.ApprovalRevision;

@JsonTypeName("ApprovalRevision")
public record ApprovalRevisionResponse(
    @JsonProperty(CONTEXT_PROPERTY) URI context,
    URI id,
    URI specializationOf,
    Instant generatedAtTime,
    Activity wasGeneratedBy)
    implements ChangeResponse {

  public static ApprovalRevisionResponse create(ApprovalRevision revision, String apiHost) {
    var approval = revision.approval();
    return new ApprovalRevisionResponse(
        null,
        changeId(apiHost, approval.identifier(), revision.identifier()),
        approvalId(apiHost, approval.identifier()),
        revision.createdDate(),
        activity(revision, apiHost));
  }

  @Override
  public ChangeResponse withContext(URI context) {
    return new ApprovalRevisionResponse(
        context, id, specializationOf, generatedAtTime, wasGeneratedBy);
  }

  private static Activity activity(ApprovalRevision revision, String apiHost) {
    var agent = SoftwareAgent.actingFor(revision.agent(), apiHost);
    return switch (revision.activity()) {
      case CREATE_APPROVAL -> new ApprovalCreation(agent);
      case UPDATE_APPROVAL -> new ApprovalUpdate(agent);
    };
  }
}
