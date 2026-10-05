package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.getCursor;
import static no.sikt.nva.approvals.utils.RequestUtils.toApiGatewayException;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import no.sikt.nva.approvals.domain.ChangeService;
import no.sikt.nva.approvals.domain.ChangeServiceImpl;
import no.sikt.nva.approvals.utils.RequestUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchApprovalChangesHandler extends ApiGatewayHandler<Void, ChangeListResponse> {

  private final ChangeService changeService;
  private final String apiHost;

  @JacocoGenerated
  public FetchApprovalChangesHandler() {
    this(new Environment());
  }

  @JacocoGenerated
  private FetchApprovalChangesHandler(Environment environment) {
    this(ChangeServiceImpl.defaultInstance(environment), environment);
  }

  public FetchApprovalChangesHandler(ChangeService changeService, Environment environment) {
    super(Void.class, environment);
    this.changeService = changeService;
    this.apiHost = RequestUtils.getApiHost(environment);
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(APPLICATION_JSON_LD, MediaType.JSON_UTF_8);
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    getDefaultResponseContentTypeHeaderValue(requestInfo);
  }

  @Override
  protected ChangeListResponse processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var approvalIdentifier = getApprovalIdentifier(requestInfo);
    var cursor = getCursor(requestInfo);
    return attempt(() -> changeService.listChangesByApproval(approvalIdentifier, cursor))
        .map(changes -> ChangeListResponse.fromChangeList(changes, approvalIdentifier, apiHost))
        .orElseThrow(failure -> toApiGatewayException(failure.getException()));
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, ChangeListResponse output) {
    return HTTP_OK;
  }
}
