package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static no.sikt.nva.approvals.utils.UriConstants.changeId;
import static no.sikt.nva.approvals.utils.UriConstants.viewId;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import no.sikt.nva.approvals.domain.SourceViewService;
import no.sikt.nva.approvals.domain.StoredSourceView;
import no.sikt.nva.approvals.utils.EntityTag;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.hc.core5.http.HttpHeaders;

public class FetchApprovalViewHandler extends ApiGatewayHandler<Void, Object> {

  private static final String VIEW_NOT_FOUND_MESSAGE = "No view is available for the approval";
  private static final String NOT_MODIFIED_BODY = "";
  private static final String TYPE_FIELD = "type";
  private static final String ID_FIELD = "id";
  private static final String WAS_DERIVED_FROM_FIELD = "wasDerivedFrom";

  private final SourceViewService sourceViewService;
  private final String apiHost;

  @JacocoGenerated
  public FetchApprovalViewHandler() {
    this(SourceViewService.defaultInstance(new Environment()), new Environment());
  }

  public FetchApprovalViewHandler(SourceViewService sourceViewService, Environment environment) {
    super(Void.class, environment);
    this.sourceViewService = sourceViewService;
    this.apiHost = getApiHost(environment);
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context) {}

  @Override
  protected Object processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var storedView =
        sourceViewService
            .fetchStoredView(getApprovalIdentifier(requestInfo))
            .orElseThrow(() -> new NotFoundException(VIEW_NOT_FOUND_MESSAGE));
    var entityTag =
        EntityTag.forVersionAndRevision(storedView.snapshotIdentifier(), storedView.viewVersion());
    addAdditionalHeaders(() -> Map.of(HttpHeaders.ETAG, entityTag));
    return EntityTag.isMatchedByIfNoneMatch(requestInfo, entityTag)
        ? NOT_MODIFIED_BODY
        : toResponse(storedView);
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, Object output) {
    return NOT_MODIFIED_BODY.equals(output) ? HTTP_NOT_MODIFIED : HTTP_OK;
  }

  private ObjectNode toResponse(StoredSourceView storedView) {
    ObjectNode view = JsonUtils.dtoObjectMapper.valueToTree(storedView.view());
    var response = JsonUtils.dtoObjectMapper.createObjectNode();
    response.set(TYPE_FIELD, view.remove(TYPE_FIELD));
    response.put(ID_FIELD, viewId(apiHost, storedView.approvalIdentifier()).toString());
    response.put(WAS_DERIVED_FROM_FIELD, derivedFrom(storedView));
    response.setAll(view);
    return response;
  }

  private String derivedFrom(StoredSourceView storedView) {
    return changeId(apiHost, storedView.approvalIdentifier(), storedView.snapshotIdentifier())
        .toString();
  }
}
