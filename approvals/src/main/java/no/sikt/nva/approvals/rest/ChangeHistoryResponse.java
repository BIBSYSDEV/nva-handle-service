package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.UriConstants.CONTEXT_PROPERTY;
import static no.sikt.nva.approvals.utils.UriConstants.CURSOR_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.approvalId;
import static no.sikt.nva.approvals.utils.UriConstants.changeListId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ChangeList;
import no.sikt.nva.approvals.utils.UriConstants;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("ChangeHistory")
public record ChangeHistoryResponse(
    @JsonProperty(CONTEXT_PROPERTY) URI context,
    URI id,
    AboutApproval about,
    List<ChangeResponse> changes,
    URI next) {

  public static ChangeHistoryResponse fromChangeList(ChangeList changeList, String apiHost) {
    var approval = changeList.approval();
    var changes =
        changeList.changes().stream()
            .map(change -> ChangeResponse.fromChange(change, apiHost))
            .toList();
    var next =
        changeList
            .next()
            .map(cursor -> nextUri(apiHost, approval.identifier(), cursor))
            .orElse(null);
    return new ChangeHistoryResponse(
        UriConstants.context(apiHost),
        changeListId(apiHost, approval.identifier()),
        AboutApproval.fromApproval(approval, apiHost),
        changes,
        next);
  }

  private static URI nextUri(String apiHost, UUID approvalIdentifier, SortableIdentifier cursor) {
    return UriWrapper.fromUri(changeListId(apiHost, approvalIdentifier))
        .addQueryParameter(CURSOR_QUERY_PARAMETER, cursor.toString())
        .getUri();
  }

  @JsonTypeInfo(use = Id.NAME, property = "type")
  @JsonTypeName("Approval")
  public record AboutApproval(URI id, URI handle) {

    private static AboutApproval fromApproval(Approval approval, String apiHost) {
      return new AboutApproval(
          approvalId(apiHost, approval.identifier()), approval.handle().value());
    }
  }
}
