package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.Map;
import no.sikt.nva.approvals.domain.ApprovalService;
import no.sikt.nva.approvals.domain.ApprovalServiceImpl;
import no.sikt.nva.approvals.utils.EntityTag;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.hc.core5.http.HttpHeaders;

public class FetchApprovalHandler extends ApiGatewayHandler<Void, Object> {

  private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found";
  private static final String NOT_MODIFIED_BODY = "";

  private final ApprovalService approvalService;
  private final String apiHost;

  @JacocoGenerated
  public FetchApprovalHandler() {
    this(ApprovalServiceImpl.defaultInstance(new Environment()), new Environment());
  }

  public FetchApprovalHandler(ApprovalService approvalService, Environment environment) {
    super(Void.class, environment);
    this.approvalService = approvalService;
    this.apiHost = getApiHost(environment);
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(MediaType.JSON_UTF_8, APPLICATION_JSON_LD);
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context) {}

  @Override
  protected Object processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var approval =
        approvalService
            .getApprovalByIdentifier(getApprovalIdentifier(requestInfo))
            .orElseThrow(() -> new NotFoundException(APPROVAL_NOT_FOUND_MESSAGE));

    var entityTag = EntityTag.forVersion(approval.version());
    addAdditionalHeaders(() -> Map.of(HttpHeaders.ETAG, entityTag));
    return EntityTag.isMatchedByIfNoneMatch(requestInfo, entityTag)
        ? NOT_MODIFIED_BODY
        : ApprovalResponse.fromApproval(approval, apiHost);
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, Object output) {
    return NOT_MODIFIED_BODY.equals(output) ? HTTP_NOT_MODIFIED : HTTP_OK;
  }
}
