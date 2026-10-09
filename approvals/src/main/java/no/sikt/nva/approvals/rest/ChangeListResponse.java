package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.rest.RestConstants.CONTEXT_PROPERTY;
import static no.sikt.nva.approvals.rest.RestConstants.CURSOR_QUERY_PARAMETER;
import static no.sikt.nva.approvals.rest.RestConstants.approvalId;
import static no.sikt.nva.approvals.rest.RestConstants.changeListId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ChangeList;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("ChangeList")
public record ChangeListResponse(
    @JsonProperty(CONTEXT_PROPERTY) URI context,
    URI id,
    URI approval,
    List<ChangeResponse> changes,
    URI next) {

  public static ChangeListResponse fromChangeList(
      ChangeList changeList, UUID approvalIdentifier, String apiHost) {
    var changes =
        changeList.changes().stream()
            .map(change -> ChangeResponse.fromChange(change, apiHost))
            .toList();
    var next =
        changeList.next().map(cursor -> nextUri(apiHost, approvalIdentifier, cursor)).orElse(null);
    return new ChangeListResponse(
        RestConstants.context(apiHost),
        changeListId(apiHost, approvalIdentifier),
        approvalId(apiHost, approvalIdentifier),
        changes,
        next);
  }

  private static URI nextUri(String apiHost, UUID approvalIdentifier, SortableIdentifier cursor) {
    return UriWrapper.fromUri(changeListId(apiHost, approvalIdentifier))
        .addQueryParameter(CURSOR_QUERY_PARAMETER, cursor.toString())
        .getUri();
  }
}
